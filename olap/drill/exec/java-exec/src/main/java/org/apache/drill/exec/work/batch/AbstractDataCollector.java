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
package org.apache.drill.exec.work.batch;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicIntegerArray;

import org.apache.drill.common.AutoCloseables;
import org.apache.drill.exec.memory.BufferAllocator;
import org.apache.drill.exec.ops.FragmentContext;
import org.apache.drill.exec.proto.BitControl.Collector;
import org.apache.drill.exec.record.RawFragmentBatch;
import org.apache.drill.exec.util.ArrayWrappedIntIntMap;

import org.apache.drill.shaded.guava.com.google.common.base.Preconditions;

public abstract class AbstractDataCollector implements DataCollector {
  private final int oppositeMajorFragmentId;
  private final AtomicIntegerArray remainders;
  private final AtomicInteger remainingRequired;
  private final AtomicInteger parentAccounter;
  private final int incomingStreams;
  protected final RawBatchBuffer[] buffers;
  protected final ArrayWrappedIntIntMap fragmentMap;
  /** Allocator which owns incoming batches */
  protected BufferAllocator ownerAllocator;

  /**
   * @param parentAccounter
   * @param numBuffers Number of RawBatchBuffer inputs required to store the incoming data
   * @param bufferCapacity Capacity of each RawBatchBuffer.
   * @param context
   */
  public AbstractDataCollector(AtomicInteger parentAccounter,
      final int numBuffers, Collector collector, final int bufferCapacity, FragmentContext context) {
    Preconditions.checkNotNull(collector);
    Preconditions.checkNotNull(parentAccounter);

    this.incomingStreams = collector.getIncomingMinorFragmentCount();
    this.parentAccounter = parentAccounter;
    this.remainders = new AtomicIntegerArray(incomingStreams);
    this.oppositeMajorFragmentId = collector.getOppositeMajorFragmentId();
    this.ownerAllocator = context.getAllocator();
    // Create fragmentId to index that is within the range [0, incoming.size()-1]
    // We use this mapping to find objects belonging to the fragment in buffers and remainders arrays.
    fragmentMap = new ArrayWrappedIntIntMap();
    int index = 0;
    for (Integer endpoint : collector.getIncomingMinorFragmentList()) {
      fragmentMap.put(endpoint, index);
      index++;
    }

    buffers = new RawBatchBuffer[numBuffers];
    remainingRequired = new AtomicInteger(numBuffers);

    final boolean spooling = collector.getIsSpooling();
    final boolean enableDynamicFc = collector.hasEnableDynamicFc();

    for (int i = 0; i < numBuffers; i++) {
      if (spooling) {
        buffers[i] = new SpoolingRawBatchBuffer(context, bufferCapacity, collector.getOppositeMajorFragmentId(), i, enableDynamicFc);
      } else {
        buffers[i] = new UnlimitedRawBatchBuffer(context, bufferCapacity, enableDynamicFc);
      }
    }
  }

  @Override
  public int getOppositeMajorFragmentId() {
    return oppositeMajorFragmentId;
  }

  @Override
  public RawBatchBuffer[] getBuffers(){
    return buffers;
  }

  @Override
  public boolean batchArrived(int minorFragmentId, RawFragmentBatch batch)  throws IOException {

    // check to see if we have enough fragments reporting to proceed.
    boolean decrementedToZero = false;
    if (remainders.compareAndSet(fragmentMap.get(minorFragmentId), 0, 1)) {
      int rem = remainingRequired.decrementAndGet();
      if (rem == 0) {
        decrementedToZero = 0 == parentAccounter.decrementAndGet();
      }
    }

    getBuffer(minorFragmentId).enqueue(batch);

    return decrementedToZero;
  }


  @Override
  public int getTotalIncomingFragments() {
    return incomingStreams;
  }

  protected abstract RawBatchBuffer getBuffer(int minorFragmentId);

  @Override
  public void close() throws Exception {
    AutoCloseables.close(buffers);
  }

  /** {@inheritDoc} */
  @Override
  public BufferAllocator getAllocator() {
    return this.ownerAllocator;
  }

  /** {@inheritDoc} */
  @Override
  public void setAllocator(BufferAllocator allocator) {
    Preconditions.checkArgument(allocator != null, "buffer allocator cannot be null");
    this.ownerAllocator = allocator;
  }

}