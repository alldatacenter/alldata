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
package org.apache.drill.common.util;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.drill.common.exceptions.LogicalPlanParsingException;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.core.JsonParser.Feature;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Captures all properties and turns them into an object node for late bind conversion.
 */
public abstract class AbstractDynamicBean {
  static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(AbstractDynamicBean.class);

  private static volatile ObjectMapper MAPPER;

  private ObjectNode objectNode = new ObjectNode(null);

  @JsonAnySetter
  public void _anySetter(String name, JsonNode value){
    objectNode.put(name, value);
  }

  @JsonAnyGetter
  public Map<String,JsonNode> _anyGetter() {
    Map<String, JsonNode> unknowns = new HashMap<String, JsonNode>();

    for(Iterator<Entry<String, JsonNode>> i = objectNode.fields(); i.hasNext(); ){
      Entry<String, JsonNode> e = i.next();
      unknowns.put(e.getKey(), e.getValue());
    }
    return unknowns;
  }

  public <T> T getWith(Class<T> c){
    try {
      return getMapper().treeToValue(objectNode, c);
    } catch (JsonProcessingException e) {
      throw new LogicalPlanParsingException(String.format("Failure while trying to convert late bound json type to type of %s.", c.getCanonicalName()), e);
    }
  }


  private static synchronized ObjectMapper getMapper(){
    if(MAPPER == null){
      ObjectMapper mapper = new ObjectMapper();
      mapper.enable(SerializationFeature.INDENT_OUTPUT);
      mapper.configure(Feature.ALLOW_UNQUOTED_FIELD_NAMES, true);
      mapper.configure(Feature.ALLOW_COMMENTS, true);
      MAPPER = mapper;
    }
    return MAPPER;
  }
}
