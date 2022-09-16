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

import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * SizeSemaphore
 */
public class SizeSemaphore {

    public static final int ONEKB = 1024;

    private int maxSize = 0;
    private int leftSize = 0;
    private Semaphore sizeSemaphore = null;
    private AtomicInteger leftSemaphore = new AtomicInteger(0);

    /**
     * Constructor
     * 
     * @param maxSize
     * @param leftSize
     */
    public SizeSemaphore(int maxSize, int leftSize) {
        this.maxSize = maxSize;
        this.leftSize = leftSize;
        this.sizeSemaphore = new Semaphore(maxSize, true);
    }

    /**
     * small change
     */
    public int leftSemaphore() {
        return leftSemaphore.get();
    }

    /**
     * availablePermits
     */
    public int availablePermits() {
        return sizeSemaphore.availablePermits();
    }

    /**
     * maxSize
     */
    public int maxSize() {
        return maxSize;
    }

    /**
     * getIdleRate
     */
    public double getIdleRate() {
        double remaining = sizeSemaphore.availablePermits();
        return remaining * 100.0 / maxSize;
    }

    /**
     * tryAcquire
     */
    public boolean tryAcquire(long sizeInByte) {
        int sizeInKb = (int) (sizeInByte / leftSize);
        int sizeChange = (int) (sizeInByte % leftSize);
        if (leftSemaphore.get() - sizeChange < 0) {
            boolean result = sizeSemaphore.tryAcquire(sizeInKb + 1);
            if (result) {
                leftSemaphore.addAndGet(-sizeChange + leftSize);
            }
            return result;
        } else {
            boolean result = sizeSemaphore.tryAcquire(sizeInKb);
            if (result) {
                leftSemaphore.addAndGet(-sizeChange);
            }
            return result;
        }
    }

    /**
     * acquire
     */
    public void acquire(long sizeInByte) {
        int sizeInKb = (int) (sizeInByte / leftSize);
        int sizeChange = (int) (sizeInByte % leftSize);
        if (leftSemaphore.get() - sizeChange < 0) {
            sizeSemaphore.acquireUninterruptibly(sizeInKb + 1);
            leftSemaphore.addAndGet(-sizeChange + leftSize);
        } else {
            sizeSemaphore.acquireUninterruptibly(sizeInKb);
            leftSemaphore.addAndGet(-sizeChange);
        }
    }

    /**
     * release
     */
    public void release(long sizeInByte) {
        int sizeInKb = (int) (sizeInByte / leftSize);
        int sizeChange = (int) (sizeInByte % leftSize);
        if (leftSemaphore.get() + sizeChange > leftSize) {
            sizeSemaphore.release(sizeInKb + 1);
            leftSemaphore.addAndGet(sizeChange - leftSize);
        } else {
            sizeSemaphore.release(sizeInKb);
            leftSemaphore.addAndGet(sizeChange);
        }
    }
}
