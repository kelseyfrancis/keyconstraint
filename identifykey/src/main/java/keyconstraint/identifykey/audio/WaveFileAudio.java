package keyconstraint.identifykey.audio;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;

/**
 * Format reference: https://ccrma.stanford.edu/courses/422/projects/WaveFormat/
 */
public class WaveFileAudio implements Audio {

    private final double[] samples;
    private final int sampleRateInHz;
    private final int channels;

    public WaveFileAudio(File file) throws IOException {
        int headerLengthInBytes = 44;

        samples = new double[(int) ((file.length() - headerLengthInBytes) / 2)];
        FileInputStream inputStream = new FileInputStream(file);
        FileChannel channel = inputStream.getChannel();

        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(8192);
        byteBuffer.order(ByteOrder.LITTLE_ENDIAN);

        // read header
        byteBuffer.limit(headerLengthInBytes);
        if (channel.read(byteBuffer) != headerLengthInBytes) {
            throw new IOException("Could not read header");
        }

        byteBuffer.position(22);
        channels = (int) byteBuffer.getShort();
        sampleRateInHz = byteBuffer.getInt();
        byteBuffer.position(34);
        short bitsPerSample = byteBuffer.getShort();
        if (bitsPerSample != Short.SIZE && bitsPerSample != Byte.SIZE) {
            throw new IOException("Cannot read " + bitsPerSample + "-bit samples");
        }

        // read samples
        byteBuffer.clear();

        int bytesRead;
        int samplesRead = 0;
        while ((bytesRead = channel.read(byteBuffer)) != -1) {
            byteBuffer.position(0);
            byteBuffer.limit(bytesRead);
            while (byteBuffer.hasRemaining()) {
                double sample;
                if (bitsPerSample == Short.SIZE) {
                    sample = ((double) byteBuffer.getShort()) / Short.MAX_VALUE;
                } else {
                    sample = ((double) byteBuffer.get()) / Byte.MAX_VALUE;
                }
                samples[samplesRead++] = sample;
            }
            byteBuffer.clear();
        }

        channel.close();
    }

    @Override
    public double[] getSamples() {
        return samples;
    }

    @Override
    public int getSampleRateInHz() {
        return sampleRateInHz;
    }

    @Override
    public int getChannels() {
        return channels;
    }

    @Override
    public int getBitsPerSample() {
        return Short.SIZE;
    }
}
