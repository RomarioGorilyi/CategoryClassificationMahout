package com.genesys.knowledge.classification;

import com.genesys.knowledge.classification.classifier.LogisticRegressionClassifier;
import com.genesys.knowledge.classification.util.DocumentHandler;
import com.genesys.knowledge.domain.Category;
import com.genesys.knowledge.domain.Document;
import org.junit.Test;

import java.security.SecureRandom;
import java.util.*;

/**
 * Created by rhorilyi on 27.04.2017.
 */
public class LrClassifierConstructedWithDocumentsTest {

    @Test
    public void testClassifier() {
        List<Document> documents = DocumentHandler.retrieveDocuments();
        Collections.shuffle(documents, new SecureRandom());
        List<Document> trainingDocuments = documents.subList(0, 2 * documents.size() / 3);
        List<Document> testDocuments = documents.subList(2 * documents.size() / 3, documents.size());

        int[] trainingLoopsNumbers = {10/*, 30, 50, 100*/};
        for (int i = 0; i < trainingLoopsNumbers.length; i++) {
            System.out.println("Number of training loops: " + trainingLoopsNumbers[i]);
            test(trainingDocuments, testDocuments, trainingLoopsNumbers[i]);
        }
    }

    private void test(List<Document> trainingDocuments, List<Document> testDocuments, int trainingLoopsNumber) {
        int[] correct = new int[testDocuments.size() + 1];
        for (int run = 0; run < 50; run++) {
            LogisticRegressionClassifier classifier = new LogisticRegressionClassifier(trainingDocuments);
            classifier.getCategoryHandler().clear();

            // train the model
            for (int i = 0; i < trainingLoopsNumber; i++) {
                Collections.shuffle(trainingDocuments, new SecureRandom());
                for (Document trainingDoc : trainingDocuments) {
                    classifier.train(trainingDoc);
                }
            }

            int successfulPredictionNumberPerLoop = testClassificationModel(classifier, testDocuments);
            correct[successfulPredictionNumberPerLoop]++;
        }

        // evaluate the model
        for (int i = 0; i < Math.floor(0.95 * testDocuments.size()); i++) {
            if (correct[i] != 0) {
                System.out.println("correct[" + i + "]=" + correct[i]);
            }
        }
        if (correct[testDocuments.size() - 1] != 0) {
            System.out.println("100%% accuracy detected: " + correct[testDocuments.size() - 1] + " times");
        }
    }

    private int testClassificationModel(LogisticRegressionClassifier classifier, List<Document> documents) {
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
