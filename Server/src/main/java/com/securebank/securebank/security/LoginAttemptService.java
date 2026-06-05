package com.securebank.securebank.security;

import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;

@Service
public class LoginAttemptService {

    private static final int MAX_ATTEMPTS = 5;
    private static final long LOCKOUT_DURATION_MS = 15 * 60 * 1000L;

    private record AttemptRecord(int count, long firstAttemptTime) {}

    private final ConcurrentHashMap<String, AttemptRecord> attempts = new ConcurrentHashMap<>();

    public void loginFailed(String key) {
        attempts.compute(key, (k, record) -> {
            if (record == null || isExpired(record)) {
                return new AttemptRecord(1, System.currentTimeMillis());
            }
            return new AttemptRecord(record.count() + 1, record.firstAttemptTime());
        });
    }

    public void loginSucceeded(String key) {
        attempts.remove(key);
    }

    public boolean isBlocked(String key) {
        AttemptRecord record = attempts.get(key);
        if (record == null) return false;
        if (isExpired(record)) {
            attempts.remove(key);
            return false;
        }
        return record.count() >= MAX_ATTEMPTS;
    }

    public void resetAll() {
        attempts.clear();
    }

    private boolean isExpired(AttemptRecord record) {
        return System.currentTimeMillis() - record.firstAttemptTime() > LOCKOUT_DURATION_MS;
    }
}
