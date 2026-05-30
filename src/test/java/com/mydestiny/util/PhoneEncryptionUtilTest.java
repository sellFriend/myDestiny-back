package com.mydestiny.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PhoneEncryptionUtilTest {

    // 테스트용 32바이트 키 (application.properties 동일값)
    private final PhoneEncryptionUtil util =
            new PhoneEncryptionUtil("MDEyMzQ1Njc4OTAxMjM0NTY3ODkwMTIzNDU2Nzg5MDE=");

    @Test
    void 암호화_후_복호화하면_원문_복원() {
        String plain = "01012345678";
        String encrypted = util.encrypt(plain);
        assertThat(util.decrypt(encrypted)).isEqualTo(plain);
    }

    @Test
    void 같은_입력도_매번_다른_암호문_생성_IV_랜덤() {
        String enc1 = util.encrypt("01012345678");
        String enc2 = util.encrypt("01012345678");
        assertThat(enc1).isNotEqualTo(enc2);
    }

    @Test
    void 암호문은_plaintext_포함하지_않음() {
        String encrypted = util.encrypt("01012345678");
        assertThat(encrypted).doesNotContain("01012345678");
    }

    @Test
    void mask_마지막4자리만_표시() {
        assertThat(util.mask("01012345678")).isEqualTo("010-****-5678");
        assertThat(util.mask("010-1234-5678")).isEqualTo("010-****-5678");
    }

    @Test
    void 잘못된_키_길이는_초기화_실패() {
        assertThatThrownBy(() -> new PhoneEncryptionUtil("dG9vc2hvcnQ=")) // "tooshort" = 8 bytes
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("32 bytes");
    }
}
