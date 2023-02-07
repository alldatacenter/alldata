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
package org.apache.drill.exec.metastore.analyze;

import org.apache.calcite.sql.SqlKind;
import org.apache.drill.common.types.TypeProtos;
import org.apache.drill.metastore.statistics.BaseStatisticsKind;
import org.apache.drill.metastore.statistics.ColumnStatisticsKind;
import org.apache.drill.metastore.statistics.ExactStatisticsConstants;
import org.apache.drill.metastore.statistics.StatisticsKind;
import org.apache.drill.metastore.statistics.TableStatisticsKind;
import org.apache.drill.shaded.guava.com.google.common.collect.ImmutableMap;

import java.util.Map;

public class AnalyzeColumnUtils {
  private static final String COLUMN_SEPARATOR = "$";

  public static final Map<StatisticsKind<?>, SqlKind> COLUMN_STATISTICS_FUNCTIONS = ImmutableMap.<StatisticsKind<?>, SqlKind>builder()
      .put(ColumnStatisticsKind.MAX_VALUE, SqlKind.MAX)
      .put(ColumnStatisticsKind.MIN_VALUE, SqlKind.MIN)
      .put(ColumnStatisticsKind.NON_NULL_VALUES_COUNT, SqlKind.COUNT)
      .put(TableStatisticsKind.ROW_COUNT, SqlKind.COUNT)
      .build();

  public static final Map<StatisticsKind<?>, TypeProtos.MinorType> COLUMN_STATISTICS_TYPES = ImmutableMap.<StatisticsKind<?>, TypeProtos.MinorType>builder()
      .put(ColumnStatisticsKind.NON_NULL_VALUES_COUNT, TypeProtos.MinorType.BIGINT)
      .put(TableStatisticsKind.ROW_COUNT, TypeProtos.MinorType.BIGINT)
      .build();

  public static final Map<StatisticsKind<?>, SqlKind> META_STATISTICS_FUNCTIONS = ImmutableMap.<StatisticsKind<?>, SqlKind>builder()
      .put(TableStatisticsKind.ROW_COUNT, SqlKind.COUNT)
      .build();

  /**
   * Returns actual column name obtained form intermediate name which includes statistics kind and other analyze-specific info.
   * <p>
   * Example: column which corresponds to max statistics value for {@code `o_shippriority`} column is {@code column$maxValue$`o_shippriority`}.
   * This method will return escaped actual column name: {@code `o_shippriority`}.
   *
   * @param fullName the source of actual column name
   * @return actual column name
   */
  public static String getColumnName(String fullName) {
    return fullName.substring(fullName.indexOf(COLUMN_SEPARATOR, fullName.indexOf(COLUMN_SEPARATOR) + 1) + 1);
  }

  /**
   * Returns {@link StatisticsKind} instance obtained form intermediate field name.
   *
   * @param fullName the source of {@link StatisticsKind} to obtain
   * @return {@link StatisticsKind} instance
   */
  public static StatisticsKind<?> getStatisticsKind(String fullName) {
    String statisticsIdentifier = fullName.split("\\" + COLUMN_SEPARATOR)[1];
    switch (statisticsIdentifier) {
      case ExactStatisticsConstants.MIN_VALUE:
        return ColumnStatisticsKind.MIN_VALUE;
      case ExactStatisticsConstants.MAX_VALUE:
        return ColumnStatisticsKind.MAX_VALUE;
      case ExactStatisticsConstants.NULLS_COUNT:
        return ColumnStatisticsKind.NULLS_COUNT;
      case ExactStatisticsConstants.NON_NULL_VALUES_COUNT:
        return ColumnStatisticsKind.NON_NULL_VALUES_COUNT;
      case ExactStatisticsConstants.ROW_COUNT:
        return TableStatisticsKind.ROW_COUNT;
    }
    return new BaseStatisticsKind<>(statisticsIdentifier, false);
  }

  /**
   * Returns analyze-specific field name for column statistics which includes
   * actual column name and statistics kind information.
   * <p>
   * Example: analyze-specific field name for column {@code `o_shippriority`}
   * and statistics {@code MAX_VALUE} is the following: {@code column$maxValue$`o_shippriority`}.
   *
   * @param columnName     name of the column
   * @param statisticsKind statistics kind
   * @return analyze-specific field name which includes actual column name and statistics kind information
   */
  public static String getColumnStatisticsFieldName(String columnName, StatisticsKind<?> statisticsKind) {
    return String.format("column%1$s%2$s%1$s%3$s", COLUMN_SEPARATOR, statisticsKind.getName(), columnName);
  }

  /**
   * Returns analyze-specific field name for metadata statistics which includes statistics kind information.
   * <p>
   * Example: analyze-specific field name for statistics {@code ROW_COUNT} is the following: {@code metadata$rowCount}.
   *
   * @param statisticsKind statistics kind
   * @return analyze-specific field name for metadata statistics
   */
  public static String getMetadataStatisticsFieldName(StatisticsKind<?> statisticsKind) {
    return String.format("metadata%s%s", COLUMN_SEPARATOR, statisticsKind.getName());
  }

  /**
   * Checks whether specified field name is analyze-specific field for column statistics.
   *
   * @param fieldName name of the field to check
   * @return {@code true} if specified field name is analyze-specific field for column statistics
   */
  public static boolean isColumnStatisticsField(String fieldName) {
    return fieldName.startsWith("column" + COLUMN_SEPARATOR);
  }

  /**
   * Checks whether specified field name is analyze-specific field for metadata statistics.
   * @param fieldName name of the field to check
   * @return {@code true} if specified field name is analyze-specific field for metadata statistics
   */
  public static boolean isMetadataStatisticsField(String fieldName) {
    return fieldName.startsWith("metadata" + COLUMN_SEPARATOR);
  }
}
