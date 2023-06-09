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
import org.apache.drill.shaded.guava.com.google.common.collect.Lists;
import org.apache.calcite.rel.core.JoinRelType;
import org.apache.drill.common.logical.data.JoinCondition;
import org.apache.drill.exec.physical.base.AbstractJoinPop;
import org.apache.drill.exec.physical.base.PhysicalOperator;

import java.util.List;

@JsonTypeName("merge-join")
public class MergeJoinPOP extends AbstractJoinPop {

  public static final String OPERATOR_TYPE = "MERGE_JOIN";

  @JsonCreator
  public MergeJoinPOP(
      @JsonProperty("left") PhysicalOperator left,
      @JsonProperty("right") PhysicalOperator right,
      @JsonProperty("conditions") List<JoinCondition> conditions,
      @JsonProperty("joinType") JoinRelType joinType
  ) {
    super(left, right, joinType, false, null, conditions);
    Preconditions.checkArgument(joinType != null, "Join type is missing!");
    Preconditions.checkArgument(joinType != JoinRelType.FULL,
      "Full outer join not currently supported with Merge Join");
  }

  @Override
  public PhysicalOperator getNewWithChildren(List<PhysicalOperator> children) {
    Preconditions.checkArgument(children.size() == 2);
    return new MergeJoinPOP(children.get(0), children.get(1), conditions, joinType);
  }

  public MergeJoinPOP flipIfRight(){
    if(joinType == JoinRelType.RIGHT){
      List<JoinCondition> flippedConditions = Lists.newArrayList();
      for(JoinCondition c : conditions){
        flippedConditions.add(c.flip());
      }
      return new MergeJoinPOP(right, left, flippedConditions, JoinRelType.LEFT);
    }else{
      return this;
    }
  }

  @Override
  public String getOperatorType() {
    return OPERATOR_TYPE;
  }
}
