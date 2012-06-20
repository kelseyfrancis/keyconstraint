package keyconstraint.identifykey.audio.writer;

import java.io.IOException;
import java.io.OutputStream;

import keyconstraint.identifykey.audio.Audio;

public class AuFileWriter {

    private final Audio audio;
    private final AudioOutputStream out;

    public AuFileWriter(Audio audio, OutputStream out) {
        this.audio = audio;
        this.out = new AudioOutputStream(out);
    }

    public void write() throws IOException {
        short[] samples = audio.getSamples();
        int bitsPerSample = audio.getBitsPerSample();
        int bytesPerSample = bitsPerSample / Byte.SIZE;
        int lengthInBytes = samples.length * bytesPerSample;

        // header
        out.writeBigEndian(0x2e736e64);                 // magic number (".snd")
        out.writeBigEndian(24);                         // offset to data in bytes
        out.writeBigEndian(lengthInBytes);              // size in bytes
        out.writeBigEndian(3);                          // 16-bit linear PCM
        out.writeBigEndian(audio.getSampleRateInHz());  // sample rate
        out.writeBigEndian(audio.getChannels());        // num channels

        // data
        for (short sample : samples) {
            out.writeBigEndian(sample);
        }
    }
}
