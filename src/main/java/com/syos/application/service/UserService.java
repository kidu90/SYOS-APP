package com.syos.application.service;

import java.util.Optional;

import com.syos.domain.entity.User;
import com.syos.domain.repository.UserRepository;

public class UserService {
    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User register(String fullName, String username, String password, String address) {
        if (fullName == null || fullName.isBlank() || username == null || username.isBlank()
            || password == null || password.isBlank() || address == null || address.isBlank()) {
            throw new IllegalArgumentException("All fields are required");
        }

        Optional<User> existing = userRepository.findByUsername(username.trim());
        if (existing.isPresent()) {
            throw new IllegalArgumentException("Username already exists");
        }

        User user = new User(fullName, username, password, address);
        userRepository.save(user);
        return userRepository.findByUsername(user.getUsername())
            .orElseThrow(() -> new IllegalStateException("Failed to create user"));
    }

    public User login(String username, String password) {
        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            throw new IllegalArgumentException("All fields are required");
        }

        User user = userRepository.findByUsername(username.trim())
            .orElseThrow(() -> new IllegalArgumentException("Username not found"));

        if (!user.getPassword().equals(password)) {
            throw new IllegalArgumentException("Wrong password");
        }

        return user;
    }
}
