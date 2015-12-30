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

import no.priv.garshol.duke.Comparator;

/**
 *
 * @author moscac
 */
public class PartialMatchComparator implements Comparator {

    @Override
    public boolean isTokenized() {
        return false;
    }

    @Override
    public double compare(String v1, String v2) {
        if (v1.isEmpty() || v2.isEmpty()) {
            return 0.0;
        }
        if (v1.equalsIgnoreCase(v2)) {
            return 1.0;
        }
        if (v1.length() == v2.length()) {
            return 0.0;
        }
        if (longest(v1, v2, true).contains(longest(v1, v2, false))) {
            return 1.0;
        }
        return 0.0;
    }

    private String longest(String v1, String v2, boolean longest) {
        if (v1.length() >= v2.length()) {
            return longest ? v1 : v2;
        }
        return longest ? v2 : v1;
    }

}
