package com.fabelfabrik.model;

import jakarta.json.bind.annotation.JsonbProperty;
import jakarta.json.bind.annotation.JsonbTransient;
import io.quarkus.mongodb.panache.PanacheMongoEntity;
import java.util.List;

public class Story extends PanacheMongoEntity {

    // ID wird bei der JSON-Serialisierung ignoriert
    @JsonbTransient
    public Object getId() {
        return super.id;
    }

    @JsonbProperty("title")
    public String title;

    @JsonbProperty("content")
    public String content;

    @JsonbProperty("images")
    public List<String> images;

    @JsonbProperty("audio")
    public String audio;

    public Story() {}

    public Story(String title, String content, List<String> images, String audio) {
        this.title = title;
        this.content = content;
        this.images = images;
        this.audio = audio;
    }
}