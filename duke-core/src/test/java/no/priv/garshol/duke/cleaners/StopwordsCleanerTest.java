
package no.priv.garshol.duke.cleaners;

import org.junit.Before;
import org.junit.Test;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

public class StopwordsCleanerTest extends LowerCaseNormalizeCleanerTest {

    public void setUp() {
        cleaner = new StopwordsCleaner();
    }

    @Test
    public void testMapping() {
        assertEquals("Hello my name is duke", cleaner.clean("hello name duke"));
    }

    @Test
  	public void testEmpty() {
    	assertTrue(cleaner.clean("") == "");
  	}

    @Test
  	public void testNull() {
    	assertTrue(cleaner.clean(null) == null);
  	}


}