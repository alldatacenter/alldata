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
package org.apache.drill.metastore.expressions;

import org.apache.drill.metastore.MetastoreColumn;

import java.util.List;
import java.util.StringJoiner;

/**
 * Indicates list predicate implementations which have column and list of values.
 *
 * @param <T> predicate value type
 */
public abstract class ListPredicate<T> implements FilterExpression {

  private final MetastoreColumn column;
  private final Operator operator;
  private final List<T> values;

  protected ListPredicate(MetastoreColumn column, Operator operator, List<T> values) {
    this.column = column;
    this.operator = operator;
    this.values = values;
  }

  public MetastoreColumn column() {
    return column;
  }

  public List<T> values() {
    return values;
  }

  @Override
  public Operator operator() {
    return operator;
  }

  @Override
  public String toString() {
    return new StringJoiner(", ", ListPredicate.class.getSimpleName() + "[", "]")
      .add("column=" + column)
      .add("operator=" + operator)
      .add("values=" + values)
      .toString();
  }

  /**
   * Indicates {@link FilterExpression.Operator#IN} operator expression:
   * storagePlugin in ('dfs', 's3').
   *
   * @param <T> expression value type
   */
  public static class In<T> extends ListPredicate<T> {

    public In(MetastoreColumn column, List<T> values) {
      super(column, Operator.IN, values);
    }

    @Override
    public <V> V accept(Visitor<V> visitor) {
      return visitor.visit(this);
    }
  }

  /**
   * Indicates {@link FilterExpression.Operator#NOT_IN} operator expression:
   * storagePlugin not in ('dfs', 's3').
   *
   * @param <T> expression value type
   */
  public static class NotIn<T> extends ListPredicate<T> {

    public NotIn(MetastoreColumn column, List<T> values) {
      super(column, Operator.NOT_IN, values);
    }

    @Override
    public <V> V accept(Visitor<V> visitor) {
      return visitor.visit(this);
    }
  }
}
