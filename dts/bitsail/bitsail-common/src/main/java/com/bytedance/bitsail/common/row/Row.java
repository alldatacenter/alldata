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

package com.bytedance.bitsail.common.row;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Setter
@Getter
public class Row implements Serializable {

  private final Object[] fields;
  private RowKind kind = RowKind.INSERT;

  public Row(Object[] fields) {
    this.fields = fields;
  }

  public Row(int arity) {
    this.kind = RowKind.fromByteValue(RowKind.INSERT.toByteValue());
    this.fields = new Object[arity];
  }

  public Row(byte kind, Object[] fields) {
    this.kind = RowKind.fromByteValue(kind);
    this.fields = fields;
  }

  public void setField(int pos, Object value) {
    this.fields[pos] = value;
  }

  public Object getField(int pos) {
    return this.fields[pos];
  }

  public Object[] getFields() {
    return fields;
  }

  public int getArity() {
    return fields.length;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Row row = (Row) o;
    return kind == row.kind && Arrays.equals(fields, row.fields);
  }

  @Override
  public int hashCode() {
    int result = Objects.hash(kind);
    result = 31 * result + Arrays.hashCode(fields);
    return result;
  }

  public boolean getBoolean(int pos) {
    return (Boolean) this.fields[pos];
  }

  public byte getByte(int pos) {
    return (Byte) this.fields[pos];
  }

  public short getShort(int pos) {
    return (Short) this.fields[pos];
  }

  public int getInt(int pos) {
    return (Integer) this.fields[pos];
  }

  public Date getDate(int pos) {
    return (Date) this.fields[pos];
  }

  public long getLong(int pos) {
    return (Long) this.fields[pos];
  }

  public BigInteger getBigInteger(int pos) {
    return (BigInteger) this.fields[pos];
  }

  public float getFloat(int pos) {
    return (Float) this.fields[pos];
  }

  public double getDouble(int pos) {
    return (Double) this.fields[pos];
  }

  public String getString(int pos) {
    return (String) this.fields[pos];
  }

  public BigDecimal getDecimal(int pos, int precision, int scale) {
    return (BigDecimal) this.fields[pos];
  }

  public Timestamp getTimestamp(int pos, int precision) {
    return (Timestamp) this.fields[pos];
  }

  public byte[] getBinary(int pos) {
    return ((byte[]) this.fields[pos]);
  }

  public List getArray(int pos) {
    return (List) this.fields[pos];
  }

  public Map getMap(int pos) {
    return (Map) this.fields[pos];
  }

  public boolean isNullAt(int pos) {
    return this.fields[pos] == null;
  }

}
