package com.unitedinternet.calendar.ldap;

import com.unboundid.ldap.sdk.LDAPConnection;
import com.unboundid.ldap.sdk.LDAPConnectionOptions;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.util.ssl.SSLUtil;
import com.unboundid.util.ssl.TrustAllTrustManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.naming.NamingException;
import java.net.UnknownHostException;
import java.security.GeneralSecurityException;

@Component
public class LdapBindUnboundidComponent {

    private static final Logger LOGGER = LoggerFactory.getLogger(LdapBindUnboundidComponent.class);

    private ServerSet serverSet;

    private String ldapAuthBase;

    private String ldapAuthUserPattern;

    private String ldapUrls;

    private String managerAuthUsername;

    private String managerAuthPassword;

    private String managerEmailUsername;

    private String managerEmailPassword;

    private String ldapTlsReqcert;

    private LDAPConnection managerConnection;

    private String userDn;

    public LdapBindUnboundidComponent(
            @Value("${ldap.auth.base}") String ldapAuthBase,
            @Value("${ldap.auth.user.pattern}") String ldapAuthUserPattern,
            @Value("${ldap.urls}") String ldapUrls,
            @Value("${ldap.auth.manager.username:#{null}}") String managerAuthUsername,
            @Value("${ldap.auth.manager.password:#{null}}") String managerAuthPassword,
            @Value("${ldap.email.manager.username:#{null}}") String managerEmailUsername,
            @Value("${ldap.email.manager.password:#{null}}") String managerEmailPassword,
            @Value("${ldap.tls-reqcert}") String ldapTlsReqcert
    ) {
        this.ldapAuthBase = ldapAuthBase;
        this.ldapAuthUserPattern = ldapAuthUserPattern;
        this.ldapUrls = ldapUrls;
        this.managerAuthUsername = managerAuthUsername;
        this.managerAuthPassword = managerAuthPassword;
        this.managerEmailUsername=managerEmailUsername;
        this.managerEmailPassword=managerEmailPassword;
        this.ldapTlsReqcert = ldapTlsReqcert;

        initializeFailoverServerSet(ldapUrls); // Initialize the FailoverServerSet
    }

    public LDAPConnection userConnectByUserDn(String userDn, String password) throws NamingException {
        LDAPConnection userContext;

        try {
            userContext = createLDAPConnection(ldapUrls, userDn, password);
        } catch (Exception e) {
            LOGGER.error("LDAP bind failed...");
            return null;
        }
        return userContext;
    }

    public LDAPConnection userConnectByUserName(String userName, String password) throws NamingException {

        String userDn = ldapAuthUserPattern.replace("{0}",userName)+","+ldapAuthBase;
        return userConnectByUserDn(userDn, password);

    }

    public LDAPConnection mangerAuthConnect(){
        String userName = managerAuthUsername;
        String password = managerAuthPassword;

        if(managerConnection !=null) {
            if (userDn.equals(userName)) {
                LOGGER.info("Same manager in auth connect");
                return managerConnection;
            }
        }
        return managerConnect(userName,password);
    }

    public LDAPConnection managerEmailConnect(){
        String userName = managerEmailUsername;
        String password = managerEmailPassword;

        if(managerConnection !=null) {
            if (userDn.equals(userName)) {
                LOGGER.info("Same manager in email connect");
                return managerConnection;
            }
        }
        return managerConnect(userName,password);
    }


    public boolean isLdapEmailManagerExists(){
        return managerEmailUsername !=null && !managerEmailUsername.isEmpty();
    }

    public boolean isLdapAuthManagerExists(){
        return managerAuthUsername !=null && !managerAuthUsername.isEmpty();
    }

    public LDAPConnection managerConnect(String userName, String password) {
        if (managerConnection != null && managerConnection.isConnected()) {
            if (userDn.equals(userName)) {
                LOGGER.info("Reusing existing manager connection");
                return managerConnection;
            }
        }

        try {
            LOGGER.info("Reconnecting manager connection...");
            managerConnection = retryConnection(userName, password);
            if (managerConnection != null) {
                userDn = userName;
                LOGGER.info("LDAP manager bind successful");
            }
        } catch (Exception e) {
            LOGGER.error("LDAP manager bind failed: {}", e.getMessage());
            managerConnection = null;
        }
        return managerConnection;
    }

    private static LDAPConnection createLDAPConnection(String ldapUrls, String userDn, String password) throws LDAPException {
        LDAPConnection connection = null;

        if (serverSet != null) {
            // Use FailoverServerSet if it is initialized
            try {
                connection = serverSet.getConnection();
                connection.bind(userDn, password);
                LOGGER.info("Connected to LDAP server via FailoverServerSet");
                return connection;
            } catch (LDAPException e) {
                LOGGER.error("FailoverServerSet connection failed: " + e.getMessage());
            }
        }

        // Fall back to iterating over provided URLs
        String[] urls = ldapUrls.split(",");
        CustomNameResolver customNameResolver = new CustomNameResolver("192.168.30."); // Custom resolver
        String host = null;
        int port = -1;

        for (String url : urls) {
            try {
                String[] parts = url.split(":");
                boolean useSSL = parts[0].startsWith("ldaps");
                host = parts[1].substring(2); // Skip "//"
                port = Integer.parseInt(parts[2]);

                LDAPConnectionOptions options = new LDAPConnectionOptions();
                options.setUseSynchronousMode(true);

                if (useSSL) {
                    // Resolve hostname manually
                    String resolvedHost = customNameResolver.resolve(host);
                    SSLUtil sslUtil = new SSLUtil(new TrustAllTrustManager()); // No CustomNameResolver here
                    connection = new LDAPConnection(
                            sslUtil.createSSLSocketFactory(), // SSLSocketFactory
                            resolvedHost,                     // Resolved host
                            port,                             // Port
                            userDn,                           // User DN
                            password                          // Password
                    );
                } else {
                    // Resolve hostname manually and create a non-SSL connection
                    String resolvedHost = customNameResolver.resolve(host);
                    connection = new LDAPConnection(
                            resolvedHost, // Resolved host
                            port,         // Port
                            userDn,       // User DN
                            password      // Password
                    );
                }

                LOGGER.info("Connected to " + host + ":" + port);
                break;
            } catch (LDAPException e) {
                LOGGER.error("Failed to connect to " + host + ":" + port + " - " + e.getMessage());
            } catch (GeneralSecurityException e) {
                throw new RuntimeException("Failed to initialize SSL for connection to " + host + ":" + port, e);
            } catch (UnknownHostException e) {
                throw new RuntimeException(e);
            }
        }

        if (connection == null) {
            throw new LDAPException("Failed to connect to any of the specified LDAP servers.");
        }

        return connection;
    }

    public LDAPConnection retryConnection( String userDn, String password) throws LDAPException {
        int maxRetries = 5;
        int retryDelay = 2000; // 2 seconds

        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            return createLDAPConnection(ldapUrls, userDn, password);
        }
        LOGGER.error("Exhausted all retries to connect to LDAP server.");
        return null;
    }

    private void initializeFailoverServerSet(String ldapUrls) {
        String[] urls = ldapUrls.split(",");
        List<HostAndPort> servers = new ArrayList<>();

        for (String url : urls) {
            String[] parts = url.split(":");
            String host = parts[1].substring(2); // Skip "//"
            int port = Integer.parseInt(parts[2]);
            servers.add(new HostAndPort(host, port));
        }

        // Create FailoverServerSet
        this.serverSet = new FailoverServerSet(
                servers.toArray(new HostAndPort[0]),
                new LDAPConnectionOptions(),
                new TrustAllTrustManager() // Replace with a proper trust manager in production
        );
    }

}
