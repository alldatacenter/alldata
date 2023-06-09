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
 * Internal implementation for a list of (possible) variants when
 * the list has no type associated with it at all. This shim is a
 * placeholder that waits for the first type to be added. Used in
 * the case that a list may eventually hold a union, but at present
 * it holds nothing.
 */

public class EmptyListShim implements UnionShim {

  private UnionWriterImpl writer;

  @Override
  public void bindWriter(UnionWriterImpl writer) {
    this.writer = writer;
  }

  @Override
  public void bindIndex(ColumnWriterIndex index) { }

  @Override
  public void bindListener(ColumnWriterListener listener) { }

  @Override
  public void startWrite() { }

  @Override
  public void startRow() { }

  @Override
  public void endArrayValue() { }

  @Override
  public void restartRow() { }

  @Override
  public void saveRow() { }

  @Override
  public void endWrite() { }

  @Override
  public void preRollover() { }

  @Override
  public void postRollover() { }

  @Override
  public void setNull() {
    // OK to set a non-existent type to null
  }

  @Override
  public boolean hasType(MinorType type) { return false; }

  @Override
  public ObjectWriter member(MinorType type) {
    return addMember(type);
  }

  @Override
  public void setType(MinorType type) {

    // This shim has no types at all. Let the listener add a type.
    // If the result is a new shim, then ask the writer to set the
    // type using that new shim.

    addMember(type);
    writer.setType(type);
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
   * that handles a single type or a union, so that, when the listener returns,
   * this shim is no longer bound to the writer. Our dying act is to ask the
   * new shim to add the new writer as a member.
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
  public int lastWriteIndex() { return 0; }

  @Override
  public int rowStartIndex() { return 0; }

  @Override
  public int writeIndex() { return 0; }

  @Override
  public void addMember(AbstractObjectWriter colWriter) {

    // This shim has no types. If this is called, then the shim replacement
    // logic has a bug.

    throw new UnsupportedOperationException();
  }

  @Override
  public void dump(HierarchicalFormatter format) {
    format.startObject(this).endObject();
  }
}
