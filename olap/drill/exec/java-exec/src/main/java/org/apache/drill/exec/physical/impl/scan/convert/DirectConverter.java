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
package org.apache.drill.exec.physical.impl.scan.convert;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

import org.apache.drill.exec.vector.accessor.ScalarWriter;
import org.apache.drill.exec.vector.accessor.ValueWriter;
import org.joda.time.Period;

/**
 * Base class for Java type-based conversion. Use this base class if your
 * reader works with Java types, but you need to convert from one Java
 * type to another (such as decoding a byte array, converting a string
 * to a number, etc.)
 * <p>
 * Instances of this class can be freely mixed with "plain"
 * {@code ScalarWriter} instances to avoid unnecessary calls when there
 * is a direct mapping from reader Java type to Drill type. You only need
 * insert converters for those columns where some conversion step is needed.
 */
public class DirectConverter extends ColumnConverter implements ValueWriter {

  public DirectConverter(ScalarWriter colWriter) {
    super(colWriter);
  }

  @Override
  public void setNull() {
    baseWriter.setNull();
  }

  @Override
  public void setBoolean(boolean value) {
    baseWriter.setBoolean(value);
  }

  @Override
  public void setInt(int value) {
    baseWriter.setInt(value);
  }

  @Override
  public void setLong(long value) {
    baseWriter.setLong(value);
  }

  @Override
  public void setFloat(float value) {
    baseWriter.setDouble(value);
  }

  @Override
  public void setDouble(double value) {
    baseWriter.setDouble(value);
  }

  @Override
  public void setString(String value) {
    baseWriter.setString(value);
  }

  @Override
  public void setBytes(byte[] value, int len) {
    baseWriter.setBytes(value, len);
  }

  @Override
  public void appendBytes(byte[] value, int len) {
    throw conversionError("bytes");
  }

  @Override
  public void setDecimal(BigDecimal value) {
    baseWriter.setDecimal(value);
  }

  @Override
  public void setPeriod(Period value) {
    baseWriter.setPeriod(value);
  }

  @Override
  public void setDate(LocalDate value) {
    baseWriter.setDate(value);
  }

  @Override
  public void setTime(LocalTime value) {
    baseWriter.setTime(value);
  }

  @Override
  public void setTimestamp(Instant value) {
    baseWriter.setTimestamp(value);
  }

  @Override
  public void setValue(Object value) {
    baseWriter.setValue(value);
  }
}
