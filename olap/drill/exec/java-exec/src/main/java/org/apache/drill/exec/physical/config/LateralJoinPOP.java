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
import org.apache.drill.common.expression.SchemaPath;
import org.apache.drill.exec.physical.base.AbstractJoinPop;
import org.apache.drill.exec.physical.base.PhysicalOperator;
import org.apache.drill.exec.physical.base.PhysicalVisitor;

import java.util.List;

@JsonTypeName("lateral-join")
public class LateralJoinPOP extends AbstractJoinPop {

  public static final String OPERATOR_TYPE = "LATERAL_JOIN";

  @JsonProperty("excludedColumns")
  private List<SchemaPath> excludedColumns;

  @JsonProperty("implicitRIDColumn")
  private String implicitRIDColumn;

  @JsonProperty("unnestForLateralJoin")
  private UnnestPOP unnestForLateralJoin;

  @JsonCreator
  public LateralJoinPOP(
      @JsonProperty("left") PhysicalOperator left,
      @JsonProperty("right") PhysicalOperator right,
      @JsonProperty("joinType") JoinRelType joinType,
      @JsonProperty("implicitRIDColumn") String implicitRIDColumn,
      @JsonProperty("excludedColumns") List<SchemaPath> excludedColumns) {
    super(left, right, joinType, false, null, null);
    Preconditions.checkArgument(joinType != JoinRelType.FULL,
      "Full outer join is currently not supported with Lateral Join");
    Preconditions.checkArgument(joinType != JoinRelType.RIGHT,
      "Right join is currently not supported with Lateral Join");
    this.excludedColumns = excludedColumns;
    this.implicitRIDColumn = implicitRIDColumn;
  }

  @Override
  public PhysicalOperator getNewWithChildren(List<PhysicalOperator> children) {
    Preconditions.checkArgument(children.size() == 2,
      "Lateral join should have two physical operators");
    LateralJoinPOP newPOP =  new LateralJoinPOP(children.get(0), children.get(1), joinType, this.implicitRIDColumn, this.excludedColumns);
    newPOP.unnestForLateralJoin = this.unnestForLateralJoin;
    return newPOP;
  }

  @JsonProperty("unnestForLateralJoin")
  public UnnestPOP getUnnestForLateralJoin() {
    return this.unnestForLateralJoin;
  }

  @JsonProperty("excludedColumns")
  public List<SchemaPath> getExcludedColumns() {
    return this.excludedColumns;
  }

  public void setUnnestForLateralJoin(UnnestPOP unnest) {
    this.unnestForLateralJoin = unnest;
  }

  @JsonProperty("implicitRIDColumn")
  public String getImplicitRIDColumn() { return this.implicitRIDColumn; }

  @Override
  public String getOperatorType() {
    return OPERATOR_TYPE;
  }

  @Override
  public <T, X, E extends Throwable> T accept(PhysicalVisitor<T, X, E> physicalVisitor, X value) throws E {
    return physicalVisitor.visitLateralJoin(this, value);
  }
}
