package com.genesys.knowledge.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Created by rhorilyi on 25.04.2017.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@NoArgsConstructor
public class ResponseMessage {

    private String status;
    private ResponseData data;

    @JsonIgnoreProperties(ignoreUnknown = true)
    @Data
    @NoArgsConstructor
    public class ResponseData {

        private Document[] documents;
    }
}
