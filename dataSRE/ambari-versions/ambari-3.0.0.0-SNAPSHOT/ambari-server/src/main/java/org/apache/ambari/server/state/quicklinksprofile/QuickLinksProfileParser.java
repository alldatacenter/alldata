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

package org.apache.ambari.server.state.quicklinksprofile;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableList;
import com.google.common.io.Resources;

/**
 * Loads and parses JSON quicklink profiles.
 */
public class QuickLinksProfileParser {
  private final ObjectMapper mapper = new ObjectMapper();

  public QuickLinksProfileParser() {
    SimpleModule module =
        new SimpleModule("Quick Links Parser", new Version(1, 0, 0, null, null, null));
    module.addDeserializer(Filter.class, new QuickLinksFilterDeserializer());
    mapper.registerModule(module);
    mapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
  }

  public QuickLinksProfile parse(byte[] input) throws IOException {
    return mapper.readValue(input, QuickLinksProfile.class);
  }

  public QuickLinksProfile parse(URL url) throws IOException {
    return parse(Resources.toByteArray(url));
  }

  public String encode(QuickLinksProfile profile) throws IOException {
    return mapper.writeValueAsString(profile);
  }
}

/**
 * Custom deserializer is needed to handle filter polymorphism.
 */
class QuickLinksFilterDeserializer extends StdDeserializer<Filter> {
  static final String PARSE_ERROR_MESSAGE_AMBIGUOUS_FILTER =
      "A filter is not allowed to declare both link_name and link_attribute at the same time.";

  static final String PARSE_ERROR_MESSAGE_INVALID_JSON_TAG =
      "Invalid attribute(s) in filter declaration: ";

  QuickLinksFilterDeserializer() {
    super(Filter.class);
  }

  /**
   * Filter polymorphism is handled here. If a filter object in the JSON document has:
   * <ul>
   *   <li>a {@code link_attribute} field, it will parsed as {@link LinkAttributeFilter}</li>
   *   <li>a {@code link_name} field, it will be parsed as {@link LinkNameFilter}</li>
   *   <li>both {@code link_attribute} and {@code link_name}, it will throw a {@link JsonParseException}</li>
   *   <li>neither of the above fields, it will be parsed as {@link AcceptAllFilter}</li>
   * </ul>
   *
   * @throws JsonParseException if ambiguous filter definitions are found, or any JSON syntax error.
   */
  @Override
  public Filter deserialize (JsonParser parser, DeserializationContext context) throws IOException, JsonProcessingException {
    ObjectMapper mapper = (ObjectMapper) parser.getCodec();
    ObjectNode root = mapper.readTree(parser);
    Class<? extends Filter> filterClass = null;
    List<String> invalidAttributes = new ArrayList<>();
    for (String fieldName: ImmutableList.copyOf(root.fieldNames())) {
      switch(fieldName) {
        case LinkAttributeFilter.LINK_ATTRIBUTE:
          if (null != filterClass) {
            throw new JsonParseException(parser, PARSE_ERROR_MESSAGE_AMBIGUOUS_FILTER, parser.getCurrentLocation());
          }
          filterClass = LinkAttributeFilter.class;
          break;
        case LinkNameFilter.LINK_NAME:
        case LinkNameFilter.LINK_URL:
          if (null != filterClass && !filterClass.equals(LinkNameFilter.class)) {
            throw new JsonParseException(parser, PARSE_ERROR_MESSAGE_AMBIGUOUS_FILTER, parser.getCurrentLocation());
          }
          filterClass = LinkNameFilter.class;
          break;
        case Filter.VISIBLE:
          // silently ignore here, will be parsed later in mapper.readValue
          break;
        default:
          invalidAttributes.add(fieldName);
      }
    }
    if (!invalidAttributes.isEmpty()) {
      throw new JsonParseException(parser, PARSE_ERROR_MESSAGE_INVALID_JSON_TAG + invalidAttributes,
          parser.getCurrentLocation());
    }
    if (null == filterClass) {
      filterClass = AcceptAllFilter.class;
    }
    return mapper.readValue(root.traverse(), filterClass);
  }
}