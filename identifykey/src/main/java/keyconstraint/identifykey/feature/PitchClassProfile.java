package keyconstraint.identifykey.feature;

import java.io.File;
import java.io.IOException;
import java.util.List;

import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import keyconstraint.identifykey.audio.Audio;
import keyconstraint.identifykey.audio.WaveFileAudio;
import keyconstraint.identifykey.audio.analyzer.Frame;
import keyconstraint.identifykey.audio.analyzer.Spectrum;
import keyconstraint.identifykey.audio.analyzer.SpectrumAnalyzer;
import keyconstraint.identifykey.audio.analyzer.window.WindowFunctions;
import keyconstraint.identifykey.audio.mixer.StereoToMonoMixer;

import static java.lang.Math.round;
import static keyconstraint.identifykey.audio.analyzer.Math2.log2;
import static keyconstraint.identifykey.audio.analyzer.Math2.normalize;

/**
 * http://en.wikipedia.org/wiki/Pitch_class
 * http://en.wikipedia.org/wiki/Harmonic_pitch_class_profiles
 */
public class PitchClassProfile {

    private static final String[] notes = { "C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B" };

    private static final int semitonesPerOctave = 12;

    private final double[] pitchDist;
    private final double[] allOctavesPitchDist;

    public PitchClassProfile(Spectrum spectrum) {
        int octaves = 9;
        pitchDist = new double[octaves * semitonesPerOctave]; // TODO allow more than one bin per semitone?
        allOctavesPitchDist = new double[semitonesPerOctave];
        int numBins = spectrum.getNumBins();
        for (int bin = 0; bin < numBins; ++bin) {
            double binFreq = spectrum.getBinFreq(bin);
            int pitchClass = pitchClass(binFreq);
            if (0 <= pitchClass && pitchClass < pitchDist.length) {
                double magnitude = spectrum.getMagnitude(bin);
                pitchDist[pitchClass] += magnitude;
                allOctavesPitchDist[pitchClass % semitonesPerOctave] += magnitude;
            }
        }

        normalize(pitchDist);
        normalize(allOctavesPitchDist);
    }

    private static int pitchClass(double freq) {
        return 69 + (int) round(12 * log2(freq / 440.0));
    }

    private static String noteForPitchClass(int pitchClass) {
        return notes[pitchClass % semitonesPerOctave];
    }

    @Override
    public String toString() {
        StringBuilder s = new StringBuilder();
        for (String note : notes) {
            s.append(String.format("%8s ", note));
        }
        s.append('\n');
        for (double mag : allOctavesPitchDist) {
            s.append(String.format("%1.6f ", mag));
        }
        return s.toString();
    }

    public static void main(String[] args) throws IOException {
        File file = new File(args[0]);
        Audio in = new WaveFileAudio(file);
        if (in.getChannels() == 2) {
            in = new StereoToMonoMixer().mix(in);
        }
        List<Frame> frames = new SpectrumAnalyzer(WindowFunctions.hamming).analyze(in);
        PitchClassProfile pcp = new PitchClassProfile(
                Spectrum.combine(Iterables.transform(frames, new Function<Frame, Spectrum>() {
                    @Override
                    public Spectrum apply(Frame frame) {
                        return frame.getSpectrum();
                    }
                })));
        System.out.println(pcp);
    }
}