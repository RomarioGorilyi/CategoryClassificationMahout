package com.genesys.knowledge.classification.classifier;

import com.genesys.knowledge.classification.util.DocumentHandler;
import com.genesys.knowledge.domain.Category;
import com.genesys.knowledge.domain.Document;
import org.junit.Before;
import org.junit.Test;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;

/**
 * Created by rhorilyi on 28.04.2017.
 */
public class LrClassifierConstructedWithFixedCategoriesAndFeaturesNumbersTest {

    private LogisticRegressionClassifier classifier;

    @Before
    public void prepareClassifier() {
        classifier = new LogisticRegressionClassifier(2, 7);
        classifier.getCategoryHandler().clear();
    }

    @Test
    public void testTrainClassifierWithTwoDocs() {
        Map<Document, List<String>> documentTokens = new HashMap<>();

        Category category1 = new Category("category1");
        Document document1 = new Document("Test text to teach machine.");
        List<String> tokens1 = DocumentHandler.convertTextToTokens(document1.getText(), null);
        documentTokens.put(document1, tokens1);
        trainClassifier(tokens1, category1, documentTokens.values());

        Category category2 = new Category("category2");
        Document document2 = new Document("Real document that can be really applied.");
        List<String> tokens2 = DocumentHandler.convertTextToTokens(document2.getText(), null);
        documentTokens.put(document2, tokens2);
        trainClassifier(tokens2, category2, documentTokens.values());

        String expectedAlreadyTrainedDocumentCategory = category1.getId();
        String actualAlreadyTrainedDocumentCategory = classifier.calcMostConfidentCategory(tokens1, documentTokens.values());
        assertEquals(expectedAlreadyTrainedDocumentCategory, actualAlreadyTrainedDocumentCategory);

        Document testDocument = new Document("Teaching machines is our hobby");
        List<String> testTokens = DocumentHandler.convertTextToTokens(testDocument.getText(), null);
        documentTokens.put(testDocument, testTokens);
        String expectedTestDocumentCategory = category1.getId();
        String actualTestDocumentCategory = classifier.calcMostConfidentCategory(testTokens, documentTokens.values());
        assertEquals(expectedTestDocumentCategory, actualTestDocumentCategory);
    }

    @Test
    public void testTrainClassifierWithOnlyOneDoc() {
        Map<Document, List<String>> documentTokens = new HashMap<>();

        Category category = new Category("category");
        Document document = new Document("Test text");
        List<String> tokens = DocumentHandler.convertTextToTokens(document.getText(), null);
        documentTokens.put(document, tokens);
        trainClassifier(tokens, category, documentTokens.values());

        Document testDocument = new Document("Teaching machines is our hobby");
        List<String> testTokens = DocumentHandler.convertTextToTokens(testDocument.getText(), null);
        documentTokens.put(testDocument, testTokens);
        String expectedTestDocumentCategory = category.getId();
        String actualTestDocumentCategory = classifier.calcMostConfidentCategory(testTokens, documentTokens.values());
        assertEquals(expectedTestDocumentCategory, actualTestDocumentCategory);
    }

    @Test
    public void testCalculateUnknownCategoryProbability() {
        Map<Document, List<String>> documentTokens = new HashMap<>();

        Category category = new Category("category");
        Document document = new Document("Test text");
        List<String> tokens = DocumentHandler.convertTextToTokens(document.getText(), null);
        documentTokens.put(document, tokens);
        trainClassifier(tokens, category, documentTokens.values());

        Category unknownCategory = new Category("unknownCategory");
        double actualProbability = classifier.calcCategoryProbability(tokens, unknownCategory, documentTokens.values());
        double expectedProbability = 0;
        assertEquals(expectedProbability, actualProbability, 0);
    }

    private void trainClassifier(List<String> tokens, Category category, Collection<List<String>> documentTokens) {
        for (int trainLoop = 0; trainLoop < 30; trainLoop++) {
            classifier.train(tokens, category, documentTokens);
        }
    }
}
