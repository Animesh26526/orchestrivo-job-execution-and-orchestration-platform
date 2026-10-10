package com.orchestrivo.service;

import at.favre.lib.crypto.bcrypt.BCrypt;
import com.orchestrivo.model.User;
import com.orchestrivo.persistence.UserRepository;
import java.util.Optional;

public class AuthenticationService {
    private final UserRepository userRepository;

    public AuthenticationService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User register(String email, String password, String role) {
        if (email == null || email.trim().isEmpty() || password == null || password.trim().isEmpty()) {
            throw new IllegalArgumentException("Email and password are required.");
        }
        Optional<User> existing = userRepository.findByEmail(email);
        if (existing.isPresent()) {
            throw new IllegalArgumentException("User with this email already exists.");
        }
        String hash = BCrypt.withDefaults().hashToString(12, password.toCharArray());
        User user = new User();
        user.setEmail(email);
        user.setPasswordHash(hash);
        user.setRole(role != null ? role : "USER");
        user.setActive(true);
        return userRepository.save(user);
    }

    public User authenticate(String email, String password) {
        Optional<User> optUser = userRepository.findByEmail(email);
        if (optUser.isEmpty()) {
            return null;
        }
        User user = optUser.get();
        BCrypt.Result result = BCrypt.verifyer().verify(password.toCharArray(), user.getPasswordHash());
        if (result.verified && user.isActive()) {
            return user;
        }
        return null;
    }
}
