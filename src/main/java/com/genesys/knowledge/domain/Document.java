package com.genesys.knowledge.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;

/**
 * Created by rhorilyi on 25.04.2017.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@AllArgsConstructor
@Slf4j
public class Document {

    @JsonProperty(value = "body")
    @Getter @Setter
    private String text;
    @Getter @Setter
    private ArrayList<Category> categories;

    public Document() {
        categories = new ArrayList<>();
        text = "";
    }

    public Document(String text) {
        this.text = text;
    }

    public void addCategory(Category category) {
        categories.add(category);
    }
}
