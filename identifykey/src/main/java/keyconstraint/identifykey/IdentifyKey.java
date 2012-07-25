package keyconstraint.identifykey;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.Iterables;
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
import net.sourceforge.argparse4j.inf.Argument;
import net.sourceforge.argparse4j.inf.ArgumentChoice;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import net.sourceforge.argparse4j.inf.ArgumentType;
import net.sourceforge.argparse4j.inf.Namespace;

import static net.sourceforge.argparse4j.impl.Arguments.storeTrue;

public class IdentifyKey {

    private static final List<String> keys = Arrays.asList(
            "C", "c", "C#", "c#", "D", "d", "D#", "d#", "E", "e", "F", "f",
            "F#", "f#", "G", "g", "G#", "g#", "A", "a", "A#", "a#", "B", "b");
    private static final BiMap<String, String> keySynonyms = ImmutableBiMap.<String, String>builder()
            .put("Db", "C#")
            .put("db", "c#")
            .put("Eb", "D#")
            .put("eb", "d#")
            .put("Gb", "F#")
            .put("gb", "f#")
            .put("Ab", "G#")
            .put("ab", "g#")
            .put("Bb", "A#")
            .put("bb", "a#")
            .build();

    private static final String keyAttribute = "key";

    private static final Object askForLabelFlag = new Object();
    private static final String skipLabel = "?";

    private IdentifyKey() {}

    public static void main(String[] args) throws IOException {
        ArgumentParser parser = ArgumentParsers.newArgumentParser("IdentifyKey")
                .defaultHelp(true)
                .description("Identify the key of the given songs.");
        parser.addArgument("-v", "--verbose").action(storeTrue());
        parser.addArgument("--dist").action(storeTrue());
        parser.addArgument("--title").action(storeTrue());
        parser.addArgument("--labels")
                .setDefault("labels.arff")
                .help("Labeled data set (ARFF)");
        parser.addArgument("--label")
                .nargs("?")
                .setConst(askForLabelFlag)
                .help("Label to affix to the given song. If no label specified, ask for label.");
        parser.addArgument("--labelsmap")
                .help("File that maps titles to labels");
        parser.addArgument("--classifier")
                .setDefault(WekaClassifierType.COMBINED)
                .choices(new ArgumentChoice() {
                    @Override
                    public boolean contains(Object val) {
                        return Arrays.asList(WekaClassifierType.values()).contains(val);
                    }

                    @Override
                    public String textualFormat() {
                        return Joiner.on("|").join(Iterables.transform(Arrays.asList(WekaClassifierType.values()),
                                new Function<WekaClassifierType, Object>() {
                                    @Override
                                    public Object apply(WekaClassifierType type) {
                                        return type.getName().toString().toLowerCase();
                                    }
                                }));
                    }
                })
                .type(new ArgumentType<WekaClassifierType>() {
                    @Override
                    public WekaClassifierType convert(ArgumentParser parser, Argument arg, String value) throws ArgumentParserException {
                        return WekaClassifierType.forName(value);
                    }
                });
        parser.addArgument("-f", "--file")
                .required(true)
                .nargs("+")
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
        WekaClassifierType classifierType = (WekaClassifierType) ns.get("classifier");
        WekaClassifier classifier = new WekaClassifier(
                classifierType, labels, "IdentifyKey", attributes, keyAttribute(), metaDataAttributes);

        boolean askForLabel = ns.get("label") == askForLabelFlag;
        String labelArg = askForLabel ? null : ns.getString("label");

        Properties labelsMap = null;
        String labelsMapFilename = ns.getString("labelsmap");
        if (labelsMapFilename != null) {
            labelsMap = new Properties();
            File labelsMapFile = new File(labelsMapFilename);
            if (labelsMapFile.exists()) {
                InputStream stream = new BufferedInputStream(new FileInputStream(labelsMapFile));
                labelsMap.load(stream);
                stream.close();
            }
        }

        boolean classifierTrained = false;

        for (String filename : ns.<String>getList("file")) {
            if (verbose) System.out.printf("Reading %s...\n", filename);
            File file = new File(filename);

            Iterable<AudioTrack> tracks = tracks(file);
            for (AudioTrack track : tracks) {
                if (verbose) System.out.printf("Loading audio for `%s'...\n", track.title);
                Audio in = track.acquireAudio();
//                new WaveFileWriter(in, new FileOutputStream(in.getTitle() + ".wav")).write();

                if (verbose) System.out.println("Extracting features...");
                List<Feature> features = extractor.extractFeatures(in);

                String label = labelArg;
                if (label != null || askForLabel) {
                    if (askForLabel) {
                        if (labelsMap != null) {
                            label = labelsMap.getProperty(in.getTitle());
                        }
                        if (label == null) {
                            BufferedReader cons = new BufferedReader(new InputStreamReader(System.in));
                            for (;;) {
                                System.out.printf("Label for `%s': ", in.getTitle());
                                label = key(cons.readLine());
                                if (isValidLabel(label)) break;
                                System.out.printf("Invalid label: `%s'\n", label);
                            }
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

                    if (labelsMap != null) {
                        labelsMap.setProperty(in.getTitle(), label);
                        OutputStream stream = new BufferedOutputStream(new FileOutputStream(labelsMapFilename));
                        labelsMap.store(stream, "labels map");
                        stream.close();
                    }

                    if (verbose) System.out.println("Done.");
                } else {
                    if (!classifierTrained) {
                        if (verbose) System.out.printf("Training `%s' classifier...\n", classifierType.getName());
                        classifier.train();
                        classifierTrained = true;
                    }
                    if (verbose) System.out.println("Classifying...");

                    @SuppressWarnings("unchecked")
                    List<NominalLabel> predictedLabels = (List<NominalLabel>) (List<?>) classifier.classify(features);

                    NominalLabel predictedLabel = predictedLabels.get(0);
                    String predictedKey = predictedLabel.getValue();
                    if (ns.getBoolean("title")) predictedKey = keyWithSynonyms(predictedKey);
                    System.out.print(predictedKey);

                    if (ns.getBoolean("title")) System.out.printf(" : `%s'", in.getTitle());
                    System.out.println();

                    if (ns.getBoolean("dist")) {
                        List<String> output = Lists.newArrayList();
                        for (NominalLabel l : predictedLabels) {
                            double prob = l.getProbability();
                            if (prob > 0.01 || output.isEmpty()) {
                                output.add(String.format("%s (p=%.4f)", keyWithSynonyms(l.getValue()), prob));
                            }
                        }
                        System.out.println(Joiner.on(", ").join(output));
                    }

                    if (verbose) {
                        System.out.printf("Detected key of `%s' in `%s' with probability %.4f.\n",
                                keyWithSynonyms(predictedLabel.getValue()), in.getTitle(), predictedLabel.getProbability());
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

    private static String key(String synonym) {
        String key = keys.contains(synonym) ? synonym : keySynonyms.get(synonym);
        return key == null ? synonym : key;
    }

    private static String keyWithSynonyms(String key) {
        String synonym = keySynonyms.inverse().get(key);
        return synonym == null ? key : (key + "/" + synonym);
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
