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
package org.apache.drill.exec.store.mapr.db.json;


import com.mapr.db.Table;
import com.mapr.db.Table.TableOption;
import com.mapr.db.exceptions.DBException;
import com.mapr.db.impl.IdCodec;
import com.mapr.db.impl.MapRDBImpl;
import com.mapr.db.index.IndexDesc;
import com.mapr.db.ojai.DBDocumentReaderBase;
import com.mapr.db.util.ByteBufs;
import io.netty.buffer.DrillBuf;
import org.apache.drill.common.exceptions.ExecutionSetupException;
import org.apache.drill.common.exceptions.UserException;
import org.apache.drill.common.expression.PathSegment;
import org.apache.drill.common.expression.SchemaPath;
import org.apache.drill.exec.ExecConstants;
import org.apache.drill.exec.exception.SchemaChangeException;
import org.apache.drill.exec.expr.fn.impl.DateUtility;
import org.apache.drill.exec.ops.FragmentContext;
import org.apache.drill.exec.ops.OperatorContext;
import org.apache.drill.exec.ops.OperatorStats;
import org.apache.drill.exec.physical.impl.OutputMutator;
import org.apache.drill.exec.record.metadata.TupleMetadata;
import org.apache.drill.exec.store.AbstractRecordReader;
import org.apache.drill.exec.store.mapr.db.MapRDBFormatPlugin;
import org.apache.drill.exec.store.mapr.db.MapRDBSubScanSpec;
import org.apache.drill.exec.util.EncodedSchemaPathSet;
import org.apache.drill.exec.vector.BaseValueVector;
import org.apache.drill.exec.vector.complex.fn.JsonReaderUtils;
import org.apache.drill.exec.vector.complex.impl.MapOrListWriterImpl;
import org.apache.drill.exec.vector.complex.impl.VectorContainerWriter;
import org.apache.hadoop.fs.Path;
import org.ojai.Document;
import org.ojai.DocumentReader;
import org.ojai.DocumentStream;
import org.ojai.FieldPath;
import org.ojai.FieldSegment;
import org.ojai.store.QueryCondition;
import org.ojai.util.FieldProjector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.drill.shaded.guava.com.google.common.base.Preconditions;
import org.apache.drill.shaded.guava.com.google.common.base.Stopwatch;
import org.apache.drill.shaded.guava.com.google.common.collect.ImmutableSet;
import org.apache.drill.shaded.guava.com.google.common.collect.Iterables;
import org.apache.drill.shaded.guava.com.google.common.collect.Sets;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Stack;
import java.util.concurrent.TimeUnit;

import static org.apache.drill.exec.store.mapr.PluginConstants.DOCUMENT_SCHEMA_PATH;
import static org.apache.drill.exec.store.mapr.PluginErrorHandler.dataReadError;
import static org.ojai.DocumentConstants.ID_FIELD;

public class MaprDBJsonRecordReader extends AbstractRecordReader {
  private static final Logger logger = LoggerFactory.getLogger(MaprDBJsonRecordReader.class);
  protected enum SchemaState {SCHEMA_UNKNOWN, SCHEMA_INIT, SCHEMA_CHANGE};

  protected static final FieldPath[] ID_ONLY_PROJECTION = { ID_FIELD };

  protected Table table;
  protected QueryCondition condition;

  /**
   * A set of projected FieldPaths that are pushed into MapR-DB Scanner.
   * This set is a superset of the fields returned by {@link #getColumns()} when
   * projection pass-through is in effect. In such cases, {@link #getColumns()}
   * returns only those fields which are required by Drill to run its operators.
   */
  private FieldPath[] scannedFields;

  private OperatorContext operatorContext;
  protected VectorContainerWriter vectorWriter;
  private DBDocumentReaderBase reader;
  Document document;
  protected OutputMutator vectorWriterMutator;

  private DrillBuf buffer;

  private DocumentStream documentStream;

  private Iterator<DocumentReader> documentReaderIterators;
  private Iterator<Document> documentIterator;

  private boolean includeId;
  private boolean idOnly;
  private SchemaState schemaState;
  private boolean projectWholeDocument;
  private FieldProjector projector;

  private final boolean unionEnabled;
  private final boolean readNumbersAsDouble;
  private final boolean readTimestampWithZoneOffset;
  private boolean disablePushdown;
  private final boolean allTextMode;
  private final boolean ignoreSchemaChange;
  private final boolean disableCountOptimization;
  private final boolean nonExistentColumnsProjection;
  private final TupleMetadata schema;

  protected final MapRDBSubScanSpec subScanSpec;
  protected final MapRDBFormatPlugin formatPlugin;

  protected OjaiValueWriter valueWriter;
  protected DocumentReaderVectorWriter documentWriter;
  protected int maxRecordsToRead = -1;
  protected DBDocumentReaderBase lastDocumentReader;
  protected Document lastDocument;

  public MaprDBJsonRecordReader(MapRDBSubScanSpec subScanSpec, MapRDBFormatPlugin formatPlugin,
                                List<SchemaPath> projectedColumns, FragmentContext context, int maxRecords, TupleMetadata schema) {
    this(subScanSpec, formatPlugin, projectedColumns, context, schema);
    this.maxRecordsToRead = maxRecords;
    this.lastDocumentReader = null;
    this.lastDocument = null;
    this.schemaState = SchemaState.SCHEMA_UNKNOWN;
  }

  protected MaprDBJsonRecordReader(MapRDBSubScanSpec subScanSpec, MapRDBFormatPlugin formatPlugin,
                                List<SchemaPath> projectedColumns, FragmentContext context, TupleMetadata schema) {
    buffer = context.getManagedBuffer();
    final Path tablePath = new Path(Preconditions.checkNotNull(subScanSpec,
      "MapRDB reader needs a sub-scan spec").getTableName());
    this.subScanSpec = subScanSpec;
    this.formatPlugin = formatPlugin;
    this.schema = schema;
    final IndexDesc indexDesc = subScanSpec.getIndexDesc();
    byte[] serializedFilter = subScanSpec.getSerializedFilter();
    condition = null;

    if (serializedFilter != null) {
      condition = com.mapr.db.impl.ConditionImpl.parseFrom(ByteBufs.wrap(serializedFilter));
    }

    disableCountOptimization = formatPlugin.getConfig().disableCountOptimization();
    // Below call will set the scannedFields and includeId correctly
    setColumns(projectedColumns);
    unionEnabled = context.getOptions().getOption(ExecConstants.ENABLE_UNION_TYPE);
    readNumbersAsDouble = formatPlugin.getConfig().isReadAllNumbersAsDouble();
    readTimestampWithZoneOffset = formatPlugin.getConfig().isReadTimestampWithZoneOffset();
    allTextMode = formatPlugin.getConfig().isAllTextMode();
    ignoreSchemaChange = formatPlugin.getConfig().isIgnoreSchemaChange();
    disablePushdown = !formatPlugin.getConfig().isEnablePushdown();
    nonExistentColumnsProjection = formatPlugin.getConfig().isNonExistentFieldSupport();

    // Do not use cached table handle for two reasons.
    // cached table handles default timeout is 60 min after which those handles will become stale.
    // Since execution can run for longer than 60 min, we want to get a new table handle and use it
    // instead of the one from cache.
    // Since we are setting some table options, we do not want to use shared handles.
    //
    // Call it here instead of setup since this will make sure it's called under correct UGI block when impersonation
    // is enabled and table is used with and without views.
    table = (indexDesc == null ? MapRDBImpl.getTable(tablePath) : MapRDBImpl.getIndexTable(indexDesc));

    if (condition != null) {
      logger.debug("Created record reader with query condition {}", condition.toString());
    } else {
      logger.debug("Created record reader with query condition NULL");
    }
  }

  @Override
  protected Collection<SchemaPath> transformColumns(Collection<SchemaPath> columns) {
    Set<SchemaPath> transformed = Sets.newLinkedHashSet();
    Set<SchemaPath> encodedSchemaPathSet = Sets.newLinkedHashSet();

    if (disablePushdown) {
      transformed.add(SchemaPath.STAR_COLUMN);
      includeId = true;
    } else {
      if (isStarQuery()) {
        transformed.add(SchemaPath.STAR_COLUMN);
        includeId = true;
        if (isSkipQuery() && !disableCountOptimization) {
          // `SELECT COUNT(*)` query
          idOnly = true;
          scannedFields = ID_ONLY_PROJECTION;
        }
      } else {
        Set<FieldPath> scannedFieldsSet = Sets.newTreeSet();
        Set<FieldPath> projectedFieldsSet = null;

        for (SchemaPath column : columns) {
          if (EncodedSchemaPathSet.isEncodedSchemaPath(column)) {
            encodedSchemaPathSet.add(column);
          } else {
            transformed.add(column);
            if (!DOCUMENT_SCHEMA_PATH.equals(column)) {
              FieldPath fp = getFieldPathForProjection(column);
              scannedFieldsSet.add(fp);
            } else {
              projectWholeDocument = true;
            }
          }
        }
        if (projectWholeDocument) {
          // we do not want to project the fields from the encoded field path list
          // hence make a copy of the scannedFieldsSet here for projection.
          projectedFieldsSet = new ImmutableSet.Builder<FieldPath>()
              .addAll(scannedFieldsSet).build();
        }

        if (encodedSchemaPathSet.size() > 0) {
          Collection<SchemaPath> decodedSchemaPaths = EncodedSchemaPathSet.decode(encodedSchemaPathSet);
          // now we look at the fields which are part of encoded field set and either
          // add them to scanned set or clear the scanned set if all fields were requested.
          for (SchemaPath column : decodedSchemaPaths) {
            if (column.equals(SchemaPath.STAR_COLUMN)) {
              includeId = true;
              scannedFieldsSet.clear();
              break;
            }
            scannedFieldsSet.add(getFieldPathForProjection(column));
          }
        }

        if (scannedFieldsSet.size() > 0) {
          if (includesIdField(scannedFieldsSet)) {
            includeId = true;
          }
          scannedFields = scannedFieldsSet.toArray(new FieldPath[scannedFieldsSet.size()]);
        }

        if (disableCountOptimization) {
          idOnly = (scannedFields == null);
        }

        if (projectWholeDocument) {
          projector = new FieldProjector(projectedFieldsSet);
        }

      }
    }
    return transformed;
  }

  protected FieldPath[] getScannedFields() {
    return scannedFields;
  }

  protected boolean getIdOnly() {
    return idOnly;
  }

  protected Table getTable() {
    return table;
  }

  protected boolean getIgnoreSchemaChange() {
    return ignoreSchemaChange;
  }

  @Override
  public void setup(OperatorContext context, OutputMutator output) throws ExecutionSetupException {
    this.vectorWriter = new VectorContainerWriter(output, unionEnabled);
    this.vectorWriterMutator = output;
    this.operatorContext = context;

    try {
      table.setOption(TableOption.EXCLUDEID, !includeId);
      documentStream = table.find(condition, scannedFields);
      documentIterator = documentStream.iterator();
      setupWriter();
    } catch (DBException ex) {
      throw new ExecutionSetupException(ex);
    }
  }

  /**
   * Setup the valueWriter and documentWriters based on config options.
   */
  private void setupWriter() {
    if (allTextMode) {
      if (readTimestampWithZoneOffset) {
        valueWriter = new AllTextValueWriter(buffer) {
          /**
           * Applies local time zone offset to timestamp value read using specified {@code reader}.
           *
           * @param writer    writer to store string representation of timestamp value
           * @param fieldName name of the field
           * @param reader    document reader
           */
          @Override
          protected void writeTimeStamp(MapOrListWriterImpl writer, String fieldName, DocumentReader reader) {
            String formattedTimestamp = Instant.ofEpochMilli(reader.getTimestampLong())
                .atZone(ZoneId.systemDefault()).format(DateUtility.UTC_FORMATTER);
            writeString(writer, fieldName, formattedTimestamp);
          }
        };
      } else {
        valueWriter = new AllTextValueWriter(buffer);
      }
    } else if (readNumbersAsDouble) {
      if (readTimestampWithZoneOffset) {
        valueWriter = new NumbersAsDoubleValueWriter(buffer) {
          @Override
          protected void writeTimeStamp(MapOrListWriterImpl writer, String fieldName, DocumentReader reader) {
            writeTimestampWithLocalZoneOffset(writer, fieldName, reader);
          }
        };
      } else {
        valueWriter = new NumbersAsDoubleValueWriter(buffer);
      }
    } else {
      if (readTimestampWithZoneOffset) {
        valueWriter = new OjaiValueWriter(buffer) {
          @Override
          protected void writeTimeStamp(MapOrListWriterImpl writer, String fieldName, DocumentReader reader) {
            writeTimestampWithLocalZoneOffset(writer, fieldName, reader);
          }
        };
      } else {
        valueWriter = new OjaiValueWriter(buffer);
      }
    }

    if (projectWholeDocument) {
      documentWriter = new ProjectionPassthroughVectorWriter(valueWriter, projector, includeId);
    } else if (isSkipQuery()) {
      documentWriter = new RowCountVectorWriter(valueWriter);
    } else if (idOnly) {
      documentWriter = new IdOnlyVectorWriter(valueWriter);
    } else {
      documentWriter = new FieldTransferVectorWriter(valueWriter);
    }
  }

  /**
   * Applies local time zone offset to timestamp value read using specified {@code reader}.
   *
   * @param writer    writer to store timestamp value
   * @param fieldName name of the field
   * @param reader    document reader
   */
  private void writeTimestampWithLocalZoneOffset(MapOrListWriterImpl writer, String fieldName, DocumentReader reader) {
    Instant utcInstant = Instant.ofEpochMilli(reader.getTimestampLong());
    ZonedDateTime localZonedDateTime = utcInstant.atZone(ZoneId.systemDefault());
    ZonedDateTime convertedZonedDateTime = localZonedDateTime.withZoneSameLocal(ZoneId.of("UTC"));
    long timeStamp = convertedZonedDateTime.toInstant().toEpochMilli();
    writer.timeStamp(fieldName).writeTimeStamp(timeStamp);
  }

  @Override
  public int next() {
    Stopwatch watch = Stopwatch.createUnstarted();
    watch.start();

    vectorWriter.allocate();
    vectorWriter.reset();

    int recordCount = 0;
    reader = null;
    document = null;

    int maxRecordsForThisBatch = this.maxRecordsToRead >= 0?
        Math.min(BaseValueVector.INITIAL_VALUE_ALLOCATION, this.maxRecordsToRead) : BaseValueVector.INITIAL_VALUE_ALLOCATION;

    try {
      // If the last document caused a SchemaChange create a new output schema for this scan batch
      if (schemaState == SchemaState.SCHEMA_CHANGE && !ignoreSchemaChange) {
        // Clear the ScanBatch vector container writer/mutator in order to be able to generate the new schema
        vectorWriterMutator.clear();
        vectorWriter = new VectorContainerWriter(vectorWriterMutator, unionEnabled);
        logger.debug("Encountered schema change earlier use new writer {}", vectorWriter.toString());
        document = lastDocument;
        setupWriter();
        if (recordCount < maxRecordsForThisBatch) {
          vectorWriter.setPosition(recordCount);
          if (document != null) {
            reader = (DBDocumentReaderBase) document.asReader();
            documentWriter.writeDBDocument(vectorWriter, reader);
            recordCount++;
          }
        }
      }
    } catch (SchemaChangeException e) {
      String err_row = reader.getId().asJsonString();
      if (ignoreSchemaChange) {
        logger.warn("{}. Dropping row '{}' from result.", e.getMessage(), err_row);
        logger.debug("Stack trace:", e);
      } else {
          /* We should not encounter a SchemaChangeException here since this is the first document for this
           * new schema. Something is very wrong - cannot handle any further!
           */
        throw dataReadError(logger, e, "SchemaChangeException for row '%s'.", err_row);
      }
    }
    schemaState = SchemaState.SCHEMA_INIT;
    while(recordCount < maxRecordsForThisBatch) {
      vectorWriter.setPosition(recordCount);
      try {
        document = nextDocument();
        if (document == null) {
          break; // no more documents for this reader
        } else {
          documentWriter.writeDBDocument(vectorWriter, (DBDocumentReaderBase) document.asReader());
        }
        recordCount++;
      } catch (UserException e) {
        throw UserException.unsupportedError(e)
            .addContext(String.format("Table: %s, document id: '%s'",
                table.getPath(),
                    document.asReader() == null ? null :
                        IdCodec.asString(((DBDocumentReaderBase)document.asReader()).getId())))
            .build(logger);
      } catch (SchemaChangeException e) {
        String err_row = ((DBDocumentReaderBase)document.asReader()).getId().asJsonString();
        if (ignoreSchemaChange) {
          logger.warn("{}. Dropping row '{}' from result.", e.getMessage(), err_row);
          logger.debug("Stack trace:", e);
        } else {
          /* Save the current document reader for next iteration. The recordCount is not updated so we
           * would start from this reader on the next next() call
           */
          lastDocument = document;
          schemaState = SchemaState.SCHEMA_CHANGE;
          break;
        }
      }
    }

    if (nonExistentColumnsProjection && recordCount > 0) {
      if (schema == null || schema.isEmpty()) {
        JsonReaderUtils.ensureAtLeastOneField(vectorWriter, getColumns(), allTextMode, Collections.emptyList());
      } else {
        JsonReaderUtils.writeColumnsUsingSchema(vectorWriter, getColumns(), schema, allTextMode);
      }
    }
    vectorWriter.setValueCount(recordCount);
    if (maxRecordsToRead > 0) {
      maxRecordsToRead -= recordCount;
    }
    logger.debug("Took {} ms to get {} records", watch.elapsed(TimeUnit.MILLISECONDS), recordCount);
    return recordCount;
  }

  protected DBDocumentReaderBase nextDocumentReader() {
    final OperatorStats operatorStats = operatorContext == null ? null : operatorContext.getStats();
    try {
      if (operatorStats != null) {
        operatorStats.startWait();
      }
      try {
        if (!documentReaderIterators.hasNext()) {
          return null;
        } else {
          return (DBDocumentReaderBase) documentReaderIterators.next();
        }
      } finally {
        if (operatorStats != null) {
          operatorStats.stopWait();
        }
      }
    } catch (DBException e) {
      throw dataReadError(logger, e);
    }
  }

  protected Document nextDocument() {
    final OperatorStats operatorStats = operatorContext == null ? null : operatorContext.getStats();
    try {
      if (operatorStats != null) {
        operatorStats.startWait();
      }
      try {
        if (!documentIterator.hasNext()) {
          return null;
        } else {
          return documentIterator.next();
        }
      } finally {
        if (operatorStats != null) {
          operatorStats.stopWait();
        }
      }
    } catch (DBException e) {
      throw dataReadError(logger, e);
    }
  }
  /*
   * Extracts contiguous named segments from the SchemaPath, starting from the
   * root segment and build the FieldPath from it for projection.
   *
   * This is due to bug 22726 and 22727, which cause DB's DocumentReaders to
   * behave incorrectly for sparse lists, hence we avoid projecting beyond the
   * first encountered ARRAY field and let Drill handle the projection.
   */
  private static FieldPath getFieldPathForProjection(SchemaPath column) {
    Stack<PathSegment.NameSegment> pathSegments = new Stack<>();
    PathSegment seg = column.getRootSegment();
    while (seg != null && seg.isNamed()) {
      pathSegments.push((PathSegment.NameSegment) seg);
      seg = seg.getChild();
    }
    FieldSegment.NameSegment child = null;
    while (!pathSegments.isEmpty()) {
      child = new FieldSegment.NameSegment(pathSegments.pop().getPath(), child, false);
    }
    return new FieldPath(child);
  }

  public static boolean includesIdField(Collection<FieldPath> projected) {
    return Iterables.tryFind(projected, path -> Preconditions.checkNotNull(path).equals(ID_FIELD)).isPresent();
  }

  @Override
  public void close() {
    if (documentStream != null) {
      documentStream.close();
    }
    if (table != null) {
      table.close();
    }
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder("MaprDBJsonRecordReader[Table=")
        .append(table != null ? table.getPath() : null);
    if (reader != null) {
      sb.append(", Document ID=")
          .append(IdCodec.asString(reader.getId()));
    }
    sb.append(", reader=")
        .append(reader)
        .append(']');
    return sb.toString();
  }
}
