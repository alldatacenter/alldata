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

package org.apache.ambari.server.api.services.serializers;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.ambari.server.api.services.DeleteResultMetadata;
import org.apache.ambari.server.api.services.Result;
import org.apache.ambari.server.api.services.ResultMetadata;
import org.apache.ambari.server.api.services.ResultStatus;
import org.apache.ambari.server.api.util.TreeNode;
import org.apache.ambari.server.api.util.TreeNodeImpl;
import org.apache.ambari.server.controller.internal.OperationStatusMetaData;
import org.apache.ambari.server.controller.spi.Resource;
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.util.DefaultPrettyPrinter;

/**
 * JSON serializer.
 * Responsible for representing a result as JSON.
 */
public class JsonSerializer implements ResultSerializer {

  /**
   * Factory used to create JSON generator.
   */
  JsonFactory m_factory = new JsonFactory();

  ObjectMapper m_mapper = new ObjectMapper(m_factory);

  /**
   * Generator which writes JSON.
   */
  JsonGenerator m_generator;


  @Override
  public Object serialize(Result result) {
    try {
      ByteArrayOutputStream bytesOut = init();

      if (result.getStatus().isErrorState()) {
        return serializeError(result.getStatus());
      }

      TreeNode<Resource> treeNode = result.getResultTree();
      processNode(treeNode);
      processResultMetadata(result.getResultMetadata());
      m_generator.close();
      return bytesOut.toString("UTF-8");
    } catch (IOException e) {
      //todo: exception handling.  Create ResultStatus 500 and call serializeError
      throw new RuntimeException("Unable to serialize to json: " + e, e);
    }
  }

  @Override
  public Object serializeError(ResultStatus error) {
    try {
      ByteArrayOutputStream bytesOut = init();
      //m_mapper.writeValue(m_generator, error);
      m_generator.writeStartObject();
      m_generator.writeNumberField("status", error.getStatus().getStatus());
      m_generator.writeStringField("message", error.getMessage());
      m_generator.writeEndObject();
      m_generator.close();
      return bytesOut.toString("UTF-8");

    } catch (IOException e) {
      //todo: exception handling
      throw new RuntimeException("Unable to serialize to json: " + e, e);
    }
  }

  private ByteArrayOutputStream init() throws IOException {
    ByteArrayOutputStream bytesOut = new ByteArrayOutputStream();
    m_generator = createJsonGenerator(bytesOut);

    DefaultPrettyPrinter p = new DefaultPrettyPrinter();
    p.indentArraysWith(new DefaultPrettyPrinter.Lf2SpacesIndenter());
    m_generator.setPrettyPrinter(p);

    return bytesOut;
  }

  private void processResultMetadata(ResultMetadata resultMetadata) throws IOException {
    if (resultMetadata == null) {
      return;
    }

    if (resultMetadata.getClass() == DeleteResultMetadata.class) {
      processResultMetadata((DeleteResultMetadata) resultMetadata);
    } else if (resultMetadata.getClass() == OperationStatusMetaData.class) {
      processResultMetadata((OperationStatusMetaData) resultMetadata);
    } else {
      throw new IllegalArgumentException("ResultDetails is not of type DeleteResultDetails, cannot parse");
    }
  }

  private void processResultMetadata(DeleteResultMetadata deleteResultMetadata) throws IOException {
    m_generator.writeStartObject();
    m_generator.writeArrayFieldStart("deleteResult");
    //write successfully deleted keys
    for (String key : deleteResultMetadata.getDeletedKeys()) {
      m_generator.writeStartObject();
      m_generator.writeObjectFieldStart("deleted");
      m_generator.writeStringField("key", key);
      m_generator.writeEndObject();
      m_generator.writeEndObject();
    }

    //write exceptions
    for (Map.Entry<String, ResultStatus> entry : deleteResultMetadata.getExcptions().entrySet()) {
      ResultStatus resultStatus = entry.getValue();
      m_generator.writeStartObject();
      m_generator.writeObjectFieldStart("error");
      m_generator.writeStringField("key", entry.getKey());
      m_generator.writeNumberField("code", resultStatus.getStatusCode());
      m_generator.writeStringField("message", resultStatus.getMessage());
      m_generator.writeEndObject();
      m_generator.writeEndObject();
    }
    m_generator.writeEndArray();
    m_generator.writeEndObject();
  }

  private void processResultMetadata(OperationStatusMetaData metaData) throws IOException {
    m_generator.writeStartObject();
    m_generator.writeObjectFieldStart("operationResults");

    for (OperationStatusMetaData.Result result : metaData.getResults()) {
      m_generator.writeObjectFieldStart(result.getId());
      m_generator.writeStringField("status", result.isSuccess() ? "success" : "error");

      if (result.getMessage() != null) {
        m_generator.writeStringField("message", result.getMessage());
      }

      if (result.getResponse() != null) {
        m_generator.writeFieldName("response");
        m_mapper.writeValue(m_generator, result.getResponse());
      }

      m_generator.writeEndObject();
    }

    m_generator.writeEndObject();
    m_generator.writeEndObject();
  }

  private void processNode(TreeNode<Resource> node) throws IOException {
    if (isObject(node)) {
      m_generator.writeStartObject();

      writeHref(node);
      writeItemCount(node);

      Resource r = node.getObject();
      if (r != null) {
        handleResourceProperties(getTreeProperties(r.getPropertiesMap()));
      }
    }

    if (isArray(node)) {
      if (node.getName() != null) {
        m_generator.writeArrayFieldStart(node.getName());
      } else {
        m_generator.writeStartArray();
      }
    }

    for (TreeNode<Resource> child : node.getChildren()) {
      processNode(child);
    }

    if (isArray(node)) {
      m_generator.writeEndArray();
    }

    if (isObject(node)) {
      m_generator.writeEndObject();
    }
  }

  // Determines whether or not the given node is an object
  private boolean isObject(TreeNode<Resource> node) {
    return node.getObject() != null ||
        ((node.getName() != null) && ((node.getParent() == null) || !isObject(node.getParent())));
  }

  // Determines whether or not the given node is an array
  private boolean isArray(TreeNode<Resource> node) {
    return (node.getObject() == null && node.getName() != null) ||
        (node.getObject() == null && node.getName() == null &&
            node.getChildren().size() > 1);
  }

  private TreeNode<Map<String, Object>> getTreeProperties(Map<String, Map<String, Object>> propertiesMap) {
    TreeNode<Map<String, Object>> treeProperties = new TreeNodeImpl<>(null, new LinkedHashMap<>(), null);

    for (Map.Entry<String, Map<String, Object>> entry : propertiesMap.entrySet()) {
      String category = entry.getKey();
      TreeNode<Map<String, Object>> node;
      if (category == null || category.isEmpty()) {
        node = treeProperties;
      } else {
        node = treeProperties.getChild(category);
        if (node == null) {
          String[] tokens = category.split("/");
          node = treeProperties;
          for (String t : tokens) {
            TreeNode<Map<String, Object>> child = node.getChild(t);
            if (child == null) {
              child = node.addChild(new LinkedHashMap<>(), t);
            }
            node = child;
          }
        }
      }

      Map<String, Object> properties = entry.getValue();

      for (Map.Entry<String, Object> propertyEntry : properties.entrySet()) {
        node.getObject().put(propertyEntry.getKey(), propertyEntry.getValue());
      }
    }
    return treeProperties;
  }

  private void handleResourceProperties(TreeNode<Map<String, Object>> node) throws IOException {
    String category = node.getName();

    if (category != null) {
      m_generator.writeFieldName(category);
      m_generator.writeStartObject();
    }

    for (Map.Entry<String, Object> entry : node.getObject().entrySet()) {
      m_generator.writeFieldName(entry.getKey());
      m_mapper.writeValue(m_generator, entry.getValue());
    }

    for (TreeNode<Map<String, Object>> n : node.getChildren()) {
      handleResourceProperties(n);
    }

    if (category != null) {
      m_generator.writeEndObject();
    }
  }

  private JsonGenerator createJsonGenerator(ByteArrayOutputStream baos) throws IOException {
    JsonGenerator generator = m_factory.createJsonGenerator(new OutputStreamWriter(baos,
        Charset.forName("UTF-8").newEncoder()));

    DefaultPrettyPrinter p = new DefaultPrettyPrinter();
    p.indentArraysWith(new DefaultPrettyPrinter.Lf2SpacesIndenter());
    generator.setPrettyPrinter(p);

    return generator;
  }

  private void writeHref(TreeNode<Resource> node) throws IOException {
    String hrefProp = node.getStringProperty("href");
    if (hrefProp != null) {
      m_generator.writeStringField("href", hrefProp);
    }
  }

  private void writeItemCount(TreeNode<Resource> node) throws IOException {
    String countProp = node.getStringProperty("count");
    if (countProp != null) {
      m_generator.writeStringField("itemTotal", countProp);
      // Write once
      node.setProperty("count", null);
    }
  }
}
