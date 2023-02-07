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

import org.apache.drill.exec.vector.complex.writer.BaseWriter;
import org.apache.drill.exec.vector.complex.writer.BaseWriter.MapOrListWriter;
import org.apache.drill.exec.vector.complex.writer.BigIntWriter;
import org.apache.drill.exec.vector.complex.writer.BitWriter;
import org.apache.drill.exec.vector.complex.writer.DateWriter;
import org.apache.drill.exec.vector.complex.writer.Decimal18Writer;
import org.apache.drill.exec.vector.complex.writer.Decimal28DenseWriter;
import org.apache.drill.exec.vector.complex.writer.Decimal28SparseWriter;
import org.apache.drill.exec.vector.complex.writer.Decimal38DenseWriter;
import org.apache.drill.exec.vector.complex.writer.Decimal38SparseWriter;
import org.apache.drill.exec.vector.complex.writer.Decimal9Writer;
import org.apache.drill.exec.vector.complex.writer.VarDecimalWriter;
import org.apache.drill.exec.vector.complex.writer.Float4Writer;
import org.apache.drill.exec.vector.complex.writer.Float8Writer;
import org.apache.drill.exec.vector.complex.writer.IntWriter;
import org.apache.drill.exec.vector.complex.writer.IntervalDayWriter;
import org.apache.drill.exec.vector.complex.writer.IntervalWriter;
import org.apache.drill.exec.vector.complex.writer.IntervalYearWriter;
import org.apache.drill.exec.vector.complex.writer.SmallIntWriter;
import org.apache.drill.exec.vector.complex.writer.TimeStampWriter;
import org.apache.drill.exec.vector.complex.writer.TimeWriter;
import org.apache.drill.exec.vector.complex.writer.TinyIntWriter;
import org.apache.drill.exec.vector.complex.writer.UInt1Writer;
import org.apache.drill.exec.vector.complex.writer.UInt2Writer;
import org.apache.drill.exec.vector.complex.writer.UInt4Writer;
import org.apache.drill.exec.vector.complex.writer.UInt8Writer;
import org.apache.drill.exec.vector.complex.writer.Var16CharWriter;
import org.apache.drill.exec.vector.complex.writer.VarBinaryWriter;
import org.apache.drill.exec.vector.complex.writer.VarCharWriter;

public class MapOrListWriterImpl implements MapOrListWriter {

  public final BaseWriter.MapWriter map;
  public final BaseWriter.ListWriter list;

  public MapOrListWriterImpl(final BaseWriter.MapWriter writer) {
    this.map = writer;
    this.list = null;
  }

  public MapOrListWriterImpl(final BaseWriter.ListWriter writer) {
    this.map = null;
    this.list = writer;
  }

  public void start() {
    if (map != null) {
      map.start();
    } else {
      list.startList();
    }
  }

  public void end() {
    if (map != null) {
      map.end();
    } else {
      list.endList();
    }
  }

  public MapOrListWriter map(final String name) {
    assert map != null;
    return new MapOrListWriterImpl(map.map(name));
  }

  public MapOrListWriter listoftmap(final String name) {
    assert list != null;
    return new MapOrListWriterImpl(list.map());
  }

  @Override
  public MapOrListWriter dict(String name) {
    return new MapOrListWriterImpl(map != null ? map.dict(name) : list.dict());
  }

  @Override
  public MapOrListWriter listOfDict() {
    assert list != null;
    return new MapOrListWriterImpl(list.dict());
  }

  public MapOrListWriter list(final String name) {
    assert map != null;
    return new MapOrListWriterImpl(map.list(name));
  }

  public boolean isMapWriter() {
    return map != null;
  }

  public boolean isListWriter() {
    return list != null;
  }

  public VarCharWriter varChar(final String name) {
    return (map != null) ? map.varChar(name) : list.varChar();
  }

  public IntWriter integer(final String name) {
    return (map != null) ? map.integer(name) : list.integer();
  }

  public BigIntWriter bigInt(final String name) {
    return (map != null) ? map.bigInt(name) : list.bigInt();
  }

  public Float4Writer float4(final String name) {
    return (map != null) ? map.float4(name) : list.float4();
  }

  public Float8Writer float8(final String name) {
    return (map != null) ? map.float8(name) : list.float8();
  }

  public BitWriter bit(final String name) {
    return (map != null) ? map.bit(name) : list.bit();
  }

  /**
   * {@inheritDoc}
   */
  @Deprecated
  public VarBinaryWriter binary(final String name) {
    return (map != null) ? map.varBinary(name) : list.varBinary();
  }

  @Override
  public TinyIntWriter tinyInt(String name) {
    return (map != null) ? map.tinyInt(name) : list.tinyInt();
  }

  @Override
  public SmallIntWriter smallInt(String name) {
    return (map != null) ? map.smallInt(name) : list.smallInt();
  }

  @Override
  public DateWriter date(String name) {
    return (map != null) ? map.date(name) : list.date();
  }

  @Override
  public TimeWriter time(String name) {
    return (map != null) ? map.time(name) : list.time();
  }

  @Override
  public TimeStampWriter timeStamp(String name) {
    return (map != null) ? map.timeStamp(name) : list.timeStamp();
  }

  @Override
  public VarBinaryWriter varBinary(String name) {
    return (map != null) ? map.varBinary(name) : list.varBinary();
  }

  @Override
  public Var16CharWriter var16Char(String name) {
    return (map != null) ? map.var16Char(name) : list.var16Char();
  }

  @Override
  public UInt1Writer uInt1(String name) {
    return (map != null) ? map.uInt1(name) : list.uInt1();
  }

  @Override
  public UInt2Writer uInt2(String name) {
    return (map != null) ? map.uInt2(name) : list.uInt2();
  }

  @Override
  public UInt4Writer uInt4(String name) {
    return (map != null) ? map.uInt4(name) : list.uInt4();
  }

  @Override
  public UInt8Writer uInt8(String name) {
    return (map != null) ? map.uInt8(name) : list.uInt8();
  }

  @Override
  public IntervalYearWriter intervalYear(String name) {
    return (map != null) ? map.intervalYear(name) : list.intervalYear();
  }

  @Override
  public IntervalDayWriter intervalDay(String name) {
    return (map != null) ? map.intervalDay(name) : list.intervalDay();
  }

  @Override
  public IntervalWriter interval(String name) {
    return (map != null) ? map.interval(name) : list.interval();
  }

  @Override
  public Decimal9Writer decimal9(String name) {
    return (map != null) ? map.decimal9(name) : list.decimal9();
  }

  @Override
  public Decimal18Writer decimal18(String name) {
    return (map != null) ? map.decimal18(name) : list.decimal18();
  }

  @Override
  public Decimal28DenseWriter decimal28Dense(String name) {
    return (map != null) ? map.decimal28Dense(name) : list.decimal28Dense();
  }

  @Override
  public Decimal38DenseWriter decimal38Dense(String name) {
    return (map != null) ? map.decimal38Dense(name) : list.decimal38Dense();
  }

  @Override
  public VarDecimalWriter varDecimal(String name) {
    return (map != null) ? map.varDecimal(name) : list.varDecimal();
  }

  @Override
  public VarDecimalWriter varDecimal(String name, int precision, int scale) {
    return (map != null) ? map.varDecimal(name, precision, scale) : list.varDecimal(precision, scale);
  }

  @Override
  public Decimal38SparseWriter decimal38Sparse(String name) {
    return (map != null) ? map.decimal38Sparse(name) : list.decimal38Sparse();
  }

  @Override
  public Decimal28SparseWriter decimal28Sparse(String name) {
    return (map != null) ? map.decimal28Sparse(name) : list.decimal28Sparse();
  }

}
