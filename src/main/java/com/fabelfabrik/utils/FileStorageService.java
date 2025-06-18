package com.fabelfabrik.utils;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.jboss.logging.Logger;

import java.io.File;
import java.io.InputStream;

/**
 * Service for file storage operations.
 * This service delegates to the appropriate FileStorage implementation
 * which is provided by FileStorageProducer.
 */
@ApplicationScoped
public class FileStorageService {

    @Inject
    @Named("selectedFileStorage")
    FileStorage fileStorage;


    /**
     * Store an image file
     */
    public String storeImage(InputStream inputStream, String fileName) {
        return fileStorage.storeImage(inputStream, fileName);
    }

    /**
     * Store a PDF file
     */
    public String storePdf(InputStream inputStream, String fileName) {
        return fileStorage.storePdf(inputStream, fileName);
    }

    /**
     * Store an audio file
     */
    public String storeAudio(InputStream inputStream, String fileName) {
        return fileStorage.storeAudio(inputStream, fileName);
    }

    /**
     * Store a video file
     */
    public String storeVideo(InputStream inputStream, String fileName) {
        return fileStorage.storeVideo(inputStream, fileName);
    }

    /**
     * Get an image file
     */
    public File getImage(String imagePath) {
        return fileStorage.getImage(imagePath);
    }

    /**
     * Get a PDF file
     */
    public File getPdf(String pdfPath) {
        return fileStorage.getPdf(pdfPath);
    }

    /**
     * Get an audio file
     */
    public File getAudio(String audioPath) {
        return fileStorage.getAudio(audioPath);
    }

    /**
     * Get a video file
     */
    public File getVideo(String videoPath) {
        return fileStorage.getVideo(videoPath);
    }
}
