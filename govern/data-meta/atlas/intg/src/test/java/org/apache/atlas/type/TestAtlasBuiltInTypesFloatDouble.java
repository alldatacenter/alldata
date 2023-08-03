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
package org.apache.atlas.type;

import org.testng.annotations.Test;

import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;

public class TestAtlasBuiltInTypesFloatDouble {

    @Test
    public void floatRangeCheck() {
        assertFloatChecks(String.valueOf("-1.E-45"), true);
        assertFloatChecks(String.valueOf(Float.MAX_VALUE), true);
        assertFloatChecks("3.4028235E32", true);
        assertFloatChecks("-3.4028235E32", true);
        assertFloatChecks(String.valueOf(Float.MIN_VALUE), true);
        assertFloatChecks("4028235e+555", false);
        assertFloatChecks("-4028235e+555", false);
    }

    @Test
    public void doubleRangeCheck() {
        assertDoubleChecks(String.valueOf(Double.MAX_VALUE), true);
        assertDoubleChecks("3.4028235E32", true);
        assertDoubleChecks(String.valueOf(Double.MIN_VALUE), true);
        assertDoubleChecks("4028235e+55555", false);
        assertDoubleChecks("-4028235e+55555", false);
    }

    private void assertFloatChecks(String v, boolean notNull) {
        assertNullNotNull(notNull, new AtlasBuiltInTypes.AtlasFloatType().getNormalizedValue(v));
    }


    private void assertDoubleChecks(String v, boolean notNull) {
        assertNullNotNull(notNull, new AtlasBuiltInTypes.AtlasDoubleType().getNormalizedValue(v));
    }

    private void assertNullNotNull(boolean notNull, Object f) {
        if(notNull) {
            assertNotNull(f);
        } else {
            assertNull(f);
        }
    }
}
