package com.unitedinternet.calendar.ldap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import javax.naming.Context;
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

    private String managerUsername;

    private String managerPassword;

    private String ldapTlsReqcert;

    private DirContext context;

    public LdapBindComponent(
            @Value("${ldap.auth.base}") String ldapAuthBase,
            @Value("${ldap.auth.user.pattern}") String ldapAuthUserPattern,
            @Value("${ldap.urls}") String ldapUrls,
            @Value("${ldap.email.manager.username:#{null}}") String managerUsername,
            @Value("${ldap.email.manager.password:#{null}}") String managerPassword,
            @Value("${ldap.tls-reqcert}") String ldapTlsReqcert
    ) {
        this.ldapAuthBase = ldapAuthBase;
        this.ldapAuthUserPattern = ldapAuthUserPattern;
        this.ldapUrls = ldapUrls;
        this.managerUsername = managerUsername;
        this.managerPassword = managerPassword;
        this.ldapTlsReqcert = ldapTlsReqcert;
    }

    public DirContext connect(Authentication authentication){

        String userName = authentication.getName();
        String password = authentication.getCredentials().toString();

        String ldapUser = ldapAuthUserPattern.replace("{0}",userName)+","+ldapAuthBase;

        if(managerUsername !=null && !managerUsername.isEmpty()) {
            ldapUser = managerUsername;
            password = managerPassword;
        }

        Hashtable<String, String> env = new Hashtable<>();
        env.put(javax.naming.Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
        env.put(javax.naming.Context.PROVIDER_URL, ldapUrls);
        env.put(javax.naming.Context.SECURITY_AUTHENTICATION, "simple");
        env.put(javax.naming.Context.SECURITY_PRINCIPAL, ldapUser);
        env.put(javax.naming.Context.SECURITY_CREDENTIALS, password);

        env.put("java.naming.ldap.version", "3");

        env.put("java.naming.security.authentication", "simple");

        env.put("java.naming.ldap.attributes.binary", "tls_reqcert=" + ldapTlsReqcert);

        try {
            context = new InitialDirContext(env);
            LOGGER.info("LDAP bind successful...");
        } catch (NamingException e) {
            LOGGER.error("LDAP bind failed...");
            return null;
        }
        return context;
    }

    public DirContext getContext(){
        return context;
    }


    public boolean checkUsernameAndPassword(String userName, String password) throws NamingException {
        String ldapUser = ldapAuthUserPattern.replace("{0}",userName)+","+ldapAuthBase;
        if(context!=null) {
            Hashtable<?, ?> env = context.getEnvironment();
            if (context.getEnvironment().get(javax.naming.Context.SECURITY_PRINCIPAL).equals(ldapUser)) {
                LOGGER.info("Same user");
                return true;
            }
            Hashtable <String, String> userEnv = (Hashtable<String, String>) env.clone();
            userEnv.put(javax.naming.Context.SECURITY_PRINCIPAL, ldapUser);
            userEnv.put(javax.naming.Context.SECURITY_CREDENTIALS, password);
            context = new InitialDirContext(userEnv);
            LOGGER.info("New context");
            context = new InitialDirContext(env);
            LOGGER.info("Return to old context");
        }  else {
            Hashtable<String, String> userEnv = new Hashtable<>();
            userEnv.put(javax.naming.Context.SECURITY_PRINCIPAL, ldapUser);
            userEnv.put(javax.naming.Context.SECURITY_CREDENTIALS, password);
            context = new InitialDirContext(userEnv);
            LOGGER.info("Create new context");
        }
        return true;
    }


}
