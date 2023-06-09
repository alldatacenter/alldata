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

import java.io.IOException;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rel.type.RelDataType;
import org.apache.calcite.schema.Table;
import org.apache.calcite.sql.SqlIdentifier;
import org.apache.calcite.sql.SqlNode;
import org.apache.calcite.sql.SqlNodeList;
import org.apache.calcite.sql.SqlSelect;
import org.apache.calcite.sql.parser.SqlParserPos;
import org.apache.calcite.tools.RelConversionException;
import org.apache.calcite.tools.ValidationException;
import org.apache.drill.common.exceptions.UserException;
import org.apache.drill.common.expression.SchemaPath;
import org.apache.drill.common.logical.FormatPluginConfig;
import org.apache.drill.exec.dotdrill.DotDrillType;
import org.apache.drill.exec.physical.PhysicalPlan;
import org.apache.drill.exec.physical.base.PhysicalOperator;
import org.apache.drill.exec.planner.common.DrillStatsTable;
import org.apache.drill.exec.planner.logical.DrillAnalyzeRel;
import org.apache.drill.exec.planner.logical.DrillRel;
import org.apache.drill.exec.planner.logical.DrillScreenRel;
import org.apache.drill.exec.planner.logical.DrillTable;
import org.apache.drill.exec.planner.logical.DrillWriterRel;
import org.apache.drill.exec.planner.physical.Prel;
import org.apache.drill.exec.planner.sql.SchemaUtilites;
import org.apache.drill.exec.planner.sql.parser.SqlAnalyzeTable;
import org.apache.drill.exec.store.AbstractSchema;
import org.apache.drill.exec.store.dfs.DrillFileSystem;
import org.apache.drill.exec.store.dfs.FileSystemPlugin;
import org.apache.drill.exec.store.dfs.FormatSelection;
import org.apache.drill.exec.store.dfs.NamedFormatPluginConfig;
import org.apache.drill.exec.store.parquet.ParquetFormatConfig;
import org.apache.drill.exec.util.Pointer;
import org.apache.drill.exec.work.foreman.ForemanSetupException;
import org.apache.drill.exec.work.foreman.SqlUnsupportedException;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.Path;

public class AnalyzeTableHandler extends DefaultSqlHandler {
  private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(AnalyzeTableHandler.class);

  public AnalyzeTableHandler(SqlHandlerConfig config, Pointer<String> textPlan) {
    super(config, textPlan);
  }

  @Override
  public PhysicalPlan getPlan(SqlNode sqlNode)
      throws ValidationException, RelConversionException, IOException, ForemanSetupException {
    final SqlAnalyzeTable sqlAnalyzeTable = unwrap(sqlNode, SqlAnalyzeTable.class);

    verifyNoUnsupportedFunctions(sqlAnalyzeTable);

    SqlNode tableRef = sqlAnalyzeTable.getTableRef();
    SqlSelect scanSql = new SqlSelect(
        SqlParserPos.ZERO,              /* position */
        SqlNodeList.EMPTY,              /* keyword list */
        getColumnList(sqlAnalyzeTable), /* select list */
        tableRef,                       /* from */
        null,                           /* where */
        null,                           /* group by */
        null,                           /* having */
        null,                           /* windowDecls */
        null,                           /* orderBy */
        null,                           /* offset */
        null                            /* fetch */
    );

    ConvertedRelNode convertedRelNode = validateAndConvert(rewrite(scanSql));
    RelDataType validatedRowType = convertedRelNode.getValidatedRowType();

    RelNode relScan = convertedRelNode.getConvertedNode();
    DrillTableInfo drillTableInfo = DrillTableInfo.getTableInfoHolder(sqlAnalyzeTable.getTableRef(), config);
    String tableName = drillTableInfo.tableName();
    AbstractSchema drillSchema = SchemaUtilites.resolveToDrillSchema(
        config.getConverter().getDefaultSchema(), drillTableInfo.schemaPath());
    Table table = SqlHandlerUtil.getTableFromSchema(drillSchema, tableName);

    if (table == null) {
      throw UserException.validationError()
          .message("No table with given name [%s] exists in schema [%s]", tableName,
              drillSchema.getFullSchemaName())
          .build(logger);
    } else if (!(table instanceof DrillTable)) {
      return DrillStatsTable.notSupported(context, tableName);
    }

    DrillTable drillTable = (DrillTable) table;
    final Object selection = drillTable.getSelection();
    if (!(selection instanceof FormatSelection)) {
      return DrillStatsTable.notSupported(context, tableName);
    }
    // Do not support non-parquet tables
    FormatSelection formatSelection = (FormatSelection) selection;
    FormatPluginConfig formatConfig = formatSelection.getFormat();
    if (!((formatConfig instanceof ParquetFormatConfig)
          || ((formatConfig instanceof NamedFormatPluginConfig)
               && ((NamedFormatPluginConfig) formatConfig).getName().equals("parquet")))) {
      return DrillStatsTable.notSupported(context, tableName);
    }

    FileSystemPlugin plugin = (FileSystemPlugin) drillTable.getPlugin();
    DrillFileSystem fs = new DrillFileSystem(plugin.getFormatPlugin(
        formatSelection.getFormat()).getFsConf());

    Path selectionRoot = formatSelection.getSelection().getSelectionRoot();
    if (!selectionRoot.toUri().getPath().endsWith(tableName) || !fs.getFileStatus(selectionRoot).isDirectory()) {
      return DrillStatsTable.notSupported(context, tableName);
    }
    // Do not recompute statistics, if stale
    Path statsFilePath = new Path(selectionRoot, DotDrillType.STATS.getEnding());
    if (fs.exists(statsFilePath) && !isStatsStale(fs, statsFilePath)) {
     return DrillStatsTable.notRequired(context, tableName);
    }
    // Convert the query to Drill Logical plan and insert a writer operator on top.
    DrillRel drel = convertToDrel(relScan, drillSchema, tableName, sqlAnalyzeTable.getSamplePercent());
    Prel prel = convertToPrel(drel, validatedRowType);
    logAndSetTextPlan("Drill Physical", prel, logger);
    PhysicalOperator pop = convertToPop(prel);
    PhysicalPlan plan = convertToPlan(pop);
    log("Drill Plan", plan, logger);

    return plan;
  }

  /* Determines if the table was modified after computing statistics based on
   * directory/file modification timestamps
   */
  private boolean isStatsStale(DrillFileSystem fs, Path statsFilePath)
      throws IOException {
    long statsFileModifyTime = fs.getFileStatus(statsFilePath).getModificationTime();
    Path parentPath = statsFilePath.getParent();
    FileStatus directoryStatus = fs.getFileStatus(parentPath);
    // Parent directory modified after stats collection?
    return directoryStatus.getModificationTime() > statsFileModifyTime ||
        tableModified(fs, parentPath, statsFileModifyTime);
  }

  /* Determines if the table was modified after computing statistics based on
   * directory/file modification timestamps. Recursively checks sub-directories.
   */
  private boolean tableModified(DrillFileSystem fs, Path parentPath,
                                long statsModificationTime) throws IOException {
    for (final FileStatus file : fs.listStatus(parentPath)) {
      // If directory or files within it are modified
      if (file.getModificationTime() > statsModificationTime) {
        return true;
      }
      // For a directory, we should recursively check sub-directories
      if (file.isDirectory() && tableModified(fs, file.getPath(), statsModificationTime)) {
        return true;
      }
    }
    return false;
  }

  /* Generates the column list specified in the ANALYZE statement */
  private SqlNodeList getColumnList(final SqlAnalyzeTable sqlAnalyzeTable) {
    SqlNodeList columnList = sqlAnalyzeTable.getFieldList();
    if (columnList == null || columnList.size() <= 0) {
      columnList = new SqlNodeList(SqlParserPos.ZERO);
      columnList.add(new SqlIdentifier(SchemaPath.STAR_COLUMN.rootName(), SqlParserPos.ZERO));
    }
    /*final SqlNodeList columnList = new SqlNodeList(SqlParserPos.ZERO);
    final List<String> fields = sqlAnalyzeTable.getFieldNames();
    if (fields == null || fields.size() <= 0) {
      columnList.add(new SqlIdentifier(SchemaPath.STAR_COLUMN.rootName(), SqlParserPos.ZERO));
    } else {
      for(String field : fields) {
        columnList.add(new SqlIdentifier(field, SqlParserPos.ZERO));
      }
    }*/
    return columnList;
  }

  /* Converts to Drill logical plan */
  protected DrillRel convertToDrel(RelNode relNode, AbstractSchema schema, String analyzeTableName,
      double samplePercent) throws SqlUnsupportedException {
    DrillRel convertedRelNode = convertToRawDrel(relNode);

    final RelNode analyzeRel = new DrillAnalyzeRel(
        convertedRelNode.getCluster(), convertedRelNode.getTraitSet(), convertedRelNode, samplePercent);

    final RelNode writerRel = new DrillWriterRel(
        analyzeRel.getCluster(),
        analyzeRel.getTraitSet(),
        analyzeRel,
        schema.appendToStatsTable(analyzeTableName)
    );

    return new DrillScreenRel(writerRel.getCluster(), writerRel.getTraitSet(), writerRel);
  }

  // Make sure no unsupported features in ANALYZE statement are used
  private static void verifyNoUnsupportedFunctions(final SqlAnalyzeTable analyzeTable) {
    // throw unsupported error for functions that are not yet implemented
    if (analyzeTable.getEstimate()) {
      throw UserException.unsupportedError()
          .message("Statistics estimation is not yet supported.")
          .build(logger);
    }

    if (analyzeTable.getSamplePercent() <= 0 && analyzeTable.getSamplePercent() > 100.0) {
      throw UserException.unsupportedError()
          .message("Valid sampling percent between 0-100 is not specified.")
          .build(logger);
    }
  }
}
