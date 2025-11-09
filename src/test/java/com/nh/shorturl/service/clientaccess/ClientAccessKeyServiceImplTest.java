package com.nh.shorturl.service.clientaccess;

import com.nh.shorturl.dto.request.clientaccess.ClientAccessKeyCreateRequest;
import com.nh.shorturl.dto.request.clientaccess.ClientAccessKeyUpdateRequest;
import com.nh.shorturl.entity.ClientAccessKey;
import com.nh.shorturl.repository.ClientAccessKeyRepository;
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
class ClientAccessKeyServiceImplTest {

    @Mock
    private ClientAccessKeyRepository repository;

    @InjectMocks
    private ClientAccessKeyServiceImpl service;

    @Test
    @DisplayName("클라이언트 키 생성 시 랜덤 키 값이 저장된다")
    void createGeneratesKeyValue() {
        ClientAccessKey saved = ClientAccessKey.builder()
            .id(1L)
            .name("demo")
            .keyValue("abc123")
            .build();
        given(repository.save(any(ClientAccessKey.class))).willReturn(saved);

        ClientAccessKeyCreateRequest request = new ClientAccessKeyCreateRequest();
        request.setName("demo");
        request.setIssuedBy("tester");

        ClientAccessKey result = service.create(request);

        assertThat(result).isEqualTo(saved);
        verify(repository).save(any(ClientAccessKey.class));
    }

    @Test
    @DisplayName("존재하지 않는 키 수정 시 예외 발생")
    void updateThrowsWhenMissing() {
        given(repository.findById(99L)).willReturn(Optional.empty());

        ClientAccessKeyUpdateRequest request = new ClientAccessKeyUpdateRequest();
        request.setName("new");

        assertThatThrownBy(() -> service.update(99L, request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("not found");
    }

    @Test
    @DisplayName("활성화된 키만 검증에 성공한다")
    void validateRequiresActiveKey() {
        ClientAccessKey key = ClientAccessKey.builder()
            .id(10L)
            .name("demo")
            .keyValue("value")
            .active(true)
            .build();
        given(repository.findByKeyValueAndDeletedFalse("value")).willReturn(Optional.of(key));

        ClientAccessKey validated = service.validateActiveKey("value");

        assertThat(validated).isEqualTo(key);
        assertThat(validated.getLastUsedAt()).isNotNull();
    }

    @Test
    @DisplayName("비활성 키 검증 시 예외")
    void validateInactiveThrows() {
        ClientAccessKey key = ClientAccessKey.builder()
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
        ClientAccessKey key = ClientAccessKey.builder()
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
