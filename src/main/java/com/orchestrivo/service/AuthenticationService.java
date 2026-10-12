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

    private static final java.util.regex.Pattern EMAIL_PATTERN =
            java.util.regex.Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    public User register(String email, String password, String role) {
        String cleanEmail = email != null ? email.trim().toLowerCase() : "";
        String defaultName = cleanEmail.contains("@") ? cleanEmail.substring(0, cleanEmail.indexOf('@')) : "User";
        String hash = BCrypt.withDefaults().hashToString(12, (password != null ? password : "").toCharArray());
        User user = new User();
        user.setEmail(cleanEmail);
        user.setFullName(defaultName);
        user.setPasswordHash(hash);
        user.setRole(role != null ? role : "USER");
        user.setActive(true);
        return userRepository.save(user);
    }

    public User register(String email, String password, String fullName, String role) {
        if (fullName == null || fullName.trim().isEmpty()) {
            throw new IllegalArgumentException("Full name is required.");
        }
        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("Email address is required.");
        }
        String cleanEmail = email.trim().toLowerCase();
        if (!EMAIL_PATTERN.matcher(cleanEmail).matches()) {
            throw new IllegalArgumentException("Invalid email address format.");
        }
        if (password == null || password.length() < 6) {
            throw new IllegalArgumentException("Password must be at least 6 characters long.");
        }

        Optional<User> existing = userRepository.findByEmail(cleanEmail);
        if (existing.isPresent()) {
            throw new IllegalArgumentException("An account with this email address already exists.");
        }

        String hash = BCrypt.withDefaults().hashToString(12, password.toCharArray());
        User user = new User();
        user.setEmail(cleanEmail);
        user.setFullName(fullName.trim());
        user.setPasswordHash(hash);
        user.setRole(role != null ? role : "USER");
        user.setActive(true);
        return userRepository.save(user);
    }

    public User authenticate(String email, String password) {
        String cleanEmail = email != null ? email.trim().toLowerCase() : "";
        Optional<User> optUser = userRepository.findByEmail(cleanEmail);
        if (optUser.isEmpty()) {
            return null;
        }
        User user = optUser.get();
        if (user.getPasswordHash() == null) {
            return null;
        }
        BCrypt.Result result = BCrypt.verifyer().verify(password != null ? password.toCharArray() : new char[0], user.getPasswordHash());
        if (result.verified && user.isActive()) {
            return user;
        }
        return null;
    }
}
