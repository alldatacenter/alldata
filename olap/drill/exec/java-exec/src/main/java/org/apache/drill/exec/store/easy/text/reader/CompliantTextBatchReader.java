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
package org.apache.drill.exec.store.easy.text.reader;

import com.univocity.parsers.common.TextParsingException;
import io.netty.buffer.DrillBuf;
import org.apache.drill.common.exceptions.UserException;
import org.apache.drill.common.types.TypeProtos.DataMode;
import org.apache.drill.common.types.TypeProtos.MinorType;
import org.apache.drill.exec.ops.OperatorContext;
import org.apache.drill.exec.physical.impl.scan.columns.ColumnsScanFramework;
import org.apache.drill.exec.physical.impl.scan.columns.ColumnsSchemaNegotiator;
import org.apache.drill.exec.physical.impl.scan.convert.StandardConversions;
import org.apache.drill.exec.physical.impl.scan.framework.ManagedReader;
import org.apache.drill.exec.physical.resultSet.RowSetLoader;
import org.apache.drill.exec.record.metadata.ColumnMetadata;
import org.apache.drill.exec.record.metadata.MetadataUtils;
import org.apache.drill.exec.record.metadata.TupleMetadata;
import org.apache.drill.exec.record.metadata.TupleSchema;
import org.apache.drill.exec.store.dfs.DrillFileSystem;
import org.apache.drill.exec.vector.accessor.ScalarWriter;
import org.apache.drill.exec.vector.accessor.ValueWriter;
import org.apache.hadoop.mapred.FileSplit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;

/**
 * Text reader, Complies with the RFC 4180 standard for text/csv files.
 */
public class CompliantTextBatchReader implements ManagedReader<ColumnsSchemaNegotiator> {
  private static final Logger logger = LoggerFactory.getLogger(CompliantTextBatchReader.class);

  private static final int MAX_RECORDS_PER_BATCH = 8096;
  private static final int READ_BUFFER = 1024 * 1024;
  private static final int WHITE_SPACE_BUFFER = 64 * 1024;

  // settings to be used while parsing
  private final TextParsingSettings settings;
  // Chunk of the file to be read by this reader
  private FileSplit split;
  // Limit pushed down from the query
  private final int maxRecords;
  // text reader implementation
  private TextReader reader;
  // input buffer
  private DrillBuf readBuffer;
  // working buffer to handle whitespace
  private DrillBuf whitespaceBuffer;
  private DrillFileSystem dfs;

  private RowSetLoader writer;

  public CompliantTextBatchReader(TextParsingSettings settings, int maxRecords) {
    this.settings = settings;
    this.maxRecords = maxRecords;

    // Validate. Otherwise, these problems show up later as a data
    // read error which is very confusing.
    if (settings.getNewLineDelimiter().length == 0) {
      throw UserException
        .validationError()
        .message("The text format line delimiter cannot be blank.")
        .build(logger);
    }
  }

  /**
   * Performs the initial setup required for the record reader.
   * Initializes the input stream, handling of the output record batch
   * and the actual reader to be used.
   *
   * @param schemaNegotiator Used to create the schema in the output record batch
   * @return true if opens successfully, false if output is null
   */
  @Override
  public boolean open(ColumnsSchemaNegotiator schemaNegotiator) {
    final OperatorContext context = schemaNegotiator.context();
    dfs = schemaNegotiator.fileSystem();
    split = schemaNegotiator.split();

    // Note: DO NOT use managed buffers here. They remain in existence
    // until the fragment is shut down. The buffers here are large.
    // If we scan 1000 files, and allocate 1 MB for each, we end up
    // holding onto 1 GB of memory in managed buffers.
    // Instead, we allocate the buffers explicitly, and must free
    // them.

    readBuffer = context.getAllocator().buffer(READ_BUFFER);
    whitespaceBuffer = context.getAllocator().buffer(WHITE_SPACE_BUFFER);
    schemaNegotiator.batchSize(MAX_RECORDS_PER_BATCH);

    // setup Output, Input, and Reader
    try {
      TextOutput output;
      if (settings.isHeaderExtractionEnabled()) {
        output = openWithHeaders(schemaNegotiator);
      } else {
        output = openWithoutHeaders(schemaNegotiator);
      }
      if (output == null) {
        return false;
      }
      openReader(output);
      return true;
    } catch (final IOException e) {
      throw UserException.dataReadError(e).addContext("File Path", split.getPath().toString()).build(logger);
    }
  }

  /**
   * Extract header and use that to define the reader schema.
   *
   * @param schemaNegotiator used to define the reader schema
   * provided schema, if any. Used when using a provided schema
   * with a text file that contains no headers; ignored for
   * text file with headers
   * @return text output
   */
  private TextOutput openWithHeaders(ColumnsSchemaNegotiator schemaNegotiator) throws IOException {
    final String [] fieldNames = extractHeader();
    if (fieldNames == null) {
      return null;
    }
    if (schemaNegotiator.hasProvidedSchema()) {
      return buildWithSchema(schemaNegotiator, fieldNames);
    } else {
      return buildFromColumnHeaders(schemaNegotiator, fieldNames);
    }
  }

  /**
   * File has headers and a provided schema is provided. Convert from VARCHAR
   * input type to the provided output type, but only if the column is projected.
   */
  private FieldVarCharOutput buildWithSchema(ColumnsSchemaNegotiator schemaNegotiator,
      String[] fieldNames) {
    TupleMetadata readerSchema = mergeSchemas(schemaNegotiator.providedSchema(), fieldNames);
    schemaNegotiator.tableSchema(readerSchema, true);
    writer = schemaNegotiator.build().writer();
    StandardConversions conversions = conversions(schemaNegotiator.providedSchema());
    ValueWriter[] colWriters = new ValueWriter[fieldNames.length];
    for (int i = 0; i < fieldNames.length; i++) {
      ScalarWriter colWriter = writer.scalar(fieldNames[i]);
      if (writer.isProjected()) {
        colWriters[i] = conversions.converterFor(colWriter, MinorType.VARCHAR);
      } else {
        colWriters[i] = colWriter;
      }
    }
    return new FieldVarCharOutput(writer, colWriters);
  }

  private TupleMetadata mergeSchemas(TupleMetadata providedSchema,
      String[] fieldNames) {
    final TupleMetadata readerSchema = new TupleSchema();
    for (String fieldName : fieldNames) {
      final ColumnMetadata providedCol = providedSchema.metadata(fieldName);
      readerSchema.addColumn(providedCol == null ? textColumn(fieldName) : providedCol);
    }
    return readerSchema;
  }

  private ColumnMetadata textColumn(String colName) {
    return MetadataUtils.newScalar(colName, MinorType.VARCHAR, DataMode.REQUIRED);
  }

  /**
   * File has column headers. No provided schema. Build schema from the
   * column headers.
   */
  private FieldVarCharOutput buildFromColumnHeaders(ColumnsSchemaNegotiator schemaNegotiator,
      String[] fieldNames) {
    final TupleMetadata schema = new TupleSchema();
    for (final String colName : fieldNames) {
      schema.addColumn(textColumn(colName));
    }
    schemaNegotiator.tableSchema(schema, true);
    writer = schemaNegotiator.build().writer();
    ValueWriter[] colWriters = new ValueWriter[fieldNames.length];
    for (int i = 0; i < fieldNames.length; i++) {
      colWriters[i] = writer.column(i).scalar();
    }
    return new FieldVarCharOutput(writer, colWriters);
  }

  /**
   * When no headers, create a single array column "columns".
   */
  private TextOutput openWithoutHeaders(
      ColumnsSchemaNegotiator schemaNegotiator) {
    if (schemaNegotiator.hasProvidedSchema()) {
      return buildWithSchema(schemaNegotiator);
    } else {
      return buildColumnsArray(schemaNegotiator);
    }
  }

  private FieldVarCharOutput buildWithSchema(ColumnsSchemaNegotiator schemaNegotiator) {
    TupleMetadata providedSchema = schemaNegotiator.providedSchema();
    schemaNegotiator.tableSchema(providedSchema, true);
    writer = schemaNegotiator.build().writer();
    StandardConversions conversions = conversions(providedSchema);
    ValueWriter[] colWriters = new ValueWriter[providedSchema.size()];
    for (int i = 0; i < colWriters.length; i++) {
      colWriters[i] = conversions.converterFor(
          writer.scalar(providedSchema.metadata(i).name()), MinorType.VARCHAR);
    }
    return new ConstrainedFieldOutput(writer, colWriters);
  }

  private TextOutput buildColumnsArray(
      ColumnsSchemaNegotiator schemaNegotiator) {
    schemaNegotiator.tableSchema(ColumnsScanFramework.columnsSchema(), true);
    writer = schemaNegotiator.build().writer();
    return new RepeatedVarCharOutput(writer, schemaNegotiator.projectedIndexes());
  }

  private void openReader(TextOutput output) throws IOException {
    logger.trace("Opening file {}", split.getPath());
    final InputStream stream = dfs.openPossiblyCompressedStream(split.getPath());
    final TextInput input = new TextInput(settings, stream, readBuffer,
        split.getStart(), split.getStart() + split.getLength());

    // setup Reader using Input and Output
    reader = new TextReader(settings, input, output, whitespaceBuffer);
    reader.start();
  }

  private StandardConversions conversions(TupleMetadata providedSchema) {

    // CSV maps blank columns to nulls (for nullable non-string columns),
    // or to the default value (for non-nullable non-string columns.)
    return StandardConversions.builder()
      .withSchema(providedSchema)
      .blankAs(ColumnMetadata.BLANK_AS_NULL)
      .build();
  }

  /**
   * Extracts header from text file.
   * Currently it is assumed to be first line if headerExtractionEnabled is set to true
   * TODO: enhance to support more common header patterns
   * @return field name strings
   */
  private String[] extractHeader() throws IOException {
    assert settings.isHeaderExtractionEnabled();

    // don't skip header in case skipFirstLine is set true
    settings.setSkipFirstLine(false);

    final HeaderBuilder hOutput = new HeaderBuilder(split.getPath());

    // setup Input using InputStream
    // we should read file header irrespective of split given given to this reader
    final InputStream hStream = dfs.openPossiblyCompressedStream(split.getPath());
    final TextInput hInput = new TextInput(settings, hStream, readBuffer, 0, split.getLength());

    // setup Reader using Input and Output
    this.reader = new TextReader(settings, hInput, hOutput, whitespaceBuffer);
    reader.start();

    // extract first row only
    reader.parseNext();

    // grab the field names from output
    final String [] fieldNames = hOutput.getHeaders();

    // cleanup and set to skip the first line next time we read input
    reader.close();
    settings.setSkipFirstLine(true);

    readBuffer.clear();
    whitespaceBuffer.clear();
    return fieldNames;
  }

  /**
   * Generates the next record batch
   * @return number of records in the batch
   */
  @Override
  public boolean next() {
    reader.resetForNextBatch();

    // If the limit is defined and the row count is greater than the limit, stop reading the file.
    if (maxRecords > 0 && writer.rowCount() > maxRecords) {
      return false;
    }

    try {
      boolean more = false;
      while (! writer.isFull()) {
        more = reader.parseNext();
        if (! more) {
          break;
        }
      }
      reader.finishBatch();

      // Return false on the batch that hits EOF. The scan operator
      // knows to process any rows in this final batch.

      return more;
    } catch (IOException | TextParsingException e) {
      if (e.getCause() != null  && e.getCause() instanceof UserException) {
        throw (UserException) e.getCause();
      }
      throw UserException.dataReadError(e)
          .addContext("Failure while reading file %s. Happened at or shortly before byte position %d.",
            split.getPath(), reader.getPos())
          .build(logger);
    }
  }

  /**
   * Cleanup state once we are finished processing all the records.
   * This would internally close the input stream we are reading from.
   */
  @Override
  public void close() {

    // Release the buffers allocated above. Double-check to handle
    // unexpected multiple calls to close().

    if (readBuffer != null) {
      readBuffer.release();
      readBuffer = null;
    }
    if (whitespaceBuffer != null) {
      whitespaceBuffer.release();
      whitespaceBuffer = null;
    }
    try {
      if (reader != null) {
        reader.close();
        reader = null;
      }
    } catch (final IOException e) {
      logger.warn("Exception while closing stream.", e);
    }
  }
}
