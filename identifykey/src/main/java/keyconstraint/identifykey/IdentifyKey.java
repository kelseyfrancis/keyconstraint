package keyconstraint.identifykey;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.List;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import keyconstraint.identifykey.audio.Audio;
import keyconstraint.identifykey.audio.WaveFileAudio;
import keyconstraint.identifykey.audio.cuesheet.CueSheet;
import keyconstraint.identifykey.audio.cuesheet.Track;
import keyconstraint.identifykey.ml.classifier.weka.WekaClassifier;
import keyconstraint.identifykey.ml.classifier.weka.WekaClassifierType;
import keyconstraint.identifykey.ml.extractor.CompositeFeatureExtractor;
import keyconstraint.identifykey.ml.extractor.FeatureExtractor;
import keyconstraint.identifykey.ml.extractor.PcpFeatureExtractor;
import keyconstraint.identifykey.ml.extractor.TitleFeatureExtractor;
import keyconstraint.identifykey.ml.feature.Feature;
import keyconstraint.identifykey.ml.feature.NominalFeature;
import keyconstraint.identifykey.ml.label.NominalLabel;
import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import net.sourceforge.argparse4j.inf.Namespace;

import static net.sourceforge.argparse4j.impl.Arguments.storeTrue;

public class IdentifyKey {

    private static final List<String> keys = Arrays.asList(
            "C", "c", "C#", "c#", "D", "d", "D#", "d#", "E", "e", "F", "f",
            "F#", "f#", "G", "g", "G#", "g#", "A", "a", "A#", "a#", "B", "b");

    private static final String keyAttribute = "key";

    private static final Object askForLabelFlag = new Object();
    private static final String skipLabel = "?";

    private IdentifyKey() {}

    public static void main(String[] args) throws IOException {
        ArgumentParser parser = ArgumentParsers.newArgumentParser("IdentifyKey")
                .defaultHelp(true)
                .description("Identify the key of the given songs.");
        parser.addArgument("-v", "--verbose").action(storeTrue());
        parser.addArgument("--labels")
                .required(true)
                .help("Labeled data set (ARFF)");
        parser.addArgument("--label")
                .nargs("?")
                .setConst(askForLabelFlag)
                .help("Label to affix to the given song. If no label specified, ask for label.");
        parser.addArgument("file")
                .nargs("*")
                .help("Song to classify or label (WAV)");

        // TODO add param for classifier algorithm

        Namespace ns;
        try {
            ns = parser.parseArgs(args);
        } catch (ArgumentParserException e) {
            parser.handleError(e);
            System.exit(1);
            return;
        }

        boolean verbose = ns.getBoolean("verbose");

        TitleFeatureExtractor titleExtractor = new TitleFeatureExtractor();
        FeatureExtractor extractor = new CompositeFeatureExtractor(
                titleExtractor,
                new PcpFeatureExtractor()
        );

        String labelsFilename = ns.getString("labels");
        File labels = new File(labelsFilename);

        List<Feature> attributes = Lists.newArrayList(extractor.getAttributes());
        List<Feature> metaDataAttributes = titleExtractor.getAttributes();
        WekaClassifier classifier = new WekaClassifier(
                WekaClassifierType.NNGE, labels, "IdentifyKey", attributes, keyAttribute(), metaDataAttributes);

        boolean askForLabel = ns.get("label") == askForLabelFlag;
        String label = askForLabel ? null : ns.getString("label");

        for (String filename : ns.<String>getList("file")) {
            if (verbose) System.out.printf("Reading %s...\n", filename);
            File file = new File(filename);

            Iterable<AudioTrack> tracks = tracks(file);
            for (AudioTrack track : tracks) {
                if (verbose) System.out.printf("Loading audio for `%s'...\n", track.title);
                Audio in = track.acquireAudio();

                if (verbose) System.out.println("Extracting features...");
                List<Feature> features = extractor.extractFeatures(in);

                if (label != null || askForLabel) {
                    if (askForLabel) {
                        BufferedReader cons = new BufferedReader(new InputStreamReader(System.in));
                        for (;;) {
                            System.out.printf("Label for `%s': ", in.getTitle());
                            label = cons.readLine();
                            if (isValidLabel(label)) break;
                            System.out.printf("Invalid label: `%s'\n", label);
                        }
                    }
                    if (!isValidLabel(label)) {
                        System.err.printf("Invalid label: `%s'\n", label);
                        System.exit(1);
                        return;
                    }

                    if (label.equals(skipLabel)) {
                        if (verbose) System.out.println("Skipping...");
                        continue;
                    }

                    if (verbose) System.out.printf("Labeling as `%s'...\n", label);
                    classifier.label(features, new NominalLabel(keyAttribute, label));

                    if (verbose) System.out.println("Saving labels...");
                    classifier.writeArffFile();

                    if (verbose) System.out.println("Done.");
                } else {
                    if (verbose) System.out.println("Training classifier...");
                    classifier.train();
                    if (verbose) System.out.println("Classifying...");

                    @SuppressWarnings("unchecked")
                    Iterable<NominalLabel> detectedLabels = (Iterable<NominalLabel>) (List<?>) classifier.classify(features);

                    List<String> output = Lists.newArrayList();
                    for (NominalLabel detectedLabel : detectedLabels) {
                        String detectedKey = detectedLabel.getValue();
                        double prob = detectedLabel.getProbability();
                        if (verbose) {
                            System.out.printf("Detected key of `%s' in `%s' with probability %.4f.\n", detectedKey, in.getTitle(), prob);
                        }
                        output.add(String.format("%s (p=%.4f)", detectedKey, prob));
                        System.out.println(Joiner.on(", ").join(output));
                    }
                }
            }
        }
    }

    private static NominalFeature keyAttribute() {
        return new NominalFeature(keyAttribute, null, keys);
    }

    private static boolean isValidLabel(String label) {
        return label != null && (label.equals(skipLabel) || keys.contains(label));
    }

    private static Iterable<AudioTrack> tracks(final File file) throws IOException {
        List<AudioTrack> audio = Lists.newArrayList();
        if (file.getName().toLowerCase().endsWith(".cue")) {
            CueSheet cueSheet = new CueSheet(file);
            for (final Track track : cueSheet.getTracks()) {
                audio.add(new AudioTrack(track.getTitle()) {
                    @Override
                    protected Audio acquireAudio() throws IOException {
                        return track.acquireAudio();
                    }
                });
            }
        } else {
            audio.add(new AudioTrack(file.getName().replaceFirst("(?i).wav$", "")) {
                @Override
                protected Audio acquireAudio() throws IOException {
                    return new WaveFileAudio(file);
                }
            });
        }
        return audio;
    }

    private abstract static class AudioTrack {
        private final String title;

        protected AudioTrack(String title) {
            this.title = title;
        }

        protected abstract Audio acquireAudio() throws IOException;
    }
}
