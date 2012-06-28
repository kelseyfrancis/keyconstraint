package keyconstraint.identifykey.ml.feature;

import java.util.Iterator;
import java.util.Map;

import com.google.common.collect.ImmutableMap;

public class Features implements Iterable<Feature> {

    private final Map<String, Feature> byName;

    public Features(Iterable<Feature> features) {
        ImmutableMap.Builder<String, Feature> byName = ImmutableMap.builder();
        for (Feature feature : features) {
            byName.put(feature.getName(), feature);
        }
        this.byName = byName.build();
    }

    public Feature getFeature(String name) {
        return byName.get(name);
    }

    @Override
    public Iterator<Feature> iterator() {
        return byName.values().iterator();
    }
}
