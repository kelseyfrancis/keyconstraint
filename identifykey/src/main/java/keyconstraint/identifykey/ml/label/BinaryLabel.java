package keyconstraint.identifykey.ml.label;

public class BinaryLabel extends Label {

    private final Boolean present;
    private final Double confidence;
    private final Integer ranking;

    public BinaryLabel(String name,
                       Boolean present,
                       Double confidence,
                       Integer ranking) {
        super(name);
        this.present = present;
        this.confidence = confidence;
        this.ranking = ranking;
    }

    public Boolean getPresent() {
        return present;
    }

    public Double getConfidence() {
        return confidence;
    }

    public Integer getRanking() {
        return ranking;
    }
}
