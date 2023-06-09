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

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.apache.drill.common.exceptions.UserException;
import org.apache.drill.exec.exception.OutOfMemoryException;
import org.apache.drill.exec.memory.BufferAllocator;
import org.apache.drill.exec.ops.OperatorContext;
import org.apache.drill.exec.physical.impl.sort.RecordBatchData;
import org.apache.drill.exec.record.BatchSchema;
import org.apache.drill.exec.record.SchemaUtil;
import org.apache.drill.exec.record.VectorAccessible;
import org.apache.drill.exec.record.VectorContainer;
import org.apache.drill.exec.record.VectorWrapper;
import org.apache.drill.exec.record.selection.SelectionVector2;

import org.apache.drill.shaded.guava.com.google.common.collect.Lists;

/**
 * Represents the set of in-memory batches accumulated by
 * the external sort.
 */

public class BufferedBatches {
  static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(BufferedBatches.class);

  /**
   * Incoming batches buffered in memory prior to spilling
   * or an in-memory merge.
   */

  private final LinkedList<InputBatch> bufferedBatches = Lists.newLinkedList();

  private final SorterWrapper sorterWrapper;

  private BatchSchema schema;

  private final OperatorContext context;

  public BufferedBatches(OperatorContext opContext) {
    context = opContext;
    sorterWrapper = new SorterWrapper(opContext);
  }

  public void setSchema(BatchSchema schema) {
    this.schema = schema;

    // New schema: must generate a new sorter and copier.

    sorterWrapper.close();

    // Coerce all existing batches to the new schema.

    for (BatchGroup b : bufferedBatches) {
      b.setSchema(schema);
    }
  }

  public int size() { return bufferedBatches.size(); }

  public void add(VectorAccessible incoming, long batchSize) {
    // Convert the incoming batch to the agreed-upon schema.
    // No converted batch means we got an empty input batch.
    // Converting the batch transfers memory ownership to our
    // allocator. This gives a round-about way to learn the batch
    // size: check the before and after memory levels, then use
    // the difference as the batch size, in bytes.

    VectorContainer convertedBatch = convertBatch(incoming);
    if (convertedBatch == null) {
      return;
    }

    SelectionVector2 sv2;
    try {
      sv2 = makeSelectionVector(incoming);
    } catch (Exception e) {
      convertedBatch.clear();
      throw e;
    }

    // Sort the incoming batch using either the original selection vector,
    // or a new one created here.

    sorterWrapper.sortBatch(convertedBatch, sv2);
    bufferBatch(convertedBatch, sv2, batchSize);
  }

  /**
   * Convert an incoming batch into the agree-upon format.
   * @param incoming
   *
   * @return the converted batch, or null if the incoming batch is empty
   */

  private VectorContainer convertBatch(VectorAccessible incoming) {

    // Must accept the batch even if no records. Then clear
    // the vectors to release memory since we won't do any
    // further processing with the empty batch.

    VectorContainer convertedBatch = SchemaUtil.coerceContainer(incoming, schema, context.getAllocator());
    if (incoming.getRecordCount() == 0) {
      for (VectorWrapper<?> w : convertedBatch) {
        w.clear();
      }
      SelectionVector2 sv2 = incoming.getSelectionVector2();
      if (sv2 != null) {
        sv2.clear();
      }
      return null;
    }
    return convertedBatch;
  }

  private SelectionVector2 makeSelectionVector(VectorAccessible incoming) {
    if (incoming.getSchema().getSelectionVectorMode() == BatchSchema.SelectionVectorMode.TWO_BYTE) {
      return incoming.getSelectionVector2().clone();
    } else {
      return newSV2(incoming);
    }
  }

  /**
   * Allocate and initialize the selection vector used as the sort index.
   * Assumes that memory is available for the vector since memory management
   * ensured space is available.
   *
   * @return a new, populated selection vector 2
   */

  private SelectionVector2 newSV2(VectorAccessible incoming) {
    SelectionVector2 sv2 = new SelectionVector2(context.getAllocator());
    if (!sv2.allocateNewSafe(incoming.getRecordCount())) {
      throw UserException.resourceError(new OutOfMemoryException("Unable to allocate sv2 buffer"))
            .build(logger);
    }
    for (int i = 0; i < incoming.getRecordCount(); i++) {
      sv2.setIndex(i, (char) i);
    }
    sv2.setRecordCount(incoming.getRecordCount());
    return sv2;
  }

  private void bufferBatch(VectorContainer convertedBatch, SelectionVector2 sv2, long netSize) {
    BufferAllocator allocator = context.getAllocator();
    RecordBatchData rbd = new RecordBatchData(convertedBatch, allocator);
    try {
      rbd.setSv2(sv2);
      bufferedBatches.add(new InputBatch(rbd.getContainer(), rbd.getSv2(), allocator, netSize));

    } catch (Throwable t) {
      rbd.clear();
      throw t;
    }
  }

  public List<BatchGroup> prepareSpill(long targetSpillSize) {

    // Determine the number of batches to spill to create a spill file
    // of the desired size. The actual file size might be a bit larger
    // or smaller than the target, which is expected.

    int spillCount = 0;
    long spillSize = 0;
    for (InputBatch batch : bufferedBatches) {
      long batchSize = batch.getDataSize();
      spillSize += batchSize;
      spillCount++;
      if (spillSize + batchSize / 2 > targetSpillSize) {
        break; }
    }

    // Must always spill at least 2, even if this creates an over-size
    // spill file. But, if this is a final consolidation, we may have only
    // a single batch.

    spillCount = Math.max(spillCount, 2);
    spillCount = Math.min(spillCount, bufferedBatches.size());
    return SpilledRuns.prepareSpillBatches(bufferedBatches, spillCount);
  }

  public List<InputBatch> removeAll() {
    List<InputBatch> batches = new ArrayList<>( );
    batches.addAll(bufferedBatches);
    bufferedBatches.clear();
    return batches;
  }

  public void close() {
    // Use the spilled runs version. In-memory batches won't throw
    // an error, but the API is generic.

    RuntimeException ex = null;
    try {
      BatchGroup.closeAll(bufferedBatches);
      bufferedBatches.clear();
    } catch (RuntimeException e) {
      ex = e;
    }
    try {
      sorterWrapper.close();
    } catch (RuntimeException e) {
      ex = (ex == null) ? e : ex;
    }
    if (ex != null) {
      throw ex;
    }
  }
}
