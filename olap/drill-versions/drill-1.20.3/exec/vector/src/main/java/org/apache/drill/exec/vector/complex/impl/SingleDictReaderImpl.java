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
package org.apache.drill.exec.vector.complex.impl;

import org.apache.drill.common.types.TypeProtos;
import org.apache.drill.common.types.TypeProtos.MinorType;
import org.apache.drill.exec.expr.fn.impl.DateUtility;
import org.apache.drill.exec.expr.holders.ValueHolder;
import org.apache.drill.exec.util.Text;
import org.apache.drill.exec.vector.ValueVector;
import org.apache.drill.exec.vector.complex.DictVector;
import org.apache.drill.exec.vector.complex.reader.BaseReader.DictReader;
import org.apache.drill.exec.vector.complex.reader.FieldReader;
import org.apache.drill.exec.vector.complex.writer.BaseWriter.ListWriter;
import org.apache.drill.exec.vector.complex.writer.BaseWriter.DictWriter;
import org.apache.drill.exec.vector.complex.writer.FieldWriter;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class SingleDictReaderImpl extends AbstractRepeatedMapReaderImpl<DictVector> implements DictReader {

  public static final int NOT_FOUND = -1;

  public SingleDictReaderImpl(DictVector vector) {
    super(vector);
  }

  @Override
  public FieldReader reader(String name){
    assert DictVector.fieldNames.contains(name);
    return super.reader(name);
  }

  @Override
  public int find(String key) {
    Object typifiedKey = getAppropriateKey(key);
    return find(typifiedKey);
  }

  @Override
  public int find(int key) {
    Object typifiedKey = getAppropriateKey(key);
    return find(typifiedKey);
  }

  @Override
  public int find(Object key) {
    int start = vector.getOffsetVector().getAccessor().get(idx());
    int end = vector.getOffsetVector().getAccessor().get(idx() + 1);
    int index = NOT_FOUND;
    ValueVector keys = vector.getKeys();

    // start from the end to ensure the most recent value for a key is found (in case if key is not unique)
    for (int i = end - 1; i >= start; i--) {
      Object keyValue = keys.getAccessor().getObject(i);
      if (keyValue.equals(key)) {
        index = i;
        break;
      }
    }

    return index;
  }

  private Object getAppropriateKey(int key) {
    TypeProtos.MajorType keyType = vector.getKeyType();
    switch (keyType.getMinorType()) {
      case SMALLINT:
        return (short) key;
      case INT:
        return key;
      case BIGINT:
        return (long) key;
      case FLOAT4:
        return (float) key;
      case FLOAT8:
        return (double) key;
      case VARDECIMAL:
        return BigDecimal.valueOf(key)
            .setScale(keyType.getScale(), RoundingMode.HALF_UP);
      case BIT:
        return key != 0;
      case VARCHAR:
      case VARBINARY:
        return new Text(String.valueOf(key));
      default:
        String message = String.format("Unknown value %d for key of type %s", key, keyType.getMinorType().toString());
        throw new IllegalArgumentException(message);
    }
  }

  private Object getAppropriateKey(String key) {
    TypeProtos.MajorType keyType = vector.getKeyType();
    switch (keyType.getMinorType()) {
      case VARCHAR:
      case VARBINARY:
        return new Text(key);
      case BIT:
        return Boolean.valueOf(key);
      case SMALLINT:
        return Short.valueOf(key);
      case INT:
        return Integer.valueOf(key);
      case BIGINT:
        return Long.valueOf(key);
      case FLOAT4:
        return Float.valueOf(key);
      case FLOAT8:
        return Double.valueOf(key);
      case VARDECIMAL:
        return BigDecimal.valueOf(Double.valueOf(key))
            .setScale(keyType.getScale(), RoundingMode.HALF_UP);
      case TIMESTAMP:
        return DateUtility.parseBest(key);
      case DATE:
        return DateUtility.parseLocalDate(key);
      case TIME:
        return DateUtility.parseLocalTime(key);
      default:
        String message = String.format("Unknown value %s for key of type %s", key, keyType.getMinorType().toString());
        throw new IllegalArgumentException(message);
    }
  }

  @Override
  public void read(String key, ValueHolder holder) {
    Object typifiedKey = getAppropriateKey(key);
    read(typifiedKey, holder);
  }

  @Override
  public void read(int key, ValueHolder holder) {
    Object typifiedKey = getAppropriateKey(key);
    read(typifiedKey, holder);
  }

  @Override
  public void read(Object key, ValueHolder holder) {
    if (isEmpty()) {
      return;
    }

    int index = find(key);
    FieldReader valueReader = reader(DictVector.FIELD_VALUE_NAME);
    valueReader.setPosition(index);
    if (index != NOT_FOUND) {
      valueReader.read(holder);
    }
  }

  @Override
  public void setPosition(int index) {
    if (index == NOT_FOUND) {
      for (FieldReader reader : fields.values()) {
        reader.setPosition(index);
      }
    }
    super.setPosition(index);
  }

  @Override
  public void copyAsValue(DictWriter writer) {
    if (isEmpty()) {
      return;
    }
    ComplexCopier.copy(this, (FieldWriter) writer);
  }

  @Override
  public void copyAsValue(ListWriter writer) {
    ComplexCopier.copy(this, (FieldWriter) writer.dict());
  }

  @Override
  public String getTypeString() {
    StringBuilder sb = new StringBuilder(super.getTypeString());
    // child readers may be empty so vector is used instead to get key and value type
    if (vector.getKeyType() != null && vector.getValueType() != null) {
      sb.append('<')
          .append(vector.getKeyType().getMinorType().name())
          .append(',')
          .append(vector.getValueType().getMinorType().name())
          .append('>');
    }
    return sb.toString();
  }

  public MinorType getVectorType() {
    return vector.getField().getType().getMinorType();
  }
}
