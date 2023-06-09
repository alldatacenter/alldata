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

package org.apache.drill.exec.store.pdf;

import org.apache.drill.exec.record.MaterializedField;
import org.apache.drill.shaded.guava.com.google.common.base.Strings;
import org.apache.drill.common.AutoCloseables;
import org.apache.drill.common.exceptions.CustomErrorContext;
import org.apache.drill.common.exceptions.UserException;
import org.apache.drill.common.types.TypeProtos.MinorType;
import org.apache.drill.exec.physical.impl.scan.file.FileScanFramework;
import org.apache.drill.exec.physical.impl.scan.framework.ManagedReader;
import org.apache.drill.exec.physical.resultSet.ResultSetLoader;
import org.apache.drill.exec.physical.resultSet.RowSetLoader;
import org.apache.drill.exec.record.metadata.ColumnMetadata;
import org.apache.drill.exec.record.metadata.SchemaBuilder;
import org.apache.drill.exec.record.metadata.TupleMetadata;
import org.apache.drill.exec.vector.accessor.ScalarWriter;
import org.apache.hadoop.mapred.FileSplit;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import technology.tabula.RectangularTextContainer;
import technology.tabula.Table;

import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

public class PdfBatchReader implements ManagedReader<FileScanFramework.FileSchemaNegotiator> {

  private static final Logger logger = LoggerFactory.getLogger(PdfBatchReader.class);
  private static final String NEW_FIELD_PREFIX = "field_";
  private final int maxRecords;

  private final List<PdfColumnWriter> writers;
  private final PdfReaderConfig config;
  private final int startingTableIndex;
  private PdfMetadataReader metadataReader;
  private FileSplit split;
  private CustomErrorContext errorContext;
  private RowSetLoader rowWriter;
  private PDDocument document;

  private SchemaBuilder builder;
  private List<String> columnHeaders;
  private Table currentTable;
  private int currentTableIndex;
  private List<String> firstRow;
  private PdfRowIterator rowIterator;
  private FileScanFramework.FileSchemaNegotiator negotiator;
  private int unregisteredColumnCount;

  // Tables
  private List<Table> tables;

  static class PdfReaderConfig {
    final PdfFormatPlugin plugin;
    PdfReaderConfig(PdfFormatPlugin plugin) {
      this.plugin = plugin;
    }
  }

  public PdfBatchReader(PdfReaderConfig readerConfig, int maxRecords) {
    this.maxRecords = maxRecords;
    this.unregisteredColumnCount = 0;
    this.writers = new ArrayList<>();
    this.config = readerConfig;
    this.startingTableIndex = readerConfig.plugin.getConfig().defaultTableIndex() < 0 ? 0 : readerConfig.plugin.getConfig().defaultTableIndex();
    this.currentTableIndex = this.startingTableIndex;
    this.columnHeaders = new ArrayList<>();
  }

  @Override
  public boolean open(FileScanFramework.FileSchemaNegotiator negotiator) {
    this.negotiator = negotiator;

    split = negotiator.split();
    errorContext = negotiator.parentErrorContext();
    builder = new SchemaBuilder();

    openFile();
    metadataReader = new PdfMetadataReader(document);

    // Get the tables if the user set the combine pages to true
    if (config.plugin.getConfig().combinePages() ) {
      tables = PdfUtils.extractTablesFromPDF(document, config.plugin.getConfig().getAlgorithm());
      currentTable = tables.get(0);
    } else {
      currentTable = PdfUtils.getSpecificTable(document, startingTableIndex, config.plugin.getConfig().getAlgorithm());
      tables = Collections.singletonList(currentTable);

      // If the user specifies a table index, and that table does not exist, throw an exception.
      if (currentTable == null && startingTableIndex != 0) {
        throw UserException.dataReadError()
          .message("The specified table index " + startingTableIndex + " does not exist in this file. ")
          .addContext(errorContext)
          .build(logger);
      }
    }

    // Get the row iterator and grab the first row to build the schema
    rowIterator = new PdfRowIterator(currentTable);
    if (rowIterator.hasNext()) {
      firstRow = PdfUtils.convertRowToStringArray(rowIterator.next());
    }

    // Support provided schema
    TupleMetadata schema = null;
    if (negotiator.hasProvidedSchema()) {
      schema = negotiator.providedSchema();
      negotiator.tableSchema(schema, false);
    } else {
      negotiator.tableSchema(buildSchema(), false);
    }

    ResultSetLoader loader = negotiator.build();
    rowWriter = loader.writer();
    metadataReader.setRowWriter(rowWriter);
    // Build the schema
    if (negotiator.hasProvidedSchema()) {
      buildWriterListFromProvidedSchema(schema);
    } else {
      buildWriterList();
    }
    metadataReader.addImplicitColumnsToSchema();
    return true;
  }

  @Override
  public boolean next() {

    while(!rowWriter.isFull()) {
      if (rowWriter.limitReached(maxRecords)) {
        // Stop reading if the limit has been reached
        return false;
      } else if (config.plugin.getConfig().combinePages() &&
                (!rowIterator.hasNext()) &&
                  currentTableIndex < (tables.size() - 1)) {
        // Case for end of current page but more tables exist and combinePages is set to true.
        // Get the next table
        currentTableIndex++;
        currentTable = tables.get(currentTableIndex);

        // Update the row iterator
        rowIterator = new PdfRowIterator(currentTable);
        // Skip the first row in the new table because it most likely contains headers.
        if (config.plugin.getConfig().extractHeaders()) {
          rowIterator.next();
        }
      } else if (! rowIterator.hasNext()) {
        // Special case for document with no tables
        if (currentTable == null) {
          rowWriter.start();
          metadataReader.writeMetadata();
          rowWriter.save();
        }
        return false;
      }

      // Process the row
      processRow(rowIterator.next());
    }
    return true;
  }

  private void processRow(List<RectangularTextContainer> row) {
    if (row == null || row.size() == 0) {
      rowWriter.start();
      metadataReader.writeMetadata();
      rowWriter.save();
      return;
    }

    String value;
    rowWriter.start();
    int rowPosition = 0;
    for (RectangularTextContainer cellValue : row) {
      value = cellValue.getText();

      if (!Strings.isNullOrEmpty(value)) {
        writers.get(rowPosition).load(row.get(rowPosition));
      }
      rowPosition++;
    }

    metadataReader.writeMetadata();
    rowWriter.save();
  }

  @Override
  public void close() {
    if (document != null) {
      AutoCloseables.closeSilently(document.getDocument());
      AutoCloseables.closeSilently(document);
      document = null;
    }
  }

  /**
   * This method opens the PDF file and finds the tables
   */
  private void openFile() {
    try {
      InputStream fsStream = negotiator.fileSystem().openPossiblyCompressedStream(split.getPath());
      if (Strings.isNullOrEmpty(config.plugin.getConfig().password())) {
        document = PDDocument.load(fsStream);
      } else {
        // Case for encrypted files
        document = PDDocument.load(fsStream, config.plugin.getConfig().password());
      }

      AutoCloseables.closeSilently(fsStream);
    } catch (Exception e) {
      throw UserException
        .dataReadError(e)
        .addContext("Failed to open open input file: %s", split.getPath().toString())
        .addContext(errorContext)
        .build(logger);
    }
  }

  private TupleMetadata buildSchema() {
    // Get column header names
    columnHeaders = firstRow;

    // Case for file with no tables
    if (columnHeaders == null) {
      return builder.buildSchema();
    }

    // Add columns to table
    int index = 0;
    for (String columnName : firstRow) {
      if (Strings.isNullOrEmpty(columnName) || !config.plugin.getConfig().extractHeaders()) {
        columnName = NEW_FIELD_PREFIX + unregisteredColumnCount;
        columnHeaders.set(index, columnName);
        unregisteredColumnCount++;
      }
      builder.addNullable(columnName, MinorType.VARCHAR);
      index++;
    }

    return builder.buildSchema();
  }

  private void buildWriterList() {
    // Case for file with no tables.
    if (columnHeaders == null) {
      return;
    }

    for (String header : columnHeaders) {
      writers.add(new StringPdfColumnWriter(columnHeaders.indexOf(header), header, rowWriter));
    }
  }

  private void buildWriterListFromProvidedSchema(TupleMetadata schema) {
    if (schema == null) {
      buildWriterList();
      return;
    }
    int counter = 0;
    for (MaterializedField field: schema.toFieldList()) {
      String fieldName = field.getName();
      MinorType type = field.getType().getMinorType();
      columnHeaders.add(fieldName);

      switch (type) {
        case VARCHAR:
          writers.add(new StringPdfColumnWriter(counter, fieldName, rowWriter));
          break;
        case SMALLINT:
        case TINYINT:
        case INT:
          writers.add(new IntPdfColumnWriter(counter, fieldName, rowWriter));
          break;
        case BIGINT:
          writers.add(new BigIntPdfColumnWriter(counter, fieldName, rowWriter));
          break;
        case FLOAT4:
        case FLOAT8:
          writers.add(new DoublePdfColumnWriter(counter, fieldName, rowWriter));
          break;
        case DATE:
          writers.add(new DatePdfColumnWriter(counter, fieldName, rowWriter, negotiator));
          break;
        case TIME:
          writers.add(new TimePdfColumnWriter(counter, fieldName, rowWriter, negotiator));
          break;
        case TIMESTAMP:
          writers.add(new TimestampPdfColumnWriter(counter, fieldName, rowWriter, negotiator));
          break;
        default:
          throw UserException.unsupportedError()
            .message("PDF Reader with provided schema does not support " + type.name() + " data type.")
            .addContext(errorContext)
            .build(logger);
      }
    }
  }

  public abstract static class PdfColumnWriter {
    final String columnName;
    final ScalarWriter writer;
    final int columnIndex;

    public PdfColumnWriter(int columnIndex, String columnName, ScalarWriter writer) {
      this.columnIndex = columnIndex;
      this.columnName = columnName;
      this.writer = writer;
    }

    public abstract void load (RectangularTextContainer<?> cell);

    public abstract void loadFromValue(Object value);
  }

  public static class IntPdfColumnWriter extends PdfColumnWriter {
    IntPdfColumnWriter (int columnIndex, String columnName, RowSetLoader rowWriter) {
      super(columnIndex, columnName, rowWriter.scalar(columnName));
    }

    @Override
    public void load(RectangularTextContainer<?> cell) {
      writer.setInt(Integer.parseInt(cell.getText()));
    }

    @Override
    public void loadFromValue(Object value) {
      writer.setInt((Integer) value);
    }
  }

  public static class BigIntPdfColumnWriter extends PdfColumnWriter {
    BigIntPdfColumnWriter (int columnIndex, String columnName, RowSetLoader rowWriter) {
      super(columnIndex, columnName, rowWriter.scalar(columnName));
    }

    @Override
    public void load(RectangularTextContainer<?> cell) {
      writer.setLong(Long.parseLong(cell.getText()));
    }

    @Override
    public void loadFromValue(Object value) {
      writer.setLong((Long) value);
    }
  }

  public static class DoublePdfColumnWriter extends PdfColumnWriter {
    DoublePdfColumnWriter (int columnIndex, String columnName, RowSetLoader rowWriter) {
      super(columnIndex, columnName, rowWriter.scalar(columnName));
    }

    @Override
    public void load(RectangularTextContainer<?> cell) {
      writer.setDouble(Double.parseDouble(cell.getText()));
    }

    @Override
    public void loadFromValue(Object value) {
      writer.setDouble((Double) value);
    }
  }

  public static class StringPdfColumnWriter extends PdfColumnWriter {
    StringPdfColumnWriter (int columnIndex, String columnName, RowSetLoader rowWriter) {
      super(columnIndex, columnName, rowWriter.scalar(columnName));
    }

    @Override
    public void load(RectangularTextContainer<?> cell) {
      writer.setString(cell.getText());
    }

    @Override
    public void loadFromValue(Object value) {
      if (! Strings.isNullOrEmpty((String) value)) {
        writer.setString((String) value);
      }
    }
  }

  public static class DatePdfColumnWriter extends PdfColumnWriter {
    private String dateFormat;

    DatePdfColumnWriter (int columnIndex, String columnName, RowSetLoader rowWriter, FileScanFramework.FileSchemaNegotiator negotiator) {
      super(columnIndex, columnName, rowWriter.scalar(columnName));

      ColumnMetadata metadata = negotiator.providedSchema().metadata(columnName);
      if (metadata != null) {
        this.dateFormat = metadata.property("drill.format");
      }
    }

    @Override
    public void load(RectangularTextContainer<?> cell) {
      LocalDate localDate;
      if (Strings.isNullOrEmpty(this.dateFormat)) {
        localDate = LocalDate.parse(cell.getText());
      } else {
        localDate = LocalDate.parse(cell.getText(), DateTimeFormatter.ofPattern(dateFormat));
      }
      writer.setDate(localDate);
    }

    @Override
    public void loadFromValue(Object value) {
      if (value != null) {
        writer.setDate(LocalDate.parse((String) value));
      }
    }
  }

  public static class TimePdfColumnWriter extends PdfColumnWriter {
    private String dateFormat;

    TimePdfColumnWriter (int columnIndex, String columnName, RowSetLoader rowWriter, FileScanFramework.FileSchemaNegotiator negotiator) {
      super(columnIndex, columnName, rowWriter.scalar(columnName));

      ColumnMetadata metadata = negotiator.providedSchema().metadata(columnName);
      if (metadata != null) {
        this.dateFormat = metadata.property("drill.format");
      }
    }

    @Override
    public void load(RectangularTextContainer<?> cell) {
      LocalTime localTime;
      if (Strings.isNullOrEmpty(this.dateFormat)) {
        localTime = LocalTime.parse(cell.getText());
      } else {
        localTime = LocalTime.parse(cell.getText(), DateTimeFormatter.ofPattern(dateFormat));
      }
      writer.setTime(localTime);
    }

    @Override
    public void loadFromValue(Object value) {
      if (value != null) {
        writer.setTime(LocalTime.parse((String) value));
      }
    }
  }

  public static class TimestampPdfColumnWriter extends PdfColumnWriter {
    private String dateFormat;

    TimestampPdfColumnWriter(int columnIndex, String columnName, RowSetLoader rowWriter) {
      super(columnIndex, columnName, rowWriter.scalar(columnName));
    }

    TimestampPdfColumnWriter (int columnIndex, String columnName, RowSetLoader rowWriter, FileScanFramework.FileSchemaNegotiator negotiator) {
      super(columnIndex, columnName, rowWriter.scalar(columnName));

      ColumnMetadata metadata = negotiator.providedSchema().metadata(columnName);
      if (metadata != null) {
        this.dateFormat = metadata.property("drill.format");
      }
    }

    @Override
    public void load(RectangularTextContainer<?> cell) {
      Instant timestamp = null;
      if (Strings.isNullOrEmpty(this.dateFormat)) {
        timestamp = Instant.parse(cell.getText());
      } else {
        try {
          SimpleDateFormat simpleDateFormat = new SimpleDateFormat(dateFormat);
          Date parsedDate = simpleDateFormat.parse(cell.getText());
          timestamp = Instant.ofEpochMilli(parsedDate.getTime());
        } catch (ParseException e) {
          logger.error("Error parsing timestamp: " + e.getMessage());
        }
      }
      writer.setTimestamp(timestamp);
    }

    @Override
    public void loadFromValue(Object value) {
      if (value != null) {
        GregorianCalendar calendar = (GregorianCalendar) value;
        writer.setTimestamp(calendar.getTime().toInstant());
      }
    }
  }
}
