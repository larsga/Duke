package no.priv.garshol.duke.recordlinkage;

import java.util.Collection;

import no.priv.garshol.duke.Configuration;
import no.priv.garshol.duke.Processor;
import no.priv.garshol.duke.Record;
import no.priv.garshol.duke.RecordLinkageStrategy;

/**
 * Strategy used for record linkage, to implement a simple greedy matching
 * algorithm where we choose the best alternative above the threshold for each
 * record.
 */
public class RecordLinkageBestStrategy implements RecordLinkageStrategy {

	@Override
	public void compare(Processor processor, Configuration config, Record record,
			Collection<Record> candidates) {

		double max = 0.0;
		Record best = null;

		// go through all candidates, and find the best
		for (Record candidate : candidates) {
			if (processor.isSameAs(record, candidate))
				continue;

			double prob = processor.compare(record, candidate);
			if (prob > max) {
				max = prob;
				best = candidate;
			}
		}

		// pass on the best match, if any
		if (max > config.getThreshold())
			processor.registerMatch(record, best, max);
		else if (config.getMaybeThreshold() != 0.0 && max > config.getMaybeThreshold())
			processor.registerMatchPerhaps(record, best, max);
		else
			processor.registerNoMatchFor(record);

	}

}
