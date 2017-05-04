package com.genesys.knowledge.classification;

import com.genesys.knowledge.classification.util.DocumentHandler;
import com.genesys.knowledge.domain.Category;
import com.genesys.knowledge.domain.Document;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * Created by rhorilyi on 27.04.2017.
 */
public class LogisticRegressionClassifierTest {

    @Test
    public void testClassifierWhichIsInitializedWithDocuments() {
    }

    @Test
    public void testClassifierWhichIsInitializedWithRetrievedDocuments() {
        Document[] documents = DocumentHandler.retrieveDocuments();
        Document[] trainDocuments = Arrays.copyOfRange(documents, 0, 2 * documents.length / 3);
        Document[] testDocuments = Arrays.copyOfRange(documents, 2 * documents.length / 3, documents.length);

        int[] correct = new int[testDocuments.length + 1];
        for (int run = 0; run < 200; run++) {
            LogisticRegressionClassifier classifier = new LogisticRegressionClassifier(trainDocuments);

            // train the model
            for (int trainLoop = 0; trainLoop < 30; trainLoop++) {
                for (Document trainDoc : trainDocuments) {
                    classifier.train(trainDoc);
                }
            }

            int successfulPredictionNumberPerLoop = testClassificationModel(classifier, testDocuments);
            System.out.println(successfulPredictionNumberPerLoop); // TODO remove after debug

            correct[successfulPredictionNumberPerLoop]++;
        }
        System.out.println(Arrays.toString(correct)); // TODO remove after debug

        // evaluate the model
        for (int i = 0; i < Math.floor(0.95 * testDocuments.length); i++) {
            if (correct[i] != 0) {
                System.out.println("correct[" + i + "]=" + correct[i]);
            }
        }
        if (correct[testDocuments.length - 1] != 0) {
            System.out.println("100%% accuracy detected: " + correct[testDocuments.length - 1] + " times");
        }
    }

    private int testClassificationModel(LogisticRegressionClassifier classifier, Document[] documents) {
        // TODO improve evaluation to check all categories of documents
        int successfulPredictionNumberPerLoop = 0;
        com.genesys.knowledge.classification.util.CategoryHandler categoryHandler = classifier.getCategoryHandler();
        for (Document document : documents) {
            for (Category category : document.getCategories()) {
                categoryHandler.addCategory(category);
            }
            String mostRelevantCategory = classifier.calculateMostRelevantCategory(document);
            ArrayList<Category> categories = document.getCategories();
            for (Category category : categories) {
                if (mostRelevantCategory.equals(category.getId())) {
                    successfulPredictionNumberPerLoop++;
                    break;
                }
            }
        }

        return successfulPredictionNumberPerLoop;
    }
}
