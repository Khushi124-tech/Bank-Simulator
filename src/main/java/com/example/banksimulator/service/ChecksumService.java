package com.example.banksimulator.service;

import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

@Service
public class ChecksumService {

    public String generateChecksum(String transactionData) {

        if (transactionData == null) {
            throw new IllegalArgumentException(
                    "Transaction data cannot be null"
            );
        }

        try {
            MessageDigest md = MessageDigest.getInstance("MD5");

            byte[] digest = md.digest(
                    transactionData.getBytes(StandardCharsets.UTF_8)
            );

            return toHex(digest);

        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(
                    "MD5 algorithm is not available",
                    e
            );
        }
    }

    private String toHex(byte[] bytes) {

        StringBuilder result = new StringBuilder();

        for (byte b : bytes) {
            result.append(String.format("%02x", b & 0xff));
        }

        return result.toString();
    }
}