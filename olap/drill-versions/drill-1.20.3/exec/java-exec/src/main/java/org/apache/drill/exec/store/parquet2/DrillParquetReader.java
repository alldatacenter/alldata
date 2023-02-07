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
package org.apache.drill.exec.store.parquet2;

import org.apache.commons.collections.CollectionUtils;
import org.apache.drill.common.exceptions.ExecutionSetupException;
import org.apache.drill.common.expression.PathSegment;
import org.apache.drill.common.expression.SchemaPath;
import org.apache.drill.exec.ExecConstants;
import org.apache.drill.exec.exception.OutOfMemoryException;
import org.apache.drill.exec.memory.BufferAllocator;
import org.apache.drill.exec.ops.FragmentContext;
import org.apache.drill.exec.ops.OperatorContext;
import org.apache.drill.exec.physical.impl.OutputMutator;
import org.apache.drill.exec.record.MaterializedField;
import org.apache.drill.exec.store.CommonParquetRecordReader;
import org.apache.drill.exec.store.dfs.DrillFileSystem;
import org.apache.drill.exec.store.parquet.ParquetDirectByteBufferAllocator;
import org.apache.drill.exec.store.parquet.ParquetReaderUtility;
import org.apache.drill.exec.store.parquet.RowGroupReadEntry;
import org.apache.drill.exec.store.parquet.compression.DrillCompressionCodecFactory;
import org.apache.drill.exec.vector.AllocationHelper;
import org.apache.drill.exec.vector.NullableIntVector;
import org.apache.drill.exec.vector.ValueVector;
import org.apache.drill.shaded.guava.com.google.common.base.Preconditions;
import org.apache.parquet.column.ColumnDescriptor;
import org.apache.parquet.compression.CompressionCodecFactory;
import org.apache.parquet.hadoop.ColumnChunkIncReadStore;
import org.apache.parquet.hadoop.metadata.BlockMetaData;
import org.apache.parquet.hadoop.metadata.ColumnChunkMetaData;
import org.apache.parquet.hadoop.metadata.ColumnPath;
import org.apache.parquet.hadoop.metadata.ParquetMetadata;
import org.apache.parquet.io.ColumnIOFactory;
import org.apache.parquet.io.MessageColumnIO;
import org.apache.parquet.io.RecordReader;
import org.apache.parquet.schema.GroupType;
import org.apache.parquet.schema.MessageType;
import org.apache.parquet.schema.Type;
import org.apache.parquet.schema.Types;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.apache.drill.common.types.Types.OPTIONAL_INT;

public class DrillParquetReader extends CommonParquetRecordReader {
  private static final Logger logger = LoggerFactory.getLogger(DrillParquetReader.class);

  private MessageType schema;
  private DrillFileSystem drillFileSystem;
  private RowGroupReadEntry entry;
  private ColumnChunkIncReadStore pageReadStore;
  private RecordReader<Void> recordReader;
  private DrillParquetRecordMaterializer recordMaterializer;
  /** Configured Parquet records per batch */
  private final int recordsPerBatch;

  // For columns not found in the file, we need to return a schema element with the correct number of values
  // at that position in the schema. Currently this requires a vector be present. Here is a list of all of these vectors
  // that need only have their value count set at the end of each call to next(), as the values default to null.
  private List<NullableIntVector> nullFilledVectors;
  // Keeps track of the number of records returned in the case where only columns outside of the file were selected.
  // No actual data needs to be read out of the file, we only need to return batches until we have 'read' the number of
  // records specified in the row group metadata
  private long totalRead = 0;
  private boolean noColumnsFound; // true if none of the columns in the projection list is found in the schema

  // See DRILL-4203
  private final ParquetReaderUtility.DateCorruptionStatus containsCorruptedDates;
  private final long numRecordsToRead;

  public DrillParquetReader(FragmentContext fragmentContext,
                            ParquetMetadata footer,
                            RowGroupReadEntry entry,
                            List<SchemaPath> columns,
                            DrillFileSystem fileSystem,
                            ParquetReaderUtility.DateCorruptionStatus containsCorruptedDates,
                            long recordsToRead) {
    super(footer, fragmentContext);
    this.containsCorruptedDates = containsCorruptedDates;
    this.drillFileSystem = fileSystem;
    this.entry = entry;
    setColumns(columns);
    this.recordsPerBatch = (int) fragmentContext.getOptions().getLong(ExecConstants.PARQUET_COMPLEX_BATCH_NUM_RECORDS);
    this.numRecordsToRead = initNumRecordsToRead(recordsToRead, entry.getRowGroupIndex(), footer);
  }

  /**
   * Creates projection MessageType from projection columns and given schema.
   *
   * @param schema Parquet file schema
   * @param projectionColumns columns to search
   * @param columnsNotFound any projection column which wasn't found in schema is added to the list
   * @return projection containing matched columns or null if none column matches schema
   */
  private static MessageType getProjection(MessageType schema,
                                           Collection<SchemaPath> projectionColumns,
                                           List<SchemaPath> columnsNotFound) {
    projectionColumns = adaptColumnsToParquetSchema(projectionColumns, schema);
    List<SchemaPath> schemaColumns = getAllColumnsFrom(schema);
    Set<SchemaPath> selectedSchemaPaths = matchProjectionWithSchemaColumns(projectionColumns, schemaColumns, columnsNotFound);
    return convertSelectedColumnsToMessageType(schema, selectedSchemaPaths);
  }

  /**
   * This method adjusts collection of SchemaPath projection columns to better match columns in given
   * schema. It does few things to reach the goal:
   * <ul>
   *   <li>skips ArraySegments if present;</li>
   *   <li>interrupts further projections for Parquet MAPs to allow EvaluationVisitor manage get by key logic;</li>
   *   <li>adds additional listName and elementName for logical lists, because they exists in schema but absent in original projection columns.</li>
   * </ul>
   *
   * @param columns original projection columns
   * @param schema Parquet file schema
   * @return adjusted projection columns
   */
  private static List<SchemaPath> adaptColumnsToParquetSchema(Collection<SchemaPath> columns, MessageType schema) {
    List<SchemaPath> modifiedColumns = new LinkedList<>();
    for (SchemaPath path : columns) {

      List<String> segments = new ArrayList<>();
      Type segmentType = schema;
      for (PathSegment seg = path.getRootSegment(); seg != null; seg = seg.getChild()) {

        if (seg.isNamed()) {
          segments.add(seg.getNameSegment().getPath());
        }

        segmentType = getSegmentType(segmentType, seg);

        if (segmentType != null && !segmentType.isPrimitive()) {
          GroupType segGroupType = segmentType.asGroupType();
          if (ParquetReaderUtility.isLogicalMapType(segGroupType)) {
            // stop the loop at a found MAP column to ensure the selection is not discarded
            // later as values obtained from dict by key differ from the actual column's path
            break;
          } else if (ParquetReaderUtility.isLogicalListType(segGroupType)) {
            // 'list' or 'bag'
            String listName = segGroupType.getType(0).getName();
            // 'element' or 'array_element'
            String elementName = segGroupType.getType(0).asGroupType().getType(0).getName();
            segments.add(listName);
            segments.add(elementName);
          }
        }
      }

      modifiedColumns.add(SchemaPath.getCompoundPath(segments.toArray(new String[0])));
    }
    return modifiedColumns;
  }

  /**
   * Convert SchemaPaths from selectedSchemaPaths and convert to parquet type, and merge into projection schema.
   *
   * @param schema Parquet file schema
   * @param selectedSchemaPaths columns found in schema
   * @return projection schema
   */
  private static MessageType convertSelectedColumnsToMessageType(MessageType schema, Set<SchemaPath> selectedSchemaPaths) {
    MessageType projection = null;
    String messageName = schema.getName();
    for (SchemaPath schemaPath : selectedSchemaPaths) {
      List<String> segments = new ArrayList<>();
      PathSegment seg = schemaPath.getRootSegment();
      do {
        segments.add(seg.getNameSegment().getPath());
      } while ((seg = seg.getChild()) != null);
      String[] pathSegments = new String[segments.size()];
      segments.toArray(pathSegments);
      Type t = getSegmentType(pathSegments, 0, schema);

      if (projection == null) {
        projection = new MessageType(messageName, t);
      } else {
        projection = projection.union(new MessageType(messageName, t));
      }
    }
    return projection;
  }


  private static Set<SchemaPath> matchProjectionWithSchemaColumns(Collection<SchemaPath> projectionColumns,
                                                                  List<SchemaPath> schemaColumns,
                                                                  List<SchemaPath> columnsNotFound) {
    // parquet type.union() seems to lose ConvertedType info when merging two columns that are the same type. This can
    // happen when selecting two elements from an array. So to work around this, we use set of SchemaPath to avoid duplicates
    // and then merge the types at the end
    Set<SchemaPath> selectedSchemaPaths = new LinkedHashSet<>();
    // loop through projection columns and add any columns that are missing from parquet schema to columnsNotFound list
    for (SchemaPath projectionColumn : projectionColumns) {
      boolean notFound = true;
      for (SchemaPath schemaColumn : schemaColumns) {
        if (schemaColumn.contains(projectionColumn)) {
          selectedSchemaPaths.add(schemaColumn);
          notFound = false;
        }
      }
      if (notFound) {
        columnsNotFound.add(projectionColumn);
      }
    }
    return selectedSchemaPaths;
  }

  /**
   * Convert the columns in the parquet schema to a list of SchemaPath columns so that they can be compared in case
   * insensitive manner to the projection columns.
   *
   * @param schema Parquet file schema
   * @return paths to all fields in schema
   */
  private static List<SchemaPath> getAllColumnsFrom(MessageType schema) {
    List<SchemaPath> schemaPaths = new LinkedList<>();
    for (ColumnDescriptor columnDescriptor : schema.getColumns()) {
      String[] schemaColDesc = Arrays.copyOf(columnDescriptor.getPath(), columnDescriptor.getPath().length);
      SchemaPath schemaPath = SchemaPath.getCompoundPath(schemaColDesc);
      schemaPaths.add(schemaPath);
    }
    return schemaPaths;
  }

  /**
   * Get type from the supplied {@code type} corresponding to given {@code segment}.
   *
   * @param parentSegmentType type to extract field corresponding to segment
   * @param segment segment which type will be returned
   * @return type corresponding to the {@code segment} or {@code null} if there is no field found in {@code type}.
   */
  private static Type getSegmentType(Type parentSegmentType, PathSegment segment) {
    Type segmentType = null;
    if (parentSegmentType != null && !parentSegmentType.isPrimitive()) {
      GroupType groupType = parentSegmentType.asGroupType();
      if (segment.isNamed()) {
        String fieldName = segment.getNameSegment().getPath();
        segmentType = groupType.getFields().stream()
            .filter(f -> f.getName().equalsIgnoreCase(fieldName))
            .findAny().map(field -> groupType.getType(field.getName()))
            .orElse(null);
      } else if (ParquetReaderUtility.isLogicalListType(parentSegmentType.asGroupType())) { // the segment is array index
        // get element type of the list
        segmentType = groupType.getType(0).asGroupType().getType(0);
      }
    }
    return segmentType;
  }

  @Override
  public void allocate(Map<String, ValueVector> vectorMap) throws OutOfMemoryException {
    try {
      for (final ValueVector v : vectorMap.values()) {
        AllocationHelper.allocate(v, Character.MAX_VALUE, 50, 10);
      }
    } catch (NullPointerException e) {
      throw new OutOfMemoryException();
    }
  }

  @Override
  public void setup(OperatorContext context, OutputMutator output) throws ExecutionSetupException {
    try {
      this.operatorContext = context;
      schema = footer.getFileMetaData().getSchema();
      MessageType projection;
      final List<SchemaPath> columnsNotFound = new ArrayList<>(getColumns().size());

      if (isStarQuery()) {
        projection = schema;
      } else {
        projection = getProjection(schema, getColumns(), columnsNotFound);
        if (projection == null) {
          projection = schema;
        }
        if (!columnsNotFound.isEmpty()) {
          nullFilledVectors = new ArrayList<>(columnsNotFound.size());
          for (SchemaPath col : columnsNotFound) {
            // col.toExpr() is used here as field name since we don't want to see these fields in the existing maps
            nullFilledVectors.add(output.addField(MaterializedField.create(col.toExpr(), OPTIONAL_INT), NullableIntVector.class));
          }
          noColumnsFound = columnsNotFound.size() == getColumns().size();
        }
      }

      logger.debug("Requesting schema {}", projection);

      if (!noColumnsFound) {
        // Discard the columns not found in the schema when create DrillParquetRecordMaterializer, since they have been added to output already.
        @SuppressWarnings("unchecked")
        Collection<SchemaPath> columns = columnsNotFound.isEmpty() ? getColumns() : CollectionUtils.subtract(getColumns(), columnsNotFound);
        recordMaterializer = new DrillParquetRecordMaterializer(output, projection, columns, fragmentContext.getOptions(), containsCorruptedDates);
      }

      if (numRecordsToRead == 0 || noColumnsFound) {
        // no need to init readers
        return;
      }

      ColumnIOFactory factory = new ColumnIOFactory(false);
      MessageColumnIO columnIO = factory.getColumnIO(projection, schema);
      BlockMetaData blockMetaData = footer.getBlocks().get(entry.getRowGroupIndex());

      Map<ColumnPath, ColumnChunkMetaData> paths = blockMetaData.getColumns().stream()
        .collect(Collectors.toMap(
          ColumnChunkMetaData::getPath,
          Function.identity(),
          (o, n) -> n));

      BufferAllocator allocator = operatorContext.getAllocator();

      CompressionCodecFactory ccf = DrillCompressionCodecFactory.createDirectCodecFactory(
        drillFileSystem.getConf(),
        new ParquetDirectByteBufferAllocator(allocator),
        0
      );

      pageReadStore = new ColumnChunkIncReadStore(
        numRecordsToRead,
        ccf,
        allocator,
        drillFileSystem,
        entry.getPath()
      );

      for (String[] path : schema.getPaths()) {
        Type type = schema.getType(path);
        if (type.isPrimitive()) {
          ColumnChunkMetaData md = paths.get(ColumnPath.get(path));
          pageReadStore.addColumn(schema.getColumnDescription(path), md);
        }
      }
      recordReader = columnIO.getRecordReader(pageReadStore, recordMaterializer);
    } catch (Exception e) {
      throw handleAndRaise("Failure in setting up reader", e);
    }
  }

  private static Type getSegmentType(String[] pathSegments, int depth, MessageType schema) {
    int nextDepth = depth + 1;
    Type type = schema.getType(Arrays.copyOfRange(pathSegments, 0, nextDepth));
    if (nextDepth == pathSegments.length) {
      return type;
    } else {
      Preconditions.checkState(!type.isPrimitive());
      return Types.buildGroup(type.getRepetition())
          .as(type.getOriginalType())
          .addField(getSegmentType(pathSegments, nextDepth, schema))
          .named(type.getName());
    }
  }

  @Override
  public int next() {
    // No columns found in the file were selected, simply return a full batch of null records for each column requested
    if (noColumnsFound) {
      if (totalRead == numRecordsToRead) {
        return 0;
      }
      for (ValueVector vv : nullFilledVectors) {
        vv.getMutator().setValueCount((int) numRecordsToRead);
      }
      totalRead = numRecordsToRead;
      return (int) numRecordsToRead;
    }

    int count = 0;
    while (count < recordsPerBatch && totalRead < numRecordsToRead) {
      recordMaterializer.setPosition(count);
      recordReader.read();
      count++;
      totalRead++;
    }
    recordMaterializer.setValueCount(count);
    // if we have requested columns that were not found in the file fill their vectors with null
    // (by simply setting the value counts inside of them, as they start null filled)
    if (nullFilledVectors != null) {
      for (ValueVector vv : nullFilledVectors) {
        vv.getMutator().setValueCount(count);
      }
    }
    return count;
  }

  @Override
  public void close() {
    closeStats(logger, entry.getPath());
    footer = null;
    drillFileSystem = null;
    entry = null;
    recordReader = null;
    recordMaterializer = null;
    nullFilledVectors = null;
    try {
      if (pageReadStore != null) {
        pageReadStore.close();
        pageReadStore = null;
      }
    } catch (IOException e) {
      logger.warn("Failure while closing PageReadStore", e);
    }
  }

  @Override
  public String toString() {
    StringJoiner stringJoiner = new StringJoiner(", ", DrillParquetReader.class.getSimpleName() + "[", "]")
      .add("schema=" + schema)
      .add("numRecordsToRead=" + numRecordsToRead);
      if (pageReadStore != null) {
        stringJoiner.add("pageReadStore=" + pageReadStore);
      }
      return stringJoiner.toString();
  }
}
