/*
 * Copyright 2015 moscac.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package no.priv.garshol.duke.comparators;

import static junit.framework.Assert.assertEquals;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author moscac
 */
public class PartialMatchComparatorTest {

    private PartialMatchComparator comp;

    @Before
    public void setup() {
        this.comp = new PartialMatchComparator();
    }

    public PartialMatchComparatorTest() {
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    @Test
    public void testEmpty() {
        assertEquals(0.0, comp.compare("", ""));
    }

    @Test
    public void testEmpty2() {
        assertEquals(0.0, comp.compare("", "foo"));
    }

    @Test
    public void testComparatorEqual() {
        assertEquals(1.0, comp.compare("foo", "foo"));
    }
    
    @Test
    public void testComparatorPartial1() {
        assertEquals(1.0, comp.compare("fooFoofoo", "foo"));
    }
}
