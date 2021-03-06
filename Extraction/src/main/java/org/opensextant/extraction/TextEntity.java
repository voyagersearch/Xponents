/**
 *
 *  Copyright 2009-2013 The MITRE Corporation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * **************************************************************************
 *                          NOTICE
 * This software was produced for the U. S. Government under Contract No.
 * W15P7T-12-C-F600, and is subject to the Rights in Noncommercial Computer
 * Software and Noncommercial Computer Software Documentation Clause
 * 252.227-7014 (JUN 1995)
 *
 * (c) 2012 The MITRE Corporation. All Rights Reserved.
 * **************************************************************************
 */
///** ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~|
//
// _____                                ____                     __                       __
///\  __`\                             /\  _`\                  /\ \__                   /\ \__
//\ \ \/\ \   _____      __     ___    \ \,\L\_\      __   __  _\ \ ,_\     __       ___ \ \ ,_\
// \ \ \ \ \ /\ '__`\  /'__`\ /' _ `\   \/_\__ \    /'__`\/\ \/'\\ \ \/   /'__`\   /' _ `\\ \ \/
//  \ \ \_\ \\ \ \L\ \/\  __/ /\ \/\ \    /\ \L\ \ /\  __/\/>  </ \ \ \_ /\ \L\.\_ /\ \/\ \\ \ \_
//   \ \_____\\ \ ,__/\ \____\\ \_\ \_\   \ `\____\\ \____\/\_/\_\ \ \__\\ \__/.\_\\ \_\ \_\\ \__\
//    \/_____/ \ \ \/  \/____/ \/_/\/_/    \/_____/ \/____/\//\/_/  \/__/ \/__/\/_/ \/_/\/_/ \/__/
//            \ \_\
//             \/_/
//
//   OpenSextant Commons
// *  ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~|
// */

package org.opensextant.extraction;

import org.apache.commons.lang.StringUtils;
import org.opensextant.util.TextUtils;

/**
 * A very simple struct to hold data useful for post-processing entities once found.
 *
 * @author Marc C. Ubaldino, MITRE, ubaldino at mitre dot org
 */
public class TextEntity {

    /**
     *
     */
    protected String text = null;

    /**
     * char offset of entity; location in document where entity starts.
     */
    public int start = -1;
    /**
     * char offset of entity; location in document where entity ends.
     */
    public int end = -1;

    // Use this
    private String context = null;
    // OR this
    private String prematch = null;
    private String postmatch = null;
    /** */
    public String match_id = null;
    /** If this entity is contained completely within some other     */
    public boolean is_submatch = false;
    /** If this entity is a overlaps with some other     */
    public boolean is_overlap = false;
    /** If this entity is a duplicate of some other     */
    public boolean is_duplicate = false;

    /**
     *
     */
    public TextEntity() {
    }

    /**
     * sets the value of the TextEntity
     * @param t text
     */
    public void setText(String t) {
        text = t;
        if (text!=null){
            isLower =TextUtils.isLower(text);
            isUpper = TextUtils.isUpper(text);
        }
    }
    
    private boolean isLower = false;
    private boolean isUpper = false;
    
    /**
     * test If text is ALL lowercase
     * @return true if all lower.
     */
    public boolean isLower(){
        return isLower;
    }
    public boolean isUpper(){
        return isUpper;
    }

    /**
     *
     * @return text, value of a TextEntity
     */
    public String getText() {
        return text;
    }

    /** get the length of the matched text
     * @return int, length
     */
    public int match_length() {
        if (start < 0) {
            // Match not initialized
            return 0;
        }
        return (end - start);
    }

    /** Convenience methods for carrying the context through the output processing */
    /** Set the context with before and after windows
     * @param before text before match
     * @param after text after match
     */
    public void setContext(String before, String after) {
        this.prematch = before;
        this.postmatch = after;
        StringBuilder buf = new StringBuilder();
        buf.append(this.prematch);
        buf.append(" ");
        buf.append(this.text);
        buf.append(" ");
        buf.append(this.postmatch);
        this.context = buf.toString();
    }

    /** Set the context buffer from a single window
     * @param window - textual window
     */
    public void setContext(String window) {
        this.context = window;
    }

    /**
     *
     * @return context buffer regardless if it is singular context or separate pre/post match 
     */
    public String getContext() {
        return this.context;
    }

    /**
     *
     * @return text before match
     */
    public String getContextBefore() {
        return this.prematch;
    }

    /**
     *
     * @return text after match
     */
    public String getContextAfter() {
        return this.postmatch;
    }

    /**
     *
     * @return  string representation of entity
     */
    @Override
    public String toString() {
        return text + " @(" + start + ":" + end + ")";
    }

    /**
     *
     * @param m match/entity object to copy  
     */
    public void copy(TextEntity m) {

        // TextMatch generic stuff:
        this.text = m.text;
        this.start = m.start;
        this.end = m.end;
        this.is_duplicate = m.is_duplicate;
        this.is_overlap = m.is_overlap;
        this.is_submatch = m.is_submatch;

        // These are private.  maybe should use this.setA(m.getA())
        this.postmatch = m.postmatch;
        this.prematch = m.prematch;
        this.context = m.context;
        this.match_id = m.match_id;
    }

    public boolean isWithin(TextEntity t) {
        return (end <= t.end && start >= t.start);
    }

    public boolean isSameMatch(TextEntity t) {
        return (start == t.start && end == t.end);
    }

    public boolean isRightMatch(TextEntity t) {
        return (start == t.start);
    }

    public boolean isLeftMatch(TextEntity t) {
        return (end == t.end);
    }

    public boolean isOverlap(TextEntity t) {
        // t overlaps with self on the left side
        // OR t overlaps with self on right side
        //
        return (end > t.end && start > t.start && start < t.end)
                || (end < t.end && start < t.start && end > t.start);
    }

}
