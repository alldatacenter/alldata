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

import org.apache.atlas.type.AtlasBuiltInTypes.AtlasDoubleType;
import org.testng.annotations.Test;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import static org.testng.Assert.*;


public class TestAtlasDoubleType {
    private final AtlasDoubleType doubleType  = new AtlasDoubleType();

    private final Object[] validValues = {
        null, Byte.valueOf((byte)1), Short.valueOf((short)1), Integer.valueOf(1), Long.valueOf(1L), Float.valueOf(1),
        Double.valueOf(1), BigInteger.valueOf(1), BigDecimal.valueOf(1), "1",
    };

    private final Object[] validValuesLimitCheck = {Byte.MIN_VALUE, Byte.MAX_VALUE, Short.MIN_VALUE, Short.MAX_VALUE,
            Integer.MIN_VALUE, Integer.MAX_VALUE, Long.MIN_VALUE, Long.MAX_VALUE, Float.MIN_VALUE, Float.MAX_VALUE, Double.MIN_VALUE, Double.MAX_VALUE};

    private final Object[] negativeValues = {
        Byte.valueOf((byte)-1), Short.valueOf((short)-1), Integer.valueOf(-1), Long.valueOf(-1L), Float.valueOf(-1),
        Double.valueOf(-1), BigInteger.valueOf(-1), BigDecimal.valueOf(-1), "-1",
    };

    private  final Object[] negativeValuesLimitCheck = {-Double.MIN_VALUE, -Double.MAX_VALUE, -Float.MIN_VALUE, -Float.MAX_VALUE,};

    private final Object[] invalidValues  = { "", "12ab", "abcd", "-12ab", (Double.MAX_VALUE + Double.MAX_VALUE), -(Double.MAX_VALUE+Double.MAX_VALUE)};


    @Test
    public void testDoubleTypeDefaultValue() {
        Double defValue = doubleType.createDefaultValue();

        assertEquals(defValue, Double.valueOf(0));
    }

    @Test
    public void testDoubleTypeIsValidValue() {
        for (Object value : validValues) {
            assertTrue(doubleType.isValidValue(value), "value=" + value);
        }

        for (Object value : validValuesLimitCheck) {
            assertTrue(doubleType.isValidValue(value), "value=" + value);
        }

        for (Object value : negativeValues) {
            assertTrue(doubleType.isValidValue(value), "value=" + value);
        }

        for (Object value : negativeValuesLimitCheck) {
            assertTrue(doubleType.isValidValue(value), "value=" + value);
        }

        for (Object value : invalidValues) {
            assertFalse(doubleType.isValidValue(value), "value=" + value);
        }
    }

    @Test
    public void testDoubleTypeGetNormalizedValue() {
        assertNull(doubleType.getNormalizedValue(null), "value=" + null);

        for (Object value : validValues) {
            if (value == null) {
                continue;
            }

            Double normalizedValue = doubleType.getNormalizedValue(value);

            assertNotNull(normalizedValue, "value=" + value);
            assertEquals(normalizedValue, Double.valueOf(1), "value=" + value);
        }

        for (Object value : validValuesLimitCheck) {
            if (value == null) {
                continue;
            }

            Double normalizedValue = doubleType.getNormalizedValue(value);

            assertNotNull(normalizedValue, "value=" + value);

            double d;
            if (value instanceof Float) {
                d = ((Float) value).doubleValue();
                assertEquals(normalizedValue, Double.valueOf(d), "value=" + value);
            } else if (value instanceof Double) {
                d = ((Double) value).doubleValue();
                assertEquals(normalizedValue, Double.valueOf(d), "value=" + value);
            } else {
                assertEquals(normalizedValue, Double.valueOf(value.toString()), "value=" + value);
            }
        }

        for (Object value : negativeValues) {
            Double normalizedValue = doubleType.getNormalizedValue(value);

            assertNotNull(normalizedValue, "value=" + value);
            assertEquals(normalizedValue, Double.valueOf(-1), "value=" + value);
        }

        for (Object value : negativeValuesLimitCheck) {
            Double normalizedValue = doubleType.getNormalizedValue(value);
            double d;
            if (value instanceof Float) {
                d = ((Float) value).doubleValue();
                assertEquals(normalizedValue, Double.valueOf(d), "value=" + value);
            } else if (value instanceof Double) {
                d = ((Double) value).doubleValue();
                assertEquals(normalizedValue, Double.valueOf(d), "value=" + value);
            }
        }

        for (Object value : invalidValues) {
            assertNull(doubleType.getNormalizedValue(value), "value=" + value);
        }
    }

    @Test
    public void testDoubleTypeValidateValue() {
        List<String> messages = new ArrayList<>();
        for (Object value : validValues) {
            assertTrue(doubleType.validateValue(value, "testObj", messages));
            assertEquals(messages.size(), 0, "value=" + value);
        }

        for (Object value : negativeValues) {
            assertTrue(doubleType.validateValue(value, "testObj", messages));
            assertEquals(messages.size(), 0, "value=" + value);
        }

        for (Object value : invalidValues) {
            assertFalse(doubleType.validateValue(value, "testObj", messages));
            assertEquals(messages.size(), 1, "value=" + value);
            messages.clear();
        }
    }
}
