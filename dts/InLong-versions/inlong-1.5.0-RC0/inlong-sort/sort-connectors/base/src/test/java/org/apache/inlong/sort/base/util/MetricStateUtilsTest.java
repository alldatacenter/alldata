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

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class MetricStateUtilsTest {

    /**
     * test assignment previous subtask index to current subtask when reduce parallelism
     */
    @Test
    public void testComputeIndexList() {
        List<Integer> expectList1 = Arrays.asList(0, 1, 2, 3, 4);
        List<Integer> currentList1 = MetricStateUtils.computeIndexList(0, 2, 10);
        assertEquals(expectList1.toString(), currentList1.toString());

        List<Integer> expectList2 = Arrays.asList(0, 1);
        List<Integer> currentList2 = MetricStateUtils.computeIndexList(0, 4, 10);
        assertEquals(expectList2.toString(), currentList2.toString());

        List<Integer> expectList3 = Arrays.asList(2, 3);
        List<Integer> currentList3 = MetricStateUtils.computeIndexList(1, 4, 10);
        assertEquals(expectList3.toString(), currentList3.toString());

        List<Integer> expectList4 = Arrays.asList(4, 5);
        List<Integer> currentList4 = MetricStateUtils.computeIndexList(2, 4, 10);
        assertEquals(expectList4.toString(), currentList4.toString());

        List<Integer> expectList5 = Arrays.asList(6, 7, 8, 9);
        List<Integer> currentList5 = MetricStateUtils.computeIndexList(3, 4, 10);
        assertEquals(expectList5.toString(), currentList5.toString());

        List<Integer> expectList7 = Arrays.asList(0);
        List<Integer> currentList7 = MetricStateUtils.computeIndexList(0, 3, 4);
        assertEquals(expectList7.toString(), currentList7.toString());

        List<Integer> expectList8 = Arrays.asList(2, 3);
        List<Integer> currentList8 = MetricStateUtils.computeIndexList(2, 3, 4);
        assertEquals(expectList8.toString(), currentList8.toString());
    }

}
