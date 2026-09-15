package com.youai.crm.account;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;

class LoginProtectionServiceTest {
    private final Clock clock = Clock.fixed(Instant.parse("2026-09-15T12:00:00Z"), ZoneOffset.UTC);

    @Test
    void locksAccountAfterConfiguredNumberOfFailures() {
        LoginProtectionService service = new LoginProtectionService(100, Duration.ofMinutes(1), 3,
                Duration.ofMinutes(15), clock);

        for (int i = 0; i < 3; i++) {
            service.checkAndRecordAttempt("192.0.2.1", "Alice");
            service.recordFailure("Alice");
        }

        assertThrows(LoginRateLimitException.class,
                () -> service.checkAndRecordAttempt("192.0.2.2", "alice"));
    }

    @Test
    void successfulLoginClearsAccountFailures() {
        LoginProtectionService service = new LoginProtectionService(100, Duration.ofMinutes(1), 2,
                Duration.ofMinutes(15), clock);
        service.recordFailure("alice");
        service.recordFailure("alice");
        service.recordSuccess("alice");

        assertDoesNotThrow(() -> service.checkAndRecordAttempt("192.0.2.1", "alice"));
    }

    @Test
    void limitsAllLoginAttemptsFromSameIp() {
        LoginProtectionService service = new LoginProtectionService(2, Duration.ofMinutes(1), 10,
                Duration.ofMinutes(15), clock);
        service.checkAndRecordAttempt("192.0.2.1", "alice");
        service.checkAndRecordAttempt("192.0.2.1", "bob");

        assertThrows(LoginRateLimitException.class,
                () -> service.checkAndRecordAttempt("192.0.2.1", "charlie"));
    }
}
