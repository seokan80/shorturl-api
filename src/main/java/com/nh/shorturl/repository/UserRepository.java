package com.nh.shorturl.repository;

import com.nh.shorturl.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);

    Optional<User> findByUsernameAndDeletedFalse(String username);

    Optional<User> findByUsernameAndRefreshToken(String username, String refreshToken);

    List<User> findAllByDeletedFalse();
}
