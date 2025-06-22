package com.fabelfabrik.model;

import com.fabelfabrik.resource.RatingRequest;
import io.quarkus.mongodb.panache.PanacheMongoEntity;
import java.util.List;
import java.util.ArrayList;

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
    public String ttsUrl;
    public String videoUrl;
    public List<Integer> ratings = new ArrayList<>();

    public void addRating(RatingRequest ratingRequest) {
        if (ratings == null) {
            ratings = new ArrayList<>();
        }
        ratings.add(ratingRequest.rating);
    }
}
