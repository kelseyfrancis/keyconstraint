package keyconstraint.identifykey.ml.feature;

public class Feature {

    private final String name;
    private final Double numericValue;
    private final String stringValue;

    private Feature(String name,
                    Double numericValue,
                    String stringValue) {
        this.name = name;
        this.numericValue = numericValue;
        this.stringValue = stringValue;
    }

    public Feature(String name, Number numericValue) {
        this(name, numericValue.doubleValue(), null);
    }

    public Feature(String name, String stringValue) {
        this(name, null, stringValue);
    }

    public Feature(String name) {
        this(name, null, null);
    }

    public String getName() {
        return name;
    }

    public double getNumericValue() {
        return numericValue;
    }

    public String getStringValue() {
        return stringValue;
    }

    public boolean isMissing() {
        return numericValue == null && stringValue == null;
    }

    public boolean isNumeric() {
        return numericValue != null;
    }

    public boolean isString() {
        return stringValue != null;
    }

    @Override
    public String toString() {
        return name + " = " +
                (isMissing() ? "?" :
                        (isNumeric() ? getNumericValue() : "'" + getStringValue() + "'"));
    }
}
