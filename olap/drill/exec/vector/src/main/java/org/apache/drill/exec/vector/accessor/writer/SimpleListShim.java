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

import org.apache.drill.common.types.TypeProtos.MinorType;
import org.apache.drill.exec.record.metadata.ColumnMetadata;
import org.apache.drill.exec.vector.accessor.ColumnWriterIndex;
import org.apache.drill.exec.vector.accessor.ObjectWriter;
import org.apache.drill.exec.vector.accessor.impl.HierarchicalFormatter;
import org.apache.drill.exec.vector.accessor.writer.UnionWriterImpl.UnionShim;
import org.apache.drill.shaded.guava.com.google.common.base.Preconditions;

/**
 * Shim for a list that holds a single type, but may eventually become a
 * list of variants. (A list that is declared to hold a single type
 * is implemented using the {@link ListWriterImpl} class that
 * directly holds that single type.) This shim is needed when we want
 * to present a uniform variant interface for a list that holds zero
 * one (this case) or many types.
 */

public class SimpleListShim implements UnionShim {

  private UnionWriterImpl writer;
  private AbstractObjectWriter colWriter;

  public SimpleListShim() { }

  public SimpleListShim(AbstractObjectWriter writer) {
    this.colWriter = writer;
  }

  @Override
  public void bindWriter(UnionWriterImpl writer) {
    this.writer = writer;
  }

  @Override
  public void bindIndex(ColumnWriterIndex index) {
    events().bindIndex(index);
  }

  @Override
  public void bindListener(ColumnWriterListener listener) {
    colWriter.events().bindListener(listener);
  }

  @Override
  public boolean hasType(MinorType type) {
    return type == colWriter.schema().type();
  }

  @Override
  public void setType(MinorType type) {
    if (! hasType(type)) {

      // Not this type. Ask the listener to promote this writer
      // to a union, then ask the writer to set the type on
      // the resulting new shim.

      addMember(type);
      writer.setType(type);
    }
  }

  @Override
  public ObjectWriter member(MinorType type) {
    if (hasType(type)) {
      return colWriter;
    }
    return addMember(type);
  }

  public AbstractObjectWriter memberWriter() {
    return colWriter;
  }

  @Override
  public AbstractObjectWriter addMember(ColumnMetadata colSchema) {
    return doAddMember((AbstractObjectWriter) writer.listener().addMember(colSchema));
  }

  @Override
  public AbstractObjectWriter addMember(MinorType type) {
    return doAddMember((AbstractObjectWriter) writer.listener().addType(type));
  }

  /**
   * This is a bit of magic. The listener will create the writer (and vector) for
   * the requested type. It will also replace the shim in the writer with something
   * that handles a union, so that, when the listener returns,
   * this shim is no longer bound to the writer. Our dying act is to ask the
   * new shim to add the new writer as a member. The listener must have moved our
   * existing reader to the new union shim; but we must add the new writer.
   *
   * @param colWriter the column writer returned from the listener
   * @return the same column writer
   */

  private AbstractObjectWriter doAddMember(AbstractObjectWriter colWriter) {
    // Something went terribly wrong if the check below fails.
    Preconditions.checkState(writer.shim() != this);
    writer.shim().addMember(colWriter);
    return colWriter;
  }

  @Override
  public void addMember(AbstractObjectWriter colWriter) {

    // Should only be called when promoting from a no-type shim to this
    // single type shim. Otherwise, something is wrong with the type
    // promotion logic.

    Preconditions.checkState(this.colWriter == null);
    this.colWriter = colWriter;
    writer.addMember(colWriter);
  }

  @Override
  public void setNull() {
    colWriter.setNull();
  }

  private WriterEvents events() { return colWriter.events(); }

  @Override
  public void startWrite() {

    // startWrite called before the column is added.

    if (colWriter != null) {
      colWriter.events().startWrite();
    }
  }

  @Override
  public void startRow() {

    // startRow called before the column is added.

    if (colWriter != null) {
      colWriter.events().startRow();
    }
  }

  @Override
  public void endArrayValue() {
    events().endArrayValue();
  }

  @Override
  public void restartRow() {
    events().restartRow();
  }

  @Override
  public void saveRow() {
    events().saveRow();
  }

  @Override
  public void endWrite() {
    events().endWrite();
  }

  @Override
  public void preRollover() {
    events().preRollover();
  }

  @Override
  public void postRollover() {
    events().postRollover();
  }

  @Override
  public int lastWriteIndex() {
    return events().lastWriteIndex();
  }

  @Override
  public int rowStartIndex() {
    return events().rowStartIndex();
  }

  @Override
  public int writeIndex() {
    return events().writeIndex();
  }

  @Override
  public void dump(HierarchicalFormatter format) {
    format.startObject(this).attribute("colWriter");
    colWriter.dump(format);
    format.endObject();
  }
}
