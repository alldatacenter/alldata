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

package org.apache.drill.exec.store.spss;

import com.bedatadriven.spss.SpssDataFileReader;
import com.bedatadriven.spss.SpssVariable;
import org.apache.drill.common.AutoCloseables;
import org.apache.drill.common.exceptions.CustomErrorContext;
import org.apache.drill.common.exceptions.UserException;
import org.apache.drill.common.types.TypeProtos;
import org.apache.drill.exec.physical.impl.scan.file.FileScanFramework.FileSchemaNegotiator;
import org.apache.drill.exec.physical.impl.scan.framework.ManagedReader;
import org.apache.drill.exec.physical.resultSet.ResultSetLoader;
import org.apache.drill.exec.physical.resultSet.RowSetLoader;
import org.apache.drill.exec.record.metadata.SchemaBuilder;
import org.apache.drill.exec.record.metadata.TupleMetadata;
import org.apache.drill.exec.vector.accessor.ScalarWriter;
import org.apache.hadoop.mapred.FileSplit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SpssBatchReader implements ManagedReader<FileSchemaNegotiator> {

  private static final Logger logger = LoggerFactory.getLogger(SpssBatchReader.class);

  private static final String VALUE_LABEL = "_value";

  private final int maxRecords;

  private FileSplit split;

  private InputStream fsStream;

  private SpssDataFileReader spssReader;

  private RowSetLoader rowWriter;

  private List<SpssVariable> variableList;

  private List<SpssColumnWriter> writerList;

  private CustomErrorContext errorContext;


  public static class SpssReaderConfig {

    protected final SpssFormatPlugin plugin;

    public SpssReaderConfig(SpssFormatPlugin plugin) {
      this.plugin = plugin;
    }
  }

  public SpssBatchReader(int maxRecords) {
    this.maxRecords = maxRecords;
  }

  @Override
  public boolean open(FileSchemaNegotiator negotiator) {
    split = negotiator.split();
    openFile(negotiator);
    negotiator.tableSchema(buildSchema(), true);
    errorContext = negotiator.parentErrorContext();
    ResultSetLoader loader = negotiator.build();
    rowWriter = loader.writer();
    buildReaderList();

    return true;
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
    if (fsStream != null) {
      AutoCloseables.closeSilently(fsStream);
      fsStream = null;
    }
  }

  private void openFile(FileSchemaNegotiator negotiator) {
    try {
      fsStream = negotiator.fileSystem().openPossiblyCompressedStream(split.getPath());
      spssReader = new SpssDataFileReader(fsStream);
    } catch (IOException e) {
      throw UserException
        .dataReadError(e)
        .message("Unable to open SPSS File %s", split.getPath())
        .addContext(e.getMessage())
        .addContext(errorContext)
        .build(logger);
    }
  }

  private boolean processNextRow() {
    // Check to see if the limit has been reached
    if (rowWriter.limitReached(maxRecords)) {
      return false;
    }

    try {
      // Stop reading when you run out of data
      if (!spssReader.readNextCase()) {
        return false;
      }

      rowWriter.start();
      for (SpssColumnWriter spssColumnWriter : writerList) {
        spssColumnWriter.load(spssReader);
      }
      rowWriter.save();

    } catch (IOException e) {
      throw UserException
        .dataReadError(e)
        .message("Error reading SPSS File.")
        .addContext(errorContext)
        .build(logger);
    }
    return true;
  }

  private TupleMetadata buildSchema() {
    SchemaBuilder builder = new SchemaBuilder();
    variableList = spssReader.getVariables();

    for (SpssVariable variable : variableList) {
      String varName = variable.getVariableName();

      if (variable.isNumeric()) {
        builder.addNullable(varName, TypeProtos.MinorType.FLOAT8);

        // Check if the column has lookups associated with it
        if (variable.getValueLabels() != null && variable.getValueLabels().size() > 0) {
          builder.addNullable(varName + VALUE_LABEL, TypeProtos.MinorType.VARCHAR);
        }

      } else {
        builder.addNullable(varName, TypeProtos.MinorType.VARCHAR);
      }
    }
    return builder.buildSchema();
  }

  private void buildReaderList() {
    writerList = new ArrayList<>();

    for (SpssVariable variable : variableList) {
      if (variable.isNumeric()) {
        writerList.add(new NumericSpssColumnWriter(variable.getIndex(), variable.getVariableName(), rowWriter, spssReader));
      } else {
        writerList.add(new StringSpssColumnWriter(variable.getIndex(), variable.getVariableName(), rowWriter));
      }
    }
  }

  public abstract static class SpssColumnWriter {
    final String columnName;
    final ScalarWriter writer;
    final int columnIndex;

    public SpssColumnWriter(int columnIndex, String columnName, ScalarWriter writer) {
      this.columnIndex = columnIndex;
      this.columnName = columnName;
      this.writer = writer;
    }

    public abstract void load (SpssDataFileReader reader);
  }

  public static class StringSpssColumnWriter extends SpssColumnWriter {

    StringSpssColumnWriter (int columnIndex, String columnName, RowSetLoader rowWriter) {
      super(columnIndex, columnName, rowWriter.scalar(columnName));
    }

    @Override
    public void load(SpssDataFileReader reader) {
      writer.setString(reader.getStringValue(columnIndex));
    }
  }

  public static class NumericSpssColumnWriter extends SpssColumnWriter {

    ScalarWriter labelWriter;
    Map<Double, String> labels;

    NumericSpssColumnWriter(int columnIndex, String columnName, RowSetLoader rowWriter, SpssDataFileReader reader) {
      super(columnIndex, columnName, rowWriter.scalar(columnName));

      if (reader.getValueLabels(columnName) != null && reader.getValueLabels(columnName).size() != 0) {
        labelWriter = rowWriter.scalar(columnName + VALUE_LABEL);
        labels = reader.getValueLabels(columnIndex);
      }
    }

    @Override
    public void load(SpssDataFileReader reader) {
      double value = reader.getDoubleValue(columnIndex);

      if (labelWriter != null) {
        String labelValue = labels.get(value);
        if (labelValue == null) {
          labelWriter.setNull();
        } else {
          labelWriter.setString(labelValue);
        }
      }
      writer.setDouble(value);
    }
  }
}
