package no.priv.garshol.duke;

import java.util.Collection;

import no.priv.garshol.duke.matchers.MatchListener;

/**
  * This interface allows the customization of the strategy for record linkage
  * Advanced possibilities exist for record linkage, but
  * they are not implemented yet. see the links below for more
  * information.
  *
  * http://code.google.com/p/duke/issues/detail?id=55
  * http://research.microsoft.com/pubs/153478/msr-report-1to1.pdf
 */
public interface RecordLinkageStrategy {

	/**
	 * Compare record
	 * @param processor The processor to notify {@link MatchListener}
	 * @param config The {@link Configuration}
	 * @param record The record to match
	 * @param candidates The possible candidates
	 */
	void compare(Processor processor, Configuration config, Record record, Collection<Record> candidates);

}
