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
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.drill.common.exceptions.DrillRuntimeException;
import org.apache.drill.exec.ExecConstants;
import org.apache.drill.exec.ops.FragmentContext;
import org.apache.drill.exec.record.RawFragmentBatch;

public abstract class BaseRawBatchBuffer<T> implements RawBatchBuffer {
  private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(BaseRawBatchBuffer.class);

  private enum BufferState {
    INIT,
    STREAMS_FINISHED,
    KILLED
  }

  protected interface BufferQueue<T> {
    void addOomBatch(RawFragmentBatch batch);
    RawFragmentBatch poll() throws IOException, InterruptedException;
    RawFragmentBatch take() throws IOException, InterruptedException;
    RawFragmentBatch poll(long timeout, TimeUnit timeUnit) throws InterruptedException, IOException;
    boolean checkForOutOfMemory();
    int size();
    boolean isEmpty();
    void add(T obj);
  }

  protected BufferQueue<T> bufferQueue;
  private volatile BufferState state = BufferState.INIT;
  protected final int bufferSizePerSocket;
  protected final AtomicBoolean outOfMemory = new AtomicBoolean(false);
  private int streamCounter;
  private final int fragmentCount;
  protected final FragmentContext context;
  protected final boolean enableDynamicFC;

  public BaseRawBatchBuffer(final FragmentContext context, final int fragmentCount, final boolean enableDynamicFC) {
    bufferSizePerSocket = context.getConfig().getInt(ExecConstants.INCOMING_BUFFER_SIZE);

    this.fragmentCount = fragmentCount;
    this.streamCounter = fragmentCount;
    this.context = context;
    this.enableDynamicFC = enableDynamicFC;
  }

  /**
   * Return the fragment count from construction time.
   *
   * @return the fragment count
   */
  protected int getFragmentCount() {
    return fragmentCount;
  }

  @Override
  public synchronized void enqueue(final RawFragmentBatch batch) throws IOException {

    // if this fragment is already canceled or failed, we shouldn't need any or more stuff. We do the null check to
    // ensure that tests run.
    if (context != null && !context.getExecutorState().shouldContinue()) {
      this.kill(context);
    }

    if (isTerminated()) {
      if (state == BufferState.KILLED) {
        // do not even enqueue just release and send ack back
        batch.release();
        batch.sendOk();
        return;
      } else {
        throw new IOException("Attempted to enqueue batch after finished");
      }
    }
    enqueueInner(batch);
  }

  /**
   * implementation specific method to enqueue batch
   *
   * @param batch
   * @throws IOException
   */
  protected abstract void enqueueInner(final RawFragmentBatch batch) throws IOException;

//  ## Add assertion that all acks have been sent. TODO
  @Override
  public void close() {
    if (!isTerminated() && context.getExecutorState().shouldContinue()) {
      final String msg = String.format("Cleanup before finished. %d out of %d streams have finished", completedStreams(), fragmentCount);
      throw  new IllegalStateException(msg);
    }

    if (!bufferQueue.isEmpty()) {
      if (context.getExecutorState().shouldContinue()) {
        context.getExecutorState().fail(new IllegalStateException("Batches still in queue during cleanup"));
        logger.error("{} Batches in queue.", bufferQueue.size());
      }
      clearBufferWithBody();
    }
  }

  @Override
  public synchronized void kill(final FragmentContext context) {
    state = BufferState.KILLED;
    clearBufferWithBody();
  }

  /**
   * Helper method to clear buffer with request bodies release also flushes ack queue - in case there are still
   * responses pending
   */
  private void clearBufferWithBody() {
    RawFragmentBatch batch;
    while (!bufferQueue.isEmpty()) {
      batch = null;
      try {
        batch = bufferQueue.poll();
        assertAckSent(batch);
      } catch (IOException e) {
        context.getExecutorState().fail(e);
        continue;
      } catch (InterruptedException e) {
        context.getExecutorState().fail(e);
        // keep the state that the thread is interrupted
        Thread.currentThread().interrupt();
        continue;
      } finally {
        if (batch != null && batch.getBody() != null) {
          batch.getBody().release();
        }
      }
    }
  }

  private void allStreamsFinished() {
    if (state != BufferState.KILLED) {
      state = BufferState.STREAMS_FINISHED;
    }

    if (!bufferQueue.isEmpty()) {
      throw new IllegalStateException("buffer not empty when finished");
    }
  }

  @Override
  public RawFragmentBatch getNext() throws IOException {

    if (outOfMemory.get()) {
      if (bufferQueue.size() < 10) {
        outOfMemory.set(false);
      }
    }

    RawFragmentBatch b;
    try {
      b = bufferQueue.poll();

      // if we didn't get a batch, block on waiting for queue.
      if (b == null && (!isTerminated() || !bufferQueue.isEmpty())) {
        // We shouldn't block infinitely here. There can be a condition such that due to a failure FragmentExecutor
        // state is changed to FAILED and queue is empty. Because of this the minor fragment main thread will block
        // here waiting for next batch to arrive. Meanwhile when next batch arrived and was enqueued it sees
        // FragmentExecutor failure state and doesn't enqueue the batch and cleans up the buffer queue. Hence this
        // thread will stuck forever. So we pool for 5 seconds until we get a batch or FragmentExecutor state is in
        // error condition.
        while (b == null) {
          b = bufferQueue.poll(5, TimeUnit.SECONDS);
          if (!context.getExecutorState().shouldContinue()) {
            kill(context);
            if (b != null) {
              assertAckSent(b);
              if (b.getBody() != null) {
                b.getBody().release();
              }
              b = null;
            }
          } // else b will be assigned a valid batch
        }
      }
    } catch (final InterruptedException e) {

      // We expect that the interrupt means the fragment is canceled or failed, so we should kill this buffer
      if (!context.getExecutorState().shouldContinue()) {
        kill(context);
      } else {
        throw new DrillRuntimeException("Interrupted but context.shouldContinue() is true", e);
      }
      // Preserve evidence that the interruption occurred so that code higher up on the call stack can learn of the
      // interruption and respond to it if it wants to.
      Thread.currentThread().interrupt();

      return null;
    }

    if (context.getAllocator().isOverLimit()) {
      outOfMemory.set(true);
    }

    if (b != null) {

      upkeep(b);

      if (b.getHeader().getIsLastBatch()) {
        logger.debug("Got last batch from {}:{}", b.getHeader().getSendingMajorFragmentId(), b.getHeader()
            .getSendingMinorFragmentId());
        final int remainingStreams = decrementStreamCounter();
        if (remainingStreams == 0) {
          logger.debug("Streams finished");
          allStreamsFinished();
        }
      }
    } else {
      if (!bufferQueue.isEmpty()) {
        throw new IllegalStateException("Returning null when there are batches left in queue");
      }
      if (!isTerminated()) {
        throw new IllegalStateException("Returning null when not finished");
      }
    }

    assertAckSent(b);
    return b;
  }

  private void assertAckSent(RawFragmentBatch batch) {
    assert batch == null || batch.isAckSent() : "Ack not sent for batch";
  }

  private int decrementStreamCounter() {
    streamCounter--;
    return streamCounter;
  }

  private int completedStreams() {
    return fragmentCount - streamCounter;
  }

  /**
   * Handle miscellaneous tasks after batch retrieval
   */
  protected abstract void upkeep(RawFragmentBatch batch);

  protected boolean isTerminated() {
    return (state == BufferState.KILLED || state == BufferState.STREAMS_FINISHED);
  }
}
