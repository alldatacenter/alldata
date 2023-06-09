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

import java.util.Iterator;

import org.apache.drill.common.types.TypeProtos.MajorType;
import org.apache.drill.common.types.TypeProtos.MinorType;
import org.apache.drill.common.types.Types;
import org.apache.drill.exec.expr.holders.UnionHolder;
import org.apache.drill.exec.record.MaterializedField;
import org.apache.drill.exec.vector.UntypedNullHolder;
import org.apache.drill.exec.vector.complex.reader.FieldReader;
import org.apache.drill.exec.vector.complex.writer.BaseWriter.ListWriter;
import org.apache.drill.exec.vector.complex.writer.FieldWriter;


abstract class AbstractBaseReader implements FieldReader {

  private int index;

  @Override
  public void setPosition(int index) { this.index = index; }

  int idx() { return index; }

  @Override
  public void reset() { index = 0; }

  @Override
  public Iterator<String> iterator() {
    throw new IllegalStateException("The current reader doesn't support reading as a map.");
  }

  @Override
  public MajorType getType() {
    throw new IllegalStateException("The current reader doesn't support getting type information.");
  }

  @Override
  public MinorType getVectorType() { return getType().getMinorType(); }

  @Override
  public MaterializedField getField() {
    return MaterializedField.create("unknown", Types.LATE_BIND_TYPE);
  }

  @Override
  public boolean next() {
    throw new IllegalStateException("The current reader doesn't support getting next information.");
  }

  @Override
  public int size() {
    throw new IllegalStateException("The current reader doesn't support getting size information.");
  }

  @Override
  public void read(UnionHolder holder) {
    holder.reader = this;
    holder.isSet = this.isSet() ? 1 : 0;
  }

  @Override
  public void read(int index, UnionHolder holder) {
    throw new IllegalStateException("The current reader doesn't support reading union type");
  }

  @Override
  public void read(UntypedNullHolder holder) {
    throw new IllegalStateException("The current reader doesn't support reading untyped null");
  }

  @Override
  public void read(int index, UntypedNullHolder holder) {
    throw new IllegalStateException("The current reader doesn't support reading untyped null");
  }

  @Override
  public void copyAsValue(UnionWriter writer) {
    throw new IllegalStateException("The current reader doesn't support reading union type");
  }

  @Override
  public void copyAsValue(ListWriter writer) {
    ComplexCopier.copy(this, (FieldWriter)writer);
  }

  @Override
  public String getTypeString() {
    return getType().getMinorType().name();
  }
}
