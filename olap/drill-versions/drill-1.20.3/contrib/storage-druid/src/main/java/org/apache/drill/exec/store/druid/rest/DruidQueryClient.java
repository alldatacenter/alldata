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
package org.apache.drill.exec.store.druid.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.drill.common.exceptions.UserException;
import org.apache.drill.exec.store.druid.druid.DruidScanResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import okhttp3.Response;

import java.io.InputStream;
import java.util.ArrayList;

public class DruidQueryClient {

  private static final Logger logger = LoggerFactory.getLogger(DruidQueryClient.class);

  private static final String QUERY_BASE_URI = "/druid/v2";
  private static final ObjectMapper mapper = new ObjectMapper();

  private final RestClient restClient;
  private final String queryUrl;

  public DruidQueryClient(String brokerURI, RestClient restClient) {
    queryUrl = brokerURI + QUERY_BASE_URI;
    this.restClient = restClient;
    logger.debug("Initialized DruidQueryClient with druidURL - {}", this.queryUrl);
  }

  public DruidScanResponse executeQuery(String query) throws Exception {
    logger.debug("Executing Query - {}", query);

    try (Response response = restClient.post(queryUrl, query)) {
      if (!response.isSuccessful()) {
        // TODO: Add a CustomErrorContext when this plugin is converted to EVF.
        throw UserException
            .dataReadError()
            .message("Error executing druid query. HTTP request failed")
            .addContext("Response code", response.code())
            .addContext("Response message", response.message())
            .build(logger);
      }

      InputStream responseStream = response.body().byteStream();
      ArrayNode responses = mapper.readValue(responseStream, ArrayNode.class);
      return parseResponse(responses);
    }
  }

  private DruidScanResponse parseResponse(ArrayNode responses) {
    String segmentId = "empty";
    ArrayList<ObjectNode> events = new ArrayList<>();
    ArrayList<String> columns = new ArrayList<>();

    if (responses.size() > 0) {
      ObjectNode firstNode = (ObjectNode) responses.get(0);
      segmentId = firstNode.get("segmentId").textValue();
      ArrayNode columnsNode = (ArrayNode) firstNode.get("columns");
      ArrayNode eventsNode = (ArrayNode) firstNode.get("events");
      for(int i=0;i < columnsNode.size(); i++) {
        String column = columnsNode.get(i).textValue();
        columns.add(column);
      }
      for(int i=0;i < eventsNode.size(); i++) {
        ObjectNode eventNode = (ObjectNode) eventsNode.get(i);
        events.add(eventNode);
      }
    }
    return new DruidScanResponse(segmentId, columns, events);
  }
}
