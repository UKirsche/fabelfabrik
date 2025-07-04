package com.fabelfabrik.resource;

import com.fabelfabrik.model.FileUploadResult;
import com.fabelfabrik.model.Story;
import com.fabelfabrik.services.FileUploadService;
import com.fabelfabrik.services.StoryService;
import org.bson.types.ObjectId;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;
import java.util.List;

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

        // Verarbeite mehrere Bilder, wenn vorhanden
        List<FileUploadResult> imagesResults = null;
        if (form.images != null && form.imageFileNames != null && !form.images.isEmpty() && !form.imageFileNames.isEmpty()) {
            LOG.infof("Verarbeite %d zusätzliche Bilder für Story-Upload", form.images.size());
            
            imagesResults = fileUploadService.processMultipleImagesUpload(form.images, form.imageFileNames);
            
            // Prüfe, ob alle Uploads erfolgreich waren
            int successCount = 0;
            for (int i = 0; i < imagesResults.size(); i++) {
                FileUploadResult result = imagesResults.get(i);
                if (!result.success) {
                    LOG.errorf("Fehler beim Upload von Bild %d (%s): %s", 
                      i + 1, 
                      form.imageFileNames.get(i), 
                      result.error);
                    return Response.serverError().entity(result.error).build();
                } else {
                    successCount++;
                    LOG.infof("Bild %d erfolgreich hochgeladen: %s -> %s", 
                     i + 1, 
                     form.imageFileNames.get(i), 
                     result.url);
                }
            }
            
            LOG.infof("Alle %d zusätzlichen Bilder erfolgreich hochgeladen", successCount);
        } else {
            LOG.debug("Keine zusätzlichen Bilder zum Upload gefunden");
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
        Story story = storyService.of(form, pdfResult, imageResult, imagesResults, audioResult, videoResult, ttsAudioResult);

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
            Story story = Story.findById(new ObjectId(id));
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