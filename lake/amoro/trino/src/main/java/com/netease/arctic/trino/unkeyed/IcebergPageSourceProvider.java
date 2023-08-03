/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.netease.arctic.trino.unkeyed;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.graph.Traverser;
import com.netease.arctic.data.ChangeAction;
import com.netease.arctic.data.DataFileType;
import com.netease.arctic.iceberg.DeleteFilter;
import com.netease.arctic.table.MetadataColumns;
import com.netease.arctic.trino.delete.DummyFileScanTask;
import com.netease.arctic.trino.delete.TrinoRow;
import io.airlift.json.JsonCodec;
import io.trino.filesystem.TrinoFileSystem;
import io.trino.filesystem.TrinoFileSystemFactory;
import io.trino.filesystem.TrinoInputFile;
import io.trino.memory.context.AggregatedMemoryContext;
import io.trino.orc.OrcColumn;
import io.trino.orc.OrcCorruptionException;
import io.trino.orc.OrcDataSource;
import io.trino.orc.OrcDataSourceId;
import io.trino.orc.OrcReader;
import io.trino.orc.OrcReaderOptions;
import io.trino.orc.OrcRecordReader;
import io.trino.orc.TupleDomainOrcPredicate;
import io.trino.orc.TupleDomainOrcPredicate.TupleDomainOrcPredicateBuilder;
import io.trino.orc.metadata.OrcType;
import io.trino.parquet.ParquetCorruptionException;
import io.trino.parquet.ParquetDataSource;
import io.trino.parquet.ParquetDataSourceId;
import io.trino.parquet.ParquetReaderOptions;
import io.trino.parquet.predicate.TupleDomainParquetPredicate;
import io.trino.parquet.reader.MetadataReader;
import io.trino.parquet.reader.ParquetReader;
import io.trino.parquet.reader.ParquetReaderColumn;
import io.trino.plugin.hive.FileFormatDataSourceStats;
import io.trino.plugin.hive.ReaderColumns;
import io.trino.plugin.hive.ReaderPageSource;
import io.trino.plugin.hive.ReaderProjectionsAdapter;
import io.trino.plugin.hive.orc.OrcPageSource;
import io.trino.plugin.hive.orc.OrcPageSource.ColumnAdaptation;
import io.trino.plugin.hive.orc.OrcReaderConfig;
import io.trino.plugin.hive.parquet.ParquetPageSource;
import io.trino.plugin.hive.parquet.ParquetReaderConfig;
import io.trino.plugin.hive.parquet.TrinoParquetDataSource;
import io.trino.plugin.iceberg.ColumnIdentity;
import io.trino.plugin.iceberg.CommitTaskData;
import io.trino.plugin.iceberg.ConstantPopulatingPageSource;
import io.trino.plugin.iceberg.IcebergAvroPageSource;
import io.trino.plugin.iceberg.IcebergColumnHandle;
import io.trino.plugin.iceberg.IcebergFileFormat;
import io.trino.plugin.iceberg.IcebergFileWriterFactory;
import io.trino.plugin.iceberg.IcebergParquetColumnIOConverter;
import io.trino.plugin.iceberg.IcebergParquetColumnIOConverter.FieldContext;
import io.trino.plugin.iceberg.IcebergTableHandle;
import io.trino.plugin.iceberg.PartitionData;
import io.trino.plugin.iceberg.TrinoOrcDataSource;
import io.trino.plugin.iceberg.delete.IcebergPositionDeletePageSink;
import io.trino.spi.TrinoException;
import io.trino.spi.connector.ColumnHandle;
import io.trino.spi.connector.ConnectorPageSource;
import io.trino.spi.connector.ConnectorPageSourceProvider;
import io.trino.spi.connector.ConnectorSession;
import io.trino.spi.connector.ConnectorSplit;
import io.trino.spi.connector.ConnectorTableHandle;
import io.trino.spi.connector.ConnectorTransactionHandle;
import io.trino.spi.connector.DynamicFilter;
import io.trino.spi.connector.EmptyPageSource;
import io.trino.spi.predicate.Domain;
import io.trino.spi.predicate.TupleDomain;
import io.trino.spi.type.ArrayType;
import io.trino.spi.type.MapType;
import io.trino.spi.type.RowType;
import io.trino.spi.type.StandardTypes;
import io.trino.spi.type.Type;
import io.trino.spi.type.TypeManager;
import org.apache.avro.file.DataFileStream;
import org.apache.avro.generic.GenericDatumReader;
import org.apache.hadoop.hdfs.BlockMissingException;
import org.apache.iceberg.FileScanTask;
import org.apache.iceberg.PartitionSpec;
import org.apache.iceberg.PartitionSpecParser;
import org.apache.iceberg.Schema;
import org.apache.iceberg.SchemaParser;
import org.apache.iceberg.avro.AvroSchemaUtil;
import org.apache.iceberg.io.FileIO;
import org.apache.iceberg.io.InputFile;
import org.apache.iceberg.io.LocationProvider;
import org.apache.iceberg.mapping.MappedField;
import org.apache.iceberg.mapping.MappedFields;
import org.apache.iceberg.mapping.NameMapping;
import org.apache.iceberg.mapping.NameMappingParser;
import org.apache.iceberg.parquet.ParquetSchemaUtil;
import org.apache.parquet.column.ColumnDescriptor;
import org.apache.parquet.hadoop.metadata.BlockMetaData;
import org.apache.parquet.hadoop.metadata.FileMetaData;
import org.apache.parquet.hadoop.metadata.ParquetMetadata;
import org.apache.parquet.io.ColumnIO;
import org.apache.parquet.io.MessageColumnIO;
import org.apache.parquet.schema.MessageType;
import org.joda.time.DateTimeZone;
import javax.inject.Inject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.Set;
import java.util.TimeZone;
import java.util.function.Function;
import java.util.function.Supplier;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.base.Verify.verify;
import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.collect.ImmutableMap.toImmutableMap;
import static com.google.common.collect.ImmutableSet.toImmutableSet;
import static com.google.common.collect.Maps.uniqueIndex;
import static io.airlift.slice.Slices.utf8Slice;
import static io.trino.memory.context.AggregatedMemoryContext.newSimpleAggregatedMemoryContext;
import static io.trino.orc.OrcReader.INITIAL_BATCH_SIZE;
import static io.trino.orc.OrcReader.ProjectedLayout;
import static io.trino.orc.OrcReader.fullyProjectedLayout;
import static io.trino.parquet.ParquetTypeUtils.getColumnIO;
import static io.trino.parquet.ParquetTypeUtils.getDescriptors;
import static io.trino.parquet.predicate.PredicateUtils.buildPredicate;
import static io.trino.parquet.predicate.PredicateUtils.predicateMatches;
import static io.trino.parquet.reader.ParquetReaderColumn.getParquetReaderFields;
import static io.trino.plugin.iceberg.IcebergColumnHandle.TRINO_MERGE_FILE_RECORD_COUNT;
import static io.trino.plugin.iceberg.IcebergColumnHandle.TRINO_MERGE_PARTITION_DATA;
import static io.trino.plugin.iceberg.IcebergColumnHandle.TRINO_MERGE_PARTITION_SPEC_ID;
import static io.trino.plugin.iceberg.IcebergErrorCode.ICEBERG_BAD_DATA;
import static io.trino.plugin.iceberg.IcebergErrorCode.ICEBERG_CANNOT_OPEN_SPLIT;
import static io.trino.plugin.iceberg.IcebergErrorCode.ICEBERG_CURSOR_ERROR;
import static io.trino.plugin.iceberg.IcebergErrorCode.ICEBERG_MISSING_DATA;
import static io.trino.plugin.iceberg.IcebergMetadataColumn.FILE_MODIFIED_TIME;
import static io.trino.plugin.iceberg.IcebergMetadataColumn.FILE_PATH;
import static io.trino.plugin.iceberg.IcebergSessionProperties.getOrcLazyReadSmallRanges;
import static io.trino.plugin.iceberg.IcebergSessionProperties.getOrcMaxBufferSize;
import static io.trino.plugin.iceberg.IcebergSessionProperties.getOrcMaxMergeDistance;
import static io.trino.plugin.iceberg.IcebergSessionProperties.getOrcMaxReadBlockSize;
import static io.trino.plugin.iceberg.IcebergSessionProperties.getOrcStreamBufferSize;
import static io.trino.plugin.iceberg.IcebergSessionProperties.getOrcTinyStripeThreshold;
import static io.trino.plugin.iceberg.IcebergSessionProperties.getParquetMaxReadBlockRowCount;
import static io.trino.plugin.iceberg.IcebergSessionProperties.getParquetMaxReadBlockSize;
import static io.trino.plugin.iceberg.IcebergSessionProperties.isOrcBloomFiltersEnabled;
import static io.trino.plugin.iceberg.IcebergSessionProperties.isOrcNestedLazy;
import static io.trino.plugin.iceberg.IcebergSessionProperties.isParquetOptimizedReaderEnabled;
import static io.trino.plugin.iceberg.IcebergSessionProperties.isUseFileSizeFromMetadata;
import static io.trino.plugin.iceberg.IcebergSplitManager.ICEBERG_DOMAIN_COMPACTION_THRESHOLD;
import static io.trino.plugin.iceberg.IcebergUtil.deserializePartitionValue;
import static io.trino.plugin.iceberg.IcebergUtil.getColumns;
import static io.trino.plugin.iceberg.IcebergUtil.getLocationProvider;
import static io.trino.plugin.iceberg.IcebergUtil.getPartitionKeys;
import static io.trino.plugin.iceberg.TypeConverter.ICEBERG_BINARY_TYPE;
import static io.trino.plugin.iceberg.TypeConverter.ORC_ICEBERG_ID_KEY;
import static io.trino.spi.StandardErrorCode.NOT_SUPPORTED;
import static io.trino.spi.predicate.Utils.nativeValueToBlock;
import static io.trino.spi.type.BigintType.BIGINT;
import static io.trino.spi.type.BooleanType.BOOLEAN;
import static io.trino.spi.type.DateTimeEncoding.packDateTimeWithZone;
import static io.trino.spi.type.TimeZoneKey.UTC_KEY;
import static io.trino.spi.type.UuidType.UUID;
import static java.lang.String.format;
import static java.util.Locale.ENGLISH;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toUnmodifiableList;
import static org.apache.iceberg.MetadataColumns.ROW_POSITION;
import static org.joda.time.DateTimeZone.UTC;

/**
 * Extend IcebergPageSourceProvider to provider idToConstant that the map of columns id to constant value
 */
public class IcebergPageSourceProvider
    implements ConnectorPageSourceProvider {
  private static final String AVRO_FIELD_ID = "field-id";
  private final TrinoFileSystemFactory fileSystemFactory;
  private final FileFormatDataSourceStats fileFormatDataSourceStats;
  private final OrcReaderOptions orcReaderOptions;
  private final ParquetReaderOptions parquetReaderOptions;
  private final TypeManager typeManager;
  private final JsonCodec<CommitTaskData> jsonCodec;
  private final IcebergFileWriterFactory fileWriterFactory;

  @Inject
  public IcebergPageSourceProvider(
      TrinoFileSystemFactory fileSystemFactory,
      FileFormatDataSourceStats fileFormatDataSourceStats,
      OrcReaderConfig orcReaderConfig,
      ParquetReaderConfig parquetReaderConfig,
      TypeManager typeManager,
      JsonCodec<CommitTaskData> jsonCodec,
      IcebergFileWriterFactory fileWriterFactory) {
    this.fileSystemFactory = requireNonNull(fileSystemFactory, "fileSystemFactory is null");
    this.fileFormatDataSourceStats = requireNonNull(fileFormatDataSourceStats, "fileFormatDataSourceStats is null");
    this.orcReaderOptions = requireNonNull(orcReaderConfig, "orcReaderConfig is null").toOrcReaderOptions();
    this.parquetReaderOptions =
        requireNonNull(parquetReaderConfig, "parquetReaderConfig is null").toParquetReaderOptions();
    this.typeManager = requireNonNull(typeManager, "typeManager is null");
    this.jsonCodec = requireNonNull(jsonCodec, "jsonCodec is null");
    this.fileWriterFactory = requireNonNull(fileWriterFactory, "fileWriterFactory is null");
  }

  @Override
  public ConnectorPageSource createPageSource(
      ConnectorTransactionHandle transaction,
      ConnectorSession session,
      ConnectorSplit connectorSplit,
      ConnectorTableHandle connectorTable,
      List<ColumnHandle> columns,
      DynamicFilter dynamicFilter) {
    Map<Integer, Optional<String>> idToConstant = new HashMap<>();
    IcebergSplit icebergSplit = (IcebergSplit) connectorSplit;
    idToConstant.put(
        MetadataColumns.TRANSACTION_ID_FILED_ID,
        Optional.ofNullable(icebergSplit.getTransactionId()).map(s -> s.toString()));
    idToConstant.put(
        MetadataColumns.CHANGE_ACTION_ID,
        Optional.ofNullable(icebergSplit.getFileType())
            .map(s -> s == DataFileType.EQ_DELETE_FILE ? ChangeAction.DELETE.name() : ChangeAction.INSERT.name()));
    DateTimeZone dateTimeZone = UTC;
    if (connectorTable instanceof AdaptHiveIcebergTableHandle) {
      dateTimeZone = DateTimeZone.forID(TimeZone.getDefault().getID());
    }
    return createPageSource(transaction, session, connectorSplit, connectorTable, columns, dynamicFilter,
        idToConstant, true, dateTimeZone);
  }

  public ConnectorPageSource createPageSource(
      ConnectorTransactionHandle transaction,
      ConnectorSession session,
      ConnectorSplit connectorSplit,
      ConnectorTableHandle connectorTable,
      List<ColumnHandle> columns,
      DynamicFilter dynamicFilter,
      Map<Integer, Optional<String>> idToConstant,
      boolean useIcebergDelete,
      DateTimeZone dateTimeZone) {
    IcebergSplit split = (IcebergSplit) connectorSplit;
    IcebergTableHandle table = (IcebergTableHandle) connectorTable;

    List<IcebergColumnHandle> icebergColumns = columns.stream()
        .map(IcebergColumnHandle.class::cast)
        .collect(toImmutableList());

    TrinoFileSystem fileSystem = fileSystemFactory.create(session);
    FileIO fileIO = fileSystem.toFileIo();
    FileScanTask dummyFileScanTask = new DummyFileScanTask(split.getPath(), split.getDeletes());
    Schema tableSchema = SchemaParser.fromJson(table.getTableSchemaJson());
    // Creating a DeleteFilter with no requestedSchema ensures `deleteFilterRequiredSchema`
    // is only columns needed by the filter.
    List<IcebergColumnHandle> deleteFilterRequiredSchema = getColumns(
        useIcebergDelete ?
            new TrinoDeleteFilter(
                dummyFileScanTask,
                tableSchema,
                ImmutableList.of(),
                fileIO)
                .requiredSchema() : tableSchema,
        typeManager);

    PartitionSpec partitionSpec = PartitionSpecParser.fromJson(tableSchema, split.getPartitionSpecJson());
    org.apache.iceberg.types.Type[] partitionColumnTypes = partitionSpec.fields().stream()
        .map(field -> field.transform().getResultType(tableSchema.findType(field.sourceId())))
        .toArray(org.apache.iceberg.types.Type[]::new);
    PartitionData partitionData = PartitionData.fromJson(split.getPartitionDataJson(), partitionColumnTypes);
    Map<Integer, Optional<String>> partitionKeys = getPartitionKeys(partitionData, partitionSpec);
    //for arctic
    if (idToConstant != null) {
      partitionKeys = ImmutableMap.<Integer, Optional<String>>builder()
          .putAll(partitionKeys)
          .putAll(idToConstant)
          .buildOrThrow();
    }

    ImmutableList.Builder<IcebergColumnHandle> requiredColumnsBuilder = ImmutableList.builder();
    requiredColumnsBuilder.addAll(icebergColumns);
    deleteFilterRequiredSchema.stream()
        .filter(column -> !icebergColumns.contains(column))
        .forEach(requiredColumnsBuilder::add);
    List<IcebergColumnHandle> requiredColumns = requiredColumnsBuilder.build();

    TupleDomain<IcebergColumnHandle> effectivePredicate = table.getUnenforcedPredicate()
        .intersect(dynamicFilter.getCurrentPredicate().transformKeys(IcebergColumnHandle.class::cast))
        .simplify(ICEBERG_DOMAIN_COMPACTION_THRESHOLD);
    if (effectivePredicate.isNone()) {
      return new EmptyPageSource();
    }

    TrinoInputFile inputfile = isUseFileSizeFromMetadata(session) ?
        fileSystem.newInputFile(split.getPath(), split.getFileSize())
        : fileSystem.newInputFile(split.getPath());

    IcebergPageSourceProvider.ReaderPageSourceWithRowPositions readerPageSourceWithRowPositions = createDataPageSource(
        session,
        fileSystem,
        inputfile,
        split.getStart(),
        split.getLength(),
        split.getFileRecordCount(),
        partitionSpec.specId(),
        split.getPartitionDataJson(),
        split.getFileFormat(),
        SchemaParser.fromJson(table.getTableSchemaJson()),
        requiredColumns,
        effectivePredicate,
        table.getNameMappingJson().map(NameMappingParser::fromJson),
        partitionKeys,
        dateTimeZone);
    ReaderPageSource dataPageSource = readerPageSourceWithRowPositions.getReaderPageSource();

    Optional<ReaderProjectionsAdapter> projectionsAdapter = dataPageSource.getReaderColumns().map(readerColumns ->
        new ReaderProjectionsAdapter(
            requiredColumns,
            readerColumns,
            column -> ((IcebergColumnHandle) column).getType(),
            IcebergPageSourceProvider::applyProjection));

    DeleteFilter<TrinoRow> deleteFilter = new TrinoDeleteFilter(
        dummyFileScanTask,
        tableSchema,
        requiredColumns,
        fileIO);

    Optional<PartitionData> partition = partitionSpec.isUnpartitioned() ? Optional.empty() : Optional.of(partitionData);
    LocationProvider locationProvider =
        getLocationProvider(table.getSchemaTableName(), table.getTableLocation(), table.getStorageProperties());
    Supplier<IcebergPositionDeletePageSink> positionDeleteSink = () -> new IcebergPositionDeletePageSink(
        split.getPath(),
        partitionSpec,
        partition,
        locationProvider,
        fileWriterFactory,
        fileSystem,
        jsonCodec,
        session,
        split.getFileFormat(),
        table.getStorageProperties(),
        split.getFileRecordCount());

    return new IcebergPageSource(
        icebergColumns,
        requiredColumns,
        dataPageSource.get(),
        projectionsAdapter,
        //                Optional.of(deleteFilter).filter(filter -> filter.hasPosDeletes() || filter.hasEqDeletes()),
        // In order to be compatible with iceberg version 0.12
        useIcebergDelete ? Optional.of(deleteFilter) : Optional.empty(),
        positionDeleteSink);
  }

  public ReaderPageSourceWithRowPositions createDataPageSource(
      ConnectorSession session,
      TrinoFileSystem fileSystem,
      TrinoInputFile inputFile,
      long start,
      long length,
      long fileRecordCount,
      int partitionSpecId,
      String partitionData,
      IcebergFileFormat fileFormat,
      Schema fileSchema,
      List<IcebergColumnHandle> dataColumns,
      TupleDomain<IcebergColumnHandle> predicate,
      Optional<NameMapping> nameMapping,
      Map<Integer, Optional<String>> partitionKeys,
      DateTimeZone dateTimeZone) {
    switch (fileFormat) {
      case ORC:
        return createOrcPageSource(
            inputFile,
            start,
            length,
            fileRecordCount,
            partitionSpecId,
            partitionData,
            dataColumns,
            predicate,
            orcReaderOptions
                .withMaxMergeDistance(getOrcMaxMergeDistance(session))
                .withMaxBufferSize(getOrcMaxBufferSize(session))
                .withStreamBufferSize(getOrcStreamBufferSize(session))
                .withTinyStripeThreshold(getOrcTinyStripeThreshold(session))
                .withMaxReadBlockSize(getOrcMaxReadBlockSize(session))
                .withLazyReadSmallRanges(getOrcLazyReadSmallRanges(session))
                .withNestedLazy(isOrcNestedLazy(session))
                .withBloomFiltersEnabled(isOrcBloomFiltersEnabled(session)),
            fileFormatDataSourceStats,
            typeManager,
            nameMapping,
            partitionKeys);
      case PARQUET:
        return createParquetPageSource(
            inputFile,
            start,
            length,
            fileRecordCount,
            partitionSpecId,
            partitionData,
            dataColumns,
            parquetReaderOptions
                .withMaxReadBlockSize(getParquetMaxReadBlockSize(session))
                .withMaxReadBlockRowCount(getParquetMaxReadBlockRowCount(session))
                .withBatchColumnReaders(isParquetOptimizedReaderEnabled(session)),
            predicate,
            fileFormatDataSourceStats,
            nameMapping,
            partitionKeys,
            dateTimeZone);
      case AVRO:
        return createAvroPageSource(
            fileSystem,
            inputFile,
            start,
            length,
            fileRecordCount,
            partitionSpecId,
            partitionData,
            fileSchema,
            nameMapping,
            dataColumns);
      default:
        throw new TrinoException(NOT_SUPPORTED, "File format not supported for Iceberg: " + fileFormat);
    }
  }

  private static ReaderPageSourceWithRowPositions createOrcPageSource(
      TrinoInputFile inputFile,
      long start,
      long length,
      long fileRecordCount,
      int partitionSpecId,
      String partitionData,
      List<IcebergColumnHandle> columns,
      TupleDomain<IcebergColumnHandle> effectivePredicate,
      OrcReaderOptions options,
      FileFormatDataSourceStats stats,
      TypeManager typeManager,
      Optional<NameMapping> nameMapping,
      Map<Integer, Optional<String>> partitionKeys) {
    OrcDataSource orcDataSource = null;
    try {
      orcDataSource = new TrinoOrcDataSource(inputFile, options, stats);

      OrcReader reader = OrcReader.createOrcReader(orcDataSource, options)
          .orElseThrow(() -> new TrinoException(ICEBERG_BAD_DATA, "ORC file is zero length"));

      List<OrcColumn> fileColumns = reader.getRootColumn().getNestedColumns();
      if (nameMapping.isPresent() && !hasIds(reader.getRootColumn())) {
        fileColumns = fileColumns.stream()
            .map(orcColumn -> setMissingFieldIds(
                orcColumn, nameMapping.get(), ImmutableList.of(orcColumn.getColumnName())))
            .collect(toImmutableList());
      }

      Map<Integer, OrcColumn> fileColumnsByIcebergId = mapIdsToOrcFileColumns(fileColumns);

      TupleDomainOrcPredicateBuilder predicateBuilder = TupleDomainOrcPredicate.builder()
          .setBloomFiltersEnabled(options.isBloomFiltersEnabled());
      Map<IcebergColumnHandle, Domain> effectivePredicateDomains = effectivePredicate.getDomains()
          .orElseThrow(() -> new IllegalArgumentException("Effective predicate is none"));

      Optional<ReaderColumns> columnProjections = projectColumns(columns);
      Map<Integer, List<List<Integer>>> projectionsByFieldId = columns.stream()
          .collect(groupingBy(
              column -> column.getBaseColumnIdentity().getId(),
              mapping(IcebergColumnHandle::getPath, toUnmodifiableList())));

      List<IcebergColumnHandle> readColumns = columnProjections
          .map(readerColumns -> (List<IcebergColumnHandle>) readerColumns.get()
              .stream().map(IcebergColumnHandle.class::cast).collect(toImmutableList()))
          .orElse(columns);
      List<OrcColumn> fileReadColumns = new ArrayList<>(readColumns.size());
      List<Type> fileReadTypes = new ArrayList<>(readColumns.size());
      List<ProjectedLayout> projectedLayouts = new ArrayList<>(readColumns.size());
      List<ColumnAdaptation> columnAdaptations = new ArrayList<>(readColumns.size());

      for (IcebergColumnHandle column : readColumns) {
        verify(column.isBaseColumn(), "Column projections must be based from a root column");
        OrcColumn orcColumn = fileColumnsByIcebergId.get(column.getId());

        if (column.isIsDeletedColumn()) {
          columnAdaptations.add(ColumnAdaptation.constantColumn(nativeValueToBlock(BOOLEAN, false)));
        } else if (partitionKeys.containsKey(column.getId())) {
          Type trinoType = column.getType();
          columnAdaptations.add(ColumnAdaptation.constantColumn(nativeValueToBlock(
              trinoType,
              deserializePartitionValue(trinoType, partitionKeys.get(column.getId()).orElse(null), column.getName()))));
        } else if (column.isPathColumn()) {
          columnAdaptations.add(
              ColumnAdaptation.constantColumn(
                  nativeValueToBlock(FILE_PATH.getType(), utf8Slice(inputFile.location()))));
        } else if (column.isFileModifiedTimeColumn()) {
          columnAdaptations.add(
              ColumnAdaptation.constantColumn(
                  nativeValueToBlock(
                      FILE_MODIFIED_TIME.getType(),
                      packDateTimeWithZone(inputFile.modificationTime(), UTC_KEY))));
        } else if (column.isUpdateRowIdColumn() || column.isMergeRowIdColumn()) {
          // $row_id is a composite of multiple physical columns. It is assembled by the IcebergPageSource
          columnAdaptations.add(ColumnAdaptation.nullColumn(column.getType()));
        } else if (column.isRowPositionColumn()) {
          columnAdaptations.add(ColumnAdaptation.positionColumn());
        } else if (column.getId() == TRINO_MERGE_FILE_RECORD_COUNT) {
          columnAdaptations.add(ColumnAdaptation.constantColumn(nativeValueToBlock(column.getType(), fileRecordCount)));
        } else if (column.getId() == TRINO_MERGE_PARTITION_SPEC_ID) {
          columnAdaptations.add(
              ColumnAdaptation.constantColumn(nativeValueToBlock(column.getType(), (long) partitionSpecId)));
        } else if (column.getId() == TRINO_MERGE_PARTITION_DATA) {
          columnAdaptations.add(
              ColumnAdaptation.constantColumn(nativeValueToBlock(column.getType(), utf8Slice(partitionData))));
        } else if (orcColumn != null) {
          Type readType = getOrcReadType(column.getType(), typeManager);

          if (column.getType() == UUID && !"UUID".equals(orcColumn.getAttributes().get(ICEBERG_BINARY_TYPE))) {
            throw new TrinoException(
                ICEBERG_BAD_DATA,
                format(
                    "Expected ORC column for UUID data to be annotated with %s=UUID: %s",
                    ICEBERG_BINARY_TYPE,
                    orcColumn)
            );
          }

          List<List<Integer>> fieldIdProjections = projectionsByFieldId.get(column.getId());
          ProjectedLayout projectedLayout = IcebergPageSourceProvider
              .IcebergOrcProjectedLayout.createProjectedLayout(orcColumn, fieldIdProjections);

          int sourceIndex = fileReadColumns.size();
          columnAdaptations.add(ColumnAdaptation.sourceColumn(sourceIndex));
          fileReadColumns.add(orcColumn);
          fileReadTypes.add(readType);
          projectedLayouts.add(projectedLayout);

          for (Map.Entry<IcebergColumnHandle, Domain> domainEntry : effectivePredicateDomains.entrySet()) {
            IcebergColumnHandle predicateColumn = domainEntry.getKey();
            OrcColumn predicateOrcColumn = fileColumnsByIcebergId.get(predicateColumn.getId());
            if (predicateOrcColumn != null &&
                column.getColumnIdentity().equals(predicateColumn.getBaseColumnIdentity())) {
              predicateBuilder.addColumn(predicateOrcColumn.getColumnId(), domainEntry.getValue());
            }
          }
        } else {
          columnAdaptations.add(ColumnAdaptation.nullColumn(column.getType()));
        }
      }

      AggregatedMemoryContext memoryUsage = newSimpleAggregatedMemoryContext();
      OrcDataSourceId orcDataSourceId = orcDataSource.getId();
      OrcRecordReader recordReader = reader.createRecordReader(
          fileReadColumns,
          fileReadTypes,
          projectedLayouts,
          predicateBuilder.build(),
          start,
          length,
          UTC,
          memoryUsage,
          INITIAL_BATCH_SIZE,
          exception -> handleException(orcDataSourceId, exception),
          new IcebergPageSourceProvider.IdBasedFieldMapperFactory(readColumns));

      return new ReaderPageSourceWithRowPositions(
          new ReaderPageSource(
              new OrcPageSource(
                  recordReader,
                  columnAdaptations,
                  orcDataSource,
                  Optional.empty(),
                  Optional.empty(),
                  memoryUsage,
                  stats,
                  reader.getCompressionKind()),
              columnProjections),
          Optional.empty(),
          Optional.empty());
    } catch (IOException | RuntimeException e) {
      if (orcDataSource != null) {
        try {
          orcDataSource.close();
        } catch (IOException ex) {
          if (!e.equals(ex)) {
            e.addSuppressed(ex);
          }
        }
      }
      if (e instanceof TrinoException) {
        throw (TrinoException) e;
      }
      String message = format(
          "Error opening Iceberg split %s (offset=%s, length=%s): %s",
          inputFile.location(), start, length, e.getMessage());
      if (e instanceof BlockMissingException) {
        throw new TrinoException(ICEBERG_MISSING_DATA, message, e);
      }
      throw new TrinoException(ICEBERG_CANNOT_OPEN_SPLIT, message, e);
    }
  }

  private static boolean hasIds(OrcColumn column) {
    if (column.getAttributes().containsKey(ORC_ICEBERG_ID_KEY)) {
      return true;
    }

    return column.getNestedColumns().stream().anyMatch(IcebergPageSourceProvider::hasIds);
  }

  private static OrcColumn setMissingFieldIds(OrcColumn column, NameMapping nameMapping, List<String> qualifiedPath) {
    MappedField mappedField = nameMapping.find(qualifiedPath);

    ImmutableMap.Builder<String, String> attributes = ImmutableMap.<String, String>builder()
        .putAll(column.getAttributes());
    if (mappedField != null && mappedField.id() != null) {
      attributes.put(ORC_ICEBERG_ID_KEY, String.valueOf(mappedField.id()));
    }

    return new OrcColumn(
        column.getPath(),
        column.getColumnId(),
        column.getColumnName(),
        column.getColumnType(),
        column.getOrcDataSourceId(),
        column.getNestedColumns().stream()
            .map(nestedColumn -> {
              ImmutableList.Builder<String> nextQualifiedPath = ImmutableList.<String>builder()
                  .addAll(qualifiedPath);
              if (column.getColumnType() == OrcType.OrcTypeKind.LIST) {
                // The Trino ORC reader uses "item" for list element names, but the NameMapper expects "element"
                nextQualifiedPath.add("element");
              } else {
                nextQualifiedPath.add(nestedColumn.getColumnName());
              }
              return setMissingFieldIds(nestedColumn, nameMapping, nextQualifiedPath.build());
            })
            .collect(toImmutableList()),
        attributes.buildOrThrow());
  }

  /**
   * Gets the index based dereference chain to get from the readColumnHandle to the expectedColumnHandle
   */
  private static List<Integer> applyProjection(ColumnHandle expectedColumnHandle, ColumnHandle readColumnHandle) {
    IcebergColumnHandle expectedColumn = (IcebergColumnHandle) expectedColumnHandle;
    IcebergColumnHandle readColumn = (IcebergColumnHandle) readColumnHandle;
    checkState(readColumn.isBaseColumn(), "Read column path must be a base column");

    ImmutableList.Builder<Integer> dereferenceChain = ImmutableList.builder();
    ColumnIdentity columnIdentity = readColumn.getColumnIdentity();
    for (Integer fieldId : expectedColumn.getPath()) {
      ColumnIdentity nextChild = columnIdentity.getChildByFieldId(fieldId);
      dereferenceChain.add(columnIdentity.getChildIndexByFieldId(fieldId));
      columnIdentity = nextChild;
    }

    return dereferenceChain.build();
  }

  private static Map<Integer, OrcColumn> mapIdsToOrcFileColumns(List<OrcColumn> columns) {
    ImmutableMap.Builder<Integer, OrcColumn> columnsById = ImmutableMap.builder();
    Traverser.forTree(OrcColumn::getNestedColumns)
        .depthFirstPreOrder(columns)
        .forEach(column -> {
          String fieldId = column.getAttributes().get(ORC_ICEBERG_ID_KEY);
          if (fieldId != null) {
            columnsById.put(Integer.parseInt(fieldId), column);
          }
        });
    return columnsById.buildOrThrow();
  }

  private static Integer getIcebergFieldId(OrcColumn column) {
    String icebergId = column.getAttributes().get(ORC_ICEBERG_ID_KEY);
    verify(icebergId != null, format("column %s does not have %s property", column, ORC_ICEBERG_ID_KEY));
    return Integer.valueOf(icebergId);
  }

  private static Type getOrcReadType(Type columnType, TypeManager typeManager) {
    if (columnType instanceof ArrayType) {
      return new ArrayType(getOrcReadType(((ArrayType) columnType).getElementType(), typeManager));
    }
    if (columnType instanceof MapType mapType) {
      Type keyType = getOrcReadType(mapType.getKeyType(), typeManager);
      Type valueType = getOrcReadType(mapType.getValueType(), typeManager);
      return new MapType(keyType, valueType, typeManager.getTypeOperators());
    }
    if (columnType instanceof RowType) {
      return RowType.from(((RowType) columnType).getFields().stream()
          .map(field -> new RowType.Field(field.getName(), getOrcReadType(field.getType(), typeManager)))
          .collect(toImmutableList()));
    }

    return columnType;
  }

  private static class IdBasedFieldMapperFactory
      implements OrcReader.FieldMapperFactory {
    // Stores a mapping between subfield names and ids for every top-level/nested column id
    private final Map<Integer, Map<String, Integer>> fieldNameToIdMappingForTableColumns;

    public IdBasedFieldMapperFactory(List<IcebergColumnHandle> columns) {
      requireNonNull(columns, "columns is null");

      ImmutableMap.Builder<Integer, Map<String, Integer>> mapping = ImmutableMap.builder();
      for (IcebergColumnHandle column : columns) {
        if (column.isUpdateRowIdColumn() || column.isMergeRowIdColumn()) {
          // The update $row_id column contains fields which should not be accounted for in the mapping.
          continue;
        }

        // Recursively compute subfield name to id mapping for every column
        populateMapping(column.getColumnIdentity(), mapping);
      }

      this.fieldNameToIdMappingForTableColumns = mapping.buildOrThrow();
    }

    @Override
    public OrcReader.FieldMapper create(OrcColumn column) {
      Map<Integer, OrcColumn> nestedColumns = uniqueIndex(
          column.getNestedColumns(),
          IcebergPageSourceProvider::getIcebergFieldId);

      int icebergId = getIcebergFieldId(column);
      return new IdBasedFieldMapper(nestedColumns, fieldNameToIdMappingForTableColumns.get(icebergId));
    }

    private static void populateMapping(
        ColumnIdentity identity,
        ImmutableMap.Builder<Integer, Map<String, Integer>> fieldNameToIdMappingForTableColumns) {
      List<ColumnIdentity> children = identity.getChildren();
      fieldNameToIdMappingForTableColumns.put(
          identity.getId(),
          children.stream()
              // Lower casing is required here because ORC StructColumnReader does the same before mapping
              .collect(toImmutableMap(child -> child.getName().toLowerCase(ENGLISH), ColumnIdentity::getId)));

      for (ColumnIdentity child : children) {
        populateMapping(child, fieldNameToIdMappingForTableColumns);
      }
    }
  }

  private static class IdBasedFieldMapper
      implements OrcReader.FieldMapper {
    private final Map<Integer, OrcColumn> idToColumnMappingForFile;
    private final Map<String, Integer> nameToIdMappingForTableColumns;

    public IdBasedFieldMapper(
        Map<Integer, OrcColumn> idToColumnMappingForFile,
        Map<String, Integer> nameToIdMappingForTableColumns) {
      this.idToColumnMappingForFile = requireNonNull(idToColumnMappingForFile, "idToColumnMappingForFile is null");
      this.nameToIdMappingForTableColumns = requireNonNull(
          nameToIdMappingForTableColumns, "nameToIdMappingForTableColumns is null");
    }

    @Override
    public OrcColumn get(String fieldName) {
      int fieldId = requireNonNull(
          nameToIdMappingForTableColumns.get(fieldName),
          () -> format("Id mapping for field %s not found", fieldName));
      return idToColumnMappingForFile.get(fieldId);
    }
  }

  private static ReaderPageSourceWithRowPositions createParquetPageSource(
      TrinoInputFile inputFile,
      long start,
      long length,
      long fileRecordCount,
      int partitionSpecId,
      String partitionData,
      List<IcebergColumnHandle> regularColumns,
      ParquetReaderOptions options,
      TupleDomain<IcebergColumnHandle> effectivePredicate,
      FileFormatDataSourceStats fileFormatDataSourceStats,
      Optional<NameMapping> nameMapping,
      Map<Integer, Optional<String>> partitionKeys,
      DateTimeZone dateTimeZone) {
    AggregatedMemoryContext memoryContext = newSimpleAggregatedMemoryContext();

    ParquetDataSource dataSource = null;
    try {
      dataSource = new TrinoParquetDataSource(inputFile, options, fileFormatDataSourceStats);
      ParquetMetadata parquetMetadata = MetadataReader.readFooter(dataSource, Optional.empty());
      FileMetaData fileMetaData = parquetMetadata.getFileMetaData();
      MessageType fileSchema = fileMetaData.getSchema();
      if (nameMapping.isPresent() && !ParquetSchemaUtil.hasIds(fileSchema)) {
        // NameMapping conversion is necessary because MetadataReader converts all column names to lowercase
        // and NameMapping is case sensitive
        fileSchema = ParquetSchemaUtil.applyNameMapping(fileSchema, convertToLowercase(nameMapping.get()));
      }

      // Mapping from Iceberg field ID to Parquet fields.
      Map<Integer, org.apache.parquet.schema.Type> parquetIdToField = fileSchema.getFields().stream()
          .filter(field -> field.getId() != null)
          .collect(toImmutableMap(field -> field.getId().intValue(), Function.identity()));

      Optional<ReaderColumns> columnProjections = projectColumns(regularColumns);
      List<IcebergColumnHandle> readColumns = columnProjections
          .map(readerColumns -> (List<IcebergColumnHandle>) readerColumns.get()
              .stream().map(IcebergColumnHandle.class::cast).collect(toImmutableList()))
          .orElse(regularColumns);

      List<org.apache.parquet.schema.Type> parquetFields = readColumns.stream()
          .map(column -> parquetIdToField.get(column.getId()))
          .collect(toList());

      MessageType requestedSchema = new MessageType(
          fileSchema.getName(), parquetFields.stream().filter(Objects::nonNull).collect(toImmutableList()));
      Map<List<String>, ColumnDescriptor> descriptorsByPath = getDescriptors(fileSchema, requestedSchema);
      TupleDomain<ColumnDescriptor> parquetTupleDomain = getParquetTupleDomain(descriptorsByPath, effectivePredicate);
      TupleDomainParquetPredicate parquetPredicate =
          buildPredicate(requestedSchema, parquetTupleDomain, descriptorsByPath, dateTimeZone);

      long nextStart = 0;
      Optional<Long> startRowPosition = Optional.empty();
      Optional<Long> endRowPosition = Optional.empty();
      ImmutableList.Builder<Long> blockStarts = ImmutableList.builder();
      List<BlockMetaData> blocks = new ArrayList<>();
      for (BlockMetaData block : parquetMetadata.getBlocks()) {
        long firstDataPage = block.getColumns().get(0).getFirstDataPageOffset();
        if (start <= firstDataPage && firstDataPage < start + length &&
            predicateMatches(
                parquetPredicate,
                block,
                dataSource,
                descriptorsByPath,
                parquetTupleDomain,
                Optional.empty(),
                Optional.empty(),
                dateTimeZone,
                ICEBERG_DOMAIN_COMPACTION_THRESHOLD)) {
          blocks.add(block);
          blockStarts.add(nextStart);
          if (startRowPosition.isEmpty()) {
            startRowPosition = Optional.of(nextStart);
          }
          endRowPosition = Optional.of(nextStart + block.getRowCount());
        }
        nextStart += block.getRowCount();
      }

      MessageColumnIO messageColumnIO = getColumnIO(fileSchema, requestedSchema);

      ConstantPopulatingPageSource.Builder constantPopulatingPageSourceBuilder = ConstantPopulatingPageSource.builder();
      int parquetSourceChannel = 0;

      ImmutableList.Builder<ParquetReaderColumn> parquetReaderColumnBuilder = ImmutableList.builder();
      for (int columnIndex = 0; columnIndex < readColumns.size(); columnIndex++) {
        IcebergColumnHandle column = readColumns.get(columnIndex);
        if (column.isIsDeletedColumn()) {
          constantPopulatingPageSourceBuilder.addConstantColumn(nativeValueToBlock(BOOLEAN, false));
        } else if (partitionKeys.containsKey(column.getId())) {
          Type trinoType = column.getType();
          constantPopulatingPageSourceBuilder.addConstantColumn(nativeValueToBlock(
              trinoType,
              deserializePartitionValue(trinoType, partitionKeys.get(column.getId()).orElse(null), column.getName())));
        } else if (column.isPathColumn()) {
          constantPopulatingPageSourceBuilder.addConstantColumn(
              nativeValueToBlock(FILE_PATH.getType(), utf8Slice(inputFile.location())));
        } else if (column.isFileModifiedTimeColumn()) {
          constantPopulatingPageSourceBuilder.addConstantColumn(
              nativeValueToBlock(
                  FILE_MODIFIED_TIME.getType(), packDateTimeWithZone(inputFile.modificationTime(), UTC_KEY)
              )
          );
        } else if (column.isUpdateRowIdColumn() || column.isMergeRowIdColumn()) {
          // $row_id is a composite of multiple physical columns, it is assembled by the IcebergPageSource
          parquetReaderColumnBuilder.add(new ParquetReaderColumn(column.getType(), Optional.empty(), false));
          constantPopulatingPageSourceBuilder.addDelegateColumn(parquetSourceChannel);
          parquetSourceChannel++;
        } else if (column.isRowPositionColumn()) {
          parquetReaderColumnBuilder.add(new ParquetReaderColumn(BIGINT, Optional.empty(), true));
          constantPopulatingPageSourceBuilder.addDelegateColumn(parquetSourceChannel);
          parquetSourceChannel++;
        } else if (column.getId() == TRINO_MERGE_FILE_RECORD_COUNT) {
          constantPopulatingPageSourceBuilder.addConstantColumn(nativeValueToBlock(column.getType(), fileRecordCount));
        } else if (column.getId() == TRINO_MERGE_PARTITION_SPEC_ID) {
          constantPopulatingPageSourceBuilder.addConstantColumn(
              nativeValueToBlock(column.getType(), (long) partitionSpecId));
        } else if (column.getId() == TRINO_MERGE_PARTITION_DATA) {
          constantPopulatingPageSourceBuilder.addConstantColumn(
              nativeValueToBlock(column.getType(), utf8Slice(partitionData)));
        } else {
          org.apache.parquet.schema.Type parquetField = parquetFields.get(columnIndex);
          Type trinoType = column.getBaseType();

          if (parquetField == null) {
            parquetReaderColumnBuilder.add(new ParquetReaderColumn(trinoType, Optional.empty(), false));
          } else {
            // The top level columns are already mapped by name/id appropriately.
            ColumnIO columnIO = messageColumnIO.getChild(parquetField.getName());
            parquetReaderColumnBuilder.add(new ParquetReaderColumn(
                trinoType,
                IcebergParquetColumnIOConverter.constructField(
                    new FieldContext(trinoType, column.getColumnIdentity()), columnIO),
                false));
          }

          constantPopulatingPageSourceBuilder.addDelegateColumn(parquetSourceChannel);
          parquetSourceChannel++;
        }
      }

      List<ParquetReaderColumn> parquetReaderColumns = parquetReaderColumnBuilder.build();
      ParquetDataSourceId dataSourceId = dataSource.getId();
      ParquetReader parquetReader = new ParquetReader(
          Optional.ofNullable(fileMetaData.getCreatedBy()),
          getParquetReaderFields(parquetReaderColumns),
          blocks,
          blockStarts.build(),
          dataSource,
          dateTimeZone,
          memoryContext,
          options,
          exception -> handleException(dataSourceId, exception));
      return new ReaderPageSourceWithRowPositions(
          new ReaderPageSource(
              constantPopulatingPageSourceBuilder.build(new ParquetPageSource(parquetReader, parquetReaderColumns)),
              columnProjections),
          startRowPosition,
          endRowPosition);
    } catch (IOException | RuntimeException e) {
      try {
        if (dataSource != null) {
          dataSource.close();
        }
      } catch (IOException ex) {
        if (!e.equals(ex)) {
          e.addSuppressed(ex);
        }
      }
      if (e instanceof TrinoException) {
        throw (TrinoException) e;
      }
      String message = format(
          "Error opening Iceberg split %s (offset=%s, length=%s): %s",
          inputFile.location(), start, length, e.getMessage());

      if (e instanceof ParquetCorruptionException) {
        throw new TrinoException(ICEBERG_BAD_DATA, message, e);
      }

      if (e instanceof BlockMissingException) {
        throw new TrinoException(ICEBERG_MISSING_DATA, message, e);
      }
      throw new TrinoException(ICEBERG_CANNOT_OPEN_SPLIT, message, e);
    }
  }

  private static ReaderPageSourceWithRowPositions createAvroPageSource(
      TrinoFileSystem fileSystem,
      TrinoInputFile inputFile,
      long start,
      long length,
      long fileRecordCount,
      int partitionSpecId,
      String partitionData,
      Schema fileSchema,
      Optional<NameMapping> nameMapping,
      List<IcebergColumnHandle> columns) {
    ConstantPopulatingPageSource.Builder constantPopulatingPageSourceBuilder = ConstantPopulatingPageSource.builder();
    int avroSourceChannel = 0;

    Optional<ReaderColumns> columnProjections = projectColumns(columns);

    List<IcebergColumnHandle> readColumns = columnProjections
        .map(readerColumns -> (List<IcebergColumnHandle>) readerColumns.get()
            .stream().map(IcebergColumnHandle.class::cast).collect(toImmutableList()))
        .orElse(columns);

    InputFile file;
    OptionalLong fileModifiedTime = OptionalLong.empty();
    try {
      file = fileSystem.toFileIo().newInputFile(inputFile.location(), inputFile.length());
      if (readColumns.stream().anyMatch(IcebergColumnHandle::isFileModifiedTimeColumn)) {
        fileModifiedTime = OptionalLong.of(inputFile.modificationTime());
      }
    } catch (IOException e) {
      throw new TrinoException(ICEBERG_CANNOT_OPEN_SPLIT, e);
    }

    // The column orders in the generated schema might be different from the original order
    try (DataFileStream<?> avroFileReader = new DataFileStream<>(file.newStream(), new GenericDatumReader<>())) {
      org.apache.avro.Schema avroSchema = avroFileReader.getSchema();
      List<org.apache.avro.Schema.Field> fileFields = avroSchema.getFields();
      if (nameMapping.isPresent() && fileFields.stream().noneMatch(IcebergPageSourceProvider::hasId)) {
        fileFields = fileFields.stream()
            .map(field -> setMissingFieldId(field, nameMapping.get(), ImmutableList.of(field.name())))
            .collect(toImmutableList());
      }

      Map<Integer, org.apache.avro.Schema.Field> fileColumnsByIcebergId = mapIdsToAvroFields(fileFields);

      ImmutableList.Builder<String> columnNames = ImmutableList.builder();
      ImmutableList.Builder<Type> columnTypes = ImmutableList.builder();
      ImmutableList.Builder<Boolean> rowIndexChannels = ImmutableList.builder();

      for (IcebergColumnHandle column : readColumns) {
        verify(column.isBaseColumn(), "Column projections must be based from a root column");
        org.apache.avro.Schema.Field field = fileColumnsByIcebergId.get(column.getId());

        if (column.isPathColumn()) {
          constantPopulatingPageSourceBuilder.addConstantColumn(
              nativeValueToBlock(FILE_PATH.getType(), utf8Slice(file.location())));
        } else if (column.isFileModifiedTimeColumn()) {
          constantPopulatingPageSourceBuilder.addConstantColumn(nativeValueToBlock(
              FILE_MODIFIED_TIME.getType(),
              packDateTimeWithZone(fileModifiedTime.orElseThrow(), UTC_KEY)
          ));
        } else if (column.isRowPositionColumn()) {
          rowIndexChannels.add(true);
          columnNames.add(ROW_POSITION.name());
          columnTypes.add(BIGINT);
          constantPopulatingPageSourceBuilder.addDelegateColumn(avroSourceChannel);
          avroSourceChannel++;
        } else if (column.getId() == TRINO_MERGE_FILE_RECORD_COUNT) {
          constantPopulatingPageSourceBuilder.addConstantColumn(nativeValueToBlock(column.getType(), fileRecordCount));
        } else if (column.getId() == TRINO_MERGE_PARTITION_SPEC_ID) {
          constantPopulatingPageSourceBuilder.addConstantColumn(
              nativeValueToBlock(column.getType(), (long) partitionSpecId));
        } else if (column.getId() == TRINO_MERGE_PARTITION_DATA) {
          constantPopulatingPageSourceBuilder.addConstantColumn(
              nativeValueToBlock(column.getType(), utf8Slice(partitionData)));
        } else if (field == null) {
          constantPopulatingPageSourceBuilder.addConstantColumn(nativeValueToBlock(column.getType(), null));
        } else {
          rowIndexChannels.add(false);
          columnNames.add(column.getName());
          columnTypes.add(column.getType());
          constantPopulatingPageSourceBuilder.addDelegateColumn(avroSourceChannel);
          avroSourceChannel++;
        }
      }

      return new ReaderPageSourceWithRowPositions(
          new ReaderPageSource(
              constantPopulatingPageSourceBuilder.build(new IcebergAvroPageSource(
                  file,
                  start,
                  length,
                  fileSchema,
                  nameMapping,
                  columnNames.build(),
                  columnTypes.build(),
                  rowIndexChannels.build(),
                  newSimpleAggregatedMemoryContext())),
              columnProjections),
          Optional.empty(),
          Optional.empty());
    } catch (IOException e) {
      throw new TrinoException(ICEBERG_CANNOT_OPEN_SPLIT, e);
    }
  }

  private static boolean hasId(org.apache.avro.Schema.Field field) {
    return AvroSchemaUtil.hasFieldId(field);
  }

  private static org.apache.avro.Schema.Field setMissingFieldId(
      org.apache.avro.Schema.Field field, NameMapping nameMapping, List<String> qualifiedPath) {
    MappedField mappedField = nameMapping.find(qualifiedPath);

    org.apache.avro.Schema schema = field.schema();
    if (mappedField != null && mappedField.id() != null) {
      field.addProp(AVRO_FIELD_ID, mappedField.id());
    }

    return new org.apache.avro.Schema.Field(field, schema);
  }

  private static Map<Integer, org.apache.avro.Schema.Field> mapIdsToAvroFields(
      List<org.apache.avro.Schema.Field> fields) {
    ImmutableMap.Builder<Integer, org.apache.avro.Schema.Field> fieldsById = ImmutableMap.builder();
    for (org.apache.avro.Schema.Field field : fields) {
      if (AvroSchemaUtil.hasFieldId(field)) {
        fieldsById.put(AvroSchemaUtil.getFieldId(field), field);
      }
    }
    return fieldsById.buildOrThrow();
  }

  /**
   * Create a new NameMapping with the same names but converted to lowercase.
   *
   * @param nameMapping The original NameMapping, potentially containing non-lowercase characters
   */
  private static NameMapping convertToLowercase(NameMapping nameMapping) {
    return NameMapping.of(convertToLowercase(nameMapping.asMappedFields().fields()));
  }

  private static MappedFields convertToLowercase(MappedFields mappedFields) {
    if (mappedFields == null) {
      return null;
    }
    return MappedFields.of(convertToLowercase(mappedFields.fields()));
  }

  private static List<MappedField> convertToLowercase(List<MappedField> fields) {
    return fields.stream()
        .map(mappedField -> {
          Set<String> lowercaseNames =
              mappedField.names().stream().map(name -> name.toLowerCase(ENGLISH)).collect(toImmutableSet());
          return MappedField.of(mappedField.id(), lowercaseNames, convertToLowercase(mappedField.nestedMapping()));
        })
        .collect(toImmutableList());
  }

  private static class IcebergOrcProjectedLayout
      implements ProjectedLayout {
    private final Map<Integer, ProjectedLayout> projectedLayoutForFieldId;

    private IcebergOrcProjectedLayout(Map<Integer, ProjectedLayout> projectedLayoutForFieldId) {
      this.projectedLayoutForFieldId = ImmutableMap.copyOf(
          requireNonNull(projectedLayoutForFieldId, "projectedLayoutForFieldId is null"));
    }

    public static ProjectedLayout createProjectedLayout(OrcColumn root, List<List<Integer>> fieldIdDereferences) {
      if (fieldIdDereferences.stream().anyMatch(List::isEmpty)) {
        return fullyProjectedLayout();
      }

      Map<Integer, List<List<Integer>>> dereferencesByField = fieldIdDereferences.stream()
          .collect(groupingBy(
              sequence -> sequence.get(0),
              mapping(sequence -> sequence.subList(1, sequence.size()), toUnmodifiableList())));

      ImmutableMap.Builder<Integer, ProjectedLayout> fieldLayouts = ImmutableMap.builder();
      for (OrcColumn nestedColumn : root.getNestedColumns()) {
        Integer fieldId = getIcebergFieldId(nestedColumn);
        if (dereferencesByField.containsKey(fieldId)) {
          fieldLayouts.put(fieldId, createProjectedLayout(nestedColumn, dereferencesByField.get(fieldId)));
        }
      }

      return new IcebergOrcProjectedLayout(fieldLayouts.buildOrThrow());
    }

    @Override
    public ProjectedLayout getFieldLayout(OrcColumn orcColumn) {
      int fieldId = getIcebergFieldId(orcColumn);
      return projectedLayoutForFieldId.getOrDefault(fieldId, fullyProjectedLayout());
    }
  }

  /**
   * Creates a mapping between the input {@code columns} and base columns if required.
   */
  public static Optional<ReaderColumns> projectColumns(List<IcebergColumnHandle> columns) {
    requireNonNull(columns, "columns is null");

    // No projection is required if all columns are base columns
    if (columns.stream().allMatch(IcebergColumnHandle::isBaseColumn)) {
      return Optional.empty();
    }

    ImmutableList.Builder<ColumnHandle> projectedColumns = ImmutableList.builder();
    ImmutableList.Builder<Integer> outputColumnMapping = ImmutableList.builder();
    Map<Integer, Integer> mappedFieldIds = new HashMap<>();
    int projectedColumnCount = 0;

    for (IcebergColumnHandle column : columns) {
      int baseColumnId = column.getBaseColumnIdentity().getId();
      Integer mapped = mappedFieldIds.get(baseColumnId);

      if (mapped == null) {
        projectedColumns.add(column.getBaseColumn());
        mappedFieldIds.put(baseColumnId, projectedColumnCount);
        outputColumnMapping.add(projectedColumnCount);
        projectedColumnCount++;
      } else {
        outputColumnMapping.add(mapped);
      }
    }

    return Optional.of(new ReaderColumns(projectedColumns.build(), outputColumnMapping.build()));
  }

  private static TupleDomain<ColumnDescriptor> getParquetTupleDomain(
      Map<List<String>, ColumnDescriptor> descriptorsByPath,
      TupleDomain<IcebergColumnHandle> effectivePredicate) {
    if (effectivePredicate.isNone()) {
      return TupleDomain.none();
    }

    ImmutableMap.Builder<ColumnDescriptor, Domain> predicate = ImmutableMap.builder();
    effectivePredicate.getDomains().orElseThrow().forEach((columnHandle, domain) -> {
      String baseType = columnHandle.getType().getTypeSignature().getBase();
      // skip looking up predicates for complex types as Parquet only stores stats for primitives
      if (columnHandle.isBaseColumn() && (!baseType.equals(StandardTypes.MAP) &&
          !baseType.equals(StandardTypes.ARRAY) && !baseType.equals(StandardTypes.ROW))) {
        ColumnDescriptor descriptor = descriptorsByPath.get(ImmutableList.of(columnHandle.getName()));
        if (descriptor != null) {
          predicate.put(descriptor, domain);
        }
      }
    });
    return TupleDomain.withColumnDomains(predicate.buildOrThrow());
  }

  private static TrinoException handleException(OrcDataSourceId dataSourceId, Exception exception) {
    if (exception instanceof TrinoException) {
      return (TrinoException) exception;
    }
    if (exception instanceof OrcCorruptionException) {
      return new TrinoException(ICEBERG_BAD_DATA, exception);
    }
    return new TrinoException(ICEBERG_CURSOR_ERROR, format("Failed to read ORC file: %s", dataSourceId), exception);
  }

  private static TrinoException handleException(ParquetDataSourceId dataSourceId, Exception exception) {
    if (exception instanceof TrinoException) {
      return (TrinoException) exception;
    }
    if (exception instanceof ParquetCorruptionException) {
      return new TrinoException(ICEBERG_BAD_DATA, exception);
    }
    return new TrinoException(ICEBERG_CURSOR_ERROR, format("Failed to read Parquet file: %s", dataSourceId), exception);
  }

  public static final class ReaderPageSourceWithRowPositions {
    private final ReaderPageSource readerPageSource;
    private final Optional<Long> startRowPosition;
    private final Optional<Long> endRowPosition;

    public ReaderPageSourceWithRowPositions(
        ReaderPageSource readerPageSource,
        Optional<Long> startRowPosition,
        Optional<Long> endRowPosition) {
      this.readerPageSource = requireNonNull(readerPageSource, "readerPageSource is null");
      this.startRowPosition = requireNonNull(startRowPosition, "startRowPosition is null");
      this.endRowPosition = requireNonNull(endRowPosition, "endRowPosition is null");
    }

    public ReaderPageSource getReaderPageSource() {
      return readerPageSource;
    }

    public Optional<Long> getStartRowPosition() {
      return startRowPosition;
    }

    public Optional<Long> getEndRowPosition() {
      return endRowPosition;
    }
  }
}
