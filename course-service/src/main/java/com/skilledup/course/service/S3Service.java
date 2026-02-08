package com.skilledup.course.service;

import com.amazonaws.HttpMethod;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.File;
import java.net.URL;
import java.util.Date;
import java.util.UUID;

@Service
public class S3Service {

    @Value("${aws.s3.access-key}")
    private String accessKey;

    @Value("${aws.s3.secret-key}")
    private String secretKey;

    @Value("${aws.s3.region}")
    private String region;

    @Value("${aws.s3.bucket}")
    private String bucketName;

    private AmazonS3 s3Client;

    @PostConstruct
    private void initializeAmazon() {
        AmazonS3ClientBuilder builder = AmazonS3ClientBuilder.standard()
                .withRegion(region);

        if (accessKey != null && !accessKey.isBlank() && secretKey != null && !secretKey.isBlank()) {
            BasicAWSCredentials credentials = new BasicAWSCredentials(accessKey, secretKey);
            builder = builder.withCredentials(new AWSStaticCredentialsProvider(credentials));
        }

        this.s3Client = builder.build();
    }

    public String uploadFile(MultipartFile file) throws IOException {
        String fileName = UUID.randomUUID().toString() + "_" + file.getOriginalFilename();
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(file.getSize());
        metadata.setContentType(file.getContentType());

        s3Client.putObject(new PutObjectRequest(bucketName, fileName, file.getInputStream(), metadata));
        return fileName; // Return key (filename) to store in DB
    }

    public void uploadFile(File file, String key) {
        PutObjectRequest request = new PutObjectRequest(bucketName, key, file);
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(file.length());

        if (key.endsWith(".m3u8")) {
            metadata.setContentType("application/vnd.apple.mpegurl");
        } else if (key.endsWith(".ts")) {
            metadata.setContentType("video/MP2T");
        }

        request.setMetadata(metadata);
        s3Client.putObject(request);
    }

    public String generatePresignedUrl(String objectKey) {
        Date expiration = new Date();
        long expTimeMillis = expiration.getTime();
        expTimeMillis += 1000 * 60 * 60 * 5; // 5 hours expiration
        expiration.setTime(expTimeMillis);

        GeneratePresignedUrlRequest generatePresignedUrlRequest = new GeneratePresignedUrlRequest(bucketName, objectKey)
                .withMethod(HttpMethod.GET)
                .withExpiration(expiration);

        URL url = s3Client.generatePresignedUrl(generatePresignedUrlRequest);
        return url.toString();
    }

    public S3Object getObject(String key) {
        return s3Client.getObject(bucketName, key);
    }
}
