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
package org.apache.drill.metastore.rdbms.transform;

import org.apache.drill.metastore.MetastoreColumn;
import org.apache.drill.metastore.expressions.DoubleExpressionPredicate;
import org.apache.drill.metastore.expressions.FilterExpression;
import org.apache.drill.metastore.expressions.IsPredicate;
import org.apache.drill.metastore.expressions.ListPredicate;
import org.apache.drill.metastore.expressions.SimplePredicate;
import org.apache.drill.metastore.expressions.SingleExpressionPredicate;
import org.jooq.Condition;
import org.jooq.Field;
import org.jooq.impl.DSL;

import java.util.Map;

/**
 * Visits {@link FilterExpression} implementations and transforms them into JOOQ {@link Condition}.
 */
@SuppressWarnings({"unchecked", "rawtypes"})
public class RdbmsFilterExpressionVisitor implements FilterExpression.Visitor<Condition> {
  private final Map<MetastoreColumn, Field<?>> fields;

  public RdbmsFilterExpressionVisitor(Map<MetastoreColumn, Field<?>> fields) {
    this.fields = fields;
  }

  @Override
  public Condition visit(SimplePredicate.Equal<?> expression) {
    Field field = fields.get(expression.column());
    return field == null ? DSL.falseCondition() : field.eq(expression.value());
  }

  @Override
  public Condition visit(SimplePredicate.NotEqual<?> expression) {
    Field field = fields.get(expression.column());
    return field == null ? DSL.trueCondition() : field.notEqual(expression.value());
  }

  @Override
  public Condition visit(SimplePredicate.LessThan<?> expression) {
    Field field = fields.get(expression.column());
    return field == null ? DSL.falseCondition() : field.lessThan(expression.value());
  }

  @Override
  public Condition visit(SimplePredicate.LessThanOrEqual<?> expression) {
    Field field = fields.get(expression.column());
    return field == null ? DSL.falseCondition() : field.lessOrEqual(expression.value());
  }

  @Override
  public Condition visit(SimplePredicate.GreaterThan<?> expression) {
    Field field = fields.get(expression.column());
    return field == null ? DSL.falseCondition() : field.greaterThan(expression.value());
  }

  @Override
  public Condition visit(SimplePredicate.GreaterThanOrEqual<?> expression) {
    Field field = fields.get(expression.column());
    return field == null ? DSL.falseCondition() : field.greaterOrEqual(expression.value());
  }

  @Override
  public Condition visit(ListPredicate.In<?> expression) {
    Field field = fields.get(expression.column());
    return field == null ? DSL.falseCondition() : field.in(expression.values());
  }

  @Override
  public Condition visit(ListPredicate.NotIn<?> expression) {
    Field field = fields.get(expression.column());
    return field == null ? DSL.trueCondition() : field.notIn(expression.values());
  }

  @Override
  public Condition visit(IsPredicate.IsNull expression) {
    Field field = fields.get(expression.column());
    return field == null ? DSL.trueCondition() : field.isNull();
  }

  @Override
  public Condition visit(IsPredicate.IsNotNull expression) {
    Field field = fields.get(expression.column());
    return field == null ? DSL.falseCondition() : field.isNotNull();
  }

  @Override
  public Condition visit(SingleExpressionPredicate.Not expression) {
    return DSL.not(expression.expression().accept(this));
  }

  @Override
  public Condition visit(DoubleExpressionPredicate.And expression) {
    Condition left = expression.left().accept(this);
    Condition right = expression.right().accept(this);
    return DSL.and(left, right);
  }

  @Override
  public Condition visit(DoubleExpressionPredicate.Or expression) {
    Condition left = expression.left().accept(this);
    Condition right = expression.right().accept(this);
    return DSL.or(left, right);
  }
}
