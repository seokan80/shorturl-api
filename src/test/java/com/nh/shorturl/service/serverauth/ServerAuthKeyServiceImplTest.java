package com.nh.shorturl.service.serverauth;

import com.nh.shorturl.dto.request.serverauth.ServerAuthKeyCreateRequest;
import com.nh.shorturl.dto.request.serverauth.ServerAuthKeyUpdateRequest;
import com.nh.shorturl.entity.ServerAuthKey;
import com.nh.shorturl.repository.ServerAuthKeyRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ServerAuthKeyServiceImplTest {

    @Mock
    private ServerAuthKeyRepository repository;

    @InjectMocks
    private ServerAuthKeyServiceImpl service;

    @Test
    @DisplayName("서버 인증 키 생성 시 랜덤 키 값이 저장된다")
    void createGeneratesKeyValue() {
        ServerAuthKey saved = ServerAuthKey.builder()
            .id(1L)
            .name("demo")
            .keyValue("abc123")
            .build();
        given(repository.save(any(ServerAuthKey.class))).willReturn(saved);

        ServerAuthKeyCreateRequest request = new ServerAuthKeyCreateRequest();
        request.setName("demo");
        request.setIssuedBy("tester");

        ServerAuthKey result = service.create(request);

        assertThat(result).isEqualTo(saved);
        verify(repository).save(any(ServerAuthKey.class));
    }

    @Test
    @DisplayName("존재하지 않는 키 수정 시 예외 발생")
    void updateThrowsWhenMissing() {
        given(repository.findById(99L)).willReturn(Optional.empty());

        ServerAuthKeyUpdateRequest request = new ServerAuthKeyUpdateRequest();
        request.setName("new");

        assertThatThrownBy(() -> service.update(99L, request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("not found");
    }

    @Test
    @DisplayName("활성화된 키만 검증에 성공한다")
    void validateRequiresActiveKey() {
        ServerAuthKey key = ServerAuthKey.builder()
            .id(10L)
            .name("demo")
            .keyValue("value")
            .active(true)
            .build();
        given(repository.findByKeyValueAndDeletedFalse("value")).willReturn(Optional.of(key));

        ServerAuthKey validated = service.validateActiveKey("value");

        assertThat(validated).isEqualTo(key);
        assertThat(validated.getLastUsedAt()).isNotNull();
    }

    @Test
    @DisplayName("비활성 키 검증 시 예외")
    void validateInactiveThrows() {
        ServerAuthKey key = ServerAuthKey.builder()
            .id(10L)
            .name("demo")
            .keyValue("value")
            .active(false)
            .build();
        given(repository.findByKeyValueAndDeletedFalse("value")).willReturn(Optional.of(key));

        assertThatThrownBy(() -> service.validateActiveKey("value"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("inactive");
    }

    @Test
    @DisplayName("만료된 키 검증 시 예외")
    void validateExpiredThrows() {
        ServerAuthKey key = ServerAuthKey.builder()
            .id(10L)
            .name("demo")
            .keyValue("value")
            .active(true)
            .expiresAt(LocalDateTime.now().minusDays(1))
            .build();
        given(repository.findByKeyValueAndDeletedFalse("value")).willReturn(Optional.of(key));

        assertThatThrownBy(() -> service.validateActiveKey("value"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("expired");
    }
}
