package com.skilledup.course.service;

import com.amazonaws.services.cloudfront.CloudFrontUrlSigner;
import com.amazonaws.auth.PEM;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.io.File;
import java.io.FileInputStream;
import java.security.PrivateKey;
import java.util.Date;

@Service
public class CloudFrontService {

    @Value("${aws.cloudfront.domain}")
    private String distributionDomain; // e.g., https://d12345.cloudfront.net

    @Value("${aws.cloudfront.key-pair-id}")
    private String keyPairId; // From AWS Console

    @Value("${aws.cloudfront.private-key-path}")
    private String privateKeyPath; // Path to your .pem file on the server

    public boolean isConfigured() {
        if (distributionDomain == null) {
            return false;
        }
        String domain = distributionDomain.trim();
        if (domain.isEmpty()) {
            return false;
        }
        if (domain.contains("d12345.cloudfront.net")) {
            return false;
        }
        return domain.startsWith("http://") || domain.startsWith("https://");
    }

    public String generateSignedUrl(String s3ObjectKey) {
        try {
            // Expire in 4 hours
            Date expiration = new Date(System.currentTimeMillis() + 1000 * 60 * 60 * 4);

            String resourceUrl = distributionDomain + "/" + s3ObjectKey;
            
            // 1. Define the Wildcard Resource for the Policy
            // This allows access to the entire folder (master.m3u8 AND all .ts segments)
            // URL: https://d123.cloudfront.net/videos/uuid/*
            String policyResourcePath = resourceUrl.substring(0, resourceUrl.lastIndexOf("/")) + "/*";

            // 2. Build Custom Policy JSON
            String policy = buildCustomPolicy(policyResourcePath, expiration);

            // 3. Load Private Key
            // Note: Ensure the .pem file exists at privateKeyPath
            // Use AWS SDK's PEM utility to read the private key (handles PKCS#1 and PKCS#8)
            PrivateKey privateKey;
            try (FileInputStream is = new FileInputStream(new File(privateKeyPath))) {
                privateKey = PEM.readPrivateKey(is);
            }
            
            if (privateKey == null) {
                throw new RuntimeException("Failed to load private key from " + privateKeyPath);
            }

            // 4. Generate Signed URL
            // This appends ?Policy=...&Signature=...&Key-Pair-Id=... to the resourceUrl
            return CloudFrontUrlSigner.getSignedURLWithCustomPolicy(
                resourceUrl,
                keyPairId,
                privateKey,
                policy
            );

        } catch (Exception e) {
            // Fallback for development (or if keys are missing)
            // Note: This will fail in the browser if the bucket is private, but helpful for debugging
            System.err.println("CloudFront signing failed (configuration might be missing): " + e.getMessage());
            return distributionDomain + "/" + s3ObjectKey;
        }
    }

    private String buildCustomPolicy(String resourcePath, Date activeUntil) {
        long activeUntilEpoch = activeUntil.getTime() / 1000;
        // Simple custom policy allowing access until expiration
        return "{\"Statement\":[{\"Resource\":\"" + resourcePath + "\",\"Condition\":{\"DateLessThan\":{\"AWS:EpochTime\":" + activeUntilEpoch + "}}}]}";
    }
}
