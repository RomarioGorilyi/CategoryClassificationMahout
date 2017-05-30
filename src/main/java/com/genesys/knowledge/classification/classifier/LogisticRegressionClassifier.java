package com.genesys.knowledge.classification.classifier;

import com.genesys.knowledge.classification.defaults.ClassifierDefaults;
import com.genesys.knowledge.classification.defaults.LogisticRegressionDefaults;
import com.genesys.knowledge.classification.exception.CategoryNotFoundException;
import com.genesys.knowledge.classification.util.DocumentHandler;
import com.genesys.knowledge.classification.util.TfIdf;
import com.genesys.knowledge.domain.Category;
import com.genesys.knowledge.domain.Document;
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
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static com.genesys.knowledge.classification.util.TfIdf.*;

/**
 * Created by rhorilyi on 25.04.2017.
 */
@Slf4j
public class LogisticRegressionClassifier extends AbstractClassifier {

    private final ConstantValueEncoder interceptEncoder = new ConstantValueEncoder("intercept");
    private final FeatureVectorEncoder featureEncoder = new StaticWordValueEncoder("feature");

    public LogisticRegressionClassifier() {
        this(ClassifierDefaults.DEFAULT_NUM_CATEGORIES, ClassifierDefaults.DEFAULT_NUM_FEATURES);
    }

    public LogisticRegressionClassifier(int categoriesNumber, int featuresNumber) {
        super(new OnlineLogisticRegression(categoriesNumber, featuresNumber, new L2())
                .learningRate(LogisticRegressionDefaults.DEFAULT_LR_LEARNING_RATE)
                .alpha(LogisticRegressionDefaults.DEFAULT_LR_ALPHA)
                .lambda(LogisticRegressionDefaults.DEFAULT_LR_LAMBDA)
                .stepOffset(LogisticRegressionDefaults.DEFAULT_LR_STEP_OFFSET)
                .decayExponent(LogisticRegressionDefaults.DEFAULT_LR_DECAY_EXPONENT)
        );
    }

    public LogisticRegressionClassifier(List<Document> documents) {
        super();
        getCategoryHandler().initHandler(documents);

        setClassifier(new OnlineLogisticRegression(
                getCategoryHandler().getCategoriesQuantity(),
                DocumentHandler.findMaxNumberOfTokens(documents),
                new L2())
                .learningRate(LogisticRegressionDefaults.DEFAULT_LR_LEARNING_RATE)
                .alpha(LogisticRegressionDefaults.DEFAULT_LR_ALPHA)
                .lambda(LogisticRegressionDefaults.DEFAULT_LR_LAMBDA)
                .stepOffset(LogisticRegressionDefaults.DEFAULT_LR_STEP_OFFSET)
                .decayExponent(LogisticRegressionDefaults.DEFAULT_LR_DECAY_EXPONENT)
        );
    }

    public LogisticRegressionClassifier(byte[] modelData) {
        super();
        deserializeModel(modelData);
    }

    /**
     * Trains {@code this} classifier model with the specified {@code List<String>} of tokens and
     * the specified {@link Category}.
     *
     * @return {@code this} LogisticRegressionClassifier
     */
    public LogisticRegressionClassifier train(List<String> tokens, Category category,
                                              Collection<List<String>> documentTokens) {
        trainOnlineLogisticRegression(tokens, category, documentTokens);
        return this;
    }

    private void trainOnlineLogisticRegression(List<String> tokens, Category category,
                                               Collection<List<String>> documentTokens) {
        int categoryOrderNumber;
        try {
            categoryOrderNumber = getCategoryHandler().getCategoryOrderNumber(category);
        } catch (CategoryNotFoundException e) {
            getCategoryHandler().addCategory(category);
            categoryOrderNumber = getCategoryHandler().getCategoriesQuantity() - 1;
        }
        ((OnlineLogisticRegression) getClassifier()).train(categoryOrderNumber, getFeatureVector(tokens, documentTokens));
    }

    @Override
    public Vector getFeatureVector(List<String> tokens, Collection<List<String>> documentTokens) {
        Vector outputVector = new RandomAccessSparseVector(((OnlineLogisticRegression) getClassifier()).numFeatures());

        interceptEncoder.addToVector("1", outputVector); // output[0] is the intercept term
        // Look at the regression graph on the link below to see why we need the intercept.
        // http://statistiksoftware.blogspot.nl/2013/01/why-we-need-intercept.html
        for (String token : tokens) {
            featureEncoder.addToVector(token, calcTokenWeight(token, tokens, documentTokens), outputVector);
        }
        return outputVector;
    }

    private double calcTokenWeight(String targetToken, List<String> tokens, Collection<List<String>> documentTokens) {
        Map<String, Double> tf = tf(tokens, TfIdf.TfType.BOOLEAN);
        Map<String, Double> idf = idf(documentTokens);
        return tfIdf(tf, idf).get(targetToken);
    }

    @Override
    public byte[] serializeModel() {
        ByteArrayOutputStream byteOutput = new ByteArrayOutputStream();
        DataOutputStream dataOut = new DataOutputStream(new BufferedOutputStream(byteOutput));

        try {
            PolymorphicWritable.write(dataOut, (OnlineLogisticRegression) getClassifier());
        } catch (IOException e) {
            log.error(e.getMessage());
        }

        return byteOutput.toByteArray();
    }

    @Override
    public void deserializeModel(byte[] modelData) {
        DataInputStream dataIn = new DataInputStream(new BufferedInputStream(new ByteArrayInputStream(modelData)));

        try {
            setClassifier(PolymorphicWritable.read(dataIn, OnlineLogisticRegression.class));
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }
}
