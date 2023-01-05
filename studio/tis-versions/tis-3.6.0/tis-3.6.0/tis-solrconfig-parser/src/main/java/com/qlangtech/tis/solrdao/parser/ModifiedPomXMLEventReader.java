/**
 *   Licensed to the Apache Software Foundation (ASF) under one
 *   or more contributor license agreements.  See the NOTICE file
 *   distributed with this work for additional information
 *   regarding copyright ownership.  The ASF licenses this file
 *   to you under the Apache License, Version 2.0 (the
 *   "License"); you may not use this file except in compliance
 *   with the License.  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */
package com.qlangtech.tis.solrdao.parser;

import java.io.File;
import java.io.StringReader;
import java.util.Stack;
import java.util.regex.Pattern;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Characters;
import javax.xml.stream.events.XMLEvent;
import org.apache.commons.io.FileUtils;

/**
how to use this class: http://tutorials.jenkov.com/java-xml/stax-xmlinputfactory.html
 *
 * @author 百岁（baisui@qlangtech.com）
 * @date 2020/04/13
 */
public class ModifiedPomXMLEventReader implements XMLEventReader {

    private static final int MAX_MARKS = 3;

    private final StringBuffer pom;

    private boolean modified = false;

    private final XMLInputFactory factory;

    private int nextStart = 0;

    private int nextEnd = 0;

    private int[] markStart = new int[3];

    private int[] markEnd = new int[3];

    private int[] markDelta = new int[3];

    private int lastStart = -1;

    private int lastEnd;

    private int lastDelta = 0;

    private XMLEvent next = null;

    private int nextDelta = 0;

    private XMLEventReader backing;

    public ModifiedPomXMLEventReader(StringBuffer pom, XMLInputFactory factory) throws XMLStreamException {
        this.pom = pom;
        this.factory = factory;
        rewind();
    }

    public void rewind() throws XMLStreamException {
        this.backing = this.factory.createXMLEventReader(new StringReader(this.pom.toString()));
        this.nextEnd = 0;
        this.nextDelta = 0;
        for (int i = 0; i < 3; i++) {
            this.markStart[i] = -1;
            this.markEnd[i] = -1;
            this.markDelta[i] = 0;
        }
        this.lastStart = -1;
        this.lastEnd = -1;
        this.lastDelta = 0;
        this.next = null;
    }

    public boolean isModified() {
        return this.modified;
    }

    public Object next() {
        try {
            return nextEvent();
        } catch (XMLStreamException e) {
        }
        return null;
    }

    public void remove() {
        throw new UnsupportedOperationException();
    }

    public XMLEvent nextEvent() throws XMLStreamException {
        try {
            XMLEvent localXMLEvent = this.next;
            return localXMLEvent;
        } finally {
            this.next = null;
            this.lastStart = this.nextStart;
            this.lastEnd = this.nextEnd;
            this.lastDelta = this.nextDelta;
        }
    // throw new RuntimeException();
    }

    public XMLEvent peek() throws XMLStreamException {
        return this.backing.peek();
    }

    public String getElementText() throws XMLStreamException {
        return this.backing.getElementText();
    }

    public XMLEvent nextTag() throws XMLStreamException {
        while (hasNext()) {
            XMLEvent e = nextEvent();
            if ((e.isCharacters()) && (!((Characters) e).isWhiteSpace())) {
                throw new XMLStreamException("Unexpected text");
            }
            if ((e.isStartElement()) || (e.isEndElement())) {
                return e;
            }
        }
        throw new XMLStreamException("Unexpected end of Document");
    }

    public Object getProperty(String name) {
        return this.backing.getProperty(name);
    }

    public void close() throws XMLStreamException {
        this.backing.close();
        this.next = null;
        this.backing = null;
    }

    public StringBuffer asStringBuffer() {
        return new StringBuffer(this.pom.toString());
    }

    public void clearMark(int index) {
        this.markStart[index] = -1;
    }

    public String getMarkVerbatim(int index) {
        if (hasMark(index)) {
            return this.pom.substring(this.markDelta[index] + this.markStart[index], this.markDelta[index] + this.markEnd[index]);
        }
        return "";
    }

    public String getPeekVerbatim() {
        if (hasNext()) {
            return this.pom.substring(this.nextDelta + this.nextStart, this.nextDelta + this.nextEnd);
        }
        return "";
    }

    public boolean hasNext() {
        if (this.next != null) {
            return true;
        }
        if (!this.backing.hasNext()) {
            return false;
        }
        try {
            this.next = this.backing.nextEvent();
            this.nextStart = this.nextEnd;
            if (this.backing.hasNext()) {
                this.nextEnd = this.backing.peek().getLocation().getCharacterOffset();
            }
            if (this.nextEnd != -1) {
                if (!this.next.isCharacters()) {
                    while ((this.nextStart < this.nextEnd) && (this.nextStart < this.pom.length()) && ((c(this.nextStart) == '\n') || (c(this.nextStart) == '\r'))) {
                        this.nextStart += 1;
                    }
                }
                while ((nextEndIncludesNextEvent()) || (nextEndIncludesNextEndElement())) {
                    this.nextEnd -= 1;
                }
            }
            return this.nextStart < this.pom.length();
        } catch (XMLStreamException e) {
        }
        return false;
    }

    public String getVerbatim() {
        if ((this.lastStart >= 0) && (this.lastEnd >= this.lastStart)) {
            return this.pom.substring(this.lastDelta + this.lastStart, this.lastDelta + this.lastEnd);
        }
        return "";
    }

    public void mark(int index) {
        this.markStart[index] = this.lastStart;
        this.markEnd[index] = this.lastEnd;
        this.markDelta[index] = this.lastDelta;
    }

    private boolean nextEndIncludesNextEndElement() {
        return (this.nextEnd > this.nextStart + 2) && (this.nextEnd - 2 < this.pom.length()) && (c(this.nextEnd - 2) == '<');
    }

    private boolean nextEndIncludesNextEvent() {
        return (this.nextEnd > this.nextStart + 1) && (this.nextEnd - 2 < this.pom.length()) && ((c(this.nextEnd - 1) == '<') || (c(this.nextEnd - 1) == '&'));
    }

    private char c(int index) {
        return this.pom.charAt(this.nextDelta + index);
    }

    public void replace(String replacement) {
        if ((this.lastStart < 0) || (this.lastEnd < this.lastStart)) {
            throw new IllegalStateException();
        }
        int start = this.lastDelta + this.lastStart;
        int end = this.lastDelta + this.lastEnd;
        if (replacement.equals(this.pom.substring(start, end))) {
            return;
        }
        this.pom.replace(start, end, replacement);
        int delta = replacement.length() - this.lastEnd - this.lastStart;
        this.nextDelta += delta;
        for (int i = 0; i < 3; i++) {
            if ((!hasMark(i)) || (this.lastStart != this.markStart[i]) || (this.markEnd[i] != this.lastEnd))
                continue;
            this.markEnd[i] += delta;
        }
        this.lastEnd += delta;
        this.modified = true;
    }

    public boolean hasMark(int index) {
        return this.markStart[index] != -1;
    }

    public String getBetween(int index1, int index2) {
        if ((!hasMark(index1)) || (!hasMark(index2)) || (this.markStart[index1] > this.markStart[index2])) {
            throw new IllegalStateException();
        }
        int start = this.markDelta[index1] + this.markEnd[index1];
        int end = this.markDelta[index2] + this.markStart[index2];
        return this.pom.substring(start, end);
    }

    public void replaceBetween(int index1, int index2, String replacement) {
        if ((!hasMark(index1)) || (!hasMark(index2)) || (this.markStart[index1] > this.markStart[index2])) {
            throw new IllegalStateException();
        }
        int start = this.markDelta[index1] + this.markEnd[index1];
        int end = this.markDelta[index2] + this.markStart[index2];
        if (replacement.equals(this.pom.substring(start, end))) {
            return;
        }
        this.pom.replace(start, end, replacement);
        int delta = replacement.length() - (end - start);
        this.nextDelta += delta;
        for (int i = 0; i < 3; i++) {
            if ((i == index1) || (i == index2) || (this.markStart[i] == -1)) {
                continue;
            }
            if (this.markStart[i] > this.markStart[index2]) {
                this.markDelta[i] += delta;
            } else if ((this.markStart[i] == this.markEnd[index1]) && (this.markEnd[i] == this.markStart[index1])) {
                this.markDelta[i] += delta;
            } else {
                if ((this.markStart[i] <= this.markEnd[index1]) && (this.markEnd[i] >= this.markStart[index2]))
                    continue;
                this.markStart[i] = -1;
            }
        }
        this.modified = true;
    }

    public void replaceMark(int index, String replacement) {
        if (!hasMark(index)) {
            throw new IllegalStateException();
        }
        int start = this.markDelta[index] + this.markStart[index];
        int end = this.markDelta[index] + this.markEnd[index];
        if (replacement.equals(this.pom.substring(start, end))) {
            return;
        }
        this.pom.replace(start, end, replacement);
        int delta = replacement.length() - this.markEnd[index] - this.markStart[index];
        this.nextDelta += delta;
        if ((this.lastStart == this.markStart[index]) && (this.lastEnd == this.markEnd[index])) {
            this.lastEnd += delta;
        } else if (this.lastStart > this.markStart[index]) {
            this.lastDelta += delta;
        }
        for (int i = 0; i < 3; i++) {
            if ((i == index) || (this.markStart[i] == -1)) {
                continue;
            }
            if (this.markStart[i] > this.markStart[index]) {
                this.markDelta[i] += delta;
            } else {
                if ((this.markStart[i] != this.markStart[index]) || (this.markEnd[i] != this.markEnd[index]))
                    continue;
                this.markDelta[i] += delta;
            }
        }
        this.markEnd[index] += delta;
        this.modified = true;
    }

    // public Model parse()
    // throws IOException, XmlPullParserException
    // {
    // MavenXpp3Reader reader = new MavenXpp3Reader();
    // return reader.read(new StringReader(this.pom.toString()));
    // }
    public static void main(String[] arg) throws Exception {
        String content = FileUtils.readFileToString(new File("C:\\Users\\baisui\\Desktop\\tsearcher_test\\tsearcher_home1\\juinstance\\search4juitem-0\\conf\\schema.xml"));
        XMLInputFactory inputFactory = XMLInputFactory.newInstance();
        inputFactory.setProperty(XMLInputFactory.IS_VALIDATING, false);
        ModifiedPomXMLEventReader pom = new ModifiedPomXMLEventReader(new StringBuffer(content), inputFactory);
        // while (reader.hasNext()) {
        // XMLEvent event = reader.nextEvent();
        //
        // System.out.println(reader.getElementText());
        //
        // System.out.println("event:" + event.toString());
        // }
        Stack stack = new Stack();
        String path = "";
        Pattern matchScopeRegex = Pattern.compile("/schema/uniqueKey");
        pom.rewind();
        while (pom.hasNext()) {
            XMLEvent event = pom.nextEvent();
            if (event.isStartElement()) {
                stack.push(path);
                path = path + "/" + event.asStartElement().getName().getLocalPart();
                if (matchScopeRegex.matcher(path).matches()) {
                    pom.mark(0);
                }
            }
            if (event.isEndElement()) {
                if (matchScopeRegex.matcher(path).matches()) {
                    pom.mark(1);
                    if ((pom.hasMark(0)) && (pom.hasMark(1))) {
                        System.out.println(pom.getBetween(0, 1).trim());
                    }
                    pom.clearMark(0);
                    pom.clearMark(1);
                }
                path = (String) stack.pop();
            }
        }
    // return null;
    }
}
