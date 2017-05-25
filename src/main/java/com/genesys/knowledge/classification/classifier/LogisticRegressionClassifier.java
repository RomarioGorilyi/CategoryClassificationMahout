package com.genesys.knowledge.classification.classifier;

import com.genesys.knowledge.classification.defaults.ClassifierDefaults;
import com.genesys.knowledge.classification.defaults.LogisticRegressionDefaults;
import com.genesys.knowledge.classification.exception.CategoryNotFoundException;
import com.genesys.knowledge.classification.util.CategoryHandler;
import com.genesys.knowledge.classification.util.DocumentHandler;
import com.genesys.knowledge.domain.Category;
import com.genesys.knowledge.domain.Document;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.mahout.classifier.sgd.L2;
import org.apache.mahout.classifier.sgd.OnlineLogisticRegression;
import org.apache.mahout.classifier.sgd.PolymorphicWritable;
import org.apache.mahout.math.RandomAccessSparseVector;
import org.apache.mahout.math.Vector;
import org.apache.mahout.vectorizer.encoders.ConstantValueEncoder;
import org.apache.mahout.vectorizer.encoders.FeatureVectorEncoder;
import org.apache.mahout.vectorizer.encoders.StaticWordValueEncoder;

import java.io.*;
import java.util.*;

/**
 * Created by rhorilyi on 25.04.2017.
 */
@Slf4j
public class LogisticRegressionClassifier extends AbstractClassifier {

    @Getter
    private CategoryHandler categoryHandler;
    @Getter
    private OnlineLogisticRegression lr;

    private final ConstantValueEncoder interceptEncoder = new ConstantValueEncoder("intercept");
    private final FeatureVectorEncoder featureEncoder = new StaticWordValueEncoder("feature");

    public LogisticRegressionClassifier() {
        categoryHandler = new CategoryHandler();

        lr = new OnlineLogisticRegression(
                ClassifierDefaults.DEFAULT_NUM_CATEGORIES,
                ClassifierDefaults.DEFAULT_NUM_FEATURES,
                new L2())
                .learningRate(LogisticRegressionDefaults.DEFAULT_LR_LEARNING_RATE)
                .alpha(LogisticRegressionDefaults.DEFAULT_LR_ALPHA)
                .lambda(LogisticRegressionDefaults.DEFAULT_LR_LAMBDA)
                .stepOffset(LogisticRegressionDefaults.DEFAULT_LR_STEP_OFFSET)
                .decayExponent(LogisticRegressionDefaults.DEFAULT_LR_DECAY_EXPONENT);
    }

    public LogisticRegressionClassifier(int categoriesNumber, int featuresNumber) {
        categoryHandler = new CategoryHandler();

        lr = new OnlineLogisticRegression(categoriesNumber, featuresNumber, new L2())
                .learningRate(LogisticRegressionDefaults.DEFAULT_LR_LEARNING_RATE)
                .alpha(LogisticRegressionDefaults.DEFAULT_LR_ALPHA)
                .lambda(LogisticRegressionDefaults.DEFAULT_LR_LAMBDA)
                .stepOffset(LogisticRegressionDefaults.DEFAULT_LR_STEP_OFFSET)
                .decayExponent(LogisticRegressionDefaults.DEFAULT_LR_DECAY_EXPONENT);
    }

    public LogisticRegressionClassifier(List<Document> documents) {
        categoryHandler = new CategoryHandler();
        categoryHandler.initHandler(documents);

        lr = new OnlineLogisticRegression(
                categoryHandler.getCategoriesQuantity(),
                DocumentHandler.findMaxNumberOfTokens(documents),
                new L2())
                .learningRate(LogisticRegressionDefaults.DEFAULT_LR_LEARNING_RATE)
                .alpha(LogisticRegressionDefaults.DEFAULT_LR_ALPHA)
                .lambda(LogisticRegressionDefaults.DEFAULT_LR_LAMBDA)
                .stepOffset(LogisticRegressionDefaults.DEFAULT_LR_STEP_OFFSET)
                .decayExponent(LogisticRegressionDefaults.DEFAULT_LR_DECAY_EXPONENT);
    }

    public LogisticRegressionClassifier(byte[] modelData) {
        categoryHandler = new CategoryHandler();
        deserializeModel(modelData);
    }

    /**
     * Trains {@code this} classifier model with the specified {@link Document},
     * if the specified {@link Document} is valid and has at least 1 category.
     *
     * @return {@code this} LogisticRegressionClassifier
     */
    @Override
    public LogisticRegressionClassifier train(Document document) {
        if (isDocumentValid(document) && document.getCategories().size() != 0) {
            for (Category category : document.getCategories()) {
                trainOnlineLogisticRegression(document, category);
            }
        }

        return this;
    }

    /**
     * Trains {@code this} classifier model with the specified {@link Document} and {@link Category},
     * if the specified {@link Document} is valid.
     *
     * @return {@code this} LogisticRegressionClassifier
     */
    public LogisticRegressionClassifier train(Document document, Category category) {
        if (isDocumentValid(document)) {
            trainOnlineLogisticRegression(document, category);
        }
        return this;
    }

    public Vector classifyDocument(Document document) {
        return lr.classifyFull(getFeatureVector(document));
    }

    /**
     * Calculates probability that the specified {@link Document} has the specified {@link Category}.
     * If {@code this} LogisticRegressionClassifier's {@link #categoryHandler} doesn't contain the category,
     * return 0.
     *
     * @param document document to classify
     * @param category category to check its presence probability in the specified document
     * @return probability as {@code double} value
     */
    public double calcCategoryProbability(Document document, Category category) {
        double probability;
        try {
            probability = lr.classifyFull(getFeatureVector(document)).get(categoryHandler.getCategoryOrderNumber(category));
        } catch (CategoryNotFoundException e) {
            probability = 0;
        }

        return probability;
    }

    /**
     * Classify the specified document calculating id of its most suitable {@link Category}.
     *
     * @param document document to classify
     * @return {@code String} category id
     */
    public String calcMostSuitableCategory(Document document) {
        int index = lr.classifyFull(getFeatureVector(document)).maxValueIndex();
        return categoryHandler.getCategory(index).getId();
    }

    private boolean isDocumentValid(Document document) {
        return !(document == null || document.getText() == null || document.getText().isEmpty());
    }

    private void trainOnlineLogisticRegression(Document document, Category category) {
        int categoryOrderNumber;
        try {
            categoryOrderNumber = categoryHandler.getCategoryOrderNumber(category);
        } catch (CategoryNotFoundException e) {
            categoryHandler.addCategory(category);
            categoryOrderNumber = categoryHandler.getCategoriesQuantity() - 1;
        }
        lr.train(categoryOrderNumber, getFeatureVector(document));
    }

    private Vector getFeatureVector(Document document) {
        Vector outputVector = new RandomAccessSparseVector(lr.numFeatures());

        interceptEncoder.addToVector("1", outputVector); // output[0] is the intercept term
        // Look at the regression graph on the link below to see why we need the intercept.
        // http://statistiksoftware.blogspot.nl/2013/01/why-we-need-intercept.html

        List<String> terms = document.getTokens();
        for (String term : terms) {
            featureEncoder.addToVector(term, 2, outputVector);
        }

        return outputVector;
    }

//    private double calcWeight(String term, List<String> tokens) {
//        double termFreq = 0;
//        for (String t : tokens) {
//            if (term.equals(t)) {
//                termFreq++;
//            }
//        }
//
//        return 0.5 + 0.5 * (termFreq / calcMaxTermFreq(tokens));
////        return 2;
//    }
//
//    private int calcMaxTermFreq(List<String> tokens) {
//        int maxTermFreq = 0;
//
//        Map<String, Integer> frequencies = new HashMap<>();
//        for (String term : tokens) {
//            frequencies.put(term, frequencies.getOrDefault(term, 0) + 1);
//        }
//        Collection<Integer> freqValues = frequencies.values();
//        for (int freq : freqValues) {
//            if (freq > maxTermFreq) {
//                maxTermFreq = freq;
//            }
//        }
//
//        return maxTermFreq;
//    }
//
//    private int calcTermFreq(String term) {
//        int termFreq = 0;
//
//        for (Document doc : documents) {
//            if (doc.getTokens().contains(term)) {
//                termFreq++;
//            }
//        }
//
//        return termFreq;
//    }

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
}
