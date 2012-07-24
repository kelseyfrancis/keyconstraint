package keyconstraint.identifykey.audio.cuesheet;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Nullable;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import keyconstraint.identifykey.audio.ArrayAudio;
import keyconstraint.identifykey.audio.Audio;

import static java.lang.Integer.parseInt;

public class CueSheet {

    private static final int framesPerSecond = 75;

    final File wavFile;
    private final List<Track> tracks;

    public CueSheet(File file) throws IOException {
        File wavFile = null;
        List<Track> tracks = Lists.newArrayList();

        Scanner scanner = new Scanner(file);
        String token;
        Track track = null;
        Track lastTrack = null;
        while (scanner.hasNext()) {
            token = scanner.next();
            if (token.equals("FILE")) {
                if (wavFile != null) {
                    throw new IOException("Only single audio file cue sheets are supported");
                }
                String filename = nextQuoted(scanner);
                token = scanner.next();
                if (!token.equals("WAVE")) {
                    throw new IOException("Only WAVE file cue sheets are supported; " + token + " not supported");
                }
                wavFile = new File(file.getParent(), filename);
            } else if (token.equals("TRACK")) {
                if (track != null) {
                    lastTrack = track;
                }
                track = new Track(this);
                tracks.add(track);
                track.startOffsetInSeconds = lastTrack == null ? Double.valueOf(0) : lastTrack.endOffsetInSeconds;
            } else if (token.equals("INDEX")) {
                token = scanner.next();
                if (token.equals("01")) {
                    if (track == null) {
                        throw new IOException("Invalid cue sheet; INDEX precedes TRACK");
                    }
                    track.startOffsetInSeconds = offsetInSeconds(scanner.next());
                    if (lastTrack != null) {
                        lastTrack.endOffsetInSeconds = track.startOffsetInSeconds;
                    }
                }
            } else if (token.equals("TITLE")) {
                if (track != null) {
                    track.title = nextQuoted(scanner);
                }
            }
        }

        this.wavFile = wavFile;
        this.tracks = ImmutableList.copyOf(tracks);
    }

    private static String nextQuoted(Scanner scanner) {
        String token = scanner.next();
        if (!token.startsWith("\"")) {
            return token;
        }

        StringBuilder quoted = new StringBuilder();
        quoted.append(token);
        while (!token.endsWith("\"")) {
            token = scanner.next();
            quoted.append(" ");
            quoted.append(token);
        }
        return quoted.substring(1, quoted.length() - 1);
    }

    private static String removeQuotes(String s) {
        return s.replaceFirst("\"([^\"]*)\"", "$1");
    }

    private static final Pattern mmssffPattern = Pattern.compile("([0-9]{2}):([0-9]{2}):([0-9]{2})");

    private static double offsetInSeconds(String mmssff) {
        Matcher m = mmssffPattern.matcher(mmssff);
        m.matches();
        return (((double) parseInt(m.group(3))) / framesPerSecond) +
                parseInt(m.group(2)) +
                (parseInt(m.group(1)) * 60);

    }

    private static Audio slice(Audio in, double startOffsetInSeconds, @Nullable Double endOffsetInSeconds) {
        int startOffsetInSamples = ((int) Math.round(startOffsetInSeconds * in.getSampleRateInHz())) * in.getChannels();
        int endOffsetInSamples = endOffsetInSeconds == null ?
                in.getSamples().length :
                (((int) Math.round(endOffsetInSeconds * in.getSampleRateInHz())) * in.getChannels()) + 1;

        double[] samples = new double[endOffsetInSamples - startOffsetInSamples];
        System.arraycopy(in.getSamples(), startOffsetInSamples, samples, 0, samples.length);
        return new ArrayAudio(samples, in.getSampleRateInHz(), in.getChannels(), in.getBitsPerSample(), in.getTitle());
    }

    public List<Track> getTracks() {
        return tracks;
    }
}
