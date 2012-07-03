package keyconstraint.identifykey.ml.feature;

import javax.annotation.Nullable;

public class StringFeature extends Feature {

    private final @Nullable String value;

    public StringFeature(String name, @Nullable String value) {
        super(name);
        this.value = value;
    }

    public @Nullable String getValue() {
        return value;
    }

    @Override
    public <R> R acceptVisitor(FeatureVisitor<R> visitor) {
        return visitor.visit(this);
    }

    @Override
    protected String valueAsString() {
        return value == null ? "?" : ("'" + value + "'");
    }
}
