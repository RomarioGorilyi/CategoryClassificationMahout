package com.genesys.knowledge.classification.learner;

import com.genesys.knowledge.classification.exception.ClassifierNotTrainedException;
import com.genesys.knowledge.classification.util.CategoryHandler;
import com.genesys.knowledge.domain.Category;
import org.apache.mahout.math.Vector;
import org.junit.Test;

import java.io.IOException;
import java.io.PrintWriter;
import java.security.SecureRandom;
import java.util.*;

import static org.junit.Assert.assertEquals;

/**
 * Created by rhorilyi on 25.07.2017.
 */
public class LearnerTest {

    private String datasetLocation = "src/main/resources/dataset/boa.json";

    @Test
    public void testClassifyDocumentTopResults() throws IOException, ClassifierNotTrainedException {
        List<Learner.Document> documents = Learner.convertJsonToDocuments(datasetLocation);
        Learner learner = new Learner(new ArrayList<>(documents));
        learner.trainClassifier(documents);

        List<Vector.Element> elements = learner.classifyDocument(documents.get(new SecureRandom().nextInt(documents.size() - 1)), 3);
        for (Vector.Element element : elements) {
            System.out.println(element.get());
        }
    }

    @Test
    public void testConvertJsonToDocuments() {
        try {
            List<Learner.Document> documents = Learner.convertJsonToDocuments(datasetLocation);
            assertEquals(441, documents.size());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testLearner() throws IOException, ClassifierNotTrainedException {
        List<Learner.Document> allDocuments = Learner.convertJsonToDocuments(datasetLocation);
        Collections.shuffle(allDocuments, new SecureRandom());
        List<Learner.Document> trainingDocuments = allDocuments.subList(0, 4 * allDocuments.size() / 5);
        List<Learner.Document> testDocuments = allDocuments.subList(4 * allDocuments.size() / 5, allDocuments.size());

        // 1
        int[] numberOfCorrectClassifications = new int[testDocuments.size() + 1];
        // 2
        List<List<Integer>> allConfidenceScoresStatistics = new ArrayList<>(101);
        for (int i = 0; i <= 100; i++) {
            allConfidenceScoresStatistics.add(Arrays.asList(0, 0));
        }
        // 3
        double avgPrecision = 0;

        for (int run = 0; run < 50; run++) {
            Learner learner = new Learner(new ArrayList<>(trainingDocuments));
            learner.trainClassifier(trainingDocuments);

//            CategoryHandler categoryHandler = classifier.getCategoryHandler();
//            categoryHandler.clear();


            // evaluate
            // 1
            int numberOfCorrectClassificationsPerRun = evaluateBestConfidentCategoriesPrecision(learner, testDocuments);
            numberOfCorrectClassifications[numberOfCorrectClassificationsPerRun]++;
            // 2
            evaluateAllConfidenceScoresPrecision(learner, testDocuments, allConfidenceScoresStatistics);
            // 3
            avgPrecision += evaluateAveragePrecision(learner, testDocuments);
        }

        // analyze evaluation
        analyzeBestConfidentCategoriesPrecisionEvaluation(testDocuments.size(), numberOfCorrectClassifications);
        analyzeAllConfidenceScoresPrecisionEvaluation(allConfidenceScoresStatistics);
        analyzeAveragePrecisionEvaluation(avgPrecision);
    }

    private int evaluateBestConfidentCategoriesPrecision(Learner learner, List<Learner.Document> documents)
            throws ClassifierNotTrainedException {
        int numberOfCorrectClassificationsPerRun = 0;

        for (Learner.Document document : documents) {
            String mostConfidentCategory = learner.classifyDocumentWithMostConfidentCategory(document);
            List<String> categories = document.getCategories();
            for (String categoryId : categories) {
                if (mostConfidentCategory.equals(categoryId)) {
                    numberOfCorrectClassificationsPerRun++;
                    break;
                }
            }
        }

        return numberOfCorrectClassificationsPerRun;
    }

    private void evaluateAllConfidenceScoresPrecision(Learner learner,
                                                      List<Learner.Document> testDocuments,
                                                      List<List<Integer>> statistics)
            throws ClassifierNotTrainedException {
        CategoryHandler categoryHandler = learner.getClassifier().getCategoryHandler();

        for (Learner.Document document : testDocuments) {
            Vector vector = learner.classifyDocument(document);
            for (Vector.Element element : vector.all()) {
                Category classifiedCategory = categoryHandler.getCategory(element.index());
                int confidence = (int) Math.round(element.get() * 100); // convert into percentage

                statistics.get(confidence).set(1, statistics.get(confidence).get(1) + 1); // increment number of all predictions
                if (document.getCategories().contains(classifiedCategory.getId())) {
                    statistics.get(confidence).set(0, statistics.get(confidence).get(0) + 1); // increment number of successful predictions
                }
            }
        }
    }

    private double evaluateAveragePrecision(Learner learner, List<Learner.Document> testDocuments)
            throws ClassifierNotTrainedException {
        double sumAvgPrecision = 0;
        CategoryHandler categoryHandler = learner.getClassifier().getCategoryHandler();

        for (Learner.Document document : testDocuments) {
            Vector vector = learner.classifyDocument(document);
            List<String> classifiedCategoryIds = getSortedCategoryIds(vector, categoryHandler);

            List<String> expectedCategories = document.getCategories();
            double avgPrecision = calcAvgPrecision(expectedCategories, classifiedCategoryIds);
            sumAvgPrecision += avgPrecision;
        }

        return sumAvgPrecision / testDocuments.size();
    }

    private void analyzeBestConfidentCategoriesPrecisionEvaluation(int testDocumentsNumber,
                                                                   int[] correctClassificationsNumber) {
        for (int i = 0; i < Math.floor(0.95 * testDocumentsNumber); i++) {
            if (correctClassificationsNumber[i] != 0) {
                System.out.println("correct[" + i + "]=" + correctClassificationsNumber[i]);
            }
        }
        if (correctClassificationsNumber[testDocumentsNumber - 1] != 0) {
            System.out.println("100%% accuracy detected: " + correctClassificationsNumber[testDocumentsNumber - 1]
                    + " times");
        }
    }

    private void analyzeAllConfidenceScoresPrecisionEvaluation(List<List<Integer>> allConfidenceScoresStatistics) {
        String csvFile = "src/main/resources/confidence-precision-statistics.csv";
        try (PrintWriter writer = new PrintWriter(csvFile)) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < allConfidenceScoresStatistics.size(); i++) {
                List<Integer> precisionPair = allConfidenceScoresStatistics.get(i);
                double precision = ((double) precisionPair.get(0)) / precisionPair.get(1) * 100;
                sb.append(i).append(',').append(precision).append('\n');
            }
            writer.write(sb.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void analyzeAveragePrecisionEvaluation(double avgPrecision) {
        double overallAvgPrecision = avgPrecision / 50;
        System.out.println("Average precision: " + overallAvgPrecision);
    }

    private List<String> getSortedCategoryIds(Vector vector, CategoryHandler categoryHandler) {
        List<String> sortedCategoryIds = new ArrayList<>();

        Map<Double, String> map = new TreeMap<>();
        for (Vector.Element element : vector.all()) {
            map.put(element.get(), categoryHandler.getCategory(element.index()).getId());
        }
        for (Map.Entry<Double, String> entry : map.entrySet()) {
            sortedCategoryIds.add(entry.getValue());
        }
        Collections.reverse(sortedCategoryIds);

        return sortedCategoryIds;
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
