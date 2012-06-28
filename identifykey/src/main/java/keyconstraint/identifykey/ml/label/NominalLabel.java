package keyconstraint.identifykey.ml.label;

public class NominalLabel extends Label {

    private final String value;
    private final double probability;

    public NominalLabel(String name,
                        String value,
                        double probability) {
        super(name);
        this.value = value;
        this.probability = probability;
    }

    public String getValue() {
        return value;
    }

    public double getProbability() {
        return probability;
    }

    @Override
    public String toString() {
        return getName() + "=" + value + " (p=" + probability + ")";
    }
}
