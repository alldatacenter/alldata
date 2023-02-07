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
package org.apache.drill.common.logical;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.drill.common.config.LogicalPlanPersistence;
import org.apache.drill.common.graph.Graph;
import org.apache.drill.common.graph.GraphAlgos;
import org.apache.drill.common.logical.data.LogicalOperator;
import org.apache.drill.common.logical.data.SinkOperator;
import org.apache.drill.common.logical.data.SourceOperator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@JsonPropertyOrder({ "head", "storage", "query" })
public class LogicalPlan {
  static final Logger logger = LoggerFactory.getLogger(LogicalPlan.class);

  private final PlanProperties properties;
  private final Map<String, StoragePluginConfig> storageEngineMap;
  private final Graph<LogicalOperator, SinkOperator, SourceOperator> graph;


  @JsonCreator
  public LogicalPlan(@JsonProperty("head") PlanProperties head,
      @JsonProperty("storage") Map<String, StoragePluginConfig> storageEngineMap,
      @JsonProperty("query") List<LogicalOperator> operators) {
    this.storageEngineMap = storageEngineMap != null ? storageEngineMap : new HashMap<String, StoragePluginConfig>();
    this.properties = head;
    this.graph = Graph.newGraph(operators, SinkOperator.class, SourceOperator.class);
  }

  @JsonProperty("query")
  public List<LogicalOperator> getSortedOperators() {
    return GraphAlgos.TopoSorter.sortLogical(graph);
  }

  public StoragePluginConfig getStorageEngineConfig(String name) {
    return storageEngineMap.get(name);
  }

  @JsonIgnore
  public Graph<LogicalOperator, SinkOperator, SourceOperator> getGraph() {
    return graph;
  }

  @JsonProperty("head")
  public PlanProperties getProperties() {
    return properties;
  }

  @JsonProperty("storage")
  public Map<String, StoragePluginConfig> getStorageEngines() {
    return storageEngineMap;
  }

  public String toJsonString(LogicalPlanPersistence config) throws JsonProcessingException {
    return config.getMapper().writeValueAsString(this);
  }

  public String toJsonStringSafe(LogicalPlanPersistence config){
    try{
      return toJsonString(config);
    }catch(JsonProcessingException e){
      logger.error("Failure while trying to get JSON representation of plan.", e);
      return "Unable to generate plan.";
    }
  }

  /** Parses a logical plan. */
  public static LogicalPlan parse(LogicalPlanPersistence config, String planString) {
    ObjectMapper mapper = config.getMapper();
    try {
      LogicalPlan plan = mapper.readValue(planString, LogicalPlan.class);
      return plan;
    } catch (IOException e) {

      throw new RuntimeException(String.format("Failure while parsing plan: \n %s}", planString), e);
    }
  }

  /** Converts a logical plan to a string. (Opposite of {@link #parse}.) */
  public String unparse(LogicalPlanPersistence config) {
    try {
      return config.getMapper().writeValueAsString(this);
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }

  public static LogicalPlanBuilder builder() {
    return new LogicalPlanBuilder();
  }

}
