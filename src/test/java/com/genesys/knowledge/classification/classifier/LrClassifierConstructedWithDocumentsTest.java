package com.genesys.knowledge.classification.classifier;

import com.genesys.knowledge.classification.util.DocumentHandler;
import com.genesys.knowledge.domain.Category;
import com.genesys.knowledge.domain.Document;
import org.apache.mahout.math.Vector;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.io.PrintWriter;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Created by rhorilyi on 27.04.2017.
 */
public class LrClassifierConstructedWithDocumentsTest {

    private List<Document> documents;

    @Before
    public void retrieveDocuments() {
        String url = "http://gks-dep-stbl:9092/gks-server/v1/kbs/langs/en/documents?tenantId=1&size=2000";
        String knowledgeBase = "bank_of_america";
        documents = DocumentHandler.retrieveDocuments(url, knowledgeBase);

        prepareDocuments();
    }

    private void prepareDocuments() {
        documents.removeIf(e -> (e.getText() == null));

        for (Document document : documents) {
            List<String> terms = DocumentHandler.convertTextToTerms(document.getText());
            document.setTerms(terms);
        }
        System.out.println(documents.size());
    }

    @Test
    public void testClassifierWithDifferentNumberOfTrainingLaps() {
        Collections.shuffle(documents, new SecureRandom());
        List<Document> trainingDocuments = documents.subList(0, 4 * documents.size() / 5);
        List<Document> testDocuments = documents.subList(4 * documents.size() / 5, documents.size());

        int[] trainingLapsNumbers = {10, 30, 50};
        for (int trainingLapsNumber : trainingLapsNumbers) {
            System.out.println("Number of training laps: " + trainingLapsNumber);
            testClassifier(trainingDocuments, testDocuments, trainingLapsNumber);
        }
    }

    @Test
    public void testClassifierCreatingConfidencePrecisionStatistics() {
        Collections.shuffle(documents, new SecureRandom());
        List<Document> trainingDocuments = documents.subList(0, 4 * documents.size() / 5);
        List<Document> testDocuments = documents.subList(4 * documents.size() / 5, documents.size());

        List<List<Integer>> statistics = new ArrayList<>(101);
        for (int i = 0; i <= 100; i++) {
            statistics.add(Arrays.asList(0, 0));
        }

        for (int run = 0; run < 50; run++) {
            LogisticRegressionClassifier classifier = new LogisticRegressionClassifier(trainingDocuments);
            classifier.getCategoryHandler().clear();

            // train
            trainClassifier(classifier, trainingDocuments, 30);

            // evaluate
            for (Document document : testDocuments) {
                Vector classificationVector = classifier.classifyDocument(document);
                for (Vector.Element element : classificationVector.all()) {
                    String classifiedCategoryId = classifier.getCategoryHandler().getCategoryId(element.index());
                    int confidence = (int) Math.round(element.get() * 100); // convert into percentage

                    statistics.get(confidence).set(1, statistics.get(confidence).get(1) + 1); // increment number of all predictions
                    if (document.getCategories().contains(new Category(classifiedCategoryId))) {
                        statistics.get(confidence).set(0, statistics.get(confidence).get(0) + 1); // increment number of successful predictions
                    }
                }
            }
        }

        String csvFile = "src/main/resources/confidence-precision-statistics.csv";
        try (PrintWriter writer = new PrintWriter(csvFile)) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < statistics.size(); i++) {
                List<Integer> precisionPair = statistics.get(i);
                double precision = ((double) precisionPair.get(0)) / precisionPair.get(1) * 100;
                sb.append(i).append(',')
                        .append(precision).append('\n');
            }
            writer.write(sb.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // TODO rename method 'cause it's confusing (note phases of classification process: training -> testing)
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

    private void trainClassifier(AbstractClassifier classifier,
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
