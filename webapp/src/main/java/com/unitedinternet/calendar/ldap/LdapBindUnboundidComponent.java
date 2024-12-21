package com.unitedinternet.calendar.ldap;

import com.unboundid.ldap.sdk.*;
import com.unboundid.util.ssl.SSLUtil;
import com.unboundid.util.*;
import com.unboundid.util.ssl.TrustAllTrustManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.naming.NamingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.List;

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

    private LDAPConnection createLDAPConnection(String ldapUrls, String userDn, String password) throws LDAPException {
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

        // Fallback to connecting using the provided URLs
        String[] urls = ldapUrls.split(",");
        CustomNameResolver customNameResolver = new CustomNameResolver("192.168.30."); // Custom resolver
        String host = null;
        int port = -1;

        // Iterate over each provided URL and attempt to establish a connection
        for (String url : urls) {
            try {
                String[] parts = url.split(":");
                boolean useSSL = parts[0].startsWith("ldaps");  // Check if SSL is required
                host = parts[1].substring(2); // Skip "//" in the URL
                port = Integer.parseInt(parts[2]);

                LDAPConnectionOptions options = new LDAPConnectionOptions();
                options.setUseSynchronousMode(true);

                if (useSSL) {
                    // Resolve the host manually if SSL is used
                    String resolvedHost = customNameResolver.resolve(host);
                    SSLUtil sslUtil = new SSLUtil(new TrustAllTrustManager()); // No CustomNameResolver here for SSL
                    connection = new LDAPConnection(
                            sslUtil.createSSLSocketFactory(), // SSLSocketFactory
                            resolvedHost,                     // Resolved host
                            port,                             // Port
                            userDn,                           // User DN
                            password                          // Password
                    );
                } else {
                    // Resolve the host manually and create a non-SSL connection
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
                throw new RuntimeException("Host resolution failed for: " + host, e);
            }
        }

        // If no connection was established, throw an exception
        if (connection == null) {
            LOGGER.error("Failed to connect to any of the specified LDAP servers.");
            throw new LDAPException(ResultCode.CONNECT_ERROR, "Failed to connect to any of the specified LDAP servers.");
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
        List<URI> servers = new ArrayList<>();

        // Process each provided URL and convert it to a URI
        for (String url : urls) {
            try {
                URI uri = new URI(url);
                servers.add(uri);
                LOGGER.info("Configured server URI: " + uri.toString());
            } catch (URISyntaxException e) {
                LOGGER.error("Invalid URI format: {}", url, e);
            }
        }

        // Extract host and port from the URI for each server
        for (URI uri : servers) {
            String host = uri.getHost();
            int port = uri.getPort();
            LOGGER.info("Host: {}, Port: {}", host, port);
            // Now you can use the host and port to configure your LDAP connection
        }
    }
}
