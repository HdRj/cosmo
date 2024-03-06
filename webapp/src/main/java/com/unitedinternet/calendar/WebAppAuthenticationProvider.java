package com.unitedinternet.calendar;

import org.apache.commons.codec.binary.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.unitedinternet.cosmo.CosmoException;
import org.unitedinternet.cosmo.acegisecurity.userdetails.CosmoUserDetails;
import org.unitedinternet.cosmo.model.EntityFactory;
import org.unitedinternet.cosmo.model.User;
import org.unitedinternet.cosmo.service.UserService;

import java.nio.charset.Charset;
import java.security.MessageDigest;

/**
 * Demo <code>AuthenticationProvider</code> that allows all requests that have a username and a password and performs
 * provisioning in case it is needed. This is for testing purposes only.
 * 
 * @author daniel grigore
 *
 */
@Primary
@Component
@Transactional
public class WebAppAuthenticationProvider implements AuthenticationProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(WebAppAuthenticationProvider.class);

    private final UserService userService;
    private final EntityFactory entityFactory;

    public WebAppAuthenticationProvider(UserService userService, EntityFactory entityFactory) {
        super();
        this.userService = userService;
        this.entityFactory = entityFactory;
    }

//    @Override
//    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
//        String userName = authentication.getName();
//        LOGGER.info("[AUTH] About to authenticate user: {}", userName);
//        User user = this.createUserIfNotPresent(authentication);
//        return new UsernamePasswordAuthenticationToken(new CosmoUserDetails(user), authentication.getCredentials(),
//                authentication.getAuthorities());
//    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        //String username = authentication.getName();
        String password = authentication.getCredentials().toString();

        User user = authenticateUser(authentication);
        if(user == null){
            throw new UsernameNotFoundException("User not found");
        }
        String digestAlgorithm = userService.getDigestAlgorithm();
        String passwordHash;
        try {
            passwordHash = new String(Hex.encodeHex(
                    MessageDigest.getInstance(digestAlgorithm).digest(password.getBytes(Charset.forName("UTF-8")))));
        }catch (Exception e){
            throw new CosmoException("cannot get digest for algorithm " + digestAlgorithm, e);
        }

        if (!passwordHash.equals(user.getPassword())) {
            throw new UsernameNotFoundException("Invalid password");
        }

        UserDetails userDetails = new CosmoUserDetails(user);

        return new UsernamePasswordAuthenticationToken(userDetails, password, userDetails.getAuthorities());
    }

    private User createUserIfNotPresent(Authentication authentication) {
        String userName = authentication.getName();
        User user = this.userService.getUser(userName);
        if (user != null) {
            LOGGER.info("[AUTH] Found user with email address: {}", user.getEmail());
            return user;
        }
        LOGGER.info("[AUTH] No user found for email address: {}. Creating one...", userName);
        user = this.entityFactory.createUser();
        user.setUsername(userName);
        user.setEmail(userName);
        user.setFirstName(userName);
        user.setLastName(userName);
        user.setPassword((authentication.getCredentials() != null) ? (String) authentication.getCredentials() : "NOT_NULL");
        user = this.userService.createUser(user);
        return user;
    }

    private User authenticateUser(Authentication authentication){
        String userUid = authentication.getName();

        User user = this.userService.getUserByUid(userUid);
        if (user != null) {
            LOGGER.info("[AUTH] Found user with uid: {}", user.getUid());
            return user;
        } else {
            LOGGER.info("[AUTH] Not found user with uid: {}", userUid);
            return null;
        }
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
    }

}