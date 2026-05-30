package com.mydestiny.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

public final class TokenUtil {

    private TokenUtil() {}

    // 256-bit 랜덤 토큰 생성 (URL-safe Base64, 패딩 없음)
    public static String generateRawToken() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    // SHA-256 hex — DB 저장용 (raw token은 URL에만 노출)
    public static String sha256(String input) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256")
                    .digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    // 6자리 OTP 생성
    public static String generateOtp() {
        return String.format("%06d", new SecureRandom().nextInt(1_000_000));
    }
}
