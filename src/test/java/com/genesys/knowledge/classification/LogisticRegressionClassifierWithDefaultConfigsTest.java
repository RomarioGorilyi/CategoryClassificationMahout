package com.genesys.knowledge.classifier;

import com.genesys.knowledge.domain.Category;
import com.genesys.knowledge.domain.Document;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Created by rhorilyi on 28.04.2017.
 */
public class LogisticRegressionClassifierWithDefaultConfigsTest {

    private LogisticRegressionClassifier classifier;

    @Before
    public void initClassifier() {
        classifier = new LogisticRegressionClassifier();
    }

    @Test
    public void testTrainClassifierWithTwoDocs() {
        Category category1 = new Category("category1");
        Document document1 = new Document("Test text1");
        trainClassifier(document1, category1);

        Category category2 = new Category("category2");
        Document document2 = new Document("Test text2");
        trainClassifier(document2, category2);

        double categoryProbability1 = classifier.calculateCategoryProbability(document1, category1);
        System.out.println("Document1: category1 probability: " + categoryProbability1);
        String expectedDocument1Category = category1.getId();
        String actualDocument1Category = classifier.calculateMostRelevantCategory(document1);
        assertEquals(expectedDocument1Category, actualDocument1Category);

        double categoryProbability2 = classifier.calculateCategoryProbability(document2, category2);
        System.out.println("Document2: category2 probability: " + categoryProbability2);
        String expectedDocument2Category = category2.getId();
        String actualDocument2Category = classifier.calculateMostRelevantCategory(document2);
        assertEquals(expectedDocument2Category, actualDocument2Category);
    }

    @Test
    public void testTrainClassifierWithOnlyOneDoc() {
        Category category = new Category("category");
        Document document = new Document("Test text");
        trainClassifier(document, category);

        double categoryProbability = classifier.calculateCategoryProbability(document, category);
        System.out.println("Category probability: " + categoryProbability);
        String expectedDocumentCategory = category.getId();
        String actualDocumentCategory = classifier.calculateMostRelevantCategory(document);
        assertEquals(expectedDocumentCategory, actualDocumentCategory);
    }

    @Test
    public void testCalculateUnknownCategoryProbability() {
        Category category = new Category("category");
        Document document = new Document("Test text");
        trainClassifier(document, category);

        Category unknownCategory = new Category("unknownCategory");
        double actualProbability = classifier.calculateCategoryProbability(document, unknownCategory);
        double expectedProbability = 0;
        assertEquals(expectedProbability, actualProbability, 0);
    }

    private void trainClassifier(Document document, Category category) {
        for (int trainLoop = 0; trainLoop < 30; trainLoop++) {
            classifier.train(document, category);
        }
    }
}
