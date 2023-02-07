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
package org.apache.drill.metastore.statistics;

import org.apache.drill.metastore.metadata.BaseMetadata;

import java.util.Comparator;
import java.util.List;

/**
 * Implementation of {@link CollectableColumnStatisticsKind} which contain base
 * column statistics kinds with implemented {@code mergeStatistics()} method.
 */
public class ColumnStatisticsKind<T> extends BaseStatisticsKind<T> implements CollectableColumnStatisticsKind<T> {

  /**
   * Column statistics kind which represents nulls count for the specific column.
   */
  public static final ColumnStatisticsKind<Long> NULLS_COUNT =
      new ColumnStatisticsKind<Long>(ExactStatisticsConstants.NULLS_COUNT, true) {
        @Override
        public Long mergeStatistics(List<? extends ColumnStatistics<?>> statisticsList) {
          long nullsCount = 0;
          for (ColumnStatistics<?> statistics : statisticsList) {
            Long statNullsCount = statistics.get(this);
            if (statNullsCount == null || statNullsCount == Statistic.NO_COLUMN_STATS) {
              return Statistic.NO_COLUMN_STATS;
            } else {
              nullsCount += statNullsCount;
            }
          }
          return nullsCount;
        }

        @Override
        public Long getFrom(ColumnStatistics<?> metadata) {
          Long rowCount = super.getFrom(metadata);
          return rowCount != null ? rowCount : Statistic.NO_COLUMN_STATS;
        }
      };

  /**
   * Column statistics kind which represents min value of the specific column.
   */
  public static final ColumnStatisticsKind<Object> MIN_VALUE =
      new ColumnStatisticsKind<Object>(ExactStatisticsConstants.MIN_VALUE, true) {
        @Override
        @SuppressWarnings("unchecked")
        public Object mergeStatistics(List<? extends ColumnStatistics<?>> statisticsList) {
          Object minValue = null;
          for (ColumnStatistics<?> statistics : statisticsList) {
            Object statMinValue = getValueStatistic(statistics);
            Comparator<Object> comp = (Comparator<Object>) statistics.getValueComparator();
            if (statMinValue != null && (comp.compare(minValue, statMinValue) > 0 || minValue == null)) {
              minValue = statMinValue;
            }
          }
          return minValue;
        }
      };

  /**
   * Column statistics kind which represents max value of the specific column.
   */
  public static final ColumnStatisticsKind<Object> MAX_VALUE =
      new ColumnStatisticsKind<Object>(ExactStatisticsConstants.MAX_VALUE, true) {
        @Override
        @SuppressWarnings("unchecked")
        public Object mergeStatistics(List<? extends ColumnStatistics<?>> statisticsList) {
          Object maxValue = null;
          for (ColumnStatistics<?> statistics : statisticsList) {
            Object statMaxValue = getValueStatistic(statistics);
            Comparator<Object> comp = (Comparator<Object>) statistics.getValueComparator();
            if (statMaxValue != null && comp.compare(maxValue, statMaxValue) < 0) {
              maxValue = statMaxValue;
            }
          }
          return maxValue;
        }
      };

  /**
   * Column statistics kind which represents exact number of non-null values for the specific column.
   */
  public static final ColumnStatisticsKind<Long> NON_NULL_VALUES_COUNT =
      new ColumnStatisticsKind<Long>(ExactStatisticsConstants.NON_NULL_VALUES_COUNT, true) {
        @Override
        public Long mergeStatistics(List<? extends ColumnStatistics<?>> statisticsList) {
          long nonNullRowCount = 0;
          for (ColumnStatistics<?> statistics : statisticsList) {
            Long nnRowCount = statistics.get(this);
            if (nnRowCount != null) {
              nonNullRowCount += nnRowCount;
            }
          }
          return nonNullRowCount;
        }
      };

  /**
   * Column statistics kind which represents estimated number of non-null values for the specific column.
   */
  public static final ColumnStatisticsKind<Double> NON_NULL_COUNT =
      new ColumnStatisticsKind<Double>(Statistic.NNROWCOUNT, false) {
        @Override
        public Double mergeStatistics(List<? extends ColumnStatistics<?>> statisticsList) {
          double nonNullRowCount = 0;
          for (ColumnStatistics<?> statistics : statisticsList) {
            Double nnRowCount = statistics.get(this);
            if (nnRowCount != null) {
              nonNullRowCount += nnRowCount;
            }
          }
          return nonNullRowCount;
        }
      };

  /**
   * Column statistics kind which represents total row count for the specific column.
   */
  public static final ColumnStatisticsKind<Double> ROWCOUNT =
      new ColumnStatisticsKind<Double>(Statistic.ROWCOUNT, false) {
        @Override
        public Double mergeStatistics(List<? extends ColumnStatistics<?>> statisticsList) {
          double rowCount = 0;
          for (ColumnStatistics<?> statistics : statisticsList) {
            Double count = getFrom(statistics);
            if (count != null) {
              rowCount += count;
            }
          }
          return rowCount;
        }
      };

  /**
   * Column statistics kind which represents number of distinct values for the specific column.
   */
  public static final ColumnStatisticsKind<Double> NDV =
    new ColumnStatisticsKind<>(Statistic.NDV, false);

  /**
   * Column statistics kind which is the width of the specific column.
   */
  public static final ColumnStatisticsKind<?> AVG_WIDTH =
      new ColumnStatisticsKind<>(Statistic.AVG_WIDTH, false);

  /**
   * Column statistics kind which is the histogram of the specific column.
   */
  public static final ColumnStatisticsKind<Histogram> HISTOGRAM =
    new ColumnStatisticsKind<>("histogram", false);

  ColumnStatisticsKind(String statisticKey, boolean exact) {
    super(statisticKey, exact);
  }

  /**
   * Returns value which corresponds to this statistic kind,
   * obtained from specified {@link BaseMetadata}.
   *
   * @param metadata the source of statistic value
   * @return value which corresponds to this statistic kind
   */
  public T getFrom(ColumnStatistics<?> metadata) {
    return metadata.get(this);
  }

  @SuppressWarnings("unchecked")
  public <V> V getValueStatistic(ColumnStatistics<V> metadata) {
    return (V) metadata.get(this);
  }

  @Override
  public T mergeStatistics(List<? extends ColumnStatistics<?>> statistics) {
    throw new UnsupportedOperationException("Cannot merge statistics for " + statisticKey);
  }
}
