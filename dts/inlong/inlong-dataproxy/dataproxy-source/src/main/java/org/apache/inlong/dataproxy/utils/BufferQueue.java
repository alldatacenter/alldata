/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
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
    private SizeSemaphore globalTokens = null;
    private final AtomicLong offerCount = new AtomicLong(0);
    private final AtomicLong pollCount = new AtomicLong(0);
    private final AtomicLong takeCount = new AtomicLong(0);

    /**
     * Constructor
     * 
     * @param maxSizeKb  the initial size of permits to acquire
     */
    public BufferQueue(int maxSizeKb) {
        this.queue = new LinkedBlockingQueue<>();
        this.currentTokens = new SizeSemaphore(maxSizeKb, SizeSemaphore.ONEKB);
    }

    /**
     * Constructor
     * 
     * @param maxSizeKb    the initial size of permits to acquire
     * @param globalTokens the global permit semaphore
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
     * Take record
     */
    public A takeRecord() {
        try {
            A record = queue.take();
            this.takeCount.incrementAndGet();
            return record;
        } catch (Throwable e) {
            return null;
        }
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
     * try to acquire required size permit
     *
     * @param sizeInByte  the size of permits to acquire
     * @return  true if the permits were acquired and false otherwise
     */
    public boolean tryAcquire(long sizeInByte) {
        if (this.globalTokens == null) {
            return currentTokens.tryAcquire(sizeInByte);
        } else {
            if (!this.globalTokens.tryAcquire(sizeInByte)) {
                return false;
            }
            if (currentTokens.tryAcquire(sizeInByte)) {
                return true;
            } else {
                this.globalTokens.release(sizeInByte);
                return false;
            }
        }
    }

    /**
     * acquire size permit
     *
     * @param sizeInByte the size of permits to acquire
     */
    public void acquire(long sizeInByte) {
        if (this.globalTokens != null) {
            globalTokens.acquire(sizeInByte);
        }
        currentTokens.acquire(sizeInByte);
    }

    /**
     * release size permit
     *
     * @param sizeInByte the size of permits to release
     */
    public void release(long sizeInByte) {
        this.currentTokens.release(sizeInByte);
        if (this.globalTokens != null) {
            this.globalTokens.release(sizeInByte);
        }
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

    /**
     * get takeCount
     *
     * @return the take count
     */
    public long getTakeCount() {
        return takeCount.getAndSet(0);
    }
}
