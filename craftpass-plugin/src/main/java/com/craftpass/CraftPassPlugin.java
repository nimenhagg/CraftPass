package com.craftpass;

import com.craftpass.auth.CryptoManager;
import com.craftpass.auth.SessionTokenManager;
import com.craftpass.config.PluginConfig;
import com.craftpass.net.TcpServer;
import com.craftpass.service.GeyserLinkService;
import com.craftpass.service.InventoryService;
import com.craftpass.service.MigrationService;
import com.craftpass.service.PasswordService;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

public class CraftPassPlugin extends JavaPlugin implements CommandExecutor {

    private PluginConfig pluginConfig;
    private CryptoManager cryptoManager;
    private SessionTokenManager sessionTokenManager;
    private InventoryService inventoryService;
    private PasswordService passwordService;
    private MigrationService migrationService;
    private GeyserLinkService geyserLinkService;
    private TcpServer tcpServer;

    @Override
    public void onEnable() {
        // 1. Initialize Configuration
        this.pluginConfig = new PluginConfig(this);

        // 2. Initialize Cryptography and Security
        this.cryptoManager = new CryptoManager(pluginConfig);
        this.sessionTokenManager = new SessionTokenManager(pluginConfig);

        // 3. Initialize Services
        this.inventoryService = new InventoryService();
        this.passwordService = new PasswordService(this, cryptoManager, sessionTokenManager);
        this.migrationService = new MigrationService(this);
        this.geyserLinkService = new GeyserLinkService(this, pluginConfig);

        // 4. Register Event Listeners
        getServer().getPluginManager().registerEvents(geyserLinkService, this);

        // 5. Start TCP Server (Non-HTTP, ICP-exempt)
        this.tcpServer = new TcpServer(this);
        this.tcpServer.start();

        // 6. Register Commands
        if (getCommand("craftpass") != null) {
            getCommand("craftpass").setExecutor(this);
        }

        getLogger().info("====================================================");
        getLogger().info(" CraftPass v" + getDescription().getVersion() + " Enabled successfully!");
        getLogger().info(" TCP Bridge: " + pluginConfig.getBindAddress() + ":" + pluginConfig.getPort());
        getLogger().info(" Pepper Defense: Active (256-bit HMAC-SHA256)");
        getLogger().info(" Wire Encryption: ECDH + AES-256-GCM");
        getLogger().info("====================================================");
    }

    @Override
    public void onDisable() {
        if (tcpServer != null) {
            tcpServer.stop();
        }
        getLogger().info("CraftPass disabled.");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("craftpass.admin")) {
                sender.sendMessage("§c权限不足！");
                return true;
            }
            pluginConfig.reload();
            sender.sendMessage("§a[CraftPass] 配置文件已重载！");
            return true;
        }

        if (args.length > 0 && args[0].equalsIgnoreCase("status")) {
            sender.sendMessage("§6§l=== CraftPass Status ===");
            sender.sendMessage("§ePort: §f" + pluginConfig.getPort());
            sender.sendMessage("§eGeyser Linking: §f" + (pluginConfig.isGeyserEnabled() ? "Enabled (Prefix: '" + pluginConfig.getBedrockPrefix() + "')" : "Disabled"));
            sender.sendMessage("§eSecurity: §fServer-side Pepper + ECDH AES-GCM (Zero-Plaintext Wire)");
            return true;
        }

        sender.sendMessage("§6[CraftPass] §f使用: /craftpass [reload|status]");
        return true;
    }

    public PluginConfig getPluginConfig() { return pluginConfig; }
    public CryptoManager getCryptoManager() { return cryptoManager; }
    public SessionTokenManager getSessionTokenManager() { return sessionTokenManager; }
    public InventoryService getInventoryService() { return inventoryService; }
    public PasswordService getPasswordService() { return passwordService; }
    public MigrationService getMigrationService() { return migrationService; }
    public GeyserLinkService getGeyserLinkService() { return geyserLinkService; }
}
