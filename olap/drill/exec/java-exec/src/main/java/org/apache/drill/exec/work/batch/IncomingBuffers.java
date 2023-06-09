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
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.drill.common.AutoCloseables;
import org.apache.drill.common.concurrent.AutoCloseableLock;
import org.apache.drill.exec.exception.FragmentSetupException;
import org.apache.drill.exec.memory.BufferAllocator;
import org.apache.drill.exec.ops.FragmentContext;
import org.apache.drill.exec.proto.BitControl.Collector;
import org.apache.drill.exec.proto.BitControl.PlanFragment;
import org.apache.drill.exec.record.RawFragmentBatch;
import org.apache.drill.exec.rpc.data.IncomingDataBatch;

import org.apache.drill.shaded.guava.com.google.common.collect.ImmutableMap;
import org.apache.drill.shaded.guava.com.google.common.collect.Maps;

/**
 * Determines when a particular fragment has enough data for each of its receiving exchanges to commence execution.  Also monitors whether we've collected all incoming data.
 */
public class IncomingBuffers implements AutoCloseable {
  static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(IncomingBuffers.class);

  private volatile boolean closed = false;
  private final AtomicInteger streamsRemaining = new AtomicInteger(0);
  private final AtomicInteger remainingRequired;
  private final Map<Integer, DataCollector> collectorMap;
  private final FragmentContext context;

  /**
   * Lock used to manage close and data acceptance. We should only create a local reference to incoming data in the case
   * that the incoming buffers are !closed. As such, we need to make sure that we aren't in the process of closing the
   * incoming buffers when data is arriving. The read lock can be shared by many incoming batches but the write lock
   * must be exclusive to the close method.
   */
  private final ReadWriteLock lock = new ReentrantReadWriteLock();
  private final AutoCloseableLock sharedIncomingBatchLock = new AutoCloseableLock(lock.readLock());
  private final AutoCloseableLock exclusiveCloseLock = new AutoCloseableLock(lock.writeLock());

  public IncomingBuffers(PlanFragment fragment, FragmentContext context) {
    this.context = context;
    Map<Integer, DataCollector> collectors = Maps.newHashMap();
    remainingRequired = new AtomicInteger(fragment.getCollectorCount());
    for(int i =0; i < fragment.getCollectorCount(); i++){
      Collector collector = fragment.getCollector(i);
      DataCollector newCollector = collector.getSupportsOutOfOrder() ?
          new MergingCollector(remainingRequired, collector, context) :
          new PartitionedCollector(remainingRequired, collector, context);
      collectors.put(collector.getOppositeMajorFragmentId(), newCollector);
    }

    logger.debug("Came up with a list of {} required fragments.  Fragments {}", remainingRequired.get(), collectors);
    collectorMap = ImmutableMap.copyOf(collectors);

    // Determine the total number of incoming streams that will need to be completed before we are finished.
    int totalStreams = 0;
    for (DataCollector bc : collectorMap.values()) {
      totalStreams += bc.getTotalIncomingFragments();
    }
    assert totalStreams >= remainingRequired.get() : String.format("Total Streams %d should be more than the minimum number of streams to commence (%d).  It isn't.", totalStreams, remainingRequired.get());
    streamsRemaining.set(totalStreams);
  }

  public boolean batchArrived(final IncomingDataBatch incomingBatch) throws FragmentSetupException, IOException {

    // we want to make sure that we only generate local record batch reference in the case that we're not closed.
    // Otherwise we would leak memory.
    try (@SuppressWarnings("unused") AutoCloseables.Closeable lock = sharedIncomingBatchLock.open()) {
      if (closed) {
        return false;
      }

      if (incomingBatch.getHeader().getIsLastBatch()) {
        streamsRemaining.decrementAndGet();
      }

      final int sendMajorFragmentId = incomingBatch.getHeader().getSendingMajorFragmentId();
      DataCollector collector = collectorMap.get(sendMajorFragmentId);
      if (collector == null) {
        throw new FragmentSetupException(String.format(
            "We received a major fragment id that we were not expecting.  The id was %d. %s", sendMajorFragmentId,
            Arrays.toString(collectorMap.values().toArray())));
      }

      // Use the Data Collector's buffer allocator if set, otherwise the fragment's one
      BufferAllocator ownerAllocator = collector.getAllocator();

      synchronized (collector) {
        final RawFragmentBatch newRawFragmentBatch = incomingBatch.newRawFragmentBatch(ownerAllocator);
        boolean decrementedToZero = collector
            .batchArrived(incomingBatch.getHeader().getSendingMinorFragmentId(), newRawFragmentBatch);
        newRawFragmentBatch.release();

        // we should only return true if remaining required has been decremented and is currently equal to zero.
        return decrementedToZero;
      }

    }

  }

  public int getRemainingRequired() {
    int rem = remainingRequired.get();
    if (rem < 0) {
      return 0;
    }
    return rem;
  }

  public DataCollector getCollector(int senderMajorFragmentId) {
    return collectorMap.get(senderMajorFragmentId);
  }

  public boolean isDone() {
    return streamsRemaining.get() < 1;
  }

  @Override
  public void close() throws Exception {
    try (@SuppressWarnings("unused") AutoCloseables.Closeable lock = exclusiveCloseLock.open()) {
      closed = true;
      AutoCloseables.close(collectorMap.values());
    }
  }

}
