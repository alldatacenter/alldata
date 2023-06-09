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

import org.apache.spark.sql.arctic.catalyst.ArcticSpark31CatalystHelper;
import org.apache.spark.sql.catalyst.plans.logical.LogicalPlan;
import org.apache.spark.sql.connector.expressions.Expression;
import org.apache.spark.sql.connector.expressions.Expressions;
import org.apache.spark.sql.connector.expressions.Transform;
import org.apache.spark.sql.connector.iceberg.expressions.NullOrdering;
import org.apache.spark.sql.connector.iceberg.expressions.SortDirection;
import org.apache.spark.sql.connector.iceberg.expressions.SortOrder;

public class ExpressionHelper {

  public Transform bucket(int numBuckets, String... columns) {
    return Expressions.bucket(numBuckets, columns);
  }

  public Transform truncate(String column, int width) {
    return Expressions.apply(
        "truncate",
        Expressions.column(column),
        Expressions.literal(width)
    );
  }

  public Expression sort(final Expression expr, boolean ascending) {
    final SortDirection direction = ascending ? SortDirection.ASCENDING : SortDirection.DESCENDING;
    final NullOrdering nullOrdering = ascending ? NullOrdering.NULLS_FIRST : NullOrdering.NULLS_LAST;
    return new SortOrder() {
      @Override
      public Expression expression() {
        return expr;
      }

      @Override
      public SortDirection direction() {
        return direction;
      }

      @Override
      public NullOrdering nullOrdering() {
        return nullOrdering;
      }

      @Override
      public String describe() {
        return String.format("%s %s %s", expr.describe(), direction, nullOrdering);
      }
    };
  }

  public org.apache.spark.sql.catalyst.expressions.Expression toCatalyst(
      Expression expr, LogicalPlan plan
  ) {
    return ArcticSpark31CatalystHelper.toCatalyst(expr, plan);
  }
}
