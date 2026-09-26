package com.craftpass.service;

import com.craftpass.auth.BCrypt;
import com.craftpass.auth.CryptoManager;
import com.craftpass.auth.SessionTokenManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.logging.Logger;

/**
 * Handles password authentication and updates against LoginSecurity database.
 * Implements Server-side Pepper defense to protect passwords even if LoginSecurity.db is publicly downloadable.
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
                    return false;
                }
                int id = rs.getInt("id");
                String storedHash = rs.getString("password");

                // 1. Try peppered verification first
                String peppered = cryptoManager.pepperPassword(rawPassword);
                if (BCrypt.checkpw(peppered, storedHash)) {
                    return true;
                }

                // 2. Try legacy un-peppered verification (for accounts created prior to CraftPass)
                if (BCrypt.checkpw(rawPassword, storedHash)) {
                    // Upgrade in-place to peppered hash to secure against public DB exposure
                    upgradeToPepperedHash(id, peppered);
                    return true;
                }
            }
        } catch (Exception e) {
            logger.warning("[PasswordService] Authentication error for " + username + ": " + e.getMessage());
        }
        return false;
    }

    private void upgradeToPepperedHash(int playerId, String pepperedPassword) {
        String newHash = BCrypt.hashpw(pepperedPassword, BCrypt.gensalt(10));
        String updateSql = "UPDATE ls_players SET password = ?, hashing_algorithm = 7 WHERE id = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(updateSql)) {
            ps.setString(1, newHash);
            ps.setInt(2, playerId);
            ps.executeUpdate();
            logger.info("[PasswordService] Seamlessly upgraded player #" + playerId + " password to Peppered-BCrypt.");
        } catch (Exception e) {
            logger.warning("[PasswordService] Failed to auto-upgrade hash for player #" + playerId + ": " + e.getMessage());
        }
    }

    public boolean changePassword(String username, String oldPassword, String newPassword) {
        if (!authenticate(username, oldPassword)) {
            return false;
        }
        if (newPassword == null || newPassword.length() < 6 || newPassword.length() > 32) {
            return false;
        }

        String pepperedNew = cryptoManager.pepperPassword(newPassword);
        String newHash = BCrypt.hashpw(pepperedNew, BCrypt.gensalt(10));

        String sql = "UPDATE ls_players SET password = ?, hashing_algorithm = 7 WHERE LOWER(last_name) = LOWER(?)";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, newHash);
            ps.setString(2, username);
            int rows = ps.executeUpdate();
            if (rows > 0) {
                // Invalidate all app sessions
                tokenManager.invalidateUserSessions(username);

                // If player is in-game, kick or notify on main thread
                Bukkit.getScheduler().runTask(plugin, () -> {
                    Player p = Bukkit.getPlayerExact(username);
                    if (p != null && p.isOnline()) {
                        p.kickPlayer("§c[CraftPass] 您的账号密码已在 App 端修改，请重新登录游戏！");
                    }
                });
                return true;
            }
        } catch (Exception e) {
            logger.severe("[PasswordService] Password update failed for " + username + ": " + e.getMessage());
        }
        return false;
    }
}
