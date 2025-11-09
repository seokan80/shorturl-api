package com.nh.shorturl.service.user;

import com.nh.shorturl.dto.request.auth.UserRequest;
import com.nh.shorturl.entity.User;

import java.util.List;

public interface UserService {
    User createUser(UserRequest request);
    List<User> getAllUsers();
    void deleteUser(String username);
    User getUser(String username);
}
