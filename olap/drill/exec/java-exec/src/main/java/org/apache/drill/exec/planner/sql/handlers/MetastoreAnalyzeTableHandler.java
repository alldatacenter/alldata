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

import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rel.core.TableScan;
import org.apache.calcite.rel.type.RelDataType;
import org.apache.calcite.sql.SqlIdentifier;
import org.apache.calcite.sql.SqlLiteral;
import org.apache.calcite.sql.SqlNode;
import org.apache.calcite.sql.SqlNodeList;
import org.apache.calcite.sql.SqlNumericLiteral;
import org.apache.calcite.sql.SqlSelect;
import org.apache.calcite.sql.parser.SqlParserPos;
import org.apache.calcite.tools.RelBuilder;
import org.apache.calcite.tools.RelConversionException;
import org.apache.calcite.tools.ValidationException;
import org.apache.drill.common.exceptions.UserException;
import org.apache.drill.common.expression.FieldReference;
import org.apache.drill.common.expression.SchemaPath;
import org.apache.drill.common.logical.data.NamedExpression;
import org.apache.drill.common.util.function.CheckedSupplier;
import org.apache.drill.exec.ExecConstants;
import org.apache.drill.exec.metastore.ColumnNamesOptions;
import org.apache.drill.exec.metastore.analyze.AnalyzeInfoProvider;
import org.apache.drill.exec.metastore.analyze.MetadataAggregateContext;
import org.apache.drill.exec.metastore.analyze.MetadataControllerContext;
import org.apache.drill.exec.metastore.analyze.MetadataHandlerContext;
import org.apache.drill.exec.metastore.analyze.MetadataInfoCollector;
import org.apache.drill.exec.metastore.analyze.MetastoreAnalyzeConstants;
import org.apache.drill.exec.physical.PhysicalPlan;
import org.apache.drill.exec.physical.base.PhysicalOperator;
import org.apache.drill.exec.planner.common.DrillRelOptUtil;
import org.apache.drill.exec.planner.logical.DrillAnalyzeRel;
import org.apache.drill.exec.planner.logical.DrillRel;
import org.apache.drill.exec.planner.logical.DrillScreenRel;
import org.apache.drill.exec.planner.logical.DrillTable;
import org.apache.drill.exec.planner.logical.MetadataAggRel;
import org.apache.drill.exec.planner.logical.MetadataControllerRel;
import org.apache.drill.exec.planner.logical.MetadataHandlerRel;
import org.apache.drill.exec.planner.physical.PlannerSettings;
import org.apache.drill.exec.planner.physical.Prel;
import org.apache.drill.exec.planner.sql.parser.SqlMetastoreAnalyzeTable;
import org.apache.drill.exec.store.AbstractSchema;
import org.apache.drill.exec.store.dfs.FormatSelection;
import org.apache.drill.exec.util.Pointer;
import org.apache.drill.exec.work.foreman.ForemanSetupException;
import org.apache.drill.exec.work.foreman.SqlUnsupportedException;
import org.apache.drill.metastore.components.tables.BasicTablesRequests;
import org.apache.drill.metastore.components.tables.MetastoreTableInfo;
import org.apache.drill.metastore.exceptions.MetastoreException;
import org.apache.drill.metastore.metadata.MetadataInfo;
import org.apache.drill.metastore.metadata.MetadataType;
import org.apache.drill.metastore.metadata.TableInfo;
import org.apache.drill.shaded.guava.com.google.common.collect.ArrayListMultimap;
import org.apache.drill.shaded.guava.com.google.common.collect.Multimap;
import org.apache.parquet.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.apache.drill.exec.planner.logical.DrillRelFactories.LOGICAL_BUILDER;

/**
 * Constructs plan to be executed for collecting metadata and storing it to the Metastore.
 */
public class MetastoreAnalyzeTableHandler extends DefaultSqlHandler {
  private static final Logger logger = LoggerFactory.getLogger(MetastoreAnalyzeTableHandler.class);

  public MetastoreAnalyzeTableHandler(SqlHandlerConfig config, Pointer<String> textPlan) {
    super(config, textPlan);
  }

  @Override
  public PhysicalPlan getPlan(SqlNode sqlNode)
      throws ValidationException, RelConversionException, IOException, ForemanSetupException {
    if (!context.getOptions().getOption(ExecConstants.METASTORE_ENABLED_VALIDATOR)) {
      throw UserException.validationError()
          .message("Running ANALYZE TABLE REFRESH METADATA command when Metastore is disabled (`metastore.enabled` is set to false)")
          .build(logger);
    }
    // disables during analyze to prevent using data about locations from the Metastore
    context.getOptions().setLocalOption(ExecConstants.METASTORE_ENABLED, false);
    SqlMetastoreAnalyzeTable sqlAnalyzeTable = unwrap(sqlNode, SqlMetastoreAnalyzeTable.class);

    SqlNode tableRef = sqlAnalyzeTable.getTableRef();

    DrillTableInfo drillTableInfo = DrillTableInfo.getTableInfoHolder(tableRef, config);

    AnalyzeInfoProvider analyzeInfoProvider = drillTableInfo.drillTable().getGroupScan().getAnalyzeInfoProvider();

    if (analyzeInfoProvider == null) {
      throw UserException.validationError()
          .message("ANALYZE is not supported for group scan [%s]", drillTableInfo.drillTable().getGroupScan())
          .build(logger);
    }

    ColumnNamesOptions columnNamesOptions = new ColumnNamesOptions(context.getOptions());

    // creates select with DYNAMIC_STAR column and analyze specific columns to obtain corresponding table scan
    SqlSelect scanSql = new SqlSelect(
        SqlParserPos.ZERO,
        SqlNodeList.EMPTY,
        getColumnList(analyzeInfoProvider.getProjectionFields(drillTableInfo.drillTable(), getMetadataType(sqlAnalyzeTable), columnNamesOptions)),
        tableRef,
        null,
        null,
        null,
        null,
        null,
        null,
        null
    );

    ConvertedRelNode convertedRelNode = validateAndConvert(rewrite(scanSql));
    RelDataType validatedRowType = convertedRelNode.getValidatedRowType();

    RelNode relScan = convertedRelNode.getConvertedNode();

    DrillRel drel = convertToDrel(relScan, sqlAnalyzeTable, drillTableInfo);

    Prel prel = convertToPrel(drel, validatedRowType);
    logAndSetTextPlan("Drill Physical", prel, logger);
    PhysicalOperator pop = convertToPop(prel);
    PhysicalPlan plan = convertToPlan(pop);
    log("Drill Plan", plan, logger);
    return plan;
  }

  /**
   * Generates the column list with {@link SchemaPath#DYNAMIC_STAR} and columns required for analyze.
   */
  private SqlNodeList getColumnList(List<SchemaPath> projectingColumns) {
    SqlNodeList columnList = new SqlNodeList(SqlParserPos.ZERO);
    columnList.add(new SqlIdentifier(SchemaPath.DYNAMIC_STAR, SqlParserPos.ZERO));
    projectingColumns.stream()
        .map(segmentColumn -> new SqlIdentifier(segmentColumn.getRootSegmentPath(), SqlParserPos.ZERO))
        .forEach(columnList::add);
    return columnList;
  }

  private MetadataType getMetadataType(SqlMetastoreAnalyzeTable sqlAnalyzeTable) {
    SqlLiteral stringLiteral = sqlAnalyzeTable.getLevel();
    // for the case when metadata level is not specified in ANALYZE statement,
    // value from the `metastore.metadata.store.depth_level` option is used
    String metadataLevel;
    if (stringLiteral == null) {
      metadataLevel = context.getOption(ExecConstants.METASTORE_METADATA_STORE_DEPTH_LEVEL).string_val;
    } else {
      metadataLevel = stringLiteral.toValue();
    }
    return metadataLevel != null ? MetadataType.valueOf(metadataLevel.toUpperCase()) : MetadataType.ALL;
  }

  /**
   * Converts to Drill logical plan
   */
  private DrillRel convertToDrel(RelNode relNode, SqlMetastoreAnalyzeTable sqlAnalyzeTable, DrillTableInfo drillTableInfo) throws ForemanSetupException, IOException {
    RelBuilder relBuilder = LOGICAL_BUILDER.create(relNode.getCluster(), null);

    DrillTable table = drillTableInfo.drillTable();
    AnalyzeInfoProvider analyzeInfoProvider = table.getGroupScan().getAnalyzeInfoProvider();

    List<String> schemaPath = drillTableInfo.schemaPath();
    String pluginName = schemaPath.get(0);
    String workspaceName = Strings.join(schemaPath.subList(1, schemaPath.size()), AbstractSchema.SCHEMA_SEPARATOR);

    String tableName = drillTableInfo.tableName();
    TableInfo tableInfo = TableInfo.builder()
        .name(tableName)
        .owner(table.getUserName())
        .type(analyzeInfoProvider.getTableTypeName())
        .storagePlugin(pluginName)
        .workspace(workspaceName)
        .build();

    ColumnNamesOptions columnNamesOptions = new ColumnNamesOptions(context.getOptions());

    List<String> segmentColumns = analyzeInfoProvider.getSegmentColumns(table, columnNamesOptions).stream()
        .map(SchemaPath::getRootSegmentPath)
        .collect(Collectors.toList());
    List<NamedExpression> segmentExpressions = segmentColumns.stream()
        .map(partitionName ->
            new NamedExpression(SchemaPath.getSimplePath(partitionName), FieldReference.getWithQuotedRef(partitionName)))
        .collect(Collectors.toList());

    List<MetadataInfo> rowGroupsInfo = Collections.emptyList();
    List<MetadataInfo> filesInfo = Collections.emptyList();
    Multimap<Integer, MetadataInfo> segments = ArrayListMultimap.create();

    BasicTablesRequests basicRequests;
    try {
      basicRequests = context.getMetastoreRegistry().get()
          .tables()
          .basicRequests();
    } catch (MetastoreException e) {
      logger.error("Error when obtaining Metastore instance for table {}", tableName, e);
      DrillRel convertedRelNode = convertToRawDrel(
          relBuilder.values(
                new String[]{MetastoreAnalyzeConstants.OK_FIELD_NAME, MetastoreAnalyzeConstants.SUMMARY_FIELD_NAME},
                false, e.getMessage())
              .build());
      return new DrillScreenRel(convertedRelNode.getCluster(), convertedRelNode.getTraitSet(), convertedRelNode);
    }

    MetadataType metadataLevel = getMetadataType(sqlAnalyzeTable);

    List<SchemaPath> interestingColumns = sqlAnalyzeTable.getFieldNames();

    MetastoreTableInfo metastoreTableInfo = basicRequests.metastoreTableInfo(tableInfo);

    List<MetadataInfo> allMetaToHandle = null;
    List<MetadataInfo> metadataToRemove = new ArrayList<>();

    // Step 1: checks whether table metadata is present in the Metastore to determine
    // whether incremental analyze may be produced
    if (metastoreTableInfo.isExists()) {
      RelNode finalRelNode = relNode;
      CheckedSupplier<TableScan, SqlUnsupportedException> tableScanSupplier =
          () -> DrillRelOptUtil.findScan(convertToDrel(finalRelNode.getInput(0)));

      MetadataInfoCollector metadataInfoCollector = analyzeInfoProvider.getMetadataInfoCollector(basicRequests,
          tableInfo, (FormatSelection) table.getSelection(), context.getPlannerSettings(),
          tableScanSupplier, interestingColumns, metadataLevel, segmentColumns.size());

      if (!metadataInfoCollector.isOutdated()) {
        DrillRel convertedRelNode = convertToRawDrel(
            relBuilder.values(new String[]{MetastoreAnalyzeConstants.OK_FIELD_NAME, MetastoreAnalyzeConstants.SUMMARY_FIELD_NAME},
                false, "Table metadata is up to date, analyze wasn't performed.")
                .build());
        return new DrillScreenRel(convertedRelNode.getCluster(), convertedRelNode.getTraitSet(), convertedRelNode);
      }

      // updates scan to read updated / new files, pass removed files into metadata handler
      relNode = relNode.copy(relNode.getTraitSet(), Collections.singletonList(metadataInfoCollector.getPrunedScan()));

      filesInfo = metadataInfoCollector.getFilesInfo();
      segments = metadataInfoCollector.getSegmentsInfo();
      rowGroupsInfo = metadataInfoCollector.getRowGroupsInfo();

      allMetaToHandle = metadataInfoCollector.getAllMetaToHandle();
      metadataToRemove = metadataInfoCollector.getMetadataToRemove();
    }

    // Step 2: constructs plan for producing analyze
    DrillRel convertedRelNode = convertToRawDrel(relNode);

    boolean createNewAggregations = true;

    // List of columns for which statistics should be collected: interesting columns + segment columns
    List<SchemaPath> statisticsColumns = interestingColumns == null ? null : new ArrayList<>(interestingColumns);
    if (statisticsColumns != null) {
      segmentColumns.stream()
          .map(SchemaPath::getSimplePath)
          .forEach(statisticsColumns::add);
    }

    SchemaPath locationField = analyzeInfoProvider.getLocationField(columnNamesOptions);

    if (analyzeInfoProvider.supportsMetadataType(MetadataType.ROW_GROUP) && metadataLevel.includes(MetadataType.ROW_GROUP)) {
      MetadataHandlerContext handlerContext = MetadataHandlerContext.builder()
          .tableInfo(tableInfo)
          .metadataToHandle(rowGroupsInfo)
          .metadataType(MetadataType.ROW_GROUP)
          .depthLevel(segmentExpressions.size())
          .segmentColumns(segmentColumns)
          .build();

      convertedRelNode = getRowGroupAggRelNode(segmentExpressions, convertedRelNode, createNewAggregations,
          statisticsColumns, handlerContext);

      createNewAggregations = false;
      locationField = SchemaPath.getSimplePath(MetastoreAnalyzeConstants.LOCATION_FIELD);
    }

    if (analyzeInfoProvider.supportsMetadataType(MetadataType.FILE) && metadataLevel.includes(MetadataType.FILE)) {
      MetadataHandlerContext handlerContext = MetadataHandlerContext.builder()
          .tableInfo(tableInfo)
          .metadataToHandle(filesInfo)
          .metadataType(MetadataType.FILE)
          .depthLevel(segmentExpressions.size())
          .segmentColumns(segmentColumns)
          .build();

      convertedRelNode = getFileAggRelNode(segmentExpressions, convertedRelNode,
          createNewAggregations, statisticsColumns, locationField, handlerContext);

      locationField = SchemaPath.getSimplePath(MetastoreAnalyzeConstants.LOCATION_FIELD);

      createNewAggregations = false;
    }

    if (analyzeInfoProvider.supportsMetadataType(MetadataType.SEGMENT) && metadataLevel.includes(MetadataType.SEGMENT)) {
      for (int i = segmentExpressions.size(); i > 0; i--) {
        MetadataHandlerContext handlerContext = MetadataHandlerContext.builder()
            .tableInfo(tableInfo)
            .metadataToHandle(new ArrayList<>(segments.get(i - 1)))
            .metadataType(MetadataType.SEGMENT)
            .depthLevel(i)
            .segmentColumns(segmentColumns.subList(0, i))
            .build();

        convertedRelNode = getSegmentAggRelNode(segmentExpressions, convertedRelNode,
            createNewAggregations, statisticsColumns, locationField, i, handlerContext);

        locationField = SchemaPath.getSimplePath(MetastoreAnalyzeConstants.LOCATION_FIELD);

        createNewAggregations = false;
      }
    }

    if (analyzeInfoProvider.supportsMetadataType(MetadataType.TABLE) && metadataLevel.includes(MetadataType.TABLE)) {
      MetadataHandlerContext handlerContext = MetadataHandlerContext.builder()
          .tableInfo(tableInfo)
          .metadataToHandle(Collections.emptyList())
          .metadataType(MetadataType.TABLE)
          .depthLevel(segmentExpressions.size())
          .segmentColumns(segmentColumns)
          .build();

      convertedRelNode = getTableAggRelNode(convertedRelNode, createNewAggregations,
          statisticsColumns, locationField, handlerContext);
    } else {
      throw new IllegalStateException("Analyze table with NONE level");
    }

    boolean useStatistics = context.getOptions().getOption(PlannerSettings.STATISTICS_USE);
    SqlNumericLiteral samplePercentLiteral = sqlAnalyzeTable.getSamplePercent();
    double samplePercent = samplePercentLiteral == null ? 100.0 : samplePercentLiteral.intValue(true);

    // Step 3: adds rel nodes for producing statistics analyze if required
    RelNode analyzeRel = useStatistics
        ? new DrillAnalyzeRel(
              convertedRelNode.getCluster(), convertedRelNode.getTraitSet(), convertToRawDrel(relNode), samplePercent)
        : convertToRawDrel(relBuilder.values(new String[]{""}, "").build());

    MetadataControllerContext metadataControllerContext = MetadataControllerContext.builder()
        .tableInfo(tableInfo)
        .metastoreTableInfo(metastoreTableInfo)
        .location(((FormatSelection) table.getSelection()).getSelection().getSelectionRoot())
        .interestingColumns(interestingColumns)
        .segmentColumns(segmentColumns)
        .metadataToHandle(allMetaToHandle)
        .metadataToRemove(metadataToRemove)
        .analyzeMetadataLevel(metadataLevel)
        .build();

    convertedRelNode = new MetadataControllerRel(convertedRelNode.getCluster(),
        convertedRelNode.getTraitSet(),
        convertedRelNode,
        analyzeRel,
        metadataControllerContext);

    return new DrillScreenRel(convertedRelNode.getCluster(), convertedRelNode.getTraitSet(), convertedRelNode);
  }

  private DrillRel getTableAggRelNode(DrillRel convertedRelNode, boolean createNewAggregations,
      List<SchemaPath> statisticsColumns, SchemaPath locationField, MetadataHandlerContext handlerContext) {
    SchemaPath lastModifiedTimeField =
        SchemaPath.getSimplePath(config.getContext().getOptions().getString(ExecConstants.IMPLICIT_LAST_MODIFIED_TIME_COLUMN_LABEL));

    List<SchemaPath> metadataColumns = Arrays.asList(locationField, lastModifiedTimeField);

    MetadataAggregateContext aggregateContext = MetadataAggregateContext.builder()
        .groupByExpressions(Collections.emptyList())
        .interestingColumns(statisticsColumns)
        .createNewAggregations(createNewAggregations)
        .metadataColumns(metadataColumns)
        .metadataLevel(MetadataType.TABLE)
        .build();

    convertedRelNode = new MetadataAggRel(convertedRelNode.getCluster(),
        convertedRelNode.getTraitSet(), convertedRelNode, aggregateContext);

    convertedRelNode =
        new MetadataHandlerRel(convertedRelNode.getCluster(),
            convertedRelNode.getTraitSet(),
            convertedRelNode,
            handlerContext);
    return convertedRelNode;
  }

  private DrillRel getSegmentAggRelNode(List<NamedExpression> segmentExpressions, DrillRel convertedRelNode,
      boolean createNewAggregations, List<SchemaPath> statisticsColumns, SchemaPath locationField,
      int segmentLevel, MetadataHandlerContext handlerContext) {
    SchemaPath lastModifiedTimeField =
        SchemaPath.getSimplePath(config.getContext().getOptions().getString(ExecConstants.IMPLICIT_LAST_MODIFIED_TIME_COLUMN_LABEL));

    List<SchemaPath> metadataColumns = Arrays.asList(lastModifiedTimeField, locationField);

    List<NamedExpression> groupByExpressions = new ArrayList<>(segmentExpressions);

    MetadataAggregateContext aggregateContext = MetadataAggregateContext.builder()
        .groupByExpressions(groupByExpressions.subList(0, segmentLevel))
        .interestingColumns(statisticsColumns)
        .createNewAggregations(createNewAggregations)
        .metadataColumns(metadataColumns)
        .metadataLevel(MetadataType.SEGMENT)
        .build();

    convertedRelNode = new MetadataAggRel(convertedRelNode.getCluster(),
        convertedRelNode.getTraitSet(), convertedRelNode, aggregateContext);

    convertedRelNode =
        new MetadataHandlerRel(convertedRelNode.getCluster(),
            convertedRelNode.getTraitSet(),
            convertedRelNode,
            handlerContext);
    return convertedRelNode;
  }

  private DrillRel getFileAggRelNode(List<NamedExpression> segmentExpressions, DrillRel convertedRelNode,
      boolean createNewAggregations, List<SchemaPath> statisticsColumns, SchemaPath locationField, MetadataHandlerContext handlerContext) {
    SchemaPath lastModifiedTimeField =
        SchemaPath.getSimplePath(config.getContext().getOptions().getString(ExecConstants.IMPLICIT_LAST_MODIFIED_TIME_COLUMN_LABEL));

    List<SchemaPath> metadataColumns = Arrays.asList(lastModifiedTimeField, locationField);

    NamedExpression locationExpression =
        new NamedExpression(locationField, FieldReference.getWithQuotedRef(MetastoreAnalyzeConstants.LOCATION_FIELD));
    List<NamedExpression> fileGroupByExpressions = new ArrayList<>(segmentExpressions);
    fileGroupByExpressions.add(locationExpression);

    MetadataAggregateContext aggregateContext = MetadataAggregateContext.builder()
        .groupByExpressions(fileGroupByExpressions)
        .interestingColumns(statisticsColumns)
        .createNewAggregations(createNewAggregations)
        .metadataColumns(metadataColumns)
        .metadataLevel(MetadataType.FILE)
        .build();

    convertedRelNode = new MetadataAggRel(convertedRelNode.getCluster(),
        convertedRelNode.getTraitSet(), convertedRelNode, aggregateContext);

    convertedRelNode =
        new MetadataHandlerRel(convertedRelNode.getCluster(),
            convertedRelNode.getTraitSet(),
            convertedRelNode,
            handlerContext);
    return convertedRelNode;
  }

  private DrillRel getRowGroupAggRelNode(List<NamedExpression> segmentExpressions, DrillRel convertedRelNode,
      boolean createNewAggregations, List<SchemaPath> statisticsColumns, MetadataHandlerContext handlerContext) {
    SchemaPath locationField =
        SchemaPath.getSimplePath(config.getContext().getOptions().getString(ExecConstants.IMPLICIT_FQN_COLUMN_LABEL));
    SchemaPath lastModifiedTimeField =
        SchemaPath.getSimplePath(config.getContext().getOptions().getString(ExecConstants.IMPLICIT_LAST_MODIFIED_TIME_COLUMN_LABEL));

    String rowGroupIndexColumn = config.getContext().getOptions().getString(ExecConstants.IMPLICIT_ROW_GROUP_INDEX_COLUMN_LABEL);
    SchemaPath rgiField = SchemaPath.getSimplePath(rowGroupIndexColumn);

    List<NamedExpression> rowGroupGroupByExpressions =
        getRowGroupExpressions(segmentExpressions, locationField, rowGroupIndexColumn, rgiField);

    SchemaPath rowGroupStartField =
        SchemaPath.getSimplePath(config.getContext().getOptions().getString(ExecConstants.IMPLICIT_ROW_GROUP_START_COLUMN_LABEL));
    SchemaPath rowGroupLengthField =
        SchemaPath.getSimplePath(config.getContext().getOptions().getString(ExecConstants.IMPLICIT_ROW_GROUP_LENGTH_COLUMN_LABEL));

    List<SchemaPath> metadataColumns = Arrays.asList(lastModifiedTimeField, locationField, rgiField, rowGroupStartField, rowGroupLengthField);

    MetadataAggregateContext aggregateContext = MetadataAggregateContext.builder()
        .groupByExpressions(rowGroupGroupByExpressions)
        .interestingColumns(statisticsColumns)
        .createNewAggregations(createNewAggregations)
        .metadataColumns(metadataColumns)
        .metadataLevel(MetadataType.ROW_GROUP)
        .build();

    convertedRelNode = new MetadataAggRel(convertedRelNode.getCluster(),
        convertedRelNode.getTraitSet(), convertedRelNode, aggregateContext);

    convertedRelNode =
        new MetadataHandlerRel(convertedRelNode.getCluster(),
            convertedRelNode.getTraitSet(),
            convertedRelNode,
            handlerContext);
    return convertedRelNode;
  }

  private List<NamedExpression> getRowGroupExpressions(List<NamedExpression> segmentExpressions,
      SchemaPath locationField, String rowGroupIndexColumn, SchemaPath rgiField) {
    List<NamedExpression> rowGroupGroupByExpressions = new ArrayList<>(segmentExpressions);
    rowGroupGroupByExpressions.add(
        new NamedExpression(locationField, FieldReference.getWithQuotedRef(MetastoreAnalyzeConstants.LOCATION_FIELD)));
    rowGroupGroupByExpressions.add(
        new NamedExpression(rgiField,
            FieldReference.getWithQuotedRef(rowGroupIndexColumn)));
    return rowGroupGroupByExpressions;
  }

}
