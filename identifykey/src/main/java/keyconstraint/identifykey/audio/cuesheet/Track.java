package keyconstraint.identifykey.audio.cuesheet;

import java.io.IOException;

import keyconstraint.identifykey.audio.Audio;
import keyconstraint.identifykey.audio.WaveFileAudio;

public class Track {

    CueSheet cueSheet;
    String title;
    Double startOffsetInSeconds;
    Double endOffsetInSeconds;

    Track(CueSheet cueSheet) {
        this.cueSheet = cueSheet;
    }

    public String getTitle() {
        return title;
    }

    public Audio acquireAudio() throws IOException {
        WaveFileAudio audio = new WaveFileAudio(cueSheet.wavFile, startOffsetInSeconds, endOffsetInSeconds);
        audio.setTitle(title);
        return audio;
    }
}
