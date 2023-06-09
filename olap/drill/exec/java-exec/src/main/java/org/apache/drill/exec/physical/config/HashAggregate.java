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
package org.apache.drill.exec.physical.config;

import org.apache.drill.common.logical.data.NamedExpression;
import org.apache.drill.exec.ExecConstants;
import org.apache.drill.exec.ops.QueryContext;
import org.apache.drill.exec.physical.base.AbstractSingle;
import org.apache.drill.exec.physical.base.PhysicalOperator;
import org.apache.drill.exec.physical.base.PhysicalVisitor;
import org.apache.drill.exec.planner.physical.AggPrelBase;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;

import java.util.List;

@JsonTypeName("hash-aggregate")
public class HashAggregate extends AbstractSingle {

  public static final String OPERATOR_TYPE = "HASH_AGGREGATE";

  private final AggPrelBase.OperatorPhase aggPhase;
  private final List<NamedExpression> groupByExprs;
  private final List<NamedExpression> aggrExprs;

  private final float cardinality;

  @JsonCreator
  public HashAggregate(@JsonProperty("child") PhysicalOperator child,
                       @JsonProperty("phase") AggPrelBase.OperatorPhase aggPhase,
                       @JsonProperty("keys") List<NamedExpression> groupByExprs,
                       @JsonProperty("exprs") List<NamedExpression> aggrExprs,
                       @JsonProperty("cardinality") float cardinality) {
    super(child);
    this.aggPhase = aggPhase;
    this.groupByExprs = groupByExprs;
    this.aggrExprs = aggrExprs;
    this.cardinality = cardinality;
  }

  public AggPrelBase.OperatorPhase getAggPhase() { return aggPhase; }

  public List<NamedExpression> getGroupByExprs() {
    return groupByExprs;
  }

  public List<NamedExpression> getAggrExprs() {
    return aggrExprs;
  }

  public double getCardinality() {
    return cardinality;
  }

  @Override
  public <T, X, E extends Throwable> T accept(PhysicalVisitor<T, X, E> physicalVisitor, X value) throws E{
    return physicalVisitor.visitHashAggregate(this, value);
  }

  @Override
  protected PhysicalOperator getNewWithChild(PhysicalOperator child) {
    HashAggregate newHAG = new HashAggregate(child, aggPhase, groupByExprs, aggrExprs, cardinality);
    newHAG.setMaxAllocation(getMaxAllocation());
    return newHAG;
  }

  @Override
  public String getOperatorType() {
    return OPERATOR_TYPE;
  }

  /**
   *
   * @param maxAllocation The max memory allocation to be set
   */
  @Override
  public void setMaxAllocation(long maxAllocation) {
    this.maxAllocation = maxAllocation;
  }
  /**
   * The Hash Aggregate operator supports spilling
   * @return true (unless a single partition is forced)
   * @param queryContext
   */
  @Override
  public boolean isBufferedOperator(QueryContext queryContext) {
    // In case forced to use a single partition - do not consider this a buffered op (when memory is divided)
    return queryContext == null ||
      1 < (int) queryContext.getOptions().getOption(ExecConstants.HASHAGG_NUM_PARTITIONS_VALIDATOR);
  }

  @Override
  public String toString() {
    return "HashAggregate[groupByExprs=" + groupByExprs
        + ", aggrExprs=" + aggrExprs
        + ", cardinality=" + cardinality
        + "]";
  }
}
