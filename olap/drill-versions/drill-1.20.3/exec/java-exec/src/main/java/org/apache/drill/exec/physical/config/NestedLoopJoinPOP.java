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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import org.apache.drill.shaded.guava.com.google.common.base.Preconditions;
import org.apache.calcite.rel.core.JoinRelType;
import org.apache.drill.common.expression.LogicalExpression;
import org.apache.drill.exec.physical.base.AbstractJoinPop;
import org.apache.drill.exec.physical.base.PhysicalOperator;

import java.util.List;

@JsonTypeName("nested-loop-join")
public class NestedLoopJoinPOP extends AbstractJoinPop {

  public static final String OPERATOR_TYPE = "NESTED_LOOP_JOIN";

  @JsonCreator
  public NestedLoopJoinPOP(
      @JsonProperty("left") PhysicalOperator left,
      @JsonProperty("right") PhysicalOperator right,
      @JsonProperty("joinType") JoinRelType joinType,
      @JsonProperty("condition") LogicalExpression condition
  ) {
    super(left, right, joinType, false, condition, null);
  }

  @Override
  public PhysicalOperator getNewWithChildren(List<PhysicalOperator> children) {
    Preconditions.checkArgument(children.size() == 2, "Nested loop join should have two physical operators");
    return new NestedLoopJoinPOP(children.get(0), children.get(1), joinType, condition);
  }

  @Override
  public String getOperatorType() {
    return OPERATOR_TYPE;
  }
}
