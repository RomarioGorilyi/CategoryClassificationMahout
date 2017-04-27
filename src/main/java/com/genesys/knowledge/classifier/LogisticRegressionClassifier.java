package com.genesys.knowledge.classifier;

import com.genesys.knowledge.classifier.defaults.ClassifierDefaults;
import com.genesys.knowledge.classifier.defaults.LogisticRegressionDefaults;
import com.genesys.knowledge.classifier.exception.CategoryNotFoundException;
import com.genesys.knowledge.classifier.util.CategoriesHandler;
import com.genesys.knowledge.classifier.util.DocumentsHandler;
import com.genesys.knowledge.domain.Category;
import com.genesys.knowledge.domain.Document;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.mahout.classifier.sgd.L1;
import org.apache.mahout.classifier.sgd.OnlineLogisticRegression;
import org.apache.mahout.classifier.sgd.PolymorphicWritable;
import org.apache.mahout.math.RandomAccessSparseVector;
import org.apache.mahout.math.Vector;
import org.apache.mahout.vectorizer.encoders.ConstantValueEncoder;
import org.apache.mahout.vectorizer.encoders.FeatureVectorEncoder;
import org.apache.mahout.vectorizer.encoders.StaticWordValueEncoder;

import java.io.*;
import java.util.Arrays;

/**
 * Created by rhorilyi on 25.04.2017.
 */
@Slf4j
public class LogisticRegressionClassifier extends AbstractClassifier {

    @Getter
    private CategoriesHandler categoriesHandler;
    private OnlineLogisticRegression lr;

    private final ConstantValueEncoder interceptEncoder = new ConstantValueEncoder("intercept");
    private final FeatureVectorEncoder featureEncoder = new StaticWordValueEncoder("feature");

    public LogisticRegressionClassifier() {
        categoriesHandler = new CategoriesHandler();

        lr = new OnlineLogisticRegression(
                ClassifierDefaults.DEFAULT_NUM_CATEGORIES,
                ClassifierDefaults.DEFAULT_NUM_FEATURES,
                new L1())
                .learningRate(LogisticRegressionDefaults.DEFAULT_LR_LEARNING_RATE)
                .alpha(LogisticRegressionDefaults.DEFAULT_LR_ALPHA)
                .lambda(LogisticRegressionDefaults.DEFAULT_LR_LAMBDA)
                .stepOffset(LogisticRegressionDefaults.DEFAULT_LR_STEP_OFFSET)
                .decayExponent(LogisticRegressionDefaults.DEFAULT_LR_DECAY_EXPONENT);
    }

    public LogisticRegressionClassifier(Document[] documents) {
        categoriesHandler = new CategoriesHandler();
        categoriesHandler.initHandler(documents);

        lr = new OnlineLogisticRegression(
                categoriesHandler.getCategoriesQuantity(),
                1000,
                new L1())
                .learningRate(LogisticRegressionDefaults.DEFAULT_LR_LEARNING_RATE)
                .alpha(LogisticRegressionDefaults.DEFAULT_LR_ALPHA)
                .lambda(LogisticRegressionDefaults.DEFAULT_LR_LAMBDA)
                .stepOffset(LogisticRegressionDefaults.DEFAULT_LR_STEP_OFFSET)
                .decayExponent(LogisticRegressionDefaults.DEFAULT_LR_DECAY_EXPONENT);
    }

    public LogisticRegressionClassifier(byte[] modelData) {
        categoriesHandler = new CategoriesHandler();
        deserializeModel(modelData);
    }

    @Override
    public void train(Document document) {
        if (document == null || document.getBody().isEmpty() || document.getCategories().length == 0) {
            return;
        }

        for (Category category : document.getCategories()) {
            int categoryOrderNumber;
            try {
                categoryOrderNumber = categoriesHandler.getCategoryOrderNumber(category);
            } catch (CategoryNotFoundException e) {
                categoriesHandler.addCategory(category);
                categoryOrderNumber = categoriesHandler.getCategoriesQuantity() - 1;
            }
            lr.train(categoryOrderNumber, getFeatureVector(document));
        }
    }

    // TODO think over: maybe remove into test class
    public double calculateCategoryProbability(Document document, Category category) throws CategoryNotFoundException {
        Vector vector = lr.classifyFull(getFeatureVector(document));
        return lr.classifyFull(getFeatureVector(document)).get(categoriesHandler.getCategoryOrderNumber(category));
    }

    public int classifyBestCategory(Document document) {
        return lr.classifyFull(getFeatureVector(document)).maxValueIndex();
    }

    private RandomAccessSparseVector getFeatureVector(Document document) {
        RandomAccessSparseVector outputVector = new RandomAccessSparseVector(lr.numFeatures());

        interceptEncoder.addToVector("1", outputVector); // output[0] is the intercept term
        // Look at the regression graph on the link below to see why we need the intercept.
        // http://statistiksoftware.blogspot.nl/2013/01/why-we-need-intercept.html

        String[] words = document.getBody()
                .replaceAll("[^\\w]", "")
                .toLowerCase()
                .split("\\s+");
        for (String word : words) {
            featureEncoder.addToVector(word, 1, outputVector);
        }

        return outputVector;
    }

    @Override
    public byte[] serializeModel() {
        ByteArrayOutputStream byteOutput = new ByteArrayOutputStream();
        DataOutputStream dataOut = new DataOutputStream(new BufferedOutputStream(byteOutput));

        try {
            PolymorphicWritable.write(dataOut, lr);
        } catch (IOException e) {
            log.error(e.getMessage());
        }

        return byteOutput.toByteArray();
    }

    @Override
    public void deserializeModel(byte[] modelData) {
        DataInputStream dataIn = new DataInputStream(new BufferedInputStream(new ByteArrayInputStream(modelData)));

        try {
            lr = PolymorphicWritable.read(dataIn, OnlineLogisticRegression.class);
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }

    public static void main(String[] args) {
        Document[] documents = DocumentsHandler.retrieveDocuments();
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

            // test the model
            // TODO improve evaluation to check all categories of documents
            int x = 0;
            CategoriesHandler categoriesHandler = classifier.getCategoriesHandler();
            //int[] count = new int[categoriesHandler.getCategoriesQuantity()];
            for (Document testDoc : testDocuments) {
                for (Category category : testDoc.getCategories()) {
                    categoriesHandler.addCategory(category);
                }
                int bestCategory = classifier.classifyBestCategory(testDoc);
                //count[bestCategory]++;
                Category[] categories = testDoc.getCategories();
                for (Category category : categories) {
                    try {
                        if (bestCategory == categoriesHandler.getCategoryOrderNumber(category)) {
                            x++;
                            break;
                        }
                    } catch (CategoryNotFoundException e) {
                        e.printStackTrace(); // TODO remove after debug
                        log.error(e.getMessage());
                    }
                }
            }
            System.out.println(x);
            correct[x]++;
        }
        System.out.println(Arrays.toString(correct));

        // assess accuracy
        for (int i = 0; i < Math.floor(0.95 * testDocuments.length); i++) {
            if (correct[i] != 0) {
                System.out.println("correct[" + i + "]=" + correct[i]);
            }
        }

        if (correct[testDocuments.length - 1] != 0) {
            System.out.println("100%% accuracy detected: " + correct[testDocuments.length - 1] + " times");
        }

        LogisticRegressionClassifier classifier = new LogisticRegressionClassifier(documents);

    }
}
