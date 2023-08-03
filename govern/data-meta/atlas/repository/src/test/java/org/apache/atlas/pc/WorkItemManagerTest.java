/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.atlas.pc;

import org.apache.commons.lang3.RandomUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.SkipException;
import org.testng.annotations.Test;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentLinkedQueue;

import static org.testng.Assert.assertEquals;

public class WorkItemManagerTest {
    private static final Logger LOG = LoggerFactory.getLogger(WorkItemManagerTest.class);

    private class IntegerConsumer extends WorkItemConsumer<Integer> {

        private final ConcurrentLinkedQueue<Integer> target;

        public IntegerConsumer(BlockingQueue<Integer> queue, ConcurrentLinkedQueue<Integer> target) {
            super(queue);
            this.target = target;
        }

        @Override
        protected void doCommit() {
            try {
                Thread.sleep(20 * RandomUtils.nextInt(10, 15));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        @Override
        protected void processItem(Integer item) {
            LOG.info("adding: {}: size: {}", item, target.size());
            target.add(item);
        }
    }

    private class IntegerConsumerBuilder implements WorkItemBuilder<IntegerConsumer, Integer> {
        ConcurrentLinkedQueue<Integer> integers = new ConcurrentLinkedQueue<>();

        @Override
        public IntegerConsumer build(BlockingQueue<Integer> queue) {
            return new IntegerConsumer(queue, integers);
        }
    }

    @Test
    public void oneWorkerSequences() {
        IntegerConsumerBuilder cb = new IntegerConsumerBuilder();
        int numberOfItems = 10;
        try {
            WorkItemManager<Integer, WorkItemConsumer> wi = getWorkItemManger(cb, 1);
            for (int i = 0; i < numberOfItems; i++) {
                wi.produce(i);
            }

            wi.shutdown();
        } catch (InterruptedException e) {
            throw new SkipException("Test skipped!");
        }

        assertEquals(cb.integers.size(), numberOfItems);
        Integer[] ints = cb.integers.toArray(new Integer[]{});
        for (int i = 0; i < numberOfItems; i++) {
            assertEquals(ints[i], i, i);
        }
    }


    @Test
    public void multipleWorkersUnpredictableSequence() {
        IntegerConsumerBuilder cb = new IntegerConsumerBuilder();
        int numberOfItems = 100;
        try {
            WorkItemManager<Integer, WorkItemConsumer> wi = getWorkItemManger(cb, 5);
            for (int i = 0; i < numberOfItems; i++) {
                wi.produce(i);
            }

            wi.shutdown();
        } catch (InterruptedException e) {
            throw new SkipException("Test skipped!");
        }

        assertEquals(cb.integers.size(), numberOfItems);
    }

    private WorkItemManager<Integer, WorkItemConsumer> getWorkItemManger(IntegerConsumerBuilder cb, int numWorkers) {
        return new WorkItemManager<>(cb, 5, numWorkers);
    }
}
