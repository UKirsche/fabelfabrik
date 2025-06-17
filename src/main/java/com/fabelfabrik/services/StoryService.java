package com.fabelfabrik.services;

import com.fabelfabrik.model.FileUploadResult;
import com.fabelfabrik.model.Story;
import com.fabelfabrik.resource.StoryUploadForm;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class StoryService {




    /**
     * Creates and persists a Story object from upload form and results
     */
    public Story of(StoryUploadForm form, FileUploadResult pdfResult,
                    FileUploadResult imageResult, FileUploadResult audioResult) {
        Story story = new Story();
        story.title = form.title;
        story.content = form.content;
        story.description = form.description;
        story.pageCount = form.pageCount;
        story.pdfUrl = pdfResult.getUrl();
        story.coverImageUrl = imageResult.getUrl();
        story.audioUrl = audioResult.getUrl();
        story.persist();
        return story;
    }
}