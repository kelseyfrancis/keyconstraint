package keyconstraint.identifykey.audio.analyzer.window;

import static java.lang.Math.PI;
import static java.lang.Math.cos;
import static java.lang.Math.sin;

public class WindowFunctions {

    /** http://en.wikipedia.org/wiki/Window_function#Rectangular_window **/
    public static final WindowFunction rectangular = new WindowFunction() {
        @Override
        public double w(int n, int N) {
            return 1;
        }
    };

    /** http://en.wikipedia.org/wiki/Window_function#Hamming_window **/
    public static final WindowFunction hamming = new WindowFunction() {
        @Override
        public double w(int n, int N) {
            return 0.54 - (0.46*cos((2*PI*n)/(N-1)));
        }
    };

    /** http://en.wikipedia.org/wiki/Modified_discrete_cosine_transform#Window_functions */
    public static final WindowFunction vorbis = new WindowFunction() {
        @Override
        public double w(int n, int N) {
            return sin((PI/2) * sq(sin((PI / (2 * N)) * (2 * n))));
        }
    };

    private WindowFunctions() {
    }

    private static double sq(double a) {
        return a * a;
    }
}
