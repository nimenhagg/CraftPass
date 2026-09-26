package com.craftpass.net;

import com.craftpass.CraftPassPlugin;
import com.craftpass.auth.CryptoManager;
import com.craftpass.auth.SessionTokenManager;
import com.craftpass.service.GeyserLinkService;
import com.craftpass.service.InventoryService;
import com.craftpass.service.MigrationService;
import com.craftpass.service.PasswordService;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * Handles a single client TCP socket connection.
 */
public class ClientSession implements Runnable {
    private final CraftPassPlugin plugin;
    private final Socket socket;
    private final Logger logger;
    private final Gson gson = new Gson();

    private DataInputStream in;
    private DataOutputStream out;
    private byte[] sharedKey = null; // Derived via ECDH
    private volatile boolean running = true;

    public ClientSession(CraftPassPlugin plugin, Socket socket) {
        this.plugin = plugin;
        this.socket = socket;
        this.logger = plugin.getLogger();
    }

    @Override
    public void run() {
        String clientIp = socket.getInetAddress().getHostAddress();
        try {
            socket.setSoTimeout(plugin.getPluginConfig().getTimeoutSeconds() * 1000);
            this.in = new DataInputStream(socket.getInputStream());
            this.out = new DataOutputStream(socket.getOutputStream());

            while (running && !socket.isClosed()) {
                ProtocolPacket packet;
                try {
                    packet = ProtocolPacket.readFrom(in);
                } catch (SocketTimeoutException | EOFException e) {
                    break; // Connection closed or timed out
                }

                ProtocolPacket response = handlePacket(packet, clientIp);
                if (response != null) {
                    response.writeTo(out);
                }
            }
        } catch (Exception e) {
            // Socket error or disconnect
        } finally {
            close();
        }
    }

    private ProtocolPacket handlePacket(ProtocolPacket packet, String clientIp) {
        try {
            JsonObject req = JsonParser.parseString(packet.getPayloadJson()).getAsJsonObject();
            JsonObject resp = new JsonObject();

            switch (packet.getType()) {
                case HANDSHAKE_REQ: {
                    String username = req.has("username") ? req.get("username").getAsString() : "";
                    String clientPubKey = req.has("clientPublicKey") ? req.get("clientPublicKey").getAsString() : "";

                    if (plugin.getSessionTokenManager().isIpLocked(clientIp)) {
                        resp.addProperty("status", "LOCKED");
                        resp.addProperty("message", "IP 暂时已被锁定，请 10 分钟后再试！");
                        return new ProtocolPacket(PacketType.HANDSHAKE_RESP, gson.toJson(resp));
                    }

                    if (!clientPubKey.isEmpty()) {
                        this.sharedKey = plugin.getCryptoManager().deriveSharedKey(clientPubKey);
                    }

                    String nonce = plugin.getCryptoManager().createChallengeNonce(username);
                    resp.addProperty("status", "OK");
                    resp.addProperty("nonce", nonce);
                    resp.addProperty("serverPublicKey", plugin.getCryptoManager().getServerPublicKeyBase64());
                    return new ProtocolPacket(PacketType.HANDSHAKE_RESP, gson.toJson(resp));
                }

                case AUTH_REQ: {
                    String username = req.get("username").getAsString();
                    String nonce = req.get("nonce").getAsString();

                    if (plugin.getSessionTokenManager().isIpLocked(clientIp)) {
                        resp.addProperty("status", "LOCKED");
                        resp.addProperty("message", "尝试次数过多，IP 已被临时锁定！");
                        return new ProtocolPacket(PacketType.AUTH_RESP, gson.toJson(resp));
                    }

                    if (!plugin.getCryptoManager().validateAndConsumeNonce(nonce, username)) {
                        resp.addProperty("status", "INVALID_NONCE");
                        resp.addProperty("message", "验证令牌无效或已过期，请重新登录！");
                        return new ProtocolPacket(PacketType.AUTH_RESP, gson.toJson(resp));
                    }

                    String rawPassword = "";
                    if (req.has("encryptedPayload") && sharedKey != null) {
                        String decrypted = plugin.getCryptoManager().decryptAesGcm(req.get("encryptedPayload").getAsString(), sharedKey);
                        JsonObject decJson = JsonParser.parseString(decrypted).getAsJsonObject();
                        rawPassword = decJson.get("password").getAsString();
                    } else if (req.has("password")) {
                        rawPassword = req.get("password").getAsString();
                    }

                    boolean authenticated = plugin.getPasswordService().authenticate(username, rawPassword);
                    if (authenticated) {
                        plugin.getSessionTokenManager().resetFailedAttempts(clientIp);
                        UUID uuid = UUID.nameUUIDFromBytes(("OfflinePlayer:" + username).getBytes(StandardCharsets.UTF_8));
                        SessionTokenManager.Session session = plugin.getSessionTokenManager().createSession(username, uuid);

                        resp.addProperty("status", "SUCCESS");
                        resp.addProperty("token", session.token);
                        resp.addProperty("username", username);
                        resp.addProperty("uuid", uuid.toString());
                        resp.addProperty("expiresAt", session.expiresAt);
                    } else {
                        plugin.getSessionTokenManager().recordFailedAttempt(clientIp);
                        resp.addProperty("status", "FAILED");
                        resp.addProperty("message", "用户名或密码错误！");
                    }
                    return new ProtocolPacket(PacketType.AUTH_RESP, gson.toJson(resp));
                }

                case GET_INVENTORY_REQ: {
                    SessionTokenManager.Session session = authenticateSession(req);
                    if (session == null) return unauthenticatedResponse();

                    InventoryService.InventoryDto inv = plugin.getInventoryService().getPlayerInventory(session.username, session.playerUuid);
                    resp.addProperty("status", "SUCCESS");
                    resp.add("data", gson.toJsonTree(inv));
                    return new ProtocolPacket(PacketType.GET_INVENTORY_RESP, gson.toJson(resp));
                }

                case CHANGE_PASSWORD_REQ: {
                    SessionTokenManager.Session session = authenticateSession(req);
                    if (session == null) return unauthenticatedResponse();

                    String oldPass = "";
                    String newPass = "";
                    if (req.has("encryptedPayload") && sharedKey != null) {
                        String dec = plugin.getCryptoManager().decryptAesGcm(req.get("encryptedPayload").getAsString(), sharedKey);
                        JsonObject decJson = JsonParser.parseString(dec).getAsJsonObject();
                        oldPass = decJson.get("oldPassword").getAsString();
                        newPass = decJson.get("newPassword").getAsString();
                    } else {
                        oldPass = req.get("oldPassword").getAsString();
                        newPass = req.get("newPassword").getAsString();
                    }

                    boolean success = plugin.getPasswordService().changePassword(session.username, oldPass, newPass);
                    resp.addProperty("status", success ? "SUCCESS" : "FAILED");
                    resp.addProperty("message", success ? "密码修改成功！请重新使用新密码登录。" : "旧密码错误或新密码不符合规范(6-32位)！");
                    return new ProtocolPacket(PacketType.CHANGE_PASSWORD_RESP, gson.toJson(resp));
                }

                case MIGRATE_ACCOUNT_REQ: {
                    SessionTokenManager.Session session = authenticateSession(req);
                    if (session == null) return unauthenticatedResponse();

                    String targetUsername = req.get("targetNewUsername").getAsString();
                    MigrationService.MigrationResult res = plugin.getMigrationService().migrateAccount(session.username, targetUsername);
                    resp.addProperty("status", res.success ? "SUCCESS" : "FAILED");
                    resp.addProperty("message", res.message);
                    return new ProtocolPacket(PacketType.MIGRATE_ACCOUNT_RESP, gson.toJson(resp));
                }

                case GET_GEYSER_LINK_REQ: {
                    SessionTokenManager.Session session = authenticateSession(req);
                    if (session == null) return unauthenticatedResponse();

                    String linked = plugin.getGeyserLinkService().getLinkedAccount(session.username);
                    resp.addProperty("status", "SUCCESS");
                    resp.addProperty("isLinked", linked != null);
                    resp.addProperty("linkedAccount", linked != null ? linked : "");
                    resp.addProperty("bedrockPrefix", plugin.getPluginConfig().getBedrockPrefix());
                    return new ProtocolPacket(PacketType.GET_GEYSER_LINK_RESP, gson.toJson(resp));
                }

                case LINK_GEYSER_REQ: {
                    SessionTokenManager.Session session = authenticateSession(req);
                    if (session == null) return unauthenticatedResponse();

                    String bedrockName = req.get("bedrockUsername").getAsString();
                    String bedrockPass = "";
                    if (req.has("encryptedPayload") && sharedKey != null) {
                        String dec = plugin.getCryptoManager().decryptAesGcm(req.get("encryptedPayload").getAsString(), sharedKey);
                        JsonObject decJson = JsonParser.parseString(dec).getAsJsonObject();
                        bedrockPass = decJson.get("bedrockPassword").getAsString();
                    } else if (req.has("bedrockPassword")) {
                        bedrockPass = req.get("bedrockPassword").getAsString();
                    }

                    // Verify Bedrock credentials
                    if (!plugin.getPasswordService().authenticate(bedrockName, bedrockPass)) {
                        resp.addProperty("status", "FAILED");
                        resp.addProperty("message", "基岩版账号密码校验失败！请检查用户名与密码。");
                        return new ProtocolPacket(PacketType.LINK_GEYSER_RESP, gson.toJson(resp));
                    }

                    boolean ok = plugin.getGeyserLinkService().linkAccounts(session.username, bedrockName);
                    resp.addProperty("status", ok ? "SUCCESS" : "FAILED");
                    resp.addProperty("message", ok ? "已成功绑定基岩版账号 " + bedrockName + "！" : "绑定失败！");
                    return new ProtocolPacket(PacketType.LINK_GEYSER_RESP, gson.toJson(resp));
                }

                case UNLINK_GEYSER_REQ: {
                    SessionTokenManager.Session session = authenticateSession(req);
                    if (session == null) return unauthenticatedResponse();

                    boolean ok = plugin.getGeyserLinkService().unlinkAccount(session.username);
                    resp.addProperty("status", ok ? "SUCCESS" : "FAILED");
                    resp.addProperty("message", ok ? "已解除双端绑定。" : "解除绑定失败。");
                    return new ProtocolPacket(PacketType.UNLINK_GEYSER_RESP, gson.toJson(resp));
                }

                case HEARTBEAT: {
                    resp.addProperty("pong", System.currentTimeMillis());
                    return new ProtocolPacket(PacketType.HEARTBEAT, gson.toJson(resp));
                }

                default:
                    resp.addProperty("status", "UNKNOWN_OP");
                    return new ProtocolPacket(PacketType.ERROR, gson.toJson(resp));
            }
        } catch (Exception e) {
            JsonObject err = new JsonObject();
            err.addProperty("status", "ERROR");
            err.addProperty("message", e.getMessage());
            return new ProtocolPacket(PacketType.ERROR, gson.toJson(err));
        }
    }

    private SessionTokenManager.Session authenticateSession(JsonObject req) {
        if (!req.has("token")) return null;
        String token = req.get("token").getAsString();
        return plugin.getSessionTokenManager().getSession(token);
    }

    private ProtocolPacket unauthenticatedResponse() {
        JsonObject err = new JsonObject();
        err.addProperty("status", "UNAUTHORIZED");
        err.addProperty("message", "登录会话已过期，请重新登录！");
        return new ProtocolPacket(PacketType.ERROR, gson.toJson(err));
    }

    public void close() {
        running = false;
        try {
            if (!socket.isClosed()) socket.close();
        } catch (IOException ignored) {}
    }
}
