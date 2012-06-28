package keyconstraint.identifykey.ml.classifier;

import java.io.Serializable;
import java.util.List;

import keyconstraint.identifykey.ml.feature.Feature;
import keyconstraint.identifykey.ml.label.Label;

public interface Classifier extends Serializable {

    void train();

    List<Label> classify(List<Feature> features);
}
