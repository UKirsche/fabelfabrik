package com.fabelfabrik.model;

import io.quarkus.mongodb.panache.PanacheMongoEntity;
import java.util.List;

import lombok.*;

@NoArgsConstructor
@AllArgsConstructor
@ToString(callSuper = true)
public class Story extends PanacheMongoEntity {
    public String title;
    public String description;
    public int pageCount;
    public String content;
    public String pdfUrl;
    public String coverImageUrl;
    public List<String> images;
    public String audioUrl;
}
