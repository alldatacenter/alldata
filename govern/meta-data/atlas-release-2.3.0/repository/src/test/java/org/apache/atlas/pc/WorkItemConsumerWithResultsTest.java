/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.atlas.pc;

import org.testng.annotations.Test;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class WorkItemConsumerWithResultsTest {
    private class IntegerConsumerSpy extends WorkItemConsumer<Integer> {
        int payload = -1;

        public IntegerConsumerSpy(BlockingQueue<Integer> queue) {
            super(queue);
        }

        @Override
        protected void doCommit() {
            addResult(payload);
        }

        @Override
        protected void processItem(Integer item) {
            payload = item.intValue();
        }

        @Override
        protected void commitDirty() {
            super.commitDirty();
        }
    }

    private class IntegerConsumerThrowingError extends WorkItemConsumer<Integer> {
        int payload = -1;

        public IntegerConsumerThrowingError(BlockingQueue<Integer> queue) {
            super(queue);
        }

        @Override
        protected void doCommit() {
            throw new NullPointerException();
        }

        @Override
        protected void processItem(Integer item) {
            payload = item.intValue();
        }

        @Override
        protected void commitDirty() {
            super.commitDirty();
        }
    }

    @Test
    public void runningConsumerWillPopulateResults() {
        CountDownLatch countDownLatch = new CountDownLatch(1);
        BlockingQueue<Integer> bc = new LinkedBlockingQueue<>(5);
        LinkedBlockingQueue<Object> results = new LinkedBlockingQueue<>();

        IntegerConsumerSpy ic = new IntegerConsumerSpy(bc);
        ic.setResults(results);
        ic.setCountDownLatch(countDownLatch);
        ic.run();

        assertTrue(bc.isEmpty());
        assertEquals(results.size(), bc.size());
        assertEquals(countDownLatch.getCount(), 0);
    }

    @Test
    public void errorInConsumerWillDecrementCountdownLatch() {
        CountDownLatch countDownLatch = new CountDownLatch(1);
        BlockingQueue<Integer> bc = new LinkedBlockingQueue<>(5);
        LinkedBlockingQueue<Object> results = new LinkedBlockingQueue<>();

        IntegerConsumerThrowingError ic = new IntegerConsumerThrowingError(bc);
        ic.setCountDownLatch(countDownLatch);
        ic.setResults(results);
        ic.run();

        assertTrue(bc.isEmpty());
        assertTrue(results.isEmpty());
        assertEquals(countDownLatch.getCount(), 0);
    }
}
