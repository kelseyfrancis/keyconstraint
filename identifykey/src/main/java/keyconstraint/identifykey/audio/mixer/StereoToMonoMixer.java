package keyconstraint.identifykey.audio.mixer;

import keyconstraint.identifykey.audio.ArrayAudio;
import keyconstraint.identifykey.audio.Audio;
import keyconstraint.identifykey.audio.WaveFileAudio;
import keyconstraint.identifykey.audio.writer.WaveFileWriter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import static com.google.common.base.Preconditions.checkArgument;

/** Mixes stereo audio to mono. */
public class StereoToMonoMixer implements Mixer {

    @Override
    public Audio mix(Audio in) {
        checkArgument(in.getChannels() == 2, "Input audio must be stereo");

        double[] samples = in.getSamples();
        checkArgument(samples.length % 2 == 0, "Input audio does not have an even number of samples");

        int mixLen = samples.length / 2;
        double[] mix = new double[mixLen];

        for (int i = 0; i < mixLen; ++i) {
            double left = samples[2*i];
            double right = samples[(2*i)+1];
            mix[i] = (left + right) / 2.0;
        }

        return new ArrayAudio(mix, in.getSampleRateInHz(), 1, in.getBitsPerSample(), in.getTitle());
    }

    public static void main(String[] args) throws IOException {
        File file = new File(args[0]);
        Audio in = new WaveFileAudio(file);
        Audio out = new StereoToMonoMixer().mix(in);
        OutputStream outStream = new FileOutputStream(args[1]);
        WaveFileWriter writer = new WaveFileWriter(out, outStream);
        writer.write();
        outStream.close();
    }
}
