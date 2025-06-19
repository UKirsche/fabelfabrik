package com.fabelfabrik.utils;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

/**
 * AWS S3 implementation of the FileStorage interface.
 * This implementation stores files in an AWS S3 bucket using the AWS SDK.
 */
@ApplicationScoped
@S3Storage
public class S3FileStorageService implements FileStorage {

    @Inject
    Logger LOG;

    @ConfigProperty(name = "my.upload.aws.access.key")
    String awsAccessKey;

    @ConfigProperty(name = "my.upload.aws.secret.key")
    String awsSecretKey;

    @ConfigProperty(name = "my.upload.aws.region")
    String awsRegion;

    @ConfigProperty(name = "my.upload.bucket.name")
    String bucketName;

    private S3Client s3Client;

    // Directory prefixes for different file types
    private static final String IMAGE_DIR = "images";
    private static final String PDF_DIR = "pdfs";
    private static final String AUDIO_DIR = "audio";
    private static final String VIDEO_DIR = "videos";

    @PostConstruct
    public void init() {
        LOG.info("Initializing S3 file storage service");
        LOG.info("AWS Region: " + awsRegion);
        LOG.info("S3 Bucket: " + bucketName);

        try {
            // Initialize the S3 client with credentials
            this.s3Client = S3Client.builder()
                    .region(Region.of(awsRegion))
                    .credentialsProvider(StaticCredentialsProvider.create(
                            AwsBasicCredentials.create(awsAccessKey, awsSecretKey)
                    ))
                    .build();

            LOG.info("S3 client initialized successfully");
        } catch (Exception e) {
            LOG.error("Failed to initialize S3 client", e);
            throw new RuntimeException("Could not initialize S3 client", e);
        }
    }

    /**
     * Generic method to store a file in a specific S3 path
     */
    private String storeFile(InputStream inputStream, String fileName, String subDir, String fileType) {
        LOG.infof("=== S3 UPLOAD DEBUG ===");
        LOG.infof("Original filename: '%s'", fileName);
        LOG.infof("Sub directory: '%s'", subDir);
        LOG.infof("File type: %s", fileType);
        LOG.infof("Bucket: %s", bucketName);
        
        try {
            // Generate a unique filename to prevent collisions
            String fileExtension = "";
            if (fileName.contains(".")) {
                fileExtension = fileName.substring(fileName.lastIndexOf("."));
            }
            String uniqueFileName = UUID.randomUUID() + fileExtension;

            // Create the S3 object key
            String s3Key = subDir + "/" + uniqueFileName;
            
            LOG.infof("Generated unique filename: '%s'", uniqueFileName);
            LOG.infof("Generated S3 key: '%s'", s3Key);

            // Create a temporary file to upload to S3
            Path tempFile = Files.createTempFile("s3-upload-", fileExtension);
            LOG.infof("Created temp file: %s", tempFile);
            
            try {
                // Copy the input stream to the temporary file
                Files.copy(inputStream, tempFile, StandardCopyOption.REPLACE_EXISTING);
                LOG.infof("Copied input stream to temp file, size: %d bytes", Files.size(tempFile));

                // Upload the file to S3
                PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                        .bucket(bucketName)
                        .key(s3Key)
                        .build();

                LOG.infof("Uploading to S3 with key: '%s'", s3Key);
                s3Client.putObject(putObjectRequest, tempFile);

                LOG.infof("Successfully stored %s in S3: '%s' (original: '%s')", fileType, s3Key, fileName);

                // Return the S3 object key for database storage
                return s3Key;
            } finally {
                // Clean up the temporary file
                try {
                    Files.deleteIfExists(tempFile);
                    LOG.infof("Cleaned up temp file: %s", tempFile);
                } catch (IOException e) {
                    LOG.warn("Failed to delete temporary file: " + tempFile, e);
                }
            }
        } catch (Exception e) {
            LOG.errorf(e, "Failed to store %s in S3", fileType);
            LOG.errorf("Error details: %s", e.getMessage());
            throw new RuntimeException("Failed to store " + fileType + " in S3", e);
        }
    }

    @Override
    public String storeImage(InputStream inputStream, String fileName) {
        return storeFile(inputStream, fileName, IMAGE_DIR, "image");
    }

    @Override
    public String storePdf(InputStream inputStream, String fileName) {
        return storeFile(inputStream, fileName, PDF_DIR, "PDF");
    }

    @Override
    public String storeAudio(InputStream inputStream, String fileName) {
        return storeFile(inputStream, fileName, AUDIO_DIR, "audio");
    }

    @Override
    public String storeVideo(InputStream inputStream, String fileName) {
        return storeFile(inputStream, fileName, VIDEO_DIR, "video");
    }

    /**
     * Generic method to get a file from S3
     */
    private File getFile(String s3Key, String fileType) {
        LOG.infof("=== S3 DOWNLOAD DEBUG ===");
        LOG.infof("Requested S3 key: '%s'", s3Key);
        LOG.infof("File type: %s", fileType);
        LOG.infof("Bucket: %s", bucketName);
        
        try {
            // Create a temporary file to download the S3 object
            String fileExtension = "";
            if (s3Key.contains(".")) {
                fileExtension = s3Key.substring(s3Key.lastIndexOf("."));
            }
            Path tempFile = Files.createTempFile("s3-download-", fileExtension);

            // Download the file from S3
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(s3Key)
                    .build();

            LOG.infof("Attempting to download from S3 with key: %s", s3Key);
            s3Client.getObject(getObjectRequest, tempFile);

            LOG.infof("Downloaded %s from S3: %s to temp file: %s", fileType, s3Key, tempFile);
            File result = tempFile.toFile();
            LOG.infof("Temp file exists: %s, size: %d", result.exists(), result.length());
            return result;

        } catch (Exception e) {
            LOG.errorf(e, "Failed to get %s from S3 with key: '%s'", fileType, s3Key);
            LOG.errorf("Error details: %s", e.getMessage());
            if (e.getCause() != null) {
                LOG.errorf("Cause: %s", e.getCause().getMessage());
            }
            return null;
        }
    }

    @Override
    public File getImage(String imagePath) {
        return getFile(imagePath, "Image");
    }

    @Override
    public File getPdf(String pdfPath) {
        return getFile(pdfPath, "PDF");
    }

    @Override
    public File getAudio(String audioPath) {
        return getFile(audioPath, "Audio");
    }

    @Override
    public File getVideo(String videoPath) {
        return getFile(videoPath, "Video");
    }
}