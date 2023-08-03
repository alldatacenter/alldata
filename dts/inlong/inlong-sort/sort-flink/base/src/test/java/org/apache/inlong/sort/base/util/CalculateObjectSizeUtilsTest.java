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

package org.apache.inlong.sort.base.util;

import org.apache.flink.core.memory.MemorySegmentFactory;
import org.apache.flink.table.data.binary.BinaryRowData;
import org.junit.Assert;
import org.junit.Test;

/**
 * Test for {@link CalculateObjectSizeUtils}
 */
public class CalculateObjectSizeUtilsTest {

    public static final BinaryRowData EMPTY_ROW;

    static {
        EMPTY_ROW = new BinaryRowData(0);
        int size = EMPTY_ROW.getFixedLengthPartSize();
        byte[] bytes = new byte[size];
        EMPTY_ROW.pointTo(MemorySegmentFactory.wrap(bytes), 0, size);
    }

    @Test
    public void testGetDataSize() {
        String data1 = null;
        long expected1 = 0L;
        long actual1 = CalculateObjectSizeUtils.getDataSize(data1);
        Assert.assertEquals(expected1, actual1);

        String data2 = "test";
        long expected2 = 4L;
        long actual2 = CalculateObjectSizeUtils.getDataSize(data2);
        Assert.assertEquals(expected2, actual2);

        long expected3 = 8L;
        long actual3 = CalculateObjectSizeUtils.getDataSize(EMPTY_ROW);
        Assert.assertEquals(expected3, actual3);
    }
}
