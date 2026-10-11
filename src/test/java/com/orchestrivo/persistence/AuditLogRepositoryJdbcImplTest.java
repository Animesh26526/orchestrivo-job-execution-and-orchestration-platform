package com.orchestrivo.persistence;

import com.orchestrivo.model.AuditLogRecord;
import com.orchestrivo.model.User;
import com.orchestrivo.service.AuditService;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class AuditLogRepositoryJdbcImplTest {

    private static AuditLogRepository auditLogRepository;
    private static UserRepository userRepository;

    @BeforeAll
    static void setUp() {
        try (Connection conn = DataSourceManager.getDataSource().getConnection()) {
            Assumptions.assumeTrue(conn.isValid(2), "Database connection is not available");
            auditLogRepository = new AuditLogRepositoryJdbcImpl();
            userRepository = new UserRepositoryJdbcImpl();
        } catch (Exception e) {
            Assumptions.abort("Database connection failed, skipping tests");
        }
    }

    @Test
    void testSaveAndQueryAuditLogsWithRedaction() {
        User user = new User();
        user.setEmail("audit_test_" + System.currentTimeMillis() + "@example.com");
        user.setPasswordHash("hash");
        user.setRole("USER");
        user.setActive(true);
        user = userRepository.save(user);

        AuditService auditService = new AuditService(auditLogRepository);

        Map<String, Object> meta = new HashMap<>();
        meta.put("workloadName", "Batch1");
        meta.put("password", "Secret123!");
        meta.put("csrfToken", "uuid-token-xyz");

        auditService.log(user.getId(), "WORKLOAD_CREATE", "WORKLOAD", "50", meta);

        List<AuditLogRecord> logs = auditService.getLogsForActor(user.getId(), 10);
        assertFalse(logs.isEmpty());

        AuditLogRecord latest = logs.get(0);
        assertEquals(user.getId(), latest.getActorUserId());
        assertEquals("WORKLOAD_CREATE", latest.getEventType());
        assertEquals("WORKLOAD", latest.getResourceType());
        assertEquals("50", latest.getResourceId());
        assertNotNull(latest.getMetadata());

        // Verify secrets were redacted
        assertFalse(latest.getMetadata().contains("Secret123!"), "Password must not be stored in audit metadata");
        assertFalse(latest.getMetadata().contains("uuid-token-xyz"), "CSRF token must not be stored in audit metadata");
        assertTrue(latest.getMetadata().contains("[REDACTED]"));
        assertTrue(latest.getMetadata().contains("Batch1"));

        userRepository.deleteById(user.getId());
    }
}
