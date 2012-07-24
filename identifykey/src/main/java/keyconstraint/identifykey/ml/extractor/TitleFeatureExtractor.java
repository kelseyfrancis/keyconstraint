package keyconstraint.identifykey.ml.extractor;

import java.util.Arrays;
import java.util.List;

import keyconstraint.identifykey.audio.Audio;
import keyconstraint.identifykey.ml.feature.Feature;
import keyconstraint.identifykey.ml.feature.StringFeature;

public class TitleFeatureExtractor implements FeatureExtractor {

    public static final String TITLE = "title";

    @Override
    public List<Feature> extractFeatures(Audio audio) {
        return Arrays.<Feature>asList(new StringFeature(TITLE, audio.getTitle()));
    }

    @Override
    public List<Feature> getAttributes() {
        return Arrays.<Feature>asList(new StringFeature(TITLE, null));
    }
}
