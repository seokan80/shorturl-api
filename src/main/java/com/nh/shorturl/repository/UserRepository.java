package com.nh.shorturl.repository;

import com.nh.shorturl.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsernameAndApiKey(String username, String apiKey);

    Optional<User> findByUsername(String username);
}