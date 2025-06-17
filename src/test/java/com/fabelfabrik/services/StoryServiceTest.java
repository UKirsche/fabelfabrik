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
    }

    @Test
    public void testStoryCreation() {
        // When
        Story story = storyService.of(form, pdfResult, imageResult, audioResult);

        // Then
        assertNotNull(story);
        assertEquals("Test Story", story.title);
        assertEquals("Test Content", story.content);
        assertEquals("Test Description", story.description);
        assertEquals(10, story.pageCount);
        assertEquals("pdfs/test.pdf", story.pdfUrl);
        assertEquals("images/test.jpg", story.coverImageUrl);
        assertEquals("audio/test.mp3", story.audioUrl);
    }
}