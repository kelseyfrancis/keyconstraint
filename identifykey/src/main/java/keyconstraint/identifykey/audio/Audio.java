package keyconstraint.identifykey.audio;

public interface Audio {

    double[] getSamples();
    int getSampleRateInHz();
    int getChannels();
    int getBitsPerSample();
}
