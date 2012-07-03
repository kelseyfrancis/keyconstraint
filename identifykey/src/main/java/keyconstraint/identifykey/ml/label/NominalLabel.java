package keyconstraint.identifykey.ml.label;

import java.util.List;

public class NominalLabel extends Label {

    private final String value;
    private final double probability;
    private final List<String> allowedValues;

    public NominalLabel(String name, String value) {
        this(name, value, null);
    }

    public NominalLabel(String name, String value, double probability) {
        this(name, value, probability, null);
    }

    public NominalLabel(String name, String value, List<String> allowedValues) {
        this(name, value, 1.0, allowedValues);
    }

    public NominalLabel(String name, String value, double probability, List<String> allowedValues) {
        super(name);
        this.value = value;
        this.probability = probability;
        this.allowedValues = allowedValues;
    }

    public String getValue() {
        return value;
    }

    public double getProbability() {
        return probability;
    }

    public List<String> getAllowedValues() {
        return allowedValues;
    }

    @Override
    public String toString() {
        return getName() + "=" + value + " (p=" + probability + ")";
    }
}
