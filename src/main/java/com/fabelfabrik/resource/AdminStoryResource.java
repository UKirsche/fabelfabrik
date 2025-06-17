package com.fabelfabrik.resource;

import com.fabelfabrik.model.FileUploadResult;
import com.fabelfabrik.model.Story;
import com.fabelfabrik.services.FileUploadService;
import com.fabelfabrik.services.StoryService;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;
import org.jboss.resteasy.annotations.providers.multipart.MultipartForm;

@Path("/api/admin/story")
@RolesAllowed("admin")
@Produces(MediaType.APPLICATION_JSON)
public class AdminStoryResource {

    @Inject
    Logger LOG;

    @Inject
    FileUploadService fileUploadService;

    @Inject
    StoryService storyService;

    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Response uploadStory(@MultipartForm StoryUploadForm form) {

        // Alle Uploads verarbeiten
        FileUploadResult pdfResult = fileUploadService.processPdfUpload(form.pdf, form.pdfFileName);
        if (!pdfResult.success) {
            return Response.serverError().entity(pdfResult.error).build();
        }

        FileUploadResult imageResult = fileUploadService.processCoverImageUpload(form.coverImage, form.coverImageFileName);
        if (!imageResult.success) {
            return Response.serverError().entity(imageResult.error).build();
        }

        FileUploadResult audioResult = fileUploadService.processAudioUpload(form.audio, form.audioFileName);
        if (!audioResult.success) {
            return Response.serverError().entity(audioResult.error).build();
        }

        // Story erstellen und speichern
        Story story = storyService.of(form, pdfResult, imageResult, audioResult);

        LOG.infof("Story created: %s", story);
        return Response.ok(story).build();
    }
}