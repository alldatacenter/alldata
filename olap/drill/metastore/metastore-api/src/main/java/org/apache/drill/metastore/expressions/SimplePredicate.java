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

import java.util.StringJoiner;

/**
 * Indicates simple predicate implementations which have column and one value.
 *
 * @param <T> predicate value type
 */
public abstract class SimplePredicate<T> implements FilterExpression {

  private final MetastoreColumn column;
  private final Operator operator;
  private final T value;

  protected SimplePredicate(MetastoreColumn column, Operator operator, T value) {
    this.column = column;
    this.operator = operator;
    this.value = value;
  }

  public MetastoreColumn column() {
    return column;
  }

  public T value() {
    return value;
  }

  @Override
  public Operator operator() {
    return operator;
  }

  @Override
  public String toString() {
    return new StringJoiner(", ", SimplePredicate.class.getSimpleName() + "[", "]")
      .add("column=" + column)
      .add("operator=" + operator)
      .add("value=" + value)
      .toString();
  }

  /**
   * Indicates {@link FilterExpression.Operator#EQUAL} operator expression:
   * storagePlugin = 'dfs'.
   *
   * @param <T> expression value type
   */
  public static class Equal<T> extends SimplePredicate<T> {

    public Equal(MetastoreColumn column, T value) {
      super(column, Operator.EQUAL, value);
    }

    @Override
    public <V> V accept(Visitor<V> visitor) {
      return visitor.visit(this);
    }
  }

  /**
   * Indicates {@link FilterExpression.Operator#NOT_EQUAL} operator expression:
   * storagePlugin != 'dfs'.
   *
   * @param <T> expression value type
   */
  public static class NotEqual<T> extends SimplePredicate<T> {

    public NotEqual(MetastoreColumn column, T value) {
      super(column, Operator.NOT_EQUAL, value);
    }

    @Override
    public <V> V accept(Visitor<V> visitor) {
      return visitor.visit(this);
    }
  }

  /**
   * Indicates {@link FilterExpression.Operator#LESS_THAN} operator expression:
   * index < 1.
   *
   * @param <T> expression value type
   */
  public static class LessThan<T> extends SimplePredicate<T> {

    public LessThan(MetastoreColumn column, T value) {
      super(column, Operator.LESS_THAN, value);
    }

    @Override
    public <V> V accept(Visitor<V> visitor) {
      return visitor.visit(this);
    }
  }

  /**
   * Indicates {@link FilterExpression.Operator#LESS_THAN_OR_EQUAL} operator expression:
   * index <= 1.
   *
   * @param <T> expression value type
   */
  public static class LessThanOrEqual<T> extends SimplePredicate<T> {

    public LessThanOrEqual(MetastoreColumn column, T value) {
      super(column, Operator.LESS_THAN_OR_EQUAL, value);
    }

    @Override
    public <V> V accept(Visitor<V> visitor) {
      return visitor.visit(this);
    }
  }

  /**
   * Indicates {@link FilterExpression.Operator#GREATER_THAN} operator expression:
   * index > 1.
   *
   * @param <T> expression value type
   */
  public static class GreaterThan<T> extends SimplePredicate<T> {

    public GreaterThan(MetastoreColumn column, T value) {
      super(column, Operator.GREATER_THAN, value);
    }

    @Override
    public <V> V accept(Visitor<V> visitor) {
      return visitor.visit(this);
    }
  }

  /**
   * Indicates {@link FilterExpression.Operator#GREATER_THAN_OR_EQUAL} operator expression:
   * index >= 1.
   *
   * @param <T> expression value type
   */
  public static class GreaterThanOrEqual<T> extends SimplePredicate<T> {

    public GreaterThanOrEqual(MetastoreColumn column, T value) {
      super(column, Operator.GREATER_THAN_OR_EQUAL, value);
    }

    @Override
    public <V> V accept(Visitor<V> visitor) {
      return visitor.visit(this);
    }
  }
}
