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
package org.apache.drill.metastore.mongo.transform;

import com.mongodb.client.model.Filters;
import org.apache.drill.metastore.expressions.DoubleExpressionPredicate;
import org.apache.drill.metastore.expressions.FilterExpression;
import org.apache.drill.metastore.expressions.IsPredicate;
import org.apache.drill.metastore.expressions.ListPredicate;
import org.apache.drill.metastore.expressions.SimplePredicate;
import org.apache.drill.metastore.expressions.SingleExpressionPredicate;
import org.bson.conversions.Bson;

/**
 * Visits {@link FilterExpression} implementations and transforms them into {@link Bson} implementations.
 */
public class FilterExpressionVisitor implements FilterExpression.Visitor<Bson> {

  private static final FilterExpressionVisitor INSTANCE = new FilterExpressionVisitor();

  public static FilterExpression.Visitor<Bson> get() {
    return INSTANCE;
  }

  @Override
  public Bson visit(SimplePredicate.Equal<?> expression) {
    return Filters.eq(expression.column().columnName(), expression.value());
  }

  @Override
  public Bson visit(SimplePredicate.NotEqual<?> expression) {
    return Filters.ne(expression.column().columnName(), expression.value());
  }

  @Override
  public Bson visit(SimplePredicate.LessThan<?> expression) {
    return Filters.lt(expression.column().columnName(), expression.value());
  }

  @Override
  public Bson visit(SimplePredicate.LessThanOrEqual<?> expression) {
    return Filters.lte(expression.column().columnName(), expression.value());
  }

  @Override
  public Bson visit(SimplePredicate.GreaterThan<?> expression) {
    return Filters.gt(expression.column().columnName(), expression.value());
  }

  @Override
  public Bson visit(SimplePredicate.GreaterThanOrEqual<?> expression) {
    return Filters.gte(expression.column().columnName(), expression.value());
  }

  @Override
  public Bson visit(ListPredicate.In<?> expression) {
    return Filters.in(expression.column().columnName(), expression.values());
  }

  @Override
  public Bson visit(ListPredicate.NotIn<?> expression) {
    return Filters.nin(expression.column().columnName(), expression.values());
  }

  @Override
  public Bson visit(IsPredicate.IsNull expression) {
    return Filters.exists(expression.column().columnName(), false);
  }

  @Override
  public Bson visit(IsPredicate.IsNotNull expression) {
    return Filters.exists(expression.column().columnName());
  }

  @Override
  public Bson visit(SingleExpressionPredicate.Not expression) {
    Bson child = expression.expression().accept(this);
    return Filters.not(child);
  }

  @Override
  public Bson visit(DoubleExpressionPredicate.And expression) {
    Bson right = expression.right().accept(this);
    Bson left = expression.left().accept(this);
    return Filters.and(right, left);
  }

  @Override
  public Bson visit(DoubleExpressionPredicate.Or expression) {
    Bson right = expression.right().accept(this);
    Bson left = expression.left().accept(this);
    return Filters.or(right, left);
  }
}
