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
package org.apache.drill.exec.physical;

import java.io.IOException;
import java.util.List;

import org.apache.drill.common.graph.Graph;
import org.apache.drill.common.graph.GraphAlgos;
import org.apache.drill.common.logical.PlanProperties;
import org.apache.drill.exec.physical.base.Leaf;
import org.apache.drill.exec.physical.base.PhysicalOperator;
import org.apache.drill.exec.physical.base.Root;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
import org.apache.drill.shaded.guava.com.google.common.collect.Lists;

@JsonPropertyOrder({ "head", "graph" })
public class PhysicalPlan {

  protected PlanProperties properties;

  protected Graph<PhysicalOperator, Root, Leaf> graph;

  @JsonCreator
  public PhysicalPlan(@JsonProperty("head") PlanProperties properties,
                      @JsonProperty("graph") List<PhysicalOperator> operators) {
    this.properties = properties;
    this.graph = Graph.newGraph(operators, Root.class, Leaf.class);
  }

  @JsonProperty("graph")
  public List<PhysicalOperator> getSortedOperators(){
    // reverse the list so that nested references are flattened rather than nested.
    return getSortedOperators(true);
  }

  public List<PhysicalOperator> getSortedOperators(boolean reverse){
    List<PhysicalOperator> list = GraphAlgos.TopoSorter.sort(graph);
    if(reverse){
      return Lists.reverse(list);
    }else{
      return list;
    }
  }

  @JsonProperty("head")
  public PlanProperties getProperties() {
    return properties;
  }

  /** Parses a physical plan. */
  public static PhysicalPlan parse(ObjectReader reader, String planString) {
    try {
      PhysicalPlan plan = reader.readValue(planString);
      return plan;
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  /** Converts a physical plan to a string. (Opposite of {@link #parse}.) */
  public String unparse(ObjectWriter writer) {
    try {
      return writer.writeValueAsString(this);
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }

  public double totalCost() {
    double totalCost = 0;
    for (final PhysicalOperator ops : getSortedOperators()) {
      totalCost += ops.getCost().getOutputRowCount();
    }
    return totalCost;
  }

  @JsonIgnore
  public Graph<PhysicalOperator, Root, Leaf> graph() {
    return graph;
  }
}
