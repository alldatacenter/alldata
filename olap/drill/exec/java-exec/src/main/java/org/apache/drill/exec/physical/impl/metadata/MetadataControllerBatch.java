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
package org.apache.drill.exec.physical.impl.metadata;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.drill.common.exceptions.UserException;
import org.apache.drill.common.expression.SchemaPath;
import org.apache.drill.common.types.TypeProtos;
import org.apache.drill.common.types.Types;
import org.apache.drill.exec.exception.OutOfMemoryException;
import org.apache.drill.exec.metastore.ColumnNamesOptions;
import org.apache.drill.exec.metastore.analyze.AnalyzeColumnUtils;
import org.apache.drill.exec.metastore.analyze.MetadataControllerContext;
import org.apache.drill.exec.metastore.analyze.MetadataIdentifierUtils;
import org.apache.drill.exec.metastore.analyze.MetastoreAnalyzeConstants;
import org.apache.drill.exec.ops.FragmentContext;
import org.apache.drill.exec.physical.config.MetadataControllerPOP;
import org.apache.drill.exec.physical.rowSet.DirectRowSet;
import org.apache.drill.exec.physical.rowSet.RowSetReader;
import org.apache.drill.exec.planner.common.DrillStatsTable;
import org.apache.drill.exec.planner.physical.PlannerSettings;
import org.apache.drill.exec.planner.physical.WriterPrel;
import org.apache.drill.exec.record.AbstractBinaryRecordBatch;
import org.apache.drill.exec.record.BatchSchema;
import org.apache.drill.exec.record.RecordBatch;
import org.apache.drill.exec.record.VectorContainer;
import org.apache.drill.exec.record.VectorWrapper;
import org.apache.drill.exec.record.metadata.ColumnMetadata;
import org.apache.drill.exec.record.metadata.TupleMetadata;
import org.apache.drill.exec.store.EventBasedRecordWriter.FieldConverter;
import org.apache.drill.exec.store.StatisticsRecordCollector;
import org.apache.drill.exec.store.StatisticsRecordWriterImpl;
import org.apache.drill.exec.store.easy.json.StatisticsCollectorImpl;
import org.apache.drill.exec.store.parquet.ParquetTableMetadataUtils;
import org.apache.drill.exec.vector.BitVector;
import org.apache.drill.exec.vector.VarCharVector;
import org.apache.drill.exec.vector.accessor.ArrayReader;
import org.apache.drill.exec.vector.accessor.ObjectReader;
import org.apache.drill.exec.vector.accessor.ObjectType;
import org.apache.drill.exec.vector.accessor.TupleReader;
import org.apache.drill.exec.vector.complex.reader.FieldReader;
import org.apache.drill.metastore.MetastoreColumn;
import org.apache.drill.metastore.components.tables.MetastoreTableInfo;
import org.apache.drill.metastore.components.tables.TableMetadataUnit;
import org.apache.drill.metastore.components.tables.Tables;
import org.apache.drill.metastore.expressions.FilterExpression;
import org.apache.drill.metastore.metadata.BaseMetadata;
import org.apache.drill.metastore.metadata.BaseTableMetadata;
import org.apache.drill.metastore.metadata.FileMetadata;
import org.apache.drill.metastore.metadata.MetadataInfo;
import org.apache.drill.metastore.metadata.MetadataType;
import org.apache.drill.metastore.metadata.PartitionMetadata;
import org.apache.drill.metastore.metadata.RowGroupMetadata;
import org.apache.drill.metastore.metadata.SegmentMetadata;
import org.apache.drill.metastore.metadata.TableInfo;
import org.apache.drill.metastore.operate.Delete;
import org.apache.drill.metastore.operate.Modify;
import org.apache.drill.metastore.statistics.BaseStatisticsKind;
import org.apache.drill.metastore.statistics.ColumnStatistics;
import org.apache.drill.metastore.statistics.ColumnStatisticsKind;
import org.apache.drill.metastore.statistics.ExactStatisticsConstants;
import org.apache.drill.metastore.statistics.StatisticsHolder;
import org.apache.drill.metastore.statistics.StatisticsKind;
import org.apache.drill.metastore.statistics.TableStatisticsKind;
import org.apache.drill.shaded.guava.com.google.common.base.Preconditions;
import org.apache.drill.shaded.guava.com.google.common.collect.ArrayListMultimap;
import org.apache.drill.shaded.guava.com.google.common.collect.Multimap;
import org.apache.hadoop.fs.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Terminal operator for producing ANALYZE statement. This operator is
 * responsible for converting obtained metadata, fetching absent metadata from
 * the Metastore and storing resulting metadata into the Metastore.
 * <p>
 * This operator has two inputs: left input contains metadata and right input
 * contains statistics metadata.
 */
public class MetadataControllerBatch extends AbstractBinaryRecordBatch<MetadataControllerPOP> {
  private static final Logger logger = LoggerFactory.getLogger(MetadataControllerBatch.class);

  enum State { RIGHT, LEFT, WRITE, FINISHED }

  private final Tables tables;
  private final TableInfo tableInfo;
  private final Map<String, MetadataInfo> metadataToHandle;
  private final StatisticsRecordCollector statisticsCollector = new StatisticsCollectorImpl();
  private final List<TableMetadataUnit> metadataUnits = new ArrayList<>();
  private final ColumnNamesOptions columnNamesOptions;

  private State state = State.RIGHT;

  protected MetadataControllerBatch(MetadataControllerPOP popConfig,
      FragmentContext context, RecordBatch left, RecordBatch right) throws OutOfMemoryException {
    super(popConfig, context, false, left, right);
    this.tables = context.getMetastoreRegistry().get().tables();
    this.tableInfo = popConfig.getContext().tableInfo();
    this.metadataToHandle = popConfig.getContext().metadataToHandle() == null
        ? null
        : popConfig.getContext().metadataToHandle().stream()
            .collect(Collectors.toMap(MetadataInfo::identifier, Function.identity()));
    this.columnNamesOptions = new ColumnNamesOptions(context.getOptions());
  }

  @Override
  public IterOutcome innerNext() {
    while (state != State.FINISHED) {
      switch (state) {
        case RIGHT: {

          // Can only return NOT_YET
          IterOutcome outcome = handleRightIncoming();
          if (outcome != null) {
            return outcome;
          }
          break;
        }
        case LEFT: {

          // Can only return NOT_YET
          IterOutcome outcome = handleLeftIncoming();
          if (outcome != null) {
            return outcome;
          }
          break;
        }
        case WRITE:
          writeToMetastore();
          createSummary();
          state = State.FINISHED;
          return IterOutcome.OK_NEW_SCHEMA;

        case FINISHED:
          break;

        default:
          throw new IllegalStateException(state.name());
      }
    }
    return IterOutcome.NONE;
  }

  private IterOutcome handleRightIncoming() {
    outer:
    while (true) {
      IterOutcome outcome = next(0, right);
      switch (outcome) {
        case NONE:
          state = State.LEFT;
          break outer;
        case NOT_YET:
          return outcome;
        case OK_NEW_SCHEMA:
        case OK:
          appendStatistics(statisticsCollector);
          break;
        default:
          throw new UnsupportedOperationException("Unsupported upstream state " + outcome);
      }
    }
    return null;
  }

  private IterOutcome handleLeftIncoming() {
    while (true) {
      IterOutcome outcome = next(0, left);
      switch (outcome) {
        case NONE:
          // all incoming data was processed when returned OK_NEW_SCHEMA
          state = State.WRITE;
          return null;
        case NOT_YET:
          return outcome;
        case OK_NEW_SCHEMA:
        case OK:
          metadataUnits.addAll(getMetadataUnits(left.getContainer()));
          break;
        default:
          throw new UnsupportedOperationException("Unsupported upstream state " + outcome);
      }
    }
  }

  private void writeToMetastore() {
    MetadataControllerContext mdContext = popConfig.getContext();
    FilterExpression deleteFilter = mdContext.tableInfo().toFilter();

    for (MetadataInfo metadataInfo : mdContext.metadataToRemove()) {
      deleteFilter = FilterExpression.and(deleteFilter,
          FilterExpression.equal(MetastoreColumn.METADATA_KEY, metadataInfo.key()));
    }

    Modify<TableMetadataUnit> modify = tables.modify();
    if (!popConfig.getContext().metadataToRemove().isEmpty()) {
      modify.delete(Delete.builder()
        .metadataType(MetadataType.SEGMENT, MetadataType.FILE, MetadataType.ROW_GROUP, MetadataType.PARTITION)
        .filter(deleteFilter)
        .build());
    }

    MetastoreTableInfo metastoreTableInfo = mdContext.metastoreTableInfo();
    if (tables.basicRequests().hasMetastoreTableInfoChanged(metastoreTableInfo)) {
      throw UserException.executionError(null)
        .message("Metadata for table [%s] was changed before analyze is finished", tableInfo.name())
        .build(logger);
    }

    modify.overwrite(metadataUnits)
        .execute();
  }

  private void createSummary() {
    container.clear();
    BitVector bitVector =
        container.addOrGet(MetastoreAnalyzeConstants.OK_FIELD_NAME, Types.required(TypeProtos.MinorType.BIT), null);
    VarCharVector varCharVector =
        container.addOrGet(MetastoreAnalyzeConstants.SUMMARY_FIELD_NAME, Types.required(TypeProtos.MinorType.VARCHAR), null);

    bitVector.allocateNew();
    varCharVector.allocateNew();

    bitVector.getMutator().set(0, 1);
    varCharVector.getMutator().setSafe(0,
        String.format("Collected / refreshed metadata for table [%s.%s.%s]",
            popConfig.getContext().tableInfo().storagePlugin(),
            popConfig.getContext().tableInfo().workspace(),
            popConfig.getContext().tableInfo().name()).getBytes());

    container.buildSchema(BatchSchema.SelectionVectorMode.NONE);
    container.setValueCount(1);
  }

  private List<TableMetadataUnit> getMetadataUnits(VectorContainer container) {
    List<TableMetadataUnit> metadataUnits = new ArrayList<>();
    RowSetReader reader = DirectRowSet.fromContainer(container).reader();
    while (reader.next()) {
      metadataUnits.addAll(getMetadataUnits(reader, 0));
    }

    if (metadataToHandle != null) {
      // leaves only table metadata and metadata which belongs to segments to be overridden
      metadataUnits = metadataUnits.stream()
          .filter(tableMetadataUnit ->
              metadataToHandle.values().stream()
                  .map(MetadataInfo::key)
                  .anyMatch(s -> s.equals(tableMetadataUnit.metadataKey()))
              || MetadataType.TABLE.name().equals(tableMetadataUnit.metadataType()))
          .collect(Collectors.toList());

      // leaves only metadata which should be fetched from the Metastore
      metadataUnits.stream()
          .map(TableMetadataUnit::metadataIdentifier)
          .forEach(metadataToHandle::remove);

      List<TableMetadataUnit> metadata = metadataToHandle.isEmpty()
          ? Collections.emptyList()
          : tables.basicRequests().metadata(popConfig.getContext().tableInfo(), metadataToHandle.values());

      metadataUnits.addAll(metadata);
    }

    // checks whether metadataUnits contains not only table metadata before adding default segment
    // to avoid case when only table metadata should be updated and / or root segments removed
    boolean insertDefaultSegment = metadataUnits.size() > 1 && metadataUnits.stream()
        .noneMatch(metadataUnit -> metadataUnit.metadataType().equals(MetadataType.SEGMENT.name()));

    if (insertDefaultSegment) {
      TableMetadataUnit defaultSegmentMetadata = getDefaultSegment(metadataUnits);
      metadataUnits.add(defaultSegmentMetadata);
    }

    return metadataUnits;
  }

  private TableMetadataUnit getDefaultSegment(List<TableMetadataUnit> metadataUnits) {
    TableMetadataUnit tableMetadataUnit = metadataUnits.stream()
        .filter(metadataUnit -> metadataUnit.metadataType().equals(MetadataType.TABLE.name()))
        .findAny()
        .orElseThrow(() -> new IllegalStateException("Table metadata wasn't found among collected metadata."));

    List<String> paths = metadataUnits.stream()
        .filter(metadataUnit -> metadataUnit.metadataType().equals(MetadataType.FILE.name()))
        .map(TableMetadataUnit::path)
        .collect(Collectors.toList());

    return tableMetadataUnit.toBuilder()
        .metadataType(MetadataType.SEGMENT.name())
        .metadataKey(MetadataInfo.DEFAULT_SEGMENT_KEY)
        .metadataIdentifier(MetadataInfo.DEFAULT_SEGMENT_KEY)
        .owner(null)
        .tableType(null)
        .metadataStatistics(Collections.emptyList())
        .columnsStatistics(Collections.emptyMap())
        .path(tableMetadataUnit.location())
        .schema(null)
        .locations(paths)
        .build();
  }

  private List<TableMetadataUnit> getMetadataUnits(TupleReader reader, int nestingLevel) {
    List<TableMetadataUnit> metadataUnits = new ArrayList<>();

    TupleMetadata columnMetadata = reader.tupleSchema();
    ObjectReader metadataColumnReader = reader.column(MetastoreAnalyzeConstants.METADATA_TYPE);
    Preconditions.checkNotNull(metadataColumnReader, "metadataType column wasn't found");

    ObjectReader underlyingMetadataReader = reader.column(MetastoreAnalyzeConstants.COLLECTED_MAP_FIELD);
    if (underlyingMetadataReader != null) {
      if (!underlyingMetadataReader.schema().isArray()) {
        throw new IllegalStateException("Incoming vector with name `collected_map` should be repeated map");
      }
      // current row contains information about underlying metadata
      ArrayReader array = underlyingMetadataReader.array();
      while (array.next()) {
        metadataUnits.addAll(getMetadataUnits(array.tuple(), nestingLevel + 1));
      }
    }

    List<StatisticsHolder<?>> metadataStatistics = getMetadataStatistics(reader, columnMetadata);

    Long rowCount = (Long) metadataStatistics.stream()
        .filter(statisticsHolder -> statisticsHolder.getStatisticsKind() == TableStatisticsKind.ROW_COUNT)
        .findAny()
        .map(StatisticsHolder::getStatisticsValue)
        .orElse(null);

    Map<SchemaPath, ColumnStatistics<?>> columnStatistics = getColumnStatistics(reader, columnMetadata, rowCount);

    MetadataType metadataType = MetadataType.valueOf(metadataColumnReader.scalar().getString());

    BaseMetadata metadata;

    switch (metadataType) {
      case TABLE: {
        metadata = getTableMetadata(reader, metadataStatistics, columnStatistics);
        break;
      }
      case SEGMENT: {
        metadata = getSegmentMetadata(reader, metadataStatistics, columnStatistics, nestingLevel);
        break;
      }
      case PARTITION: {
        metadata = getPartitionMetadata(reader, metadataStatistics, columnStatistics, nestingLevel);
        break;
      }
      case FILE: {
        metadata = getFileMetadata(reader, metadataStatistics, columnStatistics, nestingLevel);
        break;
      }
      case ROW_GROUP: {
        metadata = getRowGroupMetadata(reader, metadataStatistics, columnStatistics, nestingLevel);
        break;
      }
      default:
        throw new UnsupportedOperationException("Unsupported metadata type: " + metadataType);
    }
    metadataUnits.add(metadata.toMetadataUnit());

    return metadataUnits;
  }

  private PartitionMetadata getPartitionMetadata(TupleReader reader, List<StatisticsHolder<?>> metadataStatistics,
      Map<SchemaPath, ColumnStatistics<?>> columnStatistics, int nestingLevel) {
    List<String> segmentColumns = popConfig.getContext().segmentColumns();

    String segmentKey = segmentColumns.size() > 0
        ? reader.column(segmentColumns.iterator().next()).scalar().getString()
        : MetadataInfo.DEFAULT_SEGMENT_KEY;

    List<String> partitionValues = segmentColumns.stream()
        .limit(nestingLevel)
        .map(columnName -> reader.column(columnName).scalar().getString())
        .collect(Collectors.toList());
    String metadataIdentifier = MetadataIdentifierUtils.getMetadataIdentifierKey(partitionValues);
    MetadataInfo metadataInfo = MetadataInfo.builder()
        .type(MetadataType.PARTITION)
        .key(segmentKey)
        .identifier(StringUtils.defaultIfEmpty(metadataIdentifier, null))
        .build();
    return PartitionMetadata.builder()
        .tableInfo(tableInfo)
        .metadataInfo(metadataInfo)
        .columnsStatistics(columnStatistics)
        .metadataStatistics(metadataStatistics)
        .locations(getIncomingLocations(reader))
        .lastModifiedTime(Long.parseLong(reader.column(columnNamesOptions.lastModifiedTime()).scalar().getString()))
//            .column(SchemaPath.getSimplePath("dir1"))
//            .partitionValues()
        .schema(TupleMetadata.of(reader.column(MetastoreAnalyzeConstants.SCHEMA_FIELD).scalar().getString()))
        .build();
  }

  private BaseTableMetadata getTableMetadata(TupleReader reader, List<StatisticsHolder<?>> metadataStatistics,
      Map<SchemaPath, ColumnStatistics<?>> columnStatistics) {
    List<StatisticsHolder<?>> updatedMetaStats = new ArrayList<>(metadataStatistics);
    updatedMetaStats.add(new StatisticsHolder<>(popConfig.getContext().analyzeMetadataLevel(), TableStatisticsKind.ANALYZE_METADATA_LEVEL));

    MetadataInfo metadataInfo = MetadataInfo.builder()
        .type(MetadataType.TABLE)
        .key(MetadataInfo.GENERAL_INFO_KEY)
        .build();

    BaseTableMetadata tableMetadata = BaseTableMetadata.builder()
        .tableInfo(tableInfo)
        .metadataInfo(metadataInfo)
        .columnsStatistics(columnStatistics)
        .metadataStatistics(updatedMetaStats)
        .partitionKeys(Collections.emptyMap())
        .interestingColumns(popConfig.getContext().interestingColumns())
        .location(popConfig.getContext().location())
        .lastModifiedTime(Long.parseLong(reader.column(columnNamesOptions.lastModifiedTime()).scalar().getString()))
        .schema(TupleMetadata.of(reader.column(MetastoreAnalyzeConstants.SCHEMA_FIELD).scalar().getString()))
        .build();

    if (context.getOptions().getOption(PlannerSettings.STATISTICS_USE)) {
      DrillStatsTable statistics = new DrillStatsTable(statisticsCollector.getStatistics());
      Map<SchemaPath, ColumnStatistics<?>> tableColumnStatistics =
          ParquetTableMetadataUtils.getColumnStatistics(tableMetadata.getSchema(), statistics);
      tableMetadata = tableMetadata.cloneWithStats(tableColumnStatistics, DrillStatsTable.getEstimatedTableStats(statistics));
    }

    return tableMetadata;
  }

  private SegmentMetadata getSegmentMetadata(TupleReader reader, List<StatisticsHolder<?>> metadataStatistics,
      Map<SchemaPath, ColumnStatistics<?>> columnStatistics, int nestingLevel) {
    List<String> segmentColumns = popConfig.getContext().segmentColumns();

    String segmentKey = segmentColumns.size() > 0
        ? reader.column(segmentColumns.iterator().next()).scalar().getString()
        : MetadataInfo.DEFAULT_SEGMENT_KEY;

    // for the case of multi-value segments, there is no nesting
    // and therefore all values should be used when forming metadata identifier
    if (popConfig.getContext().multiValueSegments()) {
      nestingLevel = segmentColumns.size();
    }

    List<String> allPartitionValues = segmentColumns.stream()
        .limit(nestingLevel)
        .map(columnName -> reader.column(columnName).scalar().getString())
        .collect(Collectors.toList());
    String metadataIdentifier = MetadataIdentifierUtils.getMetadataIdentifierKey(allPartitionValues);

    MetadataInfo metadataInfo = MetadataInfo.builder()
        .type(MetadataType.SEGMENT)
        .key(segmentKey)
        .identifier(StringUtils.defaultIfEmpty(metadataIdentifier, null))
        .build();

    int segmentLevel = nestingLevel - 1;

    // for the case of multi-value segments, there is no nesting,
    // so all partition column values should be used
    List<String> partitionValues = popConfig.getContext().multiValueSegments()
        ? allPartitionValues
        : Collections.singletonList(allPartitionValues.get(segmentLevel));

    return SegmentMetadata.builder()
        .tableInfo(tableInfo)
        .metadataInfo(metadataInfo)
        .columnsStatistics(columnStatistics)
        .metadataStatistics(metadataStatistics)
        .path(new Path(reader.column(MetastoreAnalyzeConstants.LOCATION_FIELD).scalar().getString()))
        .locations(getIncomingLocations(reader))
        .column(segmentColumns.size() > 0 ? SchemaPath.getSimplePath(segmentColumns.get(segmentLevel)) : null)
        .partitionValues(partitionValues)
        .lastModifiedTime(Long.parseLong(reader.column(columnNamesOptions.lastModifiedTime()).scalar().getString()))
        .schema(TupleMetadata.of(reader.column(MetastoreAnalyzeConstants.SCHEMA_FIELD).scalar().getString()))
        .build();
  }

  private FileMetadata getFileMetadata(TupleReader reader, List<StatisticsHolder<?>> metadataStatistics,
      Map<SchemaPath, ColumnStatistics<?>> columnStatistics, int nestingLevel) {
    List<String> segmentColumns = popConfig.getContext().segmentColumns();

    String segmentKey = segmentColumns.size() > 0
        ? reader.column(segmentColumns.iterator().next()).scalar().getString()
        : MetadataInfo.DEFAULT_SEGMENT_KEY;

    List<String> partitionValues = segmentColumns.stream()
        .limit(nestingLevel - 1)
        .map(columnName -> reader.column(columnName).scalar().getString())
        .collect(Collectors.toList());

    Path path = new Path(reader.column(MetastoreAnalyzeConstants.LOCATION_FIELD).scalar().getString());
    String metadataIdentifier = MetadataIdentifierUtils.getFileMetadataIdentifier(partitionValues, path);

    MetadataInfo metadataInfo = MetadataInfo.builder()
        .type(MetadataType.FILE)
        .key(segmentKey)
        .identifier(StringUtils.defaultIfEmpty(metadataIdentifier, null))
        .build();

    return FileMetadata.builder()
        .tableInfo(tableInfo)
        .metadataInfo(metadataInfo)
        .columnsStatistics(columnStatistics)
        .metadataStatistics(metadataStatistics)
        .path(path)
        .lastModifiedTime(Long.parseLong(reader.column(columnNamesOptions.lastModifiedTime()).scalar().getString()))
        .schema(TupleMetadata.of(reader.column(MetastoreAnalyzeConstants.SCHEMA_FIELD).scalar().getString()))
        .build();
  }

  private RowGroupMetadata getRowGroupMetadata(TupleReader reader,List<StatisticsHolder<?>> metadataStatistics,
      Map<SchemaPath, ColumnStatistics<?>> columnStatistics, int nestingLevel) {

    List<String> segmentColumns = popConfig.getContext().segmentColumns();
    String segmentKey = segmentColumns.size() > 0
        ? reader.column(segmentColumns.iterator().next()).scalar().getString()
        : MetadataInfo.DEFAULT_SEGMENT_KEY;

    List<String> partitionValues = segmentColumns.stream()
        .limit(nestingLevel - 2)
        .map(columnName -> reader.column(columnName).scalar().getString())
        .collect(Collectors.toList());

    Path path = new Path(reader.column(MetastoreAnalyzeConstants.LOCATION_FIELD).scalar().getString());

    int rowGroupIndex = Integer.parseInt(reader.column(columnNamesOptions.rowGroupIndex()).scalar().getString());

    String metadataIdentifier = MetadataIdentifierUtils.getRowGroupMetadataIdentifier(partitionValues, path, rowGroupIndex);

    MetadataInfo metadataInfo = MetadataInfo.builder()
        .type(MetadataType.ROW_GROUP)
        .key(segmentKey)
        .identifier(StringUtils.defaultIfEmpty(metadataIdentifier, null))
        .build();

    return RowGroupMetadata.builder()
        .tableInfo(tableInfo)
        .metadataInfo(metadataInfo)
        .columnsStatistics(columnStatistics)
        .metadataStatistics(metadataStatistics)
        .hostAffinity(Collections.emptyMap())
        .rowGroupIndex(rowGroupIndex)
        .path(path)
        .lastModifiedTime(Long.parseLong(reader.column(columnNamesOptions.lastModifiedTime()).scalar().getString()))
        .schema(TupleMetadata.of(reader.column(MetastoreAnalyzeConstants.SCHEMA_FIELD).scalar().getString()))
        .build();
  }

  private Map<SchemaPath, ColumnStatistics<?>> getColumnStatistics(TupleReader reader, TupleMetadata columnMetadata, Long rowCount) {
    Multimap<String, StatisticsHolder<?>> columnStatistics = ArrayListMultimap.create();
    Map<String, TypeProtos.MinorType> columnTypes = new HashMap<>();
    for (ColumnMetadata column : columnMetadata) {

      if (AnalyzeColumnUtils.isColumnStatisticsField(column.name())) {
        String fieldName = AnalyzeColumnUtils.getColumnName(column.name());
        StatisticsKind<?> statisticsKind = AnalyzeColumnUtils.getStatisticsKind(column.name());
        columnStatistics.put(fieldName,
            new StatisticsHolder<>(getConvertedColumnValue(reader.column(column.name())), statisticsKind));
        if (statisticsKind.getName().equalsIgnoreCase(ColumnStatisticsKind.MIN_VALUE.getName())
            || statisticsKind.getName().equalsIgnoreCase(ColumnStatisticsKind.MAX_VALUE.getName())) {
          columnTypes.putIfAbsent(fieldName, column.type());
        }
      }
    }

    // adds NON_NULL_COUNT to use it during filter pushdown
    if (rowCount != null) {
      Map<String, StatisticsHolder<?>> nullsCountColumnStatistics = new HashMap<>();
      columnStatistics.asMap().forEach((key, value) ->
          value.stream()
              .filter(statisticsHolder -> statisticsHolder.getStatisticsKind() == ColumnStatisticsKind.NON_NULL_VALUES_COUNT)
              .findAny()
              .map(statisticsHolder -> (Long) statisticsHolder.getStatisticsValue())
              .ifPresent(nonNullCount ->
                  nullsCountColumnStatistics.put(
                      key,
                      new StatisticsHolder<>(rowCount - nonNullCount, ColumnStatisticsKind.NULLS_COUNT))));

      nullsCountColumnStatistics.forEach(columnStatistics::put);
    }

    Map<SchemaPath, ColumnStatistics<?>> resultingStats = new HashMap<>();

    columnStatistics.asMap().forEach((fieldName, statisticsHolders) ->
        resultingStats.put(SchemaPath.parseFromString(fieldName), new ColumnStatistics<>(statisticsHolders, columnTypes.get(fieldName))));
    return resultingStats;
  }

  private List<StatisticsHolder<?>> getMetadataStatistics(TupleReader reader, TupleMetadata columnMetadata) {
    List<StatisticsHolder<?>> metadataStatistics = new ArrayList<>();
    String rgs = columnNamesOptions.rowGroupStart();
    String rgl = columnNamesOptions.rowGroupLength();
    for (ColumnMetadata column : columnMetadata) {
      String columnName = column.name();
      ObjectReader objectReader = reader.column(columnName);
      if (AnalyzeColumnUtils.isMetadataStatisticsField(columnName)) {
        metadataStatistics.add(new StatisticsHolder<>(objectReader.getObject(),
            AnalyzeColumnUtils.getStatisticsKind(columnName)));
      } else if (!objectReader.isNull()) {
        if (columnName.equals(rgs)) {
          metadataStatistics.add(new StatisticsHolder<>(Long.parseLong(objectReader.scalar().getString()),
              new BaseStatisticsKind<>(ExactStatisticsConstants.START, true)));
        } else if (columnName.equals(rgl)) {
          metadataStatistics.add(new StatisticsHolder<>(Long.parseLong(objectReader.scalar().getString()),
              new BaseStatisticsKind<>(ExactStatisticsConstants.LENGTH, true)));
        }
      }
    }
    return metadataStatistics;
  }

  private void appendStatistics(StatisticsRecordCollector statisticsCollector) {
    if (context.getOptions().getOption(PlannerSettings.STATISTICS_USE)) {
      List<FieldConverter> fieldConverters = new ArrayList<>();
      int fieldId = 0;

      for (VectorWrapper<?> wrapper : right) {
        if (wrapper.getField().getName().equalsIgnoreCase(WriterPrel.PARTITION_COMPARATOR_FIELD)) {
          continue;
        }
        FieldReader reader = wrapper.getValueVector().getReader();
        FieldConverter converter =
            StatisticsRecordWriterImpl.getConverter(statisticsCollector, fieldId++, wrapper.getField().getName(), reader);
        fieldConverters.add(converter);
      }

      try {
        for (int counter = 0; counter < right.getRecordCount(); counter++) {
          statisticsCollector.startStatisticsRecord();
          // write the current record
          for (FieldConverter converter : fieldConverters) {
            converter.setPosition(counter);
            converter.startField();
            converter.writeField();
            converter.endField();
          }
          statisticsCollector.endStatisticsRecord();
        }
      } catch (IOException e) {
        throw UserException.dataWriteError(e)
            .addContext("Failed to write metadata")
            .build(logger);
      }
    }
  }

  private Object getConvertedColumnValue(ObjectReader objectReader) {
    switch (objectReader.schema().type()) {
      case VARBINARY:
      case FIXEDBINARY:
        return new String(objectReader.scalar().getBytes());
      default:
        return objectReader.getObject();
    }
  }

  private Set<Path> getIncomingLocations(TupleReader reader) {
    Set<Path> childLocations = new HashSet<>();

    ObjectReader metadataColumnReader = reader.column(MetastoreAnalyzeConstants.METADATA_TYPE);
    Preconditions.checkNotNull(metadataColumnReader, "metadataType column wasn't found");

    MetadataType metadataType = MetadataType.valueOf(metadataColumnReader.scalar().getString());

    switch (metadataType) {
      case SEGMENT:
      case PARTITION: {
        ObjectReader locationsReader = reader.column(MetastoreAnalyzeConstants.LOCATIONS_FIELD);
        // populate list of file locations from "locations" field if it is present in the schema
        if (locationsReader != null && locationsReader.type() == ObjectType.ARRAY) {
          ArrayReader array = locationsReader.array();
          while (array.next()) {
            childLocations.add(new Path(array.scalar().getString()));
          }
          break;
        }
        // in the opposite case, populate list of file locations using underlying metadata
        ObjectReader underlyingMetadataReader = reader.column(MetastoreAnalyzeConstants.COLLECTED_MAP_FIELD);
        if (underlyingMetadataReader != null) {
          // current row contains information about underlying metadata
          ArrayReader array = underlyingMetadataReader.array();
          array.rewind();
          while (array.next()) {
            childLocations.addAll(getIncomingLocations(array.tuple()));
          }
        }
        break;
      }
      case FILE: {
        childLocations.add(new Path(reader.column(MetastoreAnalyzeConstants.LOCATION_FIELD).scalar().getString()));
      }
      default:
        break;
    }

    return childLocations;
  }

  @Override
  public void dump() {
    logger.error("MetadataHandlerBatch[container={}, popConfig={}]", container, popConfig);
  }

  @Override
  public int getRecordCount() {
    return container.getRecordCount();
  }
}
