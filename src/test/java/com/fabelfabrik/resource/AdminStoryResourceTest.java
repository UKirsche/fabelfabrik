package com.fabelfabrik.resource;

import com.fabelfabrik.model.FileUploadResult;
import com.fabelfabrik.model.Story;
import com.fabelfabrik.services.FileUploadService;
import com.fabelfabrik.services.StoryService;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.InjectMock;
import io.quarkus.test.security.TestSecurity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.ws.rs.core.MediaType;

import java.io.File;
import java.io.InputStream;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@QuarkusTest
public class AdminStoryResourceTest {

    @InjectMock
    FileUploadService fileUploadService;

    @InjectMock
    StoryService storyService;

    @BeforeEach
    public void setup() {
        // Setup test story
        Story testStory = new Story();
        testStory.title = "Test Story";
        testStory.content = "Test Content";
        testStory.description = "Test Description";
        testStory.pageCount = 10;
        testStory.pdfUrl = "pdfs/test.pdf";
        testStory.coverImageUrl = "images/test.jpg";
        testStory.audioUrl = "audio/test.mp3";
        testStory.ttsUrl = "audio/test_tts.mp3";
        testStory.videoUrl = "videos/test.mp4";

        // Setup mock responses
        when(fileUploadService.processPdfUpload(any(InputStream.class), anyString()))
                .thenReturn(FileUploadResult.success("pdfs/test.pdf"));

        when(fileUploadService.processCoverImageUpload(any(InputStream.class), anyString()))
                .thenReturn(FileUploadResult.success("images/test.jpg"));

        when(fileUploadService.processAudioUpload(any(InputStream.class), anyString()))
                .thenReturn(FileUploadResult.success("audio/test.mp3"));

        when(fileUploadService.processVideoUpload(any(InputStream.class), anyString()))
                .thenReturn(FileUploadResult.success("videos/test.mp4"));

        when(storyService.of(any(StoryUploadForm.class), any(FileUploadResult.class),
                any(FileUploadResult.class), any(FileUploadResult.class), any(FileUploadResult.class)))
                .thenReturn(testStory);
    }

    @Test
    @TestSecurity(user = "admin", roles = {"admin"})
    public void testUploadStorySuccess() {
        given()
                .multiPart("title", "Test Story")
                .multiPart("description", "Test Description")
                .multiPart("content", "Test Content")
                .multiPart("pageCount", "10")
                .multiPart("pdf", new File("src/test/resources/test.pdf"), "application/pdf")
                .multiPart("pdfFileName", "test.pdf")
                .multiPart("coverImage", new File("src/test/resources/test.jpg"), "image/jpeg")
                .multiPart("coverImageFileName", "test.jpg")
                .multiPart("audio", new File("src/test/resources/test.mp3"), "audio/mpeg")
                .multiPart("audioFileName", "test.mp3")
                .multiPart("video", new File("src/test/resources/test.mp4"), "video/mp4")
                .multiPart("videoFileName", "test.mp4")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .when()
                .post("/api/admin/story")
                .then()
                .statusCode(200)
                .body("title", is("Test Story"))
                .body("description", is("Test Description"))
                .body("content", is("Test Content"))
                .body("pageCount", is(10))
                .body("pdfUrl", is("pdfs/test.pdf"))
                .body("coverImageUrl", is("images/test.jpg"))
                .body("audioUrl", is("audio/test.mp3"))
                .body("ttsUrl", is("audio/test_tts.mp3"))
                .body("videoUrl", is("videos/test.mp4"));
    }

    @Test
    @TestSecurity(user = "admin", roles = {"admin"})
    public void testUploadStoryPdfFailure() {
        // Setup PDF upload failure
        when(fileUploadService.processPdfUpload(any(InputStream.class), anyString()))
                .thenReturn(FileUploadResult.failure("PDF upload failed"));

        given()
                .multiPart("title", "Test Story")
                .multiPart("description", "Test Description")
                .multiPart("content", "Test Content")
                .multiPart("pageCount", "10")
                .multiPart("pdf", new File("src/test/resources/test.pdf"), "application/pdf")
                .multiPart("pdfFileName", "test.pdf")
                .multiPart("coverImage", new File("src/test/resources/test.jpg"), "image/jpeg")
                .multiPart("coverImageFileName", "test.jpg")
                .multiPart("audio", new File("src/test/resources/test.mp3"), "audio/mpeg")
                .multiPart("audioFileName", "test.mp3")
                .multiPart("video", new File("src/test/resources/test.mp4"), "video/mp4")
                .multiPart("videoFileName", "test.mp4")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .when()
                .post("/api/admin/story")
                .then()
                .statusCode(500)
                .body(is("PDF upload failed"));
    }

    @Test
    public void testUploadStoryUnauthorized() {
        given()
                .multiPart("title", "Test Story")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .when()
                .post("/api/admin/story")
                .then()
                .statusCode(401); // Unauthorized
    }
}
