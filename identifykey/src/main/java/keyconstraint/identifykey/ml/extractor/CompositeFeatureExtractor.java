package keyconstraint.identifykey.ml.extractor;

import java.util.Arrays;
import java.util.List;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import keyconstraint.identifykey.audio.Audio;
import keyconstraint.identifykey.ml.feature.Feature;

public class CompositeFeatureExtractor implements FeatureExtractor {

    private final Iterable<FeatureExtractor> extractors;

    public CompositeFeatureExtractor(Iterable<FeatureExtractor> extractors) {
        this.extractors = ImmutableList.copyOf(extractors);
    }

    public CompositeFeatureExtractor(FeatureExtractor... extractors) {
        this(Arrays.asList(extractors));
    }

    @Override
    public List<Feature> extractFeatures(Audio audio) {
        List<Feature> features = Lists.newArrayList();
        for (FeatureExtractor extractor : extractors) {
            features.addAll(extractor.extractFeatures(audio));
        }
        return ImmutableList.copyOf(features);
    }

    @Override
    public List<Feature> getAttributes() {
        List<Feature> attributes = Lists.newArrayList();
        for (FeatureExtractor extractor : extractors) {
            attributes.addAll(extractor.getAttributes());
        }
        return ImmutableList.copyOf(attributes);
    }
}
