package com.genesys.knowledge.classifier.util;

import com.genesys.knowledge.domain.Document;
import com.genesys.knowledge.domain.ResponseMessage;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.apache.lucene.analysis.CharArraySet;
import org.apache.lucene.analysis.LowerCaseFilter;
import org.apache.lucene.analysis.StopFilter;
import org.apache.lucene.analysis.snowball.SnowballFilter;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.KeywordAttribute;
import org.apache.lucene.analysis.tokenattributes.KeywordAttributeImpl;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestTemplate;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

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

        ResponseMessage responseMessage = restTemplate.postForObject(url, request, ResponseMessage.class);

        return responseMessage.getData().getDocuments();
    }

    public static List<String> retrieveTermsFromDocument(Document document) {
        List<String> terms = new ArrayList<>();

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
                terms.add(stemmer.getAttribute(CharTermAttribute.class).toString());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return terms;
    }

    public static ArrayList<Integer> findTermsNumberPerDocument() {
        ArrayList<Integer> termsNumber = new ArrayList<>();
        Document[] documents = retrieveDocuments();
        for (Document document : documents) {
            List<String> terms = retrieveTermsFromDocument(document);
            termsNumber.add(terms.size());
        }

        return termsNumber;
    }

    public static void main(String[] args) {
        System.out.println(findTermsNumberPerDocument().toString());

        Document document = new Document("My test text which is good enough text sample sampler the samplest, running, catchable.");
        List<String> terms = retrieveTermsFromDocument(document);
        System.out.println(terms.toString());
    }
}
