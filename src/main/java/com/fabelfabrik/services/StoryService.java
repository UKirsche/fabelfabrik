package com.fabelfabrik.services;

import com.fabelfabrik.model.FileUploadResult;
import com.fabelfabrik.model.Story;
import com.fabelfabrik.resource.StoryUploadForm;
import com.fabelfabrik.utils.FileStorageService;
import java.time.Instant;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

@ApplicationScoped
public class StoryService {

    @Inject
    Logger LOG;

    @Inject
    FileStorageService fileStorageService;

    @ConfigProperty(name = "elevenlabs.api.url")
    String elevenLabsApiUrl;

    @ConfigProperty(name = "elevenlabs.api.key")
    String elevenLabsApiKey;

    @ConfigProperty(name = "elevenlabs.voice.id")
    String elevenLabsVoiceId;

    /**
     * Calls ElevenLabs API to generate TTS audio from the given text
     * and stores the resulting audio file
     * @param text The text to convert to speech
     * @return The URL of the stored audio file
     */
    public String generateTtsAudio(String text) {
        LOG.info("Generating TTS audio for text: " + (text.length() > 50 ? text.substring(0, 50) + "..." : text));

        try {
            // Create the request payload
            Map<String, Object> payload = new HashMap<>();
            payload.put("text", text);
            payload.put("model_id", "eleven_multilingual_v2");

            Map<String, Object> voiceSettings = new HashMap<>();
            voiceSettings.put("stability", 0.5);
            voiceSettings.put("similarity_boost", 0.75);
            payload.put("voice_settings", voiceSettings);

            // Create the client and make the request
            Client client = ClientBuilder.newClient();
            String url = elevenLabsApiUrl + "/text-to-speech/" + elevenLabsVoiceId;

            Response response = client.target(url)
                    .request(MediaType.APPLICATION_JSON)
                    .header("xi-api-key", elevenLabsApiKey)
                    .header("Accept", "audio/mpeg")
                    .post(Entity.json(payload));

            // Check if the request was successful
            if (response.getStatus() != 200) {
                LOG.error("Failed to generate TTS audio. Status: " + response.getStatus());
                return null;
            }

            // Get the audio data from the response
            byte[] audioData = response.readEntity(byte[].class);

            // Store the audio file
            try (InputStream audioStream = new ByteArrayInputStream(audioData)) {
                String fileName = "tts_" + System.currentTimeMillis() + ".mp3";
                String storedPath = fileStorageService.storeAudio(audioStream, fileName);
                LOG.info("Stored TTS audio file at: " + storedPath);
                return storedPath;
            }
        } catch (Exception e) {
            LOG.error("Error generating TTS audio", e);
            return null;
        }
    }

    /**
     * Creates and persists a Story object from upload form and results
     */
    public Story of(StoryUploadForm form, FileUploadResult pdfResult,
                    FileUploadResult imageResult, FileUploadResult audioResult,
                    FileUploadResult videoResult, FileUploadResult ttsAudioResult) {
        Story story = new Story();
        story.title = form.title;
        story.content = form.content;
        story.description = form.description;
        story.pageCount = form.pageCount;
        story.pdfUrl = pdfResult.getUrl();
        story.coverImageUrl = imageResult.getUrl();
        story.audioUrl = audioResult.getUrl();
        story.videoUrl = videoResult != null ? videoResult.getUrl() : null;
        story.createdAt = Instant.now();

        // Set ttsUrl from uploaded file if available
        if (ttsAudioResult != null && ttsAudioResult.getUrl() != null) {
            story.ttsUrl = ttsAudioResult.getUrl();
            LOG.info("Set ttsUrl from uploaded file: " + ttsAudioResult.getUrl());
        } else {
            // Generate TTS audio from the story content only if ttsUrl is not already set
            String ttsUrl = generateTtsAudio(form.content);
            if (ttsUrl != null) {
                story.ttsUrl = ttsUrl;
                LOG.info("Set ttsUrl for story: " + ttsUrl);
            } else {
                LOG.warn("Failed to generate TTS audio for story");
            }
        }

        story.persist();
        return story;
    }

    /**
     * Deletes a story and all its associated files
     * @param storyId The ID of the story to delete
     * @return true if the story was deleted successfully, false otherwise
     */
    public boolean deleteStory(String storyId) {
        LOG.infof("Deleting story with ID: %s", storyId);

        // Find the story by ID
        Story story = Story.findById(storyId);
        if (story == null) {
            LOG.warnf("Story not found with ID: %s", storyId);
            return false;
        }

        // Delete all associated files
        boolean filesDeleted = true;

        // Delete PDF file
        if (story.pdfUrl != null && !story.pdfUrl.isEmpty()) {
            boolean pdfDeleted = fileStorageService.deletePdf(story.pdfUrl);
            if (!pdfDeleted) {
                LOG.warnf("Failed to delete PDF file: %s", story.pdfUrl);
                filesDeleted = false;
            }
        }

        // Delete cover image
        if (story.coverImageUrl != null && !story.coverImageUrl.isEmpty()) {
            boolean imageDeleted = fileStorageService.deleteImage(story.coverImageUrl);
            if (!imageDeleted) {
                LOG.warnf("Failed to delete cover image: %s", story.coverImageUrl);
                filesDeleted = false;
            }
        }

        // Delete audio file
        if (story.audioUrl != null && !story.audioUrl.isEmpty()) {
            boolean audioDeleted = fileStorageService.deleteAudio(story.audioUrl);
            if (!audioDeleted) {
                LOG.warnf("Failed to delete audio file: %s", story.audioUrl);
                filesDeleted = false;
            }
        }

        // Delete TTS audio file
        if (story.ttsUrl != null && !story.ttsUrl.isEmpty()) {
            boolean ttsDeleted = fileStorageService.deleteAudio(story.ttsUrl);
            if (!ttsDeleted) {
                LOG.warnf("Failed to delete TTS audio file: %s", story.ttsUrl);
                filesDeleted = false;
            }
        }

        // Delete video file
        if (story.videoUrl != null && !story.videoUrl.isEmpty()) {
            boolean videoDeleted = fileStorageService.deleteVideo(story.videoUrl);
            if (!videoDeleted) {
                LOG.warnf("Failed to delete video file: %s", story.videoUrl);
                filesDeleted = false;
            }
        }

        // Delete the story entity from the database
        try {
            story.delete();
            // Check if the story was actually deleted
            if (Story.findById(storyId) != null) {
                LOG.warnf("Failed to delete story entity with ID: %s", storyId);
                return false;
            }
        } catch (Exception e) {
            LOG.errorf(e, "Error deleting story entity with ID: %s", storyId);
            return false;
        }

        LOG.infof("Story deleted successfully: %s, all files deleted: %s", storyId, filesDeleted);
        return true;
    }
}
