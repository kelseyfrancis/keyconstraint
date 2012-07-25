package keyconstraint.identifykey.ml.classifier.weka;

import keyconstraint.identifykey.ml.classifier.CombinedClassifier;
import weka.classifiers.Classifier;
import weka.classifiers.bayes.NaiveBayes;
import weka.classifiers.functions.MultilayerPerceptron;
import weka.classifiers.meta.MultiScheme;
import weka.classifiers.meta.Vote;
import weka.classifiers.rules.NNge;
import weka.classifiers.trees.J48;
import weka.classifiers.trees.RandomForest;
import weka.core.SelectedTag;

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
            NNge c = new NNge();
//            c.setNumAttemptsOfGeneOption(50);
//            c.setNumFoldersMIOption(50);
            return c;
        }
    },

    RANDOM_FOREST("RandomForest", "Random Forests") {
        @Override
        public Classifier newInstance() {
            RandomForest c = new RandomForest();
            c.setNumTrees(100);
            return c;
        }
    },

    NAIVE_BAYES("NaiveBayes", "Naive Bayes classifier using estimator classes") {
        @Override
        public Classifier newInstance() {
            NaiveBayes c = new NaiveBayes();
//            c.setUseKernelEstimator(true);
            return c;
        }
    },

    MULTILAYER_PERCEPTRON("Perceptron", "Multilayer perceptron") {
        @Override
        public Classifier newInstance() {
            MultilayerPerceptron c = new MultilayerPerceptron();
            c.setTrainingTime(5000);
            return c;
        }
    },

    MULTI_SCHEME("MultiScheme", "Multi scheme") {
        @Override
        public Classifier newInstance() {
            MultiScheme c = new MultiScheme();
            c.setDebug(true);
            c.setClassifiers(new Classifier[]{NNGE.newInstance(), RANDOM_FOREST.newInstance(), MULTILAYER_PERCEPTRON.newInstance(), NAIVE_BAYES.newInstance()});
//            c.setNumFolds(10);
            return c;
        }
    },

    VOTE("Vote", "Vote") {
        @Override
        public Classifier newInstance() {
            Vote c = new Vote();
            c.setClassifiers(new Classifier[]{RANDOM_FOREST.newInstance(), MULTILAYER_PERCEPTRON.newInstance()});
            c.setCombinationRule(new SelectedTag(Vote.PRODUCT_RULE, Vote.TAGS_RULES));
            return c;
        }
    },

    Combined("Combined", "Combined") {
        @Override
        public Classifier newInstance() {
            return new CombinedClassifier();
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

    public static WekaClassifierType forName(String name) {
        for (WekaClassifierType type : WekaClassifierType.values()) {
            if (type.getName().toString().equalsIgnoreCase(name)) {
                return type;
            }
        }
        return null;
    }
}
