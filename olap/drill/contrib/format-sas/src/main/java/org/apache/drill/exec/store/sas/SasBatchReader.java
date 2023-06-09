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

package org.apache.drill.exec.store.sas;

import com.epam.parso.Column;
import com.epam.parso.ColumnFormat;
import com.epam.parso.SasFileProperties;
import com.epam.parso.SasFileReader;
import com.epam.parso.impl.DateTimeConstants;
import com.epam.parso.impl.SasFileReaderImpl;
import org.apache.drill.common.AutoCloseables;
import org.apache.drill.common.exceptions.CustomErrorContext;
import org.apache.drill.common.exceptions.UserException;
import org.apache.drill.common.types.TypeProtos.DataMode;
import org.apache.drill.common.types.TypeProtos.MinorType;
import org.apache.drill.exec.physical.impl.scan.file.FileScanFramework;
import org.apache.drill.exec.physical.impl.scan.framework.ManagedReader;
import org.apache.drill.exec.physical.resultSet.ResultSetLoader;
import org.apache.drill.exec.physical.resultSet.RowSetLoader;
import org.apache.drill.exec.record.MaterializedField;
import org.apache.drill.exec.record.metadata.ColumnMetadata;
import org.apache.drill.exec.record.metadata.MetadataUtils;
import org.apache.drill.exec.record.metadata.SchemaBuilder;
import org.apache.drill.exec.record.metadata.TupleMetadata;
import org.apache.drill.exec.vector.accessor.ScalarWriter;
import org.apache.hadoop.mapred.FileSplit;
import org.apache.parquet.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class SasBatchReader implements ManagedReader<FileScanFramework.FileSchemaNegotiator> {
  private static final Logger logger = LoggerFactory.getLogger(SasBatchReader.class);
  private final int maxRecords;
  private final List<SasColumnWriter> writerList;
  private FileSplit split;
  private InputStream fsStream;
  private SasFileReader sasFileReader;
  private CustomErrorContext errorContext;
  private RowSetLoader rowWriter;
  private Object[] firstRow;


  private String compressionMethod;
  private String fileLabel;
  private String fileType;
  private String osName;
  private String osType;
  private String sasRelease;
  private String sessionEncoding;
  private String serverType;
  private LocalDate dateCreated;
  private LocalDate dateModified;

  private enum IMPLICIT_STRING_COLUMN {
    COMPRESSION_METHOD("_compression_method"),
    ENCODING("_encoding"),
    FILE_LABEL("_file_label"),
    FILE_TYPE("_file_type"),
    OS_NAME("_os_name"),
    OS_TYPE("_os_type"),
    SAS_RELEASE("_sas_release"),
    SESSION_ENCODING("_session_encoding");

    private final String fieldName;

    IMPLICIT_STRING_COLUMN(String fieldName) {
      this.fieldName = fieldName;
    }

    public String getFieldName() {
      return fieldName;
    }
  }

  private enum IMPLICIT_DATE_COLUMN {
    CREATED_DATE("_date_created"),
    MODIFIED_DATE("_date_modified");

    private final String fieldName;

    IMPLICIT_DATE_COLUMN(String fieldName) {
      this.fieldName = fieldName;
    }

    public String getFieldName() {
      return fieldName;
    }
  }

  public static class SasReaderConfig {
    protected final SasFormatPlugin plugin;
    public SasReaderConfig(SasFormatPlugin plugin) {
      this.plugin = plugin;
    }
  }

  public SasBatchReader(int maxRecords) {
    this.maxRecords = maxRecords;
    writerList = new ArrayList<>();
  }

  @Override
  public boolean open(FileScanFramework.FileSchemaNegotiator negotiator) {
    split = negotiator.split();
    errorContext = negotiator.parentErrorContext();
    openFile(negotiator);

    TupleMetadata schema;
    if (negotiator.hasProvidedSchema()) {
      schema = negotiator.providedSchema();
    } else {
      schema = buildSchema();
    }
    schema = addImplicitColumnsToSchema(schema);
    negotiator.tableSchema(schema, true);

    ResultSetLoader loader = negotiator.build();
    rowWriter = loader.writer();
    buildWriterList(schema);

    return true;
  }

  private void openFile(FileScanFramework.FileSchemaNegotiator negotiator) {
    try {
      fsStream = negotiator.fileSystem().openPossiblyCompressedStream(split.getPath());
      sasFileReader = new SasFileReaderImpl(fsStream);
      firstRow = sasFileReader.readNext();
    } catch (IOException e) {
      throw UserException
        .dataReadError(e)
        .message("Unable to open SAS File %s", split.getPath())
        .addContext(e.getMessage())
        .addContext(errorContext)
        .build(logger);
    }
  }

  private TupleMetadata buildSchema() {
    SchemaBuilder builder = new SchemaBuilder();
    List<Column> columns = sasFileReader.getColumns();
    for (Column column : columns) {
      String columnName = column.getName();
      String columnType = column.getType().getSimpleName();
      ColumnFormat columnFormat = column.getFormat();
      try {
        MinorType type = null;
        if (DateTimeConstants.TIME_FORMAT_STRINGS.contains(columnFormat.getName())) {
          type = MinorType.TIME;
        } else if (DateTimeConstants.DATE_FORMAT_STRINGS.containsKey(columnFormat.getName())) {
          type = MinorType.DATE;
        } else if (DateTimeConstants.DATETIME_FORMAT_STRINGS.containsKey(columnFormat.getName())) {
          type = MinorType.TIMESTAMP;
        } else {
          type = getType(columnType);
        }
        builder.addNullable(columnName, type);
      } catch (Exception e) {
        throw UserException.dataReadError()
          .message("Error with type of column " + columnName + "; Type: " + columnType)
          .addContext(errorContext)
          .build(logger);
      }
    }

    return builder.buildSchema();
  }

  private void buildWriterList(TupleMetadata schema) {
    int colIndex = 0;
    for (MaterializedField field : schema.toFieldList()) {
      String fieldName = field.getName();
      MinorType type = field.getType().getMinorType();
      if (type == MinorType.FLOAT8) {
        writerList.add(new DoubleSasColumnWriter(colIndex, fieldName, rowWriter));
      } else if (type == MinorType.DATE) {
        writerList.add(new DateSasColumnWriter(colIndex, fieldName, rowWriter));
      } else if (type == MinorType.TIME) {
        writerList.add(new TimeSasColumnWriter(colIndex, fieldName, rowWriter));
      } else if (type == MinorType.VARCHAR) {
        writerList.add(new StringSasColumnWriter(colIndex, fieldName, rowWriter));
      } else if (type == MinorType.TIMESTAMP) {
        writerList.add(new TimestampSasColumnWriter(colIndex, fieldName, rowWriter));
      } else {
        throw UserException.dataReadError()
          .message(fieldName + " is an unparsable data type: " + type.name() + ".  The SAS reader does not support this data type.")
          .addContext(errorContext)
          .build(logger);
      }
      colIndex++;
    }
  }

  private MinorType getType(String simpleType) {
    switch (simpleType) {
      case "String":
        return MinorType.VARCHAR;
      case "Double":
      case "Number":
      case "Numeric":
      case "Long":
        return MinorType.FLOAT8;
      case "Date":
        return MinorType.DATE;
      default:
        throw UserException.dataReadError()
          .message("SAS Reader does not support data type: " + simpleType)
          .addContext(errorContext)
          .build(logger);
    }
  }

  private TupleMetadata addImplicitColumnsToSchema(TupleMetadata schema) {
    SchemaBuilder builder = new SchemaBuilder();
    ColumnMetadata colSchema;
    builder.addAll(schema);
    SasFileProperties fileProperties = sasFileReader.getSasFileProperties();

    // Add String Metadata columns
    for (IMPLICIT_STRING_COLUMN name : IMPLICIT_STRING_COLUMN.values()) {
      colSchema = MetadataUtils.newScalar(name.getFieldName(), MinorType.VARCHAR, DataMode.OPTIONAL);
      colSchema.setBooleanProperty(ColumnMetadata.EXCLUDE_FROM_WILDCARD, true);
      builder.add(colSchema);
    }

    // Add Date Column Names
    for (IMPLICIT_DATE_COLUMN name : IMPLICIT_DATE_COLUMN.values()) {
      colSchema = MetadataUtils.newScalar(name.getFieldName(), MinorType.DATE, DataMode.OPTIONAL);
      colSchema.setBooleanProperty(ColumnMetadata.EXCLUDE_FROM_WILDCARD, true);
      builder.add(colSchema);
    }

    populateMetadata(fileProperties);
    return builder.build();
  }

  @Override
  public boolean next() {
    while (!rowWriter.isFull()) {
      if (!processNextRow()) {
        return false;
      }
    }
    return true;
  }

  @Override
  public void close() {
    AutoCloseables.closeSilently(fsStream);
  }

  private boolean processNextRow() {
    if (rowWriter.limitReached(maxRecords)) {
      return false;
    }
    Object[] row;
    try {
      // Process first row
      if (firstRow != null) {
        row = firstRow;
        firstRow = null;
      } else {
        row = sasFileReader.readNext();
      }

      if (row == null) {
        return false;
      }

      rowWriter.start();
      for (int i = 0; i < row.length; i++) {
        writerList.get(i).load(row);
      }

      // Write Metadata
      writeMetadata(row.length);
      rowWriter.save();
    } catch (IOException e) {
      throw UserException.dataReadError()
        .message("Error reading SAS file: " + e.getMessage())
        .addContext(errorContext)
        .build(logger);
    }
    return true;
  }

  private void populateMetadata(SasFileProperties fileProperties) {
    compressionMethod = fileProperties.getCompressionMethod();
    fileLabel = fileProperties.getFileLabel();
    fileType = fileProperties.getFileType();
    osName = fileProperties.getOsName();
    osType = fileProperties.getOsType();
    sasRelease = fileProperties.getSasRelease();
    sessionEncoding = fileProperties.getSessionEncoding();
    serverType = fileProperties.getServerType();
    dateCreated = convertDateToLocalDate(fileProperties.getDateCreated());
    dateModified = convertDateToLocalDate(fileProperties.getDateCreated());
  }

  private void writeMetadata(int startIndex) {
    ((StringSasColumnWriter)writerList.get(startIndex)).load(compressionMethod);
    ((StringSasColumnWriter)writerList.get(startIndex+1)).load(fileLabel);
    ((StringSasColumnWriter)writerList.get(startIndex+2)).load(fileType);
    ((StringSasColumnWriter)writerList.get(startIndex+3)).load(osName);
    ((StringSasColumnWriter)writerList.get(startIndex+4)).load(osType);
    ((StringSasColumnWriter)writerList.get(startIndex+5)).load(sasRelease);
    ((StringSasColumnWriter)writerList.get(startIndex+6)).load(sessionEncoding);
    ((StringSasColumnWriter)writerList.get(startIndex+7)).load(serverType);

    ((DateSasColumnWriter)writerList.get(startIndex+8)).load(dateCreated);
    ((DateSasColumnWriter)writerList.get(startIndex+9)).load(dateModified);
  }

  private static LocalDate convertDateToLocalDate(Date date) {
    return Instant.ofEpochMilli(date.toInstant().toEpochMilli())
      .atZone(ZoneOffset.ofHours(0))
      .toLocalDate();
  }

  public abstract static class SasColumnWriter {
    final String columnName;
    final ScalarWriter writer;
    final int columnIndex;

    public SasColumnWriter(int columnIndex, String columnName, ScalarWriter writer) {
      this.columnIndex = columnIndex;
      this.columnName = columnName;
      this.writer = writer;
    }

    public abstract void load (Object[] row);
  }

  public static class StringSasColumnWriter extends SasColumnWriter {

    StringSasColumnWriter (int columnIndex, String columnName, RowSetLoader rowWriter) {
      super(columnIndex, columnName, rowWriter.scalar(columnName));
    }

    @Override
    public void load(Object[] row) {
      if (row[columnIndex] != null) {
        writer.setString(row[columnIndex].toString());
      }
    }

    public void load (String value) {
      if (!Strings.isNullOrEmpty(value)) {
        writer.setString(value);
      }
    }
  }

  public static class DateSasColumnWriter extends SasColumnWriter {

    DateSasColumnWriter (int columnIndex, String columnName, RowSetLoader rowWriter) {
      super(columnIndex, columnName, rowWriter.scalar(columnName));
    }

    @Override
    public void load(Object[] row) {
      if (row[columnIndex] != null) {
        LocalDate value = convertDateToLocalDate((Date)row[columnIndex]);
        writer.setDate(value);
      }
    }

    public void load(LocalDate date) {
      writer.setDate(date);
    }
  }

  public static class TimeSasColumnWriter extends SasColumnWriter {

    TimeSasColumnWriter(int columnIndex, String columnName, RowSetLoader rowWriter) {
      super(columnIndex, columnName, rowWriter.scalar(columnName));
    }

    @Override
    public void load(Object[] row) {
      int seconds = ((Long) row[columnIndex]).intValue();
      LocalTime value = LocalTime.parse(formatSeconds(seconds));
      writer.setTime(value);
    }

    private String formatSeconds(int timeInSeconds)
    {
      int hours = timeInSeconds / 3600;
      int secondsLeft = timeInSeconds - hours * 3600;
      int minutes = secondsLeft / 60;
      int seconds = secondsLeft - minutes * 60;

      StringBuilder formattedTime = new StringBuilder();
      if (hours < 10) {
        formattedTime.append("0");
      }
      formattedTime.append(hours).append(":");

      if (minutes < 10) {
        formattedTime.append("0");
      }
      formattedTime.append(minutes).append(":");

      if (seconds < 10) {
        formattedTime.append("0");
      }
      formattedTime.append(seconds);
      return formattedTime.toString();
    }
  }

  public static class DoubleSasColumnWriter extends SasColumnWriter {

    DoubleSasColumnWriter (int columnIndex, String columnName, RowSetLoader rowWriter) {
      super(columnIndex, columnName, rowWriter.scalar(columnName));
    }

    @Override
    public void load(Object[] row) {
      if (row[columnIndex] != null) {
        if (row[columnIndex] instanceof Number) {
          writer.setDouble(((Number) row[columnIndex]).doubleValue());
        }
      }
    }
  }

  public static class TimestampSasColumnWriter extends SasColumnWriter {

    TimestampSasColumnWriter(int columnIndex, String columnName, RowSetLoader rowWriter) {
      super(columnIndex, columnName, rowWriter.scalar(columnName));
    }

    @Override
    public void load(Object[] row) {
      if (row[columnIndex] != null) {
        writer.setTimestamp(((Date) row[columnIndex]).toInstant());
      }
    }
  }
}
