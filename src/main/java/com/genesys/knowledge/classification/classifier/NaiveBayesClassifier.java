package com.genesys.knowledge.classification.classifier;

import com.genesys.knowledge.classification.util.CategoryHandler;
import com.genesys.knowledge.classification.util.DocumentHandler;
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
import org.apache.mahout.math.DenseVector;
import org.apache.mahout.math.RandomAccessSparseVector;
import org.apache.mahout.math.Vector;
import org.apache.mahout.math.VectorWritable;
import org.apache.mahout.vectorizer.encoders.ConstantValueEncoder;
import org.apache.mahout.vectorizer.encoders.FeatureVectorEncoder;
import org.apache.mahout.vectorizer.encoders.StaticWordValueEncoder;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Created by rhorilyi on 15.05.2017.
 */
public class NaiveBayesClassifier extends AbstractClassifier {

    String sequenceFilePath = "input/tweets-seq";
    String tempDir = "input/tmp";
    String modelPath = "input/model";
    String inputDataPath = "input/tweets-vectors";

    @Getter
    private CategoryHandler categoryHandler;

    @Getter
    private NaiveBayesModel model;
    @Getter
    private AbstractNaiveBayesClassifier classifier;

    private Configuration configuration = new Configuration();

    private final ConstantValueEncoder interceptEncoder = new ConstantValueEncoder("intercept");
    private final FeatureVectorEncoder featureEncoder = new StaticWordValueEncoder("feature");

    private static final Pattern SLASH = Pattern.compile("/");

    public NaiveBayesClassifier(List<Document> documents) {
        categoryHandler = new CategoryHandler();
        categoryHandler.initHandler(documents);

        configuration.set("mapred.job.tracker", "ua-rhorilyi-lt:8020");
        System.setProperty("hadoop.home.dir", "C:\\hdp\\hadoop-2.4.0.2.1.7.0-2162");

        prepareModel(documents);
        model.validate();
        classifier = new StandardNaiveBayesClassifier(model);
    }

    private void prepareModel(List<Document> documents) {
        writeDataToFile(documents);
        trainModel();
        try {
            model = NaiveBayesModel.materialize(new Path(modelPath, "part-r-00000"), configuration);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void writeDataToFile(List<Document> documents) {
        try (SequenceFile.Writer writer = new SequenceFile.Writer(
                FileSystem.get(configuration), configuration, new Path(inputDataPath), Text.class, VectorWritable.class)) {
            for (Document document : documents) {
                for (Category category : document.getCategories()) {
                    writer.append(new Text(category.getId()), new Text(document.getText()));
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void trainModel() {
        TrainNaiveBayesJob trainNaiveBayes = new TrainNaiveBayesJob();
        trainNaiveBayes.setConf(configuration);
        try {
            trainNaiveBayes.run(new String[] {"-i", inputDataPath + "-o", modelPath, "--tempDir", tempDir});
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public Vector getFeatureVector(List<String> tokens, Collection<List<String>> documentTokens) {
        Vector outputVector = new RandomAccessSparseVector(10000);

        interceptEncoder.addToVector("1", outputVector); // output[0] is the intercept term
        // Look at the regression graph on the link below to see why we need the intercept.
        // http://statistiksoftware.blogspot.nl/2013/01/why-we-need-intercept.html

        for (String token : tokens) {
            featureEncoder.addToVector(token, 2, outputVector);
        }

        return outputVector;
    }

    private static VectorWritable trainingInstance(Vector.Element... elems) {
        DenseVector trainingInstance = new DenseVector(6);
        for (Vector.Element elem : elems) {
            trainingInstance.set(elem.index(), elem.get());
        }
        return new VectorWritable(trainingInstance);
    }

    @Override
    public byte[] serializeModel() {
        return new byte[0];
    }

    @Override
    public void deserializeModel(byte[] modelData) {

    }
}
