/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.netease.arctic.flink.shuffle;

import com.netease.arctic.data.ChangeAction;
import com.netease.arctic.log.FormatVersion;
import com.netease.arctic.log.LogData;
import com.netease.arctic.log.data.LogArrayData;
import com.netease.arctic.log.data.LogMapData;
import org.apache.flink.table.data.ArrayData;
import org.apache.flink.table.data.DecimalData;
import org.apache.flink.table.data.GenericArrayData;
import org.apache.flink.table.data.GenericMapData;
import org.apache.flink.table.data.GenericRowData;
import org.apache.flink.table.data.MapData;
import org.apache.flink.table.data.RowData;
import org.apache.flink.table.data.StringData;
import org.apache.flink.table.data.TimestampData;
import org.apache.flink.util.FlinkRuntimeException;
import org.apache.iceberg.relocated.com.google.common.primitives.Longs;
import org.apache.iceberg.types.Type;
import org.apache.iceberg.types.Types;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Map;

import static com.netease.arctic.flink.shuffle.RowKindUtil.convertToFlinkRowKind;
import static org.apache.iceberg.relocated.com.google.common.base.Preconditions.checkArgument;
import static org.apache.iceberg.relocated.com.google.common.base.Preconditions.checkNotNull;

/**
 * An implement of {@link LogData<RowData>} in flink application.
 */
public class LogRecordV1 implements LogData<RowData>, Serializable {
  private static final long serialVersionUID = -3884886938053319336L;
  FormatVersion formatVersion;
  byte[] upstreamId;
  long epicNo;
  private boolean flip;
  private ChangeAction changeAction;
  private transient RowData actualValue;

  public LogRecordV1(
      FormatVersion version,
      byte[] upstreamId,
      long epicNo,
      boolean flip,
      ChangeAction changeAction,
      RowData actualValue) {
    this.formatVersion = checkNotNull(version);
    this.upstreamId = checkNotNull(upstreamId);
    this.epicNo = epicNo;
    this.flip = flip;
    this.changeAction = checkNotNull(changeAction);
    if (actualValue != null) {
      this.actualValue = actualValue;
      this.actualValue.setRowKind(convertToFlinkRowKind(changeAction));
    }
  }

  @Override
  public String getVersion() {
    return formatVersion.asString();
  }

  @Override
  public byte[] getVersionBytes() {
    return formatVersion.asBytes();
  }

  @Override
  public String getUpstreamId() {
    return new String(upstreamId);
  }

  @Override
  public byte[] getUpstreamIdBytes() {
    return upstreamId;
  }

  @Override
  public long getEpicNo() {
    return epicNo;
  }

  @Override
  public byte[] getEpicNoBytes() {
    return Longs.toByteArray(epicNo);
  }

  @Override
  public boolean getFlip() {
    return flip;
  }

  @Override
  public byte getFlipByte() {
    return flip ? (byte) 1 : (byte) 0;
  }

  @Override
  public ChangeAction getChangeAction() {
    return changeAction;
  }

  @Override
  public byte getChangeActionByte() {
    return changeAction.toByteValue();
  }

  @Override
  public RowData getActualValue() {
    return actualValue;
  }

  @Override
  public String toString() {
    return "LogData = {version = " + getVersion() +
        ", upstreamId = " + getUpstreamId() +
        ", epicNo = " + epicNo +
        ", flip = " + flip +
        ", changeAction = " + changeAction.toString() +
        ", actualValue = " + actualValue +
        "}";
  }

  public static class LogFlinkMapData implements LogMapData {
    private final MapData mapData;

    private LogFlinkMapData(MapData mapData) {
      this.mapData = mapData;
    }

    @Override
    public int size() {
      return mapData.size();
    }

    @Override
    public LogArrayData keyArray() {
      return new LogFlinkArrayData(mapData.keyArray());
    }

    @Override
    public LogArrayData valueArray() {
      return new LogFlinkArrayData(mapData.valueArray());
    }
  }

  public static class LogFlinkArrayData implements LogArrayData {
    ArrayData arrayData;

    private LogFlinkArrayData(ArrayData arrayData) {
      this.arrayData = arrayData;
    }

    @Override
    public boolean getBoolean(int pos) {
      return arrayData.getBoolean(pos);
    }

    @Override
    public int getInt(int pos) {
      return arrayData.getInt(pos);
    }

    @Override
    public long getLong(int pos) {
      return arrayData.getLong(pos);
    }

    @Override
    public float getFloat(int pos) {
      return arrayData.getFloat(pos);
    }

    @Override
    public double getDouble(int pos) {
      return arrayData.getFloat(pos);
    }

    @Override
    public LocalDateTime getTimestamp(int pos) {
      return arrayData.getTimestamp(pos, 9).toLocalDateTime();
    }

    @Override
    public Instant getInstant(int pos) {
      return arrayData.getTimestamp(pos, 9).toInstant();
    }

    @Override
    public String getString(int pos) {
      return arrayData.getString(pos).toString();
    }

    @Override
    public byte[] getBinary(int pos) {
      return arrayData.getBinary(pos);
    }

    @Override
    public BigDecimal getDecimal(int pos) {
      return arrayData.getDecimal(pos, 38, 18).toBigDecimal();
    }

    @Override
    public Object getStruct(int pos) {
      return arrayData.getRow(pos, 0);
    }

    @Override
    public LogArrayData getArray(int pos) {
      return new LogFlinkArrayData(arrayData.getArray(pos));
    }

    @Override
    public LogMapData getMap(int pos) {
      return new LogFlinkMapData(arrayData.getMap(pos));
    }

    @Override
    public boolean isNullAt(int pos) {
      return arrayData.isNullAt(pos);
    }

    @Override
    public int size() {
      return arrayData.size();
    }
  }

  public static final LogData.FieldGetterFactory<RowData> fieldGetterFactory =
      new LogData.FieldGetterFactory<RowData>() {
        private static final long serialVersionUID = 1L;

        @Override
        public FieldGetter<RowData> createFieldGetter(Type type, int pos) {
          final FieldGetter<RowData> fieldGetter;
          switch (type.typeId()) {
            case BOOLEAN:
              fieldGetter = RowData::getBoolean;
              break;
            case INTEGER:
            case DATE:
              fieldGetter = RowData::getInt;
              break;
            case LONG:
            case TIME:
              fieldGetter = RowData::getLong;
              break;
            case FLOAT:
              fieldGetter = RowData::getFloat;
              break;
            case DOUBLE:
              fieldGetter = RowData::getDouble;
              break;
            case TIMESTAMP:
              Types.TimestampType timestamp = (Types.TimestampType) type;
              if (timestamp.shouldAdjustToUTC()) {
                return (row, fieldPos) -> {
                  if (row.isNullAt(fieldPos)) {
                    return null;
                  }
                  return row.getTimestamp(fieldPos, 9).toInstant();
                };
              } else {
                return (row, fieldPos) -> {
                  if (row.isNullAt(fieldPos)) {
                    return null;
                  }
                  return row.getTimestamp(fieldPos, 9).toLocalDateTime();
                };
              }
            case STRING:
              fieldGetter = RowData::getString;
              break;
            case UUID:
            case FIXED:
            case BINARY:
              fieldGetter = RowData::getBinary;
              break;
            case DECIMAL:
              fieldGetter = ((row, fieldPos) -> {
                Types.DecimalType decimalType = (Types.DecimalType) type;
                int precision = decimalType.precision();
                int scale = decimalType.scale();
                DecimalData decimalData = row.getDecimal(fieldPos, precision, scale);
                return decimalData == null ? null : decimalData.toBigDecimal();
              });
              break;
            case LIST:
              fieldGetter = ((row, fieldPos) -> {
                ArrayData arrayData = row.getArray(fieldPos);
                // check arrayData is null or not. arrayData could be nullable.
                if (arrayData == null) {
                  return null;
                }
                return new LogFlinkArrayData(arrayData);
              });
              break;
            case MAP:
              fieldGetter = ((row, fieldPos) -> {
                MapData mapData = row.getMap(pos);
                if (mapData == null) {
                  return null;
                }
                return new LogFlinkMapData(mapData);
              });
              break;
            case STRUCT:
              fieldGetter = ((row, fieldPos) -> row.getRow(fieldPos, 0));
              break;
            default:
              throw new UnsupportedOperationException("not supported type:" + type);
          }
          return (row,fieldPos) -> {
            if (row.isNullAt(fieldPos)) {
              return null;
            }
            return fieldGetter.getFieldOrNull(row,fieldPos);
          };
        }
      };

  public static LogData.Factory<RowData> factory = new Factory<RowData>() {
    private static final long serialVersionUID = 2395276763978332852L;

    @Override
    public RowData createActualValue(Object[] objects, Type[] fieldTypes) {
      checkNotNull(objects);
      checkArgument(objects.length > 0, "can't construct a instance used by GenericRowData.");
      GenericRowData row = new GenericRowData(objects.length);
      for (int i = 0; i < objects.length; i++) {
        Object obj = objects[i];
        Type type = fieldTypes[i];
        obj = convertIfNecessary(type, obj);
        row.setField(i, obj);
      }
      return row;
    }

    public Object convertIfNecessary(Type primitiveType, Object obj) {
      if (obj == null) {
        return null;
      }
      switch (primitiveType.typeId()) {
        case STRING:
          obj = StringData.fromString(obj.toString());
          break;
        case TIME:
          obj = (int) (((long) obj) / 1000_1000);
          break;
        case TIMESTAMP:
          Types.TimestampType timestamp = (Types.TimestampType) primitiveType;
          if (timestamp.shouldAdjustToUTC()) {
            obj = TimestampData.fromInstant((Instant) obj);
          } else {
            obj = TimestampData.fromLocalDateTime((LocalDateTime) obj);
          }
          break;
        case DECIMAL:
          Types.DecimalType decimalType = (Types.DecimalType) primitiveType;
          int precision = decimalType.precision();
          int scale = decimalType.scale();
          obj = DecimalData.fromBigDecimal((BigDecimal) obj, precision, scale);
          break;
        case LIST:
          assert obj instanceof LogFlinkArrayData;
          LogFlinkArrayData list = (LogFlinkArrayData) obj;
          obj = list.arrayData;
          break;
        case MAP:
          assert obj instanceof LogFlinkMapData;
          LogFlinkMapData map = (LogFlinkMapData) obj;
          obj = map.mapData;
          break;
      }
      return obj;
    }

    @Override
    public LogData<RowData> create(RowData rowData, Object... headers) {
      FormatVersion formatVersion = FormatVersion.fromBytes((byte[]) headers[0]);
      if (formatVersion == null) {
        throw new FlinkRuntimeException(
            String.format("this is illegal format version, %s.", headers[0]));
      }

      return new LogRecordV1(
          formatVersion,
          (byte[]) headers[1],
          (long) headers[2],
          (boolean) headers[3],
          ChangeAction.fromByteValue((byte) headers[4]),
          rowData
      );
    }

    @Override
    public Class<?> getActualValueClass() {
      return RowData.class;
    }
  };

  public static LogArrayData.Factory arrayFactory = new LogArrayData.Factory() {
    private static final long serialVersionUID = 1L;

    @Override
    public LogArrayData create(Object[] array) {
      ArrayData flinkArrayData = new GenericArrayData(array);
      return new LogFlinkArrayData(flinkArrayData);
    }
  };

  public static LogMapData.Factory mapFactory = new LogMapData.Factory() {
    private static final long serialVersionUID = 1L;

    @Override
    public LogMapData create(Map<Object, Object> result) {
      MapData flinkMapData = new GenericMapData(result);
      return new LogFlinkMapData(flinkMapData);
    }
  };
}
