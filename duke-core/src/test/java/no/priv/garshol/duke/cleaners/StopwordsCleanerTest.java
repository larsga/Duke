
package no.priv.garshol.duke.cleaners;

import org.junit.Before;
import org.junit.Test;

import static junit.framework.Assert.assertEquals;

public class StopwordsCleanerTest extends LowerCaseNormalizeCleanerTest {

    public void setUp() {
        cleaner = new StopwordsCleaner();
    }

    public void testMapping() {
        assertEquals("Hello my name is duke", cleaner.clean("hello name duke"));
    }



}