package keyconstraint.identifykey.ml.extractor;

import java.util.List;
import java.util.Map;

import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import keyconstraint.identifykey.audio.Audio;
import keyconstraint.identifykey.audio.analyzer.Frame;
import keyconstraint.identifykey.audio.analyzer.Spectrum;
import keyconstraint.identifykey.audio.analyzer.SpectrumAnalyzer;
import keyconstraint.identifykey.audio.analyzer.window.WindowFunctions;
import keyconstraint.identifykey.audio.mixer.StereoToMonoMixer;
import keyconstraint.identifykey.feature.PitchClassProfile;
import keyconstraint.identifykey.ml.feature.Feature;

public class PcpFeatureExtractor implements FeatureExtractor {
    @Override
    public List<Feature> extractFeatures(Audio audio) {
        if (audio.getChannels() == 2) {
            audio = new StereoToMonoMixer().mix(audio);
        }
        List<Frame> frames = new SpectrumAnalyzer(WindowFunctions.hamming).analyze(audio);
        PitchClassProfile pcp = new PitchClassProfile(
                Spectrum.combine(Iterables.transform(frames, new Function<Frame, Spectrum>() {
                    @Override
                    public Spectrum apply(Frame frame) {
                        return frame.getSpectrum();
                    }
                })));

        List<Feature> features = Lists.newArrayList();
        for (Map.Entry<String, Double> entry :
                Iterables.concat(pcp.getPitchDistByNote().entrySet(), pcp.getPitchDistByOctaveByNote().entrySet())) {
            features.add(new Feature(entry.getKey(), entry.getValue()));
        }
        return features;
    }
}