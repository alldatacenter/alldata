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

package org.apache.inlong.sort.base.metric;

import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.Assert.assertEquals;

/**
 * Unit testing of {@link ThreadSafeCounter}.
 */
public class ThreadSafeCounterTest {

    @Test
    public void testCounting() {
        final ThreadSafeCounter counter = new ThreadSafeCounter();
        counter.inc();
        assertEquals(1, counter.getCount());
        counter.inc(100);
        assertEquals(101, counter.getCount());
        counter.dec();
        assertEquals(100, counter.getCount());
        counter.dec(99);
        assertEquals(1, counter.getCount());
    }

    @Test(timeout = 10000)
    public void testMultipleThreadCounting() throws Exception {
        final ThreadSafeCounter counter = new ThreadSafeCounter();
        final int threadCount = 3;
        final int incTimes = 100;
        final ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        final CountDownLatch finished = new CountDownLatch(threadCount);
        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                for (int j = 0; j < incTimes; j++) {
                    counter.inc();
                }
                finished.countDown();
            });
        }
        finished.await();
        assertEquals(threadCount * incTimes, counter.getCount());

        executorService.shutdown();
    }
}
