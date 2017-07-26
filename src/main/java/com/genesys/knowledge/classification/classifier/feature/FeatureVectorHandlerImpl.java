package com.genesys.knowledge.classification.classifier.feature;

import com.genesys.knowledge.classification.classifier.LogisticRegressionClassifier;
import com.genesys.knowledge.classification.learner.Learner;
import com.genesys.knowledge.classification.util.TfIdf;
import org.apache.mahout.math.Vector;

import java.util.List;
import java.util.Map;

import static com.genesys.knowledge.classification.util.TfIdf.idf;
import static com.genesys.knowledge.classification.util.TfIdf.tf;
import static com.genesys.knowledge.classification.util.TfIdf.tfIdf;

/**
 * Created by rhorilyi on 24.07.2017.
 */
public class FeatureVectorHandlerImpl implements FeatureVectorHandler {

    @Override
    public void addDocumentToVector(Learner.Document document, Vector outputVector,
                                    LogisticRegressionClassifier classifier, List<Learner.Document> allDocuments) {
        List<String> titleTokens = document.getTitle().getTokens();
        for (String titleToken : titleTokens) {
            classifier.getFeatureEncoder().addToVector(titleToken, calcTokenWeight(titleToken, titleTokens, allDocuments), outputVector);
        }
        List<String> bodyTokens = document.getBody().getTokens();
        for (String bodyToken : bodyTokens) {
            classifier.getFeatureEncoder().addToVector(bodyToken, calcTokenWeight(bodyToken, bodyTokens, allDocuments), outputVector);
        }
    }

    private double calcTokenWeight(String token, List<String> documentTokens, List<Learner.Document> allDocuments) {
//        Map<String, Double> tf = tf(documentTokens, TfIdf.TfType.BOOLEAN);
//        Map<String, Double> idf = idf(allDocumentTokens);
//        return tfIdf(tf, idf, TfIdf.Normalization.COSINE).get(token);
        return 2;
    }
}
