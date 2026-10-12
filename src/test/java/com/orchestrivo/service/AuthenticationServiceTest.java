package com.orchestrivo.service;

import com.orchestrivo.model.User;
import com.orchestrivo.persistence.DataSourceManager;
import com.orchestrivo.persistence.UserRepositoryJdbcImpl;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assumptions;

import com.orchestrivo.service.AuthenticationService;

import java.sql.Connection;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class AuthenticationServiceTest {
    private static AuthenticationService authService;
    private static DataSourceManager dsm;

    @BeforeAll
    static void setUp() {
        try (Connection conn = DataSourceManager.getDataSource().getConnection()) {
            Assumptions.assumeTrue(conn != null && conn.isValid(2), "MySQL not available, skipping test");
            authService = new AuthenticationService(new UserRepositoryJdbcImpl());
        } catch (Exception e) {
            Assumptions.assumeTrue(false, "MySQL not available, skipping test");
        }
    }

    @Test
    void testRegisterAndAuthenticate() {
        String email = "test" + UUID.randomUUID() + "@example.com";
        User user = authService.register(email, "password123", "Alice Smith", "USER");
        assertNotNull(user);
        assertTrue(user.getId() > 0);
        assertEquals("Alice Smith", user.getFullName());

        User auth = authService.authenticate(email, "password123");
        assertNotNull(auth);
        assertEquals(user.getId(), auth.getId());
        assertEquals("Alice Smith", auth.getFullName());

        User wrong = authService.authenticate(email, "wrongpassword");
        assertNull(wrong);
    }

    @Test
    void testRegistrationValidation() {
        // Blank full name
        assertThrows(IllegalArgumentException.class, () -> 
            authService.register("valid@example.com", "password123", "   ", "USER"));

        // Invalid email
        assertThrows(IllegalArgumentException.class, () -> 
            authService.register("not-an-email", "password123", "Valid Name", "USER"));

        // Short password
        assertThrows(IllegalArgumentException.class, () -> 
            authService.register("valid2@example.com", "12345", "Valid Name", "USER"));

        // Duplicate email
        String email = "dup" + UUID.randomUUID() + "@example.com";
        authService.register(email, "password123", "Bob Jones", "USER");
        assertThrows(IllegalArgumentException.class, () -> 
            authService.register(email, "password123", "Bob Jones", "USER"));
    }
}
