package com.unitedinternet.calendar;

import com.unitedinternet.calendar.ldap.LdapBindComponent;
import com.unitedinternet.calendar.ldap.LdapSearchComponent;
import com.unitedinternet.calendar.utils.EmailValidator;
import com.unitedinternet.calendar.utils.RandomStringGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.NoOpPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.unitedinternet.cosmo.model.EntityFactory;
import org.unitedinternet.cosmo.service.UserService;


@Configuration
@EnableWebSecurity
public class SecurityConfig extends WebSecurityConfigurerAdapter {

    @Value("${spring.security.enable-csrf}")
    private Boolean enableCSRF;
    @Value("${spring.security.enable-cors}")
    private Boolean enableCors;
    @Value("${ldap.auth.base}")
    private String ldapAuthBase;
    @Value("${ldap.auth.manager.username:#{null}}")
    String managerAuthUsername;
    @Value("${ldap.auth.manager.password:#{null}}")
    String managerAuthPassword;


    private final UserService userService;
    private final EntityFactory entityFactory;
    private final EmailValidator emailValidator;
    private final RandomStringGenerator randomStringGenerator;
    private final LdapSearchComponent ldapSearchComponent;
    private final LdapBindComponent ldapBindComponent;


    public SecurityConfig(UserService userService, EntityFactory entityFactory, EmailValidator emailValidator, RandomStringGenerator randomStringGenerator, LdapSearchComponent ldapSearchComponent, LdapBindComponent ldapBindComponent) {
        this.userService = userService;
        this.entityFactory = entityFactory;
        this.emailValidator = emailValidator;
        this.randomStringGenerator = randomStringGenerator;
        this.ldapSearchComponent = ldapSearchComponent;
        this.ldapBindComponent = ldapBindComponent;
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> {if(!enableCSRF) csrf.disable();})
                .cors(cors -> {if(!enableCors) cors.disable();})
                .authorizeRequests()
                .antMatchers("/url/**").permitAll()
                //.antMatchers("/actuator/**").hasRole("USER")
                .antMatchers("/test/**").permitAll()
                .anyRequest().authenticated()
                .and()
                .httpBasic();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return NoOpPasswordEncoder.getInstance();
    }

//    @Bean
//    public LdapContextSource contextSource() {
//        LdapContextSource ldapContextSource = new LdapContextSource();
//        ldapContextSource.setUrl(ldapUrls);
//        if(ldapUserDn!=null && !ldapUserDn.isEmpty()) {
//            ldapContextSource.setUserDn(ldapUserDn);
//            ldapContextSource.setPassword(ldapPassword);
//        }else {
//            ldapContextSource.setAnonymousReadOnly(true);
//            ldapContextSource.afterPropertiesSet();
//        }
//        ldapContextSource.setBaseEnvironmentProperties(
//                Collections.singletonMap("java.naming.ldap.attributes.binary", "tls_reqcert="+ldapTlsReqcert)
//        );
//        return ldapContextSource;
//    }

//    @Bean
//    public LdapTemplate ldapTemplate() {
//        return new LdapTemplate(contextSource());
//    }

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
                emailValidator,
                randomStringGenerator,
                ldapSearchComponent,
                ldapBindComponent,
                ldapAuthBase
        );
    }

    @Override
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
        auth.authenticationProvider(customAuthenticationProvider());
    }

}