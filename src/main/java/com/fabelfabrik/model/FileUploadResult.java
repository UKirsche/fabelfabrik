package com.fabelfabrik.model;
import lombok.Getter;

@Getter
public class FileUploadResult {
    public final String url;
    public final boolean success;
    public final String error;

    private FileUploadResult(String url, boolean success, String error) {
        this.url = url;
        this.success = success;
        this.error = error;
    }

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