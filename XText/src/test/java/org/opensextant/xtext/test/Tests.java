/**
 * **************************************************************************
 * NOTICE This software was produced for the U. S. Government under Contract No.
 * W15P7T-12-C-F600, and is subject to the Rights in Noncommercial Computer
 * Software and Noncommercial Computer Software Documentation Clause
 * 252.227-7014 (JUN 1995)
 *
 * (c) 2012 The MITRE Corporation. All Rights Reserved.
 * **************************************************************************
 */

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.opensextant.xtext.test;

import java.io.*;
import java.net.URL;
import java.util.*;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opensextant.ConfigException;
import org.opensextant.util.FileUtility;
import org.opensextant.xtext.*;
import org.opensextant.xtext.converters.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Marc C. Ubaldino, MITRE, ubaldino at mitre dot org
 */
public class Tests {

    private static final Logger log = LoggerFactory.getLogger(Tests.class);
    private static File tempDir, doc;

    @BeforeClass
    public static void createPlayground() throws IOException {

        // change to a temp directory that is java 1.6 compliant
        //tempDir = Files.createTempDirectory("xtext_test").toFile();
        File t = File.createTempFile("xtext_text","");
        tempDir = new File(t.getAbsolutePath() + "_d");
        tempDir.mkdir();
        t.delete();


        doc = new File(tempDir, "Asia_Fdn_Afghanistan_2009.pdf");
        FileUtils.copyInputStreamToFile(Tests.class.getResourceAsStream("/Asia_Fdn_Afghanistan_2009.pdf"), doc);
    }

    @AfterClass
    public static void cleanupPlayground() throws IOException {
        if (tempDir != null) {
            FileUtils.deleteDirectory(tempDir);
        }
    }

    @Test
    public void testTrivialUncache() throws IOException {
        trivialUncache(doc.getCanonicalPath());
    }

    /**
     * Test the uncaching of content. Account for any unknown exception --
     * Unicode File names is not well-understood. LANG=en_US env allows Java to
     * read in file names with unicode chars. .... otherwise this is an
     * undetectable situation and File(unicode_filename).exists() throws bad
     * error.
     */
    public void trivialUncache(String input) throws IOException {
        String[] types = {"txt"};

        File inputFile = new File(input);
        if (inputFile.isFile()) {
            try {
                ConvertedDocument doc = PathManager.getCachedDocument(inputFile);
                if (doc == null) {
                    log.info("No document in cache for " + inputFile.getPath());
                } else {
                    log.info(doc.filepath + " TITLE=" + doc.getProperty("title"));
                }
            } catch (Exception anyErr) {
                log.error("Any error could happend:  Unicode file name?  FILE=" + inputFile.getAbsolutePath(), anyErr);
            }
        } else if (inputFile.isDirectory()) {
            Collection<File> listing = FileUtils.listFiles(inputFile, types, true);
            for (File f : listing) {
                try {
                    if ("xtext".equals(f.getParentFile().getName())) {
                        ConvertedDocument doc = PathManager.getCachedDocument(f);
                        log.info(doc.filepath + " TITLE=" + doc.getProperty("title"));
                    }
                } catch (Exception anyErr) {
                    log.error("Any error could happend:  Unicode file name?  FILE=" + f.getAbsolutePath(), anyErr);
                }
            }
        }
    }

    @Test
    public void testTrivialInventory() throws IOException, ConfigException {
        trivialInventory(doc.getCanonicalPath(), true);
    }

    public void trivialInventory(String input, boolean test_save) throws IOException, ConfigException {
        XText xt = new XText();
        xt.getPathManager().enableSaveWithInput(test_save);
        xt.enableSaving(test_save);
        xt.enableOverwrite(true); // reuse cached conversions if possible.
        xt.setup();
        xt.setConversionListener(
                new ConversionListener() {
            public void handleConversion(ConvertedDocument d, String fpath) {
                log.info("FILE=" + d.filename + " Converted?=" + d.is_converted + " ID={} PATH={}", d.id, fpath);
                d.setDefaultID();
                log.info("\t\tTry resetting Doc ID to default ID = " +d.id);
            }
        });
        xt.extractText(input);
    }
    
    @Test
    public void parseEmbedded() throws IOException, ConfigException{
        URL item = Test.class.getResource("/T-DS_Excel2003-PPT2003_1.xls");
        String input = item.getFile();

        XText xt = new XText();
        xt.getPathManager().enableSaveWithInput(true);
        
        xt.enableSaving(true);
        xt.enableOverwrite(true); // reuse cached conversions if possible.
        
        xt.setup();
        xt.setConversionListener(
                new ConversionListener() {
            public void handleConversion(ConvertedDocument d, String fpath) {
                log.info("FILE=" + d.filename + " Converted?=" + d.is_converted + " ID={} PATH={}", d.id, fpath);
                d.setDefaultID();
                log.info("\t\tTry resetting Doc ID to default ID = " +d.id);
            }
        });
        xt.extractText(input);
        
        //assert(true);
}

    @Test
    public void testHTMLConversion() throws IOException, java.net.URISyntaxException {
        String buffer = "<html><head><title>Silly conversion</title></head><body> Document text would be here.</body>";
        ConvertedDocument doc = new TikaHTMLConverter(false).convert(buffer);
        log.info("HTML: tile= " + doc.getProperty("title"));
        log.info("HTML: body= " + doc.getText());

        File f = new File(Tests.class.getResource("/test.html").toURI());
        doc = new TikaHTMLConverter(true).convert(f);
        log.info("HTML: tile= " + doc.getProperty("title"));
        log.info("HTML: body= " + doc.getText());

        doc = new TikaHTMLConverter(false).convert(f);
        log.info("HTML: tile= " + doc.getProperty("title"));
        log.info("HTML: body= " + doc.getText());
    }
    
    public void testHTMLDecode(String f) throws IOException {
        String out = StringEscapeUtils.unescapeHtml(FileUtility.readFile(f));
        FileUtility.writeFile(out, f + ".out");
    }

    public static void main(String[] args) {
        Tests t = new Tests();
        try {
            t.testHTMLDecode(args[0]);
            t.testHTMLConversion();
            if (args.length > 0) {
                t.trivialUncache(args[0]);
                t.trivialInventory(args[0], true);
            }
        } catch (Exception ioerr) {
            ioerr.printStackTrace();
        }
    }

}
