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

import org.apache.commons.lang3.RandomUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.BlockingQueue;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class WorkItemManagerWithResultsTest {
    private static final Logger LOG = LoggerFactory.getLogger(WorkItemManagerWithResultsTest.class);

    private static class IntegerConsumer extends WorkItemConsumer<Integer> {
        private static ThreadLocal<Integer> payload = new ThreadLocal<Integer>();

        public IntegerConsumer(BlockingQueue<Integer> queue) {
            super(queue);
        }

        @Override
        protected void doCommit() {
            if (getPayload() == -1) {
                LOG.debug("Skipping:");
                return;
            }

            incrementPayload(100);
            addResult(getPayload());

            setPayload(0);
        }

        @Override
        protected void processItem(Integer item) {
            try {
                setPayload(item.intValue());
                Thread.sleep(20 + RandomUtils.nextInt(5, 7));
                super.commit();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        public void setPayload(int v) {
            payload.set(v);
        }

        public int getPayload() {
            return payload.get();
        }

        public void incrementPayload(int v) {
            payload.set(payload.get() + v);
        }
    }

    private class IntegerConsumerBuilder implements WorkItemBuilder<IntegerConsumer, Integer> {
        @Override
        public IntegerConsumer build(BlockingQueue<Integer> queue) {
            return new IntegerConsumer(queue);
        }
    }

    private WorkItemManager<Integer, WorkItemConsumer> getWorkItemManger(IntegerConsumerBuilder cb, int numWorkers) {
        return new WorkItemManager<>(cb, "IntegerConsumer", 5, numWorkers, true);
    }

    @Test
    public void drainTest() throws InterruptedException {
        final int maxItems = 50;

        IntegerConsumerBuilder cb = new IntegerConsumerBuilder();
        WorkItemManager<Integer, WorkItemConsumer> wi = getWorkItemManger(cb, 5);

        for (int i = 0; i < maxItems; i++) {
            wi.produce(i);
        }

        wi.drain();
        assertEquals(wi.getResults().size(), maxItems);
        Set<Integer> set = new HashSet<Integer>(wi.getResults());
        assertEquals(set.size(), maxItems);

        wi.shutdown();
    }

    @Test
    public void drainCheckProduceTest() throws InterruptedException {
        IntegerConsumerBuilder cb = new IntegerConsumerBuilder();
        WorkItemManager<Integer, WorkItemConsumer> wi = getWorkItemManger(cb, 2);

        for (int i = 0; i < 5; i++) {
            repeatedDrainAndProduce(i, wi);
        }

        wi.shutdown();
    }

    private void repeatedDrainAndProduce(int runCount, WorkItemManager<Integer, WorkItemConsumer> wi) {
        final int maxItems = 100;
        int halfWay = maxItems / 2;

        LOG.info("Run: {}", runCount);
        wi.getResults().clear();

        for (int i = 0; i < maxItems; i++) {
            if (i == halfWay) {
                wi.drain();

                Set<Integer> set = new HashSet<Integer>(wi.getResults());
                assertEquals(wi.getResults().size(), halfWay, "halfWay: total count");
                assertEquals(set.size(), halfWay, "halfWay: set match");
            }

            wi.checkProduce(i);
        }

        wi.drain();
        assertEquals(wi.getResults().size(), maxItems, "total count");
        Set<Integer> set = new HashSet<Integer>(wi.getResults());
        assertEquals(set.size(), maxItems, "set count");

        for (int i = 100; i < 100 + maxItems; i++) {
            assertTrue(set.contains(i), "Could not test: " + i);
        }
    }
}
