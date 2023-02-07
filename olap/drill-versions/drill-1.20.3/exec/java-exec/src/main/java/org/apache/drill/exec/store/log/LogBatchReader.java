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

package org.apache.drill.exec.store.log;

import org.apache.drill.common.exceptions.UserException;
import org.apache.drill.exec.physical.impl.scan.convert.StandardConversions;
import org.apache.drill.exec.physical.impl.scan.file.FileScanFramework.FileSchemaNegotiator;
import org.apache.drill.exec.physical.impl.scan.framework.ManagedReader;
import org.apache.drill.exec.physical.resultSet.ResultSetLoader;
import org.apache.drill.exec.physical.resultSet.RowSetLoader;
import org.apache.drill.exec.record.metadata.ColumnMetadata;
import org.apache.drill.exec.record.metadata.TupleMetadata;
import org.apache.drill.exec.vector.accessor.ScalarWriter;
import org.apache.drill.exec.vector.accessor.TupleWriter;
import org.apache.drill.exec.vector.accessor.ValueWriter;
import org.apache.drill.shaded.guava.com.google.common.base.Charsets;
import org.apache.hadoop.mapred.FileSplit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LogBatchReader implements ManagedReader<FileSchemaNegotiator> {
  private static final Logger logger = LoggerFactory.getLogger(LogBatchReader.class);
  public static final String RAW_LINE_COL_NAME = "_raw";
  public static final String UNMATCHED_LINE_COL_NAME = "_unmatched_rows";

  public static class LogReaderConfig {
    protected final LogFormatPlugin plugin;
    protected final Pattern pattern;
    protected final TupleMetadata providedSchema;
    protected final TupleMetadata tableSchema;
    protected final TupleMetadata readerSchema;
    protected final boolean asArray;
    protected final int groupCount;
    protected final int maxErrors;

    public LogReaderConfig(LogFormatPlugin plugin, Pattern pattern,
        TupleMetadata providedSchema, TupleMetadata tableSchema,
        TupleMetadata readerSchema,
        boolean asArray, int groupCount, int maxErrors) {
      this.plugin = plugin;
      this.pattern = pattern;
      this.providedSchema = providedSchema;
      this.tableSchema = tableSchema;
      this.readerSchema = readerSchema;
      this.asArray = asArray;
      this.groupCount = groupCount;
      this.maxErrors = maxErrors;
    }
  }

  /**
   * Write group values to value vectors.
   */
  private interface VectorWriter {
    void loadVectors(Matcher m);
  }

  /**
   * Write group values to individual scalar columns.
   */
  private static class ScalarGroupWriter implements VectorWriter {

    private final ValueWriter[] writers;

    public ScalarGroupWriter(TupleWriter rowWriter,
        TupleMetadata readerSchema, StandardConversions conversions) {
      writers = new ValueWriter[readerSchema.size()];
      for (int i = 0; i < writers.length; i++) {
        ColumnMetadata colSchema = readerSchema.metadata(i);
        writers[i] = conversions.converterFor(rowWriter.scalar(i), colSchema);
      }
    }

    @Override
    public void loadVectors(Matcher m) {
      for (int i = 0; i < m.groupCount(); i++) {
        String value = m.group(i + 1);
        if (value != null) {
          writers[i].setString(value);
        }
      }
    }
  }

  /**
   * Write group values to the columns[] array.
   */
  private static class ColumnsArrayWriter implements VectorWriter {

    private final ScalarWriter elementWriter;

    public ColumnsArrayWriter(TupleWriter rowWriter) {
      elementWriter = rowWriter.array(0).scalar();
    }

   @Override
    public void loadVectors(Matcher m) {
      for (int i = 0; i < m.groupCount(); i++) {
        String value = m.group(i + 1);
        elementWriter.setString(value == null ? "" : value);
      }
    }
  }

  private final LogReaderConfig config;
  private final int maxRecords;
  private FileSplit split;
  private BufferedReader reader;
  private ResultSetLoader loader;
  private VectorWriter vectorWriter;
  private ScalarWriter rawColWriter;
  private ScalarWriter unmatchedColWriter;
  private boolean saveMatchedRows;
  private int lineNumber;
  private int errorCount;

  public LogBatchReader(LogReaderConfig config, int maxRecords) {
    this.config = config;
    this.maxRecords = maxRecords;
  }

  @Override
  public boolean open(FileSchemaNegotiator negotiator) {
    split = negotiator.split();
    negotiator.tableSchema(config.tableSchema, true);
    loader = negotiator.build();
    bindColumns(loader.writer());
    openFile(negotiator);
    return true;
  }

  private void bindColumns(RowSetLoader writer) {
    rawColWriter = writer.scalar(RAW_LINE_COL_NAME);
    unmatchedColWriter = writer.scalar(UNMATCHED_LINE_COL_NAME);
    saveMatchedRows = rawColWriter.isProjected();

    // If no match-case columns are projected, and the unmatched
    // columns is unprojected, then we want to count (matched)
    // rows.
    saveMatchedRows |= !unmatchedColWriter.isProjected();

    // This reader is unusual: it can save only unmatched rows,
    // save only matched rows, or both. We check if we want to
    // save matched rows to by checking if any of the "normal"
    // reader columns are projected (ignoring the two special
    // columns.) If so, create a vector writer to save values.
    if (config.asArray) {
      saveMatchedRows |= writer.column(0).isProjected();
      if (saveMatchedRows) {
         // Save columns as an array
        vectorWriter = new ColumnsArrayWriter(writer);
      }
    } else {
      for (int i = 0; i <  config.readerSchema.size(); i++) {
        saveMatchedRows |= writer.column(i).isProjected();
      }
      if (saveMatchedRows) {
        // Save using the defined columns
        TupleMetadata providedSchema = config.providedSchema;
        StandardConversions conversions = StandardConversions.builder()
            .withSchema(providedSchema)
            .build();
        vectorWriter = new ScalarGroupWriter(writer, config.readerSchema, conversions);
      }
    }
  }

  private void openFile(FileSchemaNegotiator negotiator) {
    InputStream in;
    try {
      in = negotiator.fileSystem().openPossiblyCompressedStream(split.getPath());
    } catch (Exception e) {
      throw UserException
          .dataReadError(e)
          .message("Failed to open input file")
          .addContext(String.format("File path: %s", split.getPath()))
          .addContext(loader.errorContext())
          .build(logger);
    }
    reader = new BufferedReader(new InputStreamReader(in, Charsets.UTF_8));
  }

  @Override
  public boolean next() {
    RowSetLoader rowWriter = loader.writer();
    while (! rowWriter.isFull()) {
      if (! nextLine(rowWriter)) {
        return false;
      }
    }
    return true;
  }

  private boolean nextLine(RowSetLoader rowWriter) {
    if (rowWriter.limitReached(maxRecords)) {
      return false;
    }

    String line;
    try {
      line = reader.readLine();
    } catch (IOException e) {
      throw UserException
          .dataReadError(e)
          .message("Error reading file")
          .addContext(String.format("File: %s", split.getPath()))
          .addContext(loader.errorContext())
          .build(logger);
    }

    if (line == null) {
      return false;
    }
    lineNumber++;
    Matcher lineMatcher = config.pattern.matcher(line);
    if (lineMatcher.matches()) {

      // Load matched row into vectors.
      if (saveMatchedRows) {
        rowWriter.start();
        rawColWriter.setString(line);
        vectorWriter.loadVectors(lineMatcher);
        rowWriter.save();
      }
      return true;
    }

    errorCount++;
    if (errorCount < config.maxErrors) {
      logger.warn("Unmatched line: {}", line);
    } else {
      throw UserException.parseError()
          .message("Too many errors. Max error threshold exceeded.")
          .addContext("Line", line)
          .addContext("Line number", lineNumber)
          .addContext(loader.errorContext())
          .build(logger);
    }

    // For unmatched columns, create an output row only if the
    // user asked for the unmatched values.
    if (unmatchedColWriter.isProjected()) {
      rowWriter.start();
      unmatchedColWriter.setString(line);
      rowWriter.save();
    }
    return true;
  }

  @Override
  public void close() {
    if (reader == null) {
      return;
    }
    try {
      reader.close();
    } catch (IOException e) {
      logger.warn("Error when closing file: " + split.getPath(), e);
    } finally {
      reader = null;
    }
  }

  @Override
  public String toString() {
    return String.format(
        "LogRecordReader[File=%s, Line=%d]",
        split.getPath(), lineNumber);
  }
}
