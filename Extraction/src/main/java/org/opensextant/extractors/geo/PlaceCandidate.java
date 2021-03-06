/**
 * Copyright 2009-2013 The MITRE Corporation.
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
 *
 * **************************************************************************
 * NOTICE This software was produced for the U. S. Government under Contract No.
 * W15P7T-12-C-F600, and is subject to the Rights in Noncommercial Computer
 * Software and Noncommercial Computer Software Documentation Clause
 * 252.227-7014 (JUN 1995)
 *
 * (c) 2012 The MITRE Corporation. All Rights Reserved.
 * **************************************************************************
*
 */
package org.opensextant.extractors.geo;

import org.opensextant.data.Geocoding;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.opensextant.data.Place;
import org.opensextant.data.LatLon;
import org.opensextant.extraction.TextMatch;
import org.opensextant.util.TextUtils;

/**
 * A PlaceCandidate represents a portion of a document which has been identified
 * as a possible named geographic location. It is used to collect together the
 * information from the document (the evidence), as well as the possible
 * geographic locations it could represent (the Places ). It also contains the
 * results of the final decision to include:
 * <ul>
 * <li>placeConfidenceScore - Confidence that this is actually a place and not a
 * person, organization, or other type of entity.
 * <li>bestPlace - Of all the places with the same/similar names, which place is
 * it?
 * </ul>
 */
public class PlaceCandidate extends TextMatch /*Serializable*/{

    private String textnorm = null;

    // the location this was found in the document
    //private Long start;
    //private Long end;
    // --------------Place/NotPlace stuff ----------------------
    // which rules have expressed a Place/NotPlace opinion on this PC
    private Set<String> rules = new HashSet<>();
    // the confidence adjustments provided by the Place/NotPlace rules
    private List<Double> placeConfidences = new ArrayList<>();
    // --------------Disambiguation stuff ----------------------
    // the places along with their disambiguation scores
    private Map<Place, Double> scoredPlaces = new HashMap<>();
    // temporary lists to hold the ranked places and scores
    private List<Place> rankedPlaces = new ArrayList<>();
    private List<Double> rankedScores = new ArrayList<>();
    // the list of PlaceEvidences accumulated from the document about this PC
    private List<PlaceEvidence> evidence = new ArrayList<>();
    // The chosen, best place:
    private Place chosen = null;

    // basic constructor
    public PlaceCandidate() {
    }

    /**
     * If caller is willing to claim an explicit choice, so be it.
     * Otherwise unchosen places go to disambiguation.
     */
    public void choose(Place geo) {
        chosen = geo;
    }

    /**
     * 
     * @return normalized version of text.
     */
    public String getTextnorm() {
        if (textnorm == null) {
            textnorm = TextUtils.removePunctuation(TextUtils.removeDiacritics(getText()))
                    .toLowerCase();
        }
        return textnorm;
    }

    // ---- the getters and setters ---------
    //

    private String[] preTokens = null;
    private String[] postTokens = null;
    private int DEFAULT_TOKEN_SIZE = 40;

    /**
     * Get some sense of tokens surrounding match. Possibly 
     * optimize this by getting token list from SolrTextTagger (which provides the lang-specifics) 
     * 
     * @param sourceBuffer
     */
    protected void setSurroundingTokens(String sourceBuffer) {
        int[] window = TextUtils.get_text_window(start, end - start, sourceBuffer.length(),
                DEFAULT_TOKEN_SIZE);

        /* Get right most or left most whole tokens, for now whitespace delimited.
         * TODO: ensure whole tokens are retrieved. 
         */
        setPrematchTokens(TextUtils.tokens(sourceBuffer.substring(window[0], window[1])));
        setPostmatchTokens(TextUtils.tokens(sourceBuffer.substring(window[2], window[3])));
    }

    /**
     * Common evidence flags -- isCountry, isPerson, isOrganization, abbreviation, and acronym
     */
    public boolean isCountry = false;
    public boolean isPerson = false;
    public boolean isOrganization = false;
    public boolean isAbbreviation = false;
    public boolean isAcronym = false;

    /** After candidate has been scored and all, the final best place is the geocoding result for the given name in context.
     */
    public Geocoding getGeocoding() {
        getBestPlace();
        return chosen;
    }

    public Place getChosen() {
        return chosen;
    }

    /**
     * Get the most highly ranked Place, or Null if empty list.
     * @return Place the best choice
     */
    public Place getBestPlace() {
        if (chosen != null) {
            return chosen;
        }

        List<Place> l = this.getPlaces();
        if (l.isEmpty()) {
            chosen = null; // ensure null.
            return null;
        }
        chosen = l.get(0);
        return chosen;

    }

    //
    /**
     * Get the disambiguation score of the most highly ranked Place, or 0.0 if
     * empty list.
     * @return score of best place
     */
    public Double getBestPlaceScore() {
        List<Double> l = this.getScores();
        if (l.isEmpty()) {
            return 0.0;
        }
        return l.get(0);
    }

    /**
     * @return true = if a Place.  Does our confidence indicate that this is actually a place?
     */
    public boolean isPlace() {
        return (this.getPlaceConfidenceScore() > 0.0);
    }

    /**
     * Get a ranked list of places
     * @return list of places ranked, sorted.
     */
    public List<Place> getPlaces() {
        this.sort();
        return this.rankedPlaces;
    }

    /**
     * Get a ranked list of scores
     * @return list of scores
     */
    public List<Double> getScores() {
        this.sort();
        return this.rankedScores;
    }

    // add a new place with a default score
    public void addPlace(Place place) {
        this.addPlaceWithScore(place, 0.0);
    }

    // add a new place with a specific score
    public void addPlaceWithScore(Place place, Double score) {
        this.scoredPlaces.put(place, score);
    }

    // increment the score of an existing place
    public void incrementPlaceScore(Place place, Double score) {
        Double currentScore = this.scoredPlaces.get(place);
        if (currentScore != null) {
            this.scoredPlaces.put(place, currentScore + score);
        } else {
            // log.error("Tried to increment a score for a non-existent Place");
        }
    }

    // set the score of an existing place
    public void setPlaceScore(Place place, Double score) {
        if (!this.scoredPlaces.containsKey(place)) {
            // log.error("Tried to increment a score for a non-existent Place");
            return;
        }
        this.scoredPlaces.put(place, score);
    }

    public Collection<String> getRules() {
        return rules;
    }

    public boolean hasRule(String rule) {
        return rules.contains(rule);
    }

    public List<Double> getConfidences() {
        return placeConfidences;
    }

    // check if at least one of the Places has the given country code
    public boolean possibleCountry(String cc) {
        for (Place p : rankedPlaces) {
            if (p.getCountryCode() != null && p.getCountryCode().equalsIgnoreCase(cc)) {
                return true;
            }
        }
        return false;
    }

    // check if at least one of the Places has the given admin code
    public boolean possibleAdmin(String adm, String cc) {

        // check the non-null admins first
        for (Place p : rankedPlaces) {
            if (p.getAdmin1() != null && p.getAdmin1().equalsIgnoreCase(adm)) {
                return true;
            }
        }

        // some adm1codes are null, a null admin of the correct country could be possible match
        for (Place p : rankedPlaces) {
            if (p.getAdmin1() == null && p.getCountryCode().equalsIgnoreCase(cc)) {
                return true;
            }
        }

        return false;
    }

    public void addRuleAndConfidence(String rule, Double conf) {
        rules.add(rule);
        placeConfidences.add(conf);
    }

    /**
     * Get the PlaceConfidence score. This is the confidence that this
     * PlaceCandidate represents a named place and not a person,organization or
     * other entity.
     *
     * @return the place confidence score
     */
    public Double getPlaceConfidenceScore() {
        if (placeConfidences.size() == 0) {
            return 0.0;
        }

        // average of placeConfidences
        Double total = 0.0;
        for (Double tmpScore : placeConfidences) {
            total = total + tmpScore;
        }
        Double tmp = total / placeConfidences.size();

        // ensure the final score is within +-1.0
        if (tmp > 1.0) {
            tmp = 1.0;
        }

        if (tmp < -1.0) {
            tmp = -1.0;
        }

        return tmp;
    }

    public void addEvidence(PlaceEvidence evidence) {
        this.evidence.add(evidence);
        if (evidence.getRule() != null) {
            this.rules.add(evidence.getRule());
        }
    }

    public void addEvidence(String rule, double weight, Place ev) {
        addEvidence(new PlaceEvidence(ev, rule, weight));
    }

    // some convenience methods to add evidence
    public void addEvidence(String rule, double weight, String cc, String adm1, String fclass,
            String fcode, LatLon geo) {
        PlaceEvidence ev = new PlaceEvidence();
        ev.setRule(rule);
        ev.setWeight(weight);
        if (cc != null) {
            ev.setCountryCode(cc);
        }
        if (adm1 != null) {
            ev.setAdmin1(adm1);
        }
        if (fclass != null) {
            ev.setFeatureClass(fclass);
        }
        if (fcode != null) {
            ev.setFeatureCode(fcode);
        }
        if (geo != null) {
            ev.setLatLon(geo);
        }
        this.evidence.add(ev);
    }

    public void addCountryEvidence(String rule, double weight, String cc) {
        PlaceEvidence ev = new PlaceEvidence();
        ev.setRule(rule);
        ev.setWeight(weight);
        ev.setCountryCode(cc);
        this.evidence.add(ev);
    }

    public void addAdmin1Evidence(String rule, double weight, String adm1, String cc) {
        PlaceEvidence ev = new PlaceEvidence();
        ev.setRule(rule);
        ev.setWeight(weight);
        ev.setAdmin1(adm1);
        ev.setCountryCode(cc);
        this.evidence.add(ev);
    }

    public void addFeatureClassEvidence(String rule, double weight, String fclass) {
        PlaceEvidence ev = new PlaceEvidence();
        ev.setRule(rule);
        ev.setWeight(weight);
        ev.setFeatureClass(fclass);
        this.evidence.add(ev);
    }

    public void addFeatureCodeEvidence(String rule, double weight, String fcode) {
        PlaceEvidence ev = new PlaceEvidence();
        ev.setRule(rule);
        ev.setWeight(weight);
        ev.setFeatureCode(fcode);
        this.evidence.add(ev);
    }

    public void addGeocoordEvidence(String rule, double weight, LatLon coord) {
        PlaceEvidence ev = new PlaceEvidence();
        ev.setRule(rule);
        ev.setWeight(weight);
        ev.setLatLon(coord);
        this.evidence.add(ev);
    }

    public List<PlaceEvidence> getEvidence() {
        return this.evidence;
    }

    public boolean hasPlaces() {
        return !this.scoredPlaces.isEmpty();
    }

    private void sort() {
        this.rankedPlaces.clear();
        this.rankedScores.clear();

        List<ScoredPlace> tmp = new ArrayList<ScoredPlace>();

        for (Place pl : this.scoredPlaces.keySet()) {
            tmp.add(new ScoredPlace(pl, scoredPlaces.get(pl)));
        }

        Collections.sort(tmp);

        for (ScoredPlace spl : tmp) {
            this.rankedPlaces.add(spl.getPlace());
            this.rankedScores.add(spl.getScore());
        }

    }

    // an overide of toString to get a meaningful representation of this PC
    @Override
    public String toString() {
        String tmp = getText() + "(" + this.getPlaceConfidenceScore() + "/"
                + this.scoredPlaces.size() + ")" + "\n";
        tmp = tmp + "Rules=" + this.rules.toString() + "\n";
        tmp = tmp + "Evidence=" + this.evidence.toString() + "\n";

        this.sort();
        tmp = tmp + "Places=";
        for (int i = 0; i < this.rankedPlaces.size(); i++) {
            tmp = tmp + this.rankedPlaces.get(i).toString() + "="
                    + this.rankedScores.get(i).toString() + "\n";
        }
        return tmp;
    }

    /**
     * @return the preTokens
     */
    public String[] getPrematchTokens() {
        return preTokens;
    }

    /**
     * @param preTokens the preTokens to set
     */
    public void setPrematchTokens(String[] preTokens) {
        this.preTokens = preTokens;
    }

    /**
     * @return the postTokens
     */
    public String[] getPostmatchTokens() {
        return postTokens;
    }

    /**
     * @param postTokens the postTokens to set
     */
    public void setPostmatchTokens(String[] postTokens) {
        this.postTokens = postTokens;
    }
}
