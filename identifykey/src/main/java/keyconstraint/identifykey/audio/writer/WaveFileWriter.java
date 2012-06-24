package keyconstraint.identifykey.audio.writer;

import keyconstraint.identifykey.audio.Audio;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Format reference: https://ccrma.stanford.edu/courses/422/projects/WaveFormat/
 */
public class WaveFileWriter {

    private final Audio audio;
    private final AudioOutputStream out;

    public WaveFileWriter(Audio audio, OutputStream out) {
        this.audio = audio;
        this.out = new AudioOutputStream(out);
    }

    public void write() throws IOException {
        double[] samples = audio.getSamples();
        int bitsPerSample = audio.getBitsPerSample();
        int bytesPerSample = bitsPerSample / Byte.SIZE;
        int lengthInBytes = samples.length * bytesPerSample;
        int sampleRateInHz = audio.getSampleRateInHz();
        int numChannels = audio.getChannels();
        int blockAlign = numChannels * bytesPerSample;
        int byteRate = sampleRateInHz * blockAlign;

        // header
        out.writeBigEndian(0x52494646);             // "RIFF"
        out.writeLittleEndian(36 + lengthInBytes);  // chunk size
        out.writeBigEndian(0x57415645);             // "WAVE"

        // subchunk 1
        out.writeBigEndian(0x666d7420);                 // "fmt "
        out.writeLittleEndian(16);                      // subchunk 1 size
        out.writeLittleEndian((short) 1);               // audio format = PCM
        out.writeLittleEndian((short) numChannels);     // num channels
        out.writeLittleEndian(sampleRateInHz);          // sample rate
        out.writeLittleEndian(byteRate);                // byte rate
        out.writeLittleEndian((short) blockAlign);      // block align
        out.writeLittleEndian((short) bitsPerSample);   // bits per sample

        // subchunk 2
        out.writeBigEndian(0x64617461);         // "data"
        out.writeLittleEndian(lengthInBytes);   // subchunk 2 size

        // data
        for (double sample : samples) {
            out.writeLittleEndian((short) Math.round(sample * Short.MAX_VALUE));
        }
        out.flush();
    }
}
