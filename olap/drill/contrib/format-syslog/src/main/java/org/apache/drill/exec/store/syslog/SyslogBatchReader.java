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

package org.apache.drill.exec.store.syslog;

import org.apache.drill.common.AutoCloseables;
import org.apache.drill.common.exceptions.CustomErrorContext;
import org.apache.drill.common.exceptions.UserException;
import org.apache.drill.common.types.TypeProtos;
import org.apache.drill.common.types.TypeProtos.MinorType;
import org.apache.drill.exec.physical.impl.scan.file.FileScanFramework.FileSchemaNegotiator;
import org.apache.drill.exec.physical.impl.scan.framework.ManagedReader;
import org.apache.drill.exec.physical.resultSet.ResultSetLoader;
import org.apache.drill.exec.physical.resultSet.RowSetLoader;
import org.apache.drill.exec.record.metadata.ColumnMetadata;
import org.apache.drill.exec.record.metadata.MetadataUtils;
import org.apache.drill.exec.record.metadata.SchemaBuilder;
import org.apache.drill.exec.record.metadata.TupleMetadata;
import org.apache.drill.exec.store.dfs.easy.EasySubScan;
import org.apache.drill.exec.vector.accessor.ScalarWriter;
import org.apache.drill.exec.vector.accessor.TupleWriter;
import org.apache.hadoop.mapred.FileSplit;
import org.realityforge.jsyslog.message.StructuredDataParameter;
import org.realityforge.jsyslog.message.SyslogMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class SyslogBatchReader implements ManagedReader<FileSchemaNegotiator> {
  private static final Logger logger = LoggerFactory.getLogger(SyslogBatchReader.class);
  private final String STRUCTURED_DATA_PREFIX = "structured_data_";
  private final String STRUCTURED_DATA_MAP_NAME = "structured_data";
  private final String RAW_COLUMN_NAME = "_raw";

  private final int maxRecords;
  private final SyslogFormatConfig config;
  private final EasySubScan subScan;
  private final Map<String, MinorType> mappedColumns = new LinkedHashMap<>();
  private int lineCount;
  private int errorCount;
  private CustomErrorContext errorContext;
  private InputStream fsStream;
  private FileSplit split;
  private BufferedReader reader;
  private RowSetLoader rowWriter;
  private List<ScalarWriter> writerArray;
  private ScalarWriter rawColumnWriter;
  private ScalarWriter messageWriter;
  private TupleWriter structuredDataWriter;


  public SyslogBatchReader(int maxRecords, SyslogFormatConfig config, EasySubScan scan) {
    this.maxRecords = maxRecords;
    this.config = config;
    this.subScan = scan;
    populateMappedColumns();
  }

  @Override
  public boolean open(FileSchemaNegotiator negotiator) {
    split = negotiator.split();
    openFile(negotiator);
    negotiator.tableSchema(buildSchema(), false);
    errorContext = negotiator.parentErrorContext();

    ResultSetLoader loader = negotiator.build();
    rowWriter = loader.writer();
    writerArray = populateRowWriters();
    rawColumnWriter = rowWriter.scalar(RAW_COLUMN_NAME);
    messageWriter = rowWriter.scalar("message");
    return true;
  }

  @Override
  public boolean next() {
    while (!rowWriter.isFull()) {
      if (!processNextLine()) {
        return false;
      }
    }
    return true;
  }

  @Override
  public void close() {
    if (fsStream != null) {
      AutoCloseables.closeSilently(fsStream);
      fsStream = null;
    }

    if (reader != null) {
      AutoCloseables.closeSilently(reader);
      reader = null;
    }
  }

  private void openFile(FileSchemaNegotiator negotiator) {
    try {
      fsStream = negotiator.fileSystem().openPossiblyCompressedStream(split.getPath());
    } catch (IOException e) {
      throw UserException
        .dataReadError(e)
        .message("Unable to open Syslog File %s", split.getPath())
        .addContext(e.getMessage())
        .addContext(errorContext)
        .build(logger);
    }
    this.lineCount = 0;
    reader = new BufferedReader(new InputStreamReader(fsStream));
  }

  public TupleMetadata buildSchema() {
    SchemaBuilder builder = new SchemaBuilder();
    for (Map.Entry<String, MinorType> entry : mappedColumns.entrySet()) {
      builder.addNullable(entry.getKey(), entry.getValue());
    }
    if (! config.flattenStructuredData()) {
      ColumnMetadata structuredDataMap = MetadataUtils.newMap(STRUCTURED_DATA_MAP_NAME);
      builder.add(structuredDataMap);
    }

    builder.addNullable("message", MinorType.VARCHAR);

    // Add _raw column
    ColumnMetadata colSchema = MetadataUtils.newScalar(RAW_COLUMN_NAME, MinorType.VARCHAR, TypeProtos.DataMode.OPTIONAL);
    colSchema.setBooleanProperty(ColumnMetadata.EXCLUDE_FROM_WILDCARD, true);
    builder.add(colSchema);
    return builder.buildSchema();
  }

  private List<ScalarWriter> populateRowWriters() {
    List<ScalarWriter> writerArray = new ArrayList<>();
    for (Map.Entry<String, MinorType> entry : mappedColumns.entrySet()) {
      writerArray.add(rowWriter.scalar(entry.getKey()));
    }

    if (! config.flattenStructuredData()) {
       structuredDataWriter = rowWriter.tuple(STRUCTURED_DATA_MAP_NAME);
    }

    return writerArray;
  }

  private void populateMappedColumns() {
    mappedColumns.put("event_date", MinorType.TIMESTAMP);
    mappedColumns.put("severity_code", MinorType.INT);
    mappedColumns.put("facility_code", MinorType.INT);
    mappedColumns.put("severity", MinorType.VARCHAR);
    mappedColumns.put("facility", MinorType.VARCHAR);
    mappedColumns.put("ip", MinorType.VARCHAR);
    mappedColumns.put("app_name", MinorType.VARCHAR);
    mappedColumns.put("process_id", MinorType.VARCHAR);
    mappedColumns.put("message_id", MinorType.VARCHAR);
    mappedColumns.put("structured_data_text", MinorType.VARCHAR);
  }

  private boolean processNextLine() {
    // Check to see if the limit has been reached
    if (rowWriter.limitReached(maxRecords)) {
      return false;
    }

    String line;
    try {
      line = reader.readLine();

      // If the line is empty, return false
      if (line == null) {
        return false;
      }

      // Remove leading and trailing whitespace
      line = line.trim();
      if (line.length() == 0) {
        // Skip empty lines
        return true;
      }

      SyslogMessage parsedMessage = SyslogMessage.parseStructuredSyslogMessage(line);
      rowWriter.start();
      writeStructuredColumns(parsedMessage);
      writeStructuredData(parsedMessage);

      if (isProjected(rawColumnWriter)) {
        rawColumnWriter.setString(line);
      }

      if (isProjected(messageWriter)) {
        logger.debug("Message: {}", parsedMessage.getMessage());
        messageWriter.setString(parsedMessage.getMessage());
      }

    } catch (IOException e) {
      errorCount++;
      if (errorCount > config.getMaxErrors()) {
        throw UserException
          .dataReadError()
          .message("Maximum Error Threshold Exceeded. Error reading Syslog file at line %d", lineCount)
          .addContext(e.getMessage())
          .build(logger);
      }
    }
    lineCount++;
    rowWriter.save();
    return true;
  }

  private void writeStructuredColumns(SyslogMessage parsedMessage) {
    long milliseconds = parsedMessage.getTimestamp().getMillis();
    writerArray.get(0).setTimestamp(Instant.ofEpochMilli(milliseconds));
    writerArray.get(1).setInt(parsedMessage.getLevel().ordinal());
    writerArray.get(2).setInt(parsedMessage.getFacility().ordinal());
    setString(writerArray.get(3), parsedMessage.getLevel().name());
    setString(writerArray.get(4), parsedMessage.getFacility().name());
    setString(writerArray.get(5), parsedMessage.getHostname());
    setString(writerArray.get(6), parsedMessage.getAppName());
    setString(writerArray.get(7), parsedMessage.getProcId());
    setString(writerArray.get(8), parsedMessage.getMsgId());

    Map<String, List<StructuredDataParameter>> structuredData = parsedMessage.getStructuredData();

    if (structuredData != null) {
      writerArray.get(9).setString(parsedMessage.getStructuredData().toString());
    }
    logger.debug("Successfully mapped known fields");
  }

  /**
   * Write the flattened structured data fields to Drill vectors. The data in the structured fields is not known in
   * advance and also is not consistent between syslog entries, so we have to add these fields on the fly.  The only possible
   * data type in these cases are VARCHARs.
   * @param parsedMessage The parsed syslog message
   */
  private void writeStructuredData(SyslogMessage parsedMessage) {
    Map<String, List<StructuredDataParameter>> structuredData = parsedMessage.getStructuredData();
    // Prevent NPE if there is no structured data text
    if (structuredData == null) {
      return;
    }

    if (config.flattenStructuredData()) {
      // Iterate over the structured data fields and map to Drill vectors
      for (Map.Entry<String, List<StructuredDataParameter>> entry : structuredData.entrySet()) {
        for (StructuredDataParameter parameter : entry.getValue()) {
          // These fields are not known in advance and are not necessarily consistent
          String fieldName = STRUCTURED_DATA_PREFIX + parameter.getName();
          String fieldValue = parameter.getValue();
          writeStringColumn(rowWriter, fieldName, fieldValue);
          logger.debug("Writing {} {}", fieldName, fieldValue);
        }
      }
    } else {
      writeStructuredDataToMap(structuredData);
    }
  }

  private void writeStructuredDataToMap(Map<String, List<StructuredDataParameter>> structuredData) {
    // Iterate over the structured data fields and write to a Drill map
    for (Map.Entry<String, List<StructuredDataParameter>> entry : structuredData.entrySet()) {
      for (StructuredDataParameter parameter : entry.getValue()) {
        // These fields are not known in advance and are not necessarily consistent
        String fieldName = parameter.getName();
        String fieldValue = parameter.getValue();
        writeStringColumn(structuredDataWriter, fieldName, fieldValue);
      }
    }
  }

  /**
   * Writes data to a String column.  If there is no ScalarWriter for the particular column, this function will create one.
   * @param rowWriter The ScalarWriter to which we are writing
   * @param name The field name to be written
   * @param value The field value to be written
   */
  private void writeStringColumn(TupleWriter rowWriter, String name, String value) {
    ScalarWriter colWriter = getColWriter(rowWriter, name, TypeProtos.MinorType.VARCHAR);
    colWriter.setString(value);
  }

  private ScalarWriter getColWriter(TupleWriter tupleWriter, String fieldName, TypeProtos.MinorType type) {
    int index = tupleWriter.tupleSchema().index(fieldName);
    if (index == -1) {
      ColumnMetadata colSchema = MetadataUtils.newScalar(fieldName, type, TypeProtos.DataMode.OPTIONAL);
      index = tupleWriter.addColumn(colSchema);
    }
    return tupleWriter.scalar(index);
  }

  /**
   * The ScalarWriter objects have method to verify whether the writer is projected or not, however it does not
   * seem to take the star queries into account.  This method checks to see if the query is a star query and includes that
   * in the determination of whether the column is projected or not.
   * @param writer A scalarWriter
   * @return True if the column is projected, false if not.
   */
  private boolean isProjected(ScalarWriter writer) {
    // Case for star query
    if (subScan.getColumns().size() == 1 && subScan.getColumns().get(0).isDynamicStar()) {
      return true;
    } else {
      return writer.isProjected();
    }
  }

  private void setString(ScalarWriter writer, String value) {
    if (value == null) {
      return;
    }
    writer.setString(value);
  }
}
