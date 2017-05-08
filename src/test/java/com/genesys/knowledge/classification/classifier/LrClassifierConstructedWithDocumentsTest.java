package com.genesys.knowledge.classification;

import com.genesys.knowledge.classification.classifier.LogisticRegressionClassifier;
import com.genesys.knowledge.classification.util.DocumentHandler;
import com.genesys.knowledge.domain.Category;
import com.genesys.knowledge.domain.Document;
import org.junit.Before;
import org.junit.Test;

import java.security.SecureRandom;
import java.util.*;

/**
 * Created by rhorilyi on 27.04.2017.
 */
public class LrClassifierConstructedWithDocumentsTest {

    private List<Document> documents;
    private String urlTemplate = "http://gks-dep-stbl:9092/gks-server/v2/knowledge/tenants/1/langs/en_US/documents?size=%d&from=%d";

    @Before
    public void retrieveDocuments() {
        documents = new ArrayList<>(1000);
        for (int i = 0; i < 3; i++) {
            String url = String.format(urlTemplate, 200, i * 200);
            documents.addAll(DocumentHandler.retrieveDocuments(url));
        }

        initDocumentsTerms();
    }

    @Test
    public void testClassifier() {
        Collections.shuffle(documents, new SecureRandom());
        List<Document> trainingDocuments = documents.subList(0, 4 * documents.size() / 5);
        List<Document> testDocuments = documents.subList(4 * documents.size() / 5, documents.size());

        int[] trainingLoopsNumbers = {10, 30, 50, 100};
        for (int i = 0; i < trainingLoopsNumbers.length; i++) {
            System.out.println("Number of training loops: " + trainingLoopsNumbers[i]);
            test(trainingDocuments, testDocuments, trainingLoopsNumbers[i]);
        }
    }

    private void initDocumentsTerms() {
        for (Document document : documents) {
            List<String> terms = DocumentHandler.convertTextToTerms(document.getText());
            document.setTerms(terms);
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
            List<Category> categories = document.getCategories();
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