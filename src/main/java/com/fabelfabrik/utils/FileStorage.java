package com.fabelfabrik.utils;

import java.io.File;
import java.io.InputStream;

/**
 * Interface for file storage operations.
 * This interface defines the operations needed for storing and retrieving files,
 * regardless of the underlying storage mechanism (local file system or S3).
 */
public interface FileStorage {

    /**
     * Initialize the storage system
     */
    void init();

    /**
     * Store an image file
     * @param inputStream The input stream of the file
     * @param fileName The original file name
     * @return The relative path or identifier of the stored file
     */
    String storeImage(InputStream inputStream, String fileName);

    /**
     * Store a PDF file
     * @param inputStream The input stream of the file
     * @param fileName The original file name
     * @return The relative path or identifier of the stored file
     */
    String storePdf(InputStream inputStream, String fileName);

    /**
     * Store an audio file
     * @param inputStream The input stream of the file
     * @param fileName The original file name
     * @return The relative path or identifier of the stored file
     */
    String storeAudio(InputStream inputStream, String fileName);

    /**
     * Store a video file
     * @param inputStream The input stream of the file
     * @param fileName The original file name
     * @return The relative path or identifier of the stored file
     */
    String storeVideo(InputStream inputStream, String fileName);

    /**
     * Get an image file
     * @param imagePath The relative path or identifier of the file
     * @return The file object or null if not found
     */
    File getImage(String imagePath);

    /**
     * Get a PDF file
     * @param pdfPath The relative path or identifier of the file
     * @return The file object or null if not found
     */
    File getPdf(String pdfPath);

    /**
     * Get an audio file
     * @param audioPath The relative path or identifier of the file
     * @return The file object or null if not found
     */
    File getAudio(String audioPath);

    /**
     * Get a video file
     * @param videoPath The relative path or identifier of the file
     * @return The file object or null if not found
     */
    File getVideo(String videoPath);
}