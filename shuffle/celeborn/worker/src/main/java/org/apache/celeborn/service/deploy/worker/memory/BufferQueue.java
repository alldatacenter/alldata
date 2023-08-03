/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.celeborn.service.deploy.worker.memory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.Nullable;

import io.netty.buffer.ByteBuf;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// Assume that max-managed memory for a MapDataPartition is (2^31 * buffersize)
public class BufferQueue {
  public static final Logger logger = LoggerFactory.getLogger(BufferQueue.class);

  private final Queue<ByteBuf> buffers = new ConcurrentLinkedQueue<>();

  private final MemoryManager memoryManager = MemoryManager.instance();

  /** Number of buffers occupied by this buffer queue (added but still not recycled). */
  private final AtomicInteger numBuffersOccupied = new AtomicInteger();

  private final AtomicInteger pendingRequestBuffers = new AtomicInteger();

  /** Whether this buffer queue is released or not. */
  private volatile boolean isReleased = false;

  private volatile int localBuffersTarget = 0;

  /** Returns the number of available buffers in this buffer queue. */
  public int size() {
    return buffers.size();
  }

  /**
   * Returns an available buffer from this buffer queue or returns null if no buffer is available
   * currently.
   */
  @Nullable
  public ByteBuf poll() {
    synchronized (buffers) {
      return buffers.poll();
    }
  }

  /**
   * Add buffers and increment numBufferOccupied. Free all buffers to global memory pool if this
   * buffer queue is released.
   */
  public void add(Collection<ByteBuf> availableBuffers) {
    synchronized (buffers) {
      if (!isReleased) {
        buffers.addAll(availableBuffers);
        numBuffersOccupied.addAndGet(availableBuffers.size());
        pendingRequestBuffers.addAndGet(-1 * availableBuffers.size());
      } else {
        for (ByteBuf availableBuffer : availableBuffers) {
          memoryManager.recycleReadBuffer(availableBuffer);
        }
      }
    }
  }

  public void recycle(ByteBuf buffer) {
    if (numBuffersOccupied.get() > localBuffersTarget) {
      recycleToGlobalPool(buffer);
    } else {
      recycleToLocalPool(buffer);
    }
  }

  public void recycleToGlobalPool(ByteBuf buffer) {
    numBuffersOccupied.decrementAndGet();
    memoryManager.recycleReadBuffer(buffer);
  }

  public void recycleToLocalPool(ByteBuf buffer) {
    buffer.clear();
    buffers.add(buffer);
  }

  // free unused buffer to the main pool if possible
  public void trim() {
    List<ByteBuf> buffersToFree = new ArrayList<>();
    synchronized (this) {
      while (numBuffersOccupied.get() > localBuffersTarget) {
        ByteBuf buffer = poll();
        if (buffer != null) {
          buffersToFree.add(buffer);
          numBuffersOccupied.decrementAndGet();
        } else {
          // there are no unused buffers here
          break;
        }
      }
    }

    if (!buffersToFree.isEmpty()) {
      buffersToFree.forEach(memoryManager::recycleReadBuffer);
    }
  }

  /**
   * Releases this buffer queue and recycles all available buffers. After released, no buffer can be
   * added to or polled from this buffer queue.
   */
  public void release() {
    synchronized (buffers) {
      isReleased = true;
      buffers.forEach(this::recycleToGlobalPool);
      buffers.clear();
    }
    pendingRequestBuffers.set(0);
    numBuffersOccupied.set(0);
  }

  /** Returns true is this buffer queue has been released. */
  public boolean isReleased() {
    return isReleased;
  }

  public int getLocalBuffersTarget() {
    return localBuffersTarget;
  }

  public void setLocalBuffersTarget(int localBuffersTarget) {
    this.localBuffersTarget = localBuffersTarget;
  }

  public void tryApplyNewBuffers(
      int readerSize, int bufferSize, ReadBufferListener readBufferListener) {
    if (readerSize != 0) {
      synchronized (this) {
        int occupiedSnapshot = numBuffersOccupied.get();
        int pendingSnapShot = pendingRequestBuffers.get();
        if (occupiedSnapshot + pendingSnapShot < localBuffersTarget) {
          int newBuffersCount = (localBuffersTarget - occupiedSnapshot - pendingSnapShot);
          logger.debug(
              "apply new buffers {} while current buffer queue size {} with read count {}",
              newBuffersCount,
              numBuffersOccupied.get(),
              readerSize);
          pendingRequestBuffers.addAndGet(newBuffersCount);
          memoryManager.requestReadBuffers(
              new ReadBufferRequest(newBuffersCount, bufferSize, readBufferListener));
        }
      }
    }
  }

  public boolean bufferAvailable() {
    return !buffers.isEmpty();
  }
}
