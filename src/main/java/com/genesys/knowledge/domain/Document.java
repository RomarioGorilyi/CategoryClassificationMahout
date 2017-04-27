package com.genesys.knowledge.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;
import lombok.extern.slf4j.Slf4j;

/**
 * Created by rhorilyi on 25.04.2017.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@AllArgsConstructor
@Slf4j
public class Document {

    @Getter @Setter
    private String body;
    @Getter @Setter
    private Category[] categories;

    public Document() {
        categories = new Category[0];
    }
}
