package com.fabelfabrik.utils;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@ApplicationScoped
public class FileStorageService {

    @Inject
    Logger LOG;

    private static final String UPLOAD_DIR = "uploads";
    private static final String IMAGE_DIR = "images";
    private static final String PDF_DIR = "pdfs";
    private static final String AUDIO_DIR = "audio";

    @PostConstruct
    public void init() {
        // Create upload directories if they don't exist
        createDirectories();
    }

    private void createDirectories() {
        try {
            Path uploadPath = Paths.get(UPLOAD_DIR);
            Path imagePath = Paths.get(UPLOAD_DIR, IMAGE_DIR);
            Path pdfPath = Paths.get(UPLOAD_DIR, PDF_DIR);
            Path audioPath = Paths.get(UPLOAD_DIR, AUDIO_DIR);

            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
                LOG.info("Created upload directory: " + uploadPath.toAbsolutePath());
            }

            if (!Files.exists(imagePath)) {
                Files.createDirectories(imagePath);
                LOG.info("Created image directory: " + imagePath.toAbsolutePath());
            }

            if (!Files.exists(pdfPath)) {
                Files.createDirectories(pdfPath);
                LOG.info("Created PDF directory: " + pdfPath.toAbsolutePath());
            }

            if (!Files.exists(audioPath)) {
                Files.createDirectories(audioPath);
                LOG.info("Created audio directory: " + audioPath.toAbsolutePath());
            }
        } catch (IOException e) {
            LOG.error("Failed to create upload directories", e);
            throw new RuntimeException("Could not create upload directories", e);
        }
    }

    /**
     * Generic method to store a file in a specific subdirectory
     */
    private String storeFile(InputStream inputStream, String fileName, String subDir, String fileType) {
        try {
            // Generate a unique filename to prevent collisions
            String fileExtension = "";
            if (fileName.contains(".")) {
                fileExtension = fileName.substring(fileName.lastIndexOf("."));
            }
            String uniqueFileName = UUID.randomUUID().toString() + fileExtension;

            // Create the full path where the file will be stored
            Path targetPath = Paths.get(UPLOAD_DIR, subDir, uniqueFileName);

            // Copy the file to the target location
            Files.copy(inputStream, targetPath, StandardCopyOption.REPLACE_EXISTING);

            LOG.infof("Stored %s: %s (original: %s)", fileType, uniqueFileName, fileName);

            // Return the relative path that can be stored in the database
            return Paths.get(subDir, uniqueFileName).toString();
        } catch (IOException e) {
            LOG.error("Failed to store " + fileType, e);
            throw new RuntimeException("Failed to store " + fileType, e);
        }
    }

    /**
     * Store an image file
     */
    public String storeImage(InputStream inputStream, String fileName) {
        return storeFile(inputStream, fileName, IMAGE_DIR, "image");
    }

    /**
     * Store a PDF file
     */
    public String storePdf(InputStream inputStream, String fileName) {
        return storeFile(inputStream, fileName, PDF_DIR, "PDF");
    }

    /**
     * Store an audio file
     */
    public String storeAudio(InputStream inputStream, String fileName) {
        return storeFile(inputStream, fileName, AUDIO_DIR, "audio");
    }

    /**
     * Generic method to get a file from a specific path
     */
    private File getFile(String filePath, String fileType) {
        Path path = Paths.get(UPLOAD_DIR, filePath);
        File file = path.toFile();

        if (!file.exists() || !file.isFile()) {
            LOG.warnf("%s not found: %s", fileType, filePath);
            return null;
        }

        return file;
    }

    /**
     * Get an image file
     */
    public File getImage(String imagePath) {
        return getFile(imagePath, "Image");
    }

    /**
     * Get a PDF file
     */
    public File getPdf(String pdfPath) {
        return getFile(pdfPath, "PDF");
    }

    /**
     * Get an audio file
     */
    public File getAudio(String audioPath) {
        return getFile(audioPath, "Audio");
    }
}
