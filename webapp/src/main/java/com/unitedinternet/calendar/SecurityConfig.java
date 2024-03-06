package com.unitedinternet.calendar;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.ldap.core.ContextSource;
import org.springframework.ldap.core.support.BaseLdapPathContextSource;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.ldap.LdapBindAuthenticationManagerFactory;
import org.springframework.security.ldap.DefaultSpringSecurityContextSource;
import org.springframework.security.ldap.authentication.BindAuthenticator;
import org.springframework.security.ldap.authentication.LdapAuthenticationProvider;
import org.springframework.security.ldap.authentication.LdapAuthenticator;
import org.springframework.security.ldap.server.UnboundIdContainer;
import org.springframework.security.ldap.userdetails.PersonContextMapper;
import org.springframework.beans.factory.annotation.Value;
import org.unitedinternet.cosmo.model.EntityFactory;
import org.unitedinternet.cosmo.service.UserService;

@Configuration
@EnableWebSecurity
public class SecurityConfig extends WebSecurityConfigurerAdapter {

    @Value("${ldap.port}")
    private String ldapPort;
    @Value("${ldap.host}")
    private String ldapHost;
    @Value("${ldap.parameters}")
    private String ldapParameters;

    private final UserService userService;
    private final EntityFactory entityFactory;

    public SecurityConfig(UserService userService, EntityFactory entityFactory) {
        this.userService = userService;
        this.entityFactory = entityFactory;
    }

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

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.authorizeRequests()
                .antMatchers("/url/**").permitAll()
                //.antMatchers("/actuator/**").hasRole("USER")
                .antMatchers("/test/**").permitAll()
                .anyRequest().authenticated()
                .and()
                .httpBasic();
    }

    @Bean
    public AllowAllAuthenticationProvider customAuthenticationProvider() {
        return new AllowAllAuthenticationProvider(userService,entityFactory);
    }

    @Override
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
        auth.authenticationProvider(customAuthenticationProvider());
    }

}