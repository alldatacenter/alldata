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
package org.apache.atlas.glossary;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.testng.Assert.assertEquals;

public class PaginationHelperTest {
    @DataProvider
    public Object[][] paginationProvider() {
        return new Object[][] {
                // maxSize, offset, limit, expected
                {10, 0 , 5, 5},
                {10, 0 , -1, 10},
                {10, -1 , -1, 10},
                {10, -1 , 5, 5},
                {10, -1 , 11, 10},
                {10, 5 , 11, 5},
                {10, 2 , 11, 8},
                {10, 8 , 11, 2},
                {10, 5 , 1, 1},
                {10, 9 , 1, 1},
                {10, 10 , 5, 0},
                {10, 11 , 5, 0},
                {10, 20 , -1, 0},
                {5, 6 , -1, 0},
                {0, -1 , -1, 0},
                {0, 5 , 10, 0},
                {0, 0 , 10, 0},
                {1, -1 , -1, 1},
                {1, 5 , 10, 0},
                {1, 0 , 10, 1},
        };
    }

    @Test(dataProvider = "paginationProvider")
    public void testPaginationHelper(int maxSize, int offset, int limit, int expectedSize) {
        List<Integer> intList = IntStream.range(0, maxSize).boxed().collect(Collectors.toCollection(() -> new ArrayList<>(maxSize)));
        GlossaryService.PaginationHelper helper = new GlossaryService.PaginationHelper<>(intList, offset, limit);
        assertEquals(helper.getPaginatedList().size(), expectedSize);
    }
}
