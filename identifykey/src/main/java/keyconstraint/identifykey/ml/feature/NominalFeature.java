package keyconstraint.identifykey.ml.feature;

import java.util.List;

import javax.annotation.Nullable;

public class NominalFeature extends StringFeature {

    private final List<String> allowedValues;

    public NominalFeature(String name, @Nullable String value, List<String> allowedValues) {
        super(name, value);
        this.allowedValues = allowedValues;
    }

    @Override
    public <R> R acceptVisitor(FeatureVisitor<R> visitor) {
        return visitor.visit(this);
    }

    public List<String> getAllowedValues() {
        return allowedValues;
    }
}
