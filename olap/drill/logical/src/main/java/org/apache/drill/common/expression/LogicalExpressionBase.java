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
package org.apache.drill.common.expression;

import org.apache.drill.common.types.TypeProtos.MajorType;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;


@JsonPropertyOrder({ "type" })
public abstract class LogicalExpressionBase implements LogicalExpression {

  private final ExpressionPosition pos;

  protected LogicalExpressionBase(ExpressionPosition pos) {
    super();
    this.pos = pos;
  }

  @Override
  public ExpressionPosition getPosition() {
    return pos;
  }

  protected void i(StringBuilder sb, int indent) {
    for (int i = 0; i < indent; i++){
      sb.append("  ");
    }
  }

  @Override
  public MajorType getMajorType() {
    throw new UnsupportedOperationException(String.format("The type of %s doesn't currently support LogicalExpression.getDataType().", this.getClass().getCanonicalName()));
  }

  @JsonProperty("type")
  public String getDescription() {
    return this.getClass().getSimpleName();
  }

  @Override
  @JsonIgnore
  public int getSelfCost() {
    return 0;
  }

  @Override
  @JsonIgnore
  public int getCumulativeCost()  {
    int cost = this.getSelfCost();

    for (LogicalExpression e : this) {
      cost += e.getCumulativeCost();
    }

    return cost;
  }

}
