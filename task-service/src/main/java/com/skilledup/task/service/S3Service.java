package com.skilledup.task.service;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import jakarta.annotation.PostConstruct;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.UUID;

@Service
public class S3Service {

    @Value("${cloud.aws.credentials.access-key}")
    private String accessKey;

    @Value("${cloud.aws.credentials.secret-key}")
    private String secretKey;

    @Value("${cloud.aws.region.static}")
    private String region;

    @Value("${cloud.aws.s3.bucket}")
    private String bucketName;

    private AmazonS3 s3Client;

    @PostConstruct
    private void initializeAmazon() {
        AmazonS3ClientBuilder builder = AmazonS3ClientBuilder.standard()
                .withRegion(region)
                .withPathStyleAccessEnabled(true);

        if (accessKey != null && !accessKey.isBlank() && secretKey != null && !secretKey.isBlank()) {
            AWSCredentials credentials = new BasicAWSCredentials(accessKey, secretKey);
            builder = builder.withCredentials(new AWSStaticCredentialsProvider(credentials));
        }

        this.s3Client = builder.build();
    }

    public String uploadFile(MultipartFile file) {
        String fileName = generateFileName(file);
        try {
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(file.getSize());
            metadata.setContentType(file.getContentType());

            PutObjectRequest putObjectRequest = new PutObjectRequest(bucketName, fileName, file.getInputStream(),
                    metadata);

            s3Client.putObject(putObjectRequest);

            return s3Client.getUrl(bucketName, fileName).toString();
        } catch (IOException e) {
            throw new RuntimeException("Error while uploading file to S3", e);
        }
    }

    private String generateFileName(MultipartFile file) {
        return UUID.randomUUID().toString() + "_" + file.getOriginalFilename().replace(" ", "_");
    }

    public String uploadFromUrl(String urlString) {
        try {
            URL url = new URL(urlString);
            URLConnection connection = url.openConnection();
            connection.setRequestProperty("User-Agent", "Mozilla/5.0");
            connection.connect();

            String fileName = generateFileNameFromUrl(urlString);

            try (InputStream inputStream = connection.getInputStream()) {
                ObjectMetadata metadata = new ObjectMetadata();

                // Get content length
                long contentLength = connection.getContentLengthLong();
                if (contentLength > 0) {
                    metadata.setContentLength(contentLength);
                } else {
                    // If content length is unknown, read into byte array first
                    byte[] bytes = inputStream.readAllBytes();
                    metadata.setContentLength(bytes.length);

                    String contentType = connection.getContentType();
                    if (contentType != null) {
                        metadata.setContentType(contentType);
                    } else {
                        metadata.setContentType("application/octet-stream");
                    }

                    PutObjectRequest putObjectRequest = new PutObjectRequest(bucketName, fileName,
                            new java.io.ByteArrayInputStream(bytes), metadata);
                    s3Client.putObject(putObjectRequest);
                    return s3Client.getUrl(bucketName, fileName).toString();
                }

                String contentType = connection.getContentType();
                if (contentType != null) {
                    metadata.setContentType(contentType);
                } else {
                    metadata.setContentType("application/octet-stream");
                }

                PutObjectRequest putObjectRequest = new PutObjectRequest(bucketName, fileName, inputStream, metadata);
                s3Client.putObject(putObjectRequest);

                return s3Client.getUrl(bucketName, fileName).toString();
            }
        } catch (Exception e) {
            throw new RuntimeException("Error while uploading file from URL to S3: " + e.getMessage(), e);
        }
    }

    private String generateFileNameFromUrl(String urlString) {
        String fileName = urlString.substring(urlString.lastIndexOf('/') + 1);
        if (fileName.contains("?")) {
            fileName = fileName.substring(0, fileName.indexOf('?'));
        }
        if (fileName.isEmpty() || !fileName.contains(".")) {
            fileName = "downloaded_file_" + System.currentTimeMillis();
        }
        return UUID.randomUUID().toString() + "_" + fileName.replace(" ", "_");
    }

    public String generateSignedUrl(String fileUrl) {
        try {
            // Extract filename from S3 URL
            String fileName = extractFileNameFromUrl(fileUrl);

            // Generate presigned URL valid for 1 hour
            java.util.Date expiration = new java.util.Date();
            long expTimeMillis = expiration.getTime();
            expTimeMillis += 1000 * 60 * 60; // 1 hour
            expiration.setTime(expTimeMillis);

            com.amazonaws.services.s3.model.GeneratePresignedUrlRequest generatePresignedUrlRequest = new com.amazonaws.services.s3.model.GeneratePresignedUrlRequest(
                    bucketName, fileName)
                    .withMethod(com.amazonaws.HttpMethod.GET)
                    .withExpiration(expiration);

            URL url = s3Client.generatePresignedUrl(generatePresignedUrlRequest);
            return url.toString();
        } catch (Exception e) {
            throw new RuntimeException("Error generating signed URL: " + e.getMessage(), e);
        }
    }

    public void deleteFile(String fileUrl) {
        try {
            String fileName = extractFileNameFromUrl(fileUrl);
            s3Client.deleteObject(bucketName, fileName);
        } catch (Exception e) {
            throw new RuntimeException("Error deleting file from S3: " + e.getMessage(), e);
        }
    }

    private String extractFileNameFromUrl(String fileUrl) {
        // Extract filename from full S3 URL
        // Format: https://bucket.s3.region.amazonaws.com/filename
        // or: https://s3.region.amazonaws.com/bucket/filename
        try {
            URL url = new URL(fileUrl);
            String path = url.getPath();
            // Decode the path to handle special characters like spaces (%20), etc.
            String decodedPath = java.net.URLDecoder.decode(path, java.nio.charset.StandardCharsets.UTF_8.name());
            // Remove leading slash and bucket name if present
            String fileName = decodedPath.substring(decodedPath.lastIndexOf('/') + 1);
            return fileName;
        } catch (Exception e) {
            // If URL parsing fails, try simple extraction (decoding as well)
            try {
                String decodedUrl = java.net.URLDecoder.decode(fileUrl, java.nio.charset.StandardCharsets.UTF_8.name());
                return decodedUrl.substring(decodedUrl.lastIndexOf('/') + 1);
            } catch (Exception ex) {
                return fileUrl.substring(fileUrl.lastIndexOf('/') + 1);
            }
        }
    }
}
