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
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

import org.apache.drill.exec.ExecConstants;
import org.apache.drill.exec.ops.FragmentContext;
import org.apache.drill.exec.record.RawFragmentBatch;

import org.apache.drill.shaded.guava.com.google.common.collect.Queues;

public class UnlimitedRawBatchBuffer extends BaseRawBatchBuffer<RawFragmentBatch> {
  private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(UnlimitedRawBatchBuffer.class);

  private final int softlimit;
  private final int startlimit;
  //which controls the receiver total credit when we enable the dynamic credit based flow control.
  private int runtimeSoftLimit = -1;
  private int runtimeAckCredit = 1;
  private int sampleTimes = 0;
  private long totalBatchSize = 0L;
  private final int fragmentCount;
  private final int maxSampleTimes;
  private final long thresholdNetworkMem;

  public UnlimitedRawBatchBuffer(FragmentContext context, int fragmentCount, boolean enableDynamicFC) {
    super(context, fragmentCount, enableDynamicFC);
    this.softlimit = bufferSizePerSocket * fragmentCount;
    this.startlimit = Math.max(softlimit/2, 1);
    logger.trace("softLimit: {}, startLimit: {}", softlimit, startlimit);
    this.bufferQueue = new UnlimitedBufferQueue();
    this.fragmentCount = fragmentCount;
    this.sampleTimes = fragmentCount;
    this.maxSampleTimes = fragmentCount;
    this.thresholdNetworkMem = context.getConfig().getLong(ExecConstants.UNLIMITED_BUFFER_MAX_MEMORY_SIZE);
  }

  private class UnlimitedBufferQueue implements BufferQueue<RawFragmentBatch> {
    private final LinkedBlockingDeque<RawFragmentBatch> buffer = Queues.newLinkedBlockingDeque();

    @Override
    public void addOomBatch(RawFragmentBatch batch) {
      buffer.addFirst(batch);
    }

    @Override
    public RawFragmentBatch poll() throws IOException {
      RawFragmentBatch batch = buffer.poll();
      if (batch != null) {
        batch.sendOk();
      }
      return batch;
    }

    @Override
    public RawFragmentBatch take() throws IOException, InterruptedException {
      RawFragmentBatch batch = buffer.take();
      batch.sendOk();
      return batch;
    }

    @Override
    public RawFragmentBatch poll(long timeout, TimeUnit timeUnit) throws InterruptedException, IOException {
      RawFragmentBatch batch = buffer.poll(timeout, timeUnit);
      if (batch != null) {
        batch.sendOk();
      }
      return batch;
    }

    @Override
    public boolean checkForOutOfMemory() {
      return context.getAllocator().isOverLimit();
    }

    @Override
    public int size() {
      return buffer.size();
    }

    @Override
    public boolean isEmpty() {
      return buffer.size() == 0;
    }

    @Override
    public void add(RawFragmentBatch batch) {
      doFlowControl(batch);
      buffer.add(batch);
    }
  }

  private void doFlowControl(RawFragmentBatch batch) {
    if (enableDynamicFC) {
      calculateDynamicCredit(batch);
      if (runtimeSoftLimit > 0) {
        //we already make a decision to give a suggest runtime sender credit
        if (bufferQueue.size() < runtimeSoftLimit) {
          //we just send the same suggest credit to the sender
          batch.sendOk(runtimeAckCredit);
        }
      } else if (bufferQueue.size() < softlimit) {
        //fallback to the initial static credit based flow control
        batch.sendOk();
      }
    } else if (bufferQueue.size() < softlimit) {
      //still use the initial static sender credit
      batch.sendOk();
    }
  }

  private void calculateDynamicCredit(RawFragmentBatch batch) {
    int recordCount = batch.getHeader().getDef().getRecordCount();
    long batchByteSize = batch.getBody() == null ? 0 : batch.getBody().capacity();
    if (recordCount != 0) {
      //skip first header batch
      totalBatchSize += batchByteSize;
      sampleTimes++;
    }
    if (sampleTimes == maxSampleTimes) {
      long averageBatchSize = totalBatchSize / sampleTimes;
      //make a decision
      if (averageBatchSize > 0) {
        runtimeSoftLimit = (int) (thresholdNetworkMem / averageBatchSize);
        runtimeAckCredit = runtimeSoftLimit / fragmentCount;
        runtimeAckCredit = Math.max(runtimeAckCredit, 1);
      }
    }
  }

  protected void enqueueInner(final RawFragmentBatch batch) throws IOException {
    bufferQueue.add(batch);
  }

  protected void upkeep(RawFragmentBatch batch) {
  }
}
