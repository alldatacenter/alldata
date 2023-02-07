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

import org.apache.drill.metastore.metadata.Metadata;
import org.apache.drill.metastore.metadata.MetadataType;

import java.util.Collection;

/**
 * Implementation of {@link CollectableColumnStatisticsKind} which contain base
 * table statistics kinds with implemented {@code mergeStatistics()} method.
 */
public class TableStatisticsKind<T> extends BaseStatisticsKind<T> implements CollectableTableStatisticsKind<T> {

  /**
   * Table statistics kind which represents row count for the specific table.
   */
  public static final TableStatisticsKind<Long> ROW_COUNT =
      new TableStatisticsKind<Long>(ExactStatisticsConstants.ROW_COUNT, true) {
        @Override
        public Long mergeStatistics(Collection<? extends Metadata> statistics) {
          long rowCount = 0;
          for (Metadata statistic : statistics) {
            Long statRowCount = getValue(statistic);
            if (statRowCount == Statistic.NO_COLUMN_STATS) {
              rowCount = Statistic.NO_COLUMN_STATS;
              break;
            } else {
              rowCount += statRowCount;
            }
          }
          return rowCount;
        }

        @Override
        public Long getValue(Metadata metadata) {
          Long rowCount = super.getValue(metadata);
          return rowCount != null ? rowCount : Statistic.NO_COLUMN_STATS;
        }
      };

  /**
   * Table statistics kind which represents estimated row count for the specific table.
   */
  public static final TableStatisticsKind<Double> EST_ROW_COUNT =
      new TableStatisticsKind<Double>(Statistic.ROWCOUNT, false) {
        @Override
        public Double mergeStatistics(Collection<? extends Metadata> statisticsList) {
          double rowCount = 0;
          for (Metadata statistics : statisticsList) {
            Double statRowCount = getValue(statistics);
            if (statRowCount != null) {
              rowCount += statRowCount;
            }
          }
          return rowCount;
        }
      };

  /**
   * Table statistics kind which represents estimated row count for the specific table.
   */
  public static final TableStatisticsKind<Boolean> HAS_DESCRIPTIVE_STATISTICS =
      new TableStatisticsKind<Boolean>("hasDescriptiveStatistics", false) {
        @Override
        public Boolean mergeStatistics(Collection<? extends Metadata> statisticsList) {
          for (Metadata statistics : statisticsList) {
            Boolean hasDescriptiveStatistics = statistics.getStatistic(this);
            if (hasDescriptiveStatistics == null || !hasDescriptiveStatistics) {
              return false;
            }
          }
          return Boolean.TRUE;
        }

        @Override
        public Boolean getValue(Metadata metadata) {
          return Boolean.TRUE.equals(metadata.getStatistic(this));
        }
      };

  /**
   * Table statistics kind which represents metadata level for which analyze was produced.
   */
  public static final TableStatisticsKind<MetadataType> ANALYZE_METADATA_LEVEL =
      new TableStatisticsKind<MetadataType>("analyzeMetadataLevel", false) {
        @Override
        public MetadataType mergeStatistics(Collection<? extends Metadata> statisticsList) {
          MetadataType maxMetadataType = MetadataType.ALL;
          for (Metadata statistics : statisticsList) {
            MetadataType metadataType = statistics.getStatistic(this);
            if (metadataType != null && metadataType.compareTo(maxMetadataType) < 0) {
              maxMetadataType = metadataType;
            }
          }
          return maxMetadataType;
        }

        @Override
        public MetadataType getValue(Metadata metadata) {
          return metadata.getStatistic(this);
        }
      };

  public TableStatisticsKind(String statisticKey, boolean exact) {
    super(statisticKey, exact);
  }

  @Override
  public T mergeStatistics(Collection<? extends Metadata> statistics) {
    throw new UnsupportedOperationException("Cannot merge statistics for " + statisticKey);
  }

  /**
   * Returns value which corresponds to this statistic kind,
   * obtained from specified {@link Metadata}.
   *
   * @param metadata the source of statistic value
   * @return value which corresponds to this statistic kind
   */
  public T getValue(Metadata metadata) {
    return metadata.getStatistic(this);
  }
}
