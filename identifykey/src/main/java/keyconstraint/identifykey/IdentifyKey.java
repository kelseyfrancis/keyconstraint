package keyconstraint.identifykey;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import com.google.common.collect.Lists;
import keyconstraint.identifykey.audio.Audio;
import keyconstraint.identifykey.audio.WaveFileAudio;
import keyconstraint.identifykey.ml.classifier.weka.WekaClassifier;
import keyconstraint.identifykey.ml.classifier.weka.WekaClassifierType;
import keyconstraint.identifykey.ml.extractor.FeatureExtractor;
import keyconstraint.identifykey.ml.extractor.PcpFeatureExtractor;
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
                .help("Label to affix to the given song");
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

        FeatureExtractor extractor = new PcpFeatureExtractor();

        String labelsFilename = ns.getString("labels");
        File labels = new File(labelsFilename);

        List<Feature> attributes = Lists.newArrayList(extractor.getAttributes());
        WekaClassifier classifier = new WekaClassifier(WekaClassifierType.NNGE, labels, "IdentifyKey", attributes, keyAttribute());

        String label = ns.getString("label");

        for (String filename : ns.<String>getList("file")) {
            if (verbose) System.out.printf("Reading %s...\n", filename);
            File file = new File(filename);
            Audio in = new WaveFileAudio(file);

            if (verbose) System.out.println("Extracting features...");
            List<Feature> features = extractor.extractFeatures(in);

            if (label != null) {
                if (!keys.contains(label)) {
                    System.err.printf("Invalid label: '%s'\n", label);
                    System.exit(1);
                    return;
                }
                if (verbose) System.out.printf("Labeling as '%s'...\n", label);
                classifier.label(features, new NominalLabel(keyAttribute, label));

                if (verbose) System.out.println("Saving labels...");
                classifier.writeArffFile();

                if (verbose) System.out.println("Done.");
            } else {
                classifier.train();
                NominalLabel detectedLabel = (NominalLabel) classifier.classify(features).get(0);
                String detectedKey = detectedLabel.getValue();
                double prob = detectedLabel.getProbability();
                if (verbose) {
                    System.out.printf("Detected key of '%s' with probability %.4f.\n", detectedKey, prob);
                } else {
                    System.out.println(detectedKey);
                }
            }
        }
    }

    private static NominalFeature keyAttribute() {
        return new NominalFeature(keyAttribute, null, keys);
    }
}
