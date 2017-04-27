package com.genesys.knowledge.classifier;

import com.genesys.knowledge.classifier.exception.CategoryNotFoundException;
import com.genesys.knowledge.domain.Category;
import com.genesys.knowledge.domain.Document;
import org.junit.Test;

/**
 * Created by rhorilyi on 27.04.2017.
 */
public class LogisticRegressionClassifierTest {

    @Test
    public void testClassification() {
        LogisticRegressionClassifier classifier = new LogisticRegressionClassifier();

        String documentBody = "Test text";
        Category category = new Category("test_id");
        Category[] categories = {category};
        Document document = new Document(documentBody, categories);

        for (int trainLoop = 0; trainLoop < 30; trainLoop++) {
            classifier.train(document);
        }

        try {
            double categoryProbability = classifier.calculateCategoryProbability(document, category);
            System.out.println("Category probability: " + categoryProbability);
        } catch (CategoryNotFoundException e) {
            e.printStackTrace();
        }
    }
}
