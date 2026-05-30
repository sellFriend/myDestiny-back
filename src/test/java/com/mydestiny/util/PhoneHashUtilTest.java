package com.mydestiny.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PhoneHashUtilTest {

    private final PhoneHashUtil util = new PhoneHashUtil();

    @Test
    void 같은_번호는_matches가_true() {
        String hash = util.hash("010-1234-5678");
        assertThat(util.matches("010-1234-5678", hash)).isTrue();
    }

    @Test
    void 다른_번호는_matches가_false() {
        String hash = util.hash("010-1234-5678");
        assertThat(util.matches("010-9999-9999", hash)).isFalse();
    }

    @Test
    void 하이픈_유무_관계없이_같은번호_인식() {
        String hash = util.hash("01012345678");
        assertThat(util.matches("010-1234-5678", hash)).isTrue();
    }

    @Test
    void normalize_E164_변환() {
        assertThat(util.normalize("010-1234-5678")).isEqualTo("+821012345678");
        assertThat(util.normalize("01012345678")).isEqualTo("+821012345678");
    }

    @Test
    void hash는_매번_다른_값_반환_brute_force_방지() {
        String hash1 = util.hash("010-1234-5678");
        String hash2 = util.hash("010-1234-5678");
        // Argon2 random salt → 같은 입력이어도 다른 해시 (matches()로만 비교)
        assertThat(hash1).isNotEqualTo(hash2);
    }
}
