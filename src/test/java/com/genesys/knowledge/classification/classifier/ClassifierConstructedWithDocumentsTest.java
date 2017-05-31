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
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.security.SecureRandom;
import java.util.*;

import static com.genesys.knowledge.classification.util.DocumentHandler.convertTextToTokens;

/**
 * Created by rhorilyi on 27.04.2017.
 */
public class ClassifierConstructedWithDocumentsTest {

	private List<Document> allDocuments;
	Map<Document, List<String>> documentTokens;

	@Before
	public void retrieveDocuments() {
		String url = "http://gks-dep-stbl:9092/gks-server/v1/kbs/langs/en/documents?tenantId=1&size=2000";
		String knowledgeBase = "bank_of_america";
		allDocuments = DocumentHandler.retrieveDocuments(url, knowledgeBase);

		prepareDocuments();

		documentTokens = new HashMap<>();
		for (Document trainingDoc : allDocuments) {
			List<String> tokens = convertTextToTokens(trainingDoc.getText(), null);
			documentTokens.put(trainingDoc, tokens);
		}
	}

	private void prepareDocuments() {
		allDocuments.removeIf(e -> (e.getText() == null));
		System.out.println("Number of documents: " + allDocuments.size());
	}

	@Test
	public void testLogisticRegressionClassifier() {
		testClassifier("com.genesys.knowledge.classification.classifier.LogisticRegressionClassifier");
	}

	@Test
    public void testNaiveBayesClassifier() {
		testClassifier("com.genesys.knowledge.classification.classifier.NaiveBayesClassifier");
    }

	private void testClassifier(String classifierName) {
		Collections.shuffle(allDocuments, new SecureRandom());
		List<Document> trainingDocuments = allDocuments.subList(0, 4 * allDocuments.size() / 5);
		List<Document> testDocuments = allDocuments.subList(4 * allDocuments.size() / 5, allDocuments.size());

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
			AbstractClassifier classifier = createClassifierInstance(classifierName, trainingDocuments);
			CategoryHandler categoryHandler = classifier.getCategoryHandler();
			categoryHandler.clear();

			if ("com.genesys.knowledge.classification.classifier.LogisticRegressionClassifier".equals(classifierName)) {
				// train
				trainClassifier((LogisticRegressionClassifier) classifier, trainingDocuments);
			}

			// evaluate
			// 1
			int numberOfCorrectClassificationsPerRun = evaluateBestConfidentCategoriesPrecision(classifier, testDocuments);
			numberOfCorrectClassifications[numberOfCorrectClassificationsPerRun]++;
			// 2
			evaluateAllConfidenceScoresPrecision(classifier, testDocuments, allConfidenceScoresStatistics);
			// 3
			avgPrecision += evaluateAveragePrecision(classifier, testDocuments);
		}

		// analyze evaluation
		analyzeBestConfidentCategoriesPrecisionEvaluation(testDocuments.size(), numberOfCorrectClassifications);
		analyzeAllConfidenceScoresPrecisionEvaluation(allConfidenceScoresStatistics);
		analyzeAveragePrecisionEvaluation(avgPrecision);
	}

	private AbstractClassifier createClassifierInstance(String classifierName, List<Document> trainingDocuments) {
		AbstractClassifier classifier = null;

		try {
			Class<?> classifierDefinition = AbstractClassifier.class.getClassLoader().loadClass(classifierName);
			Class<?>[] argsClass = new Class<?>[]{List.class};
			Constructor<?> argsConstructor = classifierDefinition.getConstructor(argsClass);
			Object[] args = new Object[] {trainingDocuments};
			classifier = (AbstractClassifier) argsConstructor.newInstance(args);
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (InstantiationException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		}

		return classifier;
	}

	private void trainClassifier(LogisticRegressionClassifier classifier, List<Document> trainingDocuments) {
		for (int i = 0; i < 30; i++) {
			Collections.shuffle(trainingDocuments, new SecureRandom());
			for (Document trainingDoc : trainingDocuments) {
				for (Category category : trainingDoc.getCategories()) {
					classifier.train(documentTokens.get(trainingDoc), category, documentTokens.values());
				}
			}
		}
	}

    private int evaluateBestConfidentCategoriesPrecision(AbstractClassifier classifier,
                                                         List<Document> documents) {
        int numberOfCorrectClassificationsPerRun = 0;

        for (Document document : documents) {
			List<String> tokens = convertTextToTokens(document.getText(), null);
			String mostConfidentCategory = classifier.calcMostConfidentCategory(tokens, documentTokens.values());
            List<Category> categories = document.getCategories();
            for (Category category : categories) {
                if (mostConfidentCategory.equals(category.getId())) {
                    numberOfCorrectClassificationsPerRun++;
                    break;
                }
            }
        }

        return numberOfCorrectClassificationsPerRun;
    }

    private void evaluateAllConfidenceScoresPrecision(AbstractClassifier classifier,
                                                      List<Document> testDocuments,
                                                      List<List<Integer>> statistics) {
        CategoryHandler categoryHandler = classifier.getCategoryHandler();

        for (Document document : testDocuments) {
			List<String> tokens = convertTextToTokens(document.getText(), null);
            Vector classificationVector = classifier.classifyDocument(tokens, documentTokens.values());
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

    private double evaluateAveragePrecision(AbstractClassifier classifier,
                                            List<Document> testDocuments) {
        double sumAvgPrecision = 0;
        CategoryHandler categoryHandler = classifier.getCategoryHandler();

        for (Document document : testDocuments) {
			List<String> tokens = convertTextToTokens(document.getText(), null);
            Vector vector = classifier.classifyDocument(tokens, documentTokens.values());
            List<Category> classifiedCategories = getSortedCategories(vector, categoryHandler);

            List<Category> expectedCategories = document.getCategories();
            double avgPrecision = calcAvgPrecision(expectedCategories, classifiedCategories);
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
