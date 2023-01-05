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
package org.shai.xmodifier;

import junit.framework.TestCase;
import org.custommonkey.xmlunit.DetailedDiff;
import org.custommonkey.xmlunit.XMLUnit;
import org.junit.Assert;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2020/04/13
 */
public class XModifierTest extends TestCase {

    static {
        XMLUnit.setIgnoreAttributeOrder(true);
        XMLUnit.setIgnoreWhitespace(true);
        XMLUnit.setIgnoreComments(true);
        XMLUnit.setIgnoreDiffBetweenTextAndCDATA(true);
    }

    /*
     * test create elements, attributes, sub elements, text
     */
    @Test
    public void create() throws ParserConfigurationException, IOException, SAXException {
        Document document = createDocument();
        Document documentExpected = readDocument("createExpected.xml");
        XModifier modifier = new XModifier(document);
        modifier.setNamespace("ns", "http://localhost");
        // create an empty element
        modifier.addModify("/ns:root/ns:element1");
        // create an element with attribute
        modifier.addModify("/ns:root/ns:element2[@attr=1]");
        // append an new element to existing element1
        modifier.addModify("/ns:root/ns:element1/ns:element11");
        // create an element with text
        modifier.addModify("/ns:root/ns:element3", "TEXT");
        modifier.modify();
        assertXmlEquals(documentExpected, document);
    }

    @Test
    public void create2() throws ParserConfigurationException, IOException, SAXException {
        Document document = createDocument();
        Document documentExpected = readDocument("create2Expected.xml");
        XModifier modifier = new XModifier(document);
        modifier.setNamespace("ns", "http://localhost");
        // create an empty element
        modifier.addModify("/ns:root/ns:element1");
        // create an element with attribute
        modifier.addModify("/ns:root/ns:element2/@attr", "1");
        // append an new element to existing element1
        modifier.addModify("/ns:root/ns:element1/ns:element11");
        // create an element with text
        modifier.addModify("/ns:root/ns:element3", "TEXT");
        modifier.modify();
        assertXmlEquals(documentExpected, document);
    }

    @Test
    public void create3() throws ParserConfigurationException, IOException, SAXException {
        Document document = createDocument();
        Document documentExpected = readDocument("create3Expected.xml");
        XModifier modifier = new XModifier(document);
        modifier.setNamespace("ns", "http://localhost");
        // create an empty element
        modifier.addModify("/ns:root/ns:element1");
        // create an element with attribute
        modifier.addModify("/ns:root/ns:element2[@attr=1]/ns:element21[@attr=21]");
        // append an new element to existing element1
        modifier.addModify("/ns:root/ns:element1/ns:element11");
        // create an element with text
        modifier.addModify("/ns:root/ns:element3", "TEXT");
        modifier.modify();
        assertXmlEquals(documentExpected, document);
    }

    @Test
    public void modify1() throws ParserConfigurationException, IOException, SAXException {
        Document document = readDocument("modify.xml");
        Document documentExpected = readDocument("modifyExpected.xml");
        XModifier modifier = new XModifier(document);
        modifier.setNamespace("ns", "http://localhost");
        modifier.addModify("/ns:root/ns:element1[ns:element12]/ns:element13");
        modifier.modify();
        assertXmlEquals(documentExpected, document);
    }

    @Test
    public void modify2() throws ParserConfigurationException, IOException, SAXException {
        Document document = readDocument("PersonList.xml");
        Document documentExpected = readDocument("modify2Expected.xml");
        XModifier modifier = new XModifier(document);
        modifier.addModify("//PersonList/Person[2]/Name", "NewName");
        modifier.modify();
        assertXmlEquals(documentExpected, document);
    }

    @Test
    public void modify21() throws ParserConfigurationException, IOException, SAXException {
        Document document = readDocument("PersonList.xml");
        Document documentExpected = readDocument("modify2Expected.xml");
        XModifier modifier = new XModifier(document);
        modifier.addModify("//PersonList/Person[2]/Name/text()", "NewName");
        modifier.modify();
        assertXmlEquals(documentExpected, document);
    }

    @Test
    public void modify3() throws ParserConfigurationException, IOException, SAXException {
        Document document = readDocument("PersonList.xml");
        Document documentExpected = readDocument("modify3Expected.xml");
        XModifier modifier = new XModifier(document);
        modifier.addModify("//PersonList/Person[4]/Name", "NewName");
        modifier.modify();
        assertXmlEquals(documentExpected, document);
    }

    @Test
    public void modify31() throws ParserConfigurationException, IOException, SAXException {
        Document document = readDocument("PersonList.xml");
        Document documentExpected = readDocument("modify3Expected.xml");
        XModifier modifier = new XModifier(document);
        modifier.addModify("//PersonList/Person[4]/Name/text()", "NewName");
        modifier.modify();
        assertXmlEquals(documentExpected, document);
    }

    @Test
    public void modify4() throws ParserConfigurationException, IOException, SAXException {
        Document document = readDocument("PersonList.xml");
        Document documentExpected = readDocument("modify4Expected.xml");
        XModifier modifier = new XModifier(document);
        modifier.addModify("//PersonList/Person/Name", "NewName");
        modifier.modify();
        assertXmlEquals(documentExpected, document);
    }

    @Test
    public void delete1() throws ParserConfigurationException, IOException, SAXException {
        Document document = readDocument("PersonList.xml");
        Document documentExpected = readDocument("delete1Expected.xml");
        XModifier modifier = new XModifier(document);
        modifier.addModify("//PersonList/Person[1]/Name(:delete)");
        modifier.modify();
        assertXmlEquals(documentExpected, document);
    }

    @Test
    public void add1() throws ParserConfigurationException, IOException, SAXException {
        Document document = readDocument("PersonList.xml");
        Document documentExpected = readDocument("add1Expected.xml");
        XModifier modifier = new XModifier(document);
        modifier.addModify("//PersonList/Person(:add)/Name", "NewName");
        modifier.modify();
        assertXmlEquals(documentExpected, document);
    }

    @Test
    public void add3() throws ParserConfigurationException, IOException, SAXException {
        Document document = readDocument("PersonList.xml");
        Document documentExpected = readDocument("add3Expected.xml");
        XModifier modifier = new XModifier(document);
        modifier.addModify("//PersonList/Person(:insertBefore(Person[Name='Name2']))/Name", "NewName");
        modifier.modify();
        assertXmlEquals(documentExpected, document);
    }

    @Test
    public void test() throws IOException, SAXException, ParserConfigurationException {
        Document document = readDocument("test.xml");
        Document documentExpected = readDocument("testExpected.xml");
        XModifier modifier = new XModifier(document);
        modifier.setNamespace("ns", "http://localhost");
        modifier.addModify("/ns:root/ns:element1", "text1");
        modifier.addModify("/ns:root/ns:element4", "text4");
        modifier.addModify("//ns:element4/ns:element5[@value=100]", "text5");
        modifier.modify();
        assertXmlEquals(documentExpected, document);
    }

    private void assertXmlEquals(Document documentExpected, Document document) throws IOException, SAXException {
        String expect = writeXMLToString(documentExpected);
        String actual = writeXMLToString(document);
        DetailedDiff diff = new DetailedDiff(XMLUnit.compareXML(expect, actual));
        Assert.assertTrue(diff.getAllDifferences().toString(), diff.identical());
    }

    private Document createDocument() throws ParserConfigurationException {
        DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
        builderFactory.setNamespaceAware(true);
        DocumentBuilder documentBuilder = builderFactory.newDocumentBuilder();
        return documentBuilder.newDocument();
    }

    private Document readDocument(String name) throws ParserConfigurationException, IOException, SAXException {
        InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream(name);
        try {
            DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
            builderFactory.setNamespaceAware(true);
            DocumentBuilder documentBuilder = builderFactory.newDocumentBuilder();
            return documentBuilder.parse(in);
        } finally {
            if (in != null) {
                in.close();
            }
        }
    }

    private Document readDocument(InputStream in) throws ParserConfigurationException, IOException, SAXException {
        DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
        builderFactory.setNamespaceAware(true);
        DocumentBuilder documentBuilder = builderFactory.newDocumentBuilder();
        return documentBuilder.parse(in);
    }

    public static String formatXMLString(String xmlText) {
        String output;
        Source xmlInput = new StreamSource(new StringReader(xmlText));
        StringWriter stringWriter = new StringWriter();
        StreamResult xmlOutput = new StreamResult(stringWriter);
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer;
        try {
            transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            transformer.transform(xmlInput, xmlOutput);
            output = xmlOutput.getWriter().toString();
        } catch (Exception e) {
            throw new IllegalArgumentException(e);
        }
        return output;
    }

    public static String writeXMLToString(Node node) {
        if (node == null)
            return null;
        Transformer transformer;
        try {
            transformer = TransformerFactory.newInstance().newTransformer();
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            StreamResult result = new StreamResult(new StringWriter());
            DOMSource source = new DOMSource(node);
            transformer.transform(source, result);
            return formatXMLString(result.getWriter().toString());
        } catch (TransformerConfigurationException e) {
            throw new RuntimeException(e);
        } catch (TransformerFactoryConfigurationError e) {
            throw new RuntimeException(e);
        } catch (TransformerException e) {
            throw new RuntimeException(e);
        }
    }
}
