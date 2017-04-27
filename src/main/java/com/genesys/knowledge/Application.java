package com.genesys.knowledge;

import com.genesys.knowledge.domain.Category;
import com.genesys.knowledge.domain.Document;
import com.genesys.knowledge.domain.ResponseMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestTemplate;

/**
 * Created by rhorilyi on 25.04.2017.
 */
@Slf4j
public class Application {

    // TODO remove this class

    public static void main(String[] args) {
        String url = "http://gks-dep-stbl:9092/gks-server/v2/knowledge/tenants/1/langs/en_US/documents?size=1000";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> request = new HttpEntity<>(headers);

        RestTemplate restTemplate = new RestTemplate();

        ResponseMessage responseMessage = restTemplate.postForObject(url,request, ResponseMessage.class);

        Document[] documents = responseMessage.getData().getDocuments();
        for (Document document : documents) {
            System.out.println("\nDocument:");
            System.out.println(document.getBody());

            System.out.println("Categories:");
            Category[] categories = document.getCategories();
            for (Category category : categories) {
                System.out.println(category.getId());
            }
        }
    }
}
