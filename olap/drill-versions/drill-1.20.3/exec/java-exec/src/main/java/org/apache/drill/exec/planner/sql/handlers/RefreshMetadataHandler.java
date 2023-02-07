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
package org.apache.drill.exec.planner.sql.handlers;

import org.apache.calcite.schema.SchemaPlus;
import org.apache.calcite.schema.Table;
import org.apache.calcite.sql.SqlIdentifier;
import org.apache.calcite.sql.SqlLiteral;
import org.apache.calcite.sql.SqlNode;
import org.apache.calcite.sql.SqlNodeList;
import org.apache.calcite.sql.parser.SqlParserPos;
import org.apache.drill.common.expression.SchemaPath;
import org.apache.drill.common.logical.FormatPluginConfig;
import org.apache.drill.exec.physical.PhysicalPlan;
import org.apache.drill.exec.planner.logical.DrillTable;
import org.apache.drill.exec.planner.sql.DirectPlan;
import org.apache.drill.exec.planner.sql.SchemaUtilites;
import org.apache.drill.exec.planner.sql.parser.SqlRefreshMetadata;
import org.apache.drill.exec.store.dfs.DrillFileSystem;
import org.apache.drill.exec.store.dfs.FileSelection;
import org.apache.drill.exec.store.dfs.FormatSelection;
import org.apache.drill.exec.store.dfs.NamedFormatPluginConfig;
import org.apache.drill.exec.store.parquet.ParquetFormatConfig;
import org.apache.drill.exec.store.parquet.ParquetReaderConfig;
import org.apache.drill.exec.store.parquet.metadata.Metadata;
import org.apache.drill.exec.util.ImpersonationUtil;
import org.apache.drill.exec.work.foreman.ForemanSetupException;
import org.apache.hadoop.fs.Path;

import java.util.HashSet;
import java.util.Set;

import static org.apache.drill.exec.planner.sql.SchemaUtilites.findSchema;

public class RefreshMetadataHandler extends DefaultSqlHandler {
  private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(RefreshMetadataHandler.class);

  public RefreshMetadataHandler(SqlHandlerConfig config) {
    super(config);
  }

  private PhysicalPlan direct(boolean outcome, String message, Object... values){
    return DirectPlan.createDirectPlan(context, outcome, String.format(message, values));
  }

  private PhysicalPlan notSupported(String tbl){
    return direct(false, "Table %s does not support metadata refresh. Support is currently limited to directory-based Parquet tables.", tbl);
  }

  @Override
  public PhysicalPlan getPlan(SqlNode sqlNode) throws ForemanSetupException {
    final SqlRefreshMetadata refreshTable = unwrap(sqlNode, SqlRefreshMetadata.class);

    try {

      final SchemaPlus schema = findSchema(config.getConverter().getDefaultSchema(),
          refreshTable.getSchemaPath());

      if (schema == null) {
        return direct(false, "Storage plugin or workspace does not exist [%s]",
            SchemaUtilites.SCHEMA_PATH_JOINER.join(refreshTable.getSchemaPath()));
      }

      final String tableName = refreshTable.getName();
      final SqlNodeList columnList = getColumnList(refreshTable);
      final Set<SchemaPath> columnSet = getColumnRootSegments(columnList);
      final SqlLiteral allColumns = refreshTable.getAllColumns();

      if (tableName.contains("*") || tableName.contains("?")) {
        return direct(false, "Glob path %s not supported for metadata refresh", tableName);
      }

      final Table table = schema.getTable(tableName);

      if (table == null) {
        return direct(false, "Table %s does not exist.", tableName);
      }

      if (!(table instanceof DrillTable)) {
        return notSupported(tableName);
      }


      final DrillTable drillTable = (DrillTable) table;

      final Object selection = drillTable.getSelection();

      if (selection instanceof FileSelection && ((FileSelection) selection).isEmptyDirectory()) {
        return direct(false, "Table %s is empty and doesn't contain any parquet files.", tableName);
      }

      if (!(selection instanceof FormatSelection)) {
        return notSupported(tableName);
      }

      final FormatSelection formatSelection = (FormatSelection) selection;

      FormatPluginConfig formatConfig = formatSelection.getFormat();
      if (!((formatConfig instanceof ParquetFormatConfig) ||
          ((formatConfig instanceof NamedFormatPluginConfig) &&
            ((NamedFormatPluginConfig) formatConfig).getName().equals("parquet")))) {
        return notSupported(tableName);
      }

      // Always create filesystem object using process user, since it owns the metadata file
      final DrillFileSystem fs = ImpersonationUtil.createFileSystem(ImpersonationUtil.getProcessUserName(),
        drillTable.getPlugin().getFormatPlugin(formatConfig).getFsConf());

      final Path selectionRoot = formatSelection.getSelection().getSelectionRoot();
      if (!fs.getFileStatus(selectionRoot).isDirectory()) {
        return notSupported(tableName);
      }

      if (!(formatConfig instanceof ParquetFormatConfig)) {
        formatConfig = new ParquetFormatConfig();
      }

      final ParquetReaderConfig readerConfig = ParquetReaderConfig.builder()
        .withFormatConfig((ParquetFormatConfig) formatConfig)
        .withOptions(context.getOptions())
        .build();
      Metadata.createMeta(fs, selectionRoot, readerConfig, allColumns.booleanValue(), columnSet);
      return direct(true, "Successfully updated metadata for table %s.", tableName);

    } catch(Exception e) {
      logger.error("Failed to update metadata for table '{}'", refreshTable.getName(), e);
      return DirectPlan.createDirectPlan(context, false, String.format("Error: %s", e.getMessage()));
    }
  }

  private Set<SchemaPath> getColumnRootSegments(SqlNodeList columnList) {
    Set<SchemaPath> columnSet = new HashSet<>();
    if (columnList != null) {
      for (SqlNode column : columnList.getList()) {
        // Add only the root segment. Collect metadata for all the columns under that root segment
        columnSet.add(
            SchemaPath.getSimplePath(
                SchemaPath.parseFromString(
                        column.toSqlString(null, true).getSql())
                    .getRootSegmentPath()));
      }
    }
    return columnSet;
  }

  /**
   * Generates the column list specified in the Refresh statement
   * @param sqlrefreshMetadata sql parse node representing refresh statement
   * @return list of columns specified in the refresh command
   */
  private SqlNodeList getColumnList(final SqlRefreshMetadata sqlrefreshMetadata) {
    SqlNodeList columnList = sqlrefreshMetadata.getFieldList();
    if (SqlNodeList.isEmptyList(columnList)) {
      columnList = new SqlNodeList(SqlParserPos.ZERO);
      columnList.add(new SqlIdentifier(SchemaPath.STAR_COLUMN.rootName(), SqlParserPos.ZERO));
    }
    return columnList;
  }

}
