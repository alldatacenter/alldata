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

package org.apache.celeborn.plugin.flink.buffer;

import static org.apache.celeborn.plugin.flink.utils.Utils.checkArgument;
import static org.apache.celeborn.plugin.flink.utils.Utils.checkState;

import java.nio.ByteBuffer;
import java.util.*;

import javax.annotation.concurrent.GuardedBy;

import org.apache.flink.shaded.netty4.io.netty.buffer.ByteBuf;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** A buffer pool which will dispatch buffers to all {@link CreditListener}s. */
public class TransferBufferPool implements BufferRecycler {
  private static Logger logger = LoggerFactory.getLogger(TransferBufferPool.class);

  private static final int MIN_CREDITS_TO_NOTIFY = 2;

  private final Object lock = new Object();

  private final Queue<ByteBuf> buffers = new ArrayDeque<>();

  @GuardedBy("lock")
  private final Queue<CreditListener> listeners = new ArrayDeque<>();

  @GuardedBy("lock")
  private int numAvailableBuffers;

  @GuardedBy("lock")
  private boolean isDestroyed;

  public TransferBufferPool(Collection<ByteBuf> initialBuffers) {
    synchronized (lock) {
      buffers.addAll(initialBuffers);
      numAvailableBuffers += initialBuffers.size();
    }
  }

  /** Requests a data transmitting unit. */
  public ByteBuf requestBuffer() {
    synchronized (lock) {
      checkState(!isDestroyed, "Buffer pool has been destroyed.");

      return buffers.poll();
    }
  }

  /** Adds an available buffer to this buffer pool. */
  public void addBuffers(List<? extends ByteBuf> byteBufs) {
    List<CreditAssignment> creditAssignments;
    synchronized (lock) {
      if (isDestroyed) {
        byteBufs.forEach(ByteBuf::release);
        return;
      }

      buffers.addAll(byteBufs);
      numAvailableBuffers += byteBufs.size();
      creditAssignments = dispatchReservedCredits();
    }
    for (CreditAssignment creditAssignment : creditAssignments) {
      creditAssignment.getCreditListener().notifyAvailableCredits(creditAssignment.getNumCredits());
    }
  }

  /** Tries to reserve buffers for the target {@link CreditListener}. */
  public void reserveBuffers(CreditListener creditListener, int numRequiredBuffers) {
    int numCredits;
    CreditListener listener = null;
    synchronized (lock) {
      if (isDestroyed) {
        throw new IllegalStateException("Buffer pool has been destroyed.");
      }

      if (numRequiredBuffers > numAvailableBuffers) {
        creditListener.increaseNumCreditsNeeded(numRequiredBuffers - numAvailableBuffers);
      }

      if (!creditListener.isRegistered() && creditListener.getNumCreditsNeeded() > 0) {
        listeners.add(creditListener);
        creditListener.setRegistered(true);
      }

      numCredits = Math.min(numAvailableBuffers, numRequiredBuffers);
      if (numCredits > 0) {
        numAvailableBuffers -= numCredits;
        listener = creditListener;
      }

      logger.warn("reserveBuffers,numCredits: {}, required: {}", numCredits, numRequiredBuffers);
    }
    if (listener != null) {
      listener.notifyAvailableCredits(numCredits);
    }
  }

  /** Returns the number of available buffers. */
  public int numBuffers() {
    synchronized (lock) {
      return buffers.size();
    }
  }

  /** Destroys buffer pool. */
  public void destroy() {
    synchronized (lock) {
      isDestroyed = true;
      listeners.clear();
      buffers.forEach(ByteBuf::release);
      buffers.clear();
    }
  }

  /** Returns true if this buffer pool has been destroyed. */
  public boolean isDestroyed() {
    synchronized (lock) {
      return isDestroyed;
    }
  }

  @Override
  public void recycle(ByteBuffer buffer) {
    List<CreditAssignment> creditAssignments;
    synchronized (lock) {
      // unmanaged memory no need to recycle, currently it is used only by tests
      if (isDestroyed) {
        return;
      }

      buffers.add(new Buffer(buffer, this, 0));
      ++numAvailableBuffers;
      creditAssignments = dispatchReservedCredits();
    }
    for (CreditAssignment creditAssignment : creditAssignments) {
      creditAssignment.getCreditListener().notifyAvailableCredits(creditAssignment.getNumCredits());
    }
  }

  private int assignCredits(CreditListener creditListener) {
    assert Thread.holdsLock(lock);

    if (creditListener == null) {
      return 0;
    }

    int numCredits = Math.min(creditListener.getNumCreditsNeeded(), numAvailableBuffers);
    if (numCredits > 0) {
      creditListener.decreaseNumCreditsNeeded(numCredits);
      numAvailableBuffers -= numCredits;
    }

    if (creditListener.getNumCreditsNeeded() > 0) {
      listeners.add(creditListener);
    } else {
      creditListener.setRegistered(false);
    }
    return numCredits;
  }

  private List<CreditAssignment> dispatchReservedCredits() {
    assert Thread.holdsLock(lock);

    if (numAvailableBuffers < MIN_CREDITS_TO_NOTIFY || listeners.size() <= 0) {
      return Collections.emptyList();
    }

    List<CreditAssignment> creditAssignments = new ArrayList<>();
    while (numAvailableBuffers > 0 && listeners.size() > 0) {
      CreditListener creditListener = listeners.poll();
      int numCredits = assignCredits(creditListener);
      if (numCredits > 0) {
        creditAssignments.add(new CreditAssignment(numCredits, creditListener));
      }
    }
    return creditAssignments;
  }

  private static class CreditAssignment {

    private final int numCredits;
    private final CreditListener creditListener;

    CreditAssignment(int numCredits, CreditListener creditListener) {
      checkArgument(numCredits > 0, "Must be positive.");
      checkArgument(creditListener != null, "Must be not null.");

      this.numCredits = numCredits;
      this.creditListener = creditListener;
    }

    public int getNumCredits() {
      return numCredits;
    }

    public CreditListener getCreditListener() {
      return creditListener;
    }
  }
}
