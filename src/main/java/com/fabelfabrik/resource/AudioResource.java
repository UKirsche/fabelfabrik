package com.fabelfabrik.resource;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Response;

import java.io.File;
import java.util.Map;

@Path("/api/audio")
public class AudioResource extends AbstractMediaResource {

    private static final Map<String, String> AUDIO_CONTENT_TYPES = Map.of(
            ".mp3", "audio/mpeg",
            ".wav", "audio/wav",
            ".ogg", "audio/ogg",
            ".m4a", "audio/mp4"
    );

    @GET
    @Path("/{audioPath: .+}")
    public Response getAudio(@PathParam("audioPath") String audioPath) {
        return getMediaFile(audioPath, "audio");
    }

    @Override
    protected File getFileFromStorage(String filePath) {
        return fileStorageService.getAudio(filePath);
    }

    @Override
    protected Map<String, String> getContentTypeMap() {
        return AUDIO_CONTENT_TYPES;
    }
}