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

package org.apache.drill.exec.store.http;

import com.univocity.parsers.csv.CsvParser;
import com.univocity.parsers.csv.CsvParserSettings;
import okhttp3.HttpUrl;
import org.apache.drill.common.AutoCloseables;
import org.apache.drill.common.exceptions.ChildErrorContext;
import org.apache.drill.common.exceptions.CustomErrorContext;
import org.apache.drill.common.exceptions.UserException;
import org.apache.drill.common.types.TypeProtos;
import org.apache.drill.exec.ExecConstants;
import org.apache.drill.exec.physical.impl.scan.framework.SchemaNegotiator;
import org.apache.drill.exec.physical.resultSet.ResultSetLoader;
import org.apache.drill.exec.physical.resultSet.RowSetLoader;
import org.apache.drill.exec.record.metadata.SchemaBuilder;
import org.apache.drill.exec.record.metadata.TupleMetadata;
import org.apache.drill.exec.store.ImplicitColumnUtils.ImplicitColumns;
import org.apache.drill.exec.store.http.paginator.Paginator;
import org.apache.drill.exec.store.http.util.SimpleHttp;
import org.apache.drill.exec.vector.accessor.ScalarWriter;
import org.apache.drill.shaded.guava.com.google.common.base.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class HttpCSVBatchReader extends HttpBatchReader {
  private final HttpSubScan subScan;
  private final CsvParserSettings csvSettings;
  private final int maxRecords;
  private CsvParser csvReader;
  private List<StringColumnWriter> columnWriters;
  private String[] firstRow;
  private SchemaBuilder builder;
  private RowSetLoader rowWriter;
  private InputStream inStream;
  private ResultSetLoader resultLoader;

  private static final Logger logger = LoggerFactory.getLogger(HttpCSVBatchReader.class);

  public HttpCSVBatchReader(HttpSubScan subScan) {
    super(subScan);
    this.subScan = subScan;
    this.maxRecords = subScan.maxRecords();

    this.csvSettings = new CsvParserSettings();
    csvSettings.setLineSeparatorDetectionEnabled(true);
  }

  public HttpCSVBatchReader(HttpSubScan subScan, Paginator paginator) {
    super(subScan, paginator);
    this.subScan = subScan;
    this.maxRecords = subScan.maxRecords();

    this.csvSettings = new CsvParserSettings();
    csvSettings.setLineSeparatorDetectionEnabled(true);
  }

  @Override
  public boolean open(SchemaNegotiator negotiator) {

    // Result set loader setup
    String tempDirPath = negotiator.drillConfig().getString(ExecConstants.DRILL_TMP_DIR);

    HttpUrl url = buildUrl();

    CustomErrorContext errorContext = new ChildErrorContext(negotiator.parentErrorContext()) {
      @Override
      public void addContext(UserException.Builder builder) {
        super.addContext(builder);
        builder.addContext("URL", url.toString());
      }
    };
    negotiator.setErrorContext(errorContext);

    // Http client setup
    SimpleHttp http = SimpleHttp.builder()
      .scanDefn(subScan)
      .url(url)
      .tempDir(new File(tempDirPath))
      .proxyConfig(proxySettings(negotiator.drillConfig(), url))
      .errorContext(errorContext)
      .build();

    // CSV loader setup
    inStream = http.getInputStream();

    this.csvReader = new CsvParser(csvSettings);
    csvReader.beginParsing(inStream);

    // Build the Schema
    builder = new SchemaBuilder();
    TupleMetadata drillSchema = buildSchema();
    negotiator.tableSchema(drillSchema, true);
    resultLoader = negotiator.build();

    // Add implicit columns
    if (implicitColumnsAreProjected()) {
      implicitColumns = new ImplicitColumns(resultLoader.writer());
      buildImplicitColumns();
      populateImplicitFieldMap(http);
    }

    // Create ScalarWriters
    rowWriter = resultLoader.writer();
    populateWriterArray();

    // Close cache resources
    http.close();
    return true;
  }

  @Override
  public boolean next() {
    while (!rowWriter.isFull()) {
      if (rowWriter.limitReached(maxRecords)) {
        return false;
      }
      if (!processRow()) {
        return false;
      }
    }
    return true;
  }

  @Override
  public void close() {
    logger.debug("Closing URL: {}", baseUrl);
    AutoCloseables.closeSilently(inStream);
  }

  private TupleMetadata buildSchema() {
    firstRow = csvReader.parseNext();
    if (firstRow != null) {
      for (String value : firstRow) {
        builder.addNullable(value, TypeProtos.MinorType.VARCHAR);
      }
    }
    return builder.buildSchema();
  }

  private void populateWriterArray() {
    // Case for empty result set
    if (firstRow == null || firstRow.length == 0) {
      return;
    }

    columnWriters = new ArrayList<>();

    int colPosition = 0;
    for (String value : firstRow) {
      columnWriters.add(new StringColumnWriter(value, rowWriter, colPosition));
      colPosition++;
    }
  }

  private boolean processRow() {
    String[] nextRow = csvReader.parseNext();
    if (nextRow == null) {

      if (paginator != null &&
        maxRecords < 0 && (resultLoader.totalRowCount()) < paginator.getPageSize()) {
        paginator.notifyPartialPage();
      }
      return false;
    }
    rowWriter.start();
    for (StringColumnWriter columnWriter : columnWriters) {
      columnWriter.load(nextRow);
    }

    if (implicitColumnsAreProjected()) {
      implicitColumns.writeImplicitColumns();
    }

    rowWriter.save();
    return true;
  }

  public abstract static class ColumnWriter {

    final String colName;
    ScalarWriter columnWriter;
    int columnIndex;

    public ColumnWriter(String colName, ScalarWriter writer, int columnIndex) {
      this.colName = colName;
      this.columnWriter = writer;
      this.columnIndex = columnIndex;
    }

    public void load(String[] record) {}
  }

  public static class StringColumnWriter extends ColumnWriter {

    StringColumnWriter(String colName, RowSetLoader rowWriter, int columnIndex) {
      super(colName, rowWriter.scalar(colName), columnIndex);
    }

    @Override
    public void load(String[] record) {
      String value = record[columnIndex];
      if (Strings.isNullOrEmpty(value)) {
        columnWriter.setNull();
      } else {
        columnWriter.setString(value);
      }
    }
  }
}
