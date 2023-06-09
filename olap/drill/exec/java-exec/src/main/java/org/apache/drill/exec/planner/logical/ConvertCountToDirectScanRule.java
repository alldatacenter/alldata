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
package org.apache.drill.exec.planner.logical;

import org.apache.calcite.plan.RelOptRule;
import org.apache.calcite.plan.RelOptRuleCall;
import org.apache.calcite.plan.RelOptRuleOperand;
import org.apache.calcite.rel.core.Aggregate;
import org.apache.calcite.rel.core.AggregateCall;
import org.apache.calcite.rel.core.Project;
import org.apache.calcite.rel.core.TableScan;
import org.apache.calcite.rel.type.RelDataType;
import org.apache.calcite.rex.RexInputRef;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.drill.common.expression.SchemaPath;
import org.apache.drill.common.logical.FormatPluginConfig;

import org.apache.drill.exec.physical.base.ScanStats;
import org.apache.drill.metastore.statistics.Statistic;
import org.apache.drill.exec.planner.common.CountToDirectScanUtils;
import org.apache.drill.exec.planner.common.DrillRelOptUtil;

import org.apache.drill.exec.planner.physical.PlannerSettings;
import org.apache.drill.exec.store.ColumnExplorer;
import org.apache.drill.exec.store.dfs.DrillFileSystem;
import org.apache.drill.exec.store.dfs.FileSystemPlugin;
import org.apache.drill.exec.store.dfs.FormatSelection;
import org.apache.drill.exec.store.dfs.NamedFormatPluginConfig;
import org.apache.drill.exec.store.direct.MetadataDirectGroupScan;
import org.apache.drill.exec.store.parquet.ParquetFormatConfig;
import org.apache.drill.exec.store.parquet.ParquetReaderConfig;
import org.apache.drill.exec.store.parquet.metadata.Metadata;
import org.apache.drill.exec.store.parquet.metadata.Metadata_V4;
import org.apache.drill.exec.store.pojo.DynamicPojoRecordReader;
import org.apache.drill.shaded.guava.com.google.common.collect.ImmutableMap;
import org.apache.hadoop.fs.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.Set;

/**
 * <p> This rule is a logical planning counterpart to a corresponding <b>ConvertCountToDirectScanPrule</b>
 * physical rule
 * </p>
 * <p>
 * This rule will convert <b>" select count(*)  as mycount from table "</b>
 * or <b>" select count(not-nullable-expr) as mycount from table "</b> into
 * <pre>
 *    Project(mycount)
 *         \
 *    DirectGroupScan ( PojoRecordReader ( rowCount ))
 *</pre>
 * or <b>" select count(column) as mycount from table "</b> into
 * <pre>
 *      Project(mycount)
 *           \
 *            DirectGroupScan (PojoRecordReader (columnValueCount))
 *</pre>
 * Rule can be applied if query contains multiple count expressions.
 * <b>" select count(column1), count(column2), count(*) from table "</b>
 * </p>
 *
 * <p>
 * The rule utilizes the Parquet Metadata Cache's summary information to retrieve the total row count
 * and the per-column null count.  As such, the rule is only applicable for Parquet tables and only if the
 * metadata cache has been created with the summary information.
 * </p>
 */
public class ConvertCountToDirectScanRule extends RelOptRule {

  public static final RelOptRule AGG_ON_PROJ_ON_SCAN = new ConvertCountToDirectScanRule(
      RelOptHelper.some(Aggregate.class,
                        RelOptHelper.some(Project.class,
                            RelOptHelper.any(TableScan.class))), "Agg_on_proj_on_scan");

  public static final RelOptRule AGG_ON_SCAN = new ConvertCountToDirectScanRule(
      RelOptHelper.some(Aggregate.class,
                            RelOptHelper.any(TableScan.class)), "Agg_on_scan");

  private static final Logger logger = LoggerFactory.getLogger(ConvertCountToDirectScanRule.class);

  private ConvertCountToDirectScanRule(RelOptRuleOperand rule, String id) {
    super(rule, DrillRelFactories.LOGICAL_BUILDER, "ConvertCountToDirectScanRule:" + id);
  }

  @Override
  public void onMatch(RelOptRuleCall call) {
    final Aggregate agg = call.rel(0);
    final TableScan scan = call.rel(call.rels.length - 1);
    final Project project = call.rels.length == 3 ? (Project) call.rel(1) : null;

    // Qualifying conditions for rule:
    //    1) There's no GroupBY key,
    //    2) Agg is not a DISTINCT agg
    //    3) Additional checks are done further below ..
    if (agg.getGroupCount() > 0 ||
        agg.containsDistinctCall()) {
      return;
    }

    DrillTable drillTable = DrillRelOptUtil.getDrillTable(scan);

    if (drillTable == null) {
      logger.debug("Rule does not apply since an eligible drill table instance was not found.");
      return;
    }

    Object selection = drillTable.getSelection();

    if (!(selection instanceof FormatSelection)) {
      logger.debug("Rule does not apply since only Parquet file format is eligible.");
      return;
    }

    PlannerSettings settings = call.getPlanner().getContext().unwrap(PlannerSettings.class);

    //  Rule is applicable only if the statistics for row count and null count are available from the metadata,
    FormatSelection formatSelection = (FormatSelection) selection;

    // Rule cannot be applied if the selection had wildcard since the totalrowcount cannot be read from the parent directory
    if (formatSelection.getSelection().hadWildcard()) {
      logger.debug("Rule does not apply when there is a wild card since the COUNT could not be determined from metadata.");
      return;
    }

    Pair<Boolean, Metadata_V4.MetadataSummary> status = checkMetadataForScanStats(settings, drillTable, formatSelection);
    if (!status.getLeft()) {
      logger.debug("Rule does not apply since MetadataSummary metadata was not found.");
      return;
    }

    Metadata_V4.MetadataSummary metadataSummary = status.getRight();
    Map<String, Long> result = collectCounts(settings, metadataSummary, agg, scan, project);
    logger.trace("Calculated the following aggregate counts: {}", result);

    // if counts could not be determined, rule won't be applied
    if (result.isEmpty()) {
      logger.debug("Rule does not apply since one or more COUNTs could not be determined from metadata.");
      return;
    }

    Path summaryFileName = Metadata.getSummaryFileName(formatSelection.getSelection().getSelectionRoot());

    final RelDataType scanRowType = CountToDirectScanUtils.constructDataType(agg, result.keySet());

    final DynamicPojoRecordReader<Long> reader = new DynamicPojoRecordReader<>(
        CountToDirectScanUtils.buildSchema(scanRowType.getFieldNames()),
        Collections.singletonList(new ArrayList<>(result.values())));

    final ScanStats scanStats = new ScanStats(ScanStats.GroupScanProperty.EXACT_ROW_COUNT, 1, 1, scanRowType.getFieldCount());
    final MetadataDirectGroupScan directScan = new MetadataDirectGroupScan(reader, summaryFileName, 1, scanStats, true, false);

    final DrillDirectScanRel newScan = new DrillDirectScanRel(scan.getCluster(), scan.getTraitSet().plus(DrillRel.DRILL_LOGICAL),
      directScan, scanRowType);

    final DrillProjectRel newProject = new DrillProjectRel(agg.getCluster(), agg.getTraitSet().plus(DrillRel.DRILL_LOGICAL),
      newScan, CountToDirectScanUtils.prepareFieldExpressions(scanRowType), agg.getRowType());

    call.transformTo(newProject);
  }

  private Pair<Boolean, Metadata_V4.MetadataSummary> checkMetadataForScanStats(PlannerSettings settings, DrillTable drillTable,
                                                                               FormatSelection formatSelection) {

    // Currently only support metadata rowcount stats for Parquet tables
    FormatPluginConfig formatConfig = formatSelection.getFormat();
    if (!((formatConfig instanceof ParquetFormatConfig)
      || ((formatConfig instanceof NamedFormatPluginConfig)
      && ((NamedFormatPluginConfig) formatConfig).getName().equals("parquet")))) {
      return new ImmutablePair<>(false, null);
    }

    FileSystemPlugin plugin = (FileSystemPlugin) drillTable.getPlugin();
    DrillFileSystem fs;
    try {
       fs = new DrillFileSystem(plugin.getFormatPlugin(formatSelection.getFormat()).getFsConf());
    } catch (IOException e) {
      logger.warn("Unable to create the file system object for retrieving statistics from metadata cache file ", e);
      return new ImmutablePair<>(false, null);
    }

    // check if the cacheFileRoot has been set: this is needed because after directory pruning, the
    // cacheFileRoot could have been changed and not be the same as the original selectionRoot
    Path selectionRoot = formatSelection.getSelection().getCacheFileRoot() != null ?
            formatSelection.getSelection().getCacheFileRoot() :
            formatSelection.getSelection().getSelectionRoot();

    ParquetReaderConfig parquetReaderConfig= ParquetReaderConfig.builder()
            .withFormatConfig((ParquetFormatConfig) formatConfig)
            .withOptions(settings.getOptions())
            .build();

    Metadata_V4.MetadataSummary metadataSummary = Metadata.getSummary(fs, selectionRoot, false, parquetReaderConfig);

    return metadataSummary != null ? new ImmutablePair<>(true, metadataSummary) :
        new ImmutablePair<>(false, null);
  }

  /**
   * Collects counts for each aggregation call by using the metadata summary information
   * Will return empty result map if was not able to determine count for at least one aggregation call.
   *
   * For each aggregate call will determine if count can be calculated. Collects counts only for COUNT function.
   *   1. First, we get the total row count from the metadata summary.
   *   2. For COUNT(*) and COUNT(<non null column>) and COUNT(<implicit column>), the count = total row count
   *   3. For COUNT(nullable column), count = (total row count - column's null count)
   *   4. Also count can not be calculated for parition columns.
   *   5. For the columns that are not present in the Summary(Non-existent columns), the count = 0
   *
   * @param settings planner options
   * @param metadataSummary metadata summary containing row counts and column counts
   * @param agg aggregate relational expression
   * @param scan scan relational expression
   * @param project project relational expression
   * @return result map where key is count column name, value is count value
   */
  private Map<String, Long> collectCounts(PlannerSettings settings, Metadata_V4.MetadataSummary metadataSummary,
                                          Aggregate agg, TableScan scan, Project project) {
    final Set<String> implicitColumnsNames = ColumnExplorer.initImplicitFileColumns(settings.getOptions()).keySet();
    final long totalRecordCount = metadataSummary.getTotalRowCount();
    final LinkedHashMap<String, Long> result = new LinkedHashMap<>();

    for (int i = 0; i < agg.getAggCallList().size(); i++) {
      AggregateCall aggCall = agg.getAggCallList().get(i);
      long cnt;

      // rule can be applied only for count function, return empty counts
      if (!"count".equalsIgnoreCase(aggCall.getAggregation().getName()) ) {
        return ImmutableMap.of();
      }

      if (CountToDirectScanUtils.containsStarOrNotNullInput(aggCall, agg)) {
        cnt = totalRecordCount;

      } else if (aggCall.getArgList().size() == 1) {
        // count(columnName) ==> Agg ( Scan )) ==> columnValueCount
        int index = aggCall.getArgList().get(0);

        if (project != null) {
          // project in the middle of Agg and Scan : Only when input of AggCall is a RexInputRef in Project, we find the index of Scan's field.
          // For instance,
          // Agg - count($0)
          //  \
          //  Proj - Exp={$1}
          //    \
          //   Scan (col1, col2).
          // return count of "col2" in Scan's metadata, if found.
          if (!(project.getProjects().get(index) instanceof RexInputRef)) {
            return ImmutableMap.of(); // do not apply for all other cases.
          }

          index = ((RexInputRef) project.getProjects().get(index)).getIndex();
        }

        String columnName = scan.getRowType().getFieldNames().get(index).toLowerCase();

        // for implicit column count will be the same as total record count
        if (implicitColumnsNames.contains(columnName)) {
          cnt = totalRecordCount;
        } else {
          SchemaPath simplePath = SchemaPath.getSimplePath(columnName);

          if (ColumnExplorer.isPartitionColumn(settings.getOptions(), simplePath)) {
            return ImmutableMap.of();
          }

          Metadata_V4.ColumnTypeMetadata_v4 columnMetadata = metadataSummary.getColumnTypeInfo(new Metadata_V4.ColumnTypeMetadata_v4.Key(simplePath));

          if (columnMetadata == null) {
            // If the column doesn't exist in the table, row count is set to 0
            cnt = 0;
          } else if (columnMetadata.totalNullCount == Statistic.NO_COLUMN_STATS) {
            // if column stats is not available don't apply this rule, return empty counts
            return ImmutableMap.of();
          } else {
           // count of a nullable column = (total row count - column's null count)
           cnt = totalRecordCount - columnMetadata.totalNullCount;
         }

        }
      } else {
        return ImmutableMap.of();
      }

      String name = "count" + i + "$" + (aggCall.getName() == null ? aggCall.toString() : aggCall.getName());
      result.put(name, cnt);
    }

    return ImmutableMap.copyOf(result);
  }
}
