package com.microchip.lambda_core.service.util;

import java.security.SecureRandom;
import org.springframework.stereotype.Component;

@Component
public class ShareCodeGenerator {

    private static final String ALPHABET = "23456789ABCDEFGHJKMNPQRSTUVWXYZ";
    private static final int LENGTH = 6;

    public String generate() {
        StringBuilder code = new StringBuilder(LENGTH);
        for (int i = 0; i < LENGTH; i++) {
            code.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
        }
        return code.toString();
    }

    private final SecureRandom random = new SecureRandom();
}
