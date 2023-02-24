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
package org.apache.atlas.utils;

import org.apache.atlas.model.Clearable;
import org.apache.commons.lang.StringUtils;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

public class FixedBufferListTest {
    private String STR_PREFIX = "str:%s";

    public static class Spying implements Clearable {
        public static AtomicInteger callsToCtor = new AtomicInteger();
        public static AtomicInteger callsToClear = new AtomicInteger();

        private int anInt;
        private String aString;
        private long aLong;

        public Spying() {
            callsToCtor.incrementAndGet();
        }

        @Override
        public void clear() {
            callsToClear.incrementAndGet();

            anInt = 0;
            aString = StringUtils.EMPTY;
            aLong = 0;
        }

        public static void resetCounters() {
            Spying.callsToCtor.set(0);
            Spying.callsToClear.set(0);
        }
    }

    private static class SpyingFixedBufferList extends FixedBufferList<Spying> {
        public SpyingFixedBufferList(int incrementCapacityFactor) {
            super(Spying.class, incrementCapacityFactor);
        }
    }

    @Test
    public void instantiateListWithParameterizedClass() {
        FixedBufferList<Spying> list = new FixedBufferList<>(Spying.class);
        assertNotNull(list);
    }

    @Test
    public void createdBasedOnInitialSize() {
        Spying.resetCounters();

        int incrementByFactor = 2;
        SpyingFixedBufferList fixedBufferList = new SpyingFixedBufferList(incrementByFactor);
        addElements(fixedBufferList, 0, 3);

        List<Spying> list = fixedBufferList.toList();
        assertSpyingList(list, 3);
        assertEquals(Spying.callsToCtor.get(), incrementByFactor * 2);
    }

    @Test (dependsOnMethods = "createdBasedOnInitialSize")
    public void bufferIncreasesIfNeeded() {
        Spying.resetCounters();

        int incrementSizeBy = 5;
        SpyingFixedBufferList fixedBufferList = new SpyingFixedBufferList(incrementSizeBy);
        addElements(fixedBufferList, 0, incrementSizeBy);
        List<Spying> spyings = fixedBufferList.toList();
        assertEquals(spyings.size(), incrementSizeBy);
        assertEquals(Spying.callsToCtor.get(), incrementSizeBy);

        fixedBufferList.reset();
        addElements(fixedBufferList, 0, incrementSizeBy * 2);
        spyings = fixedBufferList.toList();
        assertEquals(Spying.callsToCtor.get(), incrementSizeBy * 2);
        assertSpyingList(spyings, incrementSizeBy * 2);
        assertEquals(Spying.callsToClear.get(), incrementSizeBy);
    }

    @Test
    public void retrieveEmptyList() {
        int size = 5;
        SpyingFixedBufferList fixedBufferList = new SpyingFixedBufferList(size);

        List<Spying> list = fixedBufferList.toList();
        assertEquals(list.size(), 0);

        addElements(fixedBufferList, 0, 3);
        list = fixedBufferList.toList();
        assertEquals(list.size(), 3);
    }

    private void assertSpyingList(List<Spying> list, int expectedSize) {
        assertEquals(list.size(), expectedSize);
        for (int i1 = 0; i1 < list.size(); i1++) {
            Assert.assertNotNull(list.get(i1));
            assertSpying(list.get(i1), i1);
        }
    }

    private void assertSpying(Spying spying, int i) {
        assertEquals(spying.aLong, i);
        assertEquals(spying.anInt, i);
        assertEquals(spying.aString, String.format(STR_PREFIX, i));
    }

    private Spying createSpyingClass(Spying spying, int i) {
        spying.aLong = i;
        spying.anInt = i;
        spying.aString = String.format(STR_PREFIX, i);

        return spying;
    }

    private void addElements(SpyingFixedBufferList fixedBufferList, int startIndex, int numElements) {
        for (int i = startIndex; i < (startIndex + numElements); i++) {
            Spying spyForUpdate = fixedBufferList.next();
            createSpyingClass(spyForUpdate, i);
        }
    }
}
