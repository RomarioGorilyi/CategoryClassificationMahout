package com.genesys.knowledge.classification.learner;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.genesys.knowledge.classification.classifier.LogisticRegressionClassifier;
import com.genesys.knowledge.classification.exception.ClassifierNotTrainedException;
import lombok.Getter;
import lombok.Setter;
import org.apache.mahout.math.Vector;

import java.io.File;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by rhorilyi on 24.07.2017.
 */
public class Learner {

    @Getter
    private LogisticRegressionClassifier classifier;
    @Getter @Setter
    private List<Document> dataset;

    public Learner(ArrayList<Document> documents) {
        classifier = new LogisticRegressionClassifier(documents);
        dataset = documents;
    }

    public void trainClassifier(List<Document> trainingDocuments) {
        for (int i = 0; i < 30; i++) {
            Collections.shuffle(trainingDocuments, new SecureRandom());
            for (Document trainingDoc : trainingDocuments) {
                for (String categoryId : trainingDoc.getCategories()) {
                    classifier.train(trainingDoc, categoryId, trainingDocuments);
                }
            }
        }
    }

    public Vector classifyDocument(Document document)
            throws ClassifierNotTrainedException {
        dataset.add(document);
        return classifier.classifyDocument(document, dataset);
    }

    public List<Vector.Element> classifyDocument(Document document, int numberOfTopResults)
            throws ClassifierNotTrainedException {
        dataset.add(document);
        return classifier.classifyDocument(document, numberOfTopResults, dataset);
    }

    public String classifyDocumentWithMostConfidentCategory(Document document)
            throws ClassifierNotTrainedException {
        dataset.add(document);
        return classifier.classifyDocumentWithMostConfidentCategory(document, dataset);
    }

    public static class Document {

        @Getter @Setter
        private String id;
        @Getter @Setter
        private Field title;
        @Getter @Setter
        private Field body;
        @Getter @Setter
        private List<String> categories;

        public static class Field {
            @Getter @Setter
            private String original;
            @Getter @Setter
            private List<String> tokens;
            @Getter @Setter
            @JsonProperty("pos_tokens")
            private List<PosToken> posTokens;
        }

        private static class PosToken {
            @Getter @Setter
            private String pos;
            @Getter @Setter
            private String token;
        }
    }

    public static List<Document> convertJsonToDocuments(String fileName) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(new File(fileName), new TypeReference<List<Document>>(){});
    }
}
