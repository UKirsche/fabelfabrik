package com.fabelfabrik.services;

import com.fabelfabrik.model.FileUploadResult;
import com.fabelfabrik.model.Story;
import com.fabelfabrik.resource.StoryUploadForm;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@QuarkusTest
public class StoryServiceTest {

    @Inject
    StoryService storyService;

    private StoryUploadForm form;
    private FileUploadResult pdfResult;
    private FileUploadResult imageResult;
    private FileUploadResult audioResult;
    private FileUploadResult videoResult;
    private FileUploadResult ttsAudioResult;

    @BeforeEach
    public void setup() {
        // Setup test form
        form = new StoryUploadForm();
        form.title = "Test Story";
        form.content = "Test Content";
        form.description = "Test Description";
        form.pageCount = 10;

        // Setup mock results
        pdfResult = FileUploadResult.success("pdfs/test.pdf");
        imageResult = FileUploadResult.success("images/test.jpg");
        audioResult = FileUploadResult.success("audio/test.mp3");
        videoResult = FileUploadResult.success("videos/test.mp4");
        ttsAudioResult = FileUploadResult.success("audio/tts_test.mp3");
    }

    @Test
    public void testStoryCreation() {
        // When
        Story story = storyService.of(form, pdfResult, imageResult, audioResult, videoResult, ttsAudioResult);

        // Then
        assertNotNull(story);
        assertEquals("Test Story", story.title);
        assertEquals("Test Content", story.content);
        assertEquals("Test Description", story.description);
        assertEquals(10, story.pageCount);
        assertEquals("pdfs/test.pdf", story.pdfUrl);
        assertEquals("images/test.jpg", story.coverImageUrl);
        assertEquals("audio/test.mp3", story.audioUrl);
        assertEquals("videos/test.mp4", story.videoUrl);
        // Note: We're not asserting the specific value of ttsUrl because it depends on the ElevenLabs API call
    }

    @Test
    public void testLargeStoryContent() {
        // Create a story with 5000 words
        StringBuilder largeContent = new StringBuilder();
        for (int i = 0; i < 5000; i++) {
            largeContent.append("word").append(i).append(" ");
        }

        form.content = largeContent.toString();

        // When
        Story story = storyService.of(form, pdfResult, imageResult, audioResult, videoResult, ttsAudioResult);

        // Then
        assertNotNull(story);
        assertEquals(form.content, story.content);
        assertEquals(5000, story.content.split("\\s+").length);
    }
}
