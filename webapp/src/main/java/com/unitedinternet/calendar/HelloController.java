package com.unitedinternet.calendar;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.ldap.userdetails.Person;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;

@RestController
@RequestMapping("/test")
public class HelloController {
    @GetMapping("/hello")
    public String hello(Authentication authentication) {
        String name = "anonym";
        if(authentication!=null){
            name = authentication.getName();
        }
        return "Hello " +
                name +
                "!";
    }

    @GetMapping("/friendly")
    public String hello(@AuthenticationPrincipal Person person) {
        return "Hello " +
                person.getUsername() + "\n" +
                person.getGivenName() + "\n" +
                person.getDn() + "\n" +
                Arrays.toString(person.getCn()) + "\n" +
                person.getSn() +
                "!";
    }

    @GetMapping("/test")
    public String test(){
        return "<h2>Test</h2><br/>It is working...";
    }
}