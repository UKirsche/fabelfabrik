package com.fabelfabrik.resource;

import com.fabelfabrik.utils.FileStorageService;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;

import java.io.File;
import java.util.Map;

public abstract class AbstractMediaResource {

    protected final Logger log;

    @Inject
    protected FileStorageService fileStorageService;

    protected AbstractMediaResource() {
        this.log = Logger.getLogger(this.getClass());
    }

    protected Response getMediaFile(String filePath, String mediaType) {
        // Normalisiere den Pfad - entferne führende Slashes und doppelte Slashes
        String normalizedPath = normalizePath(filePath);
        
        log.infof("=== MEDIA REQUEST DEBUG ===");
        log.infof("Original path: '%s'", filePath);
        log.infof("Normalized path: '%s'", normalizedPath);
        log.infof("Media type: %s", mediaType);
        log.infof("FileStorageService class: %s", fileStorageService.getClass().getName());
        
        File file = getFileFromStorage(normalizedPath);
        
        log.infof("File retrieved: %s", file != null ? file.getAbsolutePath() : "NULL");
        
        if (file == null) {
            log.errorf("File not found for path: '%s' (normalized: '%s')", filePath, normalizedPath);
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        
        log.infof("File exists: %s, Size: %d bytes", file.exists(), file.length());
        
        String contentType = determineContentType(file.getName());
        log.infof("Content type: %s", contentType);
        
        return Response.ok(file, contentType).build();
    }

    private String normalizePath(String path) {
        if (path == null || path.isEmpty()) {
            return path;
        }
        
        // Entferne führende Slashes
        String normalized = path.replaceAll("^/+", "");
        // Ersetze mehrfache Slashes durch einzelne
        normalized = normalized.replaceAll("/+", "/");
        
        return normalized;
    }

    protected abstract File getFileFromStorage(String filePath);
    
    protected abstract Map<String, String> getContentTypeMap();

    private String determineContentType(String fileName) {
        String lowerCaseFileName = fileName.toLowerCase();
        
        return getContentTypeMap().entrySet().stream()
                .filter(entry -> lowerCaseFileName.endsWith(entry.getKey()))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse("application/octet-stream");
    }
}