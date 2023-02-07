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
package org.apache.drill.exec.planner.cost;

import java.io.IOException;
import org.apache.calcite.rel.core.Aggregate;
import org.apache.calcite.rel.core.Filter;
import org.apache.calcite.rel.core.Join;
import org.apache.calcite.rel.core.TableScan;
import org.apache.calcite.rel.core.Union;
import org.apache.calcite.rel.metadata.ReflectiveRelMetadataProvider;
import org.apache.calcite.rel.metadata.RelMdRowCount;
import org.apache.calcite.rel.metadata.RelMetadataProvider;
import org.apache.calcite.rel.metadata.RelMetadataQuery;
import org.apache.calcite.schema.Table;
import org.apache.calcite.util.BuiltInMethod;
import org.apache.calcite.util.ImmutableBitSet;
import org.apache.drill.exec.planner.common.DrillLimitRelBase;
import org.apache.drill.exec.planner.common.DrillRelOptUtil;
import org.apache.drill.exec.planner.common.DrillScanRelBase;
import org.apache.drill.exec.planner.logical.DrillTable;
import org.apache.drill.exec.planner.physical.AggPrelBase;
import org.apache.drill.exec.planner.physical.PlannerSettings;
import org.apache.drill.exec.planner.physical.PrelUtil;
import org.apache.drill.exec.util.Utilities;
import org.apache.drill.metastore.statistics.TableStatisticsKind;


public class DrillRelMdRowCount extends RelMdRowCount{
  private static final DrillRelMdRowCount INSTANCE = new DrillRelMdRowCount();

  public static final RelMetadataProvider SOURCE = ReflectiveRelMetadataProvider.reflectiveSource(BuiltInMethod.ROW_COUNT.method, INSTANCE);

  private static final Double DEFAULT_SCAN_ROW_COUNT = 1e9;

  @Override
  public Double getRowCount(Aggregate rel, RelMetadataQuery mq) {
    ImmutableBitSet groupKey = ImmutableBitSet.range(rel.getGroupCount());

    if (groupKey.isEmpty()) {
      return 1.0;
    } else if (!DrillRelOptUtil.guessRows(rel) &&
          rel instanceof AggPrelBase &&
          ((AggPrelBase) rel).getOperatorPhase() == AggPrelBase.OperatorPhase.PHASE_1of2) {
      // Phase 1 Aggregate would return rows in the range [NDV, input_rows]. Hence, use the
      // existing estimate of 1/10 * input_rows
        double rowCount = mq.getRowCount(rel.getInput()) / 10;
        Double ndv = mq.getDistinctRowCount(rel.getInput(), groupKey, null);
        // Use max of NDV and input_rows/10
        if (ndv != null) {
          rowCount = Math.max(ndv, rowCount);
        }
        // Grouping sets multiply
        rowCount *= rel.getGroupSets().size();
        return rowCount;
      } else {
      return super.getRowCount(rel, mq);
    }
  }

  public double getRowCount(DrillLimitRelBase rel, RelMetadataQuery mq) {
    return rel.estimateRowCount(mq);
  }

  @Override
  public Double getRowCount(Union rel, RelMetadataQuery mq) {
    return rel.estimateRowCount(mq);
  }

  @Override
  public Double getRowCount(Join rel, RelMetadataQuery mq) {
    return rel.estimateRowCount(mq);
  }

  @Override
  public Double getRowCount(Filter rel, RelMetadataQuery mq) {
    // Need capped selectivity estimates. See the Filter getRows() method
    return rel.getRows();
  }

  @Override
  public Double getRowCount(TableScan rel, RelMetadataQuery mq) {
    DrillTable table = Utilities.getDrillTable(rel.getTable());
    PlannerSettings settings = PrelUtil.getSettings(rel.getCluster());
    // If guessing, return selectivity from RelMDRowCount
    if (DrillRelOptUtil.guessRows(rel)) {
      if (rel instanceof DrillScanRelBase
        || rel.getTable().unwrap(Table.class).getStatistic().getRowCount() != null) {
        return super.getRowCount(rel, mq);
      } else {
        // if table doesn't have row count statistics, return large row count
        // to make sure that limit will be pushed down
        return DEFAULT_SCAN_ROW_COUNT;
      }
    }
    // Return rowcount from statistics, if available. Otherwise, delegate to parent.
    try {
      if (table != null
          && table.getGroupScan().getTableMetadata() != null
          && TableStatisticsKind.HAS_DESCRIPTIVE_STATISTICS.getValue(table.getGroupScan().getTableMetadata())) {
          /* For GroupScan rely on accurate count from the scan, if available, instead of
           * statistics since partition pruning/filter pushdown might have occurred.
           * e.g. ParquetGroupScan returns accurate rowcount. The other way would be to
           * iterate over the rowgroups present in the GroupScan to compute the rowcount.
           */
        if (!table.getGroupScan().getScanStats(settings).getGroupScanProperty().hasExactRowCount()) {
          return TableStatisticsKind.EST_ROW_COUNT.getValue(table.getGroupScan().getTableMetadata());
        } else {
          if (!(rel instanceof DrillScanRelBase)) {
            return table.getGroupScan().getScanStats(settings).getRecordCount();
          }
        }
      }
    } catch (IOException ex) {
      return super.getRowCount(rel, mq);
    }
    return super.getRowCount(rel, mq);
  }
}
