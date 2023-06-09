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

import java.util.Iterator;
import java.util.List;

import org.apache.drill.common.exceptions.ExpressionParsingException;
import org.apache.drill.common.expression.LogicalExpression;
import org.apache.drill.common.logical.data.visitors.LogicalVisitor;
import org.apache.calcite.rel.core.JoinRelType;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import org.apache.drill.shaded.guava.com.google.common.base.Preconditions;
import org.apache.drill.shaded.guava.com.google.common.collect.Iterators;
import org.apache.drill.shaded.guava.com.google.common.collect.Lists;

@JsonTypeName("join")
public class Join extends LogicalOperatorBase {
  private final LogicalOperator left;
  private final LogicalOperator right;
  private final JoinRelType type;
  private final List<JoinCondition> conditions;

  public static JoinRelType resolve(String val) {
    for (JoinRelType jt : JoinRelType.values()) {
      if (jt.name().equalsIgnoreCase(val)) {
        return jt;
      }
    }
    throw new ExpressionParsingException(String.format("Unable to determine join type for value '%s'.", val));
  }

  // TODO - should not have two @JsonCreators, need to figure out which one is being used
  // I'm guess this one, is the case insensitive match in resolve() actually needed?
  @JsonCreator
  public Join(@JsonProperty("left") LogicalOperator left, @JsonProperty("right") LogicalOperator right,
      @JsonProperty("conditions") List<JoinCondition> conditions, @JsonProperty("type") String type) {
    this(left, right, conditions, resolve(type));
  }

  @JsonCreator
  public Join(@JsonProperty("left") LogicalOperator left, @JsonProperty("right") LogicalOperator right,
              @JsonProperty("conditions") List<JoinCondition> conditions, @JsonProperty("type") JoinRelType type) {
    super();
    this.conditions = conditions;
    this.left = left;
    this.right = right;
    left.registerAsSubscriber(this);
    right.registerAsSubscriber(this);
    this.type = type;

  }

  public LogicalOperator getLeft() {
    return left;
  }

  public LogicalOperator getRight() {
    return right;
  }

  public List<JoinCondition> getConditions() {
    return conditions;
  }

  @JsonIgnore
  public JoinRelType getJoinType() {
    return type;
  }

  public String getType() {
    return type.name();
  }

  @Override
  public <T, X, E extends Throwable> T accept(LogicalVisitor<T, X, E> logicalVisitor, X value) throws E {
    return logicalVisitor.visitJoin(this, value);
  }

  @Override
  public Iterator<LogicalOperator> iterator() {
    return Iterators.forArray(getLeft(), getRight());
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder extends AbstractBuilder<Join>{
    private LogicalOperator left;
    private LogicalOperator right;
    private JoinRelType type;
    private List<JoinCondition> conditions = Lists.newArrayList();

    public Builder type(JoinRelType type) {
      this.type = type;
      return this;
    }

    public Builder left(LogicalOperator left) {
      this.left = left;
      return this;
    }
    public Builder right(LogicalOperator right) {
      this.right = right;
      return this;
    }

    public Builder addCondition(String relationship, LogicalExpression left, LogicalExpression right) {
      conditions.add(new JoinCondition(relationship, left, right));
      return this;
    }

    @Override
    public Join build() {
      Preconditions.checkNotNull(left);
      Preconditions.checkNotNull(right);
      Preconditions.checkNotNull(type);
      return new Join(left, right, conditions, type);
    }

  }

}
