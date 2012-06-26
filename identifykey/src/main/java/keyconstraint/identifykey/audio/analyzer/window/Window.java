package keyconstraint.identifykey.audio.analyzer.window;

public class Window {

    /** Window size */
    public final int N;
    private final double[] w;

    public Window(WindowFunction fn, int N) {
        this.N = N;
        w = new double[N];
        for (int n = 0; n < N; n++) {
            w[n] = fn.w(n, N);
        }
    }

    public double[] apply(double[] samples, int offset) {
        double[] window = new double[N];
        for (int n = 0; n < N; n++) {
            window[n] = samples[n + offset] * w[n];
        }
        return window;
    }
}
