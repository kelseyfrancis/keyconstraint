package keyconstraint.identifykey.audio.analyzer;

import java.io.File;
import java.io.IOException;
import java.util.List;

import com.google.common.collect.Lists;
import edu.emory.mathcs.jtransforms.fft.DoubleFFT_1D;
import keyconstraint.identifykey.audio.Audio;
import keyconstraint.identifykey.audio.WaveFileAudio;
import keyconstraint.identifykey.audio.analyzer.window.Window;
import keyconstraint.identifykey.audio.analyzer.window.WindowFunction;
import keyconstraint.identifykey.audio.analyzer.window.WindowFunctions;
import keyconstraint.identifykey.audio.mixer.StereoToMonoMixer;

import static java.lang.Math.sqrt;
import static keyconstraint.identifykey.audio.analyzer.Math2.sq;

public class SpectrumAnalyzer {

    private final WindowFunction windowFn;

    public SpectrumAnalyzer(WindowFunction windowFn) {
        this.windowFn = windowFn;
    }

    public List<Frame> analyze(Audio audio) {
        double[] samples = audio.getSamples();

        int frameSize = audio.getSampleRateInHz(); // yields resolution of 1 Hz
        Window window = new Window(windowFn, frameSize);
        int increment = frameSize / 2;

        int spectrumBins = frameSize / 2;
        double maxFreq = audio.getSampleRateInHz() / 2.0;

        int len = samples.length;

        DoubleFFT_1D fft = new DoubleFFT_1D(frameSize);

        List<Frame> frames = Lists.newArrayListWithCapacity(len / increment);
        double[] fftResult = new double[frameSize*2];

        int end = len - (len % frameSize) - increment;
        for (int offset = 0; offset < end; offset += increment) {
            window.apply(samples, offset, fftResult);

            fft.realForwardFull(fftResult);

            double[] spectrum = new double[spectrumBins];
            for (int i = 0; i < spectrumBins; i++) {
                double re = fftResult[2*i];
                double im = fftResult[(2*i)+1];
                spectrum[i] = sqrt(sq(re) + sq(im));
            }

            frames.add(new Frame(new Spectrum(spectrum, maxFreq)));
        }

        return frames;
    }

    public static void main(String[] args) throws IOException {
        File file = new File(args[0]);
        Audio in = new WaveFileAudio(file);
        if (in.getChannels() == 2) {
            in = new StereoToMonoMixer().mix(in);
        }
        new SpectrumAnalyzer(WindowFunctions.rectangular).analyze(in);
    }
}
