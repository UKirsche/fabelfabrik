package com.fabelfabrik.resource;

import com.fabelfabrik.utils.FileStorageService;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;

import java.io.File;

@Path("/api/audio")
public class AudioResource {

    private static final Logger LOG = Logger.getLogger(AudioResource.class);

    @Inject
    FileStorageService fileStorageService;

    @GET
    @Path("/{audioPath: .+}")
    public Response getAudio(@PathParam("audioPath") String audioPath) {
        LOG.infof("Retrieving audio: %s", audioPath);
        File file = fileStorageService.getAudio(audioPath);
        if (file == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        String contentType = determineContentType(file.getName());
        return Response.ok(file, contentType).build();
    }

    private String determineContentType(String fileName) {
        if (fileName.toLowerCase().endsWith(".mp3")) {
            return "audio/mpeg";
        } else if (fileName.toLowerCase().endsWith(".wav")) {
            return "audio/wav";
        } else if (fileName.toLowerCase().endsWith(".ogg")) {
            return "audio/ogg";
        } else if (fileName.toLowerCase().endsWith(".m4a")) {
            return "audio/mp4";
        } else {
            return "application/octet-stream";
        }
    }
}