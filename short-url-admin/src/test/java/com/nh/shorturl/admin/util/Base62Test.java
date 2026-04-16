package com.nh.shorturl.admin.util;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class Base62Test {

    private static final Pattern BASE62_CHARSET = Pattern.compile("^[0-9A-Za-z]+$");

    @Test
    void encodeUUID_returnsFixedLengthOfEight() {
        String encoded = Base62.encodeUUID(UUID.fromString("a1b2c3d4-e5f6-7890-1234-56789abcdef0"));

        assertThat(encoded).hasSize(8);
    }

    @Test
    void encodeUUID_containsOnlyBase62Alphabet() {
        for (int i = 0; i < 100; i++) {
            String encoded = Base62.encodeUUID(UUID.randomUUID());

            assertThat(encoded).hasSize(8);
            assertThat(BASE62_CHARSET.matcher(encoded).matches())
                    .as("encoded value %s must only contain [0-9A-Za-z]", encoded)
                    .isTrue();
        }
    }

    @Test
    void encodeUUID_isDeterministicForSameInput() {
        UUID uuid = UUID.fromString("11111111-2222-3333-4444-555555555555");

        String first = Base62.encodeUUID(uuid);
        String second = Base62.encodeUUID(uuid);

        assertThat(first).isEqualTo(second);
    }

    @Test
    void encodeUUID_producesDistinctValuesForDifferentInputs() {
        Set<String> seen = new HashSet<>();
        int sampleSize = 10_000;

        for (int i = 0; i < sampleSize; i++) {
            seen.add(Base62.encodeUUID(UUID.randomUUID()));
        }

        // 8자 Base62 는 62^8 ≈ 2.18e14 공간이므로 1만개 샘플에서는 충돌 확률 무시 가능.
        // 충돌이 발생하면 인코딩 로직에 결함이 있는 것으로 본다.
        assertThat(seen).as("collision in 10k samples indicates encoding bias").hasSize(sampleSize);
    }

    @Test
    void encodeUUID_rejectsZeroUUID() {
        // UUID(0,0) 은 BigInteger ZERO 가 되어 substring(0,8) 에서 StringIndexOutOfBoundsException 발생.
        // 현재 구현의 알려진 엣지 케이스를 문서화한다. 운영상 randomUUID() 만 사용하므로 실제 위험은 없음.
        UUID zero = new UUID(0L, 0L);

        assertThatThrownBy(() -> Base62.encodeUUID(zero))
                .isInstanceOf(StringIndexOutOfBoundsException.class);
    }
}
