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
package org.apache.drill.exec.vector.accessor.reader;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

import org.apache.drill.exec.record.metadata.ColumnMetadata;
import org.apache.drill.exec.vector.accessor.ColumnReader;
import org.apache.drill.exec.vector.accessor.ColumnReaderIndex;
import org.apache.drill.exec.vector.accessor.ObjectType;
import org.apache.drill.exec.vector.accessor.ScalarReader;
import org.apache.drill.exec.vector.accessor.UnsupportedConversionError;
import org.apache.drill.exec.vector.accessor.ValueType;
import org.apache.drill.exec.vector.accessor.impl.AccessorUtilities;
import org.joda.time.Period;

public abstract class AbstractScalarReader implements ScalarReader, ReaderEvents {

  public static class ScalarObjectReader extends AbstractObjectReader {

    private final AbstractScalarReader scalarReader;

    public ScalarObjectReader(AbstractScalarReader scalarReader) {
      this.scalarReader = scalarReader;
    }

    @Override
    public ScalarReader scalar() {
      return scalarReader;
    }

    @Override
    public Object getObject() {
      return scalarReader.getObject();
    }

    @Override
    public String getAsString() {
      return scalarReader.getAsString();
    }

    @Override
    public ReaderEvents events() { return scalarReader; }

    @Override
    public ColumnReader reader() { return scalarReader; }
  }

  public static class NullReader extends AbstractScalarReader {

    protected final ColumnMetadata schema;

    protected NullReader(ColumnMetadata schema) {
      this.schema = schema;
    }

    @Override
    public ValueType valueType() { return ValueType.NULL; }

    @Override
    public boolean isNull() { return true; }

    @Override
    public void bindIndex(ColumnReaderIndex rowIndex) { }

    @Override
    public ColumnMetadata schema() { return schema; }

    @Override
    public void bindBuffer() { }
  }

  protected ColumnReaderIndex vectorIndex;
  protected NullStateReader nullStateReader;

  public static ScalarObjectReader nullReader(ColumnMetadata schema) {
    return new ScalarObjectReader(new NullReader(schema));
  }

  @Override
  public void bindIndex(ColumnReaderIndex rowIndex) {
    vectorIndex = rowIndex;
    nullStateReader.bindIndex(rowIndex);
  }

  @Override
  public void bindNullState(NullStateReader nullStateReader) {
    this.nullStateReader = nullStateReader;
  }

  @Override
  public ObjectType type() { return ObjectType.SCALAR; }

  @Override
  public ValueType extendedType() { return valueType(); }

  @Override
  public NullStateReader nullStateReader() { return nullStateReader; }

  @Override
  public void reposition() { }

  @Override
  public boolean isNull() {
    return nullStateReader.isNull();
  }

  protected UnsupportedConversionError conversionError(String javaType) {
    return UnsupportedConversionError.writeError(schema(), javaType);
  }

  @Override
  public boolean getBoolean() {
    throw conversionError("boolean");
  }

  @Override
  public int getInt() {
    throw conversionError("int");
  }

  @Override
  public long getLong() {
    throw conversionError("long");
  }

  @Override
  public float getFloat() {
    throw conversionError("double");
  }

  @Override
  public double getDouble() {
    throw conversionError("double");
  }

  @Override
  public String getString() {
    throw conversionError("String");
  }

  @Override
  public byte[] getBytes() {
    throw conversionError("bytes");
  }

  @Override
  public BigDecimal getDecimal() {
    throw conversionError("Decimal");
  }

  @Override
  public Period getPeriod() {
    throw conversionError("Period");
  }

  @Override
  public LocalDate getDate() {
    throw conversionError("Date");
  }

  @Override
  public LocalTime getTime() {
    throw conversionError("Time");
  }

  @Override
  public Instant getTimestamp() {
    throw conversionError("Timestamp");
  }

  @Override
  public Object getObject() {
    if (isNull()) {
      return null;
    }
    switch (valueType()) {
    case BOOLEAN:
      return getBoolean();
    case BYTES:
      return getBytes();
    case DECIMAL:
      return getDecimal();
    case FLOAT:
      return getFloat();
    case DOUBLE:
      return getDouble();
    case INTEGER:
      return getInt();
    case LONG:
      return getLong();
    case PERIOD:
      return getPeriod();
    case STRING:
      return getString();
    case DATE:
      return getDate();
    case TIME:
      return getTime();
    case TIMESTAMP:
      return getTimestamp();
    default:
      throw new IllegalStateException("Unexpected type: " + valueType());
    }
  }

  @Override
  public Object getValue() {
    if (isNull()) {
      return null;
    }
    switch (extendedType()) {
    case DATE:
      return getDate();
    case TIME:
      return getTime();
    case TIMESTAMP:
      return getTimestamp();
    default:
      return getObject();
    }
  }

  @Override
  public String getAsString() {
    if (isNull()) {
      return "null";
    }
    switch (extendedType()) {
    case BYTES:
      return AccessorUtilities.bytesToString(getBytes());
    case FLOAT:
      return Double.toString(getFloat());
    case DOUBLE:
      return Double.toString(getDouble());
    case INTEGER:
      return Integer.toString(getInt());
    case LONG:
      return Long.toString(getLong());
    case STRING:
      return "\"" + getString() + "\"";
    case DECIMAL:
      return getDecimal().toPlainString();
    case PERIOD:
      return getPeriod().normalizedStandard().toString();
    default:
      return getValue().toString();
    }
  }
}
