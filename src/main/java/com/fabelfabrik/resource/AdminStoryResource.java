package com.fabelfabrik.resource;

import com.fabelfabrik.model.Story;
import com.fabelfabrik.utils.FileStorageService;
import jakarta.ws.rs.Path;
import org.jboss.resteasy.annotations.providers.multipart.MultipartForm;
import org.jboss.logging.Logger;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.io.*;
import java.nio.file.*;
import java.util.UUID;

@Path("/api/admin/story")
@RolesAllowed("admin")
public class AdminStoryResource {

    private static final Logger LOG = Logger.getLogger(AdminStoryResource.class);

    @Inject
    FileStorageService fileStorageService;

    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    public Response uploadStory(@MultipartForm StoryUploadForm form) {
        String pdfUrl = null;
        String coverImageUrl = null;
        String audio = null;

        // Ensure upload directories exist
        fileStorageService.init();

        // PDF speichern
        if (form.pdf != null && form.pdfFileName != null) {
            try {
                String pdfName = UUID.randomUUID() + "_" + form.pdfFileName;
                java.nio.file.Path uploadPath = Paths.get("uploads");
                java.nio.file.Path pdfPath = ((java.nio.file.Path) uploadPath).resolve(pdfName);

                try (OutputStream out = Files.newOutputStream(pdfPath)) {
                    form.pdf.transferTo(out);
                    pdfUrl = pdfPath.toString();
                    LOG.infof("PDF stored at: %s", pdfUrl);
                }
            } catch (IOException e) {
                LOG.error("PDF-Upload fehlgeschlagen", e);
                return Response.serverError().entity("PDF-Upload fehlgeschlagen: " + e).build();
            }
        }

        // Coverbild speichern
        if (form.coverImage != null && form.coverImageFileName != null) {
            try {
                coverImageUrl = fileStorageService.storeImage(form.coverImage, form.coverImageFileName);
                LOG.infof("Cover image stored at: %s", coverImageUrl);
            } catch (Exception e) {
                LOG.error("Bild-Upload fehlgeschlagen", e);
                return Response.serverError().entity("Bild-Upload fehlgeschlagen: " + e).build();
            }
        }

        // Audio speichern
        if (form.audio != null && form.audioFileName != null) {
            try {
                String audioName = UUID.randomUUID() + "_" + form.audioFileName;
                java.nio.file.Path uploadPath = Paths.get("uploads");
                java.nio.file.Path audioPath = uploadPath.resolve(audioName);

                try (OutputStream out = Files.newOutputStream(audioPath)) {
                    form.audio.transferTo(out);
                    audio = audioPath.toString();
                    LOG.infof("Audio stored at: %s", audio);
                }
            } catch (IOException e) {
                LOG.error("Audio-Upload fehlgeschlagen", e);
                return Response.serverError().entity("Audio-Upload fehlgeschlagen: " + e).build();
            }
        }

        // Story speichern
        Story story = new Story();
        story.title = form.title;
        story.description = form.description;
        story.pageCount = form.pageCount;
        story.pdfUrl = pdfUrl;
        story.coverImageUrl = coverImageUrl;
        story.audio = audio;
        story.persist();

        LOG.infof("Story created: %s", story);
        return Response.ok(story).build();
    }
}