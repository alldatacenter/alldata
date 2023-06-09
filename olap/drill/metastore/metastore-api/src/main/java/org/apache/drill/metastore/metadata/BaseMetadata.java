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
package org.apache.drill.metastore.metadata;

import org.apache.drill.common.expression.SchemaPath;
import org.apache.drill.exec.record.metadata.ColumnMetadata;
import org.apache.drill.exec.record.metadata.TupleMetadata;
import org.apache.drill.metastore.components.tables.TableMetadataUnit;
import org.apache.drill.metastore.statistics.ColumnStatistics;
import org.apache.drill.metastore.statistics.StatisticsHolder;
import org.apache.drill.metastore.statistics.StatisticsKind;
import org.apache.drill.metastore.util.SchemaPathUtils;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Common provider of tuple schema, column metadata, and statistics for table, partition, file or row group.
 */
public abstract class BaseMetadata implements Metadata {

  public static final long UNDEFINED_TIME = -1;

  protected final TableInfo tableInfo;
  protected final MetadataInfo metadataInfo;
  protected final TupleMetadata schema;
  protected final Map<SchemaPath, ColumnStatistics<?>> columnsStatistics;
  protected final Map<String, StatisticsHolder<?>> metadataStatistics;
  protected final long lastModifiedTime;

  protected <T extends BaseMetadataBuilder<T>> BaseMetadata(BaseMetadataBuilder<T> builder) {
    this.tableInfo = builder.tableInfo;
    this.metadataInfo = builder.metadataInfo;
    this.schema = builder.schema;
    this.columnsStatistics = builder.columnsStatistics;
    this.metadataStatistics = builder.metadataStatistics.stream()
        .collect(Collectors.toMap(
            statistic -> statistic.getStatisticsKind().getName(),
            Function.identity(),
            (a, b) -> a.getStatisticsKind().isExact() ? a : b));
    this.lastModifiedTime = builder.lastModifiedTime;
  }

  @Override
  public Map<SchemaPath, ColumnStatistics<?>> getColumnsStatistics() {
    return columnsStatistics;
  }

  @Override
  public ColumnStatistics<?> getColumnStatistics(SchemaPath columnName) {
    return columnsStatistics.get(columnName);
  }

  @Override
  public TupleMetadata getSchema() {
    return schema;
  }

  @Override
  @SuppressWarnings("unchecked")
  public <V> V getStatistic(StatisticsKind<V> statisticsKind) {
    StatisticsHolder<V> statisticsHolder = (StatisticsHolder<V>)
        metadataStatistics.get(statisticsKind.getName());
    return statisticsHolder != null ? statisticsHolder.getStatisticsValue() : null;
  }

  @Override
  public boolean containsExactStatistics(StatisticsKind<?> statisticsKind) {
    StatisticsHolder<?> statisticsHolder = metadataStatistics.get(statisticsKind.getName());
    return statisticsHolder != null && statisticsHolder.getStatisticsKind().isExact();
  }

  @Override
  public <V> V getStatisticsForColumn(SchemaPath columnName, StatisticsKind<V> statisticsKind) {
    return columnsStatistics.get(columnName).get(statisticsKind);
  }

  @Override
  public ColumnMetadata getColumn(SchemaPath name) {
    return SchemaPathUtils.getColumnMetadata(name, schema);
  }

  @Override
  public TableInfo getTableInfo() {
    return tableInfo;
  }

  @Override
  public MetadataInfo getMetadataInfo() {
    return metadataInfo;
  }

  /**
   * Allows to check the time, when any files were modified.
   * It is in Unix Timestamp, unit of measurement is millisecond.
   * Undefined time value will be indicated using {@link #UNDEFINED_TIME} value.
   *
   * @return last modified time of files
   */
  public long getLastModifiedTime() {
    return lastModifiedTime;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof BaseMetadata)) {
      return false;
    }
    BaseMetadata that = (BaseMetadata) o;
    return lastModifiedTime == that.lastModifiedTime
        && Objects.equals(tableInfo, that.tableInfo)
        && Objects.equals(metadataInfo, that.metadataInfo)
        && Objects.equals(columnsStatistics, that.columnsStatistics)
        && Objects.equals(metadataStatistics, that.metadataStatistics)
        && (schema == that.schema
            || (schema != null && schema.isEquivalent(that.schema)));
  }

  @Override
  public int hashCode() {
    return Objects.hash(tableInfo, metadataInfo, schema,
        columnsStatistics, metadataStatistics, lastModifiedTime);
  }

  @Override
  public TableMetadataUnit toMetadataUnit() {
    TableMetadataUnit.Builder builder = TableMetadataUnit.builder();

    tableInfo.toMetadataUnitBuilder(builder);
    metadataInfo.toMetadataUnitBuilder(builder);

    if (schema != null) {
      builder.schema(schema.jsonString());
    }

    builder.columnsStatistics(columnsStatistics.entrySet().stream()
      .collect(Collectors.toMap(
        entry -> entry.getKey().toString(),
        entry -> entry.getValue().jsonString(),
        (o, n) -> n)));

    builder.metadataStatistics(metadataStatistics.values().stream()
      .map(StatisticsHolder::jsonString)
      .collect(Collectors.toList()));

    builder.lastModifiedTime(lastModifiedTime);

    toMetadataUnitBuilder(builder);
    return builder.build();
  }

  protected abstract void toMetadataUnitBuilder(TableMetadataUnit.Builder builder);

  protected abstract BaseMetadataBuilder<?> toBuilder();

  public static abstract class BaseMetadataBuilder<T extends BaseMetadataBuilder<T>> {
    protected TableInfo tableInfo;
    protected MetadataInfo metadataInfo;
    protected TupleMetadata schema;
    protected Map<SchemaPath, ColumnStatistics<?>> columnsStatistics;
    protected Collection<StatisticsHolder<?>> metadataStatistics;
    protected long lastModifiedTime = UNDEFINED_TIME;

    public T tableInfo(TableInfo tableInfo) {
      this.tableInfo = tableInfo;
      return self();
    }

    public T metadataInfo(MetadataInfo metadataInfo) {
      this.metadataInfo = metadataInfo;
      return self();
    }

    public T schema(TupleMetadata schema) {
      this.schema = schema;
      return self();
    }

    public T columnsStatistics(Map<SchemaPath, ColumnStatistics<?>> columnsStatistics) {
      this.columnsStatistics = columnsStatistics;
      return self();
    }

    public T metadataStatistics(Collection<StatisticsHolder<?>> metadataStatistics) {
      this.metadataStatistics = metadataStatistics;
      return self();
    }

    public T lastModifiedTime(long lastModifiedTime) {
      this.lastModifiedTime = lastModifiedTime;
      return self();
    }

    public T metadataUnit(TableMetadataUnit unit) {
      tableInfo(TableInfo.builder().metadataUnit(unit).build());
      metadataInfo(MetadataInfo.builder().metadataUnit(unit).build());
      schema(TupleMetadata.of(unit.schema()));

      if (unit.columnsStatistics() != null) {
        columnsStatistics(unit.columnsStatistics().entrySet().stream()
          .collect(Collectors.toMap(
            entry -> SchemaPath.parseFromString(entry.getKey()),
            entry -> ColumnStatistics.of(entry.getValue()),
            (o, n) -> n)));
      }

      if (unit.metadataStatistics() != null) {
        metadataStatistics(unit.metadataStatistics().stream()
          .map(StatisticsHolder::of)
          .collect(Collectors.toList()));
      }

      if (unit.lastModifiedTime() != null) {
        lastModifiedTime(unit.lastModifiedTime());
      }

      return metadataUnitInternal(unit);
    }

    protected void checkRequiredValues() {
      Objects.requireNonNull(tableInfo, "tableInfo was not set");
      Objects.requireNonNull(metadataInfo, "metadataInfo was not set");
      Objects.requireNonNull(columnsStatistics, "columnsStatistics were not set");
      Objects.requireNonNull(metadataStatistics, "metadataStatistics were not set");
    }

    public abstract BaseMetadata build();

    protected abstract T self();

    protected abstract T metadataUnitInternal(TableMetadataUnit unit);
  }
}
