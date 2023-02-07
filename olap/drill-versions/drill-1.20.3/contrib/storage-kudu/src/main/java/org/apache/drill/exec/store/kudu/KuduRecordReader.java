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
package org.apache.drill.exec.store.kudu;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;

import org.apache.drill.common.exceptions.ExecutionSetupException;
import org.apache.drill.common.exceptions.UserException;
import org.apache.drill.common.expression.SchemaPath;
import org.apache.drill.common.types.TypeProtos.MajorType;
import org.apache.drill.common.types.TypeProtos.MinorType;
import org.apache.drill.common.types.Types;
import org.apache.drill.exec.exception.SchemaChangeException;
import org.apache.drill.exec.expr.TypeHelper;
import org.apache.drill.exec.ops.OperatorContext;
import org.apache.drill.exec.physical.impl.OutputMutator;
import org.apache.drill.exec.record.MaterializedField;
import org.apache.drill.exec.store.AbstractRecordReader;
import org.apache.drill.exec.store.kudu.KuduSubScan.KuduSubScanSpec;
import org.apache.drill.exec.vector.BigIntVector;
import org.apache.drill.exec.vector.BitVector;
import org.apache.drill.exec.vector.Float4Vector;
import org.apache.drill.exec.vector.Float8Vector;
import org.apache.drill.exec.vector.IntVector;
import org.apache.drill.exec.vector.NullableBigIntVector;
import org.apache.drill.exec.vector.NullableBitVector;
import org.apache.drill.exec.vector.NullableFloat4Vector;
import org.apache.drill.exec.vector.NullableFloat8Vector;
import org.apache.drill.exec.vector.NullableIntVector;
import org.apache.drill.exec.vector.NullableTimeStampVector;
import org.apache.drill.exec.vector.NullableVarBinaryVector;
import org.apache.drill.exec.vector.NullableVarCharVector;
import org.apache.drill.exec.vector.TimeStampVector;
import org.apache.drill.exec.vector.ValueVector;
import org.apache.drill.exec.vector.VarBinaryVector;
import org.apache.drill.exec.vector.VarCharVector;
import org.apache.kudu.ColumnSchema;
import org.apache.kudu.Schema;
import org.apache.kudu.Type;
import org.apache.kudu.client.KuduClient;
import org.apache.kudu.client.KuduScanner;
import org.apache.kudu.client.KuduScanner.KuduScannerBuilder;
import org.apache.kudu.client.KuduTable;
import org.apache.kudu.client.RowResult;
import org.apache.kudu.client.RowResultIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.drill.shaded.guava.com.google.common.collect.ImmutableList;
import org.apache.drill.shaded.guava.com.google.common.collect.ImmutableMap;
import org.apache.drill.shaded.guava.com.google.common.collect.Lists;

public class KuduRecordReader extends AbstractRecordReader {
  private static final Logger logger = LoggerFactory.getLogger(KuduRecordReader.class);

  private static final int TARGET_RECORD_COUNT = 4000;

  private final KuduClient client;
  private final KuduSubScanSpec scanSpec;
  private KuduScanner scanner;
  private RowResultIterator iterator;

  private OutputMutator output;
  private OperatorContext context;

  private String lastColumnName;
  private Type lastColumnType;

  private static class ProjectedColumnInfo {
    int index;
    ValueVector vv;
    ColumnSchema kuduColumn;
  }

  private ImmutableList<ProjectedColumnInfo> projectedCols;

  public KuduRecordReader(KuduClient client, KuduSubScan.KuduSubScanSpec subScanSpec, List<SchemaPath> projectedColumns) {
    setColumns(projectedColumns);
    this.client = client;
    scanSpec = subScanSpec;
    logger.debug("Scan spec: {}", subScanSpec);
  }

  @Override
  public void setup(OperatorContext context, OutputMutator output) throws ExecutionSetupException {
    this.output = output;
    this.context = context;
    try {
      KuduTable table = client.openTable(scanSpec.getTableName());

      KuduScannerBuilder builder = client.newScannerBuilder(table);
      if (!isStarQuery()) {
        List<String> colNames = Lists.newArrayList();
        for (SchemaPath p : this.getColumns()) {
          colNames.add(p.getRootSegmentPath());
        }
        builder.setProjectedColumnNames(colNames);
      }

      context.getStats().startWait();
      try {
        scanner = builder
            .lowerBoundRaw(scanSpec.getStartKey())
            .exclusiveUpperBoundRaw(scanSpec.getEndKey())
            .build();
      } finally {
        context.getStats().stopWait();
      }
    } catch (Exception e) {
      throw new ExecutionSetupException(e);
    }
  }

  static final Map<Type, MinorType> TYPES;

  static {
    TYPES = ImmutableMap.<Type, MinorType> builder()
        .put(Type.BINARY, MinorType.VARBINARY)
        .put(Type.BOOL, MinorType.BIT)
        .put(Type.DOUBLE, MinorType.FLOAT8)
        .put(Type.FLOAT, MinorType.FLOAT4)
        .put(Type.INT8, MinorType.INT)
        .put(Type.INT16, MinorType.INT)
        .put(Type.INT32, MinorType.INT)
        .put(Type.INT64, MinorType.BIGINT)
        .put(Type.STRING, MinorType.VARCHAR)
        .put(Type.UNIXTIME_MICROS, MinorType.TIMESTAMP)
        .build();
  }

  @Override
  public int next() {
    int rowCount = 0;
    try {
      while (iterator == null || !iterator.hasNext()) {
        if (!scanner.hasMoreRows()) {
          iterator = null;
          return 0;
        }
        context.getStats().startWait();
        try {
          iterator = scanner.nextRows();
        } finally {
          context.getStats().stopWait();
        }
      }
      for (; rowCount < TARGET_RECORD_COUNT && iterator.hasNext(); rowCount++) {
        addRowResult(iterator.next(), rowCount);
      }
    } catch (Exception ex) {
      throw new RuntimeException(ex);
    }
    for (ProjectedColumnInfo pci : projectedCols) {
      pci.vv.getMutator().setValueCount(rowCount);
    }
    return rowCount;
  }

  private void initCols(Schema schema) throws SchemaChangeException {
    ImmutableList.Builder<ProjectedColumnInfo> pciBuilder = ImmutableList.builder();

    for (int i = 0; i < schema.getColumnCount(); i++) {
      ColumnSchema col = schema.getColumnByIndex(i);

      final String name = col.getName();
      final Type kuduType = col.getType();
      lastColumnName = name;
      lastColumnType = kuduType;
      MinorType minorType = TYPES.get(kuduType);
      if (minorType == null) {
        logger.warn("Ignoring column that is unsupported.", UserException
            .unsupportedError()
            .message(
                "A column you queried has a data type that is not currently supported by the Kudu storage plugin. "
                    + "The column's name was %s and its Kudu data type was %s. ",
                name, kuduType.toString())
            .addContext("column Name", name)
            .addContext("plugin", "kudu")
            .build(logger));

        continue;
      }
      MajorType majorType;
      if (col.isNullable()) {
        majorType = Types.optional(minorType);
      } else {
        majorType = Types.required(minorType);
      }
      MaterializedField field = MaterializedField.create(name, majorType);
      final Class<? extends ValueVector> clazz = TypeHelper.getValueVectorClass(
          minorType, majorType.getMode());
      ValueVector vector = output.addField(field, clazz);
      vector.allocateNew();

      ProjectedColumnInfo pci = new ProjectedColumnInfo();
      pci.vv = vector;
      pci.kuduColumn = col;
      pci.index = i;
      pciBuilder.add(pci);
    }

    projectedCols = pciBuilder.build();
  }

  private void addRowResult(RowResult result, int rowIndex) throws SchemaChangeException {
    if (projectedCols == null) {
      initCols(result.getColumnProjection());
    }

    for (ProjectedColumnInfo pci : projectedCols) {
      if (result.isNull(pci.index)) {
        continue;
      }
      switch (pci.kuduColumn.getType()) {
      case BINARY: {
        ByteBuffer value = result.getBinary(pci.index);
        if (pci.kuduColumn.isNullable()) {
          ((NullableVarBinaryVector.Mutator) pci.vv.getMutator())
              .setSafe(rowIndex, value, 0, value.remaining());
        } else {
          ((VarBinaryVector.Mutator) pci.vv.getMutator())
              .setSafe(rowIndex, value, 0, value.remaining());
        }
        break;
      }
      case STRING: {
        ByteBuffer value = ByteBuffer.wrap(result.getString(pci.index).getBytes());
        if (pci.kuduColumn.isNullable()) {
          ((NullableVarCharVector.Mutator) pci.vv.getMutator())
              .setSafe(rowIndex, value, 0, value.remaining());
        } else {
          ((VarCharVector.Mutator) pci.vv.getMutator())
              .setSafe(rowIndex, value, 0, value.remaining());
        }
        break;
      }
      case BOOL:
        if (pci.kuduColumn.isNullable()) {
          ((NullableBitVector.Mutator) pci.vv.getMutator())
              .setSafe(rowIndex, result.getBoolean(pci.index) ? 1 : 0);
        } else {
          ((BitVector.Mutator) pci.vv.getMutator())
              .setSafe(rowIndex, result.getBoolean(pci.index) ? 1 : 0);
        }
        break;
      case DOUBLE:
        if (pci.kuduColumn.isNullable()) {
          ((NullableFloat8Vector.Mutator) pci.vv.getMutator())
              .setSafe(rowIndex, result.getDouble(pci.index));
        } else {
          ((Float8Vector.Mutator) pci.vv.getMutator())
              .setSafe(rowIndex, result.getDouble(pci.index));
        }
        break;
      case FLOAT:
        if (pci.kuduColumn.isNullable()) {
          ((NullableFloat4Vector.Mutator) pci.vv.getMutator())
              .setSafe(rowIndex, result.getFloat(pci.index));
        } else {
          ((Float4Vector.Mutator) pci.vv.getMutator())
              .setSafe(rowIndex, result.getFloat(pci.index));
        }
        break;
      case INT16:
        if (pci.kuduColumn.isNullable()) {
          ((NullableIntVector.Mutator) pci.vv.getMutator())
              .setSafe(rowIndex, result.getShort(pci.index));
        } else {
          ((IntVector.Mutator) pci.vv.getMutator())
              .setSafe(rowIndex, result.getShort(pci.index));
        }
        break;
      case INT32:
        if (pci.kuduColumn.isNullable()) {
          ((NullableIntVector.Mutator) pci.vv.getMutator())
              .setSafe(rowIndex, result.getInt(pci.index));
        } else {
          ((IntVector.Mutator) pci.vv.getMutator())
              .setSafe(rowIndex, result.getInt(pci.index));
        }
        break;
      case INT8:
        if (pci.kuduColumn.isNullable()) {
          ((NullableIntVector.Mutator) pci.vv.getMutator())
              .setSafe(rowIndex, result.getByte(pci.index));
        } else {
          ((IntVector.Mutator) pci.vv.getMutator())
              .setSafe(rowIndex, result.getByte(pci.index));
        }
        break;
      case INT64:
        if (pci.kuduColumn.isNullable()) {
          ((NullableBigIntVector.Mutator) pci.vv.getMutator())
              .setSafe(rowIndex, result.getLong(pci.index));
        } else {
          ((BigIntVector.Mutator) pci.vv.getMutator())
              .setSafe(rowIndex, result.getLong(pci.index));
        }
        break;
        case UNIXTIME_MICROS:
        if (pci.kuduColumn.isNullable()) {
          ((NullableTimeStampVector.Mutator) pci.vv.getMutator())
              .setSafe(rowIndex, result.getLong(pci.index) / 1000);
        } else {
          ((TimeStampVector.Mutator) pci.vv.getMutator())
              .setSafe(rowIndex, result.getLong(pci.index) / 1000);
        }
        break;
      default:
        throw new SchemaChangeException("unknown type"); // TODO make better
      }
    }
  }

  @Override
  public void close() {
  }

  @Override
  public String toString() {
    return "KuduRecordReader[Column=" + lastColumnName
        + ", Type=" + lastColumnType
        + "]";
  }
}
