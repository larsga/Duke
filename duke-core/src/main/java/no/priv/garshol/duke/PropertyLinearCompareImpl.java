/*
 * Copyright 2015 marko.
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
package no.priv.garshol.duke;

/**
 *
 * @author marko
 */
public class PropertyLinearCompareImpl extends PropertyImpl {         
    
    public PropertyLinearCompareImpl(String name) {
        super(name);
    }

    public PropertyLinearCompareImpl(String name, Comparator comparator, double low,
            double high) {
        super(name, comparator, low, high);
    }

    @Override
    public RetVal compare(String v1, String v2) {    
        RetVal ret = new RetVal();
        if (comparator == null) {
            return ret;
        }        
        ret.raw = comparator.compare(v1, v2);
        ret.calculated = ret.raw * (high - low) + low;
        return ret;
    }

    @Override
    public Property copy() {
        if (id) {
            return new PropertyLinearCompareImpl(name);
        }

        PropertyLinearCompareImpl p = new PropertyLinearCompareImpl(name, comparator, low, high);
        p.setIgnoreProperty(ignore);
        p.setLookupBehaviour(lookup);
        return p;
    }
}
