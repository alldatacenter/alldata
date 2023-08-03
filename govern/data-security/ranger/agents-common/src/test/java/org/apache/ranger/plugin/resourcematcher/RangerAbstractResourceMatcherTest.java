/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.ranger.plugin.resourcematcher;

import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.*;

public class RangerAbstractResourceMatcherTest {

    @Test
    public void test_isAllPossibleValues() {
        RangerAbstractResourceMatcher matcher = new AbstractMatcherWrapper();
        for (String resource : new String[] { null, "", "*"}) {
            assertTrue(matcher.isAllValuesRequested(resource));
        }

        for (String resource : new String[] { " ", "\t", "\n", "foo"}) {
            assertFalse(matcher.isAllValuesRequested(resource));
        }
    }

    static class AbstractMatcherWrapper extends RangerAbstractResourceMatcher {

        @Override
        public boolean isMatch(Object resource, Map<String, Object> evalContext) {
            fail("This method is not expected to be used by test!");
            return false;
        }
    }
}
