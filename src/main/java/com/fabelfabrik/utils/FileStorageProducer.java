package com.fabelfabrik.utils;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

/**
 * Producer for FileStorage implementation.
 * This producer selects the appropriate FileStorage implementation based on the active profile.
 */
@ApplicationScoped
public class FileStorageProducer {

    @Inject
    Logger LOG;

    @Inject
    @ConfigProperty(name = "quarkus.profile", defaultValue = "dev")
    String activeProfile;

    @Inject
    @LocalStorage
    FileStorage localFileStorage;

    @Inject
    @S3Storage
    FileStorage s3FileStorage;

    /**
     * Produces the appropriate FileStorage implementation based on the active profile.
     * Uses S3FileStorageService for production and LocalFileStorageService for development.
     * 
     * @return The appropriate FileStorage implementation
     */
    @Produces
    @ApplicationScoped
    @Named("selectedFileStorage")
    public FileStorage produceFileStorage() {
        FileStorage fileStorage;
        
        if ("prd".equals(activeProfile)) {
            LOG.info("Using S3 file storage for production environment");
            try {
                fileStorage = s3FileStorage;
            } catch (Exception e) {
                LOG.warn("S3FileStorageService not available, falling back to local storage: " + e.getMessage());
                fileStorage = localFileStorage;
            }
        } else {
            LOG.info("Using local file storage for development environment");
            fileStorage = localFileStorage;
        }

        return fileStorage;
    }
}