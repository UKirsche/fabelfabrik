package com.fabelfabrik.resource;
import org.jboss.resteasy.annotations.providers.multipart.PartType;
import jakarta.ws.rs.FormParam;
import java.io.InputStream;

public class StoryUploadForm {
    @FormParam("title")
    @PartType("text/plain")
    public String title;

    @FormParam("description")
    @PartType("text/plain")
    public String description;

    @FormParam("content")
    @PartType("text/plain")
    public String content;

    @FormParam("pageCount")
    @PartType("text/plain")
    public int pageCount;

    @FormParam("pdf")
    @PartType("application/pdf")
    public InputStream pdf;

    @FormParam("pdfFileName")
    @PartType("text/plain")
    public String pdfFileName;

    @FormParam("coverImage")
    @PartType("application/octet-stream")
    public InputStream coverImage;

    @FormParam("coverImageFileName")
    @PartType("text/plain")
    public String coverImageFileName;

    @FormParam("audio")
    @PartType("application/octet-stream")
    public InputStream audio;

    @FormParam("audioFileName")
    @PartType("text/plain")
    public String audioFileName;

    @FormParam("video")
    @PartType("application/octet-stream")
    public InputStream video;

    @FormParam("videoFileName")
    @PartType("text/plain")
    public String videoFileName;

    @FormParam("ttsAudio")
    @PartType("application/octet-stream")
    public InputStream ttsAudio;

    @FormParam("ttsFileName")
    @PartType("text/plain")
    public String ttsFileName;
}
