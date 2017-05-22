package com.genesys.knowledge.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

/**
 * Created by rhorilyi on 25.04.2017.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class Category {

    @Getter @Setter
    private String id;
}
