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
package com.yushu.tis.xmodifier;

import com.qlangtech.tis.runtime.module.action.IModifierProcess;
import org.jdom2.Content;
import org.jdom2.DocType;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.filter.ElementFilter;
import org.jdom2.filter.Filters;
import org.jdom2.input.SAXBuilder;
import org.jdom2.input.sax.XMLReaderSAX2Factory;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.jdom2.xpath.XPathExpression;
import org.jdom2.xpath.XPathFactory;
import org.shai.xmodifier.XModifyNode;
import org.shai.xmodifier.exception.XModifyFailException;
import org.shai.xmodifier.util.ArrayUtils;
import org.shai.xmodifier.util.StringUtils;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.xpath.XPathExpressionException;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 基于jdom2的xml modifer
 *
 * @author 百岁（baisui@qlangtech.com）
 * @date 2015年1月17日下午9:21:58
 */
public class XModifier {

    private static final SAXBuilder saxBuilder = new SAXBuilder(new XMLReaderSAX2Factory(false));
    private static final Format xmlPrettyformat = Format.getPrettyFormat().setEncoding(StandardCharsets.UTF_8.name());

    private static final Format xmlRawformat = Format.getRawFormat().setEncoding(StandardCharsets.UTF_8.name());

    static {
        XModifier.saxBuilder.setEntityResolver(new EntityResolver() {

            public InputSource resolveEntity(String publicId, String systemId) throws SAXException, IOException {
                InputSource source = new InputSource();
                source.setCharacterStream(new StringReader(""));
                return source;
            }
        });
    }

    private final Document document;

    private Map<String, String> nsMap = new HashMap<String, String>();

    private List<XModifyNode> xModifyNodes = new ArrayList<XModifyNode>();

    public XModifier(Document document) {
        this.document = document;
    }

    public static byte[] modifySchemaContent(byte[] content, IModifierProcess contentModifier) throws Exception {
        ByteArrayInputStream inputStream = new ByteArrayInputStream(content);
        Document document2 = saxBuilder.build(inputStream);
        final XModifier modifier = new XModifier(document2);

        contentModifier.process(document2, modifier);
        modifier.modify();
        XMLOutputter xmlout = new XMLOutputter(xmlPrettyformat);
        xmlout.setFormat(xmlPrettyformat.setEncoding(StandardCharsets.UTF_8.name()));
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            try (OutputStreamWriter writer = new OutputStreamWriter(out, StandardCharsets.UTF_8)) {
                xmlout.output(document2, writer);
                writer.flush();
                return out.toByteArray();
            }
        }
    }

    public void setNamespace(String prefix, String url) {
        nsMap.put(prefix, url);
    }

    public void deleteUniqueKey() {
        this.addModify("/uniqueKey(:delete)");
        // this.addModify("/defaultSearchField(:delete)");
    }

    public void deleteSharedKey() {
        this.addModify("/sharedKey(:delete)");
    }

    // public void deleteDefaultSearchField() {
    // //        this.addModify("/defaultSearchField(:delete)");
    // //    }
    public void addModify(String xPath, String value) {
        xModifyNodes.add(new XModifyNode(nsMap, xPath, value));
    }

    public void addModify(String xPath) {
        xModifyNodes.add(new XModifyNode(nsMap, xPath, null));
    }

    private void modify() {
        initXPath();
        for (XModifyNode xModifyNode : xModifyNodes) {
            try {
                create(document.getRootElement(), xModifyNode);
            } catch (Exception e) {
                throw new XModifyFailException(xModifyNode.toString(), e);
            }
        }
    }

    public static void main(String[] arg) throws Exception {
        // StringReader read = new StringReader(schema);
        // InputSource source = new InputSource(read);
        SAXBuilder builder = new SAXBuilder(new XMLReaderSAX2Factory(false));
        // builder.setEntityResolver(entityResolver);
        builder.setEntityResolver(new EntityResolver() {

            public InputSource resolveEntity(String publicId, String systemId) throws SAXException, IOException {
                InputSource source = new InputSource();
                source.setCharacterStream(new StringReader(""));
                return source;
            }
        });
        InputStream inputStream = new FileInputStream(new File("D:\\home\\schema.xml"));
        Document document = builder.build(inputStream);
        XModifier modifier = new XModifier(document);
        modifier.addModify("/types/fieldType[@name='singleString']/@class", "java.lang.String");
        modifier.addModify("/types/fieldType[@name='kkkkkkkk']/@class", "ddddddd");
        modifier.addModify("/types/fieldType[@name='xxxxxx'][@class='java.lang.String']");
        modifier.addModify("/uniqueKey/text()", "ddddddd");
        modifier.addModify("/fields/field[@name='email_dynamic_info'](:delete)");
        modifier.modify();
        // 设置xml文件格式---选用这种格式可以使生成的xml文件自动换行同时缩进
        Format format = Format.getPrettyFormat();
        // ▲▲▲▲▲▲************************ 构建schema文件格式
        // 将生成的元素加入文档：根元素
        // 添加docType属性
        DocType docType = new DocType("schema", "http://tis.qlangtech.com:9999/dtd/solrschema.dtd");
        document.setDocType(docType);
        XMLOutputter xmlout = new XMLOutputter(format);
        // 设置xml内容编码
        xmlout.setFormat(format.setEncoding("utf8"));
        ByteArrayOutputStream byteRsp = new ByteArrayOutputStream();
        xmlout.output(document, byteRsp);
        System.out.println(byteRsp.toString("utf8"));
    }

    private void create(Element parent, XModifyNode node) throws XPathExpressionException {
        Element newNode;
        if (node.isAttributeModifier()) {
            // attribute
            createAttributeByXPath(parent, node.getCurNode().substring(1), node.getValue());
        } else {
            // element
            if (node.isRootNode()) {
                // root node
                newNode = parent;
                boolean canMoveToNext = node.moveNext();
                if (!canMoveToNext) {
                    // last node
                    newNode.setText(node.getValue());
                } else {
                    // next node
                    create(newNode, node);
                }
            } else if (node.getCurNode().equals("text()")) {
                parent.setText(node.getValue());
            } else {
                // element
                findOrCreateElement(parent, node);
            }
        }
    }

    private XPathFactory factory;

    private void initXPath() {
        factory = XPathFactory.instance();
        // XPath xPath = factory.
        // xPath.setNamespaceContext(new NamespaceContext() {
        // @Override
        // public String getNamespaceURI(String prefix) {
        // return nsMap.get(prefix);
        // }
        //
        // @Override
        // public String getPrefix(String namespaceURI) {
        // for (Map.Entry<String, String> entry : nsMap.entrySet()) {
        // if (entry.getValue().equals(namespaceURI)) {
        // return entry.getKey();
        // }
        // }
        // return null;
        // }
        //
        // @Override
        // public Iterator getPrefixes(String namespaceURI) {
        // return nsMap.keySet().iterator();
        // }
        // });
        // this.xPathEvaluator = xPath;
    }

    private void createAttributeByXPath(Content node, String current, String value) {
        ((Element) node).setAttribute(current, value);
    }

    private void findOrCreateElement(Element parent, XModifyNode node) throws XPathExpressionException {
        if (node.isAdding()) {
            // create new element without double check
            Element newCreatedNode = createNewElement(node.getNamespaceURI(), node.getLocalName(), node.getConditions());
            parent.addContent(newCreatedNode);
            boolean canMoveToNext = node.moveNext();
            if (!canMoveToNext) {
                // last node
                newCreatedNode.setText(node.getValue());
            } else {
                // next node
                create(newCreatedNode, node);
            }
            return;
        }
        if (node.isInsertBefore()) {
            // create new element without double check
            // Element newCreatedNode = createNewElement(node.getNamespaceURI(),
            // node.getLocalName(), node.getConditions());
            //
            // XPathExpression<Element> xpath = factory.compile(
            // node.getInsertBeforeXPath(), new ElementFilter());
            //
            // Element referNode = xpath.evaluateFirst(parent);
            //
            // parent.getParentElement().addContent(child)
            //
            // parent.insertBefore(newCreatedNode, referNode);
            // boolean canMoveToNext = node.moveNext();
            // if (!canMoveToNext) {
            // // last node
            // newCreatedNode.setTextContent(node.getValue());
            // } else {
            // // next node
            // create(newCreatedNode, node);
            // }
            // return;
        }
        XPathExpression<Element> xpath = factory.compile(node.getCurNodeXPath(), Filters.element());
        List<Element> existNodeList = xpath.evaluate(parent);
        // node.getCurNodeXPath(), parent, XPathConstants.NODESET);
        if (existNodeList.size() > 0) {
            for (int i = 0; i < existNodeList.size(); i++) {
                XModifyNode newNode = node.duplicate();
                Element item = existNodeList.get(i);
                if (node.isDeleting()) {
                    parent.removeContent(item);
                    continue;
                }
                boolean canMoveToNext = newNode.moveNext();
                if (!canMoveToNext) {
                    // last node
                    item.setText(node.getValue());
                } else {
                    // next node
                    create(item, newNode);
                }
            }
        } else {
            Element newCreatedNode = createNewElement(node.getNamespaceURI(), node.getLocalName(), node.getConditions());
            parent.addContent(newCreatedNode);
            // Node checkExistNode = (Node) xPathEvaluator.evaluate(
            // node.getCurNodeXPath(), parent, XPathConstants.NODE);
            xpath = factory.compile(node.getCurNodeXPath(), new ElementFilter());
            Element checkExistNode = xpath.evaluateFirst(parent);
            if (!newCreatedNode.equals(checkExistNode)) {
                throw new XModifyFailException("Error to create " + node.getCurNode());
            }
            boolean canMoveToNext = node.moveNext();
            if (!canMoveToNext) {
                // last node
                newCreatedNode.setText(node.getValue());
            } else {
                // next node
                create(newCreatedNode, node);
            }
        }
    }

    private Element createNewElement(String namespaceURI, String local, String[] conditions) throws XPathExpressionException {
        Element newElement = null;
        if (namespaceURI != null) {
            // document.createElementNS(namespaceURI,
            newElement = new Element(local, namespaceURI);
            // local);
        } else {
            newElement = new Element(local);
        }
        if (ArrayUtils.isNotEmpty(conditions)) {
            for (String condition : conditions) {
                if (StringUtils.containsOnly(condition, "0123456789")) {
                    continue;
                }
                // TODO: support not( ) function, need to refactory
                if (condition.startsWith("not")) {
                    continue;
                }
                String[] strings = StringUtils.splitBySeparator(condition, '=');
                String xpath = strings[0];
                String value = StringUtils.unquote(strings[1]);
                create(newElement, new XModifyNode(nsMap, xpath, value));
            }
        }
        return newElement;
    }
}
