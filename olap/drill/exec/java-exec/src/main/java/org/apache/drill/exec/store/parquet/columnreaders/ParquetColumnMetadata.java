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
package org.apache.drill.exec.store.parquet.columnreaders;

import java.util.Map;

import org.apache.drill.common.exceptions.ExecutionSetupException;
import org.apache.drill.common.expression.SchemaPath;
import org.apache.drill.common.types.TypeProtos;
import org.apache.drill.common.types.TypeProtos.DataMode;
import org.apache.drill.common.types.TypeProtos.MajorType;
import org.apache.drill.exec.exception.SchemaChangeException;
import org.apache.drill.exec.expr.TypeHelper;
import org.apache.drill.exec.physical.impl.OutputMutator;
import org.apache.drill.exec.record.MaterializedField;
import org.apache.drill.exec.server.options.OptionManager;
import org.apache.drill.exec.store.parquet.ParquetReaderUtility;
import org.apache.drill.exec.vector.ValueVector;
import org.apache.drill.exec.vector.complex.RepeatedValueVector;
import org.apache.parquet.column.ColumnDescriptor;
import org.apache.parquet.format.SchemaElement;
import org.apache.parquet.hadoop.metadata.ColumnChunkMetaData;
import org.apache.parquet.schema.PrimitiveType;
import org.apache.parquet.schema.PrimitiveType.PrimitiveTypeName;

/**
 * Represents a single column read from the Parquet file by the record reader.
 */

public class ParquetColumnMetadata {

  ColumnDescriptor column;
  private SchemaElement se;
  MaterializedField field;
  int length;
  private MajorType type;
  ColumnChunkMetaData columnChunkMetaData;
  private ValueVector vector;

  public ParquetColumnMetadata(ColumnDescriptor column) {
    this.column = column;
  }

  public void resolveDrillType(Map<String, SchemaElement> schemaElements, OptionManager options) {
    se = schemaElements.get(ParquetReaderUtility.getFullColumnPath(column));
    type = ParquetToDrillTypeConverter.toMajorType(column.getType(), column.getTypeLength(),
        getDataMode(column), se, options);
    field = MaterializedField.create(toFieldName(column.getPath()).getLastSegment().getNameSegment().getPath(), type);
    length = getDataTypeLength();
  }

  private SchemaPath toFieldName(String[] paths) {
    return SchemaPath.getCompoundPath(paths);
  }

  private TypeProtos.DataMode getDataMode(ColumnDescriptor column) {
    if (isRepeated()) {
      return DataMode.REPEATED;
    } else if (column.getMaxDefinitionLevel() == 0) {
      return TypeProtos.DataMode.REQUIRED;
    } else {
      return TypeProtos.DataMode.OPTIONAL;
    }
  }

  /**
   * @param type
   * @param type a fixed length type from the parquet library enum
   * @return the length in pageDataByteArray of the type
   */
  public static int getTypeLengthInBits(PrimitiveTypeName type) {
    switch (type) {
      case INT64:   return 64;
      case INT32:   return 32;
      case BOOLEAN: return 1;
      case FLOAT:   return 32;
      case DOUBLE:  return 64;
      case INT96:   return 96;
      // binary and fixed length byte array
      default:
        throw new IllegalStateException("Length cannot be determined for type " + type);
    }
  }

  public static final int UNDEFINED_LENGTH = -1;

  /**
   * Returns data type length for a given {@see ColumnDescriptor} and it's corresponding
   * {@see SchemaElement}. Neither is enough information alone as the max
   * repetition level (indicating if it is an array type) is in the ColumnDescriptor and
   * the length of a fixed width field is stored at the schema level.
   *
   * @return the length if fixed width, else <tt>UNDEFINED_LENGTH</tt> (-1)
   */
  public int getDataTypeLength() {
    if (! isFixedLength()) {
      return UNDEFINED_LENGTH;
    } else if (isRepeated()) {
      return UNDEFINED_LENGTH;
    } else if (column.getType() == PrimitiveType.PrimitiveTypeName.FIXED_LEN_BYTE_ARRAY) {
      return se.getType_length() * 8;
    } else {
      return getTypeLengthInBits(column.getType());
    }
  }

  public boolean isFixedLength( ) {
    return column.getType() != PrimitiveType.PrimitiveTypeName.BINARY;
  }

  public boolean isRepeated() {
    return column.getMaxRepetitionLevel() > 0;
  }

  public MaterializedField getField() {
    return field;
  }

  ValueVector buildVector(OutputMutator output) throws SchemaChangeException {
    Class<? extends ValueVector> vectorClass = TypeHelper.getValueVectorClass(type.getMinorType(), type.getMode());
    vector = output.addField(field, vectorClass);
    return vector;
  }

  ColumnReader<?> makeFixedWidthReader(ParquetRecordReader reader) throws Exception {
    return ColumnReaderFactory.createFixedColumnReader(reader, true,
        column, columnChunkMetaData, vector, se);
  }

  FixedWidthRepeatedReader makeRepeatedFixedWidthReader(ParquetRecordReader reader) throws Exception {
    final RepeatedValueVector repeatedVector = RepeatedValueVector.class.cast(vector);
    ColumnReader<?> dataReader = ColumnReaderFactory.createFixedColumnReader(reader, true,
        column, columnChunkMetaData,
        repeatedVector.getDataVector(), se);
    return new FixedWidthRepeatedReader(reader, dataReader,
        getTypeLengthInBits(column.getType()), column, columnChunkMetaData, false, repeatedVector, se);
  }

  VarLengthValuesColumn<?> makeVariableWidthReader(ParquetRecordReader reader) throws ExecutionSetupException {
    return ColumnReaderFactory.getReader(reader, column, columnChunkMetaData, false, vector, se);
  }

}
