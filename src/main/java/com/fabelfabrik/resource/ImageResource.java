package com.fabelfabrik.resource;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Response;

import java.io.File;
import java.util.Map;

@Path("/api/image")
public class ImageResource extends AbstractMediaResource {

    private static final Map<String, String> IMAGE_CONTENT_TYPES = Map.of(
            ".jpg", "image/jpeg",
            ".jpeg", "image/jpeg",
            ".png", "image/png",
            ".gif", "image/gif",
            ".webp", "image/webp",
            ".svg", "image/svg+xml"
    );

    @GET
    @Path("/{imagePath: .+}")
    public Response getImage(@PathParam("imagePath") String imagePath) {
        return getMediaFile(imagePath, "image");
    }

    @Override
    protected File getFileFromStorage(String filePath) {
        return fileStorageService.getImage(filePath);
    }

    @Override
    protected Map<String, String> getContentTypeMap() {
        return IMAGE_CONTENT_TYPES;
    }
}