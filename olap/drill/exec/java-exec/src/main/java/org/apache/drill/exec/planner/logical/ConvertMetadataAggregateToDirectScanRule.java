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
import org.apache.calcite.rel.RelNode;
import org.apache.drill.common.expression.PathSegment;
import org.apache.drill.common.expression.SchemaPath;
import org.apache.drill.exec.expr.IsPredicate;
import org.apache.drill.exec.metastore.ColumnNamesOptions;
import org.apache.drill.exec.metastore.analyze.AnalyzeColumnUtils;
import org.apache.drill.exec.metastore.analyze.MetadataAggregateContext;
import org.apache.drill.exec.metastore.analyze.MetastoreAnalyzeConstants;
import org.apache.drill.exec.physical.base.GroupScan;
import org.apache.drill.exec.physical.base.ScanStats;
import org.apache.drill.exec.planner.physical.PlannerSettings;
import org.apache.drill.exec.planner.physical.PrelUtil;
import org.apache.drill.exec.record.metadata.ColumnMetadata;
import org.apache.drill.exec.record.metadata.DictColumnMetadata;
import org.apache.drill.exec.record.metadata.TupleMetadata;
import org.apache.drill.exec.store.ColumnExplorer;
import org.apache.drill.exec.store.ColumnExplorer.ImplicitFileColumns;
import org.apache.drill.exec.store.dfs.DrillFileSystem;
import org.apache.drill.exec.store.dfs.FormatSelection;
import org.apache.drill.exec.store.direct.DirectGroupScan;
import org.apache.drill.exec.store.parquet.BaseParquetMetadataProvider;
import org.apache.drill.exec.store.parquet.ParquetGroupScan;
import org.apache.drill.exec.store.pojo.DynamicPojoRecordReader;
import org.apache.drill.exec.util.ImpersonationUtil;
import org.apache.drill.exec.util.Utilities;
import org.apache.drill.metastore.metadata.MetadataType;
import org.apache.drill.metastore.metadata.RowGroupMetadata;
import org.apache.drill.metastore.statistics.ColumnStatistics;
import org.apache.drill.metastore.statistics.ColumnStatisticsKind;
import org.apache.drill.metastore.statistics.ExactStatisticsConstants;
import org.apache.drill.metastore.statistics.StatisticsKind;
import org.apache.drill.metastore.statistics.TableStatisticsKind;
import org.apache.drill.shaded.guava.com.google.common.collect.HashBasedTable;
import org.apache.drill.shaded.guava.com.google.common.collect.Multimap;
import org.apache.drill.shaded.guava.com.google.common.collect.Table;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Rule which converts
 *
 * <pre>
 *   MetadataAggRel(metadataLevel=ROW_GROUP)
 *   \
 *   DrillScanRel
 * </pre>
 * <p/>
 * plan into
 * <pre>
 *   DrillDirectScanRel
 * </pre>
 * where {@link DrillDirectScanRel} is populated with row group metadata.
 * <p/>
 * For the case when aggregate level is not ROW_GROUP, resulting plan will be the following:
 *
 * <pre>
 *   MetadataAggRel(metadataLevel=FILE (or another non-ROW_GROUP value), createNewAggregations=false)
 *   \
 *   DrillDirectScanRel
 * </pre>
 */
public class ConvertMetadataAggregateToDirectScanRule extends RelOptRule {
  private static final Logger logger = LoggerFactory.getLogger(ConvertMetadataAggregateToDirectScanRule.class);

  public static final ConvertMetadataAggregateToDirectScanRule INSTANCE =
      new ConvertMetadataAggregateToDirectScanRule();

  public ConvertMetadataAggregateToDirectScanRule() {
    super(
        RelOptHelper.some(MetadataAggRel.class, RelOptHelper.any(DrillScanRel.class)),
        DrillRelFactories.LOGICAL_BUILDER, "ConvertMetadataAggregateToDirectScanRule");
  }

  @Override
  public void onMatch(RelOptRuleCall call) {
    MetadataAggRel agg = call.rel(0);
    DrillScanRel scan = call.rel(1);

    GroupScan oldGrpScan = scan.getGroupScan();
    PlannerSettings settings = PrelUtil.getPlannerSettings(call.getPlanner());

    // Only apply the rule for parquet group scan and for the case when required column metadata is present
    if (!(oldGrpScan instanceof ParquetGroupScan)
        || (oldGrpScan.getTableMetadata().getInterestingColumns() != null
          && !oldGrpScan.getTableMetadata().getInterestingColumns().containsAll(agg.getContext().interestingColumns()))) {
      return;
    }

    try {
      DirectGroupScan directScan = buildDirectScan(agg.getContext().interestingColumns(), scan, settings);
      if (directScan == null) {
        logger.warn("Unable to use parquet metadata for ANALYZE since some required metadata is absent within parquet metadata");
        return;
      }

      RelNode converted = new DrillDirectScanRel(scan.getCluster(), scan.getTraitSet().plus(DrillRel.DRILL_LOGICAL),
          directScan, scan.getRowType());
      if (agg.getContext().metadataLevel() != MetadataType.ROW_GROUP) {
        MetadataAggregateContext updatedContext = agg.getContext().toBuilder()
            .createNewAggregations(false)
            .build();
        converted = new MetadataAggRel(agg.getCluster(), agg.getTraitSet(), converted, updatedContext);
      }

      call.transformTo(converted);
    } catch (Exception e) {
      logger.warn("Unable to use parquet metadata for ANALYZE: {}", e.getMessage(), e);
    }
  }

  private DirectGroupScan buildDirectScan(List<SchemaPath> interestingColumns, DrillScanRel scan, PlannerSettings settings) throws IOException {
    DrillTable drillTable = Utilities.getDrillTable(scan.getTable());

    ColumnNamesOptions columnNamesOptions = new ColumnNamesOptions(settings.getOptions());

    // populates schema to be used when adding record values
    FormatSelection selection = (FormatSelection) drillTable.getSelection();

    // adds partition columns to the schema
    Map<String, Class<?>> schema = ColumnExplorer.getPartitionColumnNames(selection.getSelection(), columnNamesOptions).stream()
        .collect(Collectors.toMap(
            Function.identity(),
            s -> String.class,
            (o, n) -> n));

    // adds internal implicit columns to the schema

    schema.put(MetastoreAnalyzeConstants.SCHEMA_FIELD, String.class);
    schema.put(MetastoreAnalyzeConstants.LOCATION_FIELD, String.class);
    schema.put(columnNamesOptions.rowGroupIndex(), String.class);
    schema.put(columnNamesOptions.rowGroupStart(), String.class);
    schema.put(columnNamesOptions.rowGroupLength(), String.class);
    schema.put(columnNamesOptions.lastModifiedTime(), String.class);

    return populateRecords(interestingColumns, schema, scan, columnNamesOptions);
  }

  /**
   * Populates records list with row group metadata.
   */
  private DirectGroupScan populateRecords(Collection<SchemaPath> interestingColumns, Map<String, Class<?>> schema,
      DrillScanRel scan, ColumnNamesOptions columnNamesOptions) throws IOException {
    ParquetGroupScan parquetGroupScan = (ParquetGroupScan) scan.getGroupScan();
    DrillTable drillTable = Utilities.getDrillTable(scan.getTable());

    Multimap<Path, RowGroupMetadata> rowGroupsMetadataMap = parquetGroupScan.getMetadataProvider().getRowGroupsMetadataMap();

    Table<String, Integer, Object> recordsTable = HashBasedTable.create();
    FormatSelection selection = (FormatSelection) drillTable.getSelection();
    List<String> partitionColumnNames = ColumnExplorer.getPartitionColumnNames(selection.getSelection(), columnNamesOptions);

    FileSystem rawFs = selection.getSelection().getSelectionRoot().getFileSystem(new Configuration());
    DrillFileSystem fileSystem =
        ImpersonationUtil.createFileSystem(ImpersonationUtil.getProcessUserName(), rawFs.getConf());

    int rowIndex = 0;
    for (Map.Entry<Path, RowGroupMetadata> rgEntry : rowGroupsMetadataMap.entries()) {
      Path path = rgEntry.getKey();
      RowGroupMetadata rowGroupMetadata = rgEntry.getValue();
      List<String> partitionValues = ColumnExplorer.listPartitionValues(path, selection.getSelection().getSelectionRoot(), false);
      for (int i = 0; i < partitionValues.size(); i++) {
        String partitionColumnName = partitionColumnNames.get(i);
        recordsTable.put(partitionColumnName, rowIndex, partitionValues.get(i));
      }

      recordsTable.put(MetastoreAnalyzeConstants.LOCATION_FIELD, rowIndex, ImplicitFileColumns.FQN.getValue(path));
      recordsTable.put(columnNamesOptions.rowGroupIndex(), rowIndex, String.valueOf(rowGroupMetadata.getRowGroupIndex()));

      if (interestingColumns == null) {
        interestingColumns = rowGroupMetadata.getColumnsStatistics().keySet();
      }

      // populates record list with row group column metadata
      for (SchemaPath schemaPath : interestingColumns) {
        ColumnStatistics<?> columnStatistics = rowGroupMetadata.getColumnsStatistics().get(schemaPath);

        // do not gather statistics for array columns as it is not supported by Metastore
        if (containsArrayColumn(rowGroupMetadata.getSchema(), schemaPath)) {
          continue;
        }

        if (IsPredicate.isNullOrEmpty(columnStatistics)) {
          logger.debug("Statistics for {} column wasn't found within {} row group.", schemaPath, path);
          return null;
        }
        for (StatisticsKind<?> statisticsKind : AnalyzeColumnUtils.COLUMN_STATISTICS_FUNCTIONS.keySet()) {
          Object statsValue;
          if (statisticsKind.getName().equalsIgnoreCase(TableStatisticsKind.ROW_COUNT.getName())) {
            statsValue = TableStatisticsKind.ROW_COUNT.getValue(rowGroupMetadata);
          } else if (statisticsKind.getName().equalsIgnoreCase(ColumnStatisticsKind.NON_NULL_VALUES_COUNT.getName())) {
            statsValue = TableStatisticsKind.ROW_COUNT.getValue(rowGroupMetadata) - ColumnStatisticsKind.NULLS_COUNT.getFrom(columnStatistics);
          } else {
            statsValue = columnStatistics.get(statisticsKind);
          }
          String columnStatisticsFieldName = AnalyzeColumnUtils.getColumnStatisticsFieldName(schemaPath.toExpr(), statisticsKind);
          if (statsValue != null) {
            schema.putIfAbsent(
                columnStatisticsFieldName,
                statsValue.getClass());
            recordsTable.put(columnStatisticsFieldName, rowIndex, statsValue);
          } else {
            recordsTable.put(columnStatisticsFieldName, rowIndex, BaseParquetMetadataProvider.NULL_VALUE);
          }
        }
      }

      // populates record list with row group metadata
      for (StatisticsKind<?> statisticsKind : AnalyzeColumnUtils.META_STATISTICS_FUNCTIONS.keySet()) {
        String metadataStatisticsFieldName = AnalyzeColumnUtils.getMetadataStatisticsFieldName(statisticsKind);
        Object statisticsValue = rowGroupMetadata.getStatistic(statisticsKind);

        if (statisticsValue != null) {
          schema.putIfAbsent(metadataStatisticsFieldName, statisticsValue.getClass());
          recordsTable.put(metadataStatisticsFieldName, rowIndex, statisticsValue);
        } else {
          recordsTable.put(metadataStatisticsFieldName, rowIndex, BaseParquetMetadataProvider.NULL_VALUE);
        }
      }

      // populates record list internal columns
      recordsTable.put(MetastoreAnalyzeConstants.SCHEMA_FIELD, rowIndex, rowGroupMetadata.getSchema().jsonString());
      recordsTable.put(columnNamesOptions.rowGroupStart(), rowIndex, Long.toString(rowGroupMetadata.getStatistic(() -> ExactStatisticsConstants.START)));
      recordsTable.put(columnNamesOptions.rowGroupLength(), rowIndex, Long.toString(rowGroupMetadata.getStatistic(() -> ExactStatisticsConstants.LENGTH)));
      recordsTable.put(columnNamesOptions.lastModifiedTime(), rowIndex, String.valueOf(fileSystem.getFileStatus(path).getModificationTime()));

      rowIndex++;
    }

    // DynamicPojoRecordReader requires LinkedHashMap with fields order
    // which corresponds to the value position in record list.
    LinkedHashMap<String, Class<?>> orderedSchema = new LinkedHashMap<>();
    for (String s : recordsTable.rowKeySet()) {
      Class<?> clazz = schema.get(s);
      if (clazz != null) {
        orderedSchema.put(s, clazz);
      } else {
        return null;
      }
    }

    IntFunction<List<Object>> collectRecord = currentIndex -> orderedSchema.keySet().stream()
        .map(column -> recordsTable.get(column, currentIndex))
        .map(value -> value != BaseParquetMetadataProvider.NULL_VALUE ? value : null)
        .collect(Collectors.toList());

    List<List<Object>> records = IntStream.range(0, rowIndex)
        .mapToObj(collectRecord)
        .collect(Collectors.toList());

    DynamicPojoRecordReader<?> reader = new DynamicPojoRecordReader<>(orderedSchema, records);

    ScanStats scanStats = new ScanStats(ScanStats.GroupScanProperty.EXACT_ROW_COUNT, records.size(), 1, schema.size());

    return new DirectGroupScan(reader, scanStats);
  }

  /**
   * Checks whether schema path contains array segment.
   *
   * @param schema tuple schema
   * @param schemaPath schema path
   * @return {@code true} if any segment in the schema path is an array, {@code false} otherwise
   */
  private static boolean containsArrayColumn(TupleMetadata schema, SchemaPath schemaPath) {
    PathSegment currentPath = schemaPath.getRootSegment();
    ColumnMetadata columnMetadata = schema.metadata(currentPath.getNameSegment().getPath());
    while (columnMetadata != null) {
      if (columnMetadata.isArray()) {
        return true;
      } else if (columnMetadata.isMap()) {
        currentPath = currentPath.getChild();
        columnMetadata = columnMetadata.tupleSchema().metadata(currentPath.getNameSegment().getPath());
      } else if (columnMetadata.isDict()) {
        currentPath = currentPath.getChild();
        columnMetadata = ((DictColumnMetadata) columnMetadata).valueColumnMetadata();
      } else {
        return false;
      }
    }
    return false;
  }
}
