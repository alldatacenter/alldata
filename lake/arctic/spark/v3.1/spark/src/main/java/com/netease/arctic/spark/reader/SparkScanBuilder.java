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

package com.netease.arctic.spark.reader;

import com.netease.arctic.spark.table.SupportsExtendIdentColumns;
import com.netease.arctic.table.ArcticTable;
import com.netease.arctic.table.UnkeyedTable;
import org.apache.iceberg.MetadataColumns;
import org.apache.iceberg.Schema;
import org.apache.iceberg.exceptions.ValidationException;
import org.apache.iceberg.expressions.Binder;
import org.apache.iceberg.expressions.Expression;
import org.apache.iceberg.expressions.Expressions;
import org.apache.iceberg.relocated.com.google.common.collect.Lists;
import org.apache.iceberg.spark.SparkFilters;
import org.apache.iceberg.spark.SparkSchemaUtil;
import org.apache.iceberg.types.TypeUtil;
import org.apache.iceberg.types.Types;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.connector.read.Scan;
import org.apache.spark.sql.connector.read.ScanBuilder;
import org.apache.spark.sql.connector.read.SupportsPushDownFilters;
import org.apache.spark.sql.connector.read.SupportsPushDownRequiredColumns;
import org.apache.spark.sql.sources.Filter;
import org.apache.spark.sql.types.StructType;
import org.apache.spark.sql.util.CaseInsensitiveStringMap;

import java.util.List;
import java.util.stream.Collectors;


public class SparkScanBuilder implements ScanBuilder, SupportsExtendIdentColumns, SupportsPushDownFilters,
    SupportsPushDownRequiredColumns {
  private static final Filter[] NO_FILTERS = new Filter[0];

  private final ArcticTable table;
  private final CaseInsensitiveStringMap options;

  private final List<String> metaColumns = Lists.newArrayList();

  private Schema schema = null;
  private StructType requestedProjection;
  private final boolean caseSensitive;
  private List<Expression> filterExpressions = null;
  private Filter[] pushedFilters = NO_FILTERS;

  public SparkScanBuilder(SparkSession spark, ArcticTable table, CaseInsensitiveStringMap options) {
    this.table = table;
    this.options = options;
    this.caseSensitive = Boolean.parseBoolean(spark.conf().get("spark.sql.caseSensitive"));
  }

  public SparkScanBuilder(SparkSession spark, ArcticTable table, CaseInsensitiveStringMap options, Schema schema) {
    this.table = table;
    this.options = options;
    this.schema = schema;
    this.caseSensitive = Boolean.parseBoolean(spark.conf().get("spark.sql.caseSensitive"));
  }

  private Schema lazySchemaWithRowIdent() {
    if (schema == null) {
      if (requestedProjection != null) {
        // the projection should include all columns that will be returned,
        // including those only used in filters
        this.schema = SparkSchemaUtil.prune(table.schema(),
            requestedProjection, filterExpression(), caseSensitive);
      } else {
        this.schema = table.schema();
      }
    }

    UnkeyedTable icebergTable;
    if (table.isUnkeyedTable()) {
      icebergTable = table.asUnkeyedTable();
    } else {
      icebergTable = table.asKeyedTable().baseTable();
    }
    // metadata columns
    List<Types.NestedField> fields = metaColumns.stream()
        .distinct()
        .map(column -> MetadataColumns.metadataColumn(icebergTable, column))
        .collect(Collectors.toList());
    if (fields.size() == 1) {
      return schema;
    }
    Schema meta = new Schema(fields);

    return TypeUtil.join(schema, meta);
  }

  private Expression filterExpression() {
    if (filterExpressions != null) {
      return filterExpressions.stream().reduce(Expressions.alwaysTrue(), Expressions::and);
    }
    return Expressions.alwaysTrue();
  }

  @Override
  public Filter[] pushFilters(Filter[] filters) {
    List<Expression> expressions = Lists.newArrayListWithExpectedSize(filters.length);
    List<Filter> pushed = Lists.newArrayListWithExpectedSize(filters.length);

    for (Filter filter : filters) {
      Expression expr = SparkFilters.convert(filter);
      if (expr != null) {
        try {
          Binder.bind(table.schema().asStruct(), expr, caseSensitive);
          expressions.add(expr);
          pushed.add(filter);
        } catch (ValidationException e) {
          // binding to the table schema failed, so this expression cannot be pushed down
        }
      }
    }

    this.filterExpressions = expressions;
    this.pushedFilters = pushed.toArray(new Filter[0]);

    // Spark doesn't support residuals per task, so return all filters
    // to get Spark to handle record-level filtering
    return filters;
  }

  @Override
  public Filter[] pushedFilters() {
    return pushedFilters;
  }

  @Override
  public void pruneColumns(StructType requestedSchema) {
    this.requestedProjection = requestedSchema;
  }

  @Override
  public Scan build() {
    if (table.isKeyedTable()) {
      return new KeyedSparkBatchScan(
          table.asKeyedTable(),
          caseSensitive,
          lazySchemaWithRowIdent(),
          filterExpressions,
          options);
    } else if (table.isUnkeyedTable()) {
      return new UnkeyedSparkBatchScan(
          table.asUnkeyedTable(),
          caseSensitive,
          lazySchemaWithRowIdent(),
          filterExpressions,
          options);
    } else {
      throw new IllegalStateException("Unable to build scan for table: " + table.id().toString() + ", unknown table " +
          "type");
    }
  }

  @Override
  public SupportsExtendIdentColumns withIdentifierColumns() {
    if (table.isUnkeyedTable()) {
      this.metaColumns.addAll(UnkeyedSparkBatchScan.rowIdColumns);
    } else if (table.isKeyedTable()) {
      this.metaColumns.addAll(table.asKeyedTable().primaryKeySpec().fieldNames());
    }
    return this;
  }
}
