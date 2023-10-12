package no.priv.garshol.duke.test;

import org.junit.Test;
import org.junit.Before;
import static junit.framework.Assert.assertEquals;

import no.priv.garshol.duke.comparators.GaussianComparator;

public class GaussianComparatorTest {

    private GaussianComparator defaultComparator;
    private GaussianComparator sigma10Comparator;

    @Before
    public void setUp() {
        defaultComparator = new GaussianComparator();
        sigma10Comparator = new GaussianComparator();
        sigma10Comparator.setSigma(10.0);
    }

    @Test
    public void testEqual() {
        assertEquals(1.0, defaultComparator.compare("42", "42"));
        assertEquals(1.0, sigma10Comparator.compare("42", "42"));
    }

    @Test
    public void testEqual2() {
        assertEquals(1.0, defaultComparator.compare("42.0", "42.0"));
        assertEquals(1.0, sigma10Comparator.compare("42.0", "42.0"));
    }

    @Test
    public void testClose() {
        assertEquals(0.1353, defaultComparator.compare("40", "42"), 0.0001);
        assertEquals(0.9801, sigma10Comparator.compare("40", "42"), 0.0001);
    }

    @Test
    public void testFar() {
        assertEquals(0.0, defaultComparator.compare("25", "42"), 0.0001);
        assertEquals(0.2357, sigma10Comparator.compare("25", "42"), 0.0001);
    }
}
