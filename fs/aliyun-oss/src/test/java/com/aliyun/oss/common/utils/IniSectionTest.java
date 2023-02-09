/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.aliyun.oss.common.utils;

import junit.framework.*;

import java.io.*;
import java.util.*;

public class IniSectionTest extends TestCase {

    public IniSectionTest(String name) {
        super(name);
    }

    /**
     * Illegal section names.
     */
    public void testAddSectionIllegal() {
        try {
            new IniEditor.Section("[hallo");
            fail("Should throw IllegalArgumentException.");
        } catch (IllegalArgumentException ex) { /* ok, this should happen */ }
        try {
            new IniEditor.Section("hallo]");
            fail("Should throw IllegalArgumentException.");
        } catch (IllegalArgumentException ex) { /* ok, this should happen */ }
        try {
            new IniEditor.Section("  \t ");
            fail("Should throw IllegalArgumentException.");
        } catch (IllegalArgumentException ex) { /* ok, this should happen */ }
        try {
            new IniEditor.Section("");
            fail("Should throw IllegalArgumentException.");
        } catch (IllegalArgumentException ex) {/* ok, this should happen */ }
    }

    /**
     * Setting and getting options.
     */
    public void testSetGet() {
        IniEditor.Section s = new IniEditor.Section("test");
        assertEquals(s.get("hallo"), null);
        assertTrue(!s.hasOption("hallo"));
        s.set(" HALLO \t", " \tvelo ");
        assertEquals(s.get("hallo"), "velo");
        assertTrue(s.hasOption("hallo"));
        s.set("hallo", "bike");
        assertEquals(s.get(" \tHALLO "), "bike");
        s.set("hallo", "bi\nk\n\re\n");
        assertEquals(s.get("hallo"), "bike");
    }

    /**
     * Setting and getting options with case-sensitivity.
     */
    public void testSetGetCaseSensitive() {
        IniEditor.Section s = new IniEditor.Section("test", true);
        s.set(" Hallo ", " \tvelo ");
        assertEquals(s.get("Hallo"), "velo");
        assertTrue(s.hasOption("Hallo"));
        assertTrue(!s.hasOption("hallo"));
        s.set("hallO", "bike");
        assertEquals(s.get(" \thallO "), "bike");
        assertEquals(s.get("Hallo"), "velo");
    }

    /**
     * Setting options with illegal names.
     */
    public void testSetIllegalName() {
        IniEditor.Section s = new IniEditor.Section("test");
        try {
            s.set("hallo=", "velo");
            fail("Should throw IllegalArgumentException");
        } catch (IllegalArgumentException ex) { /* ok, this should happen */ }
        try {
            s.set(" \t\t ", "velo");
            fail("Should throw IllegalArgumentException");
        } catch (IllegalArgumentException ex) { /* ok, this should happen */ }
        try {
            s.set("", "velo");
            fail("Should throw IllegalArgumentException");
        } catch (IllegalArgumentException ex) { /* ok, this should happen */ }
    }

    /**
     * Setting and getting with null arguments.
     */
    public void testSetGetNull() {
        IniEditor.Section s = new IniEditor.Section("test");
        try {
            s.set(null, "velo");
            fail("Should throw NullPointerException");
        } catch (NullPointerException ex) { /* ok, this should happen */ }
        s.set("hallo", null);
        try {
            s.get(null);
            fail("Should throw NullPointerException");
        } catch (NullPointerException ex) { /* ok, this should happen */ }
    }

    /**
     * Getting option names.
     */
    public void testOptionNames() {
        IniEditor.Section s = new IniEditor.Section("test");
        s.set("hallo", "velo");
        s.set("hello", "bike");
        List<String> names = s.optionNames();
        assertEquals(names.get(0), "hallo");
        assertEquals(names.get(1), "hello");
        assertEquals(names.size(), 2);
    }

    /**
     * Adding lines.
     */
    public void testAddLines() {
        IniEditor.Section s = new IniEditor.Section("test");
        s.addBlankLine();
        s.addComment("hollderidi");
        s.addComment("hollderidi", '#');
    }

    /**
     * Saving.
     */
    public void testSave() throws IOException {
        String[] expected = new String[]{"", "hallo = velo", "# english",
                "hello = bike"};
        IniEditor.Section s = new IniEditor.Section("test");
        s.addBlankLine();
        s.set("hallo", "velo");
        s.addComment("english");
        s.set("hello", "bike");
        File f = File.createTempFile("test", null);
        s.save(new PrintWriter(new FileOutputStream(f)));
        Object[] saved = fileToStrings(f);
        assertTrue(Arrays.equals(expected, saved));
    }

    /**
     * Loading.
     */
    public void testLoad() throws IOException {
        String[] expected = new String[]{
                "hallo = velo"
                , "hello = bike"
                , "hi = cycle"
                , "ciao : bicicletta"
                , "# some comment"
                , ""
        };
        String[] feed = new String[]{
                "hallo=velo"
                , "hello bike"
                , "hi = cycle"
                , "ciao: bicicletta"
                , "# some comment"
                , ""
                , "[nextsection]"
                , "hallo = bike"
        };
        File f = toTempFile(feed);
        IniEditor.Section s = new IniEditor.Section("test");
        s.load(new BufferedReader(new FileReader(f)));
        assertEquals(s.get("hallo"), "velo");
        assertEquals(s.get("hello"), "bike");
        assertEquals(s.get("hi"), "cycle");
        assertEquals(s.get("ciao"), "bicicletta");
        assertEquals(s.optionNames().size(), 4);
        s.save(new PrintWriter(new FileOutputStream(f)));
        Object[] saved = fileToStrings(f);
        assertTrue(Arrays.equals(expected, saved));
    }

    public void testSetOptionFormat() throws IOException {
        String[] formats = new String[]{
                "%s %s %s", "%s%s%s", "b%s%%%s%%%%%sa"
        };
        String[] expected = new String[]{
                "hallo = velo", "hallo=velo", "bhallo%=%%veloa"
        };
        for (int i = 0; i < formats.length; i++) {
            IniEditor.Section s = new IniEditor.Section("test");
            s.setOptionFormatString(formats[i]);
            s.set("hallo", "velo");
            File f = File.createTempFile("test", null);
            s.save(new PrintWriter(new FileOutputStream(f)));
            Object[] saved = fileToStrings(f);
            assertEquals(expected[i], saved[0]);
        }
    }

    public void testSetIllegalOptionFormat() {
        String[] formats = {"%s %s % %s", "%s %s", "%s%s%s%s", "%s %d %s %s"};
        IniEditor.Section s = new IniEditor.Section("test");
        for (int i = 0; i < formats.length; i++) {
            try {
                s.setOptionFormatString(formats[i]);
                fail("Should throw IllegalArgumentException");
            } catch (IllegalArgumentException ex) { /* ok, this should happen */ }
        }
    }

    private static Object[] fileToStrings(File f) throws IOException {
        BufferedReader r = new BufferedReader(new FileReader(f));
        List<String> l = new LinkedList<String>();
        while (r.ready()) {
            l.add(r.readLine());
        }
        r.close();
        return l.toArray();
    }

    private static File toTempFile(String text) throws IOException {
        File temp = File.createTempFile("inieditortest", null);
        FileWriter fw = new FileWriter(temp);
        fw.write(text);
        fw.close();
        return temp;
    }

    private static File toTempFile(String[] lines) throws IOException {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < lines.length; i++) {
            sb.append(lines[i] + "\n");
        }
        return toTempFile(sb.toString());
    }

    public static Test suite() {
        return new TestSuite(IniSectionTest.class);
    }

    public static void main(String args[]) {
        junit.textui.TestRunner.run(suite());
    }
}
