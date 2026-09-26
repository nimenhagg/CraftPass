package com.craftpass.config;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.Plugin;

import java.security.SecureRandom;
import java.util.logging.Logger;

/**
 * Manages configuration and automatic secret key generation for CraftPass.
 */
public class PluginConfig {
    private final Plugin plugin;
    private final Logger logger;

    private String bindAddress = "0.0.0.0";
    private int port = 25566;
    private int maxConnectionsPerIp = 5;
    private int timeoutSeconds = 30;

    private String serverPepper;
    private int sessionTtlMinutes = 120;
    private int nonceTtlSeconds = 60;
    private int maxFailedAttempts = 5;
    private int lockoutMinutes = 10;

    private boolean geyserEnabled = true;
    private String bedrockPrefix = ".";
    private boolean preventConcurrentLogin = true;
    private boolean syncInventoryOnQuit = true;

    public PluginConfig(Plugin plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        reload();
    }

    public void reload() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        FileConfiguration config = plugin.getConfig();

        this.bindAddress = config.getString("tcp.bind-address", "0.0.0.0");
        this.port = config.getInt("tcp.port", 25566);
        this.maxConnectionsPerIp = config.getInt("tcp.max-connections-per-ip", 5);
        this.timeoutSeconds = config.getInt("tcp.connection-timeout-seconds", 30);

        this.serverPepper = config.getString("security.server-pepper", "AUTO_GENERATE");
        if (serverPepper == null || serverPepper.isEmpty() || "AUTO_GENERATE".equalsIgnoreCase(serverPepper)) {
            this.serverPepper = generateSecureRandomHex(32); // 256-bit high entropy secret
            config.set("security.server-pepper", this.serverPepper);
            plugin.saveConfig();
            logger.info("[Security] Generated new 256-bit Server Pepper key and stored in config.yml (DO NOT DISCLOSE).");
        }

        this.sessionTtlMinutes = config.getInt("security.session-token-ttl-minutes", 120);
        this.nonceTtlSeconds = config.getInt("security.nonce-ttl-seconds", 60);
        this.maxFailedAttempts = config.getInt("security.max-failed-attempts", 5);
        this.lockoutMinutes = config.getInt("security.lockout-minutes", 10);

        this.geyserEnabled = config.getBoolean("geyser-linking.enabled", true);
        this.bedrockPrefix = config.getString("geyser-linking.bedrock-prefix", ".");
        this.preventConcurrentLogin = config.getBoolean("geyser-linking.prevent-concurrent-login", true);
        this.syncInventoryOnQuit = config.getBoolean("geyser-linking.sync-inventory-on-quit", true);
    }

    private String generateSecureRandomHex(int byteLength) {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[byteLength];
        random.nextBytes(bytes);
        StringBuilder sb = new StringBuilder(byteLength * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    public String getBindAddress() { return bindAddress; }
    public int getPort() { return port; }
    public int getMaxConnectionsPerIp() { return maxConnectionsPerIp; }
    public int getTimeoutSeconds() { return timeoutSeconds; }
    public String getServerPepper() { return serverPepper; }
    public int getSessionTtlMinutes() { return sessionTtlMinutes; }
    public int getNonceTtlSeconds() { return nonceTtlSeconds; }
    public int getMaxFailedAttempts() { return maxFailedAttempts; }
    public int getLockoutMinutes() { return lockoutMinutes; }
    public boolean isGeyserEnabled() { return geyserEnabled; }
    public String getBedrockPrefix() { return bedrockPrefix; }
    public boolean isPreventConcurrentLogin() { return preventConcurrentLogin; }
    public boolean isSyncInventoryOnQuit() { return syncInventoryOnQuit; }
}
