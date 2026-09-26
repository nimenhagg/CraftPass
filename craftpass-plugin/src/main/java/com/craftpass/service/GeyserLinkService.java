package com.craftpass.service;

import com.craftpass.config.PluginConfig;
import com.craftpass.util.PlayerDataLocator;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * Handles Geyser Bedrock <-> Java dual-identity linking:
 * 1. Mutual link verification
 * 2. Mutex online check (prevents duplicate login / item duping)
 * 3. Automatic inventory and progression sync upon quit
 */
public class GeyserLinkService implements Listener {
    private final Plugin plugin;
    private final PluginConfig config;
    private final Logger logger;
    private final File linkFile;
    private YamlConfiguration linkConfig;

    public GeyserLinkService(Plugin plugin, PluginConfig config) {
        this.plugin = plugin;
        this.config = config;
        this.logger = plugin.getLogger();
        this.linkFile = new File(plugin.getDataFolder(), "linked_accounts.yml");
        loadLinks();
    }

    private synchronized void loadLinks() {
        if (!linkFile.exists()) {
            try {
                linkFile.getParentFile().mkdirs();
                linkFile.createNewFile();
            } catch (IOException ignored) {}
        }
        this.linkConfig = YamlConfiguration.loadConfiguration(linkFile);
    }

    private synchronized void saveLinks() {
        try {
            linkConfig.save(linkFile);
        } catch (IOException e) {
            logger.warning("[GeyserLinkService] Failed to save linked accounts: " + e.getMessage());
        }
    }

    public synchronized String getLinkedAccount(String username) {
        String lower = username.toLowerCase();
        // Check if username is Java
        String bedrock = linkConfig.getString("java_to_bedrock." + lower);
        if (bedrock != null) return bedrock;

        // Check if username is Bedrock
        return linkConfig.getString("bedrock_to_java." + lower);
    }

    public synchronized boolean linkAccounts(String javaName, String bedrockName) {
        if (javaName == null || bedrockName == null) return false;
        String prefix = config.getBedrockPrefix();
        if (!bedrockName.startsWith(prefix) && javaName.startsWith(prefix)) {
            // Swap if user entered them reversed
            String temp = javaName;
            javaName = bedrockName;
            bedrockName = temp;
        }

        linkConfig.set("java_to_bedrock." + javaName.toLowerCase(), bedrockName);
        linkConfig.set("bedrock_to_java." + bedrockName.toLowerCase(), javaName);
        saveLinks();

        // Perform initial sync from Java to Bedrock
        syncPlayerData(javaName, bedrockName);
        logger.info("[GeyserLinkService] Linked Java account '" + javaName + "' <-> Bedrock account '" + bedrockName + "'");
        return true;
    }

    public synchronized boolean unlinkAccount(String username) {
        String linked = getLinkedAccount(username);
        if (linked == null) return false;

        linkConfig.set("java_to_bedrock." + username.toLowerCase(), null);
        linkConfig.set("java_to_bedrock." + linked.toLowerCase(), null);
        linkConfig.set("bedrock_to_java." + username.toLowerCase(), null);
        linkConfig.set("bedrock_to_java." + linked.toLowerCase(), null);
        saveLinks();
        return true;
    }

    private void syncPlayerData(String sourceName, String targetName) {
        try {
            UUID srcUuid = UUID.nameUUIDFromBytes(("OfflinePlayer:" + sourceName).getBytes(StandardCharsets.UTF_8));
            UUID tgtUuid = UUID.nameUUIDFromBytes(("OfflinePlayer:" + targetName).getBytes(StandardCharsets.UTF_8));

            File srcDat = PlayerDataLocator.findPlayerDataFile(srcUuid);
            if (srcDat != null && srcDat.exists()) {
                File tgtDat = new File(srcDat.getParentFile(), tgtUuid + ".dat");
                Files.copy(srcDat.toPath(), tgtDat.toPath(), StandardCopyOption.REPLACE_EXISTING);
                logger.info("[GeyserLinkService] Synced playerdata " + sourceName + " -> " + targetName);
            }
        } catch (Exception e) {
            logger.warning("[GeyserLinkService] Error syncing playerdata: " + e.getMessage());
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPreLogin(AsyncPlayerPreLoginEvent event) {
        if (!config.isGeyserEnabled() || !config.isPreventConcurrentLogin()) return;

        String name = event.getName();
        String counterpart = getLinkedAccount(name);
        if (counterpart != null) {
            Player p = Bukkit.getPlayerExact(counterpart);
            if (p != null && p.isOnline()) {
                event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER,
                        "§c[CraftPass] 已绑定的双端账号 (" + counterpart + ") 当前正在游戏中！\n§e为确保数据安全，同一时间仅允许一个终端在线。");
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        if (!config.isGeyserEnabled() || !config.isSyncInventoryOnQuit()) return;

        String name = event.getPlayer().getName();
        String counterpart = getLinkedAccount(name);
        if (counterpart != null) {
            // Player data is saved by server around quit, delay 1 second to ensure disk flush
            Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, () -> {
                syncPlayerData(name, counterpart);
            }, 20L);
        }
    }
}
