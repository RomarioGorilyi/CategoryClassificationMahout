package com.genesys.knowledge.classification.classifier;

import com.genesys.knowledge.domain.Category;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

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
        List<List<String>> tokensList = new ArrayList<>();

        Category category1 = new Category("category1");
        List<String> tokens1 = Arrays.asList("test", "text", "teach", "machine");
        tokensList.add(tokens1);
        trainClassifier(tokens1, category1, tokensList);

        Category category2 = new Category("category2");
        List<String> tokens2 = Arrays.asList("real", "document", "apply");
        tokensList.add(tokens2);
        trainClassifier(tokens2, category2, tokensList);

        String expectedAlreadyTrainedDocumentCategory = category1.getId();
        String actualAlreadyTrainedDocumentCategory = classifier.calcMostConfidentCategory(tokens1, tokensList);
        assertEquals(expectedAlreadyTrainedDocumentCategory, actualAlreadyTrainedDocumentCategory);

        List<String> testTokens = Arrays.asList("teach", "machine", "hobby");
        tokensList.add(testTokens);
        String expectedTestDocumentCategory = category1.getId();
        String actualTestDocumentCategory = classifier.calcMostConfidentCategory(testTokens, tokensList);
        assertEquals(expectedTestDocumentCategory, actualTestDocumentCategory);
    }

    @Test
    public void testTrainClassifierWithOnlyOneDoc() {
        List<List<String>> tokensList = new ArrayList<>();

        Category category = new Category("category");
        List<String> tokens = Arrays.asList("test", "text");
        tokensList.add(tokens);
        trainClassifier(tokens, category, tokensList);

        List<String> testTokens = Arrays.asList("teach", "machine", "hobby");
        tokensList.add(testTokens);
        String expectedTestDocumentCategory = category.getId();
        String actualTestDocumentCategory = classifier.calcMostConfidentCategory(testTokens, tokensList);
        assertEquals(expectedTestDocumentCategory, actualTestDocumentCategory);
    }

    @Test
    public void testCalculateUnknownCategoryProbability() {
        List<List<String>> tokensList = new ArrayList<>();

        Category category = new Category("category");
        List<String> tokens = Arrays.asList("test", "text");
        tokensList.add(tokens);
        trainClassifier(tokens, category, tokensList);

        Category unknownCategory = new Category("unknownCategory");
        double actualProbability = classifier.calcCategoryProbability(tokens, unknownCategory, tokensList);
        double expectedProbability = 0;
        assertEquals(expectedProbability, actualProbability, 0);
    }

    private void trainClassifier(List<String> tokens, Category category, Collection<List<String>> documentTokens) {
        for (int trainLoop = 0; trainLoop < 30; trainLoop++) {
            classifier.train(tokens, category, documentTokens);
        }
    }
}
