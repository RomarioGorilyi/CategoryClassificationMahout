package com.genesys.knowledge.classification.classifier;

import com.genesys.knowledge.classification.util.CategoryHandler;
import com.genesys.knowledge.domain.Category;
import com.genesys.knowledge.domain.Document;
import lombok.Getter;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.SequenceFile;
import org.apache.hadoop.io.Text;
import org.apache.mahout.classifier.naivebayes.AbstractNaiveBayesClassifier;
import org.apache.mahout.classifier.naivebayes.NaiveBayesModel;
import org.apache.mahout.classifier.naivebayes.StandardNaiveBayesClassifier;
import org.apache.mahout.classifier.naivebayes.training.TrainNaiveBayesJob;
import org.apache.mahout.math.RandomAccessSparseVector;
import org.apache.mahout.math.Vector;
import org.apache.mahout.vectorizer.SparseVectorsFromSequenceFiles;
import org.apache.mahout.vectorizer.encoders.ConstantValueEncoder;
import org.apache.mahout.vectorizer.encoders.FeatureVectorEncoder;
import org.apache.mahout.vectorizer.encoders.StaticWordValueEncoder;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;

/**
 * Created by rhorilyi on 15.05.2017.
 */
public class NaiveBayesClassifier extends AbstractClassifier {

    String sequenceFilePath = "input/tweets-seq";
    String labelIndexPath = "input/labelindex";
    String modelPath = "input/model";
    String vectorsPath = "input/tweets-vectors";

    @Getter
    private CategoryHandler categoryHandler;
    private List<Document> documents;
    @Getter
    private AbstractNaiveBayesClassifier classifier;

    private Configuration configuration = new Configuration();

    private final ConstantValueEncoder interceptEncoder = new ConstantValueEncoder("intercept");
    private final FeatureVectorEncoder featureEncoder = new StaticWordValueEncoder("feature");

    public NaiveBayesClassifier(List<Document> documents) {
        this.documents = documents;
        categoryHandler = new CategoryHandler();
        categoryHandler.initHandler(documents);

        configuration.set("mapred.job.tracker", "ua-rhorilyi-lt:8020");
        System.setProperty("hadoop.home.dir", "C:\\hdp\\hadoop-2.4.0.2.1.7.0-2162");

        try {
            NaiveBayesModel model = NaiveBayesModel.materialize(new Path(modelPath), configuration);
            classifier = new StandardNaiveBayesClassifier(model);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public AbstractClassifier train(Document queryTerms) {
        TrainNaiveBayesJob trainNaiveBayes = new TrainNaiveBayesJob();
        trainNaiveBayes.setConf(configuration);
        try {
            trainNaiveBayes.run(new String[] {"-i", vectorsPath + "/tfidf-vectors", "-o", modelPath, "-li",
                    labelIndexPath, "-el", "-c", "-ow"});
        } catch (Exception e) {
            e.printStackTrace();
        }
        return this;
    }

    /**
     * Classify the specified document calculating id of its most suitable {@link Category}.
     *
     * @param document document to classify
     * @return {@code String} category id
     */
    public String calculateMostSuitableCategory(Document document) {
        int index = classifier.classifyFull(getFeatureVector(document)).maxValueIndex();
        return categoryHandler.getCategoryId(index);
    }

    private void inputDataToSequenceFile() throws Exception {
        FileSystem fs = FileSystem.getLocal(configuration);
        Path seqFilePath = new Path(sequenceFilePath);
        fs.delete(seqFilePath, false);
        int count = 0;
        try (SequenceFile.Writer writer = SequenceFile.createWriter(
                fs, configuration, seqFilePath, Text.class, Text.class)) {
            String line;
            for (Document document : documents) {
                List<Category> categories = document.getCategories();
                for (Category category : categories) {
                    writer.append(new Text("/" + category.getId() + "/text" + count++),
                            new Text(document.getText()));
                }
            }
        }
    }

    private void sequenceFileToSparseVector() throws Exception {
        SparseVectorsFromSequenceFiles svfsf = new SparseVectorsFromSequenceFiles();
        svfsf.run(new String[] { "-i", sequenceFilePath, "-o", vectorsPath,
                "-ow" });
    }

    private Vector getFeatureVector(Document document) {
        Vector outputVector = new RandomAccessSparseVector(10000);

        interceptEncoder.addToVector("1", outputVector); // output[0] is the intercept term
        // Look at the regression graph on the link below to see why we need the intercept.
        // http://statistiksoftware.blogspot.nl/2013/01/why-we-need-intercept.html

        List<String> terms = document.getTerms();
        for (String term : terms) {
            featureEncoder.addToVector(term, 2, outputVector);
        }

        return outputVector;
    }

    @Override
    public byte[] serializeModel() {
        return new byte[0];
    }

    @Override
    public void deserializeModel(byte[] modelData) {

    }
}
