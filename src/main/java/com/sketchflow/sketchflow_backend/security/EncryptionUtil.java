package com.sketchflow.sketchflow_backend.security;

import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Encryption utility using AES-256-GCM for encrypting sensitive data
 * Demonstrates network security concepts: symmetric encryption, authentication tags
 */
@Component
public class EncryptionUtil {

    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int KEY_SIZE = 256;
    private static final int GCM_IV_LENGTH = 12; // 96 bits
    private static final int GCM_TAG_LENGTH = 128; // 128 bits

    // In production, load this from environment variable or secure key store
    private static final String MASTER_KEY_BASE64 = "sketchflow-master-key-256bit-please-change-in-production-env-32bytes";

    private final SecretKey secretKey;
    private final SecureRandom secureRandom;

    public EncryptionUtil() {
        try {
            // Initialize secret key from master key
            byte[] decodedKey = MASTER_KEY_BASE64.getBytes(StandardCharsets.UTF_8);
            byte[] key = new byte[32]; // 256 bits
            System.arraycopy(decodedKey, 0, key, 0, Math.min(decodedKey.length, 32));
            this.secretKey = new SecretKeySpec(key, ALGORITHM);
            this.secureRandom = new SecureRandom();
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize encryption", e);
        }
    }

    /**
     * Encrypt plaintext using AES-256-GCM
     * @param plaintext The data to encrypt
     * @return Base64 encoded ciphertext with IV prepended
     */
    public String encrypt(String plaintext) {
        try {
            // Generate random IV
            byte[] iv = new byte[GCM_IV_LENGTH];
            secureRandom.nextBytes(iv);

            // Create cipher instance
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, parameterSpec);

            // Encrypt the plaintext
            byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

            // Combine IV and ciphertext
            ByteBuffer byteBuffer = ByteBuffer.allocate(iv.length + ciphertext.length);
            byteBuffer.put(iv);
            byteBuffer.put(ciphertext);

            // Return Base64 encoded result
            return Base64.getEncoder().encodeToString(byteBuffer.array());
        } catch (Exception e) {
            throw new RuntimeException("Encryption failed", e);
        }
    }

    /**
     * Decrypt ciphertext using AES-256-GCM
     * @param ciphertext Base64 encoded ciphertext with IV prepended
     * @return Decrypted plaintext
     */
    public String decrypt(String ciphertext) {
        try {
            // Decode Base64
            byte[] decodedBytes = Base64.getDecoder().decode(ciphertext);

            // Extract IV and ciphertext
            ByteBuffer byteBuffer = ByteBuffer.wrap(decodedBytes);
            byte[] iv = new byte[GCM_IV_LENGTH];
            byteBuffer.get(iv);
            byte[] ciphertextBytes = new byte[byteBuffer.remaining()];
            byteBuffer.get(ciphertextBytes);

            // Create cipher instance
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, parameterSpec);

            // Decrypt
            byte[] plaintextBytes = cipher.doFinal(ciphertextBytes);

            return new String(plaintextBytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("Decryption failed", e);
        }
    }

    /**
     * Generate a new AES key (for reference/testing)
     */
    public static String generateKey() {
        try {
            KeyGenerator keyGenerator = KeyGenerator.getInstance(ALGORITHM);
            keyGenerator.init(KEY_SIZE);
            SecretKey key = keyGenerator.generateKey();
            return Base64.getEncoder().encodeToString(key.getEncoded());
        } catch (Exception e) {
            throw new RuntimeException("Key generation failed", e);
        }
    }

    /**
     * Hash sensitive data for comparison (one-way)
     */
    public String hash(String data) {
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            throw new RuntimeException("Hashing failed", e);
        }
    }
}

