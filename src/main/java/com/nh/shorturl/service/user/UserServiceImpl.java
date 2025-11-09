package com.nh.shorturl.service.user;

import com.nh.shorturl.dto.request.auth.UserRequest;
import com.nh.shorturl.entity.User;
import com.nh.shorturl.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    public UserServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public User createUser(UserRequest request) {
        return createUserInternal(request.getUsername());
    }

    private User createUserInternal(String username) {
        if (userRepository.findByUsername(username).isPresent()) {
            throw new IllegalArgumentException("Username already exists");
        }

        User user = new User(username);
        return userRepository.save(user);
    }

    @Override
    public List<User> getAllUsers() {
        return userRepository.findAllByDeletedFalse();
    }

    @Override
    public User getUser(String username) {
        return userRepository.findByUsernameAndDeletedFalse(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found with username: " + username));
    }

    @Override
    @Transactional
    public void deleteUser(String username) {
        User user = getUser(username);
        userRepository.delete(user);
    }
}
