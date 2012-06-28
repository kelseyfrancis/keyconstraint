package keyconstraint.identifykey;

import java.io.File;

import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import net.sourceforge.argparse4j.inf.Namespace;

public class IdentifyKey {

    private IdentifyKey() {}

    public static void main(String[] args) {
        ArgumentParser parser = ArgumentParsers.newArgumentParser("IdentifyKey")
                .defaultHelp(true)
                .description("Identify the key of the given songs.");
        parser.addArgument("--labels")
                .required(true)
                .help("Labeled data set (ARFF)");
        parser.addArgument("--label")
                .help("Label to affix to the given song");
        parser.addArgument("file")
                .nargs("*")
                .help("Song to classify or label (WAV)");

        Namespace ns;
        try {
            ns = parser.parseArgs(args);
        } catch (ArgumentParserException e) {
            parser.handleError(e);
            System.exit(1);
            return;
        }

        String labelsFilename = ns.getString("labels");
        File labels = new File(labelsFilename);
        if (!labels.exists()) {
            System.err.println("Could not find file `" + labelsFilename + "'");
            System.exit(1);
            return;
        }
    }
}
