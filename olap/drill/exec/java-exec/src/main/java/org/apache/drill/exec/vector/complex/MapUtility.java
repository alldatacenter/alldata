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
package org.apache.drill.exec.vector.complex;

import java.util.function.BiFunction;
import java.util.function.Function;

import org.apache.drill.common.exceptions.DrillRuntimeException;
import org.apache.drill.common.types.TypeProtos;
import org.apache.drill.common.types.TypeProtos.MinorType;
import org.apache.drill.common.types.TypeProtos.MajorType;

import org.apache.drill.exec.expr.fn.impl.MappifyUtility;
import org.apache.drill.exec.record.MaterializedField;
import org.apache.drill.exec.vector.complex.impl.UnionReader;
import org.apache.drill.exec.vector.complex.impl.UnionVectorWriter;
import org.apache.drill.exec.vector.complex.reader.FieldReader;
import org.apache.drill.exec.vector.complex.writer.BaseWriter.ListWriter;
import org.apache.drill.exec.vector.complex.writer.BaseWriter.MapWriter;
import org.apache.drill.exec.vector.complex.writer.Decimal18Writer;
import org.apache.drill.exec.vector.complex.writer.Decimal28SparseWriter;
import org.apache.drill.exec.vector.complex.writer.Decimal38SparseWriter;
import org.apache.drill.exec.vector.complex.writer.Decimal9Writer;
import org.apache.drill.exec.vector.complex.writer.VarDecimalWriter;

public class MapUtility {
  private final static String TYPE_MISMATCH_ERROR = "%s does not support heterogeneous value types. " +
      "All values in the input map must be of the same type. The field [%s] has a differing type [%s].";

  /*
   * Function to read a value from the field reader, detect the type, construct the appropriate value holder
   * and use the value holder to write to the Map.
   */
  public static void writeToMapFromReader(FieldReader fieldReader, MapWriter mapWriter, String caller) {
    writeToMapFromReader(fieldReader, mapWriter, MappifyUtility.fieldValue, caller);
  }

  public static void writeToMapFromReader(FieldReader fieldReader, MapWriter mapWriter,
                                          String fieldName, String caller) {
    try {
      MajorType valueMajorType = fieldReader.getType();
      MinorType valueMinorType = valueMajorType.getMinorType();
      WriterExtractor extractor = new WriterExtractor(fieldName, valueMajorType, mapWriter, fieldReader instanceof UnionReader);
      switch (valueMinorType) {
        case TINYINT:
          fieldReader.copyAsValue(extractor.get(ListWriter::tinyInt, MapWriter::tinyInt, UnionVectorWriter::tinyInt));
          break;
        case SMALLINT:
          fieldReader.copyAsValue(extractor.get(ListWriter::smallInt, MapWriter::smallInt, UnionVectorWriter::smallInt));
          break;
        case BIGINT:
          fieldReader.copyAsValue(extractor.get(ListWriter::bigInt, MapWriter::bigInt, UnionVectorWriter::bigInt));
          break;
        case INT:
          fieldReader.copyAsValue(extractor.get(ListWriter::integer, MapWriter::integer, UnionVectorWriter::integer));
          break;
        case UINT1:
          fieldReader.copyAsValue(extractor.get(ListWriter::uInt1, MapWriter::uInt1, UnionVectorWriter::uInt1));
          break;
        case UINT2:
          fieldReader.copyAsValue(extractor.get(ListWriter::uInt2, MapWriter::uInt2, UnionVectorWriter::uInt2));
          break;
        case UINT4:
          fieldReader.copyAsValue(extractor.get(ListWriter::uInt4, MapWriter::uInt4, UnionVectorWriter::uInt4));
          break;
        case UINT8:
          fieldReader.copyAsValue(extractor.get(ListWriter::uInt8, MapWriter::uInt8, UnionVectorWriter::uInt8));
          break;
        case DECIMAL9:
          fieldReader.copyAsValue((Decimal9Writer)
              extractor.get(ListWriter::decimal9, MapWriter::decimal9, UnionVectorWriter::decimal9));
          break;
        case DECIMAL18:
          fieldReader.copyAsValue((Decimal18Writer)
              extractor.get(ListWriter::decimal18, MapWriter::decimal18, UnionVectorWriter::decimal18));
          break;
        case DECIMAL28SPARSE:
          fieldReader.copyAsValue((Decimal28SparseWriter)
              extractor.get(ListWriter::decimal28Sparse, MapWriter::decimal28Sparse, UnionVectorWriter::decimal28Sparse));
          break;
        case DECIMAL38SPARSE:
          fieldReader.copyAsValue((Decimal38SparseWriter)
              extractor.get(ListWriter::decimal38Sparse, MapWriter::decimal38Sparse, UnionVectorWriter::decimal38Sparse));
          break;
        case VARDECIMAL:
          fieldReader.copyAsValue((VarDecimalWriter) extractor.get(
              lw -> lw.varDecimal(valueMajorType.getPrecision(), valueMajorType.getScale()),
              (mw, fn) -> mw.varDecimal(fn, valueMajorType.getPrecision(), valueMajorType.getScale()),
              uw -> uw.varDecimal(valueMajorType.getPrecision(), valueMajorType.getScale())));
          break;
        case DATE:
          fieldReader.copyAsValue(extractor.get(ListWriter::date, MapWriter::date, UnionVectorWriter::date));
          break;
        case TIME:
          fieldReader.copyAsValue(extractor.get(ListWriter::time, MapWriter::time, UnionVectorWriter::time));
          break;
        case TIMESTAMP:
          fieldReader.copyAsValue(extractor.get(ListWriter::timeStamp, MapWriter::timeStamp, UnionVectorWriter::timeStamp));
          break;
        case INTERVAL:
          fieldReader.copyAsValue(extractor.get(ListWriter::interval, MapWriter::interval, UnionVectorWriter::interval));
          break;
        case INTERVALDAY:
          fieldReader.copyAsValue(extractor.get(ListWriter::intervalDay, MapWriter::intervalDay, UnionVectorWriter::intervalDay));
          break;
        case INTERVALYEAR:
          fieldReader.copyAsValue(extractor.get(ListWriter::intervalYear, MapWriter::intervalYear, UnionVectorWriter::intervalYear));
          break;
        case FLOAT4:
          fieldReader.copyAsValue(extractor.get(ListWriter::float4, MapWriter::float4, UnionVectorWriter::float4));
          break;
        case FLOAT8:
          fieldReader.copyAsValue(extractor.get(ListWriter::float8, MapWriter::float8, UnionVectorWriter::float8));
          break;
        case BIT:
          fieldReader.copyAsValue(extractor.get(ListWriter::bit, MapWriter::bit,  UnionVectorWriter::bit));
          break;
        case VARCHAR:
          fieldReader.copyAsValue(extractor.get(ListWriter::varChar, MapWriter::varChar, UnionVectorWriter::varChar));
          break;
        case VARBINARY:
          fieldReader.copyAsValue(extractor.get(ListWriter::varBinary, MapWriter::varBinary, UnionVectorWriter::varBinary));
          break;
        case MAP:
          fieldReader.copyAsValue(extractor.get(ListWriter::map, MapWriter::map, UnionVectorWriter::map));
          break;
        case LIST:
          fieldReader.copyAsValue(mapWriter.list(fieldName).list());
          break;
        case DICT:
          fieldReader.copyAsValue(extractor.get(ListWriter::dict, MapWriter::dict, UnionVectorWriter::dict));
          break;
        default:
          throw new DrillRuntimeException(String.format("%s does not support input of type: %s", caller, valueMinorType));
      }
    } catch (ClassCastException e) {
      final MaterializedField field = fieldReader.getField();
      throw new DrillRuntimeException(String.format(TYPE_MISMATCH_ERROR, caller, field.getName(), field.getType()));
    }
  }

  public static void writeToListFromReader(FieldReader fieldReader, ListWriter listWriter, String caller) {
    try {
      MajorType valueMajorType = fieldReader.getType();
      MinorType valueMinorType = valueMajorType.getMinorType();

      switch (valueMinorType) {
        case TINYINT:
          fieldReader.copyAsValue(listWriter.tinyInt());
          break;
        case SMALLINT:
          fieldReader.copyAsValue(listWriter.smallInt());
          break;
        case BIGINT:
          fieldReader.copyAsValue(listWriter.bigInt());
          break;
        case INT:
          fieldReader.copyAsValue(listWriter.integer());
          break;
        case UINT1:
          fieldReader.copyAsValue(listWriter.uInt1());
          break;
        case UINT2:
          fieldReader.copyAsValue(listWriter.uInt2());
          break;
        case UINT4:
          fieldReader.copyAsValue(listWriter.uInt4());
          break;
        case UINT8:
          fieldReader.copyAsValue(listWriter.uInt8());
          break;
        case DECIMAL9:
          fieldReader.copyAsValue(listWriter.decimal9());
          break;
        case DECIMAL18:
          fieldReader.copyAsValue(listWriter.decimal18());
          break;
        case DECIMAL28SPARSE:
          fieldReader.copyAsValue(listWriter.decimal28Sparse());
          break;
        case DECIMAL38SPARSE:
          fieldReader.copyAsValue(listWriter.decimal38Sparse());
          break;
        case VARDECIMAL:
          fieldReader.copyAsValue(listWriter.varDecimal(valueMajorType.getPrecision(), valueMajorType.getScale()));
          break;
        case DATE:
          fieldReader.copyAsValue(listWriter.date());
          break;
        case TIME:
          fieldReader.copyAsValue(listWriter.time());
          break;
        case TIMESTAMP:
          fieldReader.copyAsValue(listWriter.timeStamp());
          break;
        case INTERVAL:
          fieldReader.copyAsValue(listWriter.interval());
          break;
        case INTERVALDAY:
          fieldReader.copyAsValue(listWriter.intervalDay());
          break;
        case INTERVALYEAR:
          fieldReader.copyAsValue(listWriter.intervalYear());
          break;
        case FLOAT4:
          fieldReader.copyAsValue(listWriter.float4());
          break;
        case FLOAT8:
          fieldReader.copyAsValue(listWriter.float8());
          break;
        case BIT:
          fieldReader.copyAsValue(listWriter.bit());
          break;
        case VARCHAR:
          fieldReader.copyAsValue(listWriter.varChar());
          break;
        case VARBINARY:
          fieldReader.copyAsValue(listWriter.varBinary());
          break;
        case MAP:
          fieldReader.copyAsValue(listWriter.map());
          break;
        case LIST:
          fieldReader.copyAsValue(listWriter.list());
          break;
        default:
          throw new DrillRuntimeException(String.format(caller
              + " function does not support input of type: %s", valueMinorType));
      }
    } catch (ClassCastException e) {
      final MaterializedField field = fieldReader.getField();
      throw new DrillRuntimeException(String.format(caller + TYPE_MISMATCH_ERROR, field.getName(), field.getType()));
    }
  }

  private static class WriterExtractor {
    private final String fieldName;
    private final boolean repeated;
    private final MapWriter mapWriter;
    private final boolean isUnionField;

    private WriterExtractor(String fieldName, MajorType majorType, MapWriter mapWriter, boolean isUnionField) {
      this.fieldName = fieldName;
      this.repeated = majorType.getMode() == TypeProtos.DataMode.REPEATED;
      this.mapWriter = mapWriter;
      this.isUnionField = isUnionField;
    }

    private <W> W get(Function<ListWriter, W> listFunc,
                      BiFunction<MapWriter, String, W> mapFunc,
                      Function<UnionVectorWriter, W> unionFunc) {
      if (repeated) {
        ListWriter listWriter = mapWriter.list(fieldName);
        if (isUnionField) {
          return unionFunc.apply(listWriter.union());
        } else {
          return listFunc.apply(listWriter);
        }
      } else {
        if (isUnionField) {
          return unionFunc.apply(mapWriter.union(fieldName));
        } else {
          return mapFunc.apply(mapWriter, fieldName);
        }
      }
    }
  }
}
