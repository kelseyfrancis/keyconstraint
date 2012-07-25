package keyconstraint.identifykey.ml.classifier;

import weka.classifiers.Classifier;
import weka.classifiers.MultipleClassifiersCombiner;
import weka.classifiers.bayes.NaiveBayes;
import weka.classifiers.functions.MultilayerPerceptron;
import weka.classifiers.rules.NNge;
import weka.classifiers.trees.RandomForest;
import weka.core.Instance;
import weka.core.Instances;

public class CombinedClassifier extends MultipleClassifiersCombiner {

    private final NNge nnge;
    private final RandomForest randomForest;
    private final MultilayerPerceptron perceptron;
    private final NaiveBayes bayes;

    public CombinedClassifier() {
        nnge = new NNge();
        nnge.setNumAttemptsOfGeneOption(10);
        nnge.setNumFoldersMIOption(10);

        randomForest = new RandomForest();
        randomForest.setNumTrees(20);

        perceptron = new MultilayerPerceptron();
        perceptron.setTrainingTime(1000);

        bayes = new NaiveBayes();
        m_Classifiers = new Classifier[]{ nnge, randomForest, perceptron, bayes };
    }

    @Override
    public void buildClassifier(Instances data) throws Exception {
        for (Classifier classifier : m_Classifiers) {
            classifier.buildClassifier(data);
        }
    }

    @Override
    public double[] distributionForInstance(Instance instance) throws Exception {
        double[] dist = scale(nnge.distributionForInstance(instance), 3.0);
        add(dist, scale(randomForest.distributionForInstance(instance), 7.0));
        add(dist, scale(perceptron.distributionForInstance(instance), 5.0));
        add(dist, scale(bayes.distributionForInstance(instance), 4.0));

        dist = scale(dist, 1.0/19.0);
        return dist;
    }

    private static void add(double[] dst, double[] src) {
        for (int i = 0; i < dst.length; i++) {
            dst[i] += src[i];
        }
    }

    private static double[] scale(double[] d, double scalar) {
        for (int i = 0; i < d.length; i++) {
            d[i] *= scalar;
        }
        return d;
    }

    void printArray(double[] d) {
        for (double v : d) {
            System.out.printf("%.4f ", v);
        }
        System.out.println();
    }
}
