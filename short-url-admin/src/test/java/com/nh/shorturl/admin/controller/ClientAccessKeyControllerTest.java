package com.nh.shorturl.admin.controller;

import com.nh.shorturl.admin.dto.response.clientaccess.ClientAccessKeyResponse;
import com.nh.shorturl.admin.entity.ClientAccessKey;
import com.nh.shorturl.admin.service.clientaccess.ClientAccessKeyService;
import com.nh.shorturl.dto.request.clientaccess.ClientAccessKeyCreateRequest;
import com.nh.shorturl.dto.request.clientaccess.ClientAccessKeyUpdateRequest;
import com.nh.shorturl.dto.response.common.ResultEntity;
import com.nh.shorturl.type.ApiResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class ClientAccessKeyControllerTest {

    private final ClientAccessKeyService clientAccessKeyService = mock(ClientAccessKeyService.class);
    private final ClientAccessKeyController controller = new ClientAccessKeyController(clientAccessKeyService);

    @Test
    @DisplayName("클라이언트 키 목록을 조회한다")
    void list() {
        // given
        ClientAccessKey key1 = createClientAccessKey(1L, "key-1", "test-key-1", "team-1");
        ClientAccessKey key2 = createClientAccessKey(2L, "key-2", "test-key-2", "team-2");

        when(clientAccessKeyService.getKeys()).thenReturn(List.of(key1, key2));

        // when
        ResultEntity<?> result = controller.list();

        // then
        assertThat(result.getCode()).isEqualTo(ApiResult.SUCCESS.getCode());
        assertThat(result.getData()).isInstanceOf(List.class);

        @SuppressWarnings("unchecked")
        List<ClientAccessKeyResponse> responseList = (List<ClientAccessKeyResponse>) result.getData();
        assertThat(responseList).hasSize(2);
        assertThat(responseList.get(0).getName()).isEqualTo("key-1");
        assertThat(responseList.get(1).getName()).isEqualTo("key-2");

        verify(clientAccessKeyService).getKeys();
    }

    @Test
    @DisplayName("새로운 클라이언트 키를 발급한다")
    void issue() {
        // given
        ClientAccessKeyCreateRequest request = new ClientAccessKeyCreateRequest();
        request.setName("test-key");
        request.setIssuedBy("test-team");
        request.setDescription("Test client key");
        request.setExpiresAt(LocalDateTime.now().plusDays(30));

        ClientAccessKey createdKey = createClientAccessKey(1L, "test-key", "generated-uuid-key", "test-team");
        when(clientAccessKeyService.create(any(ClientAccessKeyCreateRequest.class))).thenReturn(createdKey);

        // when
        ResultEntity<?> result = controller.issue(request);

        // then
        assertThat(result.getCode()).isEqualTo(ApiResult.SUCCESS.getCode());
        assertThat(result.getData()).isInstanceOf(ClientAccessKeyResponse.class);

        ClientAccessKeyResponse response = (ClientAccessKeyResponse) result.getData();
        assertThat(response.getName()).isEqualTo("test-key");
        assertThat(response.getKeyValue()).isEqualTo("generated-uuid-key");
        assertThat(response.getIssuedBy()).isEqualTo("test-team");

        verify(clientAccessKeyService).create(request);
    }

    @Test
    @DisplayName("클라이언트 키 정보를 수정한다")
    void update() {
        // given
        Long keyId = 1L;
        ClientAccessKeyUpdateRequest request = new ClientAccessKeyUpdateRequest();
        request.setName("updated-key");
        request.setDescription("Updated description");
        request.setActive(false);
        request.setExpiresAt(LocalDateTime.now().plusDays(60));

        ClientAccessKey updatedKey = createClientAccessKey(keyId, "updated-key", "test-key-value", "team");
        updatedKey.setActive(false);
        updatedKey.setDescription("Updated description");

        when(clientAccessKeyService.update(eq(keyId), any(ClientAccessKeyUpdateRequest.class)))
                .thenReturn(updatedKey);

        // when
        ResultEntity<?> result = controller.update(keyId, request);

        // then
        assertThat(result.getCode()).isEqualTo(ApiResult.SUCCESS.getCode());
        assertThat(result.getData()).isInstanceOf(ClientAccessKeyResponse.class);

        ClientAccessKeyResponse response = (ClientAccessKeyResponse) result.getData();
        assertThat(response.getName()).isEqualTo("updated-key");
        assertThat(response.getActive()).isFalse();
        assertThat(response.getDescription()).isEqualTo("Updated description");

        verify(clientAccessKeyService).update(keyId, request);
    }

    @Test
    @DisplayName("존재하지 않는 클라이언트 키를 수정하려고 하면 NOT_FOUND를 반환한다")
    void updateNotFound() {
        // given
        Long keyId = 999L;
        ClientAccessKeyUpdateRequest request = new ClientAccessKeyUpdateRequest();
        request.setName("non-existent-key");

        when(clientAccessKeyService.update(eq(keyId), any(ClientAccessKeyUpdateRequest.class)))
                .thenThrow(new IllegalArgumentException("Client access key not found"));

        // when
        ResultEntity<?> result = controller.update(keyId, request);

        // then
        assertThat(result.getCode()).isEqualTo(ApiResult.NOT_FOUND.getCode());

        verify(clientAccessKeyService).update(keyId, request);
    }

    @Test
    @DisplayName("클라이언트 키를 삭제한다")
    void delete() {
        // given
        Long keyId = 1L;
        doNothing().when(clientAccessKeyService).delete(keyId);

        // when
        ResultEntity<?> result = controller.delete(keyId);

        // then
        assertThat(result.getCode()).isEqualTo(ApiResult.SUCCESS.getCode());
        assertThat(result.getData()).isNotNull();

        verify(clientAccessKeyService).delete(keyId);
    }

    @Test
    @DisplayName("존재하지 않는 클라이언트 키를 삭제하려고 하면 NOT_FOUND를 반환한다")
    void deleteNotFound() {
        // given
        Long keyId = 999L;
        doThrow(new IllegalArgumentException("Client access key not found"))
                .when(clientAccessKeyService).delete(keyId);

        // when
        ResultEntity<?> result = controller.delete(keyId);

        // then
        assertThat(result.getCode()).isEqualTo(ApiResult.NOT_FOUND.getCode());

        verify(clientAccessKeyService).delete(keyId);
    }

    private ClientAccessKey createClientAccessKey(Long id, String name, String keyValue, String issuedBy) {
        return ClientAccessKey.builder()
                .id(id)
                .name(name)
                .keyValue(keyValue)
                .issuedBy(issuedBy)
                .description("Test description")
                .active(true)
                .deleted(false)
                .build();
    }
}
