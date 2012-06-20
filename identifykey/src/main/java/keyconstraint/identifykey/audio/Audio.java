package keyconstraint.identifykey.audio;

/** Signed 16-bit Linear PCM audio samples. */
public interface Audio {

    short[] getSamples();
    int getSampleRateInHz();
    int getChannels();
    int getBitsPerSample();
}
