package keyconstraint.identifykey.feature;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import keyconstraint.identifykey.audio.Audio;
import keyconstraint.identifykey.audio.WaveFileAudio;
import keyconstraint.identifykey.audio.analyzer.Frame;
import keyconstraint.identifykey.audio.analyzer.Spectrum;
import keyconstraint.identifykey.audio.analyzer.SpectrumAnalyzer;
import keyconstraint.identifykey.audio.analyzer.window.WindowFunctions;
import keyconstraint.identifykey.audio.mixer.StereoToMonoMixer;

import static com.google.common.math.DoubleMath.log2;
import static java.lang.Math.abs;
import static java.lang.Math.round;
import static keyconstraint.identifykey.audio.analyzer.Math2.normalize;

/**
 * http://en.wikipedia.org/wiki/Pitch_class
 * http://en.wikipedia.org/wiki/Harmonic_pitch_class_profiles
 */
public class PitchClassProfile {

    private static final String[] notes = { "C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B" };

    private static final int semitonesPerOctave = 12;
    private static final int octaves = 11;

    private final double[] pitchDistByOctave;
    private final double[] pitchDist;

    public PitchClassProfile(Spectrum spectrum) {
        pitchDistByOctave = new double[octaves * semitonesPerOctave]; // TODO allow more than one bin per semitone?
        pitchDist = new double[semitonesPerOctave];
        int numBins = spectrum.getNumBins();
        for (int bin = 0; bin < numBins; ++bin) {
            double binFreq = spectrum.getBinFreq(bin);
            int pitchClass = pitchClass(binFreq);
            if (0 <= pitchClass && pitchClass < pitchDistByOctave.length) {
                double magnitude = spectrum.getMagnitude(bin);
                pitchDistByOctave[pitchClass] += magnitude;
                pitchDist[pitchClass % semitonesPerOctave] += magnitude;
            }
        }

        normalize(pitchDistByOctave);
        normalize(pitchDist);
    }

    private static int pitchClass(double freq) {
        double exact = 69 + (12 * log2(freq / 440.0));
        int rounded = (int) round(exact);
        return abs(exact - rounded) < 0.1 ? rounded : -1;
    }

    private static String noteForPitchClass(int pitchClass) {
        return notes[pitchClass % semitonesPerOctave];
    }

    public static List<String> allNotes() {
        List<String> notes = Lists.newArrayListWithCapacity(semitonesPerOctave);
        for (int i = 0; i < semitonesPerOctave; ++i) {
            notes.add(noteForPitchClass(i));
        }
        return notes;
    }

    private static String noteWithOctaveForPitchClass(int pitchClass) {
        return noteForPitchClass(pitchClass) + ((pitchClass / semitonesPerOctave) - 1);
    }

    public static List<String> allNotesWithOctave() {
        int numNotes = semitonesPerOctave * octaves;
        List<String> notes = Lists.newArrayListWithCapacity(numNotes);
        for (int i = 0; i < numNotes; ++i) {
            notes.add(noteWithOctaveForPitchClass(i));
        }
        return notes;
    }

    public double[] getPitchDistByOctave() {
        return pitchDistByOctave;
    }

    public Map<String, Double> getPitchDistByOctaveByNote() {
        ImmutableMap.Builder<String, Double> byNote = ImmutableMap.builder();
        for (int i = 0; i < pitchDistByOctave.length; i++) {
            byNote.put(noteWithOctaveForPitchClass(i), pitchDistByOctave[i]);
        }
        return byNote.build();
    }

    public double[] getPitchDist() {
        return pitchDist;
    }

    public Map<String, Double> getPitchDistByNote() {
        ImmutableMap.Builder<String, Double> byNote = ImmutableMap.builder();
        for (int i = 0; i < pitchDist.length; i++) {
            byNote.put(noteForPitchClass(i), pitchDist[i]);
        }
        return byNote.build();
    }

    @Override
    public String toString() {
        StringBuilder s = new StringBuilder();
        for (String note : notes) {
            s.append(String.format("%8s ", note));
        }
        s.append('\n');
        for (double mag : pitchDist) {
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
        System.out.println(pcp.getPitchDistByOctaveByNote());
    }
}
