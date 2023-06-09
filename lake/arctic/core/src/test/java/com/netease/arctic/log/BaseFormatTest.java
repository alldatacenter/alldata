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

package com.netease.arctic.log;

import com.netease.arctic.data.ChangeAction;
import com.netease.arctic.log.data.LogArrayData;
import com.netease.arctic.log.data.LogMapData;
import org.apache.iceberg.Schema;
import org.apache.iceberg.relocated.com.google.common.base.Preconditions;
import org.apache.iceberg.types.Type;
import org.apache.iceberg.types.Types;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;

public class BaseFormatTest {
  final LogData.FieldGetterFactory<UserPojo> fieldGetterFactory =
      (type, fieldPos) -> {
        switch (type.typeId()) {
          case BOOLEAN:
            return UserPojo::getBoolean;
          case INTEGER:
            return UserPojo::getInt;
          case LONG:
            return UserPojo::getLong;
          case STRUCT:
            return UserPojo::getUserPojo;
          case FLOAT:
            return UserPojo::getFloat;
          case DOUBLE:
            return UserPojo::getDouble;
          case DATE:
            return UserPojo::getDate;
          case TIME:
            return UserPojo::getTime;
          case TIMESTAMP:
            Types.TimestampType timestamp = (Types.TimestampType) type;
            if (timestamp.shouldAdjustToUTC()) {
              return UserPojo::getInstant;
            } else {
              return UserPojo::getTimestamp;
            }
          case STRING:
            return UserPojo::getString;
          case UUID:
            return UserPojo::getUUID;
          case FIXED:
            return UserPojo::getFixed;
          case BINARY:
            return UserPojo::getBinary;
          case DECIMAL:
            return UserPojo::getDecimal;
          case LIST:
            return UserPojo::getArray;
          case MAP:
            return UserPojo::getMap;

          default:
            throw new UnsupportedOperationException("not support type:" + type);
        }
      };

  final Schema userSchema = new Schema(
      new ArrayList<Types.NestedField>() {
        {
          add(Types.NestedField.optional(0, "f_boolean", Types.BooleanType.get()));
          add(Types.NestedField.optional(1, "f_int", Types.IntegerType.get()));
          add(Types.NestedField.optional(2, "f_long", Types.LongType.get()));
          add(Types.NestedField.optional(3, "f_struct", Types.StructType.of(
              Types.NestedField.optional(4, "f_sub_boolean", Types.BooleanType.get()),
              Types.NestedField.optional(5, "f_sub_int", Types.IntegerType.get()),
              Types.NestedField.optional(6, "f_sub_long", Types.LongType.get())
          )));
          add(Types.NestedField.optional(7, "f_float", Types.FloatType.get()));
          add(Types.NestedField.optional(8, "f_double", Types.DoubleType.get()));
          add(Types.NestedField.optional(9, "f_date", Types.DateType.get()));
          add(Types.NestedField.optional(10, "f_time", Types.TimeType.get()));
          add(Types.NestedField.optional(11, "f_timestamp_local", Types.TimestampType.withoutZone()));
          add(Types.NestedField.optional(12, "f_timestamp_tz", Types.TimestampType.withZone()));
          add(Types.NestedField.optional(13, "f_string", Types.StringType.get()));
          add(Types.NestedField.optional(14, "f_uuid", Types.UUIDType.get()));
          add(Types.NestedField.optional(15, "f_fixed", Types.FixedType.ofLength(10)));
          add(Types.NestedField.optional(16, "f_binary", Types.BinaryType.get()));
          add(Types.NestedField.optional(17, "f_decimal", Types.DecimalType.of(10, 5)));
          add(Types.NestedField.optional(18, "f_list", Types.ListType.ofOptional(
              19, Types.LongType.get()
          )));
          add(Types.NestedField.optional(20, "f_list2", Types.ListType.ofOptional(
              21, Types.IntegerType.get()
          )));
          add(Types.NestedField.optional(22, "f_list3", Types.ListType.ofOptional(
              23, Types.StructType.of(
                  Types.NestedField.optional(24, "f_sub_boolean", Types.BooleanType.get()),
                  Types.NestedField.optional(25, "f_sub_int", Types.IntegerType.get()),
                  Types.NestedField.optional(26, "f_sub_long", Types.LongType.get())
              ))));
          add(Types.NestedField.optional(27, "f_map", Types.MapType.ofOptional(
              28, 29, Types.LongType.get(), Types.StringType.get()
          )));
        }
      });

  LogData.Factory<UserPojo> factory = new LogData.Factory<UserPojo>() {
    @Override
    public UserPojo createActualValue(Object[] objects, Type[] fieldTypes) {
      UserPojo userPojo = new UserPojo();
      userPojo.objects = objects;
      return userPojo;
    }

    @Override
    public LogData<UserPojo> create(UserPojo userPojo, Object... headers) {
      Preconditions.checkNotNull(headers);
      Preconditions.checkArgument(headers.length == 5);
      return new LogDataUser(
          (byte[]) headers[0],
          (byte[]) headers[1],
          (long) headers[2],
          (boolean) headers[3],
          ChangeAction.fromByteValue((byte) headers[4]),
          userPojo
      );
    }

    @Override
    public Class<?> getActualValueClass() {
      return UserPojo.class;
    }

    @Override
    public Object convertIfNecessary(Type primitiveType, Object obj) {
      return obj;
    }
  };

  LogArrayData.Factory arrayFactory = GenericArrayData::new;

  LogMapData.Factory mapFactory = GenericMapData::new;

  class GenericArrayData implements LogArrayData {
    private final Object array;
    private final boolean isPrimitiveArray;
    private final int size;

    public GenericArrayData(Object[] array) {
      this(array, array.length, false);
    }

    public GenericArrayData(Object array, int size, boolean isPrimitiveArray) {
      this.array = array;
      this.size = size;
      this.isPrimitiveArray = isPrimitiveArray;
    }

    @Override
    public boolean getBoolean(int pos) {
      return isPrimitiveArray ? ((boolean[]) array)[pos] : (boolean) getObject(pos);
    }

    @Override
    public int getInt(int pos) {
      return isPrimitiveArray ? ((int[]) array)[pos] : (int) getObject(pos);
    }

    @Override
    public long getLong(int pos) {
      return isPrimitiveArray ? ((long[]) array)[pos] : (long) getObject(pos);
    }

    @Override
    public float getFloat(int pos) {
      return isPrimitiveArray ? ((float[]) array)[pos] : (float) getObject(pos);
    }

    @Override
    public double getDouble(int pos) {
      return isPrimitiveArray ? ((double[]) array)[pos] : (double) getObject(pos);
    }

    @Override
    public LocalDateTime getTimestamp(int pos) {
      return (LocalDateTime) getObject(pos);
    }

    @Override
    public Instant getInstant(int pos) {
      return (Instant) getObject(pos);
    }

    @Override
    public byte[] getBinary(int pos) {
      return (byte[]) getObject(pos);
    }

    @Override
    public String getString(int pos) {
      return (String) getObject(pos);
    }

    @Override
    public BigDecimal getDecimal(int pos) {
      return (BigDecimal) getObject(pos);
    }

    @Override
    public LogArrayData getArray(int pos) {
      return (LogArrayData) getObject(pos);
    }

    @Override
    public LogMapData getMap(int pos) {
      return (LogMapData) getObject(pos);
    }

    @Override
    public Object getStruct(int pos) {
      return getObject(pos);
    }

    @Override
    public boolean isNullAt(int pos) {
      return !isPrimitiveArray && ((Object[]) array)[pos] == null;
    }

    @Override
    public int size() {
      return size;
    }

    private Object getObject(int pos) {
      return ((Object[]) array)[pos];
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      GenericArrayData that = (GenericArrayData) o;
      return size == that.size &&
          isPrimitiveArray == that.isPrimitiveArray &&
          Objects.deepEquals(array, that.array);
    }

    @Override
    public int hashCode() {
      int result = Objects.hash(size, isPrimitiveArray);
      result = 31 * result + Arrays.deepHashCode(new Object[] {array});
      return result;
    }

    @Override
    public String toString() {
      return "GenericArrayData = ";
    }
  }

  class UserPojo {
    Object[] objects;

    int getInt(int pos) {
      return (int) objects[pos];
    }

    boolean getBoolean(int pos) {
      return (boolean) objects[pos];
    }

    long getLong(int pos) {
      return (long) objects[pos];
    }

    float getFloat(int pos) {
      return (float) objects[pos];
    }

    double getDouble(int pos) {
      return (double) objects[pos];
    }

    int getDate(int pos) {
      return (int) objects[pos];
    }

    long getTime(int pos) {
      return (long) objects[pos];
    }

    LocalDateTime getTimestamp(int pos) {
      return (LocalDateTime) objects[pos];
    }

    Instant getInstant(int pos) {
      return (Instant) objects[pos];
    }

    String getString(int pos) {
      return (String) objects[pos];
    }

    byte[] getUUID(int pos) {
      return (byte[]) objects[pos];
    }

    byte[] getFixed(int pos) {
      return (byte[]) objects[pos];
    }

    byte[] getBinary(int pos) {
      return (byte[]) objects[pos];
    }

    BigDecimal getDecimal(int pos) {
      return (BigDecimal) objects[pos];
    }

    UserPojo getUserPojo(int pos) {
      return (UserPojo) objects[pos];
    }

    LogArrayData getArray(int i) {
      return (LogArrayData) objects[i];
    }

    LogMapData getMap(int i) {
      return (LogMapData) objects[i];
    }

    @Override
    public String toString() {
      return "UserPojo = " + Arrays.deepToString(objects);
    }
  }

  class GenericMapData implements LogMapData {
    private final Map<?, ?> map;

    public GenericMapData(Map<?, ?> map) {
      this.map = map;
    }

    @Override
    public int size() {
      return map.size();
    }

    @Override
    public LogArrayData keyArray() {
      Object[] keys = map.keySet().toArray();
      return new GenericArrayData(keys);
    }

    @Override
    public LogArrayData valueArray() {
      Object[] values = map.values().toArray();
      return new GenericArrayData(values);
    }

    @Override
    public boolean equals(Object o) {
      if (o == this) {
        return true;
      }
      if (!(o instanceof GenericMapData)) {
        return false;
      }
      // deepEquals for values of byte[]
      return deepEquals(map, ((GenericMapData) o).map);
    }

    private <K, V> boolean deepEquals(Map<K, V> m1, Map<?, ?> m2) {
      // copied from HashMap.equals but with deepEquals comparision
      if (m1.size() != m2.size()) {
        return false;
      }
      try {
        for (Map.Entry<K, V> e : m1.entrySet()) {
          K key = e.getKey();
          V value = e.getValue();
          if (value == null) {
            if (!(m2.get(key) == null && m2.containsKey(key))) {
              return false;
            }
          } else {
            if (!Objects.deepEquals(value, m2.get(key))) {
              return false;
            }
          }
        }
      } catch (ClassCastException | NullPointerException unused) {
        return false;
      }
      return true;
    }

    @Override
    public int hashCode() {
      int result = 0;
      for (Object key : map.keySet()) {
        // only include key because values can contain byte[]
        result += 31 * Objects.hashCode(key);
      }
      return result;
    }

    @Override
    public String toString() {
      return "GenericMapData = " + (map.toString());
    }
  }

  class LogDataUser implements LogData<UserPojo> {
    byte[] version;
    byte[] upstreamId;
    long epicNo;
    boolean flip;
    ChangeAction changeAction;
    UserPojo userPojo;

    public LogDataUser(
        byte[] version,
        byte[] upstreamId,
        long epicNo,
        boolean flip,
        ChangeAction changeAction,
        UserPojo userPojo
    ) {
      this.version = version;
      this.upstreamId = upstreamId;
      this.epicNo = epicNo;
      this.flip = flip;
      this.changeAction = changeAction;
      this.userPojo = userPojo;
    }

    @Override
    public byte[] getVersionBytes() {
      return version;
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
    public boolean getFlip() {
      return flip;
    }

    @Override
    public ChangeAction getChangeAction() {
      return changeAction;
    }

    @Override
    public UserPojo getActualValue() {
      return userPojo;
    }
  }
}
