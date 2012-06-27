package keyconstraint.identifykey.audio.analyzer;

import static java.lang.Math.min;
import static java.lang.Math.round;

public class Spectrum {

    private final double[] spectrum;
    private final double maxFreq;
    private final double binBandwidth;

    Spectrum(double[] spectrum, double maxFreq) {
        this.spectrum = spectrum;
        this.maxFreq = maxFreq;
        binBandwidth = maxFreq / spectrum.length;
    }

    public static Spectrum combine(Iterable<Spectrum> spectra) {
        Spectrum first = spectra.iterator().next();

        double[] combined = new double[first.getNumBins()];
        for (Spectrum spectrum : spectra) {
            double[] magnitudes = spectrum.spectrum;
            for (int i = 0; i < magnitudes.length; i++) {
                combined[i] += magnitudes[i];
            }
        }

        return new Spectrum(combined, first.maxFreq);
    }

    public double[] getSpectrum() {
        return spectrum;
    }

    public int getNumBins() {
        return spectrum.length;
    }

    public int getBinFor(double freq) {
        return (int) round(min(freq, maxFreq) / binBandwidth);
    }

    public double getBinFreq(int bin) {
        return bin * binBandwidth;
    }

    public double getMagnitude(int bin) {
        return spectrum[bin];
    }
}
