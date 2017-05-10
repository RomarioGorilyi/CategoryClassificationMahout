package com.genesys.knowledge.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Created by rhorilyi on 25.04.2017.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@NoArgsConstructor
public class ResponseMessage {

    @JsonProperty(value = "statusCode")
    private String status;
    @JsonProperty(value = "response")
    private ResponseData data;

    @JsonIgnoreProperties(ignoreUnknown = true)
    @Data
    @NoArgsConstructor
    public static class ResponseData {

        private List<Document> documents;
    }
}
