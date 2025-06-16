package com.fabelfabrik.utils;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
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

    @PostConstruct
    public void init() {
        // Create upload directories if they don't exist
        createDirectories();
    }

    private void createDirectories() {
        try {
            Path uploadPath = Paths.get(UPLOAD_DIR);
            Path imagePath = Paths.get(UPLOAD_DIR, IMAGE_DIR);
            
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
                LOG.info("Created upload directory: " + uploadPath.toAbsolutePath());
            }
            
            if (!Files.exists(imagePath)) {
                Files.createDirectories(imagePath);
                LOG.info("Created image directory: " + imagePath.toAbsolutePath());
            }
        } catch (IOException e) {
            LOG.error("Failed to create upload directories", e);
            throw new RuntimeException("Could not create upload directories", e);
        }
    }

    public String storeImage(InputStream inputStream, String fileName) {
        try {
            // Generate a unique filename to prevent collisions
            String fileExtension = "";
            if (fileName.contains(".")) {
                fileExtension = fileName.substring(fileName.lastIndexOf("."));
            }
            String uniqueFileName = UUID.randomUUID().toString() + fileExtension;
            
            // Create the full path where the file will be stored
            Path targetPath = Paths.get(UPLOAD_DIR, IMAGE_DIR, uniqueFileName);
            
            // Copy the file to the target location
            Files.copy(inputStream, targetPath, StandardCopyOption.REPLACE_EXISTING);
            
            LOG.infof("Stored image: %s (original: %s)", uniqueFileName, fileName);
            
            // Return the relative path that can be stored in the database
            return Paths.get(IMAGE_DIR, uniqueFileName).toString();
        } catch (IOException e) {
            LOG.error("Failed to store image", e);
            throw new RuntimeException("Failed to store image", e);
        }
    }

    public File getImage(String imagePath) {
        Path path = Paths.get(UPLOAD_DIR, imagePath);
        File file = path.toFile();
        
        if (!file.exists() || !file.isFile()) {
            LOG.warnf("Image not found: %s", imagePath);
            return null;
        }
        
        return file;
    }
}