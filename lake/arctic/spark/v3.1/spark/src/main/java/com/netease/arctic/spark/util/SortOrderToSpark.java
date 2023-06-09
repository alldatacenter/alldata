/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *  *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *  *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.netease.arctic.spark.util;

import org.apache.iceberg.NullOrder;
import org.apache.iceberg.SortDirection;
import org.apache.iceberg.transforms.SortOrderVisitor;
import org.apache.spark.sql.connector.expressions.Expression;
import org.apache.spark.sql.connector.expressions.Expressions;

public class SortOrderToSpark implements SortOrderVisitor<Expression> {
  final ExpressionHelper expressionHelper;

  public SortOrderToSpark(ExpressionHelper expressionHelper) {
    this.expressionHelper = expressionHelper;
  }

  @Override
  public Expression field(String sourceName, int sourceId, SortDirection direction, NullOrder nullOrder) {
    return expressionHelper.sort(Expressions.column(sourceName), ascending(direction));
  }

  @Override
  public Expression bucket(String sourceName, int sourceId, int width, SortDirection direction, NullOrder nullOrder) {
    return expressionHelper.sort(Expressions.bucket(width, sourceName), ascending(direction));
  }

  @Override
  public Expression truncate(String sourceName, int sourceId, int width, SortDirection direction, NullOrder nullOrder) {
    return expressionHelper.sort(expressionHelper.truncate(sourceName, width), ascending(direction));
  }

  @Override
  public Expression year(String sourceName, int sourceId, SortDirection direction, NullOrder nullOrder) {
    return expressionHelper.sort(Expressions.years(sourceName), ascending(direction));
  }

  @Override
  public Expression month(String sourceName, int sourceId, SortDirection direction, NullOrder nullOrder) {
    return expressionHelper.sort(Expressions.months(sourceName), ascending(direction));
  }

  @Override
  public Expression day(String sourceName, int sourceId, SortDirection direction, NullOrder nullOrder) {
    return expressionHelper.sort(Expressions.days(sourceName), ascending(direction));
  }

  @Override
  public Expression hour(String sourceName, int sourceId, SortDirection direction, NullOrder nullOrder) {
    return expressionHelper.sort(Expressions.hours(sourceName), ascending(direction));
  }

  private boolean ascending(SortDirection direction) {
    return direction == SortDirection.ASC;
  }
}
