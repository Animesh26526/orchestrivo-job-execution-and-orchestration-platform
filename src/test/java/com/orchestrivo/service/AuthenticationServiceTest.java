package com.orchestrivo.service;

import com.orchestrivo.model.User;
import com.orchestrivo.persistence.DataSourceManager;
import com.orchestrivo.persistence.UserRepositoryJdbcImpl;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assumptions;

import java.sql.Connection;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class AuthenticationServiceTest {
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
        User user = authService.register(email, "password123", "USER");
        assertNotNull(user);
        assertTrue(user.getId() > 0);

        User auth = authService.authenticate(email, "password123");
        assertNotNull(auth);
        assertEquals(user.getId(), auth.getId());

        User wrong = authService.authenticate(email, "wrongpassword");
        assertNull(wrong);
    }
}
