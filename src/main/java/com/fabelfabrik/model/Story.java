package com.fabelfabrik.model;

import io.quarkus.mongodb.panache.PanacheMongoEntity;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString(callSuper = true)
public class Story extends PanacheMongoEntity {

    public String title;
    public String content;
    public List<String> images;
    public String audio;
}
