package com.genesys.knowledge.classification.classifier.feature;

import com.genesys.knowledge.classification.classifier.LogisticRegressionClassifier;
import com.genesys.knowledge.classification.learner.Learner;
import org.apache.mahout.classifier.sgd.OnlineLogisticRegression;
import org.apache.mahout.math.RandomAccessSparseVector;
import org.apache.mahout.math.Vector;

import java.util.List;

/**
 * Created by rhorilyi on 24.07.2017.
 */
public interface FeatureVectorHandler {

    default Vector getFeatureVector(LogisticRegressionClassifier classifier,
                                    Learner.Document document,
                                    List<Learner.Document> allDocuments) {
        Vector outputVector = new RandomAccessSparseVector(((OnlineLogisticRegression) classifier.getClassifier()).numFeatures());

        classifier.getInterceptEncoder().addToVector("1", outputVector); // output[0] is the intercept term
        // Look at the regression graph on the link below to see why we need the intercept.
        // http://statistiksoftware.blogspot.nl/2013/01/why-we-need-intercept.html

        addDocumentToVector(document, outputVector, classifier, allDocuments);
        return outputVector;
    }

    void addDocumentToVector(Learner.Document document, Vector outputVector,
                             LogisticRegressionClassifier classifier, List<Learner.Document> allDocuments);
}
