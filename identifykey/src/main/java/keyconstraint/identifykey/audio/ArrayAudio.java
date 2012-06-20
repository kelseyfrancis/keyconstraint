package keyconstraint.identifykey.audio;

public class ArrayAudio implements Audio {

    private final short[] samples;
    private final int sampleRateInHz;
    private final int channels;
    private final int bitsPerSample;

    public ArrayAudio(short[] samples, int sampleRateInHz, int channels, int bitsPerSample) {
        this.samples = samples;
        this.sampleRateInHz = sampleRateInHz;
        this.channels = channels;
        this.bitsPerSample = bitsPerSample;
    }

    @Override
    public short[] getSamples() {
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
        return bitsPerSample;
    }
}
