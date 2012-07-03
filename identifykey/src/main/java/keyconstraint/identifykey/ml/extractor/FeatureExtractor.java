package keyconstraint.identifykey.ml.extractor;

import java.util.List;

import keyconstraint.identifykey.audio.Audio;
import keyconstraint.identifykey.ml.feature.Feature;

public interface FeatureExtractor {

    List<Feature> extractFeatures(Audio audio);

    List<Feature> getAttributes();
}
