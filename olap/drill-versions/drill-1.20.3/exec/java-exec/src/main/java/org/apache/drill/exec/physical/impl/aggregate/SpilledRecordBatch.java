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
package org.apache.drill.exec.physical.impl.aggregate;

import org.apache.drill.common.exceptions.UserException;
import org.apache.drill.common.expression.SchemaPath;
import org.apache.drill.exec.cache.VectorAccessibleSerializable;
import org.apache.drill.exec.ops.FragmentContext;
import org.apache.drill.exec.ops.OperatorContext;
import org.apache.drill.exec.physical.impl.spill.SpillSet;
import org.apache.drill.exec.record.BatchSchema;
import org.apache.drill.exec.record.CloseableRecordBatch;
import org.apache.drill.exec.record.TypedFieldId;
import org.apache.drill.exec.record.VectorContainer;
import org.apache.drill.exec.record.VectorWrapper;
import org.apache.drill.exec.record.WritableBatch;
import org.apache.drill.exec.record.selection.SelectionVector2;
import org.apache.drill.exec.record.selection.SelectionVector4;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;

/**
 * Replaces "incoming" - instead scanning a spilled partition file
 */
public class SpilledRecordBatch implements CloseableRecordBatch {
  private static final Logger logger = LoggerFactory.getLogger(SpilledRecordBatch.class);

  private VectorContainer container;
  private InputStream spillStream;
  private int spilledBatches;
  private final FragmentContext context;
  private final BatchSchema schema;
  private final SpillSet spillSet;
  private final String spillFile;
  VectorAccessibleSerializable vas;
  private final IterOutcome initialOutcome;
  // Represents last outcome of next(). If an Exception is thrown
  // during the method's execution a value IterOutcome.STOP will be assigned.
  private IterOutcome lastOutcome;

  public SpilledRecordBatch(String spillFile, int spilledBatches, FragmentContext context, BatchSchema schema, OperatorContext oContext, SpillSet spillSet) {
    this.context = context;
    this.schema = schema;
    this.spilledBatches = spilledBatches;
    this.spillSet = spillSet;
    this.spillFile = spillFile;
    vas = new VectorAccessibleSerializable(oContext.getAllocator());
    container = vas.get();

    try {
      this.spillStream = this.spillSet.openForInput(spillFile);
    } catch (IOException e) {
      throw UserException.resourceError(e).build(logger);
    }

    initialOutcome = next(); // initialize the container
    lastOutcome = initialOutcome;
  }

  @Override
  public SelectionVector2 getSelectionVector2() {
    throw new UnsupportedOperationException();
  }

  @Override
  public SelectionVector4 getSelectionVector4() {
    throw new UnsupportedOperationException();
  }

  @Override
  public TypedFieldId getValueVectorId(SchemaPath path) {
    return container.getValueVectorId(path);
  }

  @Override
  public VectorWrapper<?> getValueAccessorById(Class<?> clazz, int... ids) {
    return container.getValueAccessorById(clazz, ids);
  }

  @Override
  public Iterator<VectorWrapper<?>> iterator() {
    return container.iterator();
  }

  @Override
  public FragmentContext getContext() { return context; }

  @Override
  public BatchSchema getSchema() { return schema; }

  @Override
  public WritableBatch getWritableBatch() {
    return WritableBatch.get(this);
  }

  @Override
  public VectorContainer getOutgoingContainer() { return container; }

  @Override
  public VectorContainer getContainer() { return container; }

  @Override
  public int getRecordCount() { return container.getRecordCount(); }

  @Override
  public void cancel() {
    close(); // delete the current spill file
  }

  /**
   * Read the next batch from the spill file
   *
   * @return IterOutcome
   */
  @Override
  public IterOutcome next() {

    context.getExecutorState().checkContinue();

    if (spilledBatches <= 0) { // no more batches to read in this partition
      this.close();
      lastOutcome = IterOutcome.NONE;
      return lastOutcome;
    }

    if (spillStream == null) {
      throw new IllegalStateException("Spill stream was null");
    }

    if ( spillSet.getPosition(spillStream)  < 0 ) {
      logger.warn("Position is {} for stream {}", spillSet.getPosition(spillStream), spillStream.toString());
    }

    try {
      if (container.getNumberOfColumns() > 0) { // container already initialized
        // Pass our container to the reader because other classes (e.g. HashAggBatch, HashTable)
        // may have a reference to this container (as an "incoming")
        vas.readFromStreamWithContainer(container, spillStream);
      }
      else { // first time - create a container
        vas.readFromStream(spillStream);
        container = vas.get();
      }
    } catch (IOException e) {
      throw UserException.dataReadError(e)
          .addContext("Failed reading from a spill file")
          .build(logger);
    } catch (Exception e) {
      // TODO: Catch the error closer to the cause and create a better error message.
      throw UserException.executionError(e).build(logger);
    }

    spilledBatches--; // one less batch to read
    lastOutcome = IterOutcome.OK;
    return lastOutcome;
  }

  /**
   *  Return the initial outcome (from the first next() call )
   */
  public IterOutcome getInitialOutcome() { return initialOutcome; }

  @Override
  public void dump() {
    logger.error("SpilledRecordbatch[container={}, spilledBatches={}, schema={}, spillFile={}, spillSet={}]",
        container, spilledBatches, schema, spillFile, spillSet);
  }

  /**
   * Note: ignoring any IO errors (e.g. file not found)
   */
  @Override
  public void close() {
    container.clear();
    try {
      if (spillStream != null) {
        spillStream.close();
        spillStream = null;
      }

      spillSet.delete(spillFile);
    }
    catch (IOException e) {
      // ignore
    }
  }
}
