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
        log.infof("Retrieving %s: %s", mediaType, filePath);
        
        File file = getFileFromStorage(filePath);
        if (file == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        
        String contentType = determineContentType(file.getName());
        return Response.ok(file, contentType).build();
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