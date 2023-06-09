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
package org.apache.drill.metastore.util;

import org.apache.drill.common.expression.SchemaPath;
import org.apache.drill.common.types.TypeProtos;
import org.apache.drill.metastore.metadata.BaseMetadata;
import org.apache.drill.metastore.metadata.TableMetadata;
import org.apache.drill.metastore.statistics.CollectableColumnStatisticsKind;
import org.apache.drill.metastore.statistics.ColumnStatistics;
import org.apache.drill.metastore.statistics.ColumnStatisticsKind;
import org.apache.drill.metastore.statistics.StatisticsHolder;
import org.apache.drill.metastore.statistics.TableStatisticsKind;
import org.apache.drill.shaded.guava.com.google.common.primitives.UnsignedBytes;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class TableMetadataUtils {

  /**
   * Returns {@link Comparator} instance considering specified {@code type}.
   *
   * @param type type of the column
   * @return {@link Comparator} instance
   */
  @SuppressWarnings("unchecked")
  public static <T> Comparator<T> getComparator(TypeProtos.MinorType type) {
    switch (type) {
      case INTERVALDAY:
      case INTERVAL:
      case INTERVALYEAR:
        // This odd cast is needed because this method is poorly designed.
        // The method is statically typed to type T. But, the type
        // is selected dynamically at runtime via the type parameter.
        // As a result, we are casting a comparator to the WRONG type
        // in some cases. We have to remove the byte[] type, then force
        // the type to T. This works because we should only use this
        // case if T is byte[]. But, this is a horrible hack and should
        // be fixed.
        return (Comparator<T>) (Comparator<?>)
            Comparator.nullsFirst(UnsignedBytes.lexicographicalComparator());
      case UINT1:
        return (Comparator<T>)
            Comparator.nullsFirst(UnsignedBytes::compare);
      case UINT2:
      case UINT4:
        return (Comparator<T>) Comparator.nullsFirst(Integer::compareUnsigned);
      case UINT8:
        return (Comparator<T>) Comparator.nullsFirst(Long::compareUnsigned);
      default:
        return (Comparator<T>) getNaturalNullsFirstComparator();
    }
  }

  /**
   * Returns "natural order" comparator which threads nulls as min values.
   *
   * @param <T> type to compare
   * @return "natural order" comparator
   */
  public static <T extends Comparable<T>> Comparator<T> getNaturalNullsFirstComparator() {
    return Comparator.nullsFirst(Comparator.naturalOrder());
  }

  /**
   * Merges list of specified metadata into the map of {@link ColumnStatistics} with columns as keys.
   *
   * @param <T>                 type of metadata to collect
   * @param metadataList        list of metadata to be merged
   * @param columns             set of columns whose statistics should be merged
   * @param statisticsToCollect kinds of statistics that should be collected
   * @return list of merged metadata
   */
  @SuppressWarnings({ "unchecked", "rawtypes" })
  public static <T extends BaseMetadata> Map<SchemaPath, ColumnStatistics<?>> mergeColumnsStatistics(
      Collection<T> metadataList, Set<SchemaPath> columns, List<CollectableColumnStatisticsKind<?>> statisticsToCollect) {
    Map<SchemaPath, ColumnStatistics<?>> columnsStatistics = new HashMap<>();

    for (SchemaPath column : columns) {
      List<ColumnStatistics<?>> statisticsList = new ArrayList<>();
      for (T metadata : metadataList) {
        ColumnStatistics<?> statistics = metadata.getColumnsStatistics().get(column);
        if (statistics == null) {
          // schema change happened, set statistics which represents all nulls
          statistics = new ColumnStatistics(
              Collections.singletonList(
                  new StatisticsHolder<>(TableStatisticsKind.ROW_COUNT.getValue(metadata), ColumnStatisticsKind.NULLS_COUNT)));
        }
        statisticsList.add(statistics);
      }
      List<StatisticsHolder<?>> statisticsHolders = new ArrayList<>();
      for (CollectableColumnStatisticsKind<?> statisticsKind : statisticsToCollect) {
        Object mergedStatistic = statisticsKind.mergeStatistics(statisticsList);
        statisticsHolders.add(new StatisticsHolder<>(mergedStatistic, statisticsKind));
      }
      Iterator<ColumnStatistics<?>> iterator = statisticsList.iterator();
      // Use INT if statistics wasn't provided
      TypeProtos.MinorType comparatorType = iterator.hasNext() ? iterator.next().getComparatorType() : TypeProtos.MinorType.INT;
      columnsStatistics.put(column, new ColumnStatistics<>(statisticsHolders, comparatorType));
    }
    return columnsStatistics;
  }

  /**
   * Updates row count and column nulls count for specified table metadata and returns new {@link TableMetadata} instance with updated statistics.
   *
   * @param tableMetadata table statistics to update
   * @param statistics    list of statistics whose row count should be considered
   * @return new {@link TableMetadata} instance with updated statistics
   */
  public static TableMetadata updateRowCount(TableMetadata tableMetadata, Collection<? extends BaseMetadata> statistics) {
    List<StatisticsHolder<?>> newStats = new ArrayList<>();

    newStats.add(new StatisticsHolder<>(TableStatisticsKind.ROW_COUNT.mergeStatistics(statistics), TableStatisticsKind.ROW_COUNT));

    Set<SchemaPath> columns = tableMetadata.getColumnsStatistics().keySet();

    Map<SchemaPath, ColumnStatistics<?>> columnsStatistics =
        mergeColumnsStatistics(statistics, columns,
            Collections.singletonList(ColumnStatisticsKind.NULLS_COUNT));

    return tableMetadata.cloneWithStats(columnsStatistics, newStats);
  }
}
