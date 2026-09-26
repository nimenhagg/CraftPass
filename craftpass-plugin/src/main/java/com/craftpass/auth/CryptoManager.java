package com.craftpass.auth;

import com.craftpass.config.PluginConfig;

import javax.crypto.Cipher;
import javax.crypto.KeyAgreement;
import javax.crypto.Mac;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Handles cryptographic operations:
 * 1. Server-side Pepper HMAC-SHA256 (Defends against offline GPU brute-force on public databases).
 * 2. Ephemeral ECDH (Elliptic-Curve Diffie-Hellman) key exchange for wire encryption.
 * 3. AES-256-GCM packet payload encryption (Zero plaintext on network).
 * 4. Challenge-Response Nonce generation.
 */
public class CryptoManager {
    private final PluginConfig config;
    private final SecureRandom secureRandom = new SecureRandom();
    private final KeyPair serverEcdhKeyPair;
    private final String serverPublicKeyBase64;

    // Active challenge nonces: nonce -> NonceRecord
    private final ConcurrentHashMap<String, NonceRecord> activeNonces = new ConcurrentHashMap<>();

    public static class NonceRecord {
        public final String username;
        public final long expiresAt;
        public NonceRecord(String username, long expiresAt) {
            this.username = username;
            this.expiresAt = expiresAt;
        }
    }

    public CryptoManager(PluginConfig config) {
        this.config = config;
        try {
            KeyPairGenerator kpg = KeyPairGenerator.getInstance("EC");
            kpg.initialize(256);
            this.serverEcdhKeyPair = kpg.generateKeyPair();
            this.serverPublicKeyBase64 = Base64.getEncoder().encodeToString(serverEcdhKeyPair.getPublic().getEncoded());
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize ECDH key pair", e);
        }
    }

    public String getServerPublicKeyBase64() {
        return serverPublicKeyBase64;
    }

    /**
     * Derives a 256-bit AES symmetric key from client's public key using ECDH.
     */
    public byte[] deriveSharedKey(String clientPublicKeyBase64) throws Exception {
        byte[] clientKeyBytes = Base64.getDecoder().decode(clientPublicKeyBase64);
        KeyFactory kf = KeyFactory.getInstance("EC");
        PublicKey clientPubKey = kf.generatePublic(new X509EncodedKeySpec(clientKeyBytes));

        KeyAgreement ka = KeyAgreement.getInstance("ECDH");
        ka.init(serverEcdhKeyPair.getPrivate());
        ka.doPhase(clientPubKey, true);
        byte[] sharedSecret = ka.generateSecret();

        // Hash shared secret with SHA-256 to produce clean 256-bit AES key
        MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
        return sha256.digest(sharedSecret);
    }

    /**
     * Decrypts AES-256-GCM ciphertext.
     * Expects payload: [12 bytes IV] + [ciphertext + 16 bytes GCM tag] in Base64
     */
    public String decryptAesGcm(String base64Encrypted, byte[] sharedKey) throws Exception {
        byte[] combined = Base64.getDecoder().decode(base64Encrypted);
        if (combined.length < 28) {
            throw new IllegalArgumentException("Invalid encrypted payload length");
        }
        byte[] iv = new byte[12];
        System.arraycopy(combined, 0, iv, 0, 12);
        int cipherLen = combined.length - 12;
        byte[] cipherBytes = new byte[cipherLen];
        System.arraycopy(combined, 12, cipherBytes, 0, cipherLen);

        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        SecretKeySpec keySpec = new SecretKeySpec(sharedKey, "AES");
        GCMParameterSpec spec = new GCMParameterSpec(128, iv);
        cipher.init(Cipher.DECRYPT_MODE, keySpec, spec);

        byte[] plainBytes = cipher.doFinal(cipherBytes);
        return new String(plainBytes, StandardCharsets.UTF_8);
    }

    /**
     * Encrypts plaintext using AES-256-GCM.
     */
    public String encryptAesGcm(String plainText, byte[] sharedKey) throws Exception {
        byte[] iv = new byte[12];
        secureRandom.nextBytes(iv);

        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        SecretKeySpec keySpec = new SecretKeySpec(sharedKey, "AES");
        GCMParameterSpec spec = new GCMParameterSpec(128, iv);
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, spec);

        byte[] cipherBytes = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
        byte[] combined = new byte[12 + cipherBytes.length];
        System.arraycopy(iv, 0, combined, 0, 12);
        System.arraycopy(cipherBytes, 0, combined, 12, cipherBytes.length);

        return Base64.getEncoder().encodeToString(combined);
    }

    /**
     * Computes HMAC-SHA256 with the secret Server Pepper.
     * This ensures that even with a public database, offline brute force is prevented.
     */
    public String pepperPassword(String rawPassword) {
        return hmacSha256(rawPassword, config.getServerPepper());
    }

    public String hmacSha256(String data, String key) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKey);
            byte[] hmacBytes = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(hmacBytes.length * 2);
            for (byte b : hmacBytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("HMAC-SHA256 computation failed", e);
        }
    }

    /**
     * Creates a single-use random challenge nonce.
     */
    public String createChallengeNonce(String username) {
        byte[] bytes = new byte[24];
        secureRandom.nextBytes(bytes);
        String nonce = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        long expiresAt = System.currentTimeMillis() + (config.getNonceTtlSeconds() * 1000L);
        activeNonces.put(nonce, new NonceRecord(username, expiresAt));
        cleanExpiredNonces();
        return nonce;
    }

    public boolean validateAndConsumeNonce(String nonce, String username) {
        if (nonce == null) return false;
        NonceRecord record = activeNonces.remove(nonce);
        if (record == null) return false;
        if (System.currentTimeMillis() > record.expiresAt) return false;
        return record.username.equalsIgnoreCase(username);
    }

    private void cleanExpiredNonces() {
        long now = System.currentTimeMillis();
        activeNonces.entrySet().removeIf(entry -> now > entry.getValue().expiresAt);
    }
}
