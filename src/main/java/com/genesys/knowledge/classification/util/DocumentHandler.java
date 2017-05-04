package com.genesys.knowledge.classification.util;

import com.genesys.knowledge.domain.Category;
import com.genesys.knowledge.domain.Document;
import com.genesys.knowledge.domain.ResponseMessage;
import lombok.extern.slf4j.Slf4j;
import org.apache.lucene.analysis.CharArraySet;
import org.apache.lucene.analysis.LowerCaseFilter;
import org.apache.lucene.analysis.StopFilter;
import org.apache.lucene.analysis.snowball.SnowballFilter;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestTemplate;

import java.io.*;
import java.util.*;

/**
 * Created by rhorilyi on 26.04.2017.
 */
@Slf4j
public class DocumentHandler {

    public static Document[] retrieveDocuments() {
        String url = "http://gks-dep-stbl:9092/gks-server/v2/knowledge/tenants/1/langs/en_US/documents?size=1000";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> request = new HttpEntity<>(headers);

        RestTemplate restTemplate = new RestTemplate();

        ResponseMessage responseMessage = restTemplate.postForObject(url, request, ResponseMessage.class);

        return responseMessage.getData().getDocuments();
    }

    public static List<String> convertDocumentToTerms(Document document) {
        List<String> resultTerms = new ArrayList<>();

        StandardTokenizer standardTokenizer = new StandardTokenizer();
        standardTokenizer.setReader(new BufferedReader(new StringReader(document.getText())));

        LowerCaseFilter lowerCaseFilter = new LowerCaseFilter(standardTokenizer);

        ArrayList<String> stopWordsList = new ArrayList<>();
        try (Scanner scanner = new Scanner(new File("src/main/resources/rules/knowledge/stopwords.txt"))) {
            while (scanner.hasNext()) {
                stopWordsList.add(scanner.next());
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        CharArraySet stopWordsSet = new CharArraySet(stopWordsList, true);
        StopFilter stopFilter = new StopFilter(lowerCaseFilter, stopWordsSet);

//        KeywordAttribute attribute = new KeywordAttributeImpl();
//        attribute.setKeyword(true);
//        stopFilter.addAttribute(KeywordAttribute.class);

        SnowballFilter stemmer = new SnowballFilter(stopFilter, "English");
        try {
            stemmer.reset();
            while (stemmer.incrementToken()) {
                resultTerms.add(stemmer.getAttribute(CharTermAttribute.class).toString());
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                stemmer.end();
                stemmer.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return resultTerms;
    }

    public static int findMaxNumberOfTerms(Document[] documents) {
        int maxNumberOfTerms = 0;

        for (Document document : documents) {
            List<String> terms = convertDocumentToTerms(document);
            int termsNumber = terms.size();
            if (termsNumber > maxNumberOfTerms) {
                maxNumberOfTerms = termsNumber;
            }
        }

        return maxNumberOfTerms;
    }

    public static int findUniqueCategoriesNumber(Document[] documents) {
        Set<String> uniqueCategories = new HashSet<>();

        for (Document document : documents) {
            ArrayList<Category> categories = document.getCategories();
            for (Category category : categories) {
                uniqueCategories.add(category.getId());
            }
        }

        return uniqueCategories.size();
    }
}
