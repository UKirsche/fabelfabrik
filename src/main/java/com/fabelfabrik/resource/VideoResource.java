package com.fabelfabrik.resource;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Response;

import java.io.File;
import java.util.Map;

@Path("/api/video")
public class VideoResource extends AbstractMediaResource {

    private static final Map<String, String> VIDEO_CONTENT_TYPES = Map.of(
            ".mp4", "video/mp4",
            ".webm", "video/webm",
            ".avi", "video/x-msvideo",
            ".mov", "video/quicktime",
            ".mkv", "video/x-matroska"
    );

    @GET
    @Path("/{videoPath: .+}")
    public Response getVideo(@PathParam("videoPath") String videoPath) {
        return getMediaFile(videoPath, "video");
    }

    @Override
    protected File getFileFromStorage(String filePath) {
        return fileStorageService.getVideo(filePath);
    }

    @Override
    protected Map<String, String> getContentTypeMap() {
        return VIDEO_CONTENT_TYPES;
    }
}