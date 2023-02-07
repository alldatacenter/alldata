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

import org.apache.drill.exec.record.metadata.ColumnMetadata;
import org.apache.drill.exec.vector.BaseDataValueVector;
import org.apache.drill.exec.vector.NullableVector;
import org.apache.drill.exec.vector.accessor.ColumnAccessors.UInt1ColumnWriter;
import org.apache.drill.exec.vector.accessor.ColumnReader;
import org.apache.drill.exec.vector.accessor.ColumnWriterIndex;
import org.apache.drill.exec.vector.accessor.ValueType;
import org.apache.drill.exec.vector.accessor.impl.HierarchicalFormatter;
import org.joda.time.Period;

public class NullableScalarWriter extends AbstractScalarWriterImpl {

  public static final class ChildIndex implements ColumnWriterIndex {

    private final ColumnWriterIndex parentIndex;

    public ChildIndex(ColumnWriterIndex parentIndex) {
      this.parentIndex = parentIndex;
    }

    @Override
    public int rowStartIndex() {
      return parentIndex.rowStartIndex();
    }

    @Override
    public int vectorIndex() {
      return parentIndex.vectorIndex();
    }

    @Override
    public void nextElement() {
      // Ignore next element requests from children.
      // Nullable writers have two children, we don't want
      // to increment the index twice.
    }

    @Override
    public void prevElement() { }

    @Override
    public void rollover() {
      parentIndex.rollover();
    }

    @Override
    public ColumnWriterIndex outerIndex() {
      return parentIndex.outerIndex();
    }
  }

  private final NullableVector nullableVector;
  private final UInt1ColumnWriter isSetWriter;
  private final BaseScalarWriter baseWriter;
  private ColumnWriterIndex writerIndex;

  public NullableScalarWriter(ColumnMetadata schema, NullableVector nullableVector, BaseScalarWriter baseWriter) {
    this.schema = schema;
    this.nullableVector = nullableVector;
    isSetWriter = new UInt1ColumnWriter(nullableVector.getBitsVector());
    this.baseWriter = baseWriter;
  }

  public static ScalarObjectWriter build(ColumnMetadata schema,
      NullableVector nullableVector, BaseScalarWriter baseWriter) {
    return new ScalarObjectWriter(
        new NullableScalarWriter(schema, nullableVector, baseWriter));
  }

  public BaseScalarWriter bitsWriter() { return isSetWriter; }
  public BaseScalarWriter baseWriter() { return baseWriter; }

  @Override
  public BaseDataValueVector vector() {
    throw new UnsupportedOperationException();
  }

  @Override
  public void bindIndex(ColumnWriterIndex index) {
    writerIndex = index;
    final ColumnWriterIndex childIndex = new ChildIndex(index);
    isSetWriter.bindIndex(childIndex);
    baseWriter.bindIndex(childIndex);
  }

  @Override
  public int rowStartIndex() {
    return baseWriter.rowStartIndex();
  }

  @Override
  public ValueType valueType() {
    return baseWriter.valueType();
  }

  @Override
  public void restartRow() {
    isSetWriter.restartRow();
    baseWriter.restartRow();
  }

  @Override
  public boolean nullable() { return true; }

  @Override
  public void setNull() {
    isSetWriter.setInt(0);
    baseWriter.skipNulls();
    writerIndex.nextElement();
  }

  @Override
  public void setBoolean(boolean value) {
    baseWriter.setBoolean(value);
    isSetWriter.setInt(1);
    writerIndex.nextElement();
  }

  @Override
  public void setInt(int value) {
    baseWriter.setInt(value);
    isSetWriter.setInt(1);
    writerIndex.nextElement();
  }

  @Override
  public void setLong(long value) {
    baseWriter.setLong(value);
    isSetWriter.setInt(1);
    writerIndex.nextElement();
  }

  @Override
  public void setFloat(float value) {
    baseWriter.setFloat(value);
    isSetWriter.setInt(1);
    writerIndex.nextElement();
  }

  @Override
  public void setDouble(double value) {
    baseWriter.setDouble(value);
    isSetWriter.setInt(1);
    writerIndex.nextElement();
  }

  @Override
  public void setString(String value) {
    // String may overflow. Set bits after
    // overflow since bits vector does not have
    // overflow handling separate from the nullable
    // vector as a whole.

    baseWriter.setString(value);
    isSetWriter.setInt(1);
    writerIndex.nextElement();
  }

  @Override
  public void setBytes(byte[] value, int len) {
    baseWriter.setBytes(value, len);
    isSetWriter.setInt(1);
    writerIndex.nextElement();
  }

  @Override
  public void appendBytes(byte[] value, int len) {
    baseWriter.appendBytes(value, len);
  }

  @Override
  public void setDecimal(BigDecimal value) {
    baseWriter.setDecimal(value);
    isSetWriter.setInt(1);
    writerIndex.nextElement();
  }

  @Override
  public void setPeriod(Period value) {
    baseWriter.setPeriod(value);
    isSetWriter.setInt(1);
    writerIndex.nextElement();
  }

  @Override
  public void setDate(LocalDate value) {
    baseWriter.setDate(value);
    isSetWriter.setInt(1);
    writerIndex.nextElement();
  }

  @Override
  public void setTime(LocalTime value) {
    baseWriter.setTime(value);
    isSetWriter.setInt(1);
    writerIndex.nextElement();
  }

  @Override
  public void setTimestamp(Instant value) {
    baseWriter.setTimestamp(value);
    isSetWriter.setInt(1);
    writerIndex.nextElement();
  }

  @Override
  public void setValue(Object value) {
    if (value == null) {
      setNull();
    } else {
      baseWriter.setValue(value);
      isSetWriter.setInt(1);
      writerIndex.nextElement();
    }
  }

  @Override
  public void copy(ColumnReader from) {
    if (!from.isNull()) {
      isSetWriter.setInt(1);
      baseWriter.copy(from);
    }
  }

  @Override
  public void preRollover() {
    isSetWriter.preRollover();
    baseWriter.preRollover();
  }

  @Override
  public void postRollover() {
    isSetWriter.postRollover();
    baseWriter.postRollover();
  }

  @Override
  public int lastWriteIndex() {
    return baseWriter.lastWriteIndex();
  }

  @Override
  public void bindListener(ColumnWriterListener listener) {
    baseWriter.bindListener(listener);
  }

  @Override
  public void startWrite() {
    isSetWriter.startWrite();
    baseWriter.startWrite();
  }

  @Override
  public void startRow() {
    // Skip calls for performance: they do nothing for
    // scalar writers -- the only kind supported here.
//    isSetWriter.startRow();
    baseWriter.startRow();
  }

  @Override
  public void endArrayValue() {
    // Skip calls for performance: they do nothing for
    // scalar writers -- the only kind supported here.
//    isSetWriter.endArrayValue();
    baseWriter.endArrayValue();
  }

  @Override
  public void endWrite() {
    isSetWriter.endWrite();
    // Avoid back-filling null values.
    baseWriter.skipNulls();
    baseWriter.endWrite();
    ((NullableVector.Mutator) nullableVector.getMutator())
        .setSetCount(writerIndex.vectorIndex());
  }

  @Override
  public void dump(HierarchicalFormatter format) {
    format.extend();
    super.dump(format);
    format.attribute("isSetWriter");
    isSetWriter.dump(format);
    format.attribute("baseWriter");
    baseWriter.dump(format);
    format.endObject();
  }

  @Override
  public void setDefaultValue(Object value) {
    throw new UnsupportedOperationException(
        "Default values not supported for nullable types: " + value);
  }
}
