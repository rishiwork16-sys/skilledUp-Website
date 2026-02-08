package com.skilledup.certificate.service;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

@Service
@RequiredArgsConstructor
@Slf4j
public class S3Service {

    private final AmazonS3 s3Client;

    @Value("${cloud.aws.s3.bucket}")
    private String bucketName;

    public String uploadFile(File file, String folder) {
        String key = folder + "/" + file.getName();

        try {
            s3Client.putObject(new PutObjectRequest(bucketName, key, file));
            String url = generatePresignedUrl(key);
            log.info("File uploaded successfully to S3: {}", key);
            return url;
        } catch (Exception e) {
            log.error("Error uploading file to S3: {}", e.getMessage());
            throw new RuntimeException("Failed to upload file to S3 service");
        }
    }

    public String uploadTemplate(String key, File file) {
        try {
            String normalizedKey = key.startsWith("/") ? key.substring(1) : key;
            s3Client.putObject(new PutObjectRequest(bucketName, normalizedKey, file));
            return normalizedKey;
        } catch (Exception e) {
            log.error("Error uploading template to S3: {}", e.getMessage());
            throw new RuntimeException("Failed to upload template to S3");
        }
    }

    public String generatePresignedUrl(String keyOrUrl) {
        if (keyOrUrl == null || keyOrUrl.isBlank()) return keyOrUrl;
        if (keyOrUrl.startsWith("http://") || keyOrUrl.startsWith("https://")) return keyOrUrl;
        GeneratePresignedUrlRequest request = new GeneratePresignedUrlRequest(bucketName, keyOrUrl);
        request.setExpiration(Date.from(Instant.now().plus(7, ChronoUnit.DAYS)));
        URL url = s3Client.generatePresignedUrl(request);
        return url.toString();
    }

    public InputStream downloadFile(String key) {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("S3 key is required");
        }
        S3Object obj = s3Client.getObject(bucketName, key);
        return obj.getObjectContent();
    }
}
