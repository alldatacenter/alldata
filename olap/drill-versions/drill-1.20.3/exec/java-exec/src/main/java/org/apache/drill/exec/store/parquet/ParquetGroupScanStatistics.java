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
package org.apache.drill.exec.store.parquet;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.mutable.MutableLong;
import org.apache.drill.common.expression.SchemaPath;
import org.apache.drill.common.types.TypeProtos;
import org.apache.drill.metastore.statistics.TableStatisticsKind;
import org.apache.drill.metastore.statistics.Statistic;
import org.apache.drill.exec.record.metadata.ColumnMetadata;
import org.apache.drill.metastore.util.SchemaPathUtils;
import org.apache.drill.metastore.metadata.BaseMetadata;
import org.apache.hadoop.fs.Path;
import org.apache.drill.metastore.statistics.ColumnStatistics;
import org.apache.drill.metastore.statistics.ColumnStatisticsKind;
import org.apache.drill.metastore.metadata.LocationProvider;
import org.apache.drill.shaded.guava.com.google.common.collect.HashBasedTable;
import org.apache.drill.shaded.guava.com.google.common.collect.Table;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Holds common statistics about data in parquet group scan,
 * including information about total row count, columns counts, partition columns.
 */
public class ParquetGroupScanStatistics<T extends BaseMetadata & LocationProvider> {

  // map from file names to maps of column name to partition value mappings
  private Table<Path, SchemaPath, Object> partitionValueMap;
  // only for partition columns : value is unique for each partition
  private Map<SchemaPath, TypeProtos.MajorType> partitionColTypeMap;
  // total number of non-null value for each column in parquet files
  private Map<SchemaPath, MutableLong> columnValueCounts;
  // total number of rows (obtained from parquet footer)
  private long rowCount;


  public ParquetGroupScanStatistics(Collection<T> rowGroupInfos) {
    collect(rowGroupInfos);
  }

  public ParquetGroupScanStatistics(ParquetGroupScanStatistics<T> that) {
    this.partitionValueMap = HashBasedTable.create(that.partitionValueMap);
    this.partitionColTypeMap = new HashMap<>(that.partitionColTypeMap);
    this.columnValueCounts = new HashMap<>(that.columnValueCounts);
    this.rowCount = that.rowCount;
  }

  public long getColumnValueCount(SchemaPath column) {
    MutableLong count = columnValueCounts.get(column);
    return count != null ? count.getValue() : 0;
  }

  public List<SchemaPath> getPartitionColumns() {
    return new ArrayList<>(partitionColTypeMap.keySet());
  }

  public TypeProtos.MajorType getTypeForColumn(SchemaPath schemaPath) {
    return partitionColTypeMap.get(schemaPath);
  }

  public long getRowCount() {
    return rowCount;
  }

  public Object getPartitionValue(Path path, SchemaPath column) {
    Object partitionValue = partitionValueMap.get(path, column);
    if (partitionValue == BaseParquetMetadataProvider.NULL_VALUE) {
      return null;
    }
    return partitionValue;
  }

  public Map<Path, Object> getPartitionPaths(SchemaPath column) {
    return partitionValueMap.column(column);
  }

  public void collect(Collection<T> metadataList) {
    resetHolders();
    boolean first = true;
    for (T metadata : metadataList) {
      long localRowCount = TableStatisticsKind.ROW_COUNT.getValue(metadata);
      for (Map.Entry<SchemaPath, ColumnStatistics<?>> columnsStatistics : metadata.getColumnsStatistics().entrySet()) {
        SchemaPath schemaPath = columnsStatistics.getKey();
        ColumnStatistics<?> statistics = columnsStatistics.getValue();
        MutableLong emptyCount = new MutableLong();
        MutableLong previousCount = columnValueCounts.putIfAbsent(schemaPath, emptyCount);
        if (previousCount == null) {
          previousCount = emptyCount;
        }
        Long nullsNum = ColumnStatisticsKind.NULLS_COUNT.getFrom(statistics);
        if (previousCount.longValue() != Statistic.NO_COLUMN_STATS && nullsNum != null && nullsNum != Statistic.NO_COLUMN_STATS) {
          previousCount.add(localRowCount - nullsNum);
        } else {
          previousCount.setValue(Statistic.NO_COLUMN_STATS);
        }
        ColumnMetadata columnMetadata = SchemaPathUtils.getColumnMetadata(schemaPath, metadata.getSchema());
        // DRILL-7934
        // base on metastore/metastore-api/src/main/java/org/apache/drill/metastore/util/SchemaPathUtils.java#145
        // list schema is skipped, so that in this class drill can not get majorType by schemaPath.
        // we can change null type to return false to avoid NullPointerException
        TypeProtos.MajorType majorType = columnMetadata != null ? columnMetadata.majorType() : null;
        boolean partitionColumn = majorType != null
                && checkForPartitionColumn(statistics, first, localRowCount, majorType, schemaPath);
        if (partitionColumn) {
          Object value = partitionValueMap.get(metadata.getPath(), schemaPath);
          Object currentValue = ColumnStatisticsKind.MAX_VALUE.getFrom(statistics);
          if (value != null && value != BaseParquetMetadataProvider.NULL_VALUE) {
            if (value != currentValue) {
              partitionColTypeMap.remove(schemaPath);
            }
          } else {
            // the value of a column with primitive type can not be null,
            // so checks that there are really null value and puts it to the map
            if (localRowCount == ColumnStatisticsKind.NULLS_COUNT.getFrom(statistics)) {
              partitionValueMap.put(metadata.getPath(), schemaPath, BaseParquetMetadataProvider.NULL_VALUE);
            } else {
              partitionValueMap.put(metadata.getPath(), schemaPath, currentValue);
            }
          }
        } else {
          partitionColTypeMap.remove(schemaPath);
        }
      }
      this.rowCount += localRowCount;
      first = false;
    }
  }

  /**
   * Re-init holders eigther during first instance creation or statistics update based on updated list of row groups.
   */
  private void resetHolders() {
    this.partitionValueMap = HashBasedTable.create();
    this.partitionColTypeMap = new HashMap<>();
    this.columnValueCounts = new HashMap<>();
    this.rowCount = 0;
  }

  /**
   * When reading the very first footer, any column is a potential partition column. So for the first footer, we check
   * every column to see if it is single valued, and if so, add it to the list of potential partition columns. For the
   * remaining footers, we will not find any new partition columns, but we may discover that what was previously a
   * potential partition column now no longer qualifies, so it needs to be removed from the list.
   *
   * @param columnStatistics column metadata
   * @param first            if columns first appears in row group
   * @param rowCount         row count
   * @return whether column is a potential partition column
   */
  private boolean checkForPartitionColumn(ColumnStatistics<?> columnStatistics,
                                          boolean first,
                                          long rowCount,
                                          TypeProtos.MajorType type,
                                          SchemaPath schemaPath) {
    if (first) {
      if (hasSingleValue(columnStatistics, rowCount)) {
        partitionColTypeMap.put(schemaPath, type);
        return true;
      } else {
        return false;
      }
    } else {
      if (!partitionColTypeMap.containsKey(schemaPath)) {
        return false;
      } else {
        if (!hasSingleValue(columnStatistics, rowCount)) {
          partitionColTypeMap.remove(schemaPath);
          return false;
        }
        if (ObjectUtils.notEqual(partitionColTypeMap.get(schemaPath), type)) {
          partitionColTypeMap.remove(schemaPath);
          return false;
        }
      }
    }
    return true;
  }

  /**
   * Checks that the column chunk has a single value.
   * ColumnMetadata will have a non-null value iff the minValue and
   * the maxValue for the rowgroup are the same.
   *
   * @param columnStatistics metadata to check
   * @param rowCount         rows count in column chunk
   * @return true if column has single value
   */
  private boolean hasSingleValue(ColumnStatistics<?> columnStatistics, long rowCount) {
    return columnStatistics != null && isSingleVal(columnStatistics, rowCount);
  }

  private boolean isSingleVal(ColumnStatistics<?> columnStatistics, long rowCount) {
    Long numNulls = ColumnStatisticsKind.NULLS_COUNT.getFrom(columnStatistics);
    if (numNulls != null && numNulls != Statistic.NO_COLUMN_STATS) {
      Object min = columnStatistics.get(ColumnStatisticsKind.MIN_VALUE);
      Object max = columnStatistics.get(ColumnStatisticsKind.MAX_VALUE);
      if (min != null) {
        return (numNulls == 0 || numNulls == rowCount) && Objects.deepEquals(min, max);
      } else {
        return numNulls == rowCount && max == null;
      }
    }
    return false;
  }

}
