package com.genesys.knowledge.classification.classifier;

import com.genesys.knowledge.classification.util.CategoryHandler;
import com.genesys.knowledge.classification.util.DocumentHandler;
import com.genesys.knowledge.domain.Category;
import com.genesys.knowledge.domain.Document;
import org.apache.mahout.math.Vector;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.io.PrintWriter;
import java.security.SecureRandom;
import java.util.*;

/**
 * Created by rhorilyi on 27.04.2017.
 */
public class LrClassifierConstructedWithDocumentsTest {

    private List<Document> allDocuments;
    private String[] classifiers = {};

    @Before
    public void retrieveDocuments() {
        String url = "http://gks-dep-stbl:9092/gks-server/v1/kbs/langs/en/documents?tenantId=1&size=2000";
        String knowledgeBase = "bank_of_america";
        allDocuments = DocumentHandler.retrieveDocuments(url, knowledgeBase);

        prepareDocuments();
    }

    private void prepareDocuments() {
        allDocuments.removeIf(e -> (e.getText() == null));
        System.out.println(allDocuments.size());
    }

    @Test
    public void testClassifierWithDifferentNumberOfTrainingLaps() {
        Collections.shuffle(allDocuments, new SecureRandom());
        List<Document> trainingDocuments = allDocuments.subList(0, 4 * allDocuments.size() / 5);
        List<Document> testDocuments = allDocuments.subList(4 * allDocuments.size() / 5, allDocuments.size());

        int[] trainingLapsNumbers = {10, 30, 50};
        for (int trainingLapsNumber : trainingLapsNumbers) {
            System.out.println("Number of training laps: " + trainingLapsNumber);
            testClassifier(trainingDocuments, testDocuments, trainingLapsNumber);
        }
    }

    @Test
    public void testClassifierCalculatingConfidencePrecisionStatistics() {
        Collections.shuffle(allDocuments, new SecureRandom());
        List<Document> trainingDocuments = allDocuments.subList(0, 4 * allDocuments.size() / 5);
        List<Document> testDocuments = allDocuments.subList(4 * allDocuments.size() / 5, allDocuments.size());

        List<List<Integer>> statistics = new ArrayList<>(101);
        for (int i = 0; i <= 100; i++) {
            statistics.add(Arrays.asList(0, 0));
        }

        for (int run = 0; run < 50; run++) {
            LogisticRegressionClassifier classifier = new LogisticRegressionClassifier(trainingDocuments);
            CategoryHandler categoryHandler = classifier.getCategoryHandler();
            categoryHandler.clear();

            // train
            trainClassifier(classifier, trainingDocuments, 30);

            // evaluate
            for (Document document : testDocuments) {
                Vector classificationVector = classifier.classifyDocument(document);
                for (Vector.Element element : classificationVector.all()) {
                    Category classifiedCategory = categoryHandler.getCategory(element.index());
                    int confidence = (int) Math.round(element.get() * 100); // convert into percentage

                    statistics.get(confidence).set(1, statistics.get(confidence).get(1) + 1); // increment number of all predictions
                    if (document.getCategories().contains(classifiedCategory)) {
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

    @Test
    public void testClassifierCalculatingAveragePrecision() {
        Collections.shuffle(allDocuments, new SecureRandom());
        List<Document> trainingDocuments = allDocuments.subList(0, 4 * allDocuments.size() / 5);
        List<Document> testDocuments = allDocuments.subList(4 * allDocuments.size() / 5, allDocuments.size());

        for (int run = 0; run < 50; run++) {
            LogisticRegressionClassifier classifier = new LogisticRegressionClassifier(trainingDocuments);
            CategoryHandler categoryHandler = classifier.getCategoryHandler();
            categoryHandler.clear();

            // train
            trainClassifier(classifier, trainingDocuments, 30);

            // evaluate
            double sumAvgPrecision = 0;
            for (Document document : testDocuments) {
                Vector vector = classifier.classifyDocument(document);
                List<Category> classifiedCategories = getSortedCategories(vector, categoryHandler);

                List<Category> expectedCategories = document.getCategories();
                double avgPrecision = calcAvgPrecision(expectedCategories, classifiedCategories);
                sumAvgPrecision += avgPrecision;
            }
            double overallAvgPrecision = sumAvgPrecision / testDocuments.size();
            System.out.println(overallAvgPrecision);
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
        Map<Document, List<String>> documentTokens = new HashMap<>();
        for (Document trainingDoc : trainingDocuments) {
            List<String> tokens = DocumentHandler.convertTextToTokens(trainingDoc.getText(), null);
            documentTokens.put(trainingDoc, tokens);
        }

        for (int i = 0; i < trainingLapsNumber; i++) {
            Collections.shuffle(trainingDocuments, new SecureRandom());
            for (Document trainingDoc : trainingDocuments) {
                for (Category category : trainingDoc.getCategories()) {
                    classifier.train(documentTokens.get(trainingDoc), category);
                }
            }
        }
    }

    private int checkCorrectnessOfClassification(LogisticRegressionClassifier classifier,
                                                 List<Document> documents) {
        int numberOfCorrectClassificationsPerRun = 0;

        for (Document document : documents) {
            String mostSuitableCategory = classifier.calcMostSuitableCategory(document);
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

    private List<Category> getSortedCategories(Vector vector, CategoryHandler categoryHandler) {
        List<Category> sortedCategories = new ArrayList<>();

        Map<Double, Category> map = new TreeMap<>();
        for (Vector.Element element : vector.all()) {
            map.put(element.get(), categoryHandler.getCategory(element.index()));
        }
        for (Map.Entry<Double, Category> entry : map.entrySet()) {
            sortedCategories.add(entry.getValue());
        }
        Collections.reverse(sortedCategories);

        return sortedCategories;
    }

    private double calcAvgPrecision(Collection expected, Collection actual) {
        double s = 0.0;
        int n = actual.size(); // count of retrieved allDocuments
        int numRelevant = 0; // to keep track of number of relevant allDocuments seen so far

        Iterator iterator = actual.iterator();
        for (int i = 0; i < n; i++) {
            Object o = iterator.next();
            if (expected.contains(o)) {
                ++numRelevant;
                s += ((double)numRelevant / (double)(i + 1));
                if (numRelevant == expected.size()) {
                    break;
                }
            }
        }

        return s / (double)expected.size();
    }
}
