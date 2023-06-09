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

import java.util.List;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.apache.drill.shaded.guava.com.google.common.annotations.VisibleForTesting;
import org.apache.drill.common.logical.data.JoinCondition;
import org.apache.drill.exec.ExecConstants;
import org.apache.drill.exec.ops.QueryContext;
import org.apache.drill.exec.physical.base.PhysicalOperator;
import org.apache.drill.exec.physical.base.SubScan;
import org.apache.drill.exec.planner.common.JoinControl;
import org.apache.calcite.rel.core.JoinRelType;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import org.apache.drill.shaded.guava.com.google.common.base.Preconditions;
import org.apache.drill.shaded.guava.com.google.common.collect.Lists;
import org.apache.drill.exec.physical.base.AbstractJoinPop;
import org.apache.drill.exec.work.filter.RuntimeFilterDef;


@JsonTypeName("hash-join")
@JsonIgnoreProperties(ignoreUnknown = true)
public class HashJoinPOP extends AbstractJoinPop {

  public static final String OPERATOR_TYPE = "HASH_JOIN";

  private RuntimeFilterDef runtimeFilterDef;

  private final boolean isRowKeyJoin;
  private final int joinControl;
  @JsonProperty("subScanForRowKeyJoin")
  private SubScan subScanForRowKeyJoin;

  @JsonCreator
  public HashJoinPOP(@JsonProperty("left") PhysicalOperator left, @JsonProperty("right") PhysicalOperator right,
                     @JsonProperty("conditions") List<JoinCondition> conditions,
                     @JsonProperty("joinType") JoinRelType joinType,
                     @JsonProperty("semiJoin") boolean semiJoin,
                     @JsonProperty("runtimeFilterDef") RuntimeFilterDef runtimeFilterDef,
                     @JsonProperty("isRowKeyJoin") boolean isRowKeyJoin,
                     @JsonProperty("joinControl") int joinControl) {
    super(left, right, joinType, semiJoin,null, conditions);
    Preconditions.checkArgument(joinType != null, "Join type is missing for HashJoin Pop");
    this.runtimeFilterDef = runtimeFilterDef;
    this.isRowKeyJoin = isRowKeyJoin;
    this.subScanForRowKeyJoin = null;
    this.joinControl = joinControl;
  }

  @VisibleForTesting
  public HashJoinPOP(PhysicalOperator left, PhysicalOperator right,
                     List<JoinCondition> conditions,
                     JoinRelType joinType,
                     RuntimeFilterDef runtimeFilterDef,
                     boolean isRowKeyJoin,
                     int joinControl){
    this(left, right, conditions, joinType, false, runtimeFilterDef, isRowKeyJoin, joinControl);
  }

  @VisibleForTesting
  public HashJoinPOP(PhysicalOperator left, PhysicalOperator right,
                     List<JoinCondition> conditions,
                     JoinRelType joinType) {
    this(left, right, conditions, joinType, null, false, JoinControl.DEFAULT);
  }

  @VisibleForTesting
  public HashJoinPOP(PhysicalOperator left, PhysicalOperator right,
                     List<JoinCondition> conditions,
                     JoinRelType joinType,
                     RuntimeFilterDef runtimeFilterDef) {
    this(left, right, conditions, joinType, runtimeFilterDef, false, JoinControl.DEFAULT);
  }

  @Override
  public PhysicalOperator getNewWithChildren(List<PhysicalOperator> children) {
        Preconditions.checkArgument(children.size() == 2);

        HashJoinPOP newHashJoin = new HashJoinPOP(children.get(0), children.get(1), conditions, joinType, semiJoin, runtimeFilterDef,
              isRowKeyJoin, joinControl);
        newHashJoin.setMaxAllocation(getMaxAllocation());
        newHashJoin.setSubScanForRowKeyJoin(this.getSubScanForRowKeyJoin());
        return newHashJoin;
    }

    @JsonProperty("isRowKeyJoin")
    public boolean isRowKeyJoin() {
        return isRowKeyJoin;
    }

    @JsonProperty("joinControl")
    public int getJoinControl() {
        return joinControl;
    }

    @JsonProperty("subScanForRowKeyJoin")
    public SubScan getSubScanForRowKeyJoin() {
        return subScanForRowKeyJoin;
    }

    public void setSubScanForRowKeyJoin(SubScan subScan) {
        this.subScanForRowKeyJoin = subScan;
    }

  public HashJoinPOP flipIfRight() {
      if (joinType == JoinRelType.RIGHT) {
        List<JoinCondition> flippedConditions = Lists.newArrayList();
        for (JoinCondition c : conditions) {
          flippedConditions.add(c.flip());
        }
        return new HashJoinPOP(right, left, flippedConditions, JoinRelType.LEFT, semiJoin, runtimeFilterDef, isRowKeyJoin, joinControl);
      } else {
        return this;
      }
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
      1 < (int) queryContext.getOptions().getOption(ExecConstants.HASHJOIN_NUM_PARTITIONS_VALIDATOR);
  }

  public void setRuntimeFilterDef(RuntimeFilterDef runtimeFilterDef) {
    this.runtimeFilterDef = runtimeFilterDef;
  }


  public RuntimeFilterDef getRuntimeFilterDef() {
    return runtimeFilterDef;
  }
}
