package keyconstraint.identifykey.audio.analyzer;

import com.google.common.collect.Lists;
import edu.emory.mathcs.jtransforms.fft.DoubleFFT_1D;
import keyconstraint.identifykey.audio.Audio;
import keyconstraint.identifykey.audio.WaveFileAudio;
import keyconstraint.identifykey.audio.analyzer.window.Window;
import keyconstraint.identifykey.audio.analyzer.window.WindowFunction;
import keyconstraint.identifykey.audio.analyzer.window.WindowFunctions;
import keyconstraint.identifykey.audio.mixer.StereoToMonoMixer;

import java.io.File;
import java.io.IOException;
import java.util.List;

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

        DoubleFFT_1D fft = new DoubleFFT_1D(frameSize);
        int end = len - (len % frameSize) - increment;
        List<Frame> frames = Lists.newArrayListWithCapacity(len / increment);
        for (int offset = 0; offset < end; offset += increment) {
            double[] fftResult = new double[frameSize*2];
            window.apply(samples, offset, fftResult);
            fft.realForwardFull(fftResult);
            double[] spectrum = new double[frameSize];
            for (int i = 0; i < frameSize; i++) {
                double re = fftResult[2 * i];
                double im = fftResult[(2 * i) + 1];
                spectrum[i] = Math.sqrt((re * re) + (im * im));
            }
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
        new Analyzer(WindowFunctions.rectangular).analyze(in);
    }
}
