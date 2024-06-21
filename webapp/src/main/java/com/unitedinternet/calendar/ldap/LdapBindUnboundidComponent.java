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
import java.security.GeneralSecurityException;

@Component
public class LdapBindUnboundidComponent {

    private static final Logger LOGGER = LoggerFactory.getLogger(LdapBindUnboundidComponent.class);

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
        try {
            managerConnection = createLDAPConnection(ldapUrls, userName, password);
            if(managerConnection!=null) {
                userDn = userName;
                LOGGER.info("LDAP manager bind successful...");
            }
        } catch (Exception e) {
            LOGGER.error("LDAP manager bind failed: {}", e.getMessage());
            return null;
        }
        return managerConnection;
    }

    private static LDAPConnection createLDAPConnection( String ldapUrls, String userDn, String password) {
        LDAPConnection connection = null;

        String [] urls = ldapUrls.split(",");
        String host = null;
        int port = -1;
        for (int i = 0; i < urls.length; i++) {
            try {
                String [] parts = urls[i].split(":");
                boolean useSSL = false;
                if(parts[0].startsWith("ldaps")){
                    useSSL = true;
                }
                host = parts[1].substring(2);
                port = Integer.parseInt(parts[2]);
                LDAPConnectionOptions options = new LDAPConnectionOptions();
                options.setUseSynchronousMode(true);

                if (useSSL) {
                    SSLUtil sslUtil = new SSLUtil(new TrustAllTrustManager());
                    connection = new LDAPConnection(sslUtil.createSSLSocketFactory(), host, port, userDn, password);
                } else {
                    connection = new LDAPConnection(host, port, userDn, password);
                }

                LOGGER.info("Connected to " + host + ":" + port);
                break;
            } catch (LDAPException e) {
                LOGGER.error("Failed to connect to " + host + ":" + port + " " + e.getMessage());
            } catch (GeneralSecurityException e) {
                throw new RuntimeException(e);
            }
        }

        if (connection == null) {
            System.err.println("Failed to connect to any of the specified ports.");
        }

        return connection;
    }

}
