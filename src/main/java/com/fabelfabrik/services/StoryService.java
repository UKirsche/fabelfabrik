package com.fabelfabrik.services;

import com.fabelfabrik.model.FileUploadResult;
import com.fabelfabrik.model.Story;
import com.fabelfabrik.resource.StoryUploadForm;
import com.fabelfabrik.utils.FileStorageService;
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
                    FileUploadResult videoResult) {
        Story story = new Story();
        story.title = form.title;
        story.content = form.content;
        story.description = form.description;
        story.pageCount = form.pageCount;
        story.pdfUrl = pdfResult.getUrl();
        story.coverImageUrl = imageResult.getUrl();
        story.audioUrl = audioResult.getUrl();
        story.videoUrl = videoResult != null ? videoResult.getUrl() : null;

        // Generate TTS audio from the story content
        String ttsUrl = generateTtsAudio(form.content);
        if (ttsUrl != null) {
            story.ttsUrl = ttsUrl;
            LOG.info("Set ttsUrl for story: " + ttsUrl);
        } else {
            LOG.warn("Failed to generate TTS audio for story");
        }

        story.persist();
        return story;
    }
}
