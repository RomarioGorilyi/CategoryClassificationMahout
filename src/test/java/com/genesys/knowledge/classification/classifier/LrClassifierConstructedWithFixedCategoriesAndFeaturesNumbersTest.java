package com.genesys.knowledge.classification.classifier;

import com.genesys.knowledge.domain.Category;
import com.genesys.knowledge.domain.Document;
import org.junit.Before;
import org.junit.Test;

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
        Category category1 = new Category("category1");
        Document document1 = new Document("Test text to teach machine.");
        trainClassifier(document1, category1);

        Category category2 = new Category("category2");
        Document document2 = new Document("Real document that can be really applied.");
        trainClassifier(document2, category2);

        String expectedAlreadyTrainedDocumentCategory = category1.getId();
        String actualAlreadyTrainedDocumentCategory = classifier.calculateMostSuitableCategory(document1);
        assertEquals(expectedAlreadyTrainedDocumentCategory, actualAlreadyTrainedDocumentCategory);

        Document testDocument = new Document("Teaching machines is our hobby");
        String expectedTestDocumentCategory = category1.getId();
        String actualTestDocumentCategory = classifier.calculateMostSuitableCategory(testDocument);
        assertEquals(expectedTestDocumentCategory, actualTestDocumentCategory);
    }

    @Test
    public void testTrainClassifierWithOnlyOneDoc() {
        Category category = new Category("category");
        Document document = new Document("Test text");
        trainClassifier(document, category);

        Document testDocument = new Document("Teaching machines is our hobby");
        String expectedTestDocumentCategory = category.getId();
        String actualTestDocumentCategory = classifier.calculateMostSuitableCategory(testDocument);
        assertEquals(expectedTestDocumentCategory, actualTestDocumentCategory);
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
