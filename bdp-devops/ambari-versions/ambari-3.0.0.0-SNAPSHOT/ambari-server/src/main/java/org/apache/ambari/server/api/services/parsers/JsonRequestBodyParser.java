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

package org.apache.ambari.server.api.services.parsers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.api.services.NamedPropertySet;
import org.apache.ambari.server.api.services.RequestBody;
import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * JSON parser which parses a JSON string into a map of properties and values.
 */
public class JsonRequestBodyParser implements RequestBodyParser {
  /**
   *  Logger instance.
   */
  private final static Logger LOG = LoggerFactory.getLogger(JsonRequestBodyParser.class);

  private final static ObjectMapper mapper = new ObjectMapper();

  @Override
  public Set<RequestBody> parse(String body) throws BodyParseException {

    Set<RequestBody> requestBodySet = new HashSet<>();
    RequestBody      rootBody       = new RequestBody();
    rootBody.setBody(body);

    if (body != null && body.length() != 0) {
      try {
        JsonNode root = mapper.readTree(ensureArrayFormat(body));

        Iterator<JsonNode> iterator = root.getElements();
        while (iterator.hasNext()) {
          JsonNode            node             = iterator.next();
          Map<String, Object> mapProperties    = new HashMap<>();
          Map<String, String> requestInfoProps = new HashMap<>();
          NamedPropertySet    propertySet      = new NamedPropertySet("", mapProperties);

          processNode(node, "", propertySet, requestInfoProps);

          if (!requestInfoProps.isEmpty()) {
            // If this node has request info properties then add it as a
            // separate request body
            RequestBody requestBody = new RequestBody();
            requestBody.setBody(body);

            for (Map.Entry<String, String> entry : requestInfoProps.entrySet()) {
              String key   = entry.getKey();
              String value = entry.getValue();

              requestBody.addRequestInfoProperty(key, value);

              if (key.equals(QUERY_FIELD_NAME)) {
                requestBody.setQueryString(value);
              }
            }
            if (!propertySet.getProperties().isEmpty()) {
              requestBody.addPropertySet(propertySet);
            }
            requestBodySet.add(requestBody);
          } else {
            // If this node does not have request info properties then add it
            // as a new property set to the root request body
            if (!propertySet.getProperties().isEmpty()) {
              rootBody.addPropertySet(propertySet);
            }
            requestBodySet.add(rootBody);
          }
        }
      } catch (IOException e) {
        if (LOG.isDebugEnabled()) {
          LOG.debug("Caught exception parsing msg body.");
          LOG.debug("Message Body: {}", body, e);
        }
        throw new BodyParseException(e);
      }
    }
    if (requestBodySet.isEmpty()) {
      requestBodySet.add(rootBody);
    }
    return requestBodySet;
  }

  private void processNode(JsonNode node, String path, NamedPropertySet propertySet,
                           Map<String, String> requestInfoProps) throws IOException {
    Iterator<String> iterator = node.getFieldNames();
    while (iterator.hasNext()) {
      String   name  = iterator.next();
      JsonNode child = node.get(name);
      if (child.isArray()) {
        //array
        Iterator<JsonNode>       arrayIter = child.getElements();
        Set<Map<String, Object>> arraySet  = new LinkedHashSet<>();
        List<String> primitives = new ArrayList<>();

        while (arrayIter.hasNext()) {
          JsonNode next = arrayIter.next();

          if (next.isValueNode()) {
            // All remain nodes will be also primitives
            primitives.add(next.asText());
          } else {
            NamedPropertySet arrayPropertySet = new NamedPropertySet(name, new HashMap<>());
            processNode(next, "", arrayPropertySet, requestInfoProps);
            arraySet.add(arrayPropertySet.getProperties());
          }
        }

        Object properties = primitives.isEmpty() ? arraySet : primitives;
        propertySet.getProperties().put(PropertyHelper.getPropertyId(path, name), properties);
      } else if (child.isContainerNode()) {
        // object
        if (name.equals(BODY_TITLE)) {
          name = "";
        }
        if (name.equals(REQUEST_BLOB_TITLE)) {
          propertySet.getProperties().put(PropertyHelper.getPropertyId(path,
            name), child.toString());
        } else {
          processNode(child, path.isEmpty() ? name : path + '/' + name, propertySet, requestInfoProps);
        }
      } else {
        // field
        String value = child.isNull() ? null : child.asText();

        if (path.equals(REQUEST_INFO_PATH)) {
           requestInfoProps.put(PropertyHelper.getPropertyId(null, name),
               value);
        } else if (path.startsWith(REQUEST_INFO_PATH)) {
          requestInfoProps.put(PropertyHelper.getPropertyId(
              path.substring(REQUEST_INFO_PATH.length() + SLASH.length()), name),
              value);
        } else {
          propertySet.getProperties().put(PropertyHelper.getPropertyId(
              path.equals(BODY_TITLE) ? "" : path, name), value);
        }
      }
    }
  }

  private String ensureArrayFormat(String s) {
    return s.startsWith("[") ? s : '[' + s + ']';
  }
}
