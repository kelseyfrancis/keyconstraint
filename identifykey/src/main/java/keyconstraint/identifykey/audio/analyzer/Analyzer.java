package keyconstraint.identifykey.audio.analyzer;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import com.google.common.collect.Lists;
import edu.emory.mathcs.jtransforms.dct.DoubleDCT_1D;
import keyconstraint.identifykey.audio.Audio;
import keyconstraint.identifykey.audio.WaveFileAudio;
import keyconstraint.identifykey.audio.analyzer.window.Window;
import keyconstraint.identifykey.audio.analyzer.window.WindowFunction;
import keyconstraint.identifykey.audio.analyzer.window.WindowFunctions;
import keyconstraint.identifykey.audio.mixer.StereoToMonoMixer;

public class Analyzer {

    private final WindowFunction windowFn;

    public Analyzer(WindowFunction windowFn) {
        this.windowFn = windowFn;
    }

    public void analyze(Audio audio) {
        double[] samples = audio.getSamples();

        int frameSize = audio.getSampleRateInHz(); // yields resolution of 1 Hz
        Window window = new Window(windowFn, frameSize);
        int increment = frameSize / 2;

        int len = samples.length;

        DoubleDCT_1D dct = new DoubleDCT_1D(frameSize);
        int end = len - (len % frameSize);
        List<Frame> frames = Lists.newArrayListWithCapacity(len / increment);
        for (int offset = 0; offset < end; offset += increment) {
            double[] spectrum = window.apply(samples, offset);
            dct.forward(spectrum, false);
            frames.add(new Frame(spectrum));
        }
    }

    final class Frame {
        private final double[] spectrum;

        Frame(double[] spectrum) {
            this.spectrum = spectrum;
        }

        double[] getSpectrum() {
            return spectrum;
        }
    }

    public static void main(String[] args) throws IOException {
        File file = new File(args[0]);
        Audio in = new WaveFileAudio(file);
        if (in.getChannels() == 2) {
            in = new StereoToMonoMixer().mix(in);
        }
        new Analyzer(WindowFunctions.hamming).analyze(in);
    }
}
