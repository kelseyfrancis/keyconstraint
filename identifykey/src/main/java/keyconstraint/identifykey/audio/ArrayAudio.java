package keyconstraint.identifykey.audio;

public class ArrayAudio implements Audio {

    private final double[] samples;
    private final int sampleRateInHz;
    private final int channels;
    private final int bitsPerSample;
    private final String title;

    public ArrayAudio(double[] samples, int sampleRateInHz, int channels, int bitsPerSample, String title) {
        this.samples = samples;
        this.sampleRateInHz = sampleRateInHz;
        this.channels = channels;
        this.bitsPerSample = bitsPerSample;
        this.title = title;
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
        return bitsPerSample;
    }

    @Override
    public String getTitle() {
        return title;
    }
}
