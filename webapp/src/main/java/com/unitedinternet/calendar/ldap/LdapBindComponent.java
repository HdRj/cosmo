package com.unitedinternet.calendar.ldap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.naming.NamingException;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import java.util.Hashtable;

@Component
public class LdapBindComponent {

    private static final Logger LOGGER = LoggerFactory.getLogger(LdapBindComponent.class);

    private String ldapAuthBase;

    private String ldapAuthUserPattern;

    private String ldapUrls;

    private String managerAuthUsername;

    private String managerAuthPassword;

    private String managerEmailUsername;

    private String managerEmailPassword;

    private String ldapTlsReqcert;

    private DirContext managerContext;
    
    private DirContext userContext;

    public LdapBindComponent(
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

    public DirContext userConnectByUserDn(String userDn, String password) throws NamingException {
        if(userContext !=null) {
            if (userContext.getEnvironment().get(javax.naming.Context.SECURITY_PRINCIPAL).equals(userDn)) {
                LOGGER.info("Same user in connect");
                return userContext;
            }
        }

        Hashtable<String, String> env = new Hashtable<>();
        env.put(javax.naming.Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
        env.put(javax.naming.Context.PROVIDER_URL, ldapUrls);
        env.put(javax.naming.Context.SECURITY_AUTHENTICATION, "simple");
        env.put(javax.naming.Context.SECURITY_PRINCIPAL, userDn);
        env.put(javax.naming.Context.SECURITY_CREDENTIALS, password);

        env.put("java.naming.ldap.version", "3");

        env.put("java.naming.security.authentication", "simple");

        env.put("java.naming.ldap.attributes.binary", "tls_reqcert=" + ldapTlsReqcert);

        try {
            userContext = new InitialDirContext(env);
            LOGGER.info("LDAP bind successful...");
        } catch (NamingException e) {
            LOGGER.error("LDAP bind failed...");
            return null;
        }
        return userContext;
    }

    public DirContext userConnectByUserName(String userName, String password) throws NamingException {

        String userDn = ldapAuthUserPattern.replace("{0}",userName)+","+ldapAuthBase;
        return userConnectByUserDn(userDn, password);

    }

    public DirContext mangerAuthConnect() throws NamingException {
        String userName = managerAuthUsername;
        String password = managerAuthPassword;

        if(managerContext !=null) {
            if (managerContext.getEnvironment().get(javax.naming.Context.SECURITY_PRINCIPAL).equals(userName)) {
                LOGGER.info("Same manager in auth connect");
                return managerContext;
            }
        }
        return managerConnect(userName,password);
    }

    public DirContext managerEmailConnect() throws NamingException {
        String userName = managerEmailUsername;
        String password = managerEmailPassword;

        if(managerContext !=null) {
            if (managerContext.getEnvironment().get(javax.naming.Context.SECURITY_PRINCIPAL).equals(userName)) {
                LOGGER.info("Same manager in email connect");
                return managerContext;
            }
        }
        return managerConnect(userName,password);
    }


    public DirContext getManagerContext(){
        return managerContext;
    }

    public boolean isLdapEmailManagerExists(){
        return managerEmailUsername !=null && !managerEmailUsername.isEmpty();
    }

    public boolean isLdapAuthManagerExists(){
        return managerAuthUsername !=null && !managerAuthUsername.isEmpty();
    }

    private DirContext managerConnect(String userName, String password){
        Hashtable<String, String> env = new Hashtable<>();
        env.put(javax.naming.Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
        env.put(javax.naming.Context.PROVIDER_URL, ldapUrls);
        env.put(javax.naming.Context.SECURITY_AUTHENTICATION, "simple");
        env.put(javax.naming.Context.SECURITY_PRINCIPAL, userName);
        env.put(javax.naming.Context.SECURITY_CREDENTIALS, password);

        env.put("java.naming.ldap.version", "3");

        env.put("java.naming.security.authentication", "simple");

        env.put("java.naming.ldap.attributes.binary", "tls_reqcert=" + ldapTlsReqcert);

        try {
            managerContext = new InitialDirContext(env);
            LOGGER.info("LDAP manager bind successful...");
        } catch (NamingException e) {
            LOGGER.error("LDAP manager bind failed...");
            return null;
        }
        return managerContext;
    }

}
