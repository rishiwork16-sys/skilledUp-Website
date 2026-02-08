package com.skilledup.career.service;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.util.StringUtils;

import java.io.InputStream;
import java.net.URL;
import java.util.Date;

@Service
public class S3Service {

    private final AmazonS3 s3Client;

    @Value("${cloud.aws.s3.bucket}")
    private String bucketName;

    public S3Service(AmazonS3 s3Client) {
        this.s3Client = s3Client;
    }

    public String uploadFile(MultipartFile file, String folder) {
        String originalFilename = StringUtils.cleanPath(file.getOriginalFilename());
        String keyName = folder + "/" + System.currentTimeMillis() + "_" + originalFilename.replace(" ", "_");

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

    public String generatePresignedUrl(String keyName) {
        if (keyName == null || keyName.isBlank()) {
            return keyName;
        }
        if (keyName.startsWith("http://") || keyName.startsWith("https://")) {
            return keyName;
        }

        Date expiration = new Date();
        long expTimeMillis = expiration.getTime();
        expTimeMillis += 1000 * 60 * 60; // 1 hour
        expiration.setTime(expTimeMillis);

        try {
            URL url = s3Client.generatePresignedUrl(bucketName, keyName, expiration);
            return url.toString();
        } catch (Exception e) {
            throw new RuntimeException("Error generating presigned URL", e);
        }
    }
}
