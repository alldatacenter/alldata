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

import org.apache.drill.common.expression.SchemaPath;
import org.apache.drill.common.types.TypeProtos.MinorType;
import org.apache.drill.common.types.Types;
import org.apache.drill.exec.exception.OutOfMemoryException;
import org.apache.drill.exec.metastore.ColumnNamesOptions;
import org.apache.drill.exec.metastore.analyze.MetastoreAnalyzeConstants;
import org.apache.drill.exec.ops.FragmentContext;
import org.apache.drill.exec.physical.config.MetadataHandlerPOP;
import org.apache.drill.exec.metastore.analyze.AnalyzeColumnUtils;
import org.apache.drill.exec.physical.resultSet.ResultSetLoader;
import org.apache.drill.exec.physical.resultSet.RowSetLoader;
import org.apache.drill.exec.physical.resultSet.impl.ResultSetOptionBuilder;
import org.apache.drill.exec.physical.resultSet.impl.ResultSetLoaderImpl;
import org.apache.drill.exec.physical.rowSet.DirectRowSet;
import org.apache.drill.exec.physical.rowSet.RowSetReader;
import org.apache.drill.exec.metastore.analyze.MetadataIdentifierUtils;
import org.apache.drill.exec.record.AbstractSingleRecordBatch;
import org.apache.drill.exec.record.BatchSchema;
import org.apache.drill.exec.record.MaterializedField;
import org.apache.drill.exec.record.RecordBatch;
import org.apache.drill.exec.record.VectorContainer;
import org.apache.drill.exec.record.VectorWrapper;
import org.apache.drill.exec.record.metadata.SchemaBuilder;
import org.apache.drill.exec.vector.VarCharVector;
import org.apache.drill.metastore.components.tables.BasicTablesRequests;
import org.apache.drill.metastore.components.tables.Tables;
import org.apache.drill.metastore.metadata.BaseMetadata;
import org.apache.drill.metastore.metadata.FileMetadata;
import org.apache.drill.metastore.metadata.LocationProvider;
import org.apache.drill.metastore.metadata.MetadataInfo;
import org.apache.drill.metastore.metadata.MetadataType;
import org.apache.drill.metastore.metadata.RowGroupMetadata;
import org.apache.drill.metastore.metadata.SegmentMetadata;
import org.apache.drill.metastore.statistics.ExactStatisticsConstants;
import org.apache.drill.metastore.statistics.StatisticsKind;
import org.apache.drill.shaded.guava.com.google.common.base.Preconditions;
import org.apache.hadoop.fs.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static org.apache.drill.exec.record.RecordBatch.IterOutcome.NONE;

/**
 * Responsible for handling metadata returned by incoming aggregate operators
 * and fetching required metadata form the Metastore.
 */
public class MetadataHandlerBatch extends AbstractSingleRecordBatch<MetadataHandlerPOP> {
  private static final Logger logger = LoggerFactory.getLogger(MetadataHandlerBatch.class);

  private final Tables tables;
  private final MetadataType metadataType;
  private final Map<String, MetadataInfo> metadataToHandle;
  private final ColumnNamesOptions columnNamesOptions;

  private boolean firstBatch = true;

  protected MetadataHandlerBatch(MetadataHandlerPOP popConfig,
      FragmentContext context, RecordBatch incoming) throws OutOfMemoryException {
    super(popConfig, context, incoming);
    this.tables = context.getMetastoreRegistry().get().tables();
    this.metadataType = popConfig.getContext().metadataType();
    this.columnNamesOptions = new ColumnNamesOptions(context.getOptions());
    this.metadataToHandle = popConfig.getContext().metadataToHandle() != null
        ? popConfig.getContext().metadataToHandle().stream()
            .collect(Collectors.toMap(MetadataInfo::identifier, Function.identity()))
        : null;
  }

  @Override
  public IterOutcome doWork() {
    // 1. Consume data from incoming operators and update metadataToHandle to remove incoming metadata
    // 2. For the case when incoming operator returned nothing - no updated underlying metadata was found.
    // 3. Fetches metadata which should be handled but wasn't returned by incoming batch from the Metastore

    IterOutcome outcome = incoming.getRecordCount() == 0 ? next(incoming) : getLastKnownOutcome();

    switch (outcome) {
      case NONE:
        if (firstBatch) {
          Preconditions.checkState(metadataToHandle.isEmpty(),
              "Incoming batch didn't return the result for modified segments");
        }
        return outcome;
      case OK_NEW_SCHEMA:
        if (firstBatch) {
          firstBatch = false;
          if (!setupNewSchema()) {
            outcome = IterOutcome.OK;
          }
        }
        // fall thru
      case OK:
        assert !firstBatch : "First batch should be OK_NEW_SCHEMA";
        doWorkInternal();
        // fall thru
      case NOT_YET:
        return outcome;
      default:
        throw new UnsupportedOperationException("Unsupported upstream state " + outcome);
    }
  }

  @Override
  public IterOutcome innerNext() {
    IterOutcome outcome = getLastKnownOutcome();
    if (outcome != NONE) {
      outcome = super.innerNext();
    }
    // if incoming is exhausted, reads metadata which should be obtained from the Metastore
    // and returns OK or NONE if there is no metadata to read
    if (outcome == IterOutcome.NONE && !metadataToHandle.isEmpty()) {
      BasicTablesRequests basicTablesRequests = tables.basicRequests();

      switch (metadataType) {
        case ROW_GROUP: {
          List<RowGroupMetadata> rowGroups =
              basicTablesRequests.rowGroupsMetadata(
                  popConfig.getContext().tableInfo(),
                  new ArrayList<>(metadataToHandle.values()));
          return populateContainer(rowGroups);
        }
        case FILE: {
          List<FileMetadata> files =
              basicTablesRequests.filesMetadata(
                  popConfig.getContext().tableInfo(),
                  new ArrayList<>(metadataToHandle.values()));
          return populateContainer(files);
        }
        case SEGMENT: {
          List<SegmentMetadata> segments =
              basicTablesRequests.segmentsMetadata(
                  popConfig.getContext().tableInfo(),
                  new ArrayList<>(metadataToHandle.values()));
          return populateContainer(segments);
        }
        default:
          break;
      }
    }
    return outcome;
  }

  private <T extends BaseMetadata & LocationProvider> IterOutcome populateContainer(List<T> metadata) {
    VectorContainer populatedContainer;
    if (firstBatch) {
      populatedContainer = writeMetadata(metadata);
      setupSchemaFromContainer(populatedContainer);
    } else {
      populatedContainer = writeMetadataUsingBatchSchema(metadata);
    }
    container.transferIn(populatedContainer);
    container.setRecordCount(populatedContainer.getRecordCount());

    if (firstBatch) {
      firstBatch = false;
      return IterOutcome.OK_NEW_SCHEMA;
    } else {
      return IterOutcome.OK;
    }
  }

  private <T extends BaseMetadata & LocationProvider> VectorContainer writeMetadata(List<T> metadataList) {
    BaseMetadata firstElement = metadataList.iterator().next();

    ResultSetLoader resultSetLoader = getResultSetLoaderForMetadata(firstElement);
    resultSetLoader.startBatch();
    RowSetLoader rowWriter = resultSetLoader.writer();
    Iterator<T> segmentsIterator = metadataList.iterator();
    while (!rowWriter.isFull() && segmentsIterator.hasNext()) {
      T metadata = segmentsIterator.next();
      metadataToHandle.remove(metadata.getMetadataInfo().identifier());

      List<Object> arguments = new ArrayList<>();
      // adds required segment names to the arguments
      arguments.add(metadata.getPath().toUri().getPath());
      Collections.addAll(
          arguments,
          Arrays.copyOf(
              MetadataIdentifierUtils.getValuesFromMetadataIdentifier(metadata.getMetadataInfo().identifier()),
              popConfig.getContext().segmentColumns().size()));

      // adds column statistics values assuming that they are sorted in alphabetic order
      // (see getResultSetLoaderForMetadata() method)
      metadata.getColumnsStatistics().entrySet().stream()
          .sorted(Comparator.comparing(e -> e.getKey().toExpr()))
          .map(Map.Entry::getValue)
          .flatMap(columnStatistics ->
              AnalyzeColumnUtils.COLUMN_STATISTICS_FUNCTIONS.keySet().stream()
                  .map(columnStatistics::get))
          .forEach(arguments::add);

      AnalyzeColumnUtils.META_STATISTICS_FUNCTIONS.keySet().stream()
          .map(metadata::getStatistic)
          .forEach(arguments::add);

      // collectedMap field value
      arguments.add(new Object[]{});

      if (metadataType == MetadataType.SEGMENT) {
        arguments.add(((SegmentMetadata) metadata).getLocations().stream()
            .map(path -> path.toUri().getPath())
            .toArray(String[]::new));
      }

      if (metadataType == MetadataType.ROW_GROUP) {
        arguments.add(String.valueOf(((RowGroupMetadata) metadata).getRowGroupIndex()));
        arguments.add(Long.toString(metadata.getStatistic(() -> ExactStatisticsConstants.START)));
        arguments.add(Long.toString(metadata.getStatistic(() -> ExactStatisticsConstants.LENGTH)));
      }

      arguments.add(metadata.getSchema().jsonString());
      arguments.add(String.valueOf(metadata.getLastModifiedTime()));
      arguments.add(metadataType.name());
      rowWriter.addRow(arguments.toArray());
    }

    return resultSetLoader.harvest();
  }

  private ResultSetLoader getResultSetLoaderForMetadata(BaseMetadata baseMetadata) {
    SchemaBuilder schemaBuilder = new SchemaBuilder()
        .addNullable(MetastoreAnalyzeConstants.LOCATION_FIELD, MinorType.VARCHAR);
    for (String segmentColumn : popConfig.getContext().segmentColumns()) {
      schemaBuilder.addNullable(segmentColumn, MinorType.VARCHAR);
    }

    baseMetadata.getColumnsStatistics().entrySet().stream()
        .sorted(Comparator.comparing(e -> e.getKey().getRootSegmentPath()))
        .forEach(entry -> {
          for (StatisticsKind<?> statisticsKind : AnalyzeColumnUtils.COLUMN_STATISTICS_FUNCTIONS.keySet()) {
            MinorType type = AnalyzeColumnUtils.COLUMN_STATISTICS_TYPES.get(statisticsKind);
            type = type != null ? type : entry.getValue().getComparatorType();
            schemaBuilder.addNullable(
                AnalyzeColumnUtils.getColumnStatisticsFieldName(entry.getKey().getRootSegmentPath(), statisticsKind),
                type);
          }
        });

    for (StatisticsKind<?> statisticsKind : AnalyzeColumnUtils.META_STATISTICS_FUNCTIONS.keySet()) {
      schemaBuilder.addNullable(
          AnalyzeColumnUtils.getMetadataStatisticsFieldName(statisticsKind),
          AnalyzeColumnUtils.COLUMN_STATISTICS_TYPES.get(statisticsKind));
    }

    schemaBuilder
        .addMapArray(MetastoreAnalyzeConstants.COLLECTED_MAP_FIELD)
        .resumeSchema();

    if (metadataType == MetadataType.SEGMENT) {
      schemaBuilder.addArray(MetastoreAnalyzeConstants.LOCATIONS_FIELD, MinorType.VARCHAR);
    }

    if (metadataType == MetadataType.ROW_GROUP) {
      schemaBuilder.addNullable(columnNamesOptions.rowGroupIndex(), MinorType.VARCHAR);
      schemaBuilder.addNullable(columnNamesOptions.rowGroupStart(), MinorType.VARCHAR);
      schemaBuilder.addNullable(columnNamesOptions.rowGroupLength(), MinorType.VARCHAR);
    }

    schemaBuilder
        .addNullable(MetastoreAnalyzeConstants.SCHEMA_FIELD, MinorType.VARCHAR)
        .addNullable(columnNamesOptions.lastModifiedTime(), MinorType.VARCHAR)
        .add(MetastoreAnalyzeConstants.METADATA_TYPE, MinorType.VARCHAR);

    ResultSetLoaderImpl.ResultSetOptions options = new ResultSetOptionBuilder()
        .readerSchema(schemaBuilder.buildSchema())
        .build();

    return new ResultSetLoaderImpl(container.getAllocator(), options);
  }

  private <T extends BaseMetadata & LocationProvider> VectorContainer writeMetadataUsingBatchSchema(List<T> metadataList) {
    Preconditions.checkArgument(!metadataList.isEmpty(), "Metadata list shouldn't be empty.");

    ResultSetLoader resultSetLoader = getResultSetLoaderWithBatchSchema();
    resultSetLoader.startBatch();
    RowSetLoader rowWriter = resultSetLoader.writer();
    Iterator<T> segmentsIterator = metadataList.iterator();
    while (!rowWriter.isFull() && segmentsIterator.hasNext()) {
      T metadata = segmentsIterator.next();
      metadataToHandle.remove(metadata.getMetadataInfo().identifier());

      List<Object> arguments = new ArrayList<>();
      for (VectorWrapper<?> vectorWrapper : container) {

        String[] identifierValues = Arrays.copyOf(
            MetadataIdentifierUtils.getValuesFromMetadataIdentifier(metadata.getMetadataInfo().identifier()),
            popConfig.getContext().segmentColumns().size());

        MaterializedField field = vectorWrapper.getField();
        String fieldName = field.getName();
        if (fieldName.equals(MetastoreAnalyzeConstants.LOCATION_FIELD)) {
          arguments.add(metadata.getPath().toUri().getPath());
        } else if (fieldName.equals(MetastoreAnalyzeConstants.LOCATIONS_FIELD)) {
          if (metadataType == MetadataType.SEGMENT) {
            arguments.add(((SegmentMetadata) metadata).getLocations().stream()
                .map(path -> path.toUri().getPath())
                .toArray(String[]::new));
          } else {
            arguments.add(null);
          }
        } else if (popConfig.getContext().segmentColumns().contains(fieldName)) {
          arguments.add(identifierValues[popConfig.getContext().segmentColumns().indexOf(fieldName)]);
        } else if (AnalyzeColumnUtils.isColumnStatisticsField(fieldName)) {
          arguments.add(
              metadata.getColumnStatistics(SchemaPath.parseFromString(AnalyzeColumnUtils.getColumnName(fieldName)))
                  .get(AnalyzeColumnUtils.getStatisticsKind(fieldName)));
        } else if (AnalyzeColumnUtils.isMetadataStatisticsField(fieldName)) {
          arguments.add(metadata.getStatistic(AnalyzeColumnUtils.getStatisticsKind(fieldName)));
        } else if (fieldName.equals(MetastoreAnalyzeConstants.COLLECTED_MAP_FIELD)) {
          // collectedMap field value
          arguments.add(new Object[]{});
        } else if (fieldName.equals(MetastoreAnalyzeConstants.SCHEMA_FIELD)) {
          arguments.add(metadata.getSchema().jsonString());
        } else if (fieldName.equals(columnNamesOptions.lastModifiedTime())) {
          arguments.add(String.valueOf(metadata.getLastModifiedTime()));
        } else if (fieldName.equals(columnNamesOptions.rowGroupIndex())) {
          arguments.add(String.valueOf(((RowGroupMetadata) metadata).getRowGroupIndex()));
        } else if (fieldName.equals(columnNamesOptions.rowGroupStart())) {
          arguments.add(Long.toString(metadata.getStatistic(() -> ExactStatisticsConstants.START)));
        } else if (fieldName.equals(columnNamesOptions.rowGroupLength())) {
          arguments.add(Long.toString(metadata.getStatistic(() -> ExactStatisticsConstants.LENGTH)));
        } else if (fieldName.equals(MetastoreAnalyzeConstants.METADATA_TYPE)) {
          arguments.add(metadataType.name());
        } else {
          throw new UnsupportedOperationException(String.format("Found unexpected field [%s] in incoming batch.",  field));
        }
      }

      rowWriter.addRow(arguments.toArray());
    }

    return resultSetLoader.harvest();
  }

  private ResultSetLoader getResultSetLoaderWithBatchSchema() {
    SchemaBuilder schemaBuilder = new SchemaBuilder();
    // adds fields to the schema preserving their order to avoid issues in outcoming batches
    for (VectorWrapper<?> vectorWrapper : container) {
      MaterializedField field = vectorWrapper.getField();
      String fieldName = field.getName();
      if (fieldName.equals(MetastoreAnalyzeConstants.LOCATION_FIELD)
          || fieldName.equals(MetastoreAnalyzeConstants.SCHEMA_FIELD)
          || fieldName.equals(columnNamesOptions.lastModifiedTime())
          || fieldName.equals(columnNamesOptions.rowGroupIndex())
          || fieldName.equals(columnNamesOptions.rowGroupStart())
          || fieldName.equals(columnNamesOptions.rowGroupLength())
          || fieldName.equals(MetastoreAnalyzeConstants.METADATA_TYPE)
          || popConfig.getContext().segmentColumns().contains(fieldName)) {
        schemaBuilder.add(fieldName, field.getType().getMinorType(), field.getDataMode());
      } else if (AnalyzeColumnUtils.isColumnStatisticsField(fieldName)
          || AnalyzeColumnUtils.isMetadataStatisticsField(fieldName)) {
        schemaBuilder.add(fieldName, field.getType().getMinorType(), field.getType().getMode());
      } else if (fieldName.equals(MetastoreAnalyzeConstants.COLLECTED_MAP_FIELD)) {
        schemaBuilder.addMapArray(fieldName)
            .resumeSchema();
      } else if (fieldName.equals(MetastoreAnalyzeConstants.LOCATIONS_FIELD)) {
        schemaBuilder.addArray(fieldName, MinorType.VARCHAR);
      } else {
        throw new UnsupportedOperationException(String.format("Found unexpected field [%s] in incoming batch.",  field));
      }
    }

    ResultSetLoaderImpl.ResultSetOptions options = new ResultSetOptionBuilder()
        .readerSchema(schemaBuilder.buildSchema())
        .build();

    return new ResultSetLoaderImpl(container.getAllocator(), options);
  }

  private void setupSchemaFromContainer(VectorContainer populatedContainer) {
    container.clear();
    StreamSupport.stream(populatedContainer.spliterator(), false)
        .map(VectorWrapper::getField)
        .forEach(container::addOrGet);
    container.buildSchema(BatchSchema.SelectionVectorMode.NONE);
    container.setEmpty();
  }

  @Override
  protected boolean setupNewSchema() {
    setupSchemaFromContainer(incoming.getContainer());
    return true;
  }

  private IterOutcome doWorkInternal() {
    container.transferIn(incoming.getContainer());
    VarCharVector metadataTypeVector = container.addOrGet(
        MaterializedField.create(MetastoreAnalyzeConstants.METADATA_TYPE, Types.required(MinorType.VARCHAR)));
    metadataTypeVector.allocateNew();
    for (int i = 0; i < incoming.getRecordCount(); i++) {
      metadataTypeVector.getMutator().setSafe(i, metadataType.name().getBytes());
    }

    metadataTypeVector.getMutator().setValueCount(incoming.getRecordCount());
    container.setRecordCount(incoming.getRecordCount());

    container.buildSchema(BatchSchema.SelectionVectorMode.NONE);
    updateMetadataToHandle();

    return IterOutcome.OK;
  }

  private void updateMetadataToHandle() {
    // updates metadataToHandle to be able to fetch required data which wasn't returned by incoming batch
    if (metadataToHandle != null && !metadataToHandle.isEmpty()) {
      RowSetReader reader = DirectRowSet.fromContainer(container).reader();
      switch (metadataType) {
        case ROW_GROUP: {
          while (reader.next() && !metadataToHandle.isEmpty()) {
            List<String> partitionValues = popConfig.getContext().segmentColumns().stream()
                .map(columnName -> reader.column(columnName).scalar().getString())
                .collect(Collectors.toList());
            Path location = new Path(reader.column(MetastoreAnalyzeConstants.LOCATION_FIELD).scalar().getString());
            int rgi = Integer.parseInt(reader.column(columnNamesOptions.rowGroupIndex()).scalar().getString());
            metadataToHandle.remove(MetadataIdentifierUtils.getRowGroupMetadataIdentifier(partitionValues, location, rgi));
          }
          break;
        }
        case FILE: {
          while (reader.next() && !metadataToHandle.isEmpty()) {
            List<String> partitionValues = popConfig.getContext().segmentColumns().stream()
                .map(columnName -> reader.column(columnName).scalar().getString())
                .collect(Collectors.toList());
            Path location = new Path(reader.column(MetastoreAnalyzeConstants.LOCATION_FIELD).scalar().getString());
            // use metadata identifier for files since row group indexes are not required when file is updated
            metadataToHandle.remove(MetadataIdentifierUtils.getFileMetadataIdentifier(partitionValues, location));
          }
          break;
        }
        case SEGMENT: {
          while (reader.next() && !metadataToHandle.isEmpty()) {
            List<String> partitionValues = popConfig.getContext().segmentColumns().stream()
                .limit(popConfig.getContext().depthLevel())
                .map(columnName -> reader.column(columnName).scalar().getString())
                .collect(Collectors.toList());
            metadataToHandle.remove(MetadataIdentifierUtils.getMetadataIdentifierKey(partitionValues));
          }
          break;
        }
        default:
          break;
      }
    }
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
