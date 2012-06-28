package keyconstraint.identifykey.ml.classifier.weka;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.Collections;
import java.util.List;

import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.io.Closeables;
import keyconstraint.identifykey.ml.classifier.Classifier;
import keyconstraint.identifykey.ml.feature.Feature;
import keyconstraint.identifykey.ml.label.Label;
import keyconstraint.identifykey.ml.label.NominalLabel;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.converters.ArffSaver;
import weka.filters.Filter;
import weka.filters.supervised.attribute.Discretize;

public class WekaClassifier implements Classifier {

    private final transient File arffData;

    protected final transient Instances instances;

    private weka.classifiers.Classifier wekaClassifier;
    private WekaClassifierType wekaClassifierType;

    public WekaClassifier(File arffData, WekaClassifierType wekaClassifierType) {
        this.arffData = arffData;
        this.wekaClassifierType = wekaClassifierType;
        wekaClassifier = newClassifier();

        Instances instances = buildInstances(arffData);
        if (wekaClassifierType != null && wekaClassifierType.isRequiresDiscrete()) {
            instances = discretize(instances);
        }
        this.instances = instances;
    }

    protected weka.classifiers.Classifier newClassifier() {
        return wekaClassifierType == null ? null : wekaClassifierType.newInstance();
    }

    private static Instances buildInstances(File arffData) {
        try {
            return buildInstances(new FileInputStream(arffData));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static Instances buildInstances(InputStream arffData) {
        BufferedReader reader = new BufferedReader(new InputStreamReader(arffData));
        Instances instances;
        try {
            instances = new Instances(reader);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        Closeables.closeQuietly(reader);

        instances.setClassIndex(instances.numAttributes() - 1); // last attr is the class attr
        return instances;
    }

    private static Instances discretize(Instances instances) {
        Discretize filter = new Discretize();
        try {
            filter.setInputFormat(instances);
            instances = Filter.useFilter(instances, filter);
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
        return instances;
    }

    @Override
    public void train() {
        try {
            wekaClassifier.buildClassifier(instances);
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }

    @Override
    public List<Label> classify(List<Feature> features) {
        Instance instance = newInstance(features);

        if (wekaClassifierType.isRequiresDiscrete()) {
            instance = discretize(instance);
        }

        double[] distribution;
        try {
            distribution = wekaClassifier.distributionForInstance(instance);
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }

        List<ClassProbability> indexedDistribution = Lists.newArrayListWithCapacity(distribution.length);
        for (int i = 0; i < distribution.length; i++) {
            double probability = distribution[i];
            indexedDistribution.add(new ClassProbability(i, probability));
        }
        ClassProbability predictedClass = Collections.max(indexedDistribution);

        Attribute classAttribute = instances.classAttribute();
        Label label = new NominalLabel(
                classAttribute.name(),
                classAttribute.value(predictedClass.index),
                predictedClass.probability);
        return ImmutableList.of(label);
    }

    private Instance discretize(Instance instance) {
        Discretize discretize = new Discretize();
        try {
            discretize.setInputFormat(instances);
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
        discretize.input(instance);
        discretize.batchFinished();
        instance = discretize.output();
        return instance;
    }

    /** Assigns the given features the given label. */
    public void label(Iterable<Feature> features, NominalLabel label) {

        String labelValue = label.getValue();

        Attribute classAttribute = instances.classAttribute();
        int classIndex = classAttribute.index();
        int valueIndex = classAttribute.indexOfValue(labelValue);
        if (valueIndex == -1) {
            // need to add a new nominal value to the class attribute

            // TODO there's probably a better way to change the class attr; AddValues filter ?
            List<String> labels = Lists.newArrayListWithCapacity(classAttribute.numValues() + 1);
            for (int i = 0; i < classAttribute.numValues(); ++i) {
                labels.add(classAttribute.value(i));
            }
            labels.add(labelValue);
            classAttribute = new Attribute(classAttribute.name(), labels);
            instances.insertAttributeAt(classAttribute, classIndex + 1);
            instances.setClassIndex(classIndex + 1);

            for (Instance instance : instances) {
                instance.setClassValue(instance.value(classIndex));
            }
            instances.deleteAttributeAt(classIndex); // delete the old class attr
        }

        Instance instance = newInstance(features);
        instance.setClassValue(labelValue);
        instances.add(instance);

        try {
            writeArffData(new FileOutputStream(arffData));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private Instance newInstance(Iterable<Feature> features) {
        DenseInstance instance = new DenseInstance(instances.numAttributes());
        instance.setDataset(instances);

        for (Feature feature : features) {
            // TODO add by index instead?
            Attribute attribute = instances.attribute(feature.getName());
            if (attribute == null) {
                continue;
            }

            if (feature.isNumeric()) {
                instance.setValue(attribute, feature.getNumericValue());
            } else if (feature.isString()) {
                instance.setValue(attribute, feature.getStringValue());
            } else if (feature.isMissing()) {
                instance.setMissing(attribute);
            } else {
                throw new IllegalStateException("Invalid feature: " + feature);
            }

        }
        return instance;
    }

    public void writeArffData(OutputStream out) throws IOException {
        ArffSaver saver = new ArffSaver();
        saver.setInstances(instances);
        saver.setDestination(out);

        saver.writeBatch();
        out.flush();
        out.close();
    }

    private static final class ClassProbability implements Comparable<ClassProbability> {
        private final int index;
        private final double probability;

        private ClassProbability(int index, double probability) {
            this.index = index;
            this.probability = probability;
        }

        @Override
        public int compareTo(ClassProbability o) {
            return Double.compare(probability, o.probability);
        }
    }
}
