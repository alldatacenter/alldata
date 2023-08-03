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

import org.apache.atlas.type.AtlasBuiltInTypes.AtlasFloatType;
import org.testng.annotations.Test;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import static org.testng.Assert.*;


public class TestAtlasFloatType {
    private final AtlasFloatType floatType  = new AtlasFloatType();

    private final Object[] validValues = {
        null, Byte.valueOf((byte)1), Short.valueOf((short)1), Integer.valueOf(1), Long.valueOf(1L), Float.valueOf(1),
        Double.valueOf(1), BigInteger.valueOf(1), BigDecimal.valueOf(1), "1",
    };

    private final Object[] validValuesLimitCheck = {Byte.MIN_VALUE, Byte.MAX_VALUE, Short.MIN_VALUE, Short.MAX_VALUE,
            Integer.MIN_VALUE, Integer.MAX_VALUE, Long.MIN_VALUE, Long.MAX_VALUE, Float.MIN_VALUE, Float.MAX_VALUE, Double.MIN_VALUE};

    private final Object[] negativeValues = {
        Byte.valueOf((byte)-1), Short.valueOf((short)-1), Integer.valueOf(-1), Long.valueOf(-1L), Float.valueOf(-1),
        Double.valueOf(-1), BigInteger.valueOf(-1), BigDecimal.valueOf(-1), "-1",
    };

    private  final Object[] negativeValuesLimitCheck = {-Float.MIN_VALUE, -Float.MAX_VALUE, -Double.MIN_VALUE};

    private final Object[] invalidValues  = { "", "12ab", "abcd", "-12ab", (Float.MAX_VALUE + Float.MAX_VALUE)/*+Infinity*/, -(Float.MAX_VALUE + Float.MAX_VALUE)/*-Infinity*/
    , Double.MAX_VALUE, -Double.MAX_VALUE};


    @Test
    public void testFloatTypeDefaultValue() {
        Float defValue = floatType.createDefaultValue();

        assertEquals(defValue, Float.valueOf(0));
    }

    @Test
    public void testFloatTypeIsValidValue() {
        for (Object value : validValues) {
            assertTrue(floatType.isValidValue(value), "value=" + value);
        }

        for (Object value : validValuesLimitCheck) {
            assertTrue(floatType.isValidValue(value), "value=" + value);
        }

        for (Object value : negativeValues) {
            assertTrue(floatType.isValidValue(value), "value=" + value);
        }

        for (Object value : negativeValuesLimitCheck) {
            assertTrue(floatType.isValidValue(value), "value=" + value);
        }

        for (Object value : invalidValues) {
            assertFalse(floatType.isValidValue(value), "value=" + value);
        }
    }

    @Test
    public void testFloatTypeGetNormalizedValue() {
        assertNull(floatType.getNormalizedValue(null), "value=" + null);

        for (Object value : validValues) {
            if (value == null) {
                continue;
            }

            Float normalizedValue = floatType.getNormalizedValue(value);

            assertNotNull(normalizedValue, "value=" + value);
            assertEquals(normalizedValue, Float.valueOf(1), "value=" + value);
        }

        for (Object value : validValuesLimitCheck) {
            if (value == null) {
                continue;
            }

            Float normalizedValue = floatType.getNormalizedValue(value);

            assertNotNull(normalizedValue, "value=" + value);

            float f;
            if (value instanceof Float) {
                f = ((Float) value).floatValue();
                assertEquals(normalizedValue, Float.valueOf(f), "value=" + value);
            } else if (value instanceof Double) {
                f = ((Double) value).floatValue();
                assertEquals(normalizedValue, Float.valueOf(f), "value=" + value);
            } else {
                assertEquals(normalizedValue, Float.valueOf(value.toString()), "value=" + value);
            }
        }

        for (Object value : negativeValues) {
            Float normalizedValue = floatType.getNormalizedValue(value);

            assertNotNull(normalizedValue, "value=" + value);
            assertEquals(normalizedValue, Float.valueOf(-1), "value=" + value);
        }

        for (Object value : negativeValuesLimitCheck) {
            Float normalizedValue = floatType.getNormalizedValue(value);
            float f;
            if (value instanceof Float) {
                f = ((Float) value).floatValue();
                assertEquals(normalizedValue, Float.valueOf(f), "value=" + value);
            } else if (value instanceof Double) {
                f = ((Double) value).floatValue();
                assertEquals(normalizedValue, Float.valueOf(f), "value=" + value);
            }
        }

        for (Object value : invalidValues) {
            assertNull(floatType.getNormalizedValue(value), "value=" + value);
        }
    }

    @Test
    public void testFloatTypeValidateValue() {
        List<String> messages = new ArrayList<>();
        for (Object value : validValues) {
            assertTrue(floatType.validateValue(value, "testObj", messages));
            assertEquals(messages.size(), 0, "value=" + value);
        }

        for (Object value : validValuesLimitCheck) {
            assertTrue(floatType.validateValue(value, "testObj", messages));
            assertEquals(messages.size(), 0, "value=" + value);
        }

        for (Object value : negativeValues) {
            assertTrue(floatType.validateValue(value, "testObj", messages));
            assertEquals(messages.size(), 0, "value=" + value);
        }

        for (Object value : negativeValuesLimitCheck) {
            assertTrue(floatType.validateValue(value, "testObj", messages));
            assertEquals(messages.size(), 0, "value=" + value);
        }

        for (Object value : invalidValues) {
            assertFalse(floatType.validateValue(value, "testObj", messages));
            assertEquals(messages.size(), 1, "value=" + value);
            messages.clear();
        }
    }
}
