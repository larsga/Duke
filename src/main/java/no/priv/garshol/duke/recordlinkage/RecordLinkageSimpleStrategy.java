package no.priv.garshol.duke.recordlinkage;

import java.util.Collection;

import no.priv.garshol.duke.Configuration;
import no.priv.garshol.duke.RecordLinkageStrategy;
import no.priv.garshol.duke.Processor;
import no.priv.garshol.duke.Record;

/**
 * Simple startegy used for deduplication, where we simply
 * want all matches above the thresholds.
 */
public class RecordLinkageSimpleStrategy implements RecordLinkageStrategy {


	@Override
	public void compare(Processor processor, Configuration config, Record record, Collection<Record> candidates) {

		boolean found = false;
		for (Record candidate : candidates) {
			if (processor.isSameAs(record, candidate))
				continue;

			double prob = processor.compare(record, candidate);
			if (prob > config.getThreshold()) {
				found = true;
				processor.registerMatch(record, candidate, prob);
			} else if (config.getMaybeThreshold() != 0.0 && prob > config.getMaybeThreshold()) {
				found = true; // I guess?
				processor.registerMatchPerhaps(record, candidate, prob);
			}
		}
		if (!found) {
			processor.registerNoMatchFor(record);
		}

	}

}
