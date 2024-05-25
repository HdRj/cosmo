package com.unitedinternet.calendar;

import com.unboundid.ldap.sdk.LDAPConnection;
import com.unitedinternet.calendar.ldap.LdapBindUnboundidComponent;
import com.unitedinternet.calendar.ldap.LdapSearchUnboundidComponent;
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

import java.util.List;

@Transactional
public class WebAppLdapAuthenticationProvider implements AuthenticationProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(WebAppLdapAuthenticationProvider.class);

    private final UserService userService;
    private final EntityFactory entityFactory;
    private final EmailValidator emailValidator;
    private final RandomStringGenerator randomStringGenerator;
    private final LdapSearchUnboundidComponent ldapSearchComponent;
    private final LdapBindUnboundidComponent ldapBindComponent;
    private String ldapAuthBase;
//    private String managerAuthUsername;
//    private String managerAuthPassword;

    public WebAppLdapAuthenticationProvider(
            UserService userService,
            EntityFactory entityFactory,
            EmailValidator emailValidator,
            RandomStringGenerator randomStringGenerator,
            LdapSearchUnboundidComponent ldapSearchComponent,
            LdapBindUnboundidComponent ldapBindComponent,
            String ldapAuthBase
    ) {
        this.userService = userService;
        this.entityFactory = entityFactory;
        this.emailValidator = emailValidator;
        this.randomStringGenerator = randomStringGenerator;
        this.ldapSearchComponent = ldapSearchComponent;
        this.ldapBindComponent = ldapBindComponent;
        this.ldapAuthBase = ldapAuthBase;
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {

        String userName = authentication.getName();
        String password = authentication.getCredentials().toString();

        String userDn="";

        //DirContext context = null;
        LDAPConnection context = null;

        if(ldapBindComponent.isLdapAuthManagerExists()) {
            try {
                context = ldapBindComponent.mangerAuthConnect();

                List<String> dnUserList = ldapSearchComponent.searchUser(userName, context, "uid");
                if(dnUserList.isEmpty()){
                    LOGGER.error("Username doesn't exist");
                    return null;
                }
                userDn = dnUserList.get(0);
                context = ldapBindComponent.userConnectByUserDn(userDn,password);

            } catch (Exception e) {
                LOGGER.error("Can't connect using auth manager");
            }
        } else {
            try {
                context = ldapBindComponent.userConnectByUserName(userName, password);
                userDn = "uid="+userName+","+ldapAuthBase;
            } catch (Exception e) {
                LOGGER.error("Can't connect using userName");
            }
        }

        if(context == null){
            return null;
        }

        User user = getUser(userName);

        if(user == null) {
            String email;
            if (emailValidator.checkEmail(userName)) {
                email = userName;
            } else {

                if (ldapBindComponent.isLdapAuthManagerExists()){
                    try {
                        context = ldapBindComponent.mangerAuthConnect();
                    } catch (Exception e) {
                        LOGGER.error("Can't connect using auth manager");
                        return null;
                    }
                }

                String organization = ldapSearchComponent.getOrganization(userDn,context);
                LOGGER.info("o: " + organization);

                if (ldapBindComponent.isLdapEmailManagerExists()){
                    try {
                        context = ldapBindComponent.managerEmailConnect();
                    } catch (Exception e) {
                        LOGGER.error("Can't connect using email manager");
                        return null;
                    }
                }
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
