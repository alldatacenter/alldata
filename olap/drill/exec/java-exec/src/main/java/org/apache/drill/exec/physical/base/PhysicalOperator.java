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
package org.apache.drill.exec.physical.base;

import java.util.List;

import org.apache.drill.common.exceptions.ExecutionSetupException;
import org.apache.drill.common.graph.GraphValue;
import org.apache.drill.exec.ops.QueryContext;
import org.apache.drill.exec.planner.cost.PrelCostEstimates;
import org.apache.drill.exec.record.BatchSchema.SelectionVectorMode;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;

@JsonInclude(Include.NON_NULL)
@JsonPropertyOrder({ "@id" })
@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "@id")
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "pop")
public interface PhysicalOperator extends GraphValue<PhysicalOperator> {

  /**
   * Describes whether or not a particular physical operator can actually be executed. Most physical operators can be
   * executed. However, Exchange nodes cannot be executed. In order to be executed, they must be converted into their
   * Exec sub components.
   */
  @JsonIgnore
  boolean isExecutable();

  /**
   * Describes the SelectionVector Mode for the output steam from this physical op.
   * This property is used during physical plan creating using {@link org.apache.drill.exec.planner.physical.PhysicalPlanCreator}.
   */
  @JsonIgnore
  SelectionVectorMode getSVMode();

  /**
   * Provides capability to build a set of output based on traversing a query graph tree.
   *
   * @param physicalVisitor
   */
  <T, X, E extends Throwable> T accept(PhysicalVisitor<T, X, E> physicalVisitor, X value) throws E;

  /**
   * Regenerate with this node with a new set of children.  This is used in the case of materialization or optimization.
   * @param children
   */
  @JsonIgnore
  PhysicalOperator getNewWithChildren(List<PhysicalOperator> children) throws ExecutionSetupException;

  /**
   * @return The memory to preallocate for this operator
   */
  long getInitialAllocation();

  /**
   * @return The maximum memory this operator can allocate
   */
  long getMaxAllocation();

  /**
   *
   * @param maxAllocation The max memory allocation to be set
   */
  void setMaxAllocation(long maxAllocation);

  /**
   *
   * @return True iff this operator manages its memory (including disk spilling)
   * @param queryContext
   */
  @JsonIgnore
  boolean isBufferedOperator(QueryContext queryContext);

  // public void setBufferedOperator(boolean bo);

  @JsonProperty("@id")
  int getOperatorId();

  @JsonProperty("@id")
  void setOperatorId(int id);

  @JsonProperty("cost")
  void setCost(PrelCostEstimates cost);

  @JsonProperty("cost")
  PrelCostEstimates getCost();

  /**
   * Name of the user whom to impersonate while setting up the implementation (RecordBatch) of this
   * PhysicalOperator. Default value is "null" in which case we impersonate as user who launched the query.
   */
  @JsonProperty("userName")
  String getUserName();

  @JsonIgnore
  String getOperatorType();
}
