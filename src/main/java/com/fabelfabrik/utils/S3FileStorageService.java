package com.fabelfabrik.utils;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Alternative;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

/**
 * AWS S3 implementation of the FileStorage interface.
 * This implementation stores files in an AWS S3 bucket.
 * 
 * Note: This is a basic implementation that simulates S3 storage.
 * In a real implementation, you would use the AWS S3 SDK to interact with S3.
 */
@ApplicationScoped
@S3Storage
public class S3FileStorageService implements FileStorage {

    @Inject
    Logger LOG;

    @ConfigProperty(name = "my.upload.aws.access.key", defaultValue = "test-access-key")
    String awsAccessKey;

    @ConfigProperty(name = "my.upload.aws.secret.key", defaultValue = "test-secret-key")
    String awsSecretKey;

    @ConfigProperty(name = "my.upload.aws.region", defaultValue = "test-region")
    String awsRegion;

    @ConfigProperty(name = "my.upload.bucket.name", defaultValue = "test-bucket")
    String bucketName;

    // Temporary local directories for simulating S3 storage
    private static final String TEMP_DIR = "s3-temp";
    private static final String IMAGE_DIR = "images";
    private static final String PDF_DIR = "pdfs";
    private static final String AUDIO_DIR = "audio";
    private static final String VIDEO_DIR = "videos";

    @PostConstruct
    @Override
    public void init() {
        LOG.info("Initializing S3 file storage service");
        LOG.info("AWS Region: " + awsRegion);
        LOG.info("S3 Bucket: " + bucketName);

        // In a real implementation, you would initialize the S3 client here
        // For now, we'll create temporary directories to simulate S3 storage
        createTempDirectories();
    }

    private void createTempDirectories() {
        try {
            Path tempPath = Paths.get(TEMP_DIR);
            Path imagePath = Paths.get(TEMP_DIR, IMAGE_DIR);
            Path pdfPath = Paths.get(TEMP_DIR, PDF_DIR);
            Path audioPath = Paths.get(TEMP_DIR, AUDIO_DIR);
            Path videoPath = Paths.get(TEMP_DIR, VIDEO_DIR);

            if (!Files.exists(tempPath)) {
                Files.createDirectories(tempPath);
                LOG.info("Created temp directory: " + tempPath.toAbsolutePath());
            }

            if (!Files.exists(imagePath)) {
                Files.createDirectories(imagePath);
            }

            if (!Files.exists(pdfPath)) {
                Files.createDirectories(pdfPath);
            }

            if (!Files.exists(audioPath)) {
                Files.createDirectories(audioPath);
            }

            if (!Files.exists(videoPath)) {
                Files.createDirectories(videoPath);
            }
        } catch (IOException e) {
            LOG.error("Failed to create temp directories", e);
            throw new RuntimeException("Could not create temp directories", e);
        }
    }

    /**
     * Generic method to store a file in a specific S3 path
     */
    private String storeFile(InputStream inputStream, String fileName, String subDir, String fileType) {
        try {
            // Generate a unique filename to prevent collisions
            String fileExtension = "";
            if (fileName.contains(".")) {
                fileExtension = fileName.substring(fileName.lastIndexOf("."));
            }
            String uniqueFileName = UUID.randomUUID().toString() + fileExtension;

            // In a real implementation, you would upload the file to S3 here
            // For now, we'll store it in a temporary directory
            Path targetPath = Paths.get(TEMP_DIR, subDir, uniqueFileName);
            Files.copy(inputStream, targetPath, StandardCopyOption.REPLACE_EXISTING);

            LOG.infof("Stored %s in S3: %s (original: %s)", fileType, uniqueFileName, fileName);

            // Return the S3 path that can be stored in the database
            // In a real implementation, this would be the S3 object key
            return subDir + "/" + uniqueFileName;
        } catch (IOException e) {
            LOG.error("Failed to store " + fileType + " in S3", e);
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
    private File getFile(String filePath, String fileType) {
        // In a real implementation, you would download the file from S3 here
        // For now, we'll retrieve it from the temporary directory
        Path path = Paths.get(TEMP_DIR, filePath);
        File file = path.toFile();

        if (!file.exists() || !file.isFile()) {
            LOG.warnf("%s not found in S3: %s", fileType, filePath);
            return null;
        }

        return file;
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
