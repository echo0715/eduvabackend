package com.eduva.eduva.service;

import com.eduva.eduva.model.SubmissionData;
import com.qcloud.cos.COSClient;
import com.qcloud.cos.ClientConfig;
import com.qcloud.cos.auth.BasicCOSCredentials;
import com.qcloud.cos.auth.COSCredentials;
import com.qcloud.cos.endpoint.UserSpecifiedEndpointBuilder;
import com.qcloud.cos.http.HttpMethodName;
import com.qcloud.cos.http.HttpProtocol;
import com.qcloud.cos.model.*;
import com.qcloud.cos.region.Region;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

@Service
public class FileStorageService {

    @Value("${tencent.cos.secret-id}")
    private String secretId;

    @Value("${tencent.cos.secret-key}")
    private String secretKey;

    @Value("${tencent.cos.bucket}")
    private String bucketName;

    @Value("${tencent.cos.region}")
    private String region;

    @Value("${tencent.cos.custom-domain}")
    private String customDomain;



    private COSClient cosClient;

    @PostConstruct
    public void init() {
        // Initialize COS Client
        COSCredentials credentials = new BasicCOSCredentials(secretId, secretKey);
        ClientConfig clientConfig = new ClientConfig(new Region(region));
        clientConfig.setHttpProtocol(HttpProtocol.https);

        String serviceApiEndpoint = "service.cos.myqcloud.com";
        String userEndpoint = customDomain;
        UserSpecifiedEndpointBuilder endPointBuilder = new UserSpecifiedEndpointBuilder(userEndpoint, serviceApiEndpoint);
        clientConfig.setEndpointBuilder(endPointBuilder);

        this.cosClient = new COSClient(credentials, clientConfig);
    }



    public String storeFile(MultipartFile file, Long roleId, Long assignmentId) throws IOException {
        try {
            // Generate a unique file name
            String originalFileName = file.getOriginalFilename();
            String fileExtension = getFileExtension(originalFileName);
            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            String uniqueFileName = String.format("submissions/%d/assignment_%d/%s_%s%s",
                    roleId,
                    assignmentId,
                    timestamp,
                    UUID.randomUUID().toString(),
                    fileExtension);

            // Set metadata
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(file.getSize());
            metadata.setContentType(file.getContentType());
            metadata.setContentDisposition("inline");

            // Create upload request
            PutObjectRequest putObjectRequest = new PutObjectRequest(
                    bucketName,
                    uniqueFileName,
                    file.getInputStream(),
                    metadata
            );

            // Upload file to COS
            cosClient.putObject(putObjectRequest);

            return uniqueFileName;

        } catch (IOException e) {
            throw new IOException("Failed to store file to Tencent COS", e);
        }
    }


    public String generatePresignedUrl(String fileName) {
        try {
            Date expirationTime = new Date(System.currentTimeMillis() + 3600 * 1000);
            GeneratePresignedUrlRequest request =
                    new GeneratePresignedUrlRequest(bucketName, fileName, HttpMethodName.GET);
            request.setExpiration(expirationTime);

            ResponseHeaderOverrides overrides = new ResponseHeaderOverrides();
            overrides.setContentType("application/pdf");
            overrides.setContentDisposition("inline");
            request.setResponseHeaders(overrides);

            URL url = cosClient.generatePresignedUrl(request);
            return url.toString();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate file URL", e);
        }
    }

    public String generateImagePresignedUrl(String fileName) {
        try {
            Date expirationTime = new Date(System.currentTimeMillis() + 3600 * 1000);
            GeneratePresignedUrlRequest request =
                    new GeneratePresignedUrlRequest(bucketName, fileName, HttpMethodName.GET);
            request.setExpiration(expirationTime);

            // Determine the content type based on file extension
            String contentType = determineContentType(fileName);

            ResponseHeaderOverrides overrides = new ResponseHeaderOverrides();
            overrides.setContentType(contentType);
            overrides.setContentDisposition("inline");
            request.setResponseHeaders(overrides);

            URL url = cosClient.generatePresignedUrl(request);
            return url.toString();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate file URL", e);
        }
    }

    // Helper method to determine content type based on file extension
    private String determineContentType(String fileName) {
        String extension = "";
        int i = fileName.lastIndexOf('.');
        if (i > 0) {
            extension = fileName.substring(i + 1).toLowerCase();
        }

        switch (extension) {
            case "jpg":
            case "jpeg":
                return "image/jpeg";
            case "png":
                return "image/png";
            case "gif":
                return "image/gif";
            case "svg":
                return "image/svg+xml";
            case "webp":
                return "image/webp";
            case "bmp":
                return "image/bmp";
            case "tiff":
            case "tif":
                return "image/tiff";
            default:
                // Default to a generic image type or octet-stream if extension is unknown
                return "image/*";
        }
    }


    private String getFileExtension(String fileName) {
        if (fileName == null || fileName.lastIndexOf(".") == -1) {
            return "";
        }
        return fileName.substring(fileName.lastIndexOf("."));
    }
}
