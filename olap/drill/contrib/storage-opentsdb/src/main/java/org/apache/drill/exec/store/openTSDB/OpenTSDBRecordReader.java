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
package org.apache.drill.exec.store.openTSDB;

import org.apache.drill.shaded.guava.com.google.common.collect.ImmutableList;
import org.apache.drill.shaded.guava.com.google.common.collect.ImmutableMap;
import org.apache.drill.common.exceptions.DrillRuntimeException;
import org.apache.drill.common.exceptions.UserException;
import org.apache.drill.common.expression.SchemaPath;
import org.apache.drill.common.types.TypeProtos;
import org.apache.drill.common.types.TypeProtos.MajorType;
import org.apache.drill.common.types.TypeProtos.MinorType;
import org.apache.drill.common.types.Types;
import org.apache.drill.exec.exception.SchemaChangeException;
import org.apache.drill.exec.expr.TypeHelper;
import org.apache.drill.exec.ops.OperatorContext;
import org.apache.drill.exec.physical.impl.OutputMutator;
import org.apache.drill.exec.record.MaterializedField;
import org.apache.drill.exec.store.AbstractRecordReader;
import org.apache.drill.exec.store.openTSDB.client.OpenTSDBTypes;
import org.apache.drill.exec.store.openTSDB.client.Schema;
import org.apache.drill.exec.store.openTSDB.client.Service;
import org.apache.drill.exec.store.openTSDB.dto.ColumnDTO;
import org.apache.drill.exec.store.openTSDB.dto.MetricDTO;
import org.apache.drill.exec.vector.NullableFloat8Vector;
import org.apache.drill.exec.vector.NullableTimeStampVector;
import org.apache.drill.exec.vector.NullableVarCharVector;
import org.apache.drill.exec.vector.ValueVector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.drill.exec.store.openTSDB.Constants.METRIC_PARAM;
import static org.apache.drill.exec.store.openTSDB.Util.fromRowData;

public class OpenTSDBRecordReader extends AbstractRecordReader {

  private static final Logger logger = LoggerFactory.getLogger(OpenTSDBRecordReader.class);

  // batch size should not exceed max allowed record count
  private static final int TARGET_RECORD_COUNT = 4000;

  private static final Map<OpenTSDBTypes, MinorType> TYPES;

  private Service db;

  private Iterator<MetricDTO> tableIterator;
  private OutputMutator output;
  private ImmutableList<ProjectedColumnInfo> projectedCols;

  private Map<String, String> params;

  public OpenTSDBRecordReader(Service client, OpenTSDBSubScan.OpenTSDBSubScanSpec subScanSpec,
                       List<SchemaPath> projectedColumns) {
    setColumns(projectedColumns);
    this.db = client;
    this.params =
            fromRowData(subScanSpec.getTableName());
    logger.debug("Scan spec: {}", subScanSpec);
  }

  @Override
  public void setup(OperatorContext context, OutputMutator output) {
    this.output = output;
    Set<MetricDTO> metrics =
            db.getAllMetrics(params);
    if (metrics == null) {
      throw UserException.validationError()
              .message(String.format("Table '%s' not found", params.get(METRIC_PARAM)))
              .build(logger);
    }
    this.tableIterator = metrics.iterator();
  }

  @Override
  public int next() {
    try {
      return processOpenTSDBTablesData();
    } catch (SchemaChangeException e) {
      throw new DrillRuntimeException(e);
    }
  }

  @Override
  public void close() {
  }

  static {
    TYPES = ImmutableMap.<OpenTSDBTypes, MinorType>builder()
        .put(OpenTSDBTypes.STRING, MinorType.VARCHAR)
        .put(OpenTSDBTypes.DOUBLE, MinorType.FLOAT8)
        .put(OpenTSDBTypes.TIMESTAMP, MinorType.TIMESTAMP)
        .build();
  }

  private static class ProjectedColumnInfo {
    ValueVector vv;
    ColumnDTO openTSDBColumn;
  }

  private int processOpenTSDBTablesData() throws SchemaChangeException {
    int rowCounter = 0;
    while (tableIterator.hasNext() && rowCounter < TARGET_RECORD_COUNT) {
      MetricDTO metricDTO = tableIterator.next();
      rowCounter = addRowResult(metricDTO, rowCounter);
    }
    return rowCounter;
  }

  private int addRowResult(MetricDTO table, int rowCounter) throws SchemaChangeException {
    setupProjectedColsIfItNull();
    for (String time : table.getDps().keySet()) {
      String value = table.getDps().get(time);
      setupDataToDrillTable(table, time, value, table.getTags(), rowCounter);
      rowCounter++;
    }
    return rowCounter;
  }

  private void setupProjectedColsIfItNull() throws SchemaChangeException {
    if (projectedCols == null) {
      initCols(new Schema(db, params.get(METRIC_PARAM)));
    }
  }

  private void setupDataToDrillTable(MetricDTO table, String timestamp, String value, Map<String, String> tags, int rowCount) {
    for (ProjectedColumnInfo pci : projectedCols) {
      switch (pci.openTSDBColumn.getColumnName()) {
        case "metric":
          setStringColumnValue(table.getMetric(), pci, rowCount);
          break;
        case "aggregate tags":
          setStringColumnValue(table.getAggregateTags().toString(), pci, rowCount);
          break;
        case "timestamp":
          setTimestampColumnValue(timestamp, pci, rowCount);
          break;
        case "aggregated value":
          setDoubleColumnValue(value, pci, rowCount);
          break;
        default:
          setStringColumnValue(tags.get(pci.openTSDBColumn.getColumnName()), pci, rowCount);
      }
    }
  }

  private void setTimestampColumnValue(String timestamp, ProjectedColumnInfo pci, int rowCount) {
    setTimestampColumnValue(timestamp != null ? Long.parseLong(timestamp) : Long.parseLong("0"), pci, rowCount);
  }

  private void setDoubleColumnValue(String value, ProjectedColumnInfo pci, int rowCount) {
    setDoubleColumnValue(value != null ? Double.parseDouble(value) : 0.0, pci, rowCount);
  }

  private void setStringColumnValue(String data, ProjectedColumnInfo pci, int rowCount) {
    if (data == null) {
      data = "null";
    }
    ByteBuffer value = ByteBuffer.wrap(data.getBytes(UTF_8));
    ((NullableVarCharVector.Mutator) pci.vv.getMutator())
        .setSafe(rowCount, value, 0, value.remaining());
  }

  private void setTimestampColumnValue(Long data, ProjectedColumnInfo pci, int rowCount) {
    ((NullableTimeStampVector.Mutator) pci.vv.getMutator())
        .setSafe(rowCount, data * 1000);
  }

  private void setDoubleColumnValue(Double data, ProjectedColumnInfo pci, int rowCount) {
    ((NullableFloat8Vector.Mutator) pci.vv.getMutator())
        .setSafe(rowCount, data);
  }

  private void initCols(Schema schema) throws SchemaChangeException {
    ImmutableList.Builder<ProjectedColumnInfo> pciBuilder = ImmutableList.builder();

    for (int i = 0; i < schema.getColumnCount(); i++) {

      ColumnDTO column = schema.getColumnByIndex(i);
      final String name = column.getColumnName();
      final OpenTSDBTypes type = column.getColumnType();
      TypeProtos.MinorType minorType = TYPES.get(type);

      if (isMinorTypeNull(minorType)) {
        String message = String.format(
                "A column you queried has a data type that is not currently supported by the OpenTSDB storage plugin. "
                        + "The column's name was %s and its OpenTSDB data type was %s. ", name, type.toString());
        throw UserException.unsupportedError()
                .message(message)
                .build(logger);
      }

      ProjectedColumnInfo pci = getProjectedColumnInfo(column, name, minorType);
      pciBuilder.add(pci);
    }
    projectedCols = pciBuilder.build();
  }

  private boolean isMinorTypeNull(MinorType minorType) {
    return minorType == null;
  }

  private ProjectedColumnInfo getProjectedColumnInfo(ColumnDTO column, String name, MinorType minorType) throws SchemaChangeException {
    MajorType majorType = getMajorType(minorType);

    MaterializedField field =
        MaterializedField.create(name, majorType);

    ValueVector vector =
        getValueVector(minorType, majorType, field);

    return getProjectedColumnInfo(column, vector);
  }

  private MajorType getMajorType(MinorType minorType) {
    MajorType majorType;
    majorType = Types.optional(minorType);
    return majorType;
  }

  private ValueVector getValueVector(MinorType minorType, MajorType majorType, MaterializedField field) throws SchemaChangeException {
    final Class<? extends ValueVector> clazz = TypeHelper.getValueVectorClass(
        minorType, majorType.getMode());
    ValueVector vector = output.addField(field, clazz);
    vector.allocateNew();
    return vector;
  }

  private ProjectedColumnInfo getProjectedColumnInfo(ColumnDTO column, ValueVector vector) {
    ProjectedColumnInfo pci = new ProjectedColumnInfo();
    pci.vv = vector;
    pci.openTSDBColumn = column;
    return pci;
  }
}
