package com.orchestrivo.persistence;

import com.orchestrivo.model.User;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class UserRepositoryJdbcImplTest {

    private static UserRepository repository;

    @BeforeAll
    static void setUp() {
        try (Connection conn = DataSourceManager.getDataSource().getConnection()) {
            Assumptions.assumeTrue(conn.isValid(2), "Database connection is not available");
            repository = new UserRepositoryJdbcImpl();
        } catch (Exception e) {
            Assumptions.abort("Database connection failed, skipping tests");
        }
    }

    @Test
    void testSaveAndFindById() {
        User user = new User();
        user.setEmail("test" + System.currentTimeMillis() + "@example.com");
        user.setPasswordHash("hash123");
        user.setRole("USER");
        user.setActive(true);

        User savedUser = repository.save(user);
        assertTrue(savedUser.getId() > 0);

        Optional<User> found = repository.findById(savedUser.getId());
        assertTrue(found.isPresent());
        assertEquals(user.getEmail(), found.get().getEmail());
    }
}
