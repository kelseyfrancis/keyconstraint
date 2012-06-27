package keyconstraint.identifykey.audio.analyzer;

public final class Frame {

    private final Spectrum spectrum;

    public Frame(Spectrum spectrum) {
        this.spectrum = spectrum;
    }

    public Spectrum getSpectrum() {
        return spectrum;
    }
}
