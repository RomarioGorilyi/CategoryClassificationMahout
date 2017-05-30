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

    public Document() {
        text = "";
        categories = new ArrayList<>();
    }

    public Document(String text) {
        this.text = text;
        categories = new ArrayList<>();
    }

    public void addCategory(Category category) {
        categories.add(category);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Document)) return false;

        Document document = (Document) o;

        return text != null ? text.equals(document.text) : document.text == null;
    }

    @Override
    public int hashCode() {
        return text != null ? text.hashCode() : 0;
    }
}
