/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.inlong.tubemq.corebase.utils;

import junit.framework.TestCase;
import org.junit.Test;

public class ConcurrentHashSetTest extends TestCase {

    @Test
    public void test() {
        int count = 0;
        ConcurrentHashSet<String> testSet =
                new ConcurrentHashSet<>();
        for (String item : testSet) {
            System.out.println("Count = " + count++ + ", item = " + item);
        }
        testSet.add("test-1");
        testSet.add("test-2");
        System.out.println("test-2 add time is " + testSet.getItemAddTime("test-2"));
        testSet.add("test-3");
        testSet.add("test-4");
        assertEquals(4, testSet.size());
        assertFalse(testSet.add("test-2"));
        assertEquals(4, testSet.size());
        count = 0;
        for (String item : testSet) {
            System.out.println("Count = " + count++ + ", item = " + item);
        }
        System.out.println("test-2 add time is " + testSet.getItemAddTime("test-2"));
    }

}