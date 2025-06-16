package com.fabelfabrik.resource;

import com.fabelfabrik.utils.FileStorageService;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.RestForm;
import org.jboss.resteasy.reactive.multipart.FileUpload;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@Path("/api/images")
public class ImageResource {

    private static final Logger LOG = Logger.getLogger(ImageResource.class);

    @Inject
    FileStorageService fileStorageService;

    @POST
    @Path("/upload")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    public Response uploadImage(@RestForm("file") FileUpload file) {
        LOG.infof("Received image upload request: %s", file.fileName());
        
        try (InputStream inputStream = file.uploadedFile().toFile().toURI().toURL().openStream()) {
            String storedPath = fileStorageService.storeImage(inputStream, file.fileName());
            LOG.infof("Image stored at: %s", storedPath);
            
            return Response.ok().entity("{\"path\": \"" + storedPath + "\"}").build();
        } catch (IOException e) {
            LOG.error("Failed to process uploaded file", e);
            return Response.serverError().entity("{\"error\": \"Failed to process uploaded file\"}").build();
        }
    }

    @POST
    @Path("/upload-multiple")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    public Response uploadMultipleImages(@RestForm("files") List<FileUpload> files) {
        LOG.infof("Received multiple image upload request: %d files", files.size());
        
        List<String> storedPaths = new ArrayList<>();
        
        for (FileUpload file : files) {
            try (InputStream inputStream = file.uploadedFile().toFile().toURI().toURL().openStream()) {
                String storedPath = fileStorageService.storeImage(inputStream, file.fileName());
                storedPaths.add(storedPath);
                LOG.infof("Image stored at: %s", storedPath);
            } catch (IOException e) {
                LOG.error("Failed to process uploaded file: " + file.fileName(), e);
                // Continue with other files even if one fails
            }
        }
        
        return Response.ok().entity("{\"paths\": " + storedPaths + "}").build();
    }

    @GET
    @Path("/{imagePath: .+}")
    public Response getImage(@PathParam("imagePath") String imagePath) {
        LOG.infof("Retrieving image: %s", imagePath);
        
        File file = fileStorageService.getImage(imagePath);
        if (file == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        
        String contentType = determineContentType(file.getName());
        return Response.ok(file, contentType).build();
    }
    
    private String determineContentType(String fileName) {
        if (fileName.toLowerCase().endsWith(".jpg") || fileName.toLowerCase().endsWith(".jpeg")) {
            return "image/jpeg";
        } else if (fileName.toLowerCase().endsWith(".png")) {
            return "image/png";
        } else if (fileName.toLowerCase().endsWith(".gif")) {
            return "image/gif";
        } else if (fileName.toLowerCase().endsWith(".webp")) {
            return "image/webp";
        } else {
            return "application/octet-stream";
        }
    }
}