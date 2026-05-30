package com.mydestiny.util;

import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class PhoneHashUtil {

    private final Argon2PasswordEncoder encoder =
            Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8();

    public String hash(String phoneNumber) {
        return encoder.encode(normalize(phoneNumber));
    }

    public boolean matches(String phoneNumber, String storedHash) {
        return encoder.matches(normalize(phoneNumber), storedHash);
    }

    // 010-1234-5678 또는 01012345678 → +821012345678 (E.164)
    public String normalize(String phone) {
        String digits = phone.replaceAll("[^0-9]", "");
        if (digits.startsWith("0")) {
            return "+82" + digits.substring(1);
        }
        return digits;
    }
}
