package com.genesys.knowledge.classifier.util;

import com.genesys.knowledge.domain.Category;
import com.genesys.knowledge.domain.Document;
import com.genesys.knowledge.domain.ResponseMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestTemplate;

/**
 * Created by rhorilyi on 26.04.2017.
 */
@Slf4j
public class DocumentsHandler {

    public static Document[] retrieveDocuments() {
        String url = "http://gks-dep-stbl:9092/gks-server/v2/knowledge/tenants/1/langs/en_US/documents?size=1000";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> request = new HttpEntity<>(headers);

        RestTemplate restTemplate = new RestTemplate();

        ResponseMessage responseMessage = restTemplate.postForObject(url,request, ResponseMessage.class);

        return responseMessage.getData().getDocuments();
    }
}
