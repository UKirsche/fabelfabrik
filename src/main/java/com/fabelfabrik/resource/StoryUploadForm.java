package com.fabelfabrik.resource;
import org.jboss.resteasy.reactive.RestForm;
import java.io.InputStream;
import java.util.List;

public class StoryUploadForm {
    @RestForm
    public String title;

    @RestForm
    public String description;
    @RestForm
    public String content;
    @RestForm
    public int pageCount;
    @RestForm
    public InputStream pdf;
    @RestForm
    public String pdfFileName;
    @RestForm
    public InputStream coverImage;
    @RestForm
    public String coverImageFileName;
    @RestForm
    public List<InputStream> images;
    @RestForm
    public List<String> imageFileNames;
    @RestForm
    public InputStream audio;
    @RestForm
    public String audioFileName;
    @RestForm
    public InputStream video;
    @RestForm
    public String videoFileName;
    @RestForm
    public InputStream ttsAudio;
    @RestForm
    public String ttsFileName;
}
