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
package org.apache.drill.exec.physical.impl.sort;

import io.netty.buffer.DrillBuf;

import java.util.ArrayList;
import java.util.List;

import org.apache.drill.common.exceptions.DrillRuntimeException;
import org.apache.drill.common.exceptions.UserException;
import org.apache.drill.exec.exception.SchemaChangeException;
import org.apache.drill.exec.memory.AllocationReservation;
import org.apache.drill.exec.memory.BaseAllocator;
import org.apache.drill.exec.memory.BufferAllocator;
import org.apache.drill.exec.record.BatchSchema;
import org.apache.drill.exec.record.BatchSchema.SelectionVectorMode;
import org.apache.drill.exec.record.MaterializedField;
import org.apache.drill.exec.record.VectorAccessible;
import org.apache.drill.exec.record.VectorContainer;
import org.apache.drill.exec.record.VectorWrapper;
import org.apache.drill.exec.record.selection.SelectionVector2;
import org.apache.drill.exec.record.selection.SelectionVector4;
import org.apache.drill.exec.vector.ValueVector;

import org.apache.drill.shaded.guava.com.google.common.collect.ArrayListMultimap;
import org.apache.drill.shaded.guava.com.google.common.collect.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SortRecordBatchBuilder implements AutoCloseable {
  static final Logger logger = LoggerFactory.getLogger(SortRecordBatchBuilder.class);

  private final ArrayListMultimap<BatchSchema, RecordBatchData> batches = ArrayListMultimap.create();

  private int recordCount;
  private long runningBatches;
  private SelectionVector4 sv4;
  private final BufferAllocator allocator;
  final AllocationReservation reservation;

  public SortRecordBatchBuilder(BufferAllocator a) {
    this.allocator = a;
    this.reservation = a.newReservation();
  }

  private long getSize(VectorAccessible batch) {
    long bytes = 0;
    for (VectorWrapper<?> v : batch) {
      bytes += v.getValueVector().getBufferSize();
    }
    return bytes;
  }

  /**
   * Add another record batch to the set of record batches. TODO: Refactor this and other {@link #add
   * (RecordBatchData)} method into one method.
   * @param batch
   * @return True if the requested add completed successfully.  Returns false in the case that this builder is full and cannot receive additional packages.
   * @throws SchemaChangeException
   */
  public boolean add(VectorAccessible batch) {
    if (batch.getSchema().getSelectionVectorMode() == SelectionVectorMode.FOUR_BYTE) {
      throw new UnsupportedOperationException("A sort cannot currently work against a sv4 batch.");
    }
    if (batch.getRecordCount() == 0 && batches.size() > 0) {
      return true; // skip over empty record batches.
    }

    long batchBytes = getSize(batch);
    if (batchBytes == 0 && batches.size() > 0) {
      return true;
    }
    if (runningBatches >= Character.MAX_VALUE) {
      return false; // allowed in batch.
    }
    if (!reservation.add(batch.getRecordCount() * 4)) {
      return false;  // sv allocation available.
    }


    RecordBatchData bd = new RecordBatchData(batch, allocator);
    runningBatches++;
    batches.put(batch.getSchema(), bd);
    recordCount += bd.getRecordCount();
    return true;
  }

  public void add(RecordBatchData rbd) {
    long batchBytes = getSize(rbd.getContainer());
    if (batchBytes == 0 && batches.size() > 0) {
      return;
    }

    if (runningBatches >= Character.MAX_VALUE) {
      final String errMsg = String.format("Tried to add more than %d number of batches.", (int) Character.MAX_VALUE);
      logger.error(errMsg);
      throw new DrillRuntimeException(errMsg);
    }
    if (!reservation.add(rbd.getRecordCount() * 4)) {
      final String errMsg = String.format("Failed to pre-allocate memory for SV. " + "Existing recordCount*4 = %d, " +
          "incoming batch recordCount*4 = %d", recordCount * 4, rbd.getRecordCount() * 4);
      logger.error(errMsg);
      throw new DrillRuntimeException(errMsg);
    }


    if (rbd.getRecordCount() == 0 && batches.size() > 0) {
      rbd.getContainer().zeroVectors();
      SelectionVector2 sv2 = rbd.getSv2();
      if (sv2 != null) {
        sv2.clear();
      }
      return;
    }
    runningBatches++;
    batches.put(rbd.getContainer().getSchema(), rbd);
    recordCount += rbd.getRecordCount();
  }

  public boolean isEmpty() {
    return batches.isEmpty();
  }

  public void build(VectorContainer outputContainer) {
    outputContainer.clear();
    if (batches.keySet().size() > 1) {
      throw UserException.validationError(null)
          .message("Sort currently only supports a single schema.")
          .build(logger);
    }
    if (batches.size() > Character.MAX_VALUE) {
      throw UserException.internalError(null)
      .message("Sort cannot work on more than %d batches at a time.", Character.MAX_VALUE)
      .build(logger);
    }
    if (batches.keys().size() < 1) {
      assert false : "Invalid to have an empty set of batches with no schemas.";
    }

    final DrillBuf svBuffer = reservation.allocateBuffer();
    if (svBuffer == null) {
      throw new OutOfMemoryError("Failed to allocate direct memory for SV4 vector in SortRecordBatchBuilder.");
    }
    sv4 = new SelectionVector4(svBuffer, recordCount, ValueVector.MAX_ROW_COUNT);
    BatchSchema schema = batches.keySet().iterator().next();
    List<RecordBatchData> data = batches.get(schema);

    // now we're going to generate the sv4 pointers
    switch (schema.getSelectionVectorMode()) {
    case NONE: {
      int index = 0;
      int recordBatchId = 0;
      for (RecordBatchData d : data) {
        for (int i =0; i < d.getRecordCount(); i++, index++) {
          sv4.set(index, recordBatchId, i);
        }
        recordBatchId++;
      }
      break;
    }
    case TWO_BYTE: {
      int index = 0;
      int recordBatchId = 0;
      for (RecordBatchData d : data) {
        for (int i = 0; i < d.getRecordCount(); i++, index++) {
          sv4.set(index, recordBatchId, d.getSv2().getIndex(i));
        }
        // might as well drop the selection vector since we'll stop using it now.
        d.getSv2().clear();
        recordBatchId++;
      }
      break;
    }
    default:
      throw new UnsupportedOperationException();
    }

    // next, we'll create lists of each of the vector types.
    ArrayListMultimap<MaterializedField, ValueVector> vectors = ArrayListMultimap.create();
    for (RecordBatchData rbd : batches.values()) {
      for (ValueVector v : rbd.getVectors()) {
        vectors.put(v.getField(), v);
      }
    }

    for (MaterializedField f : schema) {
      List<ValueVector> v = vectors.get(f);
      outputContainer.addHyperList(v, false);
    }

    outputContainer.buildSchema(SelectionVectorMode.FOUR_BYTE);
  }

  public SelectionVector4 getSv4() {
    return sv4;
  }

  public void clear() {
    for (RecordBatchData d : batches.values()) {
      d.container.clear();
    }
    if (sv4 != null) {
      sv4.clear();
    }
  }

  @Override
  public void close() {
    reservation.close();
  }

  public List<VectorContainer> getHeldRecordBatches() {
    ArrayList<VectorContainer> containerList = Lists.newArrayList();
    for (BatchSchema bs : batches.keySet()) {
      for (RecordBatchData bd : batches.get(bs)) {
        VectorContainer c = bd.getContainer();
        c.setRecordCount(bd.getRecordCount());
        containerList.add(c);
      }
    }
    batches.clear();
    return containerList;
  }

  /**
   * For given record count, return the memory that SortRecordBatchBuilder needs
   * for its own purpose. This is used in ExternalSortBatch to make decisions
   * about whether to spill or not.
   *
   * @param recordCount expected output record count
   * @return number of bytes needed for an SV4, power-of-two rounded
   */

  public static long memoryNeeded(int recordCount) {
    // We need 4 bytes (SV4) for each record. Due to power-of-two allocations, the
    // backing buffer might be twice this size.
    return BaseAllocator.nextPowerOfTwo(recordCount * 4);
  }
}
