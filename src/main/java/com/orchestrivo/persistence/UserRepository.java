package com.orchestrivo.persistence;

import com.orchestrivo.model.User;
import java.util.Optional;
import java.util.List;

public interface UserRepository {
    User save(User user);
    Optional<User> findById(long id);
    Optional<User> findByEmail(String email);
    List<User> findAll();
    void deleteById(long id);
}
