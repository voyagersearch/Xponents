/**
 * Copyright 2014 The MITRE Corporation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 *
 */

package org.opensextant.extractors.geo.rules;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.opensextant.ConfigException;
import org.opensextant.data.Place;
import org.opensextant.extraction.MatchFilter;
import org.opensextant.extractors.geo.PlaceCandidate;
import org.opensextant.util.FileUtility;

public class PersonNameFilter extends GeocodeRule {

    private MatchFilter filter = null;
    private Set<String> titles = null;
    private Set<String> suffixes = null;

    public PersonNameFilter(URL names, URL persTitles, URL persSuffixes)
            throws ConfigException {
        try {
            filter = new MatchFilter(names);
            titles = FileUtility.loadDictionary(persTitles, false);
            suffixes = FileUtility.loadDictionary(persSuffixes, false);
        } catch (IOException filterErr) {
            throw new ConfigException("Default filter not found", filterErr);
        }
    }

    private Map<String, String> resolvedPersons = new HashMap<>();

    public void reset() {
        resolvedPersons.clear();
    }

    public Map<String, String> getPersonNames() {
        return resolvedPersons;
    }

    @Override
    public void evaluate(PlaceCandidate name, Place geo) {
        /* No other existing evidence that we should keep this entry
         * and if the name is a person name --- AS DEFINED BY THE USER --
         * then we mark it filtered out.
         * 
         */
        if (name.getChosen() != null) {
            return;
        }
        
        /*
         * This was filtered out already so ignore.
         */
        if (name.isFilteredOut()) {
            return;
        }
        
        /*
         * If you have already associated an Admin code with this name, then do not filter out
         * 
         *   Eugene, OR
         *   Jackson, MI
         *   
         *   TODO: 
         *   Euguene, Oregon
         *   etc.
         */
        if (name.hasRule(NameCodeRule.NAME)){
            return;
        }

        String[] toks = name.getPrematchTokens();
        if (toks != null) {
            String pre = toks[toks.length - 1].toLowerCase();
            if (titles.contains(pre)) {
                name.setFilteredOut(true);  
                resolvedPersons.put(val(pre, name.getTextnorm()), name.getText());
            }
        }

        if (filter.filterOut(name.getTextnorm())) {
            name.setFilteredOut(true);
            resolvedPersons.put(name.getTextnorm(), name.getText());
            return;
        }

        toks = name.getPostmatchTokens();
        if (toks != null) {
            String post = toks[0].toLowerCase();
            if (suffixes.contains(post)) {
                name.setFilteredOut(true);
                resolvedPersons.put(val(name.getTextnorm(), post), name.getText());
                return;
            }
        }
    }

    /**
     * Debug support -- formatted value here helps convey the name + title or suffix
     * 
     * @param nm1
     * @param nm2
     * @return
     */
    private String val(String nm1, String nm2) {
        return String.format("%s/%s", nm1, nm2);
    }
}
