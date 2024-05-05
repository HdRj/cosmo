package com.unitedinternet.calendar;

import com.unitedinternet.calendar.ldap.LdapBindComponent;
import com.unitedinternet.calendar.ldap.LdapSearchComponent;
import com.unitedinternet.calendar.utils.EmailValidator;
import com.unitedinternet.calendar.utils.RandomStringGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.transaction.annotation.Transactional;
import org.unitedinternet.cosmo.acegisecurity.userdetails.CosmoUserDetails;
import org.unitedinternet.cosmo.model.EntityFactory;
import org.unitedinternet.cosmo.model.User;
import org.unitedinternet.cosmo.service.UserService;

import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import java.util.Hashtable;
import java.util.List;

@Transactional
public class WebAppLdapAuthenticationProvider implements AuthenticationProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(WebAppLdapAuthenticationProvider.class);

    private final UserService userService;
    private final EntityFactory entityFactory;
    private final EmailValidator emailValidator;
    private final RandomStringGenerator randomStringGenerator;
    private final LdapSearchComponent ldapSearchComponent;
    private final LdapBindComponent ldapBindComponent;
    private String ldapAuthBase;
    private String managerAuthUsername;
    private String managerAuthPassword;

    public WebAppLdapAuthenticationProvider(
            UserService userService,
            EntityFactory entityFactory,
            EmailValidator emailValidator,
            RandomStringGenerator randomStringGenerator,
            LdapSearchComponent ldapSearchComponent,
            LdapBindComponent ldapBindComponent,
            String ldapAuthBase,
            String managerAuthUsername,
            String managerAuthPassword
    ) {
        this.userService = userService;
        this.entityFactory = entityFactory;
        this.emailValidator = emailValidator;
        this.randomStringGenerator = randomStringGenerator;
        this.ldapSearchComponent = ldapSearchComponent;
        this.ldapBindComponent = ldapBindComponent;
        this.ldapAuthBase = ldapAuthBase;
        this.managerAuthUsername = managerAuthUsername;
        this.managerAuthPassword = managerAuthPassword;
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {

        String userName = authentication.getName();
        String password = authentication.getCredentials().toString();


        DirContext context = ldapBindComponent.connect(authentication);

        if(context == null){
            return null;
        }

        if(managerAuthUsername !=null && !managerAuthUsername.isEmpty()) {
            userName = managerAuthUsername;
            password = managerAuthPassword;
        }

        try{
            ldapBindComponent.checkUsernameAndPassword(userName,password);
        } catch (NamingException e) {
            LOGGER.error("[Auth] Username and/or password are incorrect");
            return null;
        }

        User user = getUser(userName);

        if(user == null) {
            String email;
            if (emailValidator.checkEmail(userName)) {
                email = userName;
            } else {
                String organization = ldapSearchComponent.getOrganization("uid="+userName+","+ldapAuthBase,context);
                LOGGER.info("o: " + organization);
                List<String> emails = ldapSearchComponent.search(userName, organization,context);
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
