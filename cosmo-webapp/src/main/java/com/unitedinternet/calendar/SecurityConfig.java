package com.unitedinternet.calendar;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.ldap.core.ContextSource;
import org.springframework.ldap.core.support.BaseLdapPathContextSource;
import org.springframework.security.ldap.DefaultSpringSecurityContextSource;
import org.springframework.security.ldap.authentication.BindAuthenticator;
import org.springframework.security.ldap.authentication.LdapAuthenticationProvider;
import org.springframework.security.ldap.authentication.LdapAuthenticator;
import org.springframework.security.ldap.server.UnboundIdContainer;
import org.springframework.security.ldap.userdetails.PersonContextMapper;
import org.springframework.beans.factory.annotation.Value;

@Configuration
public class SecurityConfig {

    @Value("${ldap.port}")
    private String ldapPort;
    @Value("${ldap.host}")
    private String ldapHost;
    @Value("${ldap.parameters}")
    private String ldapParameters;


    @Bean
    UnboundIdContainer unboundIdContainer() {
        UnboundIdContainer container = new UnboundIdContainer("dc=me,dc=local", "classpath:users.ldif");
        container.setPort(0);
        return container;
    }



    @Bean
    ContextSource contextSource(UnboundIdContainer unboundIdContainer) {
        return new DefaultSpringSecurityContextSource(ldapHost+":" + ldapPort + "/" + ldapParameters);
    }


    @Bean
    BindAuthenticator bindAuthenticator(BaseLdapPathContextSource contextSource) {
        BindAuthenticator bindAuthenticator = new BindAuthenticator(contextSource);
        bindAuthenticator.setUserDnPatterns(new String[]{"uid={0},ou=accounts"});
        return bindAuthenticator;
    }

    @Bean
    AuthenticationManager authenticationManager(BaseLdapPathContextSource contextSource) {
        LdapBindAuthenticationManagerFactory factory = new LdapBindAuthenticationManagerFactory(contextSource);
        factory.setUserDnPatterns("uid={0},ou=people");
        return factory.createAuthenticationManager();
    }

    @Bean
    LdapAuthenticationProvider authenticationProvider(LdapAuthenticator ldapAuthenticator) {
        LdapAuthenticationProvider ldapAuthenticationProvider = new LdapAuthenticationProvider(ldapAuthenticator);
        ldapAuthenticationProvider.setUserDetailsContextMapper(new PersonContextMapper());
        return ldapAuthenticationProvider;
    }

}