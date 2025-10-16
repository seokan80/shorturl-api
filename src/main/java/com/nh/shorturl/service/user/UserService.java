package com.nh.shorturl.service.user;

import com.nh.shorturl.entity.User;

import java.util.Optional;

public interface UserService {
    Optional<User> findByUsernameAndApiKey(String username, String apiKey);
    User createUser(String username);
}
