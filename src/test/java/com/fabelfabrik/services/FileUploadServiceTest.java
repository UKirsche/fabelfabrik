package com.fabelfabrik.services;

import com.fabelfabrik.model.FileUploadResult;
import com.fabelfabrik.utils.FileStorageService;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.InjectMock;
import org.jboss.logging.Logger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import jakarta.inject.Inject;
import java.io.ByteArrayInputStream;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@QuarkusTest
public class FileUploadServiceTest {

    @Inject
    FileUploadService fileUploadService;

    @InjectMock
    FileStorageService fileStorageService;

    private InputStream testInputStream;

    @BeforeEach
    public void setup() {
        testInputStream = new ByteArrayInputStream("test file content".getBytes());
    }

    @Test
    public void testProcessPdfUploadSuccess() throws Exception {
        // Given
        when(fileStorageService.storePdf(any(InputStream.class), anyString()))
                .thenReturn("pdfs/test.pdf");

        // When
        FileUploadResult result = fileUploadService.processPdfUpload(testInputStream, "test.pdf");

        // Then
        assertTrue(result.success);
        assertEquals("pdfs/test.pdf", result.getUrl());
        assertNull(result.error);
    }

    @Test
    public void testProcessPdfUploadFailure() throws Exception {
        // Given
        when(fileStorageService.storePdf(any(InputStream.class), anyString()))
                .thenThrow(new RuntimeException("Storage error"));

        // When
        FileUploadResult result = fileUploadService.processPdfUpload(testInputStream, "test.pdf");

        // Then
        assertFalse(result.success);
        assertNull(result.getUrl());
        assertEquals("PDF-Upload fehlgeschlagen: Storage error", result.error);
    }

    @Test
    public void testProcessPdfUploadNotPresent() {
        // When
        FileUploadResult result = fileUploadService.processPdfUpload(null, null);

        // Then
        assertTrue(result.success);
        assertNull(result.getUrl());
        assertNull(result.error);
    }

    @Test
    public void testProcessCoverImageUploadSuccess() throws Exception {
        // Given
        when(fileStorageService.storeImage(any(InputStream.class), anyString()))
                .thenReturn("images/test.jpg");

        // When
        FileUploadResult result = fileUploadService.processCoverImageUpload(testInputStream, "test.jpg");

        // Then
        assertTrue(result.success);
        assertEquals("images/test.jpg", result.getUrl());
        assertNull(result.error);
    }

    @Test
    public void testProcessAudioUploadSuccess() throws Exception {
        // Given
        when(fileStorageService.storeAudio(any(InputStream.class), anyString()))
                .thenReturn("audio/test.mp3");

        // When
        FileUploadResult result = fileUploadService.processAudioUpload(testInputStream, "test.mp3");

        // Then
        assertTrue(result.success);
        assertEquals("audio/test.mp3", result.getUrl());
        assertNull(result.error);
    }
}