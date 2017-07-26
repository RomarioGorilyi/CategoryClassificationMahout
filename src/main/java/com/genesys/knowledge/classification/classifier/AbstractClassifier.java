package com.genesys.knowledge.classification.classifier;

import com.genesys.knowledge.classification.exception.CategoryNotFoundException;
import com.genesys.knowledge.classification.exception.ClassifierNotTrainedException;
import com.genesys.knowledge.classification.learner.Learner;
import com.genesys.knowledge.classification.util.CategoryHandler;
import com.genesys.knowledge.domain.Category;
import com.genesys.knowledge.domain.Document;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.mahout.classifier.AbstractVectorClassifier;
import org.apache.mahout.math.Vector;
import org.apache.mahout.vectorizer.encoders.ConstantValueEncoder;
import org.apache.mahout.vectorizer.encoders.FeatureVectorEncoder;
import org.apache.mahout.vectorizer.encoders.StaticWordValueEncoder;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

/**
 * Created by rhorilyi on 25.04.2017.
 */
@Slf4j
public abstract class AbstractClassifier {

    private final ConstantValueEncoder interceptEncoder = new ConstantValueEncoder("intercept");
    private final FeatureVectorEncoder featureEncoder = new StaticWordValueEncoder("feature");

    @Getter @Setter
    private CategoryHandler categoryHandler;
    @Getter @Setter
    private AbstractVectorClassifier classifier;
    @Getter @Setter
    boolean trained = false;

    public AbstractClassifier() {
        categoryHandler = new CategoryHandler();
    }

    public AbstractClassifier(AbstractVectorClassifier classifier) {
        this();
        this.classifier = classifier;
    }

    public AbstractClassifier(List<Document> documents) {
        this();
        categoryHandler.initHandler(documents);
    }

    public AbstractClassifier(ArrayList<Learner.Document> documents) {
        this();
        categoryHandler.initHandler(documents);
    }

    /**
     * Classifies the specified document calculating {@link Vector}
     * which holds pairs <{@link Category}, confidence score>.
     *
     * @param tokens tokens of the document to classify
     * @param documentTokens sets of tokens which appear in all documents
     * @return {@link Vector} of pairs of categories and their confidence scores respectively
     */
    public Vector classifyDocument(List<String> tokens, Collection<List<String>> documentTokens)
            throws ClassifierNotTrainedException {
        if (trained) {
            return classifier.classifyFull(getFeatureVector(tokens, documentTokens));
        } else {
            throw new ClassifierNotTrainedException();
        }
    }

    public Vector classifyDocument(Learner.Document document, List<Learner.Document> allDocuments)
            throws ClassifierNotTrainedException {
        if (trained) {
            return classifier.classifyFull(getFeatureVector(document, allDocuments));
        } else {
            throw new ClassifierNotTrainedException();
        }
    }

    /**
     * Classifies the specified document calculating top N categories.
     *
     * @param tokens tokens of the document to classify
     * @param numberOfTopResults number of top categories with the highest confidence level
     * @param documentTokens sets of tokens which appear in all documents
     * @return {@link Vector} of pairs of categories and their confidence scores respectively
     */
    public List<Vector.Element> classifyDocument(List<String> tokens, int numberOfTopResults, Collection<List<String>> documentTokens)
            throws ClassifierNotTrainedException {
        if (trained) {
            List<Vector.Element> elements = new ArrayList<>();

            Vector vector = classifier.classifyFull(getFeatureVector(tokens, documentTokens));
            for (int i = 0; i < vector.size(); i++) {
                Vector.Element element = vector.getElement(i);
                elements.add(element);
            }
            elements.sort((o1, o2) -> {
                double confidence1 = o1.get();
                double confidence2 = o2.get();
                if (confidence1 == confidence2)
                    return 0;
                else if (confidence1 < confidence2)
                    return 1;
                else
                    return -1;
            });

            return elements.subList(0, numberOfTopResults);
        } else {
            throw new ClassifierNotTrainedException();
        }
    }

    public List<Vector.Element> classifyDocument(Learner.Document document, int numberOfTopResults, List<Learner.Document> allDocuments)
            throws ClassifierNotTrainedException {
        if (trained) {
            List<Vector.Element> elements = new ArrayList<>();

            Vector vector = classifier.classifyFull(getFeatureVector(document, allDocuments));
            for (int i = 0; i < vector.size(); i++) {
                Vector.Element element = vector.getElement(i);
                elements.add(element);
            }
            elements.sort((o1, o2) -> {
                double confidence1 = o1.get();
                double confidence2 = o2.get();
                if (confidence1 == confidence2)
                    return 0;
                else if (confidence1 < confidence2)
                    return 1;
                else
                    return -1;
            });

            return elements.subList(0, numberOfTopResults);
        } else {
            throw new ClassifierNotTrainedException();
        }
    }

    /**
     * Calculates probability that the specified {@link Document} has the specified {@link Category}.
     * If {@code this} LogisticRegressionClassifier's {@link #categoryHandler} doesn't contain the category,
     * return 0.
     *
     * @param tokens tokens of the document to classify
     * @param category category to check its presence probability in the specified document
     * @param documentTokens sets of tokens which appear in all documents
     * @return probability as {@code double} value
     */
    public double calcCategoryProbability(List<String> tokens, Category category,
                                          Collection<List<String>> documentTokens)
            throws ClassifierNotTrainedException {
        if (trained) {
            double probability;

            try {
                Vector vector = classifier.classifyFull(getFeatureVector(tokens, documentTokens));
                probability = vector.get(categoryHandler.getCategoryOrderNumber(category));
            } catch (CategoryNotFoundException e) {
                probability = 0;
            }

            return probability;
        } else {
            throw new ClassifierNotTrainedException();
        }
    }

    public double calcCategoryProbability(Learner.Document document, Category category,
                                          List<Learner.Document> allDocuments)
            throws ClassifierNotTrainedException {
        if (trained) {
            double probability;

            try {
                Vector vector = classifier.classifyFull(getFeatureVector(document, allDocuments));
                probability = vector.get(categoryHandler.getCategoryOrderNumber(category));
            } catch (CategoryNotFoundException e) {
                probability = 0;
            }

            return probability;
        } else {
            throw new ClassifierNotTrainedException();
        }
    }

    /**
     * Classifies the specified document calculating id of a {@link Category} with the best confidence score.
     *
     * @param tokens tokens of document to classify
     * @param documentTokens sets of tokens which appear in all documents
     * @return {@code String} category id
     */
    public String classifyDocumentWithMostConfidentCategory(List<String> tokens, Collection<List<String>> documentTokens)
            throws ClassifierNotTrainedException {
        if (trained) {
            int index = classifier.classifyFull(getFeatureVector(tokens, documentTokens)).maxValueIndex();
            return categoryHandler.getCategory(index).getId();
        } else {
            throw new ClassifierNotTrainedException();
        }
    }

    public String classifyDocumentWithMostConfidentCategory(Learner.Document document, List<Learner.Document> allDocuments)
            throws ClassifierNotTrainedException {
        if (trained) {
            int index = classifier.classifyFull(getFeatureVector(document, allDocuments)).maxValueIndex();
            return categoryHandler.getCategory(index).getId();
        } else {
            throw new ClassifierNotTrainedException();
        }
    }

    /**
     * Gets the vector of features that is used by classifier.
     *
     * @param tokens tokens of the document to classify
     * @param documentTokens sets of tokens which appear in all documents
     * @return feature vector
     */
    public abstract Vector getFeatureVector(List<String> tokens, Collection<List<String>> documentTokens);

    public abstract Vector getFeatureVector(Learner.Document document, List<Learner.Document> allDocuments);

    public abstract byte[] serializeModel();

    public abstract void deserializeModel(byte[] modelData);
}
