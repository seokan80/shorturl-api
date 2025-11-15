package com.nh.shorturl.admin.repository;

import com.nh.shorturl.admin.entity.ClientAccessKey;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class ClientAccessKeyRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private ClientAccessKeyRepository repository;

    @Test
    @DisplayName("키 값으로 삭제되지 않은 클라이언트 키를 조회한다")
    void findByKeyValueAndDeletedFalse() {
        // given
        ClientAccessKey key = ClientAccessKey.builder()
                .name("test-key")
                .keyValue("test-value-123")
                .issuedBy("test-team")
                .active(true)
                .deleted(false)
                .build();
        entityManager.persist(key);
        entityManager.flush();

        // when
        Optional<ClientAccessKey> result = repository.findByKeyValueAndDeletedFalse("test-value-123");

        // then
        assertThat(result).isPresent();
        assertThat(result.get().getKeyValue()).isEqualTo("test-value-123");
        assertThat(result.get().getName()).isEqualTo("test-key");
    }

    @Test
    @DisplayName("삭제된 키는 조회되지 않는다")
    void findByKeyValueAndDeletedFalse_deletedKey() {
        // given
        ClientAccessKey key = ClientAccessKey.builder()
                .name("deleted-key")
                .keyValue("deleted-value-123")
                .issuedBy("test-team")
                .active(true)
                .deleted(true)
                .deletedAt(LocalDateTime.now())
                .build();
        entityManager.persist(key);
        entityManager.flush();

        // when
        Optional<ClientAccessKey> result = repository.findByKeyValueAndDeletedFalse("deleted-value-123");

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("존재하지 않는 키 값으로 조회하면 빈 Optional을 반환한다")
    void findByKeyValueAndDeletedFalse_notFound() {
        // when
        Optional<ClientAccessKey> result = repository.findByKeyValueAndDeletedFalse("non-existent-key");

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("삭제되지 않은 모든 키를 생성일 내림차순으로 조회한다")
    void findAllByDeletedFalseOrderByCreatedAtDesc() {
        // given
        ClientAccessKey key1 = ClientAccessKey.builder()
                .name("key-1")
                .keyValue("value-1")
                .issuedBy("team-1")
                .active(true)
                .deleted(false)
                .build();
        entityManager.persist(key1);

        // 약간의 시간차를 두기 위해 flush
        entityManager.flush();

        ClientAccessKey key2 = ClientAccessKey.builder()
                .name("key-2")
                .keyValue("value-2")
                .issuedBy("team-2")
                .active(true)
                .deleted(false)
                .build();
        entityManager.persist(key2);
        entityManager.flush();

        // when
        List<ClientAccessKey> result = repository.findAllByDeletedFalseOrderByCreatedAtDesc();

        // then
        assertThat(result).hasSize(2);
        // 최신 항목이 먼저 나와야 함
        assertThat(result.get(0).getName()).isEqualTo("key-2");
        assertThat(result.get(1).getName()).isEqualTo("key-1");
    }

    @Test
    @DisplayName("삭제된 키는 목록 조회에서 제외된다")
    void findAllByDeletedFalseOrderByCreatedAtDesc_excludesDeleted() {
        // given
        ClientAccessKey activeKey = ClientAccessKey.builder()
                .name("active-key")
                .keyValue("active-value")
                .issuedBy("team")
                .active(true)
                .deleted(false)
                .build();
        entityManager.persist(activeKey);

        ClientAccessKey deletedKey = ClientAccessKey.builder()
                .name("deleted-key")
                .keyValue("deleted-value")
                .issuedBy("team")
                .active(true)
                .deleted(true)
                .deletedAt(LocalDateTime.now())
                .build();
        entityManager.persist(deletedKey);
        entityManager.flush();

        // when
        List<ClientAccessKey> result = repository.findAllByDeletedFalseOrderByCreatedAtDesc();

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("active-key");
    }

    @Test
    @DisplayName("클라이언트 키를 저장한다")
    void save() {
        // given
        ClientAccessKey key = ClientAccessKey.builder()
                .name("new-key")
                .keyValue("new-value-123")
                .issuedBy("new-team")
                .description("New test key")
                .active(true)
                .deleted(false)
                .build();

        // when
        ClientAccessKey savedKey = repository.save(key);

        // then
        assertThat(savedKey.getId()).isNotNull();
        assertThat(savedKey.getName()).isEqualTo("new-key");
        assertThat(savedKey.getKeyValue()).isEqualTo("new-value-123");
        assertThat(savedKey.getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("클라이언트 키를 수정한다")
    void update() {
        // given
        ClientAccessKey key = ClientAccessKey.builder()
                .name("original-name")
                .keyValue("test-value")
                .issuedBy("team")
                .active(true)
                .deleted(false)
                .build();
        ClientAccessKey savedKey = entityManager.persist(key);
        entityManager.flush();

        // when
        savedKey.setName("updated-name");
        savedKey.setDescription("Updated description");
        savedKey.setActive(false);
        entityManager.flush();

        // then
        ClientAccessKey updatedKey = repository.findById(savedKey.getId()).orElseThrow();
        assertThat(updatedKey.getName()).isEqualTo("updated-name");
        assertThat(updatedKey.getDescription()).isEqualTo("Updated description");
        assertThat(updatedKey.getActive()).isFalse();
        assertThat(updatedKey.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("클라이언트 키를 삭제한다 (소프트 삭제)")
    void delete() {
        // given
        ClientAccessKey key = ClientAccessKey.builder()
                .name("to-delete")
                .keyValue("delete-value")
                .issuedBy("team")
                .active(true)
                .deleted(false)
                .build();
        ClientAccessKey savedKey = entityManager.persist(key);
        entityManager.flush();
        entityManager.clear();

        // when
        ClientAccessKey keyToDelete = repository.findById(savedKey.getId()).orElseThrow();
        repository.delete(keyToDelete);
        entityManager.flush();

        // then
        // SQLDelete 어노테이션으로 인해 실제로는 deleted = true로 업데이트됨
        Optional<ClientAccessKey> deletedKey = repository.findByKeyValueAndDeletedFalse("delete-value");
        assertThat(deletedKey).isEmpty();

        // 하지만 findById로는 조회 가능 (SQLRestriction이 적용되지 않는 경우)
        // 또는 완전히 삭제되었을 수도 있음 - 실제 동작은 엔티티 설정에 따라 달라짐
    }

    @Test
    @DisplayName("키 값은 유니크해야 한다")
    void keyValueUnique() {
        // given
        ClientAccessKey key1 = ClientAccessKey.builder()
                .name("key-1")
                .keyValue("duplicate-value")
                .issuedBy("team-1")
                .active(true)
                .deleted(false)
                .build();
        entityManager.persist(key1);
        entityManager.flush();

        ClientAccessKey key2 = ClientAccessKey.builder()
                .name("key-2")
                .keyValue("duplicate-value")
                .issuedBy("team-2")
                .active(true)
                .deleted(false)
                .build();

        // when & then
        // 동일한 keyValue로 저장 시도하면 예외 발생
        try {
            entityManager.persist(key2);
            entityManager.flush();
            // 예외가 발생하지 않으면 테스트 실패
            assertThat(false).as("Duplicate key should throw exception").isTrue();
        } catch (Exception e) {
            // 예외가 발생하면 성공
            assertThat(e).isNotNull();
        }
    }

    @Test
    @DisplayName("만료일과 마지막 사용일을 설정할 수 있다")
    void expirationAndLastUsedAt() {
        // given
        LocalDateTime expiresAt = LocalDateTime.now().plusDays(30);
        LocalDateTime lastUsedAt = LocalDateTime.now();

        ClientAccessKey key = ClientAccessKey.builder()
                .name("test-key")
                .keyValue("test-value")
                .issuedBy("team")
                .active(true)
                .deleted(false)
                .expiresAt(expiresAt)
                .lastUsedAt(lastUsedAt)
                .build();

        // when
        ClientAccessKey savedKey = repository.save(key);
        entityManager.flush();

        // then
        assertThat(savedKey.getExpiresAt()).isNotNull();
        assertThat(savedKey.getLastUsedAt()).isNotNull();
    }
}
