package com.genesys.knowledge.classification.classifier;

import com.genesys.knowledge.classification.util.DocumentHandler;
import com.genesys.knowledge.domain.Category;
import com.genesys.knowledge.domain.Document;
import org.junit.Before;
import org.junit.Test;

import java.security.SecureRandom;
import java.util.Collections;
import java.util.List;

/**
 * Created by rhorilyi on 27.04.2017.
 */
public class LrClassifierConstructedWithDocumentsTest {

    private List<Document> documents;

    @Before
    public void retrieveDocuments() {
//        String urlTemplate = "http://gks-dep-stbl:9092/gks-server/v2/knowledge/tenants/1/langs/en_US/documents?size=%d&from=%d";
//        documents = new ArrayList<>(1000);
//
//        for (int i = 0; i < 3; i++) {
//            String url = String.format(urlTemplate, 200, i * 200);
//            documents.addAll(DocumentHandler.retrieveDocuments(url));
//        }
//
//        prepareDocuments();

        String url = "http://gks-dep-stbl:9092/gks-server/v1/kbs/langs/en/documents?tenantId=1&size=2000";
        String knowledgeBase = "bank_of_america";
        documents = DocumentHandler.retrieveDocuments(url, knowledgeBase);

        prepareDocuments();
    }


    @Test
    public void testClassifier() { // TODO split method into separate test methods in order to benchmark performance with JMH
        Collections.shuffle(documents, new SecureRandom());
        List<Document> trainingDocuments = documents.subList(0, 4 * documents.size() / 5);
        List<Document> testDocuments = documents.subList(4 * documents.size() / 5, documents.size());

        int[] trainingLoopsNumbers = {10, 30, 50, 100};
        for (int i = 0; i < trainingLoopsNumbers.length; i++) {
            System.out.println("Number of training laps: " + trainingLoopsNumbers[i]);
            testClassifier(trainingDocuments, testDocuments, trainingLoopsNumbers[i]);
        }
    }

    private void prepareDocuments() {
        documents.removeIf(e -> (e.getText() == null));

        for (Document document : documents) {
            List<String> terms = DocumentHandler.convertTextToTerms(document.getText());
            document.setTerms(terms);
        }
        System.out.println(documents.size());
    }

    private void testClassifier(List<Document> trainingDocuments,
                                List<Document> testDocuments,
                                int trainingLapsNumber) {
        int[] numberOfCorrectClassifications = new int[testDocuments.size() + 1];

        for (int run = 0; run < 50; run++) {
            LogisticRegressionClassifier classifier = new LogisticRegressionClassifier(trainingDocuments);
            classifier.getCategoryHandler().clear();

            // train
            trainClassifier(classifier, trainingDocuments, trainingLapsNumber);

            // evaluate
            int numberOfCorrectClassificationsPerRun = checkCorrectnessOfClassification(classifier, testDocuments);
            numberOfCorrectClassifications[numberOfCorrectClassificationsPerRun]++;
        }

        evaluateClassification(testDocuments, numberOfCorrectClassifications);
    }

    private void trainClassifier(LogisticRegressionClassifier classifier,
                                 List<Document> trainingDocuments,
                                 int trainingLapsNumber) {
        for (int i = 0; i < trainingLapsNumber; i++) {
            Collections.shuffle(trainingDocuments, new SecureRandom());
            for (Document trainingDoc : trainingDocuments) {
                classifier.train(trainingDoc);
            }
        }
    }

    private int checkCorrectnessOfClassification(LogisticRegressionClassifier classifier,
                                                 List<Document> documents) {
        int numberOfCorrectClassificationsPerRun = 0;

        for (Document document : documents) {
            String mostSuitableCategory = classifier.calculateMostSuitableCategory(document);
            List<Category> categories = document.getCategories();
            for (Category category : categories) {
                if (mostSuitableCategory.equals(category.getId())) {
                    numberOfCorrectClassificationsPerRun++;
                    break;
                }
            }
        }

        return numberOfCorrectClassificationsPerRun;
    }

    private void evaluateClassification(List<Document> testDocuments,
                                        int[] numberOfCorrectClassifications) {
        for (int i = 0; i < Math.floor(0.95 * testDocuments.size()); i++) {
            if (numberOfCorrectClassifications[i] != 0) {
                System.out.println("correct[" + i + "]=" + numberOfCorrectClassifications[i]);
            }
        }
        if (numberOfCorrectClassifications[testDocuments.size() - 1] != 0) {
            System.out.println("100%% accuracy detected: " + numberOfCorrectClassifications[testDocuments.size() - 1]
                    + " times");
        }
    }
}
