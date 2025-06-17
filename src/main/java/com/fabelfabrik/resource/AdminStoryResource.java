package com.fabelfabrik.resource;

import com.fabelfabrik.model.Story;
import com.fabelfabrik.utils.FileStorageService;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;
import org.jboss.resteasy.annotations.providers.multipart.MultipartForm;

import java.io.InputStream;

@Path("/api/admin/story")
@RolesAllowed("admin")
@Produces(MediaType.APPLICATION_JSON)
public class AdminStoryResource {

    @Inject
    Logger LOG;

    @Inject
    FileStorageService fileStorageService;

    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Response uploadStory(@MultipartForm StoryUploadForm form) {

        // Alle Uploads verarbeiten
        FileUploadResult pdfResult = processPdfUpload(form);
        if (!pdfResult.success) {
            return Response.serverError().entity(pdfResult.error).build();
        }

        FileUploadResult imageResult = processCoverImageUpload(form);
        if (!imageResult.success) {
            return Response.serverError().entity(imageResult.error).build();
        }

        FileUploadResult audioResult = processAudioUpload(form);
        if (!audioResult.success) {
            return Response.serverError().entity(audioResult.error).build();
        }

        // Story erstellen und speichern
        Story story = of(form, pdfResult, imageResult, audioResult);

        LOG.infof("Story created: %s", story);
        return Response.ok(story).build();
    }

    private static Story of(StoryUploadForm form, FileUploadResult pdfResult, FileUploadResult imageResult, FileUploadResult audioResult) {
        Story story = new Story();
        story.title = form.title;
        story.content = form.content;
        story.description = form.description;
        story.pageCount = form.pageCount;
        story.pdfUrl = pdfResult.url;
        story.coverImageUrl = imageResult.url;
        story.audioUrl = audioResult.url;
        story.persist();
        return story;
    }

    // Einzelne Methoden für jeden Dateityp
    private FileUploadResult processPdfUpload(StoryUploadForm form) {
        return processFileUpload(form.pdf, form.pdfFileName,
                fileStorageService::storePdf, "PDF");
    }

    private FileUploadResult processCoverImageUpload(StoryUploadForm form) {
        return processFileUpload(form.coverImage, form.coverImageFileName,
                fileStorageService::storeImage, "Bild");
    }

    private FileUploadResult processAudioUpload(StoryUploadForm form) {
        return processFileUpload(form.audio, form.audioFileName,
                fileStorageService::storeAudio, "Audio");
    }

    // Generische Upload-Methode
    private FileUploadResult processFileUpload(InputStream fileStream, String fileName,
                                               FileUploadFunction uploadFunction, String fileType) {
        if (fileStream == null || fileName == null) {
            return FileUploadResult.notPresent();
        }

        try {
            String url = uploadFunction.upload(fileStream, fileName);
            LOG.infof("%s stored at: %s", fileType, url);
            return FileUploadResult.success(url);
        } catch (Exception e) {
            LOG.error(fileType + "-Upload fehlgeschlagen", e);
            return FileUploadResult.failure(fileType + "-Upload fehlgeschlagen: " + e.getMessage());
        }
    }

    // Functional Interface für Upload-Funktionen
    @FunctionalInterface
    private interface FileUploadFunction {
        String upload(InputStream stream, String fileName) throws Exception;
    }

    // Result-Klasse für Upload-Ergebnisse
    private record FileUploadResult(String url, boolean success, String error) {

        public static FileUploadResult success(String url) {
            return new FileUploadResult(url, true, null);
        }

        public static FileUploadResult failure(String error) {
            return new FileUploadResult(null, false, error);
        }

        public static FileUploadResult notPresent() {
            return new FileUploadResult(null, true, null);
        }
    }
}