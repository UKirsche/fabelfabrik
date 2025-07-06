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
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

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
     * Generic method to get a file from S3 with temp space management
     */
    private File getFile(String s3Key, String fileType) {
        LOG.infof("=== S3 DOWNLOAD DEBUG ===");
        LOG.infof("Requested S3 key: '%s'", s3Key);
        LOG.infof("File type: %s", fileType);
        LOG.infof("Bucket: %s", bucketName);

        try {
            // Check and clean temp folder if necessary before download
            cleanupTempFolderIfNeeded();

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

            // Get the object as ResponseInputStream instead of downloading to file
            try (var s3Object = s3Client.getObject(getObjectRequest)) {
                // Copy the S3 object data to our temporary file
                Files.copy(s3Object, tempFile, StandardCopyOption.REPLACE_EXISTING);

                LOG.infof("Downloaded %s from S3: %s to temp file: %s", fileType, s3Key, tempFile);
                File result = tempFile.toFile();
                LOG.infof("Temp file exists: %s, size: %d", result.exists(), result.length());
                return result;
            }

        } catch (Exception e) {
            LOG.errorf(e, "Failed to get %s from S3 with key: '%s'", fileType, s3Key);
            LOG.errorf("Error details: %s", e.getMessage());
            if (e.getCause() != null) {
                LOG.errorf("Cause: %s", e.getCause().getMessage());
            }
            return null;
        }
    }

    /**
     * Cleans up temp folder if it exceeds 1.9GB by removing S3 download files
     */
    private void cleanupTempFolderIfNeeded() {
        try {
            Path tempDir = Paths.get(System.getProperty("java.io.tmpdir"));
            long maxSizeBytes = 1_900_000_000L; // 1.9GB in bytes
        
            long currentSize = calculateDirectorySize(tempDir);
            LOG.infof("Current temp directory size: %d bytes (%.2f GB)", currentSize, currentSize / 1_000_000_000.0);
        
            if (currentSize > maxSizeBytes) {
                LOG.infof("Temp directory exceeds 1.9GB, cleaning up S3 download files...");
            
                // Delete all files starting with "s3-download-"
                try (var files = Files.walk(tempDir, 1)) {
                    List<Path> s3Files = files
                        .filter(Files::isRegularFile)
                        .filter(path -> path.getFileName().toString().startsWith("s3-download-"))
                        .toList();
                
                    long deletedSize = 0;
                    int deletedCount = 0;
                
                    for (Path file : s3Files) {
                        try {
                            long fileSize = Files.size(file);
                            Files.delete(file);
                            deletedSize += fileSize;
                            deletedCount++;
                            LOG.infof("Deleted S3 temp file: %s (size: %d bytes)", file.getFileName(), fileSize);
                        } catch (IOException e) {
                            LOG.warnf("Failed to delete S3 temp file: %s - %s", file.getFileName(), e.getMessage());
                        }
                    }
                
                    LOG.infof("Cleanup completed: deleted %d S3 files, freed %d bytes (%.2f GB)", 
                        deletedCount, deletedSize, deletedSize / 1_000_000_000.0);
                
                    // Log new size after cleanup
                    long newSize = calculateDirectorySize(tempDir);
                    LOG.infof("New temp directory size after cleanup: %d bytes (%.2f GB)", newSize, newSize / 1_000_000_000.0);
                }
            }
        } catch (Exception e) {
            LOG.errorf(e, "Error during temp folder cleanup: %s", e.getMessage());
        }
    }

    /**
     * Calculates the total size of a directory
     */
    private long calculateDirectorySize(Path directory) {
        try (var files = Files.walk(directory, 1)) {
            return files
                .filter(Files::isRegularFile)
                .mapToLong(path -> {
                    try {
                        return Files.size(path);
                    } catch (IOException e) {
                        LOG.warnf("Could not get size of file: %s - %s", path, e.getMessage());
                        return 0L;
                    }
                })
                .sum();
        } catch (IOException e) {
            LOG.errorf(e, "Error calculating directory size: %s", e.getMessage());
            return 0L;
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

    /**
     * Generic method to delete a file from S3
     */
    private boolean deleteFile(String s3Key, String fileType) {
        LOG.infof("=== S3 DELETE DEBUG ===");
        LOG.infof("Deleting S3 key: '%s'", s3Key);
        LOG.infof("File type: %s", fileType);
        LOG.infof("Bucket: %s", bucketName);

        try {
            // Delete the object from S3
            s3Client.deleteObject(builder -> builder
                .bucket(bucketName)
                .key(s3Key)
                .build());

            LOG.infof("Successfully deleted %s from S3: '%s'", fileType, s3Key);
            return true;
        } catch (Exception e) {
            LOG.errorf(e, "Failed to delete %s from S3 with key: '%s'", fileType, s3Key);
            LOG.errorf("Error details: %s", e.getMessage());
            if (e.getCause() != null) {
                LOG.errorf("Cause: %s", e.getCause().getMessage());
            }
            return false;
        }
    }

    @Override
    public boolean deleteImage(String imagePath) {
        return deleteFile(imagePath, "Image");
    }

    @Override
    public boolean deletePdf(String pdfPath) {
        return deleteFile(pdfPath, "PDF");
    }

    @Override
    public boolean deleteAudio(String audioPath) {
        return deleteFile(audioPath, "Audio");
    }

    @Override
    public boolean deleteVideo(String videoPath) {
        return deleteFile(videoPath, "Video");
    }
}