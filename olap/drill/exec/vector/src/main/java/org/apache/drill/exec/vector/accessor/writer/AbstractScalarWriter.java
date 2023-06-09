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
package org.apache.drill.exec.vector.accessor.writer;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Date;

import org.apache.drill.exec.vector.accessor.ScalarWriter;
import org.apache.drill.exec.vector.accessor.UnsupportedConversionError;
import org.apache.drill.exec.vector.accessor.ValueType;
import org.apache.drill.exec.vector.accessor.writer.WriterEvents.ColumnWriterListener;
import org.joda.time.Period;

/**
 * Base class for concrete scalar column writers including actual vector
 * writers, and wrappers for nullable types.
 */
public abstract class AbstractScalarWriter implements ScalarWriter {

  @Override
  public ValueType extendedType() { return valueType(); }

  @Override
  public void setObject(Object value) {
    if (value == null) {
      setNull();
    } else if (value instanceof Integer) {
      setInt((Integer) value);
    } else if (value instanceof Long) {
      setLong((Long) value);
    } else if (value instanceof String) {
      setString((String) value);
    } else if (value instanceof Double) {
      setDouble((Double) value);
    } else if (value instanceof Float) {
      setFloat((Float) value);
    } else if (value instanceof BigDecimal) {
      setDecimal((BigDecimal) value);
    } else if (value instanceof Period) {
      setPeriod((Period) value);
    } else if (value instanceof LocalTime) {
      setTime((LocalTime) value);
    } else if (value instanceof LocalDate) {
      setDate((LocalDate) value);
    } else if (value instanceof Instant) {
      setTimestamp((Instant) value);
    } else if (value instanceof Date) {
      setTimestamp(Instant.ofEpochMilli(((Date) value).getTime()));
    } else if (value instanceof byte[]) {
      final byte[] bytes = (byte[]) value;
      setBytes(bytes, bytes.length);
    } else if (value instanceof Byte) {
      setInt((Byte) value);
    } else if (value instanceof Short) {
      setInt((Short) value);
    } else if (value instanceof Boolean) {
      setBoolean((boolean) value);
    } else {
      throw conversionError(value.getClass().getSimpleName());
    }
  }

  protected UnsupportedConversionError conversionError(String javaType) {
    return UnsupportedConversionError.writeError(schema(), javaType);
  }

  public void bindListener(ColumnWriterListener listener) { }

  @Override
  public String toString() {
    return "[" + getClass().getSimpleName() +
        schema().toString() +
        ", projected=" + isProjected() + "]";
  }
}
