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
import com.fabelfabrik.utils.FileSystemUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

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
            String fileExtension = getFileExtension(fileName);
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
     * Downloads a file from an S3 bucket given a specific key and file type.
     * The file is stored as a temporary file in the filesystem.
     *
     * @param s3Key The key of the file in the S3 bucket.
     * @param fileType The type of the file to be downloaded (e.g., "image", "pdf").
     * @return A File object representing the downloaded file,
     *         or null if the download fails due to an error.
     */
    private File getFile(String s3Key, String fileType) {
        LOG.infof("=== S3 DOWNLOAD DEBUG ===");
        LOG.infof("Requested S3 key: '%s'", s3Key);
        LOG.infof("File type: %s", fileType);
        LOG.infof("Bucket: %s", bucketName);

        try {
            // Check and clean temp folder if necessary before download
            FileSystemUtils.cleanupTempFolderIfNeeded();

            // Create a temporary file to hold S3-Object
            String fileExtension = getFileExtension(s3Key);
            Path tempFile = Files.createTempFile("s3-download-", fileExtension);

            return s3ToFile(s3Key, fileType, tempFile);

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
     * Downloads a file from an S3 bucket given a specific key and saves it to a temporary file.
     * This method uses an S3 client to fetch the object and writes its contents into a specified
     * temporary file, replacing any existing file at the location.
     *
     * @param s3Key The key of the file in the S3 bucket.
     * @param fileType A description of the file type being downloaded (e.g., "image", "pdf").
     * @param tempFile The path to a temporary file where the downloaded data will be stored.
     * @return The file object corresponding to the downloaded file.
     * @throws IOException If an I/O error occurs during the download or file writing process.
     */
    private File s3ToFile(String s3Key, String fileType, Path tempFile) throws IOException {

        // Build Request
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(s3Key)
                .build();

        LOG.infof("Attempting to download from S3 with key: %s", s3Key);
        try (var s3Object = s3Client.getObject(getObjectRequest)) {
            // Copy the S3 object data to our temporary file
            Files.copy(s3Object, tempFile, StandardCopyOption.REPLACE_EXISTING);
            LOG.infof("Downloaded %s from S3: %s to temp file: %s", fileType, s3Key, tempFile);
            File result = tempFile.toFile();
            LOG.infof("Temp file exists: %s, size: %d", result.exists(), result.length());
            return result;
        }
    }

    /**
     * Extracts the file extension from a given S3 key string.
     * If the S3 key contains a period (.), the method returns the substring
     * starting from the last period to the end of the string. If no period
     * is present, it returns an empty string.
     *
     * @param s3Key The key representing the file in the S3 storage.
     * @return The file extension including the period, or an empty string
     *         if the S3 key does not contain a period.
     */
    private static String getFileExtension(String s3Key) {
        String fileExtension = "";
        if (s3Key.contains(".")) {
            fileExtension = s3Key.substring(s3Key.lastIndexOf("."));
        }
        return fileExtension;
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
