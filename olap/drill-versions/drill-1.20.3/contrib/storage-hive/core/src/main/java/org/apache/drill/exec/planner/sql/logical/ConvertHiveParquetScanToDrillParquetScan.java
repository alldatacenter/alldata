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
package org.apache.drill.exec.planner.sql.logical;

import org.apache.calcite.plan.RelOptRuleCall;
import org.apache.calcite.rel.type.RelDataType;
import org.apache.calcite.rel.type.RelDataTypeFactory;
import org.apache.calcite.rel.type.RelDataTypeField;
import org.apache.calcite.rex.RexBuilder;
import org.apache.calcite.rex.RexInputRef;
import org.apache.calcite.rex.RexNode;
import org.apache.calcite.sql.type.SqlTypeName;
import org.apache.drill.common.expression.SchemaPath;
import org.apache.drill.exec.ExecConstants;
import org.apache.drill.exec.planner.logical.DrillProjectRel;
import org.apache.drill.exec.planner.logical.DrillScanRel;
import org.apache.drill.exec.planner.logical.RelOptHelper;
import org.apache.drill.exec.planner.physical.PlannerSettings;
import org.apache.drill.exec.planner.physical.PrelUtil;
import org.apache.drill.exec.planner.sql.DrillSqlOperator;
import org.apache.drill.exec.server.options.OptionManager;
import org.apache.drill.exec.store.StoragePluginOptimizerRule;
import org.apache.drill.exec.store.hive.HiveDrillNativeParquetScan;
import org.apache.drill.exec.store.hive.HiveMetadataProvider;
import org.apache.drill.exec.store.hive.HiveReadEntry;
import org.apache.drill.exec.store.hive.HiveScan;
import org.apache.drill.exec.store.parquet.ParquetReaderConfig;
import org.apache.hadoop.hive.metastore.api.FieldSchema;
import org.apache.hadoop.hive.metastore.api.Table;
import org.apache.hadoop.hive.ql.io.parquet.MapredParquetInputFormat;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.apache.drill.exec.store.hive.HiveUtilities.nativeReadersRuleMatches;

/**
 * Convert Hive scan to use Drill's native parquet reader instead of Hive's native reader. It also adds a
 * project to convert/cast the output of Drill's native parquet reader to match the expected output of Hive's
 * native reader.
 */
public class ConvertHiveParquetScanToDrillParquetScan extends StoragePluginOptimizerRule {
  private static final org.slf4j.Logger logger =
      org.slf4j.LoggerFactory.getLogger(ConvertHiveParquetScanToDrillParquetScan.class);

  public static final ConvertHiveParquetScanToDrillParquetScan INSTANCE = new ConvertHiveParquetScanToDrillParquetScan();

  private static final DrillSqlOperator INT96_TO_TIMESTAMP =
      new DrillSqlOperator("convert_fromTIMESTAMP_IMPALA", 1, true, false);

  private static final DrillSqlOperator RTRIM = new DrillSqlOperator("RTRIM", 1, true, false);

  private ConvertHiveParquetScanToDrillParquetScan() {
    super(RelOptHelper.any(DrillScanRel.class), "ConvertHiveScanToHiveDrillNativeScan:Parquet");
  }

  /**
   * {@see org.apache.drill.exec.store.hive.HiveUtilities#nativeReadersRuleMatches}
   */
  @Override
  public boolean matches(RelOptRuleCall call) {
    return nativeReadersRuleMatches(call, MapredParquetInputFormat.class);
  }

  @Override
  public void onMatch(RelOptRuleCall call) {
    try {
      final DrillScanRel hiveScanRel = call.rel(0);
      final HiveScan hiveScan = (HiveScan) hiveScanRel.getGroupScan();

      final PlannerSettings settings = PrelUtil.getPlannerSettings(call.getPlanner());
      final String partitionColumnLabel = settings.getFsPartitionColumnLabel();

      final Table hiveTable = hiveScan.getHiveReadEntry().getTable();
      final HiveReadEntry hiveReadEntry = hiveScan.getHiveReadEntry();

      final HiveMetadataProvider hiveMetadataProvider = new HiveMetadataProvider(hiveScan.getUserName(), hiveReadEntry, hiveScan.getHiveConf());
      final List<HiveMetadataProvider.LogicalInputSplit> logicalInputSplits = hiveMetadataProvider.getInputSplits(hiveReadEntry);

      if (logicalInputSplits.isEmpty()) {
        // table is empty, use original scan
        return;
      }

      final Map<String, String> partitionColMapping = getPartitionColMapping(hiveTable, partitionColumnLabel);
      final DrillScanRel nativeScanRel = createNativeScanRel(partitionColMapping, hiveScanRel, logicalInputSplits, settings.getOptions());
      if (hiveScanRel.getRowType().getFieldCount() == 0) {
        call.transformTo(nativeScanRel);
      } else {
        final DrillProjectRel projectRel = createProjectRel(hiveScanRel, partitionColMapping, nativeScanRel);
        call.transformTo(projectRel);
      }


      /*
        Drill native scan should take precedence over Hive since it's more efficient and faster.
        Hive does not always give correct costing (i.e. for external tables Hive does not have number of rows
        and we calculate them approximately). On the contrary, Drill calculates number of rows exactly
        and thus Hive Scan can be chosen instead of Drill native scan because costings allegedly lower for Hive.
        To ensure Drill native scan will be chosen, reduce Hive scan importance to 0.
       */
      call.getPlanner().setImportance(hiveScanRel, 0.0);
    } catch (final Exception e) {
      logger.warn("Failed to convert HiveScan to HiveDrillNativeParquetScan", e);
    }
  }

  /**
   * Create mapping of Hive partition column to directory column mapping.
   */
  private Map<String, String> getPartitionColMapping(final Table hiveTable, final String partitionColumnLabel) {
    final Map<String, String> partitionColMapping = new HashMap<>();
    int i = 0;
    for (FieldSchema col : hiveTable.getPartitionKeys()) {
      partitionColMapping.put(col.getName(), partitionColumnLabel+i);
      i++;
    }

    return partitionColMapping;
  }

  /**
   * Helper method which creates a DrillScalRel with native HiveScan.
   */
  private DrillScanRel createNativeScanRel(Map<String, String> partitionColMapping,
                                           DrillScanRel hiveScanRel,
                                           List<HiveMetadataProvider.LogicalInputSplit> logicalInputSplits,
                                           OptionManager options) throws IOException {

    final RelDataTypeFactory typeFactory = hiveScanRel.getCluster().getTypeFactory();
    final RelDataType varCharType = typeFactory.createSqlType(SqlTypeName.VARCHAR);

    final List<String> nativeScanColNames = new ArrayList<>();
    final List<RelDataType> nativeScanColTypes = new ArrayList<>();
    for (RelDataTypeField field : hiveScanRel.getRowType().getFieldList()) {
      final String dirColName = partitionColMapping.get(field.getName());
      if (dirColName != null) { // partition column
        nativeScanColNames.add(dirColName);
        nativeScanColTypes.add(varCharType);
      } else {
        nativeScanColNames.add(field.getName());
        nativeScanColTypes.add(field.getType());
      }
    }

    final RelDataType nativeScanRowType = typeFactory.createStructType(nativeScanColTypes, nativeScanColNames);

    // Create the list of projected columns set in HiveScan. The order of this list may not be same as the order of
    // columns in HiveScan row type. Note: If the HiveScan.getColumn() contains a '*', we just need to add it as it is,
    // unlike above where we expanded the '*'. HiveScan and related (subscan) can handle '*'.
    final List<SchemaPath> nativeScanCols = new ArrayList<>();
    for (SchemaPath colName : hiveScanRel.getColumns()) {
      final String partitionCol = partitionColMapping.get(colName.getRootSegmentPath());
      if (partitionCol != null) {
        nativeScanCols.add(SchemaPath.getSimplePath(partitionCol));
      } else {
        nativeScanCols.add(colName);
      }
    }

    final HiveScan hiveScan = (HiveScan) hiveScanRel.getGroupScan();
    final HiveDrillNativeParquetScan nativeHiveScan =
        new HiveDrillNativeParquetScan(
            hiveScan.getUserName(),
            nativeScanCols,
            hiveScan.getStoragePlugin(),
            logicalInputSplits,
            hiveScan.getConfProperties(),
            ParquetReaderConfig.builder().withOptions(options).build());

    return new DrillScanRel(
        hiveScanRel.getCluster(),
        hiveScanRel.getTraitSet(),
        hiveScanRel.getTable(),
        nativeHiveScan,
        nativeScanRowType,
        nativeScanCols);
  }

  /**
   * Create a project that converts the native scan output to expected output of Hive scan.
   */
  private DrillProjectRel createProjectRel(final DrillScanRel hiveScanRel,
      final Map<String, String> partitionColMapping, final DrillScanRel nativeScanRel) {

    final List<RexNode> rexNodes = new ArrayList<>();
    final RexBuilder rb = hiveScanRel.getCluster().getRexBuilder();
    final RelDataType hiveScanRowType = hiveScanRel.getRowType();

    for (String colName : hiveScanRowType.getFieldNames()) {
      final String dirColName = partitionColMapping.get(colName);
      if (dirColName != null) {
        rexNodes.add(createPartitionColumnCast(hiveScanRel, nativeScanRel, colName, dirColName, rb));
      } else {
        rexNodes.add(createColumnFormatConversion(hiveScanRel, nativeScanRel, colName, rb));
      }
    }

    return DrillProjectRel.create(
        hiveScanRel.getCluster(), hiveScanRel.getTraitSet(), nativeScanRel, rexNodes,
        hiveScanRowType /* project rowtype and HiveScanRel rowtype should be the same */);
  }

  /**
   * Apply any data format conversion expressions.
   */
  private RexNode createColumnFormatConversion(DrillScanRel hiveScanRel, DrillScanRel nativeScanRel,
      String colName, RexBuilder rb) {

    RelDataType outputType = hiveScanRel.getRowType().getField(colName, false, false).getType();
    RelDataTypeField inputField = nativeScanRel.getRowType().getField(colName, false, false);
    RexInputRef inputRef = rb.makeInputRef(inputField.getType(), inputField.getIndex());

    PlannerSettings settings = PrelUtil.getPlannerSettings(hiveScanRel.getCluster().getPlanner());
    boolean conversionToTimestampEnabled = settings.getOptions().getBoolean(ExecConstants.PARQUET_READER_INT96_AS_TIMESTAMP);

    if (outputType.getSqlTypeName() == SqlTypeName.TIMESTAMP && !conversionToTimestampEnabled) {
      // TIMESTAMP is stored as INT96 by Hive in ParquetFormat.
      // Used convert_fromTIMESTAMP_IMPALA UDF to convert INT96 format data to TIMESTAMP
      // only for the case when `store.parquet.reader.int96_as_timestamp` is
      // disabled to avoid double conversion after reading value from parquet and here.
      return rb.makeCall(INT96_TO_TIMESTAMP, inputRef);
    }

    return inputRef;
  }

  /**
   * Create a cast for partition column. Partition column is output as "VARCHAR" in native parquet reader. Cast it
   * appropriate type according the partition type in HiveScan.
   */
  private RexNode createPartitionColumnCast(final DrillScanRel hiveScanRel, final DrillScanRel nativeScanRel,
      final String outputColName, final String dirColName, final RexBuilder rb) {

    final RelDataType outputType = hiveScanRel.getRowType().getField(outputColName, false, false).getType();
    final RelDataTypeField inputField = nativeScanRel.getRowType().getField(dirColName, false, false);
    final RexInputRef inputRef =
        rb.makeInputRef(rb.getTypeFactory().createSqlType(SqlTypeName.VARCHAR), inputField.getIndex());
    if (outputType.getSqlTypeName() == SqlTypeName.CHAR) {
      return rb.makeCall(RTRIM, inputRef);
    }

    return rb.makeCast(outputType, inputRef);
  }
}
