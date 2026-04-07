package com.multimodal.rag.service;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.model.OSSObject;
import com.aliyun.oss.model.PutObjectRequest;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.net.URL;
import java.util.Date;

@Service
@Slf4j
public class OssStorageService {

    @Value("${aliyun.oss.endpoint}")
    private String endpoint;

    @Value("${aliyun.oss.access-key-id}")
    private String accessKeyId;

    @Value("${aliyun.oss.access-key-secret}")
    private String accessKeySecret;

    @Value("${aliyun.oss.bucket-name}")
    private String bucketName;

    private OSS ossClient;

    @PostConstruct
    public void init() {
        try {
            ossClient = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);
            // Create bucket if not exists
            if (!ossClient.doesBucketExist(bucketName)) {
                ossClient.createBucket(bucketName);
                log.info("Created OSS bucket: {}", bucketName);
            }
            log.info("OSS client initialized: endpoint={}, bucket={}", endpoint, bucketName);
        } catch (Exception e) {
            log.warn("Failed to initialize OSS client: {}. File upload will fall back to local storage.", e.getMessage());
            ossClient = null;
        }
    }

    @PreDestroy
    public void destroy() {
        if (ossClient != null) {
            ossClient.shutdown();
        }
    }

    public boolean isAvailable() {
        return ossClient != null;
    }

    /**
     * Upload file to OSS.
     * @return The OSS object key (path)
     */
    public String uploadFile(String objectKey, InputStream inputStream, long contentLength) {
        if (ossClient == null) {
            throw new RuntimeException("OSS client not initialized");
        }
        try {
            PutObjectRequest request = new PutObjectRequest(bucketName, objectKey, inputStream);
            ossClient.putObject(request);
            log.info("Uploaded to OSS: {}", objectKey);
            return objectKey;
        } catch (Exception e) {
            log.error("Failed to upload to OSS: {}", e.getMessage());
            throw new RuntimeException("OSS upload failed", e);
        }
    }

    /**
     * Generate a pre-signed URL for viewing/downloading a file.
     * URL is valid for 1 hour.
     */
    public String generatePresignedUrl(String objectKey) {
        if (ossClient == null) {
            throw new RuntimeException("OSS client not initialized");
        }
        Date expiration = new Date(System.currentTimeMillis() + 3600 * 1000);
        URL url = ossClient.generatePresignedUrl(bucketName, objectKey, expiration);
        return url.toString();
    }

    /**
     * Get file input stream from OSS.
     */
    public InputStream getFileStream(String objectKey) {
        if (ossClient == null) {
            throw new RuntimeException("OSS client not initialized");
        }
        OSSObject ossObject = ossClient.getObject(bucketName, objectKey);
        return ossObject.getObjectContent();
    }

    /**
     * Delete file from OSS.
     */
    public void deleteFile(String objectKey) {
        if (ossClient == null) return;
        try {
            ossClient.deleteObject(bucketName, objectKey);
            log.info("Deleted from OSS: {}", objectKey);
        } catch (Exception e) {
            log.warn("Failed to delete from OSS: {}", e.getMessage());
        }
    }
}