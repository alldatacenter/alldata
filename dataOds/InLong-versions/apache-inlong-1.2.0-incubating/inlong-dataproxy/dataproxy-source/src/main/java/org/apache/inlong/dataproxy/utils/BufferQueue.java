/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.inlong.dataproxy.utils;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicLong;

/**
 * BufferQueue
 */
public class BufferQueue<A> {

    private final LinkedBlockingQueue<A> queue;
    private final SizeSemaphore currentTokens;
    private SizeSemaphore globalTokens;
    private final AtomicLong offerCount = new AtomicLong(0);
    private final AtomicLong pollCount = new AtomicLong(0);

    /**
     * Constructor
     * 
     * @param maxSizeKb
     */
    public BufferQueue(int maxSizeKb) {
        this.queue = new LinkedBlockingQueue<>();
        this.currentTokens = new SizeSemaphore(maxSizeKb, SizeSemaphore.ONEKB);
    }

    /**
     * Constructor
     * 
     * @param maxSizeKb
     * @param globalTokens
     */
    public BufferQueue(int maxSizeKb, SizeSemaphore globalTokens) {
        this(maxSizeKb);
        this.globalTokens = globalTokens;
    }

    /**
     * pollRecord
     */
    public A pollRecord() {
        A record = queue.poll();
        this.pollCount.getAndIncrement();
        return record;
    }

    /**
     * offer
     */
    public void offer(A record) {
        if (record == null) {
            return;
        }
        queue.offer(record);
        this.offerCount.incrementAndGet();
    }

    /**
     * queue size
     */
    public int size() {
        return queue.size();
    }

    /**
     * small change
     */
    public int leftKb() {
        return currentTokens.leftSemaphore();
    }

    /**
     * availablePermits
     */
    public int availablePermits() {
        return currentTokens.availablePermits();
    }

    /**
     * maxSizeKb
     */
    public int maxSizeKb() {
        return currentTokens.maxSize();
    }

    /**
     * getIdleRate
     */
    public double getIdleRate() {
        double remaining = currentTokens.availablePermits();
        return remaining * 100.0 / currentTokens.maxSize();
    }

    /**
     * tryAcquire
     */
    public boolean tryAcquire(long sizeInByte) {
        boolean cidResult = currentTokens.tryAcquire(sizeInByte);
        if (!cidResult) {
            return false;
        }
        if (this.globalTokens == null) {
            return true;
        }
        boolean globalResult = this.globalTokens.tryAcquire(sizeInByte);
        if (globalResult) {
            return true;
        }
        currentTokens.release(sizeInByte);
        return false;
    }

    /**
     * acquire
     */
    public void acquire(long sizeInByte) {
        currentTokens.acquire(sizeInByte);
        if (this.globalTokens != null) {
            globalTokens.acquire(sizeInByte);
        }
    }

    /**
     * release
     */
    public void release(long sizeInByte) {
        if (this.globalTokens != null) {
            this.globalTokens.release(sizeInByte);
        }
        this.currentTokens.release(sizeInByte);
    }

    /**
     * get offerCount
     * 
     * @return the offerCount
     */
    public long getOfferCount() {
        return offerCount.getAndSet(0);
    }

    /**
     * get pollCount
     * 
     * @return the pollCount
     */
    public long getPollCount() {
        return pollCount.getAndSet(0);
    }
}
