package com.campusmarket.service;

import java.util.Optional;

/**
 * Manage login sessions centrally in Redis so that all service instances can share authentication state.
 */
public interface LoginSessionService {

    /**
     * Persist a session snapshot in Redis.
     *
     * @param token     the JWT token issued to the client
     * @param userId    authenticated user id
     * @param username  authenticated username
     * @param role      role name (e.g. STUDENT/ADMIN)
     * @param ttlMillis time to live in milliseconds (typically the JWT expiration)
     * @param ip        client ip address (optional)
     * @param userAgent client user agent (optional)
     */
    void storeSession(String token,
                      Long userId,
                      String username,
                      String role,
                      long ttlMillis,
                      String ip,
                      String userAgent);

    /**
     * Remove a session (user logout or admin invalidation).
     *
     * @param token JWT token to revoke
     */
    void revokeSession(String token);

    /**
     * Remove all sessions for the given user (e.g. force logout from every device).
     *
     * @param userId the user id
     */
    void revokeAllSessions(Long userId);

    /**
     * Retrieve the session details if still active.
     *
     * @param token JWT token value
     * @return optional session description
     */
    Optional<LoginSession> getSession(String token);

    /**
     * Whether the provided token is currently active (present in Redis).
     */
    default boolean isSessionActive(String token) {
        return getSession(token).isPresent();
    }

    /**
     * Update the session last seen timestamp (e.g. on every authenticated request).
     *
     * @param token JWT token
     */
    void refreshSession(String token);

    /**
     * Snapshot of session data stored in Redis.
     */
    final class LoginSession {
        private Long userId;
        private String username;
        private String role;
        private long issuedAt;
        private long expiresAt;
        private long lastSeenAt;
        private String ip;
        private String userAgent;

        public LoginSession() {
        }

        public LoginSession(Long userId,
                            String username,
                            String role,
                            long issuedAt,
                            long expiresAt,
                            long lastSeenAt,
                            String ip,
                            String userAgent) {
            this.userId = userId;
            this.username = username;
            this.role = role;
            this.issuedAt = issuedAt;
            this.expiresAt = expiresAt;
            this.lastSeenAt = lastSeenAt;
            this.ip = ip;
            this.userAgent = userAgent;
        }

        public Long getUserId() {
            return userId;
        }

        public String getUsername() {
            return username;
        }

        public String getRole() {
            return role;
        }

        public long getIssuedAt() {
            return issuedAt;
        }

        public long getExpiresAt() {
            return expiresAt;
        }

        public long getLastSeenAt() {
            return lastSeenAt;
        }

        public String getIp() {
            return ip;
        }

        public String getUserAgent() {
            return userAgent;
        }

        public void setLastSeenAt(long lastSeenAt) {
            this.lastSeenAt = lastSeenAt;
        }
    }
}
