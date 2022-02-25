/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.oozie.ambari.view;

import java.io.IOException;
import java.io.StringReader;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

public class OozieUtils {
  private final static Logger LOGGER = LoggerFactory
    .getLogger(OozieUtils.class);
  private Utils utils = new Utils();

  public String generateConfigXml(Map<String, String> map) {
    DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
    DocumentBuilder db;
    try {
      db = dbf.newDocumentBuilder();
      Document doc = db.newDocument();
      Element configElement = doc.createElement("configuration");
      doc.appendChild(configElement);
      for (Map.Entry<String, String> entry : map.entrySet()) {
        Element propElement = doc.createElement("property");
        configElement.appendChild(propElement);
        Element nameElem = doc.createElement("name");
        nameElem.setTextContent(entry.getKey());
        Element valueElem = doc.createElement("value");
        valueElem.setTextContent(entry.getValue());
        propElement.appendChild(nameElem);
        propElement.appendChild(valueElem);
      }
      return utils.generateXml(doc);
    } catch (ParserConfigurationException e) {
      LOGGER.error("error in generating config xml", e);
      throw new RuntimeException(e);
    }
  }

  public String getJobPathPropertyKey(JobType jobType) {
    switch (jobType) {
      case WORKFLOW:
        return "oozie.wf.application.path";
      case COORDINATOR:
        return "oozie.coord.application.path";
      case BUNDLE:
        return "oozie.bundle.application.path";
    }
    throw new RuntimeException("Unknown Job Type");
  }

  public JobType deduceJobType(String xml) {
    try {
      DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
      DocumentBuilder db = null;

      db = dbf.newDocumentBuilder();
      InputSource is = new InputSource();
      is.setCharacterStream(new StringReader(xml));

      Document doc = db.parse(is);
      String rootNode = doc.getDocumentElement().getNodeName();
      if ("workflow-app".equals(rootNode)) {
        return JobType.WORKFLOW;
      } else if ("coordinator-app".equals(rootNode)) {
        return JobType.COORDINATOR;
      } else if ("bundle-app".equals(rootNode)) {
        return JobType.BUNDLE;
      }
      throw new RuntimeException("invalid xml submitted");
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public String deduceWorkflowNameFromJson(String json) {
    JsonElement jsonElement = new JsonParser().parse(json);
    String name = jsonElement.getAsJsonObject().get("name").getAsString();
    return name;
  }
  public String deduceWorkflowSchemaVersionFromJson(String json) {
    JsonElement jsonElement = new JsonParser().parse(json);
    return jsonElement.getAsJsonObject().get("xmlns").getAsString();
  }

  public String deduceWorkflowNameFromXml(String xml) {
    try {
      DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
      DocumentBuilder db = dbf.newDocumentBuilder();
      InputSource is = new InputSource();
      is.setCharacterStream(new StringReader(xml));
      Document doc = db.parse(is);
      String name = doc.getDocumentElement().getAttributeNode("name").getValue();
      return name;

    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public String generateWorkflowXml(String actionNodeXml) {
    DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
    DocumentBuilder db;
    try {
      db = dbf.newDocumentBuilder();
      Document doc = db.newDocument();

      Element workflowElement = doc.createElement("workflow-app");
      workflowElement.setAttribute("name", "testWorkflow");
      workflowElement.setAttribute("xmlns", "uri:oozie:workflow:0.5");
      doc.appendChild(workflowElement);

      Element startElement = doc.createElement("start");
      startElement.setAttribute("to", "testAction");
      workflowElement.appendChild(startElement);

      Element actionElement = doc.createElement("action");
      actionElement.setAttribute("name", "testAction");
      Element actionSettingsElement = db.parse(
        new InputSource(new StringReader(actionNodeXml)))
        .getDocumentElement();
      actionElement.appendChild(doc.importNode(actionSettingsElement,
        true));
      workflowElement.appendChild(actionElement);

      Element actionOkTransitionElement = doc.createElement("ok");
      actionOkTransitionElement.setAttribute("to", "end");
      actionElement.appendChild(actionOkTransitionElement);

      Element actionErrorTransitionElement = doc.createElement("error");
      actionErrorTransitionElement.setAttribute("to", "kill");
      actionElement.appendChild(actionErrorTransitionElement);

      Element killElement = doc.createElement("kill");
      killElement.setAttribute("name", "kill");
      Element killMessageElement = doc.createElement("message");
      killMessageElement.setTextContent("Kill node message");
      killElement.appendChild(killMessageElement);
      workflowElement.appendChild(killElement);

      Element endElement = doc.createElement("end");
      endElement.setAttribute("name", "end");
      workflowElement.appendChild(endElement);

      return utils.generateXml(doc);
    } catch (ParserConfigurationException | SAXException | IOException e) {
      LOGGER.error("error in generating workflow xml", e);
      throw new RuntimeException(e);
    }
  }

  public String getNoOpWorkflowXml(String json,JobType jobType) {
    String schema=deduceWorkflowSchemaVersionFromJson(json);
    String name=deduceWorkflowNameFromJson(json);
    switch (jobType){
      case WORKFLOW:
        return String.format("<workflow-app xmlns=\"%s\" name=\"%s\"><start to=\"end\"/><end name=\"end\"/></workflow-app>",schema,name);
      case COORDINATOR:
        return String.format("<coordinator-app xmlns=\"%s\" name=\"%s\"></coordinator-app>",schema,name);
      case BUNDLE:
        return String.format("<bundle-app xmlns=\"%s\" name=\"%s\"></bundle-app>",schema,name);
    }
    return null;
  }
}
