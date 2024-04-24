package com.unitedinternet.calendar;

import com.unitedinternet.calendar.ldap.LdapSearchComponent;
import com.unitedinternet.calendar.utils.EmailValidator;
import com.unitedinternet.calendar.utils.RandomStringGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.ldap.core.support.LdapContextSource;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;

import org.springframework.beans.factory.annotation.Value;
import org.unitedinternet.cosmo.model.EntityFactory;
import org.unitedinternet.cosmo.service.UserService;

import java.util.Collections;


@Configuration
@EnableWebSecurity
public class SecurityConfig extends WebSecurityConfigurerAdapter {

    @Value("${ldap.urls}")
    private String ldapUrls;
    @Value("${ldap.auth.manager.username:#{null}}")
    private String ldapUserDn;
    @Value("${ldap.auth.manager.password:#{null}}")
    private String ldapPassword;
    @Value("${spring.security.enable-csrf}")
    private Boolean enableCSRF;
    @Value("${spring.security.enable-cors}")
    private Boolean enableCors;

    @Value("${ldap.email.filter}")
    private String ldapFilter;

    @Value("${ldap.email.base}")
    private String ldapBase;

    @Value("${ldap.email.attribute}")
    private String ldapAttribute;

    @Value("${ldap.email.search-scope}")
    private String searchScope;

    @Value("${ldap.email.count-limit}")
    private String countLimit;

    @Value("${ldap.tls-reqcert}")
    private String ldapTlsReqcert;

    private final UserService userService;
    private final EntityFactory entityFactory;
    private final EmailValidator emailValidator;
    private final RandomStringGenerator randomStringGenerator;


    public SecurityConfig(UserService userService, EntityFactory entityFactory, EmailValidator emailValidator, RandomStringGenerator randomStringGenerator) {
        this.userService = userService;
        this.entityFactory = entityFactory;
        this.emailValidator = emailValidator;
        this.randomStringGenerator = randomStringGenerator;
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> {if(!enableCSRF) csrf.disable();})
                .cors(cors -> {if(!enableCSRF) cors.disable();})
                .authorizeRequests()
                .antMatchers("/url/**").permitAll()
                //.antMatchers("/actuator/**").hasRole("USER")
                .antMatchers("/test/**").permitAll()
                .anyRequest().authenticated()
                .and()
                .httpBasic();
    }


    @Bean
    public LdapContextSource contextSource() {
        LdapContextSource ldapContextSource = new LdapContextSource();
        ldapContextSource.setUrl(ldapUrls);
        if(ldapUserDn!=null && !ldapUserDn.isEmpty()) {
            ldapContextSource.setUserDn(ldapUserDn);
            ldapContextSource.setPassword(ldapPassword);
        }else {
            ldapContextSource.setAnonymousReadOnly(true);
            ldapContextSource.afterPropertiesSet();
        }
        ldapContextSource.setBaseEnvironmentProperties(
                Collections.singletonMap("java.naming.ldap.attributes.binary", "tls_reqcert="+ldapTlsReqcert)
        );
        return ldapContextSource;
    }

    @Bean
    public LdapTemplate ldapTemplate() {
        return new LdapTemplate(contextSource());
    }

//    @Override
//    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
//        BindAuthenticator bindAuthenticator = new BindAuthenticator(contextSource());
//        String [] patterns = {"uid={0},ou=accounts,ou=caldav,ou=services,dc=me,dc=local"};
//        bindAuthenticator.setUserDnPatterns(patterns);
//        LdapAuthenticationProvider ldapAuthenticationProvider = new LdapAuthenticationProvider(bindAuthenticator);
//        auth.authenticationProvider(ldapAuthenticationProvider);
//    }

    @Bean
    public WebAppLdapAuthenticationProvider customAuthenticationProvider() {
        return new WebAppLdapAuthenticationProvider(
                userService,
                entityFactory,
                contextSource(),
                emailValidator,
                randomStringGenerator,
                new LdapSearchComponent(ldapFilter, ldapBase, ldapAttribute, searchScope, countLimit, ldapTemplate())
        );
    }

    @Override
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
        auth.authenticationProvider(customAuthenticationProvider());
    }

}