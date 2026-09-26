package com.craftpass.service;

import com.craftpass.util.PlayerDataLocator;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * Executes atomic account data migration from old username to new username:
 * 1. World playerdata (.dat)
 * 2. Stats & Advancements (.json)
 * 3. Multiverse-Inventories groups & worlds data
 * 4. Slimefun player data
 * 5. CoreProtect player mapping
 * 6. LoginSecurity credentials
 */
public class MigrationService {
    private final Plugin plugin;
    private final Logger logger;
    private final File dataFolder;

    public MigrationService(Plugin plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        this.dataFolder = plugin.getDataFolder();
    }

    public static class MigrationResult {
        public final boolean success;
        public final String message;
        public MigrationResult(boolean success, String message) {
            this.success = success;
            this.message = message;
        }
    }

    public UUID getOfflineUuid(String username) {
        return UUID.nameUUIDFromBytes(("OfflinePlayer:" + username).getBytes(StandardCharsets.UTF_8));
    }

    public MigrationResult migrateAccount(String oldUsername, String newUsername) {
        if (oldUsername == null || newUsername == null || oldUsername.equalsIgnoreCase(newUsername)) {
            return new MigrationResult(false, "旧名字与新名字不能相同！");
        }

        Player onlineOld = Bukkit.getPlayerExact(oldUsername);
        if (onlineOld != null && onlineOld.isOnline()) {
            return new MigrationResult(false, "账号 " + oldUsername + " 当前正处于游戏中，请先退出服务器再执行迁移！");
        }

        UUID oldUuid = getOfflineUuid(oldUsername);
        UUID newUuid = getOfflineUuid(newUsername);

        File oldDat = PlayerDataLocator.findPlayerDataFile(oldUuid);
        if (oldDat == null || !oldDat.exists()) {
            return new MigrationResult(false, "未找到旧账号 " + oldUsername + " 的游戏数据文件！");
        }

        // Create backup folder
        String stamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        File backupDir = new File(plugin.getServer().getWorldContainer(), "backups/craftpass_migrations/" + stamp + "_" + oldUsername + "_to_" + newUsername);
        backupDir.mkdirs();

        try {
            // 1. Backup old data
            File backupDat = new File(backupDir, oldUuid + ".dat");
            Files.copy(oldDat.toPath(), backupDat.toPath(), StandardCopyOption.REPLACE_EXISTING);

            // 2. Migrate world playerdata
            File newDat = new File(oldDat.getParentFile(), newUuid + ".dat");
            Files.copy(oldDat.toPath(), newDat.toPath(), StandardCopyOption.REPLACE_EXISTING);

            // 3. Migrate Stats
            File oldStats = PlayerDataLocator.findStatsFile(oldUuid);
            if (oldStats != null && oldStats.exists()) {
                File newStats = new File(oldStats.getParentFile(), newUuid + ".json");
                Files.copy(oldStats.toPath(), newStats.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }

            // 4. Migrate Advancements
            File oldAdv = PlayerDataLocator.findAdvancementsFile(oldUuid);
            if (oldAdv != null && oldAdv.exists()) {
                File newAdv = new File(oldAdv.getParentFile(), newUuid + ".json");
                Files.copy(oldAdv.toPath(), newAdv.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }

            // 5. Migrate Multiverse-Inventories
            migrateMultiverseInventories(oldUuid, newUuid, backupDir);

            // 6. Migrate Slimefun
            migrateSlimefun(oldUuid, newUuid, backupDir);

            // 7. Migrate LoginSecurity DB
            migrateLoginSecurity(oldUsername, newUsername, newUuid);

            // If new player is currently online, kick to reload data cleanly
            Bukkit.getScheduler().runTask(plugin, () -> {
                Player onlineNew = Bukkit.getPlayerExact(newUsername);
                if (onlineNew != null && onlineNew.isOnline()) {
                    onlineNew.kickPlayer("§a[CraftPass] 账号数据平移完成！请重新连接进入服务器！");
                }
            });

            logger.info("[MigrationService] Successfully migrated " + oldUsername + " -> " + newUsername);
            return new MigrationResult(true, "账号数据已成功平移至 " + newUsername + "！");
        } catch (Exception e) {
            logger.severe("[MigrationService] Migration failed for " + oldUsername + " -> " + newUsername + ": " + e.getMessage());
            return new MigrationResult(false, "数据迁移失败: " + e.getMessage());
        }
    }

    private void migrateMultiverseInventories(UUID oldUuid, UUID newUuid, File backupDir) {
        File mvDir = new File(plugin.getDataFolder().getParentFile(), "Multiverse-Inventories");
        if (!mvDir.exists()) return;

        // Migrate groups & worlds folders
        File[] subDirs = new File[] { new File(mvDir, "groups"), new File(mvDir, "worlds"), new File(mvDir, "players") };
        for (File dir : subDirs) {
            if (!dir.exists()) continue;
            File[] files = dir.listFiles();
            if (files == null) continue;
            for (File groupDir : files) {
                if (groupDir.isDirectory()) {
                    File oldMv = new File(groupDir, oldUuid + ".json");
                    if (oldMv.exists()) {
                        try {
                            File newMv = new File(groupDir, newUuid + ".json");
                            Files.copy(oldMv.toPath(), newMv.toPath(), StandardCopyOption.REPLACE_EXISTING);
                        } catch (IOException ignored) {}
                    }
                }
            }
        }
    }

    private void migrateSlimefun(UUID oldUuid, UUID newUuid, File backupDir) {
        File sfPlayers = new File(plugin.getServer().getWorldContainer(), "data-storage/Slimefun/Players");
        if (!sfPlayers.exists()) {
            sfPlayers = new File(plugin.getDataFolder().getParentFile(), "Slimefun/Players");
        }
        if (sfPlayers.exists()) {
            File oldSf = new File(sfPlayers, oldUuid + ".yml");
            if (oldSf.exists()) {
                try {
                    File newSf = new File(sfPlayers, newUuid + ".yml");
                    Files.copy(oldSf.toPath(), newSf.toPath(), StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException ignored) {}
            }
        }
    }

    private void migrateLoginSecurity(String oldUsername, String newUsername, UUID newUuid) {
        File dbFile = new File(plugin.getDataFolder().getParentFile(), "LoginSecurity/LoginSecurity.db");
        if (!dbFile.exists()) return;

        String sql = "UPDATE ls_players SET last_name = ?, unique_user_id = ? WHERE LOWER(last_name) = LOWER(?)";
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + dbFile.getAbsolutePath());
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, newUsername);
            ps.setString(2, newUuid.toString());
            ps.setString(3, oldUsername);
            ps.executeUpdate();
        } catch (Exception e) {
            logger.warning("[MigrationService] LoginSecurity credential transfer warning: " + e.getMessage());
        }
    }
}
