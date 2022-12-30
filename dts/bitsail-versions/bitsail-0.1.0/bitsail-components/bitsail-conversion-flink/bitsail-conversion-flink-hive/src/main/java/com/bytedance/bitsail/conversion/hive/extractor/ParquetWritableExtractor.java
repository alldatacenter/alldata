/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytedance.bitsail.conversion.hive.extractor;

import com.bytedance.bitsail.common.BitSailException;
import com.bytedance.bitsail.common.column.ArrayMapColumn;
import com.bytedance.bitsail.common.column.Column;
import com.bytedance.bitsail.common.column.ListColumn;
import com.bytedance.bitsail.common.column.MapColumn;
import com.bytedance.bitsail.common.exception.CommonErrorCode;
import com.bytedance.bitsail.conversion.hive.ConvertToHiveObjectOptions;

import com.bytedance.bitsail.shaded.hive.client.HiveMetaClientUtil;

import org.apache.flink.types.Row;
import org.apache.hadoop.hive.common.type.HiveDecimal;
import org.apache.hadoop.hive.ql.io.parquet.serde.AbstractParquetMapInspector;
import org.apache.hadoop.hive.ql.io.parquet.serde.ArrayWritableObjectInspector;
import org.apache.hadoop.hive.serde2.io.ByteWritable;
import org.apache.hadoop.hive.serde2.io.DoubleWritable;
import org.apache.hadoop.hive.serde2.io.HiveDecimalWritable;
import org.apache.hadoop.hive.serde2.io.ShortWritable;
import org.apache.hadoop.hive.serde2.objectinspector.ListObjectInspector;
import org.apache.hadoop.hive.serde2.objectinspector.ObjectInspector;
import org.apache.hadoop.hive.serde2.objectinspector.PrimitiveObjectInspector;
import org.apache.hadoop.hive.serde2.objectinspector.SettableStructObjectInspector;
import org.apache.hadoop.hive.serde2.typeinfo.StructTypeInfo;
import org.apache.hadoop.hive.serde2.typeinfo.TypeInfo;
import org.apache.hadoop.hive.serde2.typeinfo.TypeInfoFactory;
import org.apache.hadoop.io.ArrayWritable;
import org.apache.hadoop.io.BooleanWritable;
import org.apache.hadoop.io.BytesWritable;
import org.apache.hadoop.io.FloatWritable;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Writable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.sql.Date;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// todo: merge dump and batch hive-writable-extractor
public class ParquetWritableExtractor extends HiveWritableExtractor {

  public static final Logger LOG = LoggerFactory.getLogger(ParquetWritableExtractor.class);

  public ParquetWritableExtractor() {
  }

  @Override
  public SettableStructObjectInspector createObjectInspector(final String columnNames, final String columnTypes) {
    List<TypeInfo> columnTypeList = getHiveTypeInfos(columnTypes);
    List<String> columnNameList = getHiveColumnList(columnNames);
    StructTypeInfo rowTypeInfo = (StructTypeInfo) TypeInfoFactory
        .getStructTypeInfo(columnNameList, columnTypeList);
    inspector = new ArrayWritableObjectInspector(rowTypeInfo);
    inspectorBasicTypeMap = new HashMap<>(inspector.getAllStructFieldRefs().size());
    return inspector;
  }

  @Override
  public Object createRowObject(Row record) {
    Writable[] writables = new Writable[inspector.getAllStructFieldRefs().size()];

    String columnName = "";
    try {
      for (int i = 0; i < record.getArity(); i++) {
        Column column = (Column) record.getField(i);
        columnName = fieldNames[i].toUpperCase();

        int hiveIndex;
        try {
          hiveIndex = columnMapping.get(columnName);
        } catch (Exception e) {
          throw new RuntimeException("cannot get hive index from mapping field: " + columnName, e);
        }

        if (null != column.getRawData()) {
          ObjectInspector oi = this.getObjectInspectorByIndex(hiveIndex);
          writables[hiveIndex] = createWritableFromObjectInspector(oi, column);
        } else {
          writables[hiveIndex] = RawValueToWritableConverter.createNull();
        }
      }
    } catch (BitSailException e) {
      throw BitSailException.asBitSailException(e.getErrorCode(), String.format("column[%s] %s", columnName, e.getErrorMessage()));
    }
    return createArrayWritable(writables);
  }

  public Writable createWritableFromObjectInspector(ObjectInspector oi, Column column) {
    Writable w;
    ObjectInspector.Category category = oi.getCategory();
    switch (category) {
      case PRIMITIVE:
        w = createWritablePrimitive(oi, column);
        break;
      case LIST:
        ObjectInspector elementObjectInspector = ((ListObjectInspector) oi).getListElementObjectInspector();
        ListColumn listColumn = (ListColumn) column;
        Writable[] t = new Writable[listColumn.size()];
        for (int i = 0; i < listColumn.size(); i++) {
          t[i] = createWritableFromObjectInspector(elementObjectInspector, listColumn.get(i));
        }
        w = RawValueToWritableConverter.createGroup(t);
        break;
      case MAP:
        w = createWritableMap(oi, column);
        break;
      default:
        String message = String.format("Not supported type: [%s]", category);
        throw BitSailException.asBitSailException(CommonErrorCode.CONVERT_NOT_SUPPORT, message);
    }
    return w;
  }

  public ObjectInspector getObjectInspectorByIndex(int hiveIndex) {
    switch (convertOptions.getJobType()) {
      case BATCH:
        return inspector.getAllStructFieldRefs().get(hiveIndex).getFieldObjectInspector();
      default:
        return objectInspectors.get(hiveIndex);
    }
  }

  public Writable createWritablePrimitive(ObjectInspector oi, Column column) {
    switch (convertOptions.getJobType()) {
      case BATCH:
        String typeName = oi.getTypeName();
        SupportHiveDataType columnType = getBasicDataType(typeName, inspectorBasicTypeMap);
        return createBasicWritableBatch(columnType, column);
      default:
        return createBasicWritableDump((PrimitiveObjectInspector) oi, column);
    }
  }

  public Writable createWritableMap(ObjectInspector oi, Column column) {
    if (column instanceof ArrayMapColumn &&
        convertOptions.getJobType().equals(ConvertToHiveObjectOptions.JobType.DUMP)) {
      ArrayMapColumn mapColumn = (ArrayMapColumn) column;
      AbstractParquetMapInspector mapInspector = (AbstractParquetMapInspector) oi;

      ObjectInspector keyObjectInspector = mapInspector.getMapKeyObjectInspector();
      ObjectInspector valueObjectInspector = mapInspector.getMapValueObjectInspector();
      List<Column> keys = mapColumn.getKeys();
      List<Column> values = mapColumn.getValues();
      int len = keys.size();
      Writable[] writables = new Writable[len];
      for (int i = 0; i < len; i++) {
        Writable k = createWritableFromObjectInspector(keyObjectInspector, keys.get(i));
        Writable v = createWritableFromObjectInspector(valueObjectInspector, values.get(i));
        if (k == null) {
          throw BitSailException.asBitSailException(CommonErrorCode.CONVERT_NOT_SUPPORT, "Key in Hive MAP is null.");
        }
        writables[i] = createArrayWritable(k, v);
      }
      return RawValueToWritableConverter.createGroup(writables);
    } else {
      MapColumn mapColumn = (MapColumn) column;
      Writable[] writables = new Writable[mapColumn.size()];
      AbstractParquetMapInspector mapInspector = (AbstractParquetMapInspector) oi;

      ObjectInspector keyObjectInspector = mapInspector.getMapKeyObjectInspector();
      ObjectInspector valueObjectInspector = mapInspector.getMapValueObjectInspector();

      int index = 0;
      for (Object obj : mapColumn.entrySet()) {
        Map.Entry<Column, Column> entry = (Map.Entry<Column, Column>) obj;
        Writable k = createWritableFromObjectInspector(keyObjectInspector, entry.getKey());
        Writable v = createWritableFromObjectInspector(valueObjectInspector, entry.getValue());
        if (k == null) {
          throw BitSailException.asBitSailException(CommonErrorCode.CONVERT_NOT_SUPPORT, "Key in Hive MAP is null.");
        }
        writables[index++] = createArrayWritable(k, v);
      }

      return RawValueToWritableConverter.createGroup(writables);
    }
  }

  public Writable createIntOrBigIntWritable(Column column, boolean intOrBigInt) {
    Object convertedValue = ConvertToHiveObjectOptions.toIntOrBigintHiveObject(column, intOrBigInt, convertOptions);
    if (null == convertedValue) {
      return RawValueToWritableConverter.createNull();
    }
    if (intOrBigInt) {
      return RawValueToWritableConverter.createInt((int) convertedValue);
    } else {
      return RawValueToWritableConverter.createBigInt((Long) convertedValue);
    }
  }

  private SupportHiveDataType getBasicDataType(String columnString, Map<String, String> inspectorBasicTypeMap) {
    String type = inspectorBasicTypeMap.get(columnString);
    if (null == type) {
      type = columnString.toUpperCase();
      inspectorBasicTypeMap.put(columnString, columnString.toUpperCase());
    }
    SupportHiveDataType basicType;
    // Decimal(m, n) (
    if (type.contains("(")) {
      type = type.split("\\(")[0];
    }
    basicType = SupportHiveDataType.valueOf(type);
    return basicType;
  }

  public Writable createBasicWritableBatch(SupportHiveDataType columnType, Column column) {
    try {
      switch (columnType) {
        case STRING:
        case VARCHAR:
        case CHAR:
          return ConvertToHiveObjectOptions.convertStringToWritable(column, convertOptions);
        default:
          if (column.getRawData() == null) {
            return RawValueToWritableConverter.createNull();
          }
      }

      switch (columnType) {
        case TINYINT:
          return RawValueToWritableConverter.createTinyInt(Byte.parseByte(column.asString()));
        case SMALLINT:
          return RawValueToWritableConverter.createSmallInt(Short.parseShort(column.asString()));
        case INT:
          return createIntOrBigIntWritable(column, true);
        case BIGINT:
          return createIntOrBigIntWritable(column, false);
        case FLOAT:
          return RawValueToWritableConverter.createFloat(column.asDouble().floatValue());
        case DOUBLE:
          return RawValueToWritableConverter.createDouble(column.asDouble());
        case BINARY:
          return RawValueToWritableConverter.createBinary(column.asBytes());
        case BOOLEAN:
          return RawValueToWritableConverter.createBoolean(column.asBoolean());
        case DATE:
          return RawValueToWritableConverter.createDate(new Date(column.asDate().getTime()));
        case TIMESTAMP:
          return RawValueToWritableConverter.createTimestamp(new Timestamp(column.asDate().getTime()));
        case DECIMAL:
          return RawValueToWritableConverter.createDecimal(column.asString());
        default:
          throw new IllegalArgumentException("Column type is illegal:" + columnType);
      }
    } catch (Exception e) {
      if (convertOptions.isConvertErrorColumnAsNull()) {
        LOG.warn("Error occurred while creating basic writable, returning null. Column: " + column + ", type: " + columnType, e);
        return RawValueToWritableConverter.createNull();
      } else {
        throw e;
      }
    }
  }

  /**
   * create basic type for dump task
   */
  private Writable createBasicWritableDump(PrimitiveObjectInspector primitiveInspector, Column column) {
    PrimitiveObjectInspector.PrimitiveCategory primitiveCategory = primitiveInspector.getPrimitiveCategory();
    try {
      switch (primitiveCategory) {
        case STRING:
        case VARCHAR:
        case CHAR:
          return ConvertToHiveObjectOptions.convertStringToWritable(column, convertOptions);
        default:
          if (column.getRawData() == null) {
            return RawValueToWritableConverter.createNull();
          }
      }

      switch (primitiveCategory) {
        case BYTE:
          return RawValueToWritableConverter.createTinyInt(Byte.parseByte(column.asString()));
        case SHORT:
          return RawValueToWritableConverter.createSmallInt(Short.parseShort(column.asString()));
        case INT:
          return createIntOrBigIntWritable(column, true);
        case LONG:
          return createIntOrBigIntWritable(column, false);
        case FLOAT:
          return RawValueToWritableConverter.createFloat(column.asDouble().floatValue());
        case DOUBLE:
          return RawValueToWritableConverter.createDouble(column.asDouble());
        case BINARY:
          return RawValueToWritableConverter.createBinary(column.asBytes());
        case BOOLEAN:
          return RawValueToWritableConverter.createBoolean(column.asBoolean());
        case DATE:
          return RawValueToWritableConverter.createDate(new Date(column.asDate().getTime()));
        case TIMESTAMP:
          return RawValueToWritableConverter.createTimestamp(new Timestamp(column.asDate().getTime()));
        case DECIMAL:
          return RawValueToWritableConverter.createDecimal(column.asString());
        default:
          throw BitSailException.asBitSailException(CommonErrorCode.UNSUPPORTED_ENCODING, "Column type is illegal:" + primitiveCategory.name());
      }
    } catch (Exception e) {
      throw BitSailException.asBitSailException(CommonErrorCode.RUNTIME_ERROR, "Error while creating basic writeable value", e);
    }
  }

  public static final class RawValueToWritableConverter {
    public static Writable createNull() {
      return null;
    }

    public static ByteWritable createTinyInt(byte value) {
      return new ByteWritable(value);
    }

    public static ShortWritable createSmallInt(short value) {
      return new ShortWritable(value);
    }

    public static LongWritable createBigInt(long value) {
      return new LongWritable(value);
    }

    public static IntWritable createInt(int value) {
      return new IntWritable(value);
    }

    public static FloatWritable createFloat(float value) {
      return new FloatWritable(value);
    }

    public static DoubleWritable createDouble(double value) {
      return new DoubleWritable(value);
    }

    public static BooleanWritable createBoolean(boolean value) {
      return new BooleanWritable(value);
    }

    public static BytesWritable createStringAsBytes(String value, boolean nullStringAsNull) {
      if (value == null) {
        return nullStringAsNull ? null : new BytesWritable();
      } else {
        return new BytesWritable(value.getBytes(StandardCharsets.UTF_8));
      }
    }

    public static Writable createDate(Date value) {
      return HiveMetaClientUtil.getHiveShim().toHiveDateWritable(value);
    }

    public static Writable createTimestamp(Timestamp time) {
      return HiveMetaClientUtil.getHiveShim().toHiveTimestampWritable(time);
    }

    public static BytesWritable createBinary(byte[] bytes) {
      return new BytesWritable(bytes);
    }

    public static ArrayWritable createGroup(Writable... values) {
      return HiveMetaClientUtil.getHiveShim().getArrayWritable(values);
    }

    public static HiveDecimalWritable createDecimal(String value) {
      return new HiveDecimalWritable(HiveDecimal.create(value));
    }
  }
}
