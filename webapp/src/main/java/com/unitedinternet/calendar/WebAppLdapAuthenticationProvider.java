package com.unitedinternet.calendar;

import com.unitedinternet.calendar.ldap.LdapSearchComponent;
import com.unitedinternet.calendar.utils.EmailValidator;
import com.unitedinternet.calendar.utils.RandomStringGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.ldap.core.DirContextAdapter;
import org.springframework.ldap.core.support.LdapContextSource;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.ldap.authentication.BindAuthenticator;
import org.springframework.security.ldap.authentication.LdapAuthenticationProvider;
import org.springframework.security.ldap.userdetails.LdapUserDetails;
import org.springframework.security.ldap.userdetails.LdapUserDetailsImpl;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.unitedinternet.cosmo.acegisecurity.userdetails.CosmoUserDetails;
import org.unitedinternet.cosmo.model.EntityFactory;
import org.unitedinternet.cosmo.model.User;
import org.unitedinternet.cosmo.service.UserService;

import java.util.List;
import java.util.Map;

@Primary
@Component
@Transactional
public class WebAppLdapAuthenticationProvider implements AuthenticationProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(WebAppLdapAuthenticationProvider.class);

    private final UserService userService;
    private final EntityFactory entityFactory;
    private final LdapContextSource ldapContextSource;
    private final EmailValidator emailValidator;
    private final RandomStringGenerator randomStringGenerator;
    private final LdapSearchComponent ldapSearchComponent;

    @Value("${ldap.auth.base}")
    private String ldapAuthBase;
    @Value("${ldap.auth.user.pattern}")
    private String ldapAuthUserPattern;

    public WebAppLdapAuthenticationProvider(UserService userService, EntityFactory entityFactory, LdapContextSource ldapContextSource, EmailValidator emailValidator, RandomStringGenerator randomStringGenerator, LdapSearchComponent ldapSearchComponent) {
        this.userService = userService;
        this.entityFactory = entityFactory;
        this.ldapContextSource = ldapContextSource;
        this.emailValidator = emailValidator;
        this.randomStringGenerator = randomStringGenerator;
        this.ldapSearchComponent = ldapSearchComponent;
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {

        BindAuthenticator bindAuthenticator = new BindAuthenticator(ldapContextSource);
        String [] patterns = {ldapAuthUserPattern+","+ldapAuthBase};

        bindAuthenticator.setUserDnPatterns(patterns);

        LdapAuthenticationProvider ldapAuthenticationProvider = new LdapAuthenticationProvider(bindAuthenticator);

        Authentication ldapAuthentication = ldapAuthenticationProvider.authenticate(authentication);

        String userName = ldapAuthentication.getName();

        LdapUserDetails ldapUserDetails = (LdapUserDetails) ldapAuthentication.getPrincipal();

//        LOGGER.info("DN: " + ldapUserDetails.getDn());
//        LOGGER.info("UserName: " + ldapUserDetails.getUsername());
//        LOGGER.info("All: " + ldapUserDetails);

        User user = getUser(userName);

        if(user == null) {
            String email;
            if (emailValidator.checkEmail(userName)) {
                email = userName;
            } else {
                String organization = ldapSearchComponent.getOrganization(ldapUserDetails.getDn());
                LOGGER.info("o: " + organization);
                List<String> emails = ldapSearchComponent.search(userName, organization);
                if (emails.isEmpty()) {
                    LOGGER.error("[AUTH] Email address is not found for user: {}", userName);
                    return null;
                }
                email = emails.get(0);
            }

            if (!emailValidator.checkEmail(email)) {
                LOGGER.error("[AUTH] Email address is not valid: {}", email);
                return null;
            }
            user = this.createUserIfNotPresent(userName, email);
            if (user == null) {
                return null;
            }
        }
        return new UsernamePasswordAuthenticationToken(
                new CosmoUserDetails(user),
                authentication.getCredentials(),
                authentication.getAuthorities()
        );

    }

    @Override
    public boolean supports(Class<?> authentication) {
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
    }

    private User getUser(String userName){
        User user = this.userService.getUser(userName);
        if (user != null) {
            LOGGER.info("[AUTH] Found user with email address: {}", user.getEmail());
            return user;
        } else {
            return null;
        }
    }

    private User createUserIfNotPresent(String userName,String email) {
        User user = this.userService.getUser(userName);
        if (user != null) {
            LOGGER.info("[AUTH] Found user with email address: {}", user.getEmail());
            return user;
        }

        LOGGER.info("[AUTH] No user found for uid address: {}. Creating one...", userName);
        user = this.entityFactory.createUser();
        user.setUsername(userName);
        user.setEmail(email);
        user.setFirstName(userName);
        user.setLastName(userName);
        user.setPassword(randomStringGenerator.generatePassword(16));
        try {
            user = this.userService.createUser(user);
        }catch (Exception e){
            LOGGER.info("[AUTH] Can't create new user "+ e.getMessage());
            return null;
        }
        return user;
    }
}
