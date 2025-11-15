package com.nh.shorturl.admin.repository;

import com.nh.shorturl.admin.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class UserRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private UserRepository repository;

    @Test
    @DisplayName("사용자명으로 사용자를 조회한다")
    void findByUsername() {
        // given
        User user = User.builder()
                .username("test-user")
                .groupName("test-group")
                .deleted(false)
                .build();
        entityManager.persist(user);
        entityManager.flush();

        // when
        Optional<User> result = repository.findByUsername("test-user");

        // then
        assertThat(result).isPresent();
        assertThat(result.get().getUsername()).isEqualTo("test-user");
        assertThat(result.get().getGroupName()).isEqualTo("test-group");
    }

    @Test
    @DisplayName("존재하지 않는 사용자명으로 조회하면 빈 Optional을 반환한다")
    void findByUsername_notFound() {
        // when
        Optional<User> result = repository.findByUsername("non-existent-user");

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("사용자명으로 삭제되지 않은 사용자를 조회한다")
    void findByUsernameAndDeletedFalse() {
        // given
        User user = User.builder()
                .username("active-user")
                .groupName("test-group")
                .deleted(false)
                .build();
        entityManager.persist(user);
        entityManager.flush();

        // when
        Optional<User> result = repository.findByUsernameAndDeletedFalse("active-user");

        // then
        assertThat(result).isPresent();
        assertThat(result.get().getUsername()).isEqualTo("active-user");
    }

    @Test
    @DisplayName("삭제된 사용자는 findByUsernameAndDeletedFalse로 조회되지 않는다")
    void findByUsernameAndDeletedFalse_deletedUser() {
        // given
        User user = User.builder()
                .username("deleted-user")
                .groupName("test-group")
                .deleted(true)
                .build();
        entityManager.persist(user);
        entityManager.flush();

        // when
        Optional<User> result = repository.findByUsernameAndDeletedFalse("deleted-user");

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("사용자명과 리프레시 토큰으로 사용자를 조회한다")
    void findByUsernameAndRefreshToken() {
        // given
        User user = User.builder()
                .username("token-user")
                .groupName("test-group")
                .refreshToken("refresh-token-123")
                .deleted(false)
                .build();
        entityManager.persist(user);
        entityManager.flush();

        // when
        Optional<User> result = repository.findByUsernameAndRefreshToken("token-user", "refresh-token-123");

        // then
        assertThat(result).isPresent();
        assertThat(result.get().getUsername()).isEqualTo("token-user");
        assertThat(result.get().getRefreshToken()).isEqualTo("refresh-token-123");
    }

    @Test
    @DisplayName("잘못된 리프레시 토큰으로 조회하면 빈 Optional을 반환한다")
    void findByUsernameAndRefreshToken_wrongToken() {
        // given
        User user = User.builder()
                .username("token-user")
                .groupName("test-group")
                .refreshToken("refresh-token-123")
                .deleted(false)
                .build();
        entityManager.persist(user);
        entityManager.flush();

        // when
        Optional<User> result = repository.findByUsernameAndRefreshToken("token-user", "wrong-token");

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("삭제되지 않은 모든 사용자를 생성일 내림차순으로 조회한다")
    void findAllByDeletedFalseOrderByCreatedAtDesc() {
        // given
        User user1 = User.builder()
                .username("user-1")
                .groupName("group-1")
                .deleted(false)
                .build();
        entityManager.persist(user1);
        entityManager.flush();

        User user2 = User.builder()
                .username("user-2")
                .groupName("group-2")
                .deleted(false)
                .build();
        entityManager.persist(user2);
        entityManager.flush();

        // when
        List<User> result = repository.findAllByDeletedFalseOrderByCreatedAtDesc();

        // then
        assertThat(result).hasSize(2);
        // 최신 항목이 먼저 나와야 함
        assertThat(result.get(0).getUsername()).isEqualTo("user-2");
        assertThat(result.get(1).getUsername()).isEqualTo("user-1");
    }

    @Test
    @DisplayName("삭제된 사용자는 목록 조회에서 제외된다")
    void findAllByDeletedFalseOrderByCreatedAtDesc_excludesDeleted() {
        // given
        User activeUser = User.builder()
                .username("active-user")
                .groupName("group")
                .deleted(false)
                .build();
        entityManager.persist(activeUser);

        User deletedUser = User.builder()
                .username("deleted-user")
                .groupName("group")
                .deleted(true)
                .build();
        entityManager.persist(deletedUser);
        entityManager.flush();

        // when
        List<User> result = repository.findAllByDeletedFalseOrderByCreatedAtDesc();

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getUsername()).isEqualTo("active-user");
    }

    @Test
    @DisplayName("사용자를 저장한다")
    void save() {
        // given
        User user = User.builder()
                .username("new-user")
                .groupName("new-group")
                .deleted(false)
                .build();

        // when
        User savedUser = repository.save(user);

        // then
        assertThat(savedUser.getId()).isNotNull();
        assertThat(savedUser.getUsername()).isEqualTo("new-user");
        assertThat(savedUser.getGroupName()).isEqualTo("new-group");
    }

    @Test
    @DisplayName("사용자 정보를 수정한다")
    void update() {
        // given
        User user = User.builder()
                .username("original-user")
                .groupName("original-group")
                .deleted(false)
                .build();
        User savedUser = entityManager.persist(user);
        entityManager.flush();

        // when
        savedUser.setGroupName("updated-group");
        savedUser.setApiKey("new-api-key");
        savedUser.setRefreshToken("new-refresh-token");
        entityManager.flush();

        // then
        User updatedUser = repository.findById(savedUser.getId()).orElseThrow();
        assertThat(updatedUser.getGroupName()).isEqualTo("updated-group");
        assertThat(updatedUser.getApiKey()).isEqualTo("new-api-key");
        assertThat(updatedUser.getRefreshToken()).isEqualTo("new-refresh-token");
    }

    @Test
    @DisplayName("사용자를 삭제한다 (소프트 삭제)")
    void delete() {
        // given
        User user = User.builder()
                .username("to-delete")
                .groupName("group")
                .deleted(false)
                .build();
        User savedUser = entityManager.persist(user);
        entityManager.flush();
        entityManager.clear();

        // when
        User userToDelete = repository.findById(savedUser.getId()).orElseThrow();
        repository.delete(userToDelete);
        entityManager.flush();

        // then
        // SQLDelete 어노테이션으로 인해 실제로는 deleted = true로 업데이트됨
        Optional<User> deletedUser = repository.findByUsernameAndDeletedFalse("to-delete");
        assertThat(deletedUser).isEmpty();
    }

    @Test
    @DisplayName("username과 apiKey 조합은 유니크해야 한다")
    void usernameApiKeyUnique() {
        // given
        User user1 = User.builder()
                .username("user-1")
                .groupName("group-1")
                .apiKey("api-key-1")
                .deleted(false)
                .build();
        entityManager.persist(user1);
        entityManager.flush();

        User user2 = User.builder()
                .username("user-1")
                .groupName("group-2")
                .apiKey("api-key-1")
                .deleted(false)
                .build();

        // when & then
        // 동일한 username과 apiKey 조합으로 저장 시도하면 예외 발생
        try {
            entityManager.persist(user2);
            entityManager.flush();
            // 예외가 발생하지 않으면 테스트 실패
            assertThat(false).as("Duplicate username and apiKey should throw exception").isTrue();
        } catch (Exception e) {
            // 예외가 발생하면 성공
            assertThat(e).isNotNull();
        }
    }

    @Test
    @DisplayName("같은 username이지만 다른 apiKey는 저장할 수 있다")
    void sameUsernameWithDifferentApiKey() {
        // given
        User user1 = User.builder()
                .username("same-user")
                .groupName("group-1")
                .apiKey("api-key-1")
                .deleted(false)
                .build();
        entityManager.persist(user1);
        entityManager.flush();

        User user2 = User.builder()
                .username("same-user")
                .groupName("group-2")
                .apiKey("api-key-2")
                .deleted(false)
                .build();

        // when
        User savedUser2 = entityManager.persist(user2);
        entityManager.flush();

        // then
        assertThat(savedUser2.getId()).isNotNull();
        assertThat(savedUser2.getUsername()).isEqualTo("same-user");
        assertThat(savedUser2.getApiKey()).isEqualTo("api-key-2");
    }

    @Test
    @DisplayName("groupName 없이 사용자를 생성할 수 있다")
    void saveWithoutGroupName() {
        // given
        User user = User.builder()
                .username("no-group-user")
                .groupName(null)
                .deleted(false)
                .build();

        // when
        User savedUser = repository.save(user);

        // then
        assertThat(savedUser.getId()).isNotNull();
        assertThat(savedUser.getUsername()).isEqualTo("no-group-user");
        assertThat(savedUser.getGroupName()).isNull();
    }
}
