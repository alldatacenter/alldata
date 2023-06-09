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
package org.apache.drill.metastore.iceberg.transform;

import org.apache.drill.metastore.expressions.DoubleExpressionPredicate;
import org.apache.drill.metastore.expressions.FilterExpression;
import org.apache.drill.metastore.expressions.IsPredicate;
import org.apache.drill.metastore.expressions.ListPredicate;
import org.apache.drill.metastore.expressions.SimplePredicate;
import org.apache.drill.metastore.expressions.SingleExpressionPredicate;
import org.apache.iceberg.expressions.Expression;
import org.apache.iceberg.expressions.Expressions;

/**
 * Visits {@link FilterExpression} implementations and transforms them into Iceberg {@link Expression}.
 */
public class FilterExpressionVisitor implements FilterExpression.Visitor<Expression> {

  private static final FilterExpressionVisitor INSTANCE = new FilterExpressionVisitor();

  public static FilterExpression.Visitor<Expression> get() {
    return INSTANCE;
  }

  @Override
  public Expression visit(SimplePredicate.Equal<?> expression) {
    return Expressions.equal(expression.column().columnName(), expression.value());
  }

  @Override
  public Expression visit(SimplePredicate.NotEqual<?> expression) {
    return Expressions.notEqual(expression.column().columnName(), expression.value());
  }

  @Override
  public Expression visit(SimplePredicate.LessThan<?> expression) {
    return Expressions.lessThan(expression.column().columnName(), expression.value());
  }

  @Override
  public Expression visit(SimplePredicate.LessThanOrEqual<?> expression) {
    return Expressions.lessThanOrEqual(expression.column().columnName(), expression.value());
  }

  @Override
  public Expression visit(SimplePredicate.GreaterThan<?> expression) {
    return Expressions.greaterThan(expression.column().columnName(), expression.value());
  }

  @Override
  public Expression visit(SimplePredicate.GreaterThanOrEqual<?> expression) {
    return Expressions.greaterThanOrEqual(expression.column().columnName(), expression.value());
  }

  @Override
  public Expression visit(ListPredicate.In<?> expression) {
    return Expressions.in(expression.column().columnName(), expression.values());
  }

  @Override
  public Expression visit(ListPredicate.NotIn<?> expression) {
    return Expressions.notIn(expression.column().columnName(), expression.values());
  }

  @Override
  public Expression visit(IsPredicate.IsNull expression) {
    return Expressions.isNull(expression.column().columnName());
  }

  @Override
  public Expression visit(IsPredicate.IsNotNull expression) {
    return Expressions.notNull(expression.column().columnName());
  }

  @Override
  public Expression visit(SingleExpressionPredicate.Not expression) {
    Expression child = expression.expression().accept(this);
    return Expressions.not(child);
  }

  @Override
  public Expression visit(DoubleExpressionPredicate.And expression) {
    Expression right = expression.right().accept(this);
    Expression left = expression.left().accept(this);
    return Expressions.and(right, left);
  }

  @Override
  public Expression visit(DoubleExpressionPredicate.Or expression) {
    Expression right = expression.right().accept(this);
    Expression left = expression.left().accept(this);
    return Expressions.or(right, left);
  }
}
