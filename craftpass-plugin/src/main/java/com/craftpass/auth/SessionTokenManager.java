package com.craftpass.auth;

import com.craftpass.config.PluginConfig;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages active user sessions, temporary tokens, and brute-force IP rate limiting.
 */
public class SessionTokenManager {
    private final PluginConfig config;
    private final ConcurrentHashMap<String, Session> activeSessions = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, IpAttemptRecord> ipAttempts = new ConcurrentHashMap<>();

    public static class Session {
        public final String username;
        public final UUID playerUuid;
        public final String token;
        public final long expiresAt;

        public Session(String username, UUID playerUuid, String token, long expiresAt) {
            this.username = username;
            this.playerUuid = playerUuid;
            this.token = token;
            this.expiresAt = expiresAt;
        }

        public boolean isExpired() {
            return System.currentTimeMillis() > expiresAt;
        }
    }

    private static class IpAttemptRecord {
        int failedAttempts = 0;
        long lockedUntil = 0;
    }

    private int sessionTtlMinutes = 120;
    private int maxFailedAttempts = 5;
    private int lockoutMinutes = 10;

    public SessionTokenManager(PluginConfig config) {
        this.config = config;
    }

    public SessionTokenManager(int sessionTtlMinutes, int maxFailedAttempts, int lockoutMinutes) {
        this.config = null;
        this.sessionTtlMinutes = sessionTtlMinutes;
        this.maxFailedAttempts = maxFailedAttempts;
        this.lockoutMinutes = lockoutMinutes;
    }

    private int getSessionTtl() {
        return config != null ? config.getSessionTtlMinutes() : sessionTtlMinutes;
    }

    private int getMaxFailed() {
        return config != null ? config.getMaxFailedAttempts() : maxFailedAttempts;
    }

    private int getLockoutMin() {
        return config != null ? config.getLockoutMinutes() : lockoutMinutes;
    }

    public Session createSession(String username, UUID uuid) {
        String token = UUID.randomUUID().toString().replace("-", "") + UUID.randomUUID().toString().replace("-", "");
        long expiresAt = System.currentTimeMillis() + (getSessionTtl() * 60L * 1000L);
        Session session = new Session(username, uuid, token, expiresAt);
        activeSessions.put(token, session);
        cleanExpiredSessions();
        return session;
    }

    public Session validateSession(String token) {
        if (token == null) return null;
        Session session = activeSessions.get(token);
        if (session == null || session.isExpired()) {
            activeSessions.remove(token);
            return null;
        }
        return session;
    }

    public Session getSession(String token) {
        return validateSession(token);
    }

    public void invalidateToken(String token) {
        if (token != null) {
            activeSessions.remove(token);
        }
    }

    public boolean isIpLocked(String ip) {
        IpAttemptRecord rec = ipAttempts.get(ip);
        if (rec == null) return false;
        if (System.currentTimeMillis() < rec.lockedUntil) {
            return true;
        }
        if (rec.lockedUntil > 0) {
            ipAttempts.remove(ip);
        }
        return false;
    }

    public void recordFailedAttempt(String ip) {
        IpAttemptRecord rec = ipAttempts.computeIfAbsent(ip, k -> new IpAttemptRecord());
        rec.failedAttempts++;
        if (rec.failedAttempts >= getMaxFailed()) {
            rec.lockedUntil = System.currentTimeMillis() + (getLockoutMin() * 60L * 1000L);
        }
    }

    public void resetFailedAttempts(String ip) {
        ipAttempts.remove(ip);
    }

    public boolean resetLockout(String ip) {
        IpAttemptRecord removed = ipAttempts.remove(ip);
        return removed != null && removed.lockedUntil > System.currentTimeMillis();
    }

    public void clearAllLocks() {
        ipAttempts.clear();
    }

    public void resetAllLockouts() {
        clearAllLocks();
    }

    public Map<String, Long> getActiveLocks() {
        long now = System.currentTimeMillis();
        Map<String, Long> active = new HashMap<>();
        for (Map.Entry<String, IpAttemptRecord> entry : ipAttempts.entrySet()) {
            if (entry.getValue().lockedUntil > now) {
                active.put(entry.getKey(), (entry.getValue().lockedUntil - now) / 1000);
            }
        }
        return active;
    }

    public Map<String, Long> getActiveLockouts() {
        return getActiveLocks();
    }

    private void cleanExpiredSessions() {
        long now = System.currentTimeMillis();
        activeSessions.entrySet().removeIf(entry -> now > entry.getValue().expiresAt);
    }
}
