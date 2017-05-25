package com.genesys.knowledge.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.genesys.knowledge.classification.util.DocumentHandler;
import lombok.*;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by rhorilyi on 25.04.2017.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@AllArgsConstructor
@Slf4j
public class Document {

    @Getter @Setter
    private String kbId;
    @JsonProperty(value = "answer")
    @Getter @Setter
    private String text;
    @JsonProperty(value = "relatedCategories")
    @Getter @Setter
    private List<Category> categories;
    @Getter @Setter
    private List<String> tokens;

    public Document() {
        categories = new ArrayList<>();
        text = "";
        tokens = new ArrayList<>();
    }

    public Document(String text) {
        this.text = text;
        if (text != null) {
            tokens = DocumentHandler.convertTextToTokens(text, null);
        } else {
            tokens = new ArrayList<>();
        }
    }

    public void addCategory(Category category) {
        categories.add(category);
    }
}
