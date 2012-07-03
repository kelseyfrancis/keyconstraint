package keyconstraint.identifykey.ml.feature;

public interface FeatureVisitor<R> {

    R visit(NumericFeature feature);

    R visit(StringFeature feature);

    R visit(NominalFeature feature);
}
