package com.craftpass.service;

import com.craftpass.auth.BCrypt;
import com.craftpass.auth.CryptoManager;
import com.craftpass.auth.SessionTokenManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.logging.Logger;

/**
 * Handles password authentication and updates against LoginSecurity database.
 * Directly integrates with LoginSecurity's Algorithm and BCryptLib for 100% hash parity.
 */
public class PasswordService {
    private final Plugin plugin;
    private final CryptoManager cryptoManager;
    private final SessionTokenManager tokenManager;
    private final Logger logger;
    private final File dbFile;

    public PasswordService(Plugin plugin, CryptoManager cryptoManager, SessionTokenManager tokenManager) {
        this.plugin = plugin;
        this.cryptoManager = cryptoManager;
        this.tokenManager = tokenManager;
        this.logger = plugin.getLogger();
        this.dbFile = new File(plugin.getDataFolder().getParentFile(), "LoginSecurity/LoginSecurity.db");
    }

    private Connection getConnection() throws Exception {
        if (!dbFile.exists()) {
            throw new IllegalStateException("LoginSecurity database not found at " + dbFile.getAbsolutePath());
        }
        Class.forName("org.sqlite.JDBC");
        return DriverManager.getConnection("jdbc:sqlite:" + dbFile.getAbsolutePath());
    }

    public boolean authenticate(String username, String rawPassword) {
        if (username == null || rawPassword == null || !dbFile.exists()) {
            return false;
        }

        String sql = "SELECT id, password, hashing_algorithm FROM ls_players WHERE LOWER(last_name) = LOWER(?)";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    logger.warning("[PasswordService] User '" + username + "' not found in LoginSecurity database.");
                    return false;
                }
                int id = rs.getInt("id");
                String storedHash = rs.getString("password");
                int algoId = rs.getInt("hashing_algorithm");

                boolean match = verifyPassword(rawPassword, storedHash, algoId);
                if (match) {
                    logger.info("[PasswordService] Authentication succeeded for user: " + username);
                    return true;
                } else {
                    logger.warning("[PasswordService] Password mismatch for user: " + username);
                }
            }
        } catch (Exception e) {
            logger.warning("[PasswordService] Authentication error for " + username + ": " + e.getMessage());
        }
        return false;
    }

    public boolean verifyPassword(String rawPassword, String storedHash, int algoId) {
        if (rawPassword == null || storedHash == null) {
            return false;
        }

        // 1. Direct reflection call to LoginSecurity's native Algorithm (supports BCrypt, SHA256, xAuth, etc.)
        try {
            Class<?> algoClass = Class.forName("com.lenis0012.bukkit.loginsecurity.hashing.Algorithm");
            Method getById = algoClass.getMethod("getById", int.class);
            Object algoObj = getById.invoke(null, algoId);
            if (algoObj != null) {
                Method check = algoClass.getMethod("check", String.class, String.class);
                boolean match = (Boolean) check.invoke(algoObj, rawPassword, storedHash);
                if (match) return true;
            }
        } catch (Throwable ignored) {}

        // 2. Direct reflection call to LoginSecurity's BCryptLib
        try {
            Class<?> bcryptLib = Class.forName("com.lenis0012.bukkit.loginsecurity.hashing.lib.BCryptLib");
            Method checkpw = bcryptLib.getMethod("checkpw", String.class, String.class);
            boolean match = (Boolean) checkpw.invoke(null, rawPassword, storedHash);
            if (match) return true;
        } catch (Throwable ignored) {}

        // 3. Embedded standard BCrypt fallback
        try {
            if (BCrypt.checkpw(rawPassword, storedHash)) {
                return true;
            }
        } catch (Throwable ignored) {}

        return false;
    }

    public boolean changePassword(String username, String oldPassword, String newPassword) {
        if (!authenticate(username, oldPassword)) {
            return false;
        }
        if (newPassword == null || newPassword.length() < 6 || newPassword.length() > 32) {
            return false;
        }

        String newHash = null;
        int algoId = 7; // Default to BCrypt
        try {
            Class<?> algoClass = Class.forName("com.lenis0012.bukkit.loginsecurity.hashing.Algorithm");
            Field bcryptField = algoClass.getField("BCRYPT");
            Object bcryptAlgo = bcryptField.get(null);
            Method hashMethod = algoClass.getMethod("hash", String.class);
            newHash = (String) hashMethod.invoke(bcryptAlgo, newPassword);
        } catch (Throwable t) {
            try {
                Class<?> bcryptLib = Class.forName("com.lenis0012.bukkit.loginsecurity.hashing.lib.BCryptLib");
                Method gensalt = bcryptLib.getMethod("gensalt", int.class);
                String salt = (String) gensalt.invoke(null, 10);
                Method hashpw = bcryptLib.getMethod("hashpw", String.class, String.class);
                newHash = (String) hashpw.invoke(null, newPassword, salt);
            } catch (Throwable ignored) {
                newHash = BCrypt.hashpw(newPassword, BCrypt.gensalt(10));
            }
        }

        String updateSql = "UPDATE ls_players SET password = ?, hashing_algorithm = ? WHERE LOWER(last_name) = LOWER(?)";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(updateSql)) {
            ps.setString(1, newHash);
            ps.setInt(2, algoId);
            ps.setString(3, username);
            int affected = ps.executeUpdate();
            if (affected > 0) {
                logger.info("[PasswordService] Password updated successfully in LoginSecurity for " + username);
                kickPlayerIfOnline(username, "§c您的登录密码已在移动管家端成功修改，请使用新密码重新登录！");
                return true;
            }
        } catch (Exception e) {
            logger.severe("[PasswordService] Password update failed for " + username + ": " + e.getMessage());
        }
        return false;
    }

    private void kickPlayerIfOnline(String username, String reason) {
        Bukkit.getScheduler().runTask(plugin, () -> {
            Player player = Bukkit.getPlayerExact(username);
            if (player != null && player.isOnline()) {
                player.kickPlayer(reason);
            }
        });
    }
}
