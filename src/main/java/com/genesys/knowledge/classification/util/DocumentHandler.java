package com.genesys.knowledge.classification.util;

import com.genesys.elasticsearch.index.analysis.tokenizers.FreeLingTokenizer;
import com.genesys.knowledge.domain.Category;
import com.genesys.knowledge.domain.Document;
import com.genesys.knowledge.domain.ResponseMessage;
import lombok.extern.slf4j.Slf4j;
import org.apache.lucene.analysis.*;
import org.apache.lucene.analysis.core.LowerCaseFilter;
import org.apache.lucene.analysis.core.StopFilter;
import org.apache.lucene.analysis.icu.ICUFoldingFilter;
import org.apache.lucene.analysis.snowball.SnowballFilter;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.apache.lucene.analysis.tokenattributes.*;
import org.apache.lucene.analysis.util.CharArraySet;
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

    public static List<Document> retrieveDocuments(String url, String knowledgeBase) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        String requestBody = "{\"knowledgebases\" : [ \"" + knowledgeBase + "\" ]}";
        HttpEntity<String> request = new HttpEntity<>(requestBody, headers);

        RestTemplate restTemplate = new RestTemplate();

        ResponseMessage responseMessage = restTemplate.postForObject(url, request, ResponseMessage.class);
        return responseMessage.getData().getDocuments();
    }

    // TODO talk out exception handling: mb this method should throw them
    public static List<String> convertTextToTokens(String text, TokenizerOption tokenizerOption) {
        List<String> resultTokens = new ArrayList<>();

        TokenStream tokenStream;
        if ((tokenizerOption == null) || (tokenizerOption == TokenizerOption.FreeLingTokenizer)) {
            tokenStream = new FreeLingTokenizer(new StringReader(text),
                    "C:/freeling4-win64/data/",
                    "en", null, null, false);
        } else {
            tokenStream = new StandardTokenizer(new BufferedReader(new StringReader(text)));
        }

        tokenStream = new LowerCaseFilter(tokenStream);

        ArrayList<String> stopWordsList = new ArrayList<>();
        try (Scanner scanner = new Scanner(new File("src/main/resources/rules/knowledge/stopwords.txt"))) {
            while (scanner.hasNext()) {
                stopWordsList.add(scanner.next());
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        CharArraySet stopWordsSet = new CharArraySet(stopWordsList, true);
        tokenStream = new StopFilter(tokenStream, stopWordsSet);

        tokenStream = new ICUFoldingFilter(tokenStream);

        if (tokenizerOption == TokenizerOption.StandardTokenizer) {
            tokenStream = new SnowballFilter(tokenStream, "English"); // stemming
        }

        try {
            tokenStream.reset();
            while (tokenStream.incrementToken()) {
                resultTokens.add(tokenStream.getAttribute(CharTermAttribute.class).toString());
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                tokenStream.end();
                tokenStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return resultTokens;
    }

    public static int findMaxNumberOfTokens(List<Document> documents) {
        int maxNumberOfTokens = 0;

        for (Document document : documents) {
            if (document.getText() != null) {
                List<String> tokens = convertTextToTokens(document.getText(), null);
                int tokensNumber = tokens.size();
                if (tokensNumber > maxNumberOfTokens) {
                    maxNumberOfTokens = tokensNumber;
                }
            }
        }

        return maxNumberOfTokens;
    }

    public static int findUniqueCategoriesNumber(List<Document> documents) {
        Set<String> uniqueCategories = new HashSet<>();

        for (Document document : documents) {
            List<Category> categories = document.getCategories();
            for (Category category : categories) {
                uniqueCategories.add(category.getId());
            }
        }

        return uniqueCategories.size();
    }

    public enum TokenizerOption {
        StandardTokenizer,
        FreeLingTokenizer
    }
}
