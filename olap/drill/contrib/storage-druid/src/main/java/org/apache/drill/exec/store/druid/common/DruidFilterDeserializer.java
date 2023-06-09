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

package org.apache.drill.exec.store.druid.common;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;

import static org.apache.drill.exec.store.druid.common.DruidCompareOp.TYPE_SELECTOR;

public class DruidFilterDeserializer extends JsonDeserializer<DruidFilter> {
  @Override
  public DruidFilter deserialize(JsonParser jp, DeserializationContext context) throws
      IOException {
    ObjectMapper mapper = (ObjectMapper) jp.getCodec();
    ObjectNode root = mapper.readTree(jp);

    boolean filterKnowsItsType = root.has("type");

    if (filterKnowsItsType) {
      DruidCompareOp type = DruidCompareOp.get(root.get("type").asText());
      switch (type) {
        case TYPE_SELECTOR: {
          return mapper.readValue(root.toString(), DruidSelectorFilter.class);
        }
        case TYPE_BOUND: {
          return mapper.readValue(root.toString(), DruidBoundFilter.class);
        }
        case TYPE_IN: {
          return mapper.readValue(root.toString(), DruidInFilter.class);
        }
        case TYPE_REGEX: {
          return mapper.readValue(root.toString(), DruidRegexFilter.class);
        }
        case TYPE_SEARCH: {
          return mapper.readValue(root.toString(), DruidSearchFilter.class);
        }
        case AND: {
          return mapper.readValue(root.toString(), DruidAndFilter.class);
        }
        case OR: {
          return mapper.readValue(root.toString(), DruidOrFilter.class);
        }
        case NOT: {
          return mapper.readValue(root.toString(), DruidNotFilter.class);
        }
      }
    }
    if (filterKnowsItsType && root.get("type").asText().equals(TYPE_SELECTOR.getCompareOp())) {
      return mapper.readValue(root.toString(), DruidSelectorFilter.class);
    }
    return mapper.readValue(root.toString(), DruidSelectorFilter.class);
  }
}
