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

import java.util.ArrayList;
import java.util.List;

import org.apache.atlas.type.AtlasBuiltInTypes.AtlasBooleanType;
import org.testng.annotations.Test;

import static org.testng.Assert.*;


public class TestAtlasBooleanType {
    private final AtlasBooleanType booleanType = new AtlasBooleanType();
    private final Object[] validValues   = { null, Boolean.TRUE, Boolean.FALSE, "true", "false", "TRUE", "FALSE", "tRue", "FaLse" };
    private final Object[] invalidValues = {1, 0.5,123456789, "abcd", "101010" };

    @Test
    public void testBooleanTypeDefaultValue() {
        Boolean defValue = booleanType.createDefaultValue();

        assertFalse(defValue);
    }

    @Test
    public void testBooleanTypeIsValidValue() {
        for (Object value : validValues) {
            assertTrue(booleanType.isValidValue(value), "value=" + value);
        }

        for (Object value : invalidValues) {
            assertFalse(booleanType.isValidValue(value), "value=" + value);
        }
    }

    @Test
    public void testBooleanTypeGetNormalizedValue() {
        Object[] trueValues  = { Boolean.TRUE, "true", "TRUE", "tRuE", "TrUe" };
        Object[] falseValues = { Boolean.FALSE, "false", "FALSE", "fAlSe", "FaLsE" };

        assertNull(booleanType.getNormalizedValue(null), "value=" + null);

        for (Object value : trueValues) {
            Boolean normalizedValue = booleanType.getNormalizedValue(value);

            assertNotNull(normalizedValue, "value=" + value);
            assertTrue(normalizedValue, "value=" + value);
        }

        for (Object value : falseValues) {
            Boolean normalizedValue = booleanType.getNormalizedValue(value);

            assertNotNull(normalizedValue, "value=" + value);
            assertFalse(normalizedValue, "value=" + value);
        }
    }

    @Test
    public void testBooleanTypeValidateValue() {
        List<String> messages = new ArrayList<>();
        for (Object value : validValues) {
            assertTrue(booleanType.validateValue(value, "testObj", messages));
            assertEquals(messages.size(), 0, "value=" + value);
        }

        for (Object value : invalidValues) {
            assertFalse(booleanType.validateValue(value, "testObj", messages));
            assertEquals(messages.size(), 1, "value=" + value);
            messages.clear();
        }
    }
}
