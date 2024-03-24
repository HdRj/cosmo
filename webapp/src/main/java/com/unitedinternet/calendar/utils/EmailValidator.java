package com.unitedinternet.calendar.utils;

import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class EmailValidator {
    private final Pattern REGEX_PATTERN = Pattern.compile("^[A-Z]+([A-Z0-9.]*[A-Z0-9]+)*@([A-Z0-9-]+\\.)+[A-Z]{2,}$", Pattern.CASE_INSENSITIVE);

    public boolean checkEmail(String text){
        if(text==null){
            return false;
        }
        Matcher matcher = REGEX_PATTERN.matcher(text);
        return matcher.matches();
    }
}
