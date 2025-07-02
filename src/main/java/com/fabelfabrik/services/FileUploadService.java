package com.fabelfabrik.services;

import com.fabelfabrik.model.FileUploadResult;
import com.fabelfabrik.utils.FileStorageService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@ApplicationScoped
public class FileUploadService {

    @Inject
    Logger LOG;

    @Inject
    FileStorageService fileStorageService;

    // Einzelne Methoden für jeden Dateityp
    public FileUploadResult processPdfUpload(InputStream fileStream, String fileName) {
        return processFileUpload(fileStream, fileName,
                fileStorageService::storePdf, "PDF");
    }

    public FileUploadResult processCoverImageUpload(InputStream fileStream, String fileName) {
        return processFileUpload(fileStream, fileName,
                fileStorageService::storeImage, "Bild");
    }

    public FileUploadResult processAudioUpload(InputStream fileStream, String fileName) {
        return processFileUpload(fileStream, fileName,
                fileStorageService::storeAudio, "Audio");
    }

    public FileUploadResult processVideoUpload(InputStream fileStream, String fileName) {
        return processFileUpload(fileStream, fileName,
                fileStorageService::storeVideo, "Video");
    }

    // Methode für mehrere Bilder
    public List<FileUploadResult> processMultipleImagesUpload(List<InputStream> fileStreams, List<String> fileNames) {
        List<FileUploadResult> results = new ArrayList<>();

        if (fileStreams == null || fileNames == null || fileStreams.isEmpty() || fileNames.isEmpty()) {
            return results;
        }

        // Ensure both lists have the same size
        int size = Math.min(fileStreams.size(), fileNames.size());

        for (int i = 0; i < size; i++) {
            InputStream fileStream = fileStreams.get(i);
            String fileName = fileNames.get(i);

            FileUploadResult result = processFileUpload(fileStream, fileName,
                    fileStorageService::storeImage, "Bild");
            results.add(result);
        }

        return results;
    }

    // Generische Upload-Methode
    private FileUploadResult processFileUpload(InputStream fileStream, String fileName,
                                               FileUploadFunction uploadFunction, String fileType) {
        if (fileStream == null || fileName == null) {
            return FileUploadResult.notPresent();
        }

        try {
            String url = uploadFunction.upload(fileStream, fileName);
            LOG.infof("%s stored at: %s", fileType, url);
            return FileUploadResult.success(url);
        } catch (Exception e) {
            LOG.error(fileType + "-Upload fehlgeschlagen", e);
            return FileUploadResult.failure(fileType + "-Upload fehlgeschlagen: " + e.getMessage());
        }
    }

    // Functional Interface für Upload-Funktionen
    @FunctionalInterface
    private interface FileUploadFunction {
        String upload(InputStream stream, String fileName) throws Exception;
    }
}
