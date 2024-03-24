package com.unitedinternet.calendar.utils;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.Random;

@Component
public class RandomStringGenerator {

    private String passwordSymbols="abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890";

    public String generatePassword(int length){
        Random random = new SecureRandom();
        char[] symbolsCharArray = passwordSymbols.toCharArray();
        char[] buffer = new char[length];
        for (int idx = 0; idx < buffer.length; ++idx) {
            buffer[idx] = symbolsCharArray[random.nextInt(symbolsCharArray.length)];
        }
        return new String(buffer);
    }


}
