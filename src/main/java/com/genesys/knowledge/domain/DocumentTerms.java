package com.genesys.knowledge.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by rhorilyi on 26.04.2017.
 */
@AllArgsConstructor
public class DocumentTerms {

    @Getter @Setter
    private String id;
    @Getter @Setter
    private Set<String> terms;

    public DocumentTerms() {
        terms = new HashSet<>();
    }
}
