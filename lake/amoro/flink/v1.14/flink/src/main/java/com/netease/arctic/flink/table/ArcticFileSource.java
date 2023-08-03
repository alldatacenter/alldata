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

package com.netease.arctic.flink.table;

import com.netease.arctic.flink.util.CompatibleFlinkPropertyUtil;
import com.netease.arctic.table.ArcticTable;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.configuration.ReadableConfig;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.table.api.TableSchema;
import org.apache.flink.table.connector.ChangelogMode;
import org.apache.flink.table.connector.source.DataStreamScanProvider;
import org.apache.flink.table.connector.source.DynamicTableSource;
import org.apache.flink.table.connector.source.ScanTableSource;
import org.apache.flink.table.connector.source.abilities.SupportsFilterPushDown;
import org.apache.flink.table.connector.source.abilities.SupportsLimitPushDown;
import org.apache.flink.table.connector.source.abilities.SupportsProjectionPushDown;
import org.apache.flink.table.connector.source.abilities.SupportsWatermarkPushDown;
import org.apache.flink.table.data.RowData;
import org.apache.flink.table.expressions.ResolvedExpression;
import org.apache.flink.table.types.DataType;
import org.apache.flink.types.RowKind;
import org.apache.flink.util.Preconditions;
import org.apache.iceberg.expressions.Expression;
import org.apache.iceberg.flink.FlinkFilters;
import org.apache.iceberg.relocated.com.google.common.collect.ImmutableList;
import org.apache.iceberg.relocated.com.google.common.collect.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static com.netease.arctic.flink.table.descriptors.ArcticValidator.DIM_TABLE_ENABLE;

/**
 * Flink table api that generates arctic base/change file source operators.
 */
public class ArcticFileSource implements ScanTableSource, SupportsFilterPushDown,
    SupportsProjectionPushDown, SupportsLimitPushDown, SupportsWatermarkPushDown {

  private static final Logger LOG = LoggerFactory.getLogger(ArcticFileSource.class);

  private int[] projectedFields;
  private long limit;
  private List<Expression> filters;
  private ArcticTable table;
  @Nullable
  protected WatermarkStrategy<RowData> watermarkStrategy;

  private final ArcticTableLoader loader;
  private final TableSchema tableSchema;
  private final ReadableConfig readableConfig;

  private ArcticFileSource(ArcticFileSource toCopy) {
    this.loader = toCopy.loader;
    this.tableSchema = toCopy.tableSchema;
    this.projectedFields = toCopy.projectedFields;
    this.limit = toCopy.limit;
    this.filters = toCopy.filters;
    this.readableConfig = toCopy.readableConfig;
    this.table = toCopy.table;
  }

  public ArcticFileSource(ArcticTableLoader loader,
                          TableSchema tableSchema,
                          int[] projectedFields,
                          ArcticTable table,
                          long limit,
                          List<Expression> filters,
                          ReadableConfig readableConfig) {
    this.loader = loader;
    this.tableSchema = tableSchema;
    this.projectedFields = projectedFields;
    this.limit = limit;
    this.table = table;
    this.filters = filters;
    this.readableConfig = readableConfig;
  }

  public ArcticFileSource(ArcticTableLoader loader, TableSchema tableSchema, ArcticTable table,
                          ReadableConfig readableConfig) {
    this(loader, tableSchema, null, table, -1, ImmutableList.of(), readableConfig);
  }

  @Override
  public void applyProjection(int[][] projectFields) {
    this.projectedFields = new int[projectFields.length];
    for (int i = 0; i < projectFields.length; i++) {
      Preconditions.checkArgument(projectFields[i].length == 1,
          "Don't support nested projection now.");
      this.projectedFields[i] = projectFields[i][0];
    }
  }

  private DataStream<RowData> createDataStream(StreamExecutionEnvironment execEnv) {
    return FlinkSource.forRowData()
        .env(execEnv)
        .tableLoader(loader)
        .arcticTable(table)
        .project(getProjectedSchema())
        .limit(limit)
        .filters(filters)
        .flinkConf(readableConfig)
        .watermarkStrategy(watermarkStrategy)
        .build();
  }

  private TableSchema getProjectedSchema() {
    if (projectedFields == null) {
      return tableSchema;
    } else {
      String[] fullNames = tableSchema.getFieldNames();
      DataType[] fullTypes = tableSchema.getFieldDataTypes();

      String[] projectedColumns = Arrays.stream(projectedFields).mapToObj(i -> fullNames[i]).toArray(String[]::new);
      TableSchema.Builder builder = TableSchema.builder().fields(
          projectedColumns,
          Arrays.stream(projectedFields).mapToObj(i -> fullTypes[i]).toArray(DataType[]::new));
      boolean dimTable = CompatibleFlinkPropertyUtil.propertyAsBoolean(table.properties(), DIM_TABLE_ENABLE.key(),
          DIM_TABLE_ENABLE.defaultValue());
      if (dimTable) {
        builder.watermark(tableSchema.getWatermarkSpecs().get(0));
      }

      TableSchema ts = builder.build();
      LOG.info("TableSchema after projection:{}", ts);
      return ts;
    }
  }

  @Override
  public void applyLimit(long newLimit) {
    this.limit = newLimit;
  }

  @Override
  public Result applyFilters(List<ResolvedExpression> flinkFilters) {
    List<ResolvedExpression> acceptedFilters = Lists.newArrayList();
    List<Expression> expressions = Lists.newArrayList();

    for (ResolvedExpression resolvedExpression : flinkFilters) {
      Optional<Expression> icebergExpression = FlinkFilters.convert(resolvedExpression);
      if (icebergExpression.isPresent()) {
        expressions.add(icebergExpression.get());
        acceptedFilters.add(resolvedExpression);
      }
    }

    this.filters = expressions;
    return Result.of(acceptedFilters, flinkFilters);
  }

  @Override
  public boolean supportsNestedProjection() {
    // TODO: support nested projection
    return false;
  }

  @Override
  public ChangelogMode getChangelogMode() {
    if (table.isUnkeyedTable()) {
      return ChangelogMode.insertOnly();
    }
    return ChangelogMode.newBuilder()
        .addContainedKind(RowKind.DELETE)
        .addContainedKind(RowKind.INSERT)
        .addContainedKind(RowKind.UPDATE_AFTER)
        .addContainedKind(RowKind.UPDATE_BEFORE)
        .build();
  }

  @Override
  public ScanRuntimeProvider getScanRuntimeProvider(ScanContext runtimeProviderContext) {
    return new DataStreamScanProvider() {
      @Override
      public DataStream<RowData> produceDataStream(StreamExecutionEnvironment execEnv) {
        return createDataStream(execEnv);
      }

      @Override
      public boolean isBounded() {
        return org.apache.iceberg.flink.source.FlinkSource.isBounded(table.properties());
      }
    };
  }

  @Override
  public DynamicTableSource copy() {
    return new ArcticFileSource(this);
  }

  @Override
  public String asSummaryString() {
    return "Arctic File Source";
  }

  @Override
  public void applyWatermark(WatermarkStrategy<RowData> watermarkStrategy) {
    Configuration conf = Configuration.fromMap(table.properties());
    boolean dimTable = CompatibleFlinkPropertyUtil.propertyAsBoolean(conf, DIM_TABLE_ENABLE);
    if (!dimTable) {
      this.watermarkStrategy = watermarkStrategy;
    }
  }
}