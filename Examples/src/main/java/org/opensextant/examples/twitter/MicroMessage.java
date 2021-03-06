/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensextant.examples.twitter;

import java.util.Date;

import net.sf.json.JSONObject;

import org.opensextant.data.Geocoding;

/**
 * TODO: create a better common data class.
 * 
 * @author ubaldino
 */
public abstract class MicroMessage {

    /**
     * NewsItem -- override later
     */
    public String author = null;
    public String location_cc = null;
    public String author_cc = null;
    public String author_location = null;
    public String author_xy_val = null;
    public Geocoding author_xy = null;
    /** Data attributes */
    public Date pub_date = null;
    public String id = null;
    /** Integration attributes */
    protected String messageText = null;
    public long rawbytes = 0L; // This is really Char Count.
    
    public MicroMessage() {
    }

    public abstract void fromJSON(JSONObject data) throws Exception;

    public MicroMessage(String _id, String text, Date tm) {

        this.id = _id;
        this.setText(text);
        this.pub_date = tm;
    }

    public void reset() {
        id = null;
        messageText = null;
        rawbytes = 0;
        location_cc = null;
        author_cc = null;
        author_location = null;
        author = null;
        pub_date = null;
    }

    /**
     */
    public final void setText(String t) {
        messageText = t;
        if (t != null) {
            rawbytes = t.length();
        } else {
            rawbytes = 0L;
        }
    }

    /**

     */
    public String getText() {
        return this.messageText;
    }

}
