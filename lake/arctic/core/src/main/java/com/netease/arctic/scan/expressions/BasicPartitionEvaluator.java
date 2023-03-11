/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.netease.arctic.scan.expressions;

import org.apache.commons.lang3.StringUtils;
import org.apache.iceberg.PartitionField;
import org.apache.iceberg.PartitionSpec;
import org.apache.iceberg.expressions.BoundPredicate;
import org.apache.iceberg.expressions.Expression;
import org.apache.iceberg.expressions.ExpressionVisitors;
import org.apache.iceberg.expressions.Expressions;
import org.apache.iceberg.expressions.Projections;
import org.apache.iceberg.expressions.UnboundPredicate;

import java.util.List;


public class BasicPartitionEvaluator extends Projections.ProjectionEvaluator {

  private final PartitionSpec spec;

  public BasicPartitionEvaluator(PartitionSpec spec) {
    this.spec = spec;
  }

  @Override
  public Expression project(Expression expr) {
    // projections assume that there are no NOT nodes in the expression tree. to ensure that this
    // is the case, the expression is rewritten to push all NOT nodes down to the expression
    // leaf nodes.
    // this is necessary to ensure that the default expression returned when a predicate can't be
    // projected is correct.
    return ExpressionVisitors.visit(ExpressionVisitors.visit(expr, RewriteNot.get()), this);
  }

  @Override
  public Expression alwaysTrue() {
    return Expressions.alwaysTrue();
  }

  @Override
  public Expression alwaysFalse() {
    return Expressions.alwaysFalse();
  }

  @Override
  public Expression not(Expression result) {
    throw new UnsupportedOperationException("[BUG] project called on expression with a not");
  }

  @Override
  public Expression and(Expression leftResult, Expression rightResult) {
    return Expressions.and(leftResult, rightResult);
  }

  @Override
  public Expression or(Expression leftResult, Expression rightResult) {
    return Expressions.or(leftResult, rightResult);
  }

  @Override
  public <T> Expression predicate(UnboundPredicate<T> pred) {
    Expression result = Expressions.alwaysTrue();
    String expressionName = pred.ref().name();
    if (StringUtils.isNotEmpty(expressionName)) {
      List<PartitionField> parts = spec().getFieldsBySourceId(
          spec().schema().asStruct().field(expressionName).fieldId()
      );
      return parts.size() > 0 ? pred : result;
    }
    return result;
  }

  @Override
  public <T> Expression predicate(BoundPredicate<T> pred) {
    throw new IllegalStateException("Found already bound predicate: " + pred);
  }

  PartitionSpec spec() {
    return spec;
  }

}
