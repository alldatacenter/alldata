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
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.netease.arctic.ams.server.utils;

import com.alibaba.fastjson.JSONObject;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class XmlUtils {

  public static String encodeXml(Map<String, ?> data) {
    org.dom4j.Document document = DocumentHelper.createDocument();
    document.setXMLEncoding("UTF-8");
    for (Map.Entry<String, ?> entry : data.entrySet()) {
      Element element = document.addElement(entry.getKey());
      element.setData(entry.getValue());
    }
    return document.asXML();
  }

  public static Map<String, Object> decodeXml(String xmlData) throws DocumentException {
    Map<String, Object> rs = new HashMap<>();
    System.out.println("xmldata" + xmlData);
    org.dom4j.Document document = DocumentHelper.parseText(xmlData);
    Element rootElement = document.getRootElement();
    for (Iterator i = rootElement.elementIterator(); i.hasNext(); ) {
      Element next = (Element) i.next();
      rs.put(next.getName(), next.getData());
    }
    System.out.println("result" + JSONObject.toJSON(rs));
    return rs;
  }

  public static Map<String, String> decodeXmlFromFile(String filePath)
      throws ParserConfigurationException, IOException, SAXException {
    Map<String, String> result = new HashMap<>();
    File f = new File(filePath);
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    DocumentBuilder builder = factory.newDocumentBuilder();
    Document doc = builder.parse(f);
    NodeList nl = doc.getElementsByTagName("name");
    for (int i = 0; i < nl.getLength(); i++) {
      String name = doc.getElementsByTagName("name").item(i).getFirstChild().getNodeValue();
      String value = doc.getElementsByTagName("value").item(i).getFirstChild().getNodeValue();
      result.put(name, value);
    }
    return result;
  }
}
