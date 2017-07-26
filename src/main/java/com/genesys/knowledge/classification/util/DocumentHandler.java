package com.genesys.knowledge.classification.util;

import com.genesys.elasticsearch.index.analysis.tokenizers.FreeLingTokenizer;
import com.genesys.knowledge.classification.learner.Learner;
import com.genesys.knowledge.domain.Category;
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

    public static List<com.genesys.knowledge.domain.Document> retrieveDocuments(String url, String knowledgeBase) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        String requestBody = "{\"knowledgebases\" : [ \"" + knowledgeBase + "\" ]}";
        HttpEntity<String> request = new HttpEntity<>(requestBody, headers);

        RestTemplate restTemplate = new RestTemplate();

        ResponseMessage responseMessage = restTemplate.postForObject(url, request, ResponseMessage.class);
        return responseMessage.getData().getDocuments();
    }

    /**
     * Converts the specified text into {@link List} of tokens using the specified type of a tokenizer.
     * If {@code tokenizerType} is {@code null}, use the default type.
     *
     * @param text text to convert
     * @param tokenizerType option that declare which type of tokenizer to use
     * @return
     */
    public static List<String> convertTextToTokens(String text, TokenizerType tokenizerType) {
        List<String> resultTokens = new ArrayList<>();

        TokenStream tokenStream;
        if ((tokenizerType == null) || (tokenizerType == TokenizerType.FreeLingTokenizer)) {
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

        if (tokenizerType == TokenizerType.StandardTokenizer) {
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

    public static int findMaxNumberOfTokens(List<com.genesys.knowledge.domain.Document> documents) {
        int maxNumberOfTokens = 0;

        for (com.genesys.knowledge.domain.Document document : documents) {
            String text = document.getText();
            if (text != null) {
                List<String> tokens = convertTextToTokens(text, null);
                int tokensNumber = tokens.size();
                if (tokensNumber > maxNumberOfTokens) {
                    maxNumberOfTokens = tokensNumber;
                }
            }
        }

        return maxNumberOfTokens;
    }

    public static int findMaxNumberOfTokens(ArrayList<Learner.Document> documents) {
        int maxNumberOfTokens = 0;

        for (Learner.Document document : documents) {
            int titleLength = document.getTitle().getTokens().size();
            if (titleLength > maxNumberOfTokens) {
                maxNumberOfTokens = titleLength;
            }
            int bodyLength = document.getBody().getTokens().size();
            if (bodyLength > maxNumberOfTokens) {
                maxNumberOfTokens = bodyLength;
            }
        }

        return maxNumberOfTokens;
    }

    public static int findUniqueCategoriesNumber(List<com.genesys.knowledge.domain.Document> documents) {
        Set<String> uniqueCategories = new HashSet<>();

        for (com.genesys.knowledge.domain.Document document : documents) {
            List<Category> categories = document.getCategories();
            for (Category category : categories) {
                uniqueCategories.add(category.getId());
            }
        }

        return uniqueCategories.size();
    }

    public static int findUniqueCategoriesNumber(ArrayList<Learner.Document> documents) {
        Set<String> uniqueCategories = new HashSet<>();

        for (Learner.Document document : documents) {
            List<String> categories = document.getCategories();
            uniqueCategories.addAll(categories);
        }

        return uniqueCategories.size();
    }

    public enum TokenizerType {
        StandardTokenizer,
        FreeLingTokenizer
    }
}
