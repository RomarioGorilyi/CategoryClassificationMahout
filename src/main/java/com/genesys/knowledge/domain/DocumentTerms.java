package com.genesys.knowledge.domain;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by rhorilyi on 26.04.2017.
 */
@Data
@AllArgsConstructor
public abstract class DocumentTerms {

    private String id;
    private Set<String> terms;

    public DocumentTerms() {
        terms = new HashSet<>();
    }

    public abstract void retrieveTermsFromText();
}
