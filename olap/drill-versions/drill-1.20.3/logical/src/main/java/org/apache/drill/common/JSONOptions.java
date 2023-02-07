/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.drill.common;

import java.io.IOException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import org.apache.drill.common.JSONOptions.De;
import org.apache.drill.common.JSONOptions.Se;
import org.apache.drill.common.config.LogicalPlanPersistence;
import org.apache.drill.common.exceptions.LogicalPlanParsingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonLocation;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.node.TreeTraversingParser;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import org.apache.drill.shaded.guava.com.google.common.base.Preconditions;

@JsonSerialize(using = Se.class)
@JsonDeserialize(using = De.class)
public class JSONOptions {

  private final static Logger logger = LoggerFactory.getLogger(JSONOptions.class);

  private JsonNode root;
  private JsonLocation location;
  private Object opaque;

  public JSONOptions(Object opaque) {
    this.opaque = opaque;
  }

  public JSONOptions(JsonNode n, JsonLocation location) {
    this.root = n;
    this.location = location;
  }

  @SuppressWarnings("unchecked")
  public <T> T getWith(ObjectMapper mapper, Class<T> c) {
    try {
      if (opaque != null) {
        final Class<?> opaqueClass = opaque.getClass();
        if (opaqueClass.equals(c)) {
          return (T) opaque;
        } else {
          // Enum values that override methods are given $1, $2 ... extensions. Ignore the extension.
          // e.g. SystemTable$1 for SystemTable.OPTION
          if (c.isEnum()) {
            final String opaqueName = opaqueClass.getName().replaceAll("\\$\\d+$", "");
            final String cName = c.getName();
            if(opaqueName.equals(cName)) {
              return (T) opaque;
            }
          }
          throw new IllegalArgumentException(String.format("Attempted to retrieve a option with type of %s.  " +
            "However, the JSON options carried an opaque value of type %s.", c.getName(), opaqueClass.getName()));
        }
      }

      //logger.debug("Read tree {}", root);
      return mapper.treeToValue(root, c);
    } catch (JsonProcessingException e) {
      throw new LogicalPlanParsingException(String.format("Failure while trying to convert late bound " +
        "json options to type of %s. Reference was originally located at line %d, column %d.",
        c.getCanonicalName(), location.getLineNr(), location.getColumnNr()), e);
    }
  }

  public <T> T getListWith(LogicalPlanPersistence config, TypeReference<T> t) throws IOException {
    return getListWith(config.getMapper(), t);
  }

  public JsonNode asNode(){
    Preconditions.checkArgument(this.root != null, "Attempted to grab JSONOptions as JsonNode when no root node was stored.  You can only convert non-opaque JSONOptions values to JsonNodes.");
    return root;
  }

  public JsonParser asParser(){
    Preconditions.checkArgument(this.root != null, "Attempted to grab JSONOptions as Parser when no root node was stored.  You can only convert non-opaque JSONOptions values to parsers.");
    return new TreeTraversingParser(root);
  }

  @SuppressWarnings("unchecked")
  public <T> T getListWith(ObjectMapper mapper, TypeReference<T> t) throws IOException {
    if (opaque != null) {
      Type c = t.getType();
      if (c instanceof ParameterizedType) {
        c = ((ParameterizedType)c).getRawType();
      }
      if ( c.equals(opaque.getClass())) {
        return (T) opaque;
      } else {
        throw new IOException(String.format("Attempted to retrieve a list with type of %s.  However, the JSON " +
          "options carried an opaque value of type %s.", t.getType(), opaque.getClass().getName()));
      }
    }
    if (root == null) {
      return null;
    }
    return mapper.treeAsTokens(root).readValueAs(t);
  }

  public JsonNode path(String name) {
    return root.path(name);
  }

  public JsonNode getRoot() {
      return root;
  }

  @SuppressWarnings("serial")
  public static class De extends StdDeserializer<JSONOptions> {

    public De() {
      super(JSONOptions.class);
      logger.debug("Creating Deserializer.");
    }

    @Override
    public JSONOptions deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException,
        JsonProcessingException {
      JsonLocation l = jp.getTokenLocation();
//      logger.debug("Reading tree.");
      TreeNode n = jp.readValueAsTree();
//      logger.debug("Tree {}", n);
      if (n instanceof JsonNode) {
        return new JSONOptions( (JsonNode) n, l);
      } else {
        throw new IllegalArgumentException(String.format("Received something other than a JsonNode %s", n));
      }
    }
  }

  @SuppressWarnings("serial")
  public static class Se extends StdSerializer<JSONOptions> {

    public Se() {
      super(JSONOptions.class);
    }

    @Override
    public void serialize(JSONOptions value, JsonGenerator jgen, SerializerProvider provider) throws IOException,
        JsonGenerationException {
      if (value.opaque != null) {
        jgen.writeObject(value.opaque);
      } else {
        jgen.writeTree(value.root);
      }
    }
  }
}
