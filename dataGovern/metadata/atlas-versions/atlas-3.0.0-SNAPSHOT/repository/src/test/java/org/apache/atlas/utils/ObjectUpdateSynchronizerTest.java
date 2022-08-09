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
package org.apache.atlas.utils;

import org.apache.atlas.GraphTransactionInterceptor;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.RandomStringUtils;
import org.springframework.util.CollectionUtils;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;

public class ObjectUpdateSynchronizerTest {
    private static final GraphTransactionInterceptor.ObjectUpdateSynchronizer objectUpdateSynchronizer = new GraphTransactionInterceptor.ObjectUpdateSynchronizer();

    private final List<Integer> outputList = new ArrayList<>();
    private final int MAX_COUNT = 10;

    class CounterThread extends Thread {
        String ids[];
        public CounterThread(String id) {
            this.ids = new String[1];
            this.ids[0] = id;
        }

        public void setIds(String... ids) {
            this.ids = ids;
        }

        public void run() {
            objectUpdateSynchronizer.lockObject(CollectionUtils.arrayToList(ids));
            for (int i = 0; i < MAX_COUNT; i++) {
                outputList.add(i);
                RandomStringUtils.randomAlphabetic(20);
            }

            objectUpdateSynchronizer.releaseLockedObjects();
        }
    }

    @BeforeMethod
    public void clearOutputList() {
        outputList.clear();
    }

    @Test
    public void singleThreadRun() throws InterruptedException {
        verifyMultipleThreadRun(1);
    }

    @Test
    public void twoThreadsAccessingDifferntGuids_DoNotSerialize() throws InterruptedException {
        CounterThread th[] = getCounterThreads(false, 2);

        startCounterThreads(th);
        waitForThreadsToEnd(th);
        assertArrayNotEquals(populateExpectedArrayOutput(2));
    }

    @Test
    public void twoThreadsAccessingSameGuid_Serialize() throws InterruptedException {
        verifyMultipleThreadRun(2);
    }

    @Test
    public void severalThreadsAccessingSameGuid_Serialize() throws InterruptedException {
        verifyMultipleThreadRun(10);
    }

    @Test
    public void severalThreadsSequentialAccessingListOfGuids() throws InterruptedException {
        CounterThread th[] = getCounterThreads(false, 10);
        int i = 0;
        th[i++].setIds("1", "2", "3", "4", "5");
        th[i++].setIds("1", "2", "3", "4");
        th[i++].setIds("1", "2", "3");
        th[i++].setIds("1", "2");
        th[i++].setIds("1");
        th[i++].setIds("1", "2");
        th[i++].setIds("1", "2", "3");
        th[i++].setIds("1", "2", "3", "4");
        th[i++].setIds("1", "2", "3", "4", "5");
        th[i++].setIds("1");

        startCounterThreads(th);
        waitForThreadsToEnd(th);
        assertArrayEquals(populateExpectedArrayOutput(th.length));
    }

    @Test
    public void severalThreadsNonSequentialAccessingListOfGuids() throws InterruptedException {
        CounterThread th[] = getCounterThreads(false, 5);
        int i = 0;
        th[i++].setIds("2", "1", "3", "4", "5");
        th[i++].setIds("3", "2", "4", "1");
        th[i++].setIds("2", "3", "1");
        th[i++].setIds("1", "2");
        th[i++].setIds("1");

        startCounterThreads(th);
        waitForThreadsToEnd(th);
        assertArrayEquals(populateExpectedArrayOutput(th.length));
    }

    @Test
    public void severalThreadsAccessingOverlappingListOfGuids() throws InterruptedException {
        CounterThread th[] = getCounterThreads(false, 5);
        int i = 0;
        th[i++].setIds("1", "2", "3", "4", "5");
        th[i++].setIds("3", "4", "5", "6");
        th[i++].setIds("5", "6", "7");
        th[i++].setIds("7", "8");
        th[i++].setIds("8");

        startCounterThreads(th);
        waitForThreadsToEnd(th);
        assertArrayNotEquals(populateExpectedArrayOutput(th.length));
    }


    @Test
    public void severalThreadsAccessingOverlappingListOfGuids2() throws InterruptedException {
        CounterThread th[] = getCounterThreads(false, 3);
        int i = 0;
        th[i++].setIds("1", "2", "3", "4", "5");
        th[i++].setIds("6", "7", "8", "9");
        th[i++].setIds("4", "5", "6");

        startCounterThreads(th);
        waitForThreadsToEnd(th);
        assertArrayNotEquals(populateExpectedArrayOutput(th.length));
    }

    @Test
    public void severalThreadsAccessingOverlappingListOfGuidsEnsuringSerialOutput() throws InterruptedException {
        CounterThread th[] = getCounterThreads(false, 5);
        int i = 0;
        th[i++].setIds("1", "2", "3", "4", "7");
        th[i++].setIds("3", "4", "5", "7");
        th[i++].setIds("5", "6", "7");
        th[i++].setIds("7", "8");
        th[i++].setIds("7");

        startCounterThreads(th);
        waitForThreadsToEnd(th);
        assertArrayEquals(populateExpectedArrayOutput(th.length));
    }

    private void verifyMultipleThreadRun(int limit) throws InterruptedException {
        CounterThread[] th = getCounterThreads(limit);
        startCounterThreads(th);
        waitForThreadsToEnd(th);
        assertArrayEquals(populateExpectedArrayOutput(limit));
    }

    private void startCounterThreads(CounterThread[] th) {
        for (int i = 0; i < th.length; i++) {
            th[i].start();
        }
    }
    private CounterThread[] getCounterThreads(int limit) {
        return getCounterThreads(true, limit);
    }

    private CounterThread[] getCounterThreads(boolean sameId, int limit) {
        CounterThread th[] = new CounterThread[limit];
        for (Integer i = 0; i < limit; i++) {
            th[i] = new CounterThread(sameId ? "1" : i.toString());
        }
        return th;
    }


    private void assertArrayEquals(List<Integer> expected) {
        assertEquals(outputList.toArray(), expected.toArray());
    }

    private void assertArrayNotEquals(List<Integer> expected) {
        assertFalse(ArrayUtils.isEquals(outputList.toArray(), expected));
    }

    private void waitForThreadsToEnd(CounterThread... threads) throws InterruptedException {
        for (Thread t : threads) {
            t.join();
        }
    }

    private List<Integer> populateExpectedArrayOutput(int limit) {
        List<Integer> list = new ArrayList<>();
        for (int i = 0; i < limit*MAX_COUNT; i+=MAX_COUNT) {
            for (int j = 0; j < MAX_COUNT; j++) {
                list.add(j);
            }
        }

        return list;
    }
}
