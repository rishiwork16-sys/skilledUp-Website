package com.skilledup.student.service;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.util.StringUtils;

import java.io.InputStream;
import java.io.IOException;
import java.net.URL;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

@Service
public class S3Service {

    private final AmazonS3 s3Client;

    @Value("${cloud.aws.s3.bucket}")
    private String bucketName;

    public S3Service(AmazonS3 s3Client) {
        this.s3Client = s3Client;
    }

    public String uploadFile(MultipartFile file) {
        String originalFilename = StringUtils.cleanPath(file.getOriginalFilename());
        String keyName = "uploads/" + System.currentTimeMillis() + "_" + originalFilename.replace(" ", "_");

        try {
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(file.getSize());
            metadata.setContentType(file.getContentType());
            try (InputStream inputStream = file.getInputStream()) {
                s3Client.putObject(bucketName, keyName, inputStream, metadata);
            }
            return keyName;
        } catch (Exception e) {
            throw new RuntimeException("Error while uploading file to S3", e);
        }
    }

    public String generatePresignedUrl(String keyOrUrl) {
        if (keyOrUrl == null || keyOrUrl.isBlank()) return keyOrUrl;
        if (keyOrUrl.startsWith("http://") || keyOrUrl.startsWith("https://")) return keyOrUrl;
        GeneratePresignedUrlRequest request = new GeneratePresignedUrlRequest(bucketName, keyOrUrl);
        Date expiration = Date.from(Instant.now().plus(7, ChronoUnit.DAYS));
        request.setExpiration(expiration);
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

    public String uploadFile(java.io.File file) {
        String keyName = "uploads/" + System.currentTimeMillis() + "_" + file.getName().replace(" ", "_");
        return uploadFile(keyName, file);
    }

    public String uploadFile(String keyName, java.io.File file) {
        try {
            String normalizedKey = keyName.startsWith("/") ? keyName.substring(1) : keyName;
            normalizedKey = normalizedKey.replace(" ", "_");
            s3Client.putObject(new PutObjectRequest(bucketName, normalizedKey, file));
            return normalizedKey;
        } catch (Exception e) {
            throw new RuntimeException("Error while uploading file to S3", e);
        }
    }
}
