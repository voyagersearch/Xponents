/*
 * Copyright 2013 ubaldino.
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
package org.opensextant.examples;

import java.io.File;
import java.util.List;
import java.util.Map;

import org.opensextant.data.Country;
import org.opensextant.data.Place;
import org.opensextant.extractors.geo.SolrGazetteer;

/**
 *
 * @author ubaldino
 */
public class GazetteerSearch {

    /**
     * Do a basic test
     */
    public static void main(String[] args) throws Exception {

        String solrHome = System.getProperty("solr.solr.home");
        if (solrHome == null) {
            System.out.print("Please set the solr.solr.home JVM arg");
            System.exit(1);
        }

        SolrGazetteer gaz = new SolrGazetteer();

        try {

            // Try to get countries
            Map<String, Country> countries = gaz.getCountries();
            for (Country c : countries.values()) {
                System.out.println(c.getKey() + " = " + c.name + "\t  Aliases: " + c.getAliases().toString());
            }

            List<Place> matches = gaz.search("+Boston +City");

            for (Place pc : matches) {
                System.out.println(pc.toString() + " which is categorized as: " + gaz.getFeatureName(pc.getFeatureClass(), pc.getFeatureCode()));
            }

        } catch (Exception err) {
            err.printStackTrace();
        }
        gaz.shutdown();

        System.exit(0);
    }
}
