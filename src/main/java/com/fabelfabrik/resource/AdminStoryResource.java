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

    @GET
    @Path("/auth")
    public Response checkAuth() {
        return Response.ok().build();
    }

    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Response uploadStory(StoryUploadForm form) {

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

        FileUploadResult videoResult = fileUploadService.processVideoUpload(form.video, form.videoFileName);
        if (!videoResult.success) {
            return Response.serverError().entity(videoResult.error).build();
        }

        FileUploadResult ttsAudioResult = fileUploadService.processAudioUpload(form.ttsAudio, form.ttsFileName);
        if (!ttsAudioResult.success) {
            return Response.serverError().entity(ttsAudioResult.error).build();
        }

        // Story erstellen und speichern
        Story story = storyService.of(form, pdfResult, imageResult, audioResult, videoResult, ttsAudioResult);

        LOG.infof("Story created: %s", story);
        return Response.ok(story).build();
    }

    /**
     * Deletes a story and all its associated files
     * @param id The ID of the story to delete
     * @return 200 OK if the story was deleted successfully, 404 Not Found if the story was not found,
     *         500 Internal Server Error if there was an error deleting the story
     */
    @DELETE
    @Path("/{id}")
    public Response deleteStory(@PathParam("id") String id) {
        LOG.infof("Received request to delete story with ID: %s", id);

        boolean deleted = storyService.deleteStory(id);

        if (deleted) {
            return Response.ok().build();
        } else {
            // Check if the story exists
            Story story = Story.findById(id);
            if (story == null) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity("Story not found with ID: " + id)
                        .build();
            } else {
                return Response.serverError()
                        .entity("Failed to delete story with ID: " + id)
                        .build();
            }
        }
    }
}
