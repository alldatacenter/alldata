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

package com.bytedance.bitsail.component.format.hive;

import com.bytedance.bitsail.common.BitSailException;
import com.bytedance.bitsail.common.column.BooleanColumn;
import com.bytedance.bitsail.common.column.BytesColumn;
import com.bytedance.bitsail.common.column.Column;
import com.bytedance.bitsail.common.column.ColumnCast;
import com.bytedance.bitsail.common.column.DateColumn;
import com.bytedance.bitsail.common.column.DoubleColumn;
import com.bytedance.bitsail.common.column.ListColumn;
import com.bytedance.bitsail.common.column.LongColumn;
import com.bytedance.bitsail.common.column.MapColumn;
import com.bytedance.bitsail.common.column.StringColumn;
import com.bytedance.bitsail.common.exception.CommonErrorCode;
import com.bytedance.bitsail.component.format.api.RowBuilder;
import com.bytedance.bitsail.flink.core.typeinfo.ListColumnTypeInfo;
import com.bytedance.bitsail.flink.core.typeinfo.MapColumnTypeInfo;

import com.bytedance.bitsail.shaded.hive.client.HiveMetaClientUtil;
import com.bytedance.bitsail.shaded.hive.shim.HiveShim;
import com.bytedance.bitsail.shaded.hive.shim.HiveShimLoader;

import com.google.common.annotations.VisibleForTesting;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.java.typeutils.RowTypeInfo;
import org.apache.flink.types.Row;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hive.conf.HiveConf;
import org.apache.hadoop.hive.metastore.api.StorageDescriptor;
import org.apache.hadoop.hive.serde2.Deserializer;
import org.apache.hadoop.hive.serde2.SerDeUtils;
import org.apache.hadoop.hive.serde2.objectinspector.ListObjectInspector;
import org.apache.hadoop.hive.serde2.objectinspector.MapObjectInspector;
import org.apache.hadoop.hive.serde2.objectinspector.ObjectInspector;
import org.apache.hadoop.hive.serde2.objectinspector.PrimitiveObjectInspector;
import org.apache.hadoop.hive.serde2.objectinspector.StructField;
import org.apache.hadoop.hive.serde2.objectinspector.StructObjectInspector;
import org.apache.hadoop.hive.serde2.objectinspector.primitive.DateObjectInspector;
import org.apache.hadoop.hive.serde2.objectinspector.primitive.HiveCharObjectInspector;
import org.apache.hadoop.hive.serde2.objectinspector.primitive.HiveDecimalObjectInspector;
import org.apache.hadoop.hive.serde2.objectinspector.primitive.HiveVarcharObjectInspector;
import org.apache.hadoop.hive.serde2.objectinspector.primitive.TimestampObjectInspector;
import org.apache.hadoop.hive.serde2.objectinspector.primitive.VoidObjectInspector;
import org.apache.hadoop.io.Writable;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Properties;

@SuppressWarnings("checkstyle:MagicNumber")
public class HiveGeneralRowBuilder implements RowBuilder {
  private final Map<String, String> hiveProperties;
  private final String db;
  private final String table;
  private final Map<String, Integer> columnMapping;
  private final HiveShim hiveShim;

  //Hive StructField list contain all related info for specific serde.
  private transient List<? extends StructField> structFields = null;

  //StructObjectInspector in hive helps us to look into the internal structure of a struct object.
  private transient StructObjectInspector structObjectInspector = null;
  private transient Deserializer deserializer = null;

  public HiveGeneralRowBuilder(Map<String, Integer> columnMapping,
                               String database,
                               String table,
                               Map<String, String> hiveProperties) {
    this.columnMapping = columnMapping;
    // Hive table related
    this.db = database;
    this.table = table;
    this.hiveProperties = hiveProperties;
    this.hiveShim = HiveShimLoader.loadHiveShim();
  }

  @VisibleForTesting
  public HiveGeneralRowBuilder() {
    this.columnMapping = null;
    this.db = null;
    this.table = null;
    this.hiveProperties = null;
    this.hiveShim = null;
  }

  @Override
  public void build(Object objectValue, Row reuse, String mandatoryEncoding, RowTypeInfo rowTypeInfo) throws BitSailException {
    if (deserializer == null) {
      synchronized (hiveShim) {
        if (deserializer == null) {
          try {
            HiveMetaClientUtil.init();
            HiveConf hiveConf = HiveMetaClientUtil.getHiveConf(hiveProperties);
            StorageDescriptor storageDescriptor = HiveMetaClientUtil.getTableFormat(hiveConf, db, table);
            deserializer = (Deserializer) Class.forName(storageDescriptor.getSerdeInfo().getSerializationLib()).newInstance();
            Configuration conf = new Configuration();
            Properties properties = HiveMetaClientUtil.getHiveTableMetadata(hiveConf, db, table);
            SerDeUtils.initializeSerDe(deserializer, conf, properties, null);
            structObjectInspector = (StructObjectInspector) deserializer.getObjectInspector();
            structFields = structObjectInspector.getAllStructFieldRefs();

          } catch (Exception e) {
            throw BitSailException.asBitSailException(CommonErrorCode.INTERNAL_ERROR, "Error happens when deserialize from storage file.", e);
          }
        }
      }
    }
    Object hiveRowStruct;
    try {
      //Use HiveDeserializer to deserialize an object out of a Writable blob
      hiveRowStruct = deserializer.deserialize((Writable) objectValue);
      for (int i = 0; i < reuse.getArity(); i++) {
        TypeInformation typeInfo = rowTypeInfo.getTypeAt(i);
        String columnName = rowTypeInfo.getFieldNames()[i];
        StructField structField = structFields.get(columnMapping.get(columnName.toUpperCase()));

        Column column = createColumn(
            typeInfo,
            structField.getFieldObjectInspector(),
            structObjectInspector.getStructFieldData(hiveRowStruct, structField));
        reuse.setField(i, column);

      }
    } catch (Exception e) {
      throw BitSailException.asBitSailException(CommonErrorCode.RUNTIME_ERROR, e);
    }

  }

  @VisibleForTesting
  public Column createColumn(TypeInformation typeInformation, ObjectInspector inspector, Object data) {
    Class columnTypeClass = typeInformation.getTypeClass();
    //List
    if (columnTypeClass == List.class) {
      return createListColumn(typeInformation, inspector, data);
    } else if (columnTypeClass == Map.class) {
      //Map
      return createMapColumn(typeInformation, inspector, data);
    } else {
      return createBasicColumn(typeInformation,
          inspector, data);
    }
  }

  private Column createListColumn(TypeInformation typeInformation, ObjectInspector inspector, Object data) {
    final ListColumnTypeInfo listTypeInfo = (ListColumnTypeInfo) typeInformation;
    TypeInformation<?> valueTypeInfo = listTypeInfo.getElementTypeInfo();

    ListColumn<Column> lc = new ListColumn<>(Column.class);
    if (!(data == null || inspector instanceof VoidObjectInspector)) {
      if (!(inspector instanceof ListObjectInspector)) {
        throw BitSailException.asBitSailException(CommonErrorCode.CONFIG_ERROR, "List type information with " + inspector.getTypeName());
      }
      ListObjectInspector listInspector = (ListObjectInspector) inspector;
      List<?> list = listInspector.getList(data);
      ObjectInspector elementInspector = listInspector.getListElementObjectInspector();
      for (Object o : list) {
        Column tmp = createColumn(valueTypeInfo, elementInspector, o);
        lc.add(tmp);
      }
    }
    return lc;
  }

  private Column createMapColumn(TypeInformation typeInformation, ObjectInspector inspector, Object data) {
    final MapColumnTypeInfo mapTypeInfo = (MapColumnTypeInfo) typeInformation;
    TypeInformation<?> keyTypeInfo = mapTypeInfo.getKeyTypeInfo();
    TypeInformation<?> valueTypeInfo = mapTypeInfo.getValueTypeInfo();

    MapColumn<Column, Column> mc = new MapColumn<>(Column.class, Column.class);
    if (!(data == null || inspector instanceof VoidObjectInspector)) {
      if (!(inspector instanceof MapObjectInspector)) {
        throw BitSailException.asBitSailException(CommonErrorCode.CONFIG_ERROR, "Map type information with " + inspector.getTypeName());
      }
      MapObjectInspector mapInspector = (MapObjectInspector) inspector;
      Map<?, ?> map = mapInspector.getMap(data);
      for (Map.Entry entry : map.entrySet()) {
        Column keyColumn = createColumn(keyTypeInfo, mapInspector.getMapKeyObjectInspector(), entry.getKey());
        Column valueColumn = createColumn(valueTypeInfo, mapInspector.getMapValueObjectInspector(), entry.getValue());
        mc.put(keyColumn, valueColumn);
      }
    }
    return mc;
  }

  private Column createBasicColumn(TypeInformation typeInformation, ObjectInspector inspector, Object data) {
    Class columnTypeClass = typeInformation.getTypeClass();
    boolean nullColumn = data == null || inspector instanceof VoidObjectInspector;
    Object fieldVal = null;
    if (!nullColumn && inspector instanceof PrimitiveObjectInspector) {
      if (inspector instanceof HiveDecimalObjectInspector) {
        HiveDecimalObjectInspector oi = (HiveDecimalObjectInspector) inspector;
        fieldVal = oi.getPrimitiveJavaObject(data).bigDecimalValue();
      } else if (inspector instanceof HiveVarcharObjectInspector) {
        HiveVarcharObjectInspector oi = (HiveVarcharObjectInspector) inspector;
        fieldVal = oi.getPrimitiveJavaObject(data).getValue();
      } else if (inspector instanceof HiveCharObjectInspector) {
        HiveCharObjectInspector oi = (HiveCharObjectInspector) inspector;
        fieldVal = oi.getPrimitiveJavaObject(data).getValue();
      } else if (inspector instanceof DateObjectInspector) {
        PrimitiveObjectInspector poi = (PrimitiveObjectInspector) inspector;
        fieldVal = hiveShim.toDate(poi.getPrimitiveJavaObject(data));
      } else if (inspector instanceof TimestampObjectInspector) {
        PrimitiveObjectInspector poi = (PrimitiveObjectInspector) inspector;
        fieldVal = hiveShim.toTimestamp(poi.getPrimitiveJavaObject(data));
      } else {
        PrimitiveObjectInspector poi = (PrimitiveObjectInspector) inspector;
        fieldVal = poi.getPrimitiveJavaObject(data);
      }
    }

    if (columnTypeClass == StringColumn.class) {
      return new StringColumn(nullColumn ? null : String.valueOf(fieldVal));
    } else if (columnTypeClass == BooleanColumn.class) {
      return getBooleanColumnValue(fieldVal);
    } else if (columnTypeClass == BytesColumn.class) {
      return getBytesColumnValue(fieldVal);
    } else if (columnTypeClass == LongColumn.class) {
      return getLongColumnValue(fieldVal);
    } else if (columnTypeClass == DateColumn.class) {
      return getDateColumnValue(fieldVal);
    } else if (columnTypeClass == DoubleColumn.class) {
      return getDoubleColumnValue(fieldVal);
    } else {
      throw BitSailException.asBitSailException(CommonErrorCode.UNSUPPORTED_COLUMN_TYPE, columnTypeClass + " is not supported yet!");
    }
  }

  protected LongColumn getLongColumnValue(Object fieldVal) {
    if (fieldVal == null) {
      return new LongColumn();
    }
    if (fieldVal instanceof Integer) {
      return new LongColumn((Integer) fieldVal);
    } else if (fieldVal instanceof Long) {
      return new LongColumn((Long) fieldVal);
    }
    long timestamp;
    if (fieldVal instanceof java.sql.Date) {
      timestamp = ((java.sql.Date) fieldVal).getTime() / 1000;
      return new LongColumn(timestamp);
    } else if (fieldVal instanceof java.sql.Time) {
      timestamp = ((java.sql.Time) fieldVal).getTime() / 1000;
      return new LongColumn(timestamp);
    } else if (fieldVal instanceof java.sql.Timestamp) {
      timestamp = ((java.sql.Timestamp) fieldVal).getTime() / 1000;
      return new LongColumn(timestamp);
    } else if (fieldVal instanceof Date) {
      timestamp = ((Date) fieldVal).getTime() / 1000;
      return new LongColumn(timestamp);
    } else {
      return new LongColumn(fieldVal.toString());
    }
  }

  protected BooleanColumn getBooleanColumnValue(Object fieldVal) {
    if (fieldVal == null) {
      return new BooleanColumn(false);
    }

    if (fieldVal instanceof Boolean) {
      return new BooleanColumn((Boolean) fieldVal);
    }

    return new BooleanColumn(fieldVal.toString());
  }

  protected DoubleColumn getDoubleColumnValue(Object fieldVal) {
    if (fieldVal == null) {
      return new DoubleColumn();
    }

    if (fieldVal instanceof BigDecimal) {
      return new DoubleColumn((BigDecimal) fieldVal);
    }

    return new DoubleColumn(Double.parseDouble(fieldVal.toString()));
  }

  protected BytesColumn getBytesColumnValue(Object fieldVal) {
    if (fieldVal == null) {
      return new BytesColumn();
    }
    if (fieldVal instanceof byte[]) {
      return new BytesColumn((byte[]) fieldVal);
    }

    if (fieldVal instanceof Byte[]) {
      return new BytesColumn(ArrayUtils.toPrimitive((Byte[]) fieldVal));
    }

    return new BytesColumn(fieldVal.toString().getBytes());
  }

  protected DateColumn getDateColumnValue(Object fieldVal) {
    if (fieldVal == null) {
      return new DateColumn();
    }

    if (fieldVal instanceof java.sql.Date) {
      return new DateColumn((java.sql.Date) fieldVal);
    } else if (fieldVal instanceof java.sql.Time) {
      return new DateColumn((java.sql.Time) fieldVal);
    } else if (fieldVal instanceof java.sql.Timestamp) {
      return new DateColumn((java.sql.Timestamp) fieldVal);
    } else if (fieldVal instanceof Date) {
      return new DateColumn(((Date) fieldVal).getTime());
    }

    try {
      StringColumn strColumn = new StringColumn(fieldVal.toString());
      return new DateColumn(ColumnCast.string2Date(strColumn).getTime());
    } catch (Exception e) {
      throw BitSailException.asBitSailException(
          CommonErrorCode.CONVERT_NOT_SUPPORT,
          String.format("String[%s] can't convert to Date.", fieldVal));
    }
  }
}
