package com.nh.shorturl.admin.service.clientaccess;

import com.nh.shorturl.admin.entity.ClientAccessKey;
import com.nh.shorturl.admin.repository.ClientAccessKeyRepository;
import com.nh.shorturl.dto.request.clientaccess.ClientAccessKeyCreateRequest;
import com.nh.shorturl.dto.request.clientaccess.ClientAccessKeyUpdateRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ClientAccessKeyServiceTest {

    private final ClientAccessKeyRepository repository = mock(ClientAccessKeyRepository.class);
    private final ClientAccessKeyService service = new ClientAccessKeyServiceImpl(repository);

    @Test
    @DisplayName("클라이언트 키를 생성하면 UUID 기반 키가 생성된다")
    void create() {
        // given
        ClientAccessKeyCreateRequest request = new ClientAccessKeyCreateRequest();
        request.setName("test-key");
        request.setIssuedBy("test-team");
        request.setDescription("Test description");
        request.setExpiresAt(LocalDateTime.now().plusDays(30));

        ClientAccessKey savedKey = ClientAccessKey.builder()
                .id(1L)
                .name("test-key")
                .keyValue("generated-uuid-key")
                .issuedBy("test-team")
                .description("Test description")
                .active(true)
                .build();

        when(repository.save(any(ClientAccessKey.class))).thenReturn(savedKey);

        // when
        ClientAccessKey result = service.create(request);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("test-key");
        assertThat(result.getIssuedBy()).isEqualTo("test-team");
        assertThat(result.getActive()).isTrue();

        verify(repository).save(any(ClientAccessKey.class));
    }

    @Test
    @DisplayName("이름이 없으면 기본 이름으로 클라이언트 키를 생성한다")
    void createWithDefaultName() {
        // given
        ClientAccessKeyCreateRequest request = new ClientAccessKeyCreateRequest();
        request.setIssuedBy("test-team");

        ClientAccessKey savedKey = ClientAccessKey.builder()
                .id(1L)
                .name("Client Access Key")
                .keyValue("generated-uuid-key")
                .issuedBy("test-team")
                .active(true)
                .build();

        when(repository.save(any(ClientAccessKey.class))).thenReturn(savedKey);

        // when
        ClientAccessKey result = service.create(request);

        // then
        assertThat(result.getName()).isEqualTo("Client Access Key");

        verify(repository).save(any(ClientAccessKey.class));
    }

    @Test
    @DisplayName("클라이언트 키 정보를 수정한다")
    void update() {
        // given
        Long keyId = 1L;
        ClientAccessKeyUpdateRequest request = new ClientAccessKeyUpdateRequest();
        request.setName("updated-name");
        request.setDescription("Updated description");
        request.setActive(false);
        request.setExpiresAt(LocalDateTime.now().plusDays(60));

        ClientAccessKey existingKey = ClientAccessKey.builder()
                .id(keyId)
                .name("original-name")
                .keyValue("test-key")
                .active(true)
                .build();

        when(repository.findById(keyId)).thenReturn(Optional.of(existingKey));

        // when
        ClientAccessKey result = service.update(keyId, request);

        // then
        assertThat(result.getName()).isEqualTo("updated-name");
        assertThat(result.getDescription()).isEqualTo("Updated description");
        assertThat(result.getActive()).isFalse();

        verify(repository).findById(keyId);
    }

    @Test
    @DisplayName("존재하지 않는 키를 수정하려고 하면 예외가 발생한다")
    void updateNotFound() {
        // given
        Long keyId = 999L;
        ClientAccessKeyUpdateRequest request = new ClientAccessKeyUpdateRequest();
        request.setName("updated-name");

        when(repository.findById(keyId)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> service.update(keyId, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Client access key not found");

        verify(repository).findById(keyId);
    }

    @Test
    @DisplayName("클라이언트 키를 삭제한다")
    void delete() {
        // given
        Long keyId = 1L;
        ClientAccessKey existingKey = ClientAccessKey.builder()
                .id(keyId)
                .name("test-key")
                .keyValue("test-value")
                .build();

        when(repository.findById(keyId)).thenReturn(Optional.of(existingKey));
        doNothing().when(repository).delete(existingKey);

        // when
        service.delete(keyId);

        // then
        verify(repository).findById(keyId);
        verify(repository).delete(existingKey);
    }

    @Test
    @DisplayName("존재하지 않는 키를 삭제하려고 하면 예외가 발생한다")
    void deleteNotFound() {
        // given
        Long keyId = 999L;
        when(repository.findById(keyId)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> service.delete(keyId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Client access key not found");

        verify(repository).findById(keyId);
        verify(repository, never()).delete(any());
    }

    @Test
    @DisplayName("삭제되지 않은 모든 클라이언트 키를 조회한다")
    void getKeys() {
        // given
        ClientAccessKey key1 = ClientAccessKey.builder()
                .id(1L)
                .name("key-1")
                .keyValue("value-1")
                .build();
        ClientAccessKey key2 = ClientAccessKey.builder()
                .id(2L)
                .name("key-2")
                .keyValue("value-2")
                .build();

        when(repository.findAllByDeletedFalseOrderByCreatedAtDesc()).thenReturn(List.of(key1, key2));

        // when
        List<ClientAccessKey> result = service.getKeys();

        // then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getName()).isEqualTo("key-1");
        assertThat(result.get(1).getName()).isEqualTo("key-2");

        verify(repository).findAllByDeletedFalseOrderByCreatedAtDesc();
    }

    @Test
    @DisplayName("유효한 활성 키를 검증하고 lastUsedAt을 업데이트한다")
    void validateActiveKey() {
        // given
        String keyValue = "valid-key";
        ClientAccessKey activeKey = ClientAccessKey.builder()
                .id(1L)
                .keyValue(keyValue)
                .active(true)
                .expiresAt(LocalDateTime.now().plusDays(30))
                .build();

        when(repository.findByKeyValueAndDeletedFalse(keyValue)).thenReturn(Optional.of(activeKey));

        // when
        ClientAccessKey result = service.validateActiveKey(keyValue);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getLastUsedAt()).isNotNull();

        verify(repository).findByKeyValueAndDeletedFalse(keyValue);
    }

    @Test
    @DisplayName("존재하지 않는 키로 검증하면 예외가 발생한다")
    void validateNonExistentKey() {
        // given
        String keyValue = "non-existent-key";
        when(repository.findByKeyValueAndDeletedFalse(keyValue)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> service.validateActiveKey(keyValue))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid client access key");

        verify(repository).findByKeyValueAndDeletedFalse(keyValue);
    }

    @Test
    @DisplayName("비활성 키로 검증하면 예외가 발생한다")
    void validateInactiveKey() {
        // given
        String keyValue = "inactive-key";
        ClientAccessKey inactiveKey = ClientAccessKey.builder()
                .id(1L)
                .keyValue(keyValue)
                .active(false)
                .build();

        when(repository.findByKeyValueAndDeletedFalse(keyValue)).thenReturn(Optional.of(inactiveKey));

        // when & then
        assertThatThrownBy(() -> service.validateActiveKey(keyValue))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Client access key is inactive");

        verify(repository).findByKeyValueAndDeletedFalse(keyValue);
    }

    @Test
    @DisplayName("만료된 키로 검증하면 예외가 발생한다")
    void validateExpiredKey() {
        // given
        String keyValue = "expired-key";
        ClientAccessKey expiredKey = ClientAccessKey.builder()
                .id(1L)
                .keyValue(keyValue)
                .active(true)
                .expiresAt(LocalDateTime.now().minusDays(1))
                .build();

        when(repository.findByKeyValueAndDeletedFalse(keyValue)).thenReturn(Optional.of(expiredKey));

        // when & then
        assertThatThrownBy(() -> service.validateActiveKey(keyValue))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Client access key expired");

        verify(repository).findByKeyValueAndDeletedFalse(keyValue);
    }

    @Test
    @DisplayName("만료일이 없는 활성 키는 정상적으로 검증된다")
    void validateActiveKeyWithoutExpiration() {
        // given
        String keyValue = "no-expiry-key";
        ClientAccessKey keyWithoutExpiry = ClientAccessKey.builder()
                .id(1L)
                .keyValue(keyValue)
                .active(true)
                .expiresAt(null)
                .build();

        when(repository.findByKeyValueAndDeletedFalse(keyValue)).thenReturn(Optional.of(keyWithoutExpiry));

        // when
        ClientAccessKey result = service.validateActiveKey(keyValue);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getExpiresAt()).isNull();
        assertThat(result.getLastUsedAt()).isNotNull();

        verify(repository).findByKeyValueAndDeletedFalse(keyValue);
    }
}
