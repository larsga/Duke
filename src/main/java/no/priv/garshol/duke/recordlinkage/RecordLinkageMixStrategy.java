package no.priv.garshol.duke.recordlinkage;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import no.priv.garshol.duke.Configuration;
import no.priv.garshol.duke.Processor;
import no.priv.garshol.duke.Record;
import no.priv.garshol.duke.RecordLinkageStrategy;

/**
 * Strategy used for record linkage, to implement a simple greedy matching
 * algorithm where we choose the best alternative above the threshold for each
 * record.
 */
public class RecordLinkageMixStrategy implements RecordLinkageStrategy {

	@Override
	public void compare(Processor processor, Configuration config, Record record, Collection<Record> candidates) {

		double max = 0.0;
		Record best = null;

		List<Record> maybe = new ArrayList<Record>();
		List<Double> maybeScores = new ArrayList<Double>();

		// go through all candidates, and find the best
		for (Record candidate : candidates) {
			if (processor.isSameAs(record, candidate))
				continue;

			double prob = processor.compare(record, candidate);

			if (prob > config.getThreshold()) {
				if (prob > max) {
					max = prob;
					best = candidate;
				}
			} else if (config.getMaybeThreshold() != 0.0 && prob > config.getMaybeThreshold()) {
				maybe.add(candidate);
				maybeScores.add(prob);
			}
		}

		// notify MatchListeners

		if (best!=null) {
			processor.registerMatch(record, best, max);
		}
		else if (maybe.size()>0) {
			for (int i = 0; i < maybe.size(); i++) {
				processor.registerMatchPerhaps(record, maybe.get(i), maybeScores.get(i));
			}
		}
		else {
			processor.registerNoMatchFor(record);
		}

	}

}
