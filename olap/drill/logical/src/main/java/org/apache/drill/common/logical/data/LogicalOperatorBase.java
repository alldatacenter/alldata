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
package org.apache.drill.common.logical.data;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.drill.common.graph.GraphVisitor;
import org.apache.drill.common.logical.ValidationError;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;


public abstract class LogicalOperatorBase implements LogicalOperator{

  private final List<LogicalOperator> children = new ArrayList<>();

  private String memo;

  @Override
  public final int hashCode() {
    return super.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    return super.equals(obj);
  }

  @Override
  public void setupAndValidate(List<LogicalOperator> operators, Collection<ValidationError> errors) {
    // TODO: remove this and implement individually.
  }

  @Override
  public NodeBuilder<?> nodeBuilder() {
    // FIXME: Implement this on all logical operators
    throw new UnsupportedOperationException("Not yet implemented.");
  }

  @Override
  public void accept(GraphVisitor<LogicalOperator> visitor) {
    if (visitor.enter(this)) {
      visitor.leave(this);
    }
  }

  @Override
  public void registerAsSubscriber(LogicalOperator operator) {
    if (operator == null) {
      throw new IllegalArgumentException("You attempted to register a null operators.");
    }
    children.add(operator);
  }

  @Override
  public String toString() {
    return this.getClass().getSimpleName() + " [memo=" + memo + "]";
  }

  @JsonInclude(Include.NON_EMPTY)
  @JsonProperty("memo")
  public String getMemo() {
    return memo;
  }

  public void setMemo(String memo) {
    this.memo = memo;
  }
}
