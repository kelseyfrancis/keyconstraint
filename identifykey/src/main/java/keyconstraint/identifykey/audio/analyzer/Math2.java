package keyconstraint.identifykey.audio.analyzer;

import static com.google.common.base.Preconditions.checkArgument;
import static java.lang.Math.sqrt;

public class Math2 {

    private Math2() {}

    public static double sq(double a) {
        return a * a;
    }

    public static void normalize(double[] a) {
        double max = max(a);
        for (int i = 0; i < a.length; i++) {
            a[i] = a[i] / max;
        }
    }

    public static void vectorNormalize(double[] a) {
        double mag = vectorMag(a);
        for (int i = 0; i < a.length; i++) {
            a[i] = a[i] / mag;
        }
    }

    public static double vectorMag(double[] a) {
        return sqrt(dot(a, a));
    }

    public static double dot(double[] a, double[] b) {
        checkArgument(a.length == b.length);
        double dot = 0.0;
        for (int i = 0; i < a.length; i++) {
            dot += a[i] * b[i];
        }
        return dot;
    }

    public static double max(double... a) {
        checkArgument(a.length > 0);
        double max = a[0];
        for (int i = 1; i < a.length; i++) {
            max = Math.max(max, a[i]);
        }
        return max;
    }
}
