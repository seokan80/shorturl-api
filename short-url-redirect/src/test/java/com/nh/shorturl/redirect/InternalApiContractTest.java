package com.nh.shorturl.redirect;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nh.shorturl.dto.response.common.ResultEntity;
import com.nh.shorturl.dto.response.control.RedirectionConfigResponse;
import com.nh.shorturl.dto.response.shorturl.ShortUrlResponse;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * admin 내부 API 응답 ↔ redirect 역직렬화 계약을 잠그는 테스트.
 *
 * <p>admin {@code InternalApiController} 는 응답을 {@link ResultEntity} 로 감싸 내려주고,
 * redirect {@code ShortUrlCacheSyncer}/{@code RedirectionConfigStore} 는 이를
 * {@code ResultEntity<List<ShortUrlResponse>>} / {@code ResultEntity<RedirectionConfigResponse>} 로 역직렬화한다.
 * 두 가지가 깨지면(봉투 누락 또는 DTO 에 @Setter 부재로 필드 미주입) 캐시·설정이 비어 통합이 무력화되므로
 * 직렬화→역직렬화 라운드트립으로 필드가 실제 채워지는지 검증한다.
 */
class InternalApiContractTest {

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Test
    void shortUrlListEnvelope_roundTrips_withPopulatedFields() throws Exception {
        // admin 이 보내는 형태: ResultEntity.ok(List.of(...))
        ShortUrlResponse item = ShortUrlResponse.builder()
                .id(1L)
                .shortKey("Ab3dEf12")
                .shortUrl("http://localhost:18081/s/Ab3dEf12")
                .longUrl("https://example.com/landing")
                .expiredAt("2026-05-30T23:59:59")
                .deleted(false)
                .build();
        String json = objectMapper.writeValueAsString(ResultEntity.ok(List.of(item)));

        // redirect 가 파싱하는 형태
        ResultEntity<List<ShortUrlResponse>> parsed =
                objectMapper.readValue(json, new TypeReference<ResultEntity<List<ShortUrlResponse>>>() {});

        assertThat(parsed.getCode()).isEqualTo("0000");
        assertThat(parsed.getData()).hasSize(1);
        ShortUrlResponse out = parsed.getData().get(0);
        assertThat(out.getShortKey()).isEqualTo("Ab3dEf12");
        assertThat(out.getLongUrl()).isEqualTo("https://example.com/landing");
        assertThat(out.getExpiredAt()).isEqualTo("2026-05-30T23:59:59");
        assertThat(out.getDeleted()).isFalse();
    }

    @Test
    void redirectionConfigEnvelope_roundTrips_withPopulatedFields() throws Exception {
        RedirectionConfigResponse config = RedirectionConfigResponse.builder()
                .fallbackUrl("https://www.nhbank.com")
                .defaultHost("https://www.nhbank.com")
                .showErrorPage(true)
                .trackingFields("utm_source,utm_medium,utm_campaign")
                .build();
        String json = objectMapper.writeValueAsString(ResultEntity.ok(config));

        ResultEntity<RedirectionConfigResponse> parsed =
                objectMapper.readValue(json, new TypeReference<ResultEntity<RedirectionConfigResponse>>() {});

        assertThat(parsed.getData()).isNotNull();
        assertThat(parsed.getData().getFallbackUrl()).isEqualTo("https://www.nhbank.com");
        assertThat(parsed.getData().getDefaultHost()).isEqualTo("https://www.nhbank.com");
        assertThat(parsed.getData().getShowErrorPage()).isTrue();
        assertThat(parsed.getData().getTrackingFields()).isEqualTo("utm_source,utm_medium,utm_campaign");
    }
}
