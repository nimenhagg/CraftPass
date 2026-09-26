package com.craftpass.auth;

import com.craftpass.config.PluginConfig;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages active session tokens and brute-force protection.
 */
public class SessionTokenManager {
    private final PluginConfig config;

    public static class Session {
        public final String token;
        public final String username;
        public final UUID playerUuid;
        public final long createdAt;
        public long expiresAt;

        public Session(String token, String username, UUID playerUuid, long expiresAt) {
            this.token = token;
            this.username = username;
            this.playerUuid = playerUuid;
            this.createdAt = System.currentTimeMillis();
            this.expiresAt = expiresAt;
        }

        public boolean isExpired() {
            return System.currentTimeMillis() > expiresAt;
        }
    }

    private static class IpAttemptRecord {
        int failedAttempts;
        long lockedUntil;
    }

    private final ConcurrentHashMap<String, Session> activeSessions = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, IpAttemptRecord> ipAttempts = new ConcurrentHashMap<>();

    public SessionTokenManager(PluginConfig config) {
        this.config = config;
    }

    public Session createSession(String username, UUID playerUuid) {
        String token = UUID.randomUUID().toString().replace("-", "") + UUID.randomUUID().toString().replace("-", "");
        long expiresAt = System.currentTimeMillis() + (config.getSessionTtlMinutes() * 60L * 1000L);
        Session session = new Session(token, username, playerUuid, expiresAt);
        activeSessions.put(token, session);
        return session;
    }

    public Session getSession(String token) {
        if (token == null) return null;
        Session session = activeSessions.get(token);
        if (session == null) return null;
        if (session.isExpired()) {
            activeSessions.remove(token);
            return null;
        }
        // Slide session expiration
        session.expiresAt = System.currentTimeMillis() + (config.getSessionTtlMinutes() * 60L * 1000L);
        return session;
    }

    public void invalidateUserSessions(String username) {
        activeSessions.entrySet().removeIf(entry -> entry.getValue().username.equalsIgnoreCase(username));
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
        if (rec.failedAttempts >= config.getMaxFailedAttempts()) {
            rec.lockedUntil = System.currentTimeMillis() + (config.getLockoutMinutes() * 60L * 1000L);
        }
    }

    public void resetFailedAttempts(String ip) {
        ipAttempts.remove(ip);
    }
}
