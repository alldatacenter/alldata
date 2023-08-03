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
package org.apache.atlas.repository.store.graph.v2;

import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.slf4j.Logger;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.Matchers.*;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class BulkImportPercentTest {

    private final int MAX_PERCENT = 100;
    private final float MAX_PERCENT_FLOAT = 100.0F;
    private List<Integer> percentHolder;
    private Logger log;

    public void setupPercentHolder(long max) {
        percentHolder = new ArrayList<>();
    }

    @BeforeClass
    void mockLog() {
        log = mock(Logger.class);

        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                Object[] args = invocationOnMock.getArguments();
                Integer d = (Integer) args[1];
                percentHolder.add(d.intValue());
                return null;
            }
        }).when(log).info(anyString(), anyInt(), anyLong(), anyString());
    }

    @Test
    public void percentTest_Equal4() throws Exception {
        runWithSize(4);
        assertEqualsForPercentHolder(25.0, 50.0, 75.0, 100.0);
    }

    @Test
    public void percentTest_Equal10() throws Exception {
        runWithSize(10);

        assertEqualsForPercentHolder(10.0, 20.0, 30.0, 40.0, 50, 60, 70, 80, 90, 100);
    }

    private void assertEqualsForPercentHolder(double... expected) {
        assertEquals(percentHolder.size(), expected.length);
        Object actual[] = percentHolder.toArray();
        for (int i = 0; i < expected.length; i++) {
            assertTrue((int) Double.compare((int) actual[i], expected[i]) == 0);
        }
    }

    @Test
    public void bulkImportPercentageTestLessThan100() throws Exception {
        int streamSize = 20;

        runWithSize(streamSize);
        assertEqualsForPercentHolder(5, 10, 15, 20, 25, 30, 35, 40, 45, 50, 55, 60, 65, 70, 75, 80, 85, 90, 95, 100);
    }

    @Test
    public void percentTest_Equal101() throws Exception {
        int streamSize = 101;

        double[] expected = fillPercentHolderWith100();

        runWithSize(streamSize);
        assertEqualsForPercentHolder(expected);
    }

    @Test
    public void percentTest_Equal200() throws Exception {
        int streamSize = 200;

        double[] expected = fillPercentHolderWith100();

        runWithSize(streamSize);
        assertEqualsForPercentHolder(expected);
    }

    @Test
    public void percentTest_Equal202() throws Exception {
        int streamSize = 202;

        double[] expected = fillPercentHolderWith100();

        runWithSize(streamSize);
        assertEqualsForPercentHolder(expected);
    }

    @Test
    public void percentTest_Equal1001() throws Exception {
        int streamSize = 1001;
        double[] expected = fillPercentHolderWith100();

        runWithSize(streamSize);
        assertEqualsForPercentHolder(expected);
    }

    @Test
    public void percentTest_Equal100M() throws Exception {
        long streamSize = 100000000;
        double[] expected = fillPercentHolderWith100();

        runWithSize(streamSize);
        assertEqualsForPercentHolder(expected);
    }

    @Test
    public void percentTest_Equal4323() throws Exception {
        int streamSize = 4323;

        double[] expected = fillPercentHolderWith100();
        runWithSize(streamSize);
        assertEqualsForPercentHolder(expected);
    }

    @Test
    public void percentTest_Equal269() throws Exception {
        int streamSize = 269;

        double[] expected = fillPercentHolderWith100();
        runWithSize(streamSize);
        assertEqualsForPercentHolder(expected);
    }

    @Test
    public void exceedingInitialStreamSize_KeepsPercentAt100() throws Exception {
        runWithSize(4);
        double[] expected = fillPercentHolderWith100();
        float f = BulkImporterImpl.updateImportProgress(log, 5, 4, 100, "additional info");

        assertTrue((f - MAX_PERCENT_FLOAT) <= 0.0001);
    }

    private void runWithSize(long streamSize) throws Exception {
        float currentPercent = 0;
        setupPercentHolder(streamSize);
        for (int currentIndex = 0; currentIndex < streamSize; currentIndex++) {
            currentPercent = invokeBulkImportProgress(currentIndex + 1, streamSize, currentPercent);
        }
    }

    private float invokeBulkImportProgress(int currentIndex, long streamSize, float currentPercent) throws Exception {
        return BulkImporterImpl.updateImportProgress(log, currentIndex, streamSize, currentPercent, "additional info");
    }

    private double[] fillPercentHolderWith100() {
        double start = 1;
        double expected[] = new double[MAX_PERCENT];
        for (int i = 0; i < expected.length; i++) {
            expected[i] = start;
            start ++;
        }
        return expected;
    }

    @Test
    public void jsonArrayTest() {
        String t1 = "123-abcd";
        String t2 = "456-efgh";

        String jsonArray = BulkImporterImpl.getJsonArray(null, t1);
        assertEquals(jsonArray, String.format("[\"%s\"]", t1));
        assertEquals(BulkImporterImpl.getJsonArray(jsonArray, t2), String.format("[\"%s\",\"%s\"]", t1, t2));
    }

}
