package keyconstraint.identifykey.ml.feature;

public abstract class Feature {

    private final String name;

    Feature(String name) {
        this.name = name;
    }

    public abstract <R> R acceptVisitor(FeatureVisitor<R> visitor);

    public String getName() {
        return name;
    }

    @Override
    public final String toString() {
        return name + " = " + valueAsString();
    }

    protected abstract String valueAsString();
}
