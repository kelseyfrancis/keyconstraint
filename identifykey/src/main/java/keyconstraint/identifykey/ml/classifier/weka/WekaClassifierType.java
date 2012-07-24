package keyconstraint.identifykey.ml.classifier.weka;

import weka.classifiers.Classifier;
import weka.classifiers.bayes.AODE;
import weka.classifiers.bayes.AODEsr;
import weka.classifiers.bayes.NaiveBayes;
import weka.classifiers.bayes.WAODE;
import weka.classifiers.functions.MultilayerPerceptron;
import weka.classifiers.rules.NNge;
import weka.classifiers.trees.J48;
import weka.classifiers.trees.RandomForest;

public enum WekaClassifierType {

    J48("C4.5", "C4.5 decision tree classifier") {
        @Override
        public Classifier newInstance() {
            return new J48();
        }
    },

    NNGE("NNge", "Nearest-neighbor-like algorithm using non-nested generalized exemplars") {
        @Override
        public Classifier newInstance() {
            return new NNge();
        }
    },

    RANDOM_FOREST("RandomForest", "Random Forests") {
        @Override
        public Classifier newInstance() {
            return new RandomForest();
        }
    },

    AODE("AODE", "Averaged One-Dependence Estimators", true) {
        @Override
        public Classifier newInstance() {
            return new AODE();
        }
    },

    AODE_SR("AODEsr", "Averaged One-Dependence Estimators with Subsumption Resolution", true) {
        @Override
        public Classifier newInstance() {
            return new AODEsr();
        }
    },

    WAODE("WAODE", "Weightily Averaged One-Dependence Estimators", true) {
        @Override
        public Classifier newInstance() {
            return new WAODE();
        }
    },

    NAIVE_BAYES("Naive Bayes", "Naive Bayes classifier using estimator classes") {
        @Override
        public Classifier newInstance() {
            return new NaiveBayes();
        }
    },

    MULTILAYER_PERCEPTRON("Multilayer perceptron", "Multilayer perceptron") {
        @Override
        public Classifier newInstance() {
            return new MultilayerPerceptron();
        }
    };

    private final CharSequence name;
    private final CharSequence description;
    private final boolean requiresDiscrete;

    WekaClassifierType(CharSequence name,
                       CharSequence description,
                       boolean requiresDiscrete) {
        this.name = name;
        this.description = description;
        this.requiresDiscrete = requiresDiscrete;
    }

    WekaClassifierType(CharSequence name, CharSequence description) {
        this(name, description, false);
    }

    public abstract Classifier newInstance();

    public CharSequence getName() {
        return name;
    }

    public CharSequence getDescription() {
        return description;
    }

    public boolean isRequiresDiscrete() {
        return requiresDiscrete;
    }
}
