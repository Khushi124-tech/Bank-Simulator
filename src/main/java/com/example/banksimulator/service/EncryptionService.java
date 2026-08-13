package com.example.banksimulator.service;

import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Base64;

@Service
public class EncryptionService {

    private static final String KEY_HASH_ALGORITHM = "SHA-256";

    private static final String CIPHER_ALGORITHM =
            "AES/CBC/PKCS5Padding";

    private static final int AES_KEY_LENGTH = 16;

    public String encrypt(String plainText, String suppliedKey) {

        if (plainText == null) {
            throw new IllegalArgumentException(
                    "Plain text cannot be null"
            );
        }

        try {

            SecretKeySpec key = deriveKey(suppliedKey);

            byte[] ivBytes = key.getEncoded();

            IvParameterSpec iv =
                    new IvParameterSpec(ivBytes);

            Cipher cipher =
                    Cipher.getInstance(CIPHER_ALGORITHM);

            cipher.init(
                    Cipher.ENCRYPT_MODE,
                    key,
                    iv
            );

            byte[] encrypted =
                    cipher.doFinal(
                            plainText.getBytes(
                                    StandardCharsets.UTF_8
                            )
                    );

            return Base64.getEncoder()
                    .encodeToString(encrypted);

        } catch(IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {

            throw new IllegalStateException(
                    "Unable to encrypt transaction data",
                    e
            );
        }
    }

    public String decrypt(
            String encryptedText,
            String suppliedKey) {

        if (encryptedText == null ||
                encryptedText.isBlank()) {

            throw new IllegalArgumentException(
                    "Encrypted text cannot be null or blank"
            );
        }

        try {

            SecretKeySpec key =
                    deriveKey(suppliedKey);

            byte[] ivBytes = key.getEncoded();

            IvParameterSpec iv =
                    new IvParameterSpec(ivBytes);

            Cipher cipher =
                    Cipher.getInstance(CIPHER_ALGORITHM);

            cipher.init(
                    Cipher.DECRYPT_MODE,
                    key,
                    iv
            );

            byte[] encrypted =
                    Base64.getDecoder()
                            .decode(encryptedText);

            byte[] decrypted =
                    cipher.doFinal(encrypted);

            return new String(
                    decrypted,
                    StandardCharsets.UTF_8
            );

        } catch (Exception e) {

            throw new IllegalStateException(
                    "Unable to decrypt transaction data",
                    e
            );
        }
    }
    private SecretKeySpec deriveKey(String suppliedKey) {

        if (suppliedKey == null || suppliedKey.isBlank()) {
            throw new IllegalArgumentException(
                    "Encryption key cannot be null or blank"
            );
        }

        try {

            MessageDigest sha256 =
                    MessageDigest.getInstance(
                            KEY_HASH_ALGORITHM
                    );

            byte[] hash = sha256.digest(
                    suppliedKey.getBytes(
                            StandardCharsets.UTF_8
                    )
            );

            byte[] aesKey = Arrays.copyOf(
                    hash,
                    AES_KEY_LENGTH
            );

            return new SecretKeySpec(
                    aesKey,
                    "AES"
            );

        } catch (NoSuchAlgorithmException e) {

            throw new IllegalStateException(
                    "SHA-256 algorithm is not available",
                    e
            );
        }
    }
}