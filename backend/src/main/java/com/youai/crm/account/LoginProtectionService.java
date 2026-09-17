package com.youai.crm.account;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class LoginProtectionService {
    private final Map<String, ArrayDeque<Instant>> ipAttempts = new ConcurrentHashMap<>();
    private final Map<String, FailureState> accountFailures = new ConcurrentHashMap<>();
    private final Clock clock;
    private final int ipMaxAttempts;
    private final Duration ipWindow;
    private final int accountMaxFailures;
    private final Duration accountLockDuration;

    @Autowired
    public LoginProtectionService(
            @Value("${security.login.ip-max-attempts:30}") int ipMaxAttempts,
            @Value("${security.login.ip-window-seconds:60}") long ipWindowSeconds,
            @Value("${security.login.account-max-failures:5}") int accountMaxFailures,
            @Value("${security.login.account-lock-seconds:900}") long accountLockSeconds) {
        this(ipMaxAttempts, Duration.ofSeconds(ipWindowSeconds), accountMaxFailures,
                Duration.ofSeconds(accountLockSeconds), Clock.systemUTC());
    }

    LoginProtectionService(int ipMaxAttempts, Duration ipWindow, int accountMaxFailures,
            Duration accountLockDuration, Clock clock) {
        this.ipMaxAttempts = ipMaxAttempts;
        this.ipWindow = ipWindow;
        this.accountMaxFailures = accountMaxFailures;
        this.accountLockDuration = accountLockDuration;
        this.clock = clock;
    }

    public void checkAndRecordAttempt(String clientIp, String username) {
        Instant now = clock.instant();
        recordIpAttempt(clientIp, now);
        checkAccount(username, now);
    }

    public void recordIpAttempt(String clientIp) {
        recordIpAttempt(clientIp, clock.instant());
    }

    public void checkAccount(String username) {
        checkAccount(username, clock.instant());
    }

    private void recordIpAttempt(String clientIp, Instant now) {
        String ipKey = normalizeIp(clientIp);
        ArrayDeque<Instant> attempts = ipAttempts.computeIfAbsent(ipKey, ignored -> new ArrayDeque<>());
        synchronized (attempts) {
            Instant cutoff = now.minus(ipWindow);
            while (!attempts.isEmpty() && !attempts.peekFirst().isAfter(cutoff)) attempts.removeFirst();
            if (attempts.size() >= ipMaxAttempts) {
                long retry = Duration.between(now, attempts.peekFirst().plus(ipWindow)).toSeconds() + 1;
                throw new LoginRateLimitException("登录尝试过于频繁，请稍后再试", retry);
            }
            attempts.addLast(now);
        }
    }

    private void checkAccount(String username, Instant now) {
        FailureState state = accountFailures.get(normalizeUsername(username));
        if (state != null && state.lockedUntil != null && state.lockedUntil.isAfter(now)) {
            throw new LoginRateLimitException("该账号因多次登录失败已被临时锁定",
                    Duration.between(now, state.lockedUntil).toSeconds() + 1);
        }
    }

    public void recordFailure(String username) {
        String key = normalizeUsername(username);
        Instant now = clock.instant();
        accountFailures.compute(key, (ignored, previous) -> {
            int failures = previous == null || (previous.lockedUntil != null && !previous.lockedUntil.isAfter(now))
                    ? 1 : previous.failures + 1;
            Instant lockedUntil = failures >= accountMaxFailures ? now.plus(accountLockDuration) : null;
            return new FailureState(failures, lockedUntil);
        });
    }

    public void recordSuccess(String username) {
        accountFailures.remove(normalizeUsername(username));
    }

    /** Clears the temporary failed-login lock for an account. */
    public void unlock(String username) {
        accountFailures.remove(normalizeUsername(username));
    }

    public boolean isAccountLocked(String username) {
        FailureState state = accountFailures.get(normalizeUsername(username));
        Instant now = clock.instant();
        return state != null && state.lockedUntil != null && state.lockedUntil.isAfter(now);
    }

    private String normalizeUsername(String username) {
        return username == null ? "" : username.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeIp(String clientIp) {
        return clientIp == null || clientIp.isBlank() ? "unknown" : clientIp;
    }

    private record FailureState(int failures, Instant lockedUntil) {}
}
