package keyconstraint.identifykey.ml.feature;

import javax.annotation.Nullable;

public class NumericFeature extends Feature {

    private @Nullable final Double value;

    public NumericFeature(String name, @Nullable Double value) {
        super(name);
        this.value = value;
    }

    public @Nullable Double getValue() {
        return value;
    }

    @Override
    public <R> R acceptVisitor(FeatureVisitor<R> visitor) {
        return visitor.visit(this);
    }

    @Override
    protected String valueAsString() {
        return value == null ? "?" : String.format("%.5f", value);
    }
}
