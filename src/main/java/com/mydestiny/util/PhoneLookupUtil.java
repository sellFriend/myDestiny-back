package com.mydestiny.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HexFormat;

/**
 * 전화번호 중복검사용 blind index.
 * 고정키 HMAC-SHA256 으로 같은 번호는 항상 같은 값을 내므로 SQL 동등비교(중복 조회)가 가능하다.
 * (subject_phone_hash 는 Argon2 salted 라 매번 달라 동등비교 불가)
 */
@Component
public class PhoneLookupUtil {

    private final byte[] keyBytes;
    private final PhoneHashUtil phoneHashUtil;

    public PhoneLookupUtil(@Value("${app.phone.lookup-key}") String base64Key,
                           PhoneHashUtil phoneHashUtil) {
        this.keyBytes = Base64.getDecoder().decode(base64Key);
        if (keyBytes.length < 16) {
            throw new IllegalStateException("app.phone.lookup-key must be at least 16 bytes (base64-encoded)");
        }
        this.phoneHashUtil = phoneHashUtil;
    }

    // 정규화(E.164) 후 HMAC-SHA256 → hex (64자)
    public String lookup(String phoneNumber) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(keyBytes, "HmacSHA256"));
            byte[] digest = mac.doFinal(
                    phoneHashUtil.normalize(phoneNumber).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (Exception e) {
            throw new IllegalStateException("전화번호 조회 해시 생성 실패", e);
        }
    }
}
