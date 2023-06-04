/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytedance.bitsail.common.ddl;

import com.bytedance.bitsail.common.BitSailException;
import com.bytedance.bitsail.common.configuration.BitSailConfiguration;
import com.bytedance.bitsail.common.ddl.sink.SinkEngineConnector;
import com.bytedance.bitsail.common.ddl.source.SourceEngineConnector;
import com.bytedance.bitsail.common.exception.CommonErrorCode;
import com.bytedance.bitsail.common.model.ColumnInfo;
import com.bytedance.bitsail.common.option.CommonOptions;
import com.bytedance.bitsail.common.option.ReaderOptions;
import com.bytedance.bitsail.common.option.WriterOptions;
import com.bytedance.bitsail.common.type.TypeInfoConverter;
import com.bytedance.bitsail.common.typeinfo.TypeInfo;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
public class DdlSyncManager {
  private static final char[] INVALID_CHARS = {'+', '-', '*', '/', '.', ','};

  private final SourceEngineConnector readerExternalEngineConnector;
  private final SinkEngineConnector writerExternalEngineConnector;
  private final BitSailConfiguration commonConf;
  private final BitSailConfiguration readerConf;
  private final BitSailConfiguration writerConf;
  private final boolean ddlSyncSkipErrorColumns;
  private final boolean ddlSyncPreExecute;
  private final boolean ddlSyncIgnoreAdd;
  private final boolean ddlSyncIgnoreUpdate;
  private final boolean ddlSyncIgnoreDrop;
  @Getter
  private final List<ColumnInfo> alignedReaderColumns;
  @Getter
  private final List<ColumnInfo> alignedWriterColumns;
  private BitSailConfiguration globalConfiguration;
  @Getter
  private List<ColumnInfo> pendingAddColumns;
  @Getter
  private List<ColumnInfo> pendingUpdateColumns;
  @Getter
  private List<ColumnInfo> pendingDeleteColumns;

  public DdlSyncManager(SourceEngineConnector readerExternalEngineConnector,
                        SinkEngineConnector writerExternalEngineConnector,
                        BitSailConfiguration commonConf,
                        BitSailConfiguration readerConf,
                        BitSailConfiguration writerConf) {

    this.readerExternalEngineConnector = readerExternalEngineConnector;
    this.writerExternalEngineConnector = writerExternalEngineConnector;
    this.commonConf = commonConf;
    this.readerConf = readerConf;
    this.writerConf = writerConf;

    this.ddlSyncSkipErrorColumns = commonConf.get(CommonOptions.SYNC_DDL_SKIP_ERROR_COLUMNS);
    this.ddlSyncPreExecute = commonConf.get(CommonOptions.SYNC_DDL_PRE_EXECUTE);
    this.ddlSyncIgnoreAdd = commonConf.get(CommonOptions.SYNC_DDL_IGNORE_ADD);
    this.ddlSyncIgnoreUpdate = commonConf.get(CommonOptions.SYNC_DDL_IGNORE_UPDATE);
    this.ddlSyncIgnoreDrop = commonConf.get(CommonOptions.SYNC_DDL_IGNORE_DROP);

    this.alignedReaderColumns = Lists.newLinkedList();
    this.alignedWriterColumns = Lists.newLinkedList();
    this.pendingAddColumns = new LinkedList<>();
    this.pendingDeleteColumns = new LinkedList<>();
    this.pendingUpdateColumns = new LinkedList<>();
  }

  private static boolean containsInvalidChar(String fieldName) {
    for (char c : INVALID_CHARS) {
      if (StringUtils.contains(fieldName, c)) {
        log.warn("Reader field name {} contains invalid character {}.", fieldName, c);
        return true;
      }
    }
    return false;
  }

  public void doColumnAlignment(boolean supportSchemaCheck) throws Exception {
    //1. acquire alignment strategy
    SchemaColumnAligner.ColumnAlignmentStrategy alignmentStrategy = getAlignmentStrategy();

    //2. get pending align columns by the strategy
    doColumnConfAlignment(alignmentStrategy);

    //3. do the real align between reader & writer.
    doExternalColumnAlignment(alignmentStrategy);

    //4. final check.
    checkColumnsAlignment(readerExternalEngineConnector, supportSchemaCheck);
  }

  @VisibleForTesting
  SchemaColumnAligner.ColumnAlignmentStrategy getAlignmentStrategy() {
    String columnAlignmentStrategy = commonConf
        .get(CommonOptions.COLUMN_ALIGN_STRATEGY)
        .toLowerCase().trim();
    SchemaColumnAligner.ColumnAlignmentStrategy strategy;
    try {
      strategy = SchemaColumnAligner.ColumnAlignmentStrategy.valueOf(columnAlignmentStrategy);
    } catch (Exception e) {
      throw BitSailException.asBitSailException(CommonErrorCode.CONFIG_ERROR,
          String.format("Strategy for the column align %s is not supported, support values are = %s",
              columnAlignmentStrategy, Arrays.toString(SchemaColumnAligner.ColumnAlignmentStrategy.values())));
    }
    return strategy;
  }

  public void doColumnConfAlignment(SchemaColumnAligner.ColumnAlignmentStrategy strategy) throws Exception {
    switch (strategy) {
      case disable:
        log.info("auto_get_columns is disable!");
        return;
      case intersect:
        doIntersectAlignment();
        return;
      case source_only:
        doSourceOnlyAlignment();
        return;
      default:
        throw BitSailException.asBitSailException(CommonErrorCode.CONFIG_ERROR,
            String.format("Unsupported AutoGetColumnMode mode:[%s]. Allowed modes are %s, please check your configuration.",
                strategy, Arrays.toString(SchemaColumnAligner.ColumnAlignmentStrategy.values())));
    }
  }

  void getChangeableColumns(SchemaColumnAligner.ColumnAlignmentStrategy strategy) throws Exception {

    List<ColumnInfo> readerColumnInfos = readerConf
        .getNecessaryOption(ReaderOptions.BaseReaderOptions.COLUMNS, CommonErrorCode.CONFIG_ERROR);

    List<ColumnInfo> writerColumnInfos;
    if (strategy == SchemaColumnAligner.ColumnAlignmentStrategy.intersect) {
      writerColumnInfos = writerConf
          .getNecessaryOption(WriterOptions.BaseWriterOptions.COLUMNS, CommonErrorCode.CONFIG_ERROR);
    } else {
      writerColumnInfos = writerExternalEngineConnector.getExternalColumnInfos();
    }

    Map<String, String> writerColumnMapping = writerColumnInfos.stream()
        .collect(Collectors.toMap(ColumnInfo::getName, ColumnInfo::getType));

    List<String> writerExcludedColumnNames = writerExternalEngineConnector.getExcludedColumnInfos()
        .stream()
        .map(StringUtils::lowerCase)
        .collect(Collectors.toList());

    log.info("Reader Columns are {}, and size: {}.", readerColumnInfos, CollectionUtils.size(readerColumnInfos));
    log.info("Writer Columns are {}, and size: {}.", writerColumnInfos, CollectionUtils.size(writerColumnInfos));

    List<ColumnInfo> columnsToAdd = Lists.newLinkedList();
    List<ColumnInfo> columnsToUpdate = Lists.newLinkedList();

    TypeInfoConverter readerTypeInfoConverter = readerExternalEngineConnector.createTypeInfoConverter();
    TypeInfoConverter writerTypeInfoConverter = writerExternalEngineConnector.createTypeInfoConverter();

    for (ColumnInfo readerColumnInfo : readerColumnInfos) {
      String readerColumnName = StringUtils.lowerCase(readerColumnInfo.getName());
      String readerColumnType = StringUtils.lowerCase(readerColumnInfo.getType());

      if (containsInvalidChar(readerColumnInfo.getName())) {
        if (ddlSyncSkipErrorColumns) {
          log.info("Here have an column name: {} which contains invalid chars, skip it do column alignment",
              readerColumnName);
          continue;
        }
        throw BitSailException.asBitSailException(CommonErrorCode.PLUGIN_ERROR,
            String.format("Invalid column name:[%s]. Please validate your columns names!", readerColumnName));
      }

      if (writerExcludedColumnNames.contains(readerColumnName)) {
        log.info("Ignore the excluded column: {}.", readerColumnName);
        alignedReaderColumns.add(new ColumnInfo(readerColumnName, readerColumnType));
        //todo if writer columns hasn't the source name.
        alignedWriterColumns.add(new ColumnInfo(readerColumnName, writerColumnMapping.get(readerColumnName)));
        continue;
      }

      if (writerColumnMapping.containsKey(readerColumnName)) {
        //Update the columns
        columnsToUpdate(readerTypeInfoConverter,
            writerTypeInfoConverter,
            readerColumnName,
            readerColumnType,
            columnsToUpdate,
            writerColumnMapping);
      } else {
        //Adding the columns.
        columnToAdding(readerTypeInfoConverter,
            writerTypeInfoConverter,
            readerColumnName,
            readerColumnType,
            columnsToAdd);
      }
    }

    if (CollectionUtils.isNotEmpty(columnsToAdd) && !ddlSyncIgnoreAdd) {
      pendingAddColumns = columnsToAdd;
      log.info("There are some fields need to be added to sink storage: {}", pendingAddColumns);
    } else {
      log.info("There is no fields need to be added to sink storage");
    }

    if (CollectionUtils.isNotEmpty(columnsToUpdate) && !ddlSyncIgnoreUpdate) {
      pendingUpdateColumns = columnsToUpdate;
      log.info("There may be some fields need to be modified in sink storage: {}", pendingUpdateColumns);
    } else {
      log.info("There is no fields need to be modified in sink storage");
    }
    //todo Current now, we didn't support column deletion action.
    //pendingDeleteColumns = null;
  }

  private void columnToAdding(TypeInfoConverter readerTypeInfoConverter,
                              TypeInfoConverter writerTypeInfoConverter,
                              String sourceColumnName,
                              String sourceColumnType,
                              List<ColumnInfo> columnsToAdd) {
    TypeInfo<?> readerTypeInfo = readerTypeInfoConverter.fromTypeString(sourceColumnType);
    String writerExternalEngineTypeName = writerTypeInfoConverter.fromTypeInfo(readerTypeInfo);

    log.info("Adding column for reader column name: {}, " +
        "reader column type: {}, reader column type info: {}.", sourceColumnName, sourceColumnType, readerTypeInfo);
    if (Objects.nonNull(writerExternalEngineTypeName)) {
      columnsToAdd.add(new ColumnInfo(sourceColumnName, writerExternalEngineTypeName));
      alignedReaderColumns.add(new ColumnInfo(sourceColumnName, sourceColumnType));
      alignedWriterColumns.add(new ColumnInfo(sourceColumnName, writerExternalEngineTypeName));
      return;
    }

    if (!ddlSyncSkipErrorColumns) {
      log.info("Cannot get sink type from source type: [{}] , continue...", sourceColumnType);
      return;
    }
    throw BitSailException.asBitSailException(CommonErrorCode.PLUGIN_ERROR, "Column name: " + sourceColumnName +
        " Cannot get sink type from source type: " + sourceColumnType + ", Please validate your columns type!");
  }

  private void columnsToUpdate(TypeInfoConverter readerTypeInfoConverter,
                               TypeInfoConverter writerTypeInfoConverter,
                               String sourceColumnName,
                               String sourceColumnType,
                               List<ColumnInfo> columnsToUpdate,
                               Map<String, String> writerColumnMapping) {
    String originalWriterColumnType = writerColumnMapping.get(sourceColumnName);

    TypeInfo<?> readerTypeInfo = readerTypeInfoConverter.fromTypeString(sourceColumnType);
    String newWriterColumnType = writerTypeInfoConverter.fromTypeInfo(readerTypeInfo);
    log.info("Updating column for column name: {}, reader column type: {}, " +
            "reader column type info: {}, writer column name: {}.",
        sourceColumnName, sourceColumnType, readerTypeInfo, newWriterColumnType);

    if (Objects.nonNull(newWriterColumnType)) {
      alignedReaderColumns.add(new ColumnInfo(sourceColumnName, sourceColumnType));

      if (writerExternalEngineConnector.isTypeCompatible(newWriterColumnType, originalWriterColumnType)) {
        alignedWriterColumns.add(new ColumnInfo(sourceColumnName, originalWriterColumnType));

      } else {
        alignedWriterColumns.add(new ColumnInfo(sourceColumnName, newWriterColumnType));
        columnsToUpdate.add(new ColumnInfo(sourceColumnName, newWriterColumnType));
      }

      return;
    }
    //if writer engine not support this column type
    if (ddlSyncSkipErrorColumns) {
      log.warn("Sink column is [{}], cannot get sink type from source type:[{}], this column will not be transmitted.",
          sourceColumnName, sourceColumnType);
    } else {
      throw BitSailException.asBitSailException(CommonErrorCode.PLUGIN_ERROR,
          String.format("Sink column is: [%s] ,cannot get sink type from source type: [%s], Please validate your columns type!",
              sourceColumnName, sourceColumnType));
    }
  }

  @SuppressWarnings("checkstyle:MagicNumber")
  public void invokeExternalColumnAlignment(SchemaColumnAligner.ColumnAlignmentStrategy strategy) throws Exception {
    StopWatch stopWatch = new StopWatch();
    stopWatch.start();

    try {
      log.info("Starting alignment source external system and sink external system...");
      if (this.readerExternalEngineConnector == null || this.writerExternalEngineConnector == null) {
        throw BitSailException.asBitSailException(CommonErrorCode.CONFIG_ERROR,
            "source or sink isn't support schema manager, switch off the job.common.sync_ddl option!");
      }
      getChangeableColumns(strategy);

      if (!ddlSyncIgnoreAdd) {
        this.writerExternalEngineConnector.addColumns(this.pendingAddColumns);
      }

      if (!ddlSyncIgnoreUpdate) {
        this.writerExternalEngineConnector.updateColumns(this.pendingUpdateColumns);
      }

      if (!ddlSyncIgnoreDrop) {
        this.writerExternalEngineConnector.deleteColumns(this.pendingDeleteColumns);
      }
    } finally {
      stopWatch.stop();
    }
    log.info("sync ddl finished, cost: {}s", stopWatch.getTime() / 1000);
  }

  /**
   * @throws Exception
   */
  private void doIntersectAlignment() throws Exception {
    if (null == readerExternalEngineConnector || null == writerExternalEngineConnector) {
      throw BitSailException.asBitSailException(CommonErrorCode.CONFIG_ERROR,
          "The option of auto_get_schema is intersect, but the source or sink does not support schema manage now!");
    }
    SchemaColumnAligner schemaColumnAligner = new SchemaColumnAligner(readerExternalEngineConnector, writerExternalEngineConnector);
    SchemaHelper.SchemaIntersectionResponse schemaResponse = schemaColumnAligner.doIntersectStrategy();
    List<ColumnInfo> readerCols = schemaResponse.getReaderColumns();
    List<ColumnInfo> writerCols = schemaResponse.getWriterColumns();

    readerConf.set(ReaderOptions.BaseReaderOptions.COLUMNS, readerCols);
    writerConf.set(WriterOptions.BaseWriterOptions.COLUMNS, writerCols);

    if (globalConfiguration != null) {
      globalConfiguration.set(ReaderOptions.BaseReaderOptions.COLUMNS, readerCols);
      globalConfiguration.set(WriterOptions.BaseWriterOptions.COLUMNS, writerCols);
    }
    log.info("auto_get_columns is intersect, get columns from DB finished, final reader columns {}, writer columns {}.",
        readerCols, writerCols);
  }

  private void doSourceOnlyAlignment() throws Exception {
    if (readerExternalEngineConnector == null) {
      throw BitSailException.asBitSailException(CommonErrorCode.CONFIG_ERROR,
          "The option of auto_get_schema is source_only, but the source is not support schema manage now!");
    }
    SchemaColumnAligner schemaColumnAligner = new SchemaColumnAligner(readerExternalEngineConnector, writerExternalEngineConnector);
    List<ColumnInfo> columnInfos = schemaColumnAligner.doSourceOnlyStrategy();

    readerConf.set(ReaderOptions.BaseReaderOptions.COLUMNS, columnInfos);
    if (globalConfiguration != null) {
      globalConfiguration.set(ReaderOptions.BaseReaderOptions.COLUMNS, columnInfos);
    }
    log.info("auto_get_columns is source_only, get columns from DB finished, final reader columns {}.", columnInfos);
  }

  /**
   * @throws Exception
   */
  public void doExternalColumnAlignment(SchemaColumnAligner.ColumnAlignmentStrategy strategy) throws Exception {
    if (!commonConf.get(CommonOptions.SYNC_DDL)) {
      log.info("sync_ddl switch is off, will not auto sync ddl.");
      return;
    }
    invokeExternalColumnAlignment(strategy);

    readerConf.set(ReaderOptions.BaseReaderOptions.COLUMNS, alignedReaderColumns);
    log.info("Final reader's columns: {}", alignedReaderColumns);

    writerConf.set(WriterOptions.BaseWriterOptions.COLUMNS, alignedWriterColumns);
    log.info("Final writer's columns: {}", alignedWriterColumns);

    if (globalConfiguration != null) {
      globalConfiguration.set(ReaderOptions.BaseReaderOptions.COLUMNS, alignedReaderColumns);
      globalConfiguration.set(WriterOptions.BaseWriterOptions.COLUMNS, alignedWriterColumns);
    }
  }

  private void checkColumnsAlignment(SourceEngineConnector sourceSchemaManager,
                                     boolean supportSchemaCheck) {
    Boolean ddlSync = commonConf.get(CommonOptions.SYNC_DDL);
    if (ddlSync != null && ddlSync) {
      log.info("The DDL sync mode is enabled. Columns mapping check will be ignored!");
      return;
    }
    String autoGetColumns = commonConf.get(CommonOptions.COLUMN_ALIGN_STRATEGY);
    if (StringUtils.equalsIgnoreCase(SchemaColumnAligner.ColumnAlignmentStrategy.intersect.toString(), autoGetColumns)) {
      log.info("The auto_get_columns mode is 'intersect'. Columns mapping check will be ignored!");
      return;
    }
    try {
      LinkedHashMap<String, String> readerCols =
          SchemaHelper.getLinkedMapColumnsFromConfColumns(readerConf.get(ReaderOptions.BaseReaderOptions.COLUMNS), false);
      if (supportSchemaCheck) {
        LinkedHashMap<String, String> sourceCols = SchemaHelper.getLinkedMapColumnsFromConfColumns(sourceSchemaManager.getExternalColumnInfos(), false);
        if (SchemaHelper.isColumnsSizeEqual(sourceCols, readerCols, "source", "reader")) {
          SchemaHelper.compareColumnsName(sourceCols, readerCols, "source", "reader");
        }
      }
    } catch (Exception e) {
      log.error("Columns mapping check failed.");
    }
  }
}