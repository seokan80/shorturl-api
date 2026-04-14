package com.nh.shorturl.admin.service.user;

import com.nh.shorturl.admin.entity.User;
import com.nh.shorturl.dto.request.auth.UserRequest;
import com.nh.shorturl.dto.request.auth.UserUpdateRequest;

import java.util.List;

public interface UserService {
    User createUser(UserRequest request, String groupName);

    User updateUser(String username, UserUpdateRequest request);

    List<User> getAllUsers();

    void deleteUser(String username);

    User getUser(String username);
}
