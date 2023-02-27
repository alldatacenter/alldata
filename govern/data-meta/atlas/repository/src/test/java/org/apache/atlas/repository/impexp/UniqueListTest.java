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
package org.apache.atlas.repository.impexp;

import org.apache.atlas.repository.util.UniqueList;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

public class UniqueListTest {
    private final String firstElement = "firstElement";
    private UniqueList<String> uniqueList;

    @BeforeClass
    public void setup() {
        uniqueList = new UniqueList();
        uniqueList.add(firstElement);
        uniqueList.add("def");
        uniqueList.add("firstElement");
        uniqueList.add("ghi");
    }

    @Test
    public void add3Elements_ListHas2() {
        assertEquals(3, uniqueList.size());
    }

    @Test
    public void addAllList_ListHas2() {
        UniqueList<String> uniqueList2 = new UniqueList<>();
        uniqueList2.addAll(uniqueList);

        assertEquals(3, uniqueList2.size());
    }

    @Test
    public void attemptClear_SizeIsZero() {
        UniqueList<String> uniqueList2 = new UniqueList<>();
        uniqueList2.addAll(uniqueList);
        uniqueList2.clear();

        assertEquals(0, uniqueList2.size());
    }

    @Test
    public void attemptOneRemove_SizeIsReduced() {
        UniqueList<String> uniqueList2 = new UniqueList<>();
        uniqueList2.addAll(uniqueList);
        String removedElement = uniqueList2.remove(0);

        assertEquals(2, uniqueList2.size());
        assertEquals(firstElement, removedElement);
    }
}
