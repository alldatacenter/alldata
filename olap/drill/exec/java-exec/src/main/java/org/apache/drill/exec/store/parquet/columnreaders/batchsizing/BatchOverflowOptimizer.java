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
package org.apache.drill.exec.store.parquet.columnreaders.batchsizing;

import java.util.List;
import java.util.Map;

import org.apache.drill.common.map.CaseInsensitiveMap;
import org.apache.drill.exec.record.MaterializedField;
import org.apache.drill.exec.store.parquet.columnreaders.ParquetColumnMetadata;
import org.apache.drill.exec.store.parquet.columnreaders.batchsizing.BatchSizingMemoryUtil.VectorMemoryUsageInfo;
import org.apache.drill.exec.store.parquet.columnreaders.batchsizing.RecordBatchSizerManager.ColumnMemoryInfo;
import org.apache.drill.exec.store.parquet.columnreaders.batchsizing.RecordBatchSizerManager.VarLenColumnBatchStats;

/**
 * A class which uses aggregate statistics to minimize overflow (in terms
 * of size and occurrence).
 */
final class BatchOverflowOptimizer {
  /** A threshold to trigger column memory redistribution based on latest statistics */
  private static final int MAX_NUM_OVERFLOWS = 3;

  /** Field to column memory information map */
  private final Map<String, ColumnMemoryInfo> columnMemoryInfoMap;
  /** Stats for each VL column */
  private final Map<String, ColumnPrecisionStats> columnStatsMap = CaseInsensitiveMap.newHashMap();
  /** Number of batches */
  private int numBatches;
  /** Number of overflows */
  private int numOverflows;
  /** An indicator to track whether a column precision has changed */
  boolean columnPrecisionChanged;

  BatchOverflowOptimizer(Map<String, ColumnMemoryInfo> columnMemoryInfoMap) {
    this.columnMemoryInfoMap = columnMemoryInfoMap;
  }

  void setup() {
    for (ColumnMemoryInfo columnInfo : columnMemoryInfoMap.values()) {
      final ParquetColumnMetadata columnMeta = columnInfo.columnMeta;

      if (!columnMeta.isFixedLength()) {
        columnStatsMap.put(
          columnMeta.getField().getName(),
          new ColumnPrecisionStats(columnMeta.getField())
        );
      }
    }
  }

  boolean onEndOfBatch(int batchNumRecords, List<VarLenColumnBatchStats> batchStats) {

    if (batchNumRecords == 0) {
      return false; // NOOP
    }

    ++numBatches; // increment the number of batches

    // Indicator that an overflow happened
    boolean overflow = false;

    // Reusable container to compute a VL column's amount of data within
    // this batch.
    final VectorMemoryUsageInfo vectorMemoryUsage = new VectorMemoryUsageInfo();

    for (VarLenColumnBatchStats stat : batchStats) {
      final String columnName                   = stat.vector.getField().getName();
      ColumnPrecisionStats columnPrecisionStats = columnStatsMap.get(columnName);
      assert columnPrecisionStats != null;

      if (stat.numValuesRead > batchNumRecords) {
        overflow = true;
      }

      // Compute this column data usage in the last batch; note that we
      // do not account for null values as we are interested in the
      // actual data that is being stored within a batch.
      BatchSizingMemoryUtil.getMemoryUsage(stat.vector, stat.numValuesRead, vectorMemoryUsage);
      final long batchColumnPrecision = Math.max(1, vectorMemoryUsage.dataBytesUsed / stat.numValuesRead);

      double currAvgPrecision = columnPrecisionStats.avgPrecision;
      double newAvgPrecision  = ((numBatches - 1) * currAvgPrecision + batchColumnPrecision) / numBatches;

      if (newAvgPrecision > currAvgPrecision) {
        columnPrecisionStats.avgPrecision = (int) Math.ceil(newAvgPrecision);
        columnPrecisionChanged            = true;
      }
    }

    if (overflow) {
      ++numOverflows;
    }

    if (numBatches == 1 // In the first batch, we only used defaults; we need to update
     || (columnPrecisionChanged && numOverflows >= MAX_NUM_OVERFLOWS) // better stats and overflow occurred
    ) {

      for (ColumnPrecisionStats columnPrecisionStats : columnStatsMap.values()) {
        ColumnMemoryInfo columnInfo = columnMemoryInfoMap.get(columnPrecisionStats.field.getName());
        assert columnInfo != null;

        // update the precision
        columnInfo.columnPrecision   = columnPrecisionStats.avgPrecision;
        columnInfo.columnMemoryQuota.reset();
      }

      // Reset some tracking counters
      numOverflows           = 0;
      columnPrecisionChanged = false;

      return true;
    }

    return false; // NOOP
  }

// ----------------------------------------------------------------------------
// Inner Classes
// ----------------------------------------------------------------------------

  /** Container class which computes the average variable column precision */
  private static final class ColumnPrecisionStats {
    /** Materialized field */
    private final MaterializedField field;
    /** Average column precision */
    private long avgPrecision;

    private ColumnPrecisionStats(MaterializedField field) {
      this.field = field;
    }
  }
}
