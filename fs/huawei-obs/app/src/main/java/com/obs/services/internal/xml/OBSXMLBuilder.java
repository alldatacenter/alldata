/**
 * Copyright 2019 Huawei Technologies Co.,Ltd.
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use
 * this file except in compliance with the License.  You may obtain a copy of the
 * License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed
 * under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations under the License.
 */

package com.obs.services.internal.xml;

import com.jamesmurty.utils.BaseXMLBuilder;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.namespace.NamespaceContext;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;
import java.io.IOException;
import java.security.AccessController;
import java.security.PrivilegedAction;

public class OBSXMLBuilder extends BaseXMLBuilder {

    private static final String DEFAULT_PACKAGE = "com.sun.org.apache.xerces.internal";

    private static String xmlDocumentBuilderFactoryClass = 
            "com.sun.org.apache.xerces.internal.jaxp.DocumentBuilderFactoryImpl";
    
    public static void setXmlDocumentBuilderFactoryClass(String className) {
        if (null != className 
                && !className.trim().equals("")) {
            xmlDocumentBuilderFactoryClass = className;
        }
    }
    
    protected OBSXMLBuilder(Document xmlDocument) {
        super(xmlDocument);
    }

    protected OBSXMLBuilder(Node myNode, Node parentNode) {
        super(myNode, parentNode);
    }

    private static DocumentBuilderFactory findDocumentBuilderFactory() {
        if (xmlDocumentBuilderFactoryClass != null && xmlDocumentBuilderFactoryClass.startsWith(DEFAULT_PACKAGE)) {
            return DocumentBuilderFactory.newInstance();
        }

        return newInstance(DocumentBuilderFactory.class, xmlDocumentBuilderFactoryClass, null, true, false);
    }

    protected static Document createDocumentImpl(String name, String namespaceURI, boolean enableExternalEntities,
            boolean isNamespaceAware) throws ParserConfigurationException, FactoryConfigurationError {
        DocumentBuilderFactory factory = OBSXMLBuilder.findDocumentBuilderFactory();
        factory.setNamespaceAware(isNamespaceAware);
        enableOrDisableExternalEntityParsing(factory, enableExternalEntities);
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document document = builder.newDocument();
        Element rootElement = null;
        if (namespaceURI != null && namespaceURI.length() > 0) {
            rootElement = document.createElementNS(namespaceURI, name);
        } else {
            rootElement = document.createElement(name);
        }
        document.appendChild(rootElement);
        return document;
    }

    protected static Document parseDocumentImpl(InputSource inputSource, boolean enableExternalEntities,
            boolean isNamespaceAware) throws ParserConfigurationException, SAXException, IOException {
        DocumentBuilderFactory factory = OBSXMLBuilder.findDocumentBuilderFactory();
        factory.setNamespaceAware(isNamespaceAware);
        enableOrDisableExternalEntityParsing(factory, enableExternalEntities);
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document document = builder.parse(inputSource);
        return document;
    }

    private static ClassLoader getContextClassLoader() throws SecurityException {
        return (ClassLoader) AccessController.doPrivileged(new PrivilegedAction<Object>() {
            public Object run() {
                ClassLoader cl = null;
                cl = Thread.currentThread().getContextClassLoader();

                if (cl == null) {
                    cl = ClassLoader.getSystemClassLoader();
                }
                return cl;
            }
        });
    }

    private static Class<?> getProviderClass(String className, ClassLoader cl, boolean doFallback,
            boolean useBSClsLoader) throws ClassNotFoundException {
        try {
            if (cl == null) {
                if (useBSClsLoader) {
                    cl = OBSXMLBuilder.class.getClassLoader();
                } else {
                    cl = getContextClassLoader();
                    if (cl == null) {
                        throw new ClassNotFoundException();
                    }
                }
            }

            return Class.forName(className, false, cl);
        } catch (ClassNotFoundException e1) {
            if (doFallback) {
                return Class.forName(className, false, OBSXMLBuilder.class.getClassLoader());
            } else {
                throw e1;
            }
        }
    }

    private static <T> T newInstance(Class<T> type, String className, ClassLoader cl, boolean doFallback,
            boolean useBSClsLoader) throws FactoryConfigurationError {
        if (System.getSecurityManager() != null) {
            if (className != null && className.startsWith(DEFAULT_PACKAGE)) {
                cl = null;
                useBSClsLoader = true;
            }
        }

        try {
            Class<?> providerClass = getProviderClass(className, cl, doFallback, useBSClsLoader);
            if (!type.isAssignableFrom(providerClass)) {
                throw new ClassCastException(className + " cannot be cast to " + type.getName());
            }
            Object instance = providerClass.newInstance();
            return type.cast(instance);
        } catch (ClassNotFoundException x) {
            throw new FactoryConfigurationError(x, "Provider " + className + " not found");
        } catch (Exception x) {
            throw new FactoryConfigurationError(x, "Provider " + className + " could not be instantiated: " + x);
        }
    }

    public static OBSXMLBuilder create(String name) throws ParserConfigurationException, FactoryConfigurationError {
        return new OBSXMLBuilder(createDocumentImpl(name, null, false, true));
    }

    public static OBSXMLBuilder parse(InputSource inputSource, boolean enableExternalEntities, boolean isNamespaceAware)
            throws ParserConfigurationException, SAXException, IOException {
        return new OBSXMLBuilder(parseDocumentImpl(inputSource, enableExternalEntities, isNamespaceAware));
    }

    public static OBSXMLBuilder parse(InputSource inputSource)
            throws ParserConfigurationException, SAXException, IOException {
        return OBSXMLBuilder.parse(inputSource, false, true);
    }

    @Override
    public OBSXMLBuilder stripWhitespaceOnlyTextNodes() throws XPathExpressionException {
        super.stripWhitespaceOnlyTextNodesImpl();
        return this;
    }

    @Override
    public OBSXMLBuilder importXMLBuilder(BaseXMLBuilder builder) {
        super.importXMLBuilderImpl(builder);
        return this;
    }

    @Override
    public OBSXMLBuilder root() {
        return new OBSXMLBuilder(getDocument());
    }

    @Override
    public OBSXMLBuilder xpathFind(String xpath, NamespaceContext nsContext) throws XPathExpressionException {
        Node foundNode = super.xpathFindImpl(xpath, nsContext);
        return new OBSXMLBuilder(foundNode, null);
    }

    @Override
    public OBSXMLBuilder xpathFind(String xpath) throws XPathExpressionException {
        return xpathFind(xpath, null);
    }

    @Override
    public OBSXMLBuilder element(String name) {
        String namespaceURI = super.lookupNamespaceURIImpl(name);
        return element(name, namespaceURI);
    }

    @Override
    public OBSXMLBuilder elem(String name) {
        return element(name);
    }

    @Override
    public OBSXMLBuilder e(String name) {
        return element(name);
    }

    @Override
    public OBSXMLBuilder element(String name, String namespaceURI) {
        Element elem = super.elementImpl(name, namespaceURI);
        return new OBSXMLBuilder(elem, this.getElement());
    }

    @Override
    public OBSXMLBuilder elementBefore(String name) {
        Element newElement = super.elementBeforeImpl(name);
        return new OBSXMLBuilder(newElement, null);
    }

    @Override
    public OBSXMLBuilder elementBefore(String name, String namespaceURI) {
        Element newElement = super.elementBeforeImpl(name, namespaceURI);
        return new OBSXMLBuilder(newElement, null);
    }

    @Override
    public OBSXMLBuilder attribute(String name, String value) {
        super.attributeImpl(name, value);
        return this;
    }

    @Override
    public OBSXMLBuilder attr(String name, String value) {
        return attribute(name, value);
    }

    @Override
    public OBSXMLBuilder a(String name, String value) {
        return attribute(name, value);
    }

    @Override
    public OBSXMLBuilder text(String value, boolean replaceText) {
        super.textImpl(value, replaceText);
        return this;
    }

    @Override
    public OBSXMLBuilder text(String value) {
        return this.text(value, false);
    }

    @Override
    public OBSXMLBuilder t(String value) {
        return text(value);
    }

    @Override
    public OBSXMLBuilder cdata(String data) {
        super.cdataImpl(data);
        return this;
    }

    @Override
    public OBSXMLBuilder data(String data) {
        return cdata(data);
    }

    @Override
    public OBSXMLBuilder d(String data) {
        return cdata(data);
    }

    @Override
    public OBSXMLBuilder cdata(byte[] data) {
        super.cdataImpl(data);
        return this;
    }

    @Override
    public OBSXMLBuilder data(byte[] data) {
        return cdata(data);
    }

    @Override
    public OBSXMLBuilder d(byte[] data) {
        return cdata(data);
    }

    @Override
    public OBSXMLBuilder comment(String comment) {
        super.commentImpl(comment);
        return this;
    }

    @Override
    public OBSXMLBuilder cmnt(String comment) {
        return comment(comment);
    }

    @Override
    public OBSXMLBuilder c(String comment) {
        return comment(comment);
    }

    @Override
    public OBSXMLBuilder instruction(String target, String data) {
        super.instructionImpl(target, data);
        return this;
    }

    @Override
    public OBSXMLBuilder inst(String target, String data) {
        return instruction(target, data);
    }

    @Override
    public OBSXMLBuilder i(String target, String data) {
        return instruction(target, data);
    }

    @Override
    public OBSXMLBuilder insertInstruction(String target, String data) {
        super.insertInstructionImpl(target, data);
        return this;
    }

    @Override
    public OBSXMLBuilder reference(String name) {
        super.referenceImpl(name);
        return this;
    }

    @Override
    public OBSXMLBuilder ref(String name) {
        return reference(name);
    }

    @Override
    public OBSXMLBuilder r(String name) {
        return reference(name);
    }

    @Override
    public OBSXMLBuilder namespace(String prefix, String namespaceURI) {
        super.namespaceImpl(prefix, namespaceURI);
        return this;
    }

    @Override
    public OBSXMLBuilder ns(String prefix, String namespaceURI) {
        return attribute(prefix, namespaceURI);
    }

    @Override
    public OBSXMLBuilder namespace(String namespaceURI) {
        this.namespace(null, namespaceURI);
        return this;
    }

    @Override
    public OBSXMLBuilder ns(String namespaceURI) {
        return namespace(namespaceURI);
    }

    @Override
    public OBSXMLBuilder up(int steps) {
        Node currNode = super.upImpl(steps);
        if (currNode instanceof Document) {
            return new OBSXMLBuilder((Document) currNode);
        } else {
            return new OBSXMLBuilder(currNode, null);
        }
    }

    @Override
    public OBSXMLBuilder up() {
        return up(1);
    }

    @Override
    public OBSXMLBuilder document() {
        return new OBSXMLBuilder(getDocument(), null);
    }

    @Override
    public int hashCode() throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean equals(Object obj) {
        return obj == this;
    }

}
