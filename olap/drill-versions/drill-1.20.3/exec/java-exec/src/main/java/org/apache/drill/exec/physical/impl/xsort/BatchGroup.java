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
package org.apache.drill.exec.physical.impl.xsort;

import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;

import org.apache.drill.common.AutoCloseables;
import org.apache.drill.common.exceptions.UserException;
import org.apache.drill.common.expression.SchemaPath;
import org.apache.drill.exec.memory.BufferAllocator;
import org.apache.drill.exec.record.BatchSchema;
import org.apache.drill.exec.record.SchemaUtil;
import org.apache.drill.exec.record.TypedFieldId;
import org.apache.drill.exec.record.VectorAccessible;
import org.apache.drill.exec.record.VectorContainer;
import org.apache.drill.exec.record.VectorWrapper;
import org.apache.drill.exec.record.selection.SelectionVector2;
import org.apache.drill.exec.record.selection.SelectionVector4;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Represents a group of batches spilled to disk.
 * <p>
 * The batches are defined by a schema which can change over time. When the schema changes,
 * all existing and new batches are coerced into the new schema. Provides a
 * uniform way to iterate over records for one or more batches whether
 * the batches are in memory or on disk.
 * <p>
 * The <code>BatchGroup</code> operates in two modes as given by the two
 * subclasses:
 * <ul>
 * <li>Input mode {@link InputBatch}: Used to buffer in-memory batches
 * prior to spilling.</li>
 * <li>Spill mode {@link SpilledRun}: Holds a "memento" to a set
 * of batches written to disk. Acts as both a reader and writer for
 * those batches.</li>
 * </ul>
 */
public abstract class BatchGroup implements VectorAccessible, AutoCloseable {
  static final Logger logger = LoggerFactory.getLogger(BatchGroup.class);

  protected final BufferAllocator allocator;
  protected VectorContainer currentContainer;
  /**
   * This class acts as both "holder" for a vector container and an iterator
   * into that container when the sort enters the merge phase. (This should
   * be revisited.) This field keeps track of the next record to merge
   * during the merge phase.
   */
  protected int mergeIndex;
  protected BatchSchema schema;

  public BatchGroup(VectorContainer container, BufferAllocator allocator) {
    this.currentContainer = container;
    this.allocator = allocator;
  }

  /**
   * Updates the schema for this batch group. The current as well as any
   * deserialized batches will be coerced to this schema.
   * @param schema
   */
  public void setSchema(BatchSchema schema) {
    currentContainer = SchemaUtil.coerceContainer(currentContainer, schema, allocator);
    this.schema = schema;
  }

  public int getNextIndex() {
    if (mergeIndex == getRecordCount()) {
      return -1;
    }
    int val = mergeIndex++;
    assert val < currentContainer.getRecordCount();
    return val;
  }

  public VectorContainer getContainer() {
    return currentContainer;
  }

  @Override
  public void close() throws IOException {
    currentContainer.zeroVectors();
  }

  @Override
  public VectorWrapper<?> getValueAccessorById(Class<?> clazz, int... ids) {
    return currentContainer.getValueAccessorById(clazz, ids);
  }

  @Override
  public TypedFieldId getValueVectorId(SchemaPath path) {
    return currentContainer.getValueVectorId(path);
  }

  @Override
  public BatchSchema getSchema() {
    return currentContainer.getSchema();
  }

  @Override
  public int getRecordCount() {
    return currentContainer.getRecordCount();
  }

  public int getUnfilteredRecordCount() {
    return currentContainer.getRecordCount();
  }

  @Override
  public Iterator<VectorWrapper<?>> iterator() {
    return currentContainer.iterator();
  }

  @Override
  public SelectionVector2 getSelectionVector2() {
    throw new UnsupportedOperationException();
  }

  @Override
  public SelectionVector4 getSelectionVector4() {
    throw new UnsupportedOperationException();
  }

  public static void closeAll(Collection<? extends BatchGroup> groups) {
    try {
      AutoCloseables.close(groups);
    } catch (Exception e) {
      throw UserException.dataWriteError(e)
        .message("Failure while flushing spilled data")
        .build(logger);
    }
  }
}
