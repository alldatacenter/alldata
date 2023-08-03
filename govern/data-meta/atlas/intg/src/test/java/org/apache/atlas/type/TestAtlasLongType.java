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

import org.apache.atlas.type.AtlasBuiltInTypes.AtlasLongType;
import org.testng.annotations.Test;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import static org.testng.Assert.*;


public class TestAtlasLongType {
    private final AtlasLongType longType = new AtlasLongType();

    private final Object[] validValues = {
        null, Byte.valueOf((byte)1), Short.valueOf((short)1), Integer.valueOf(1), Long.valueOf(1L), Float.valueOf(1),
        Double.valueOf(1), BigInteger.valueOf(1), BigDecimal.valueOf(1), "1",
    };

    private final Object[] validValuesLimitCheck = {Byte.MIN_VALUE, Byte.MAX_VALUE, Short.MIN_VALUE, Short.MAX_VALUE,
            Integer.MIN_VALUE, Integer.MAX_VALUE, Long.MIN_VALUE, Long.MAX_VALUE, Float.MIN_VALUE, Double.MIN_VALUE};

    private final Object[] negativeValues = {
        Byte.valueOf((byte)-1), Short.valueOf((short)-1), Integer.valueOf(-1), Long.valueOf(-1L), Float.valueOf(-1),
        Double.valueOf(-1), BigInteger.valueOf(-1), BigDecimal.valueOf(-1), "-1",
    };

    private  final Object[] negativeValuesLimitCheck = {-Float.MIN_VALUE, -Double.MIN_VALUE};

    BigInteger bgIntLongMaxPlus1 = new BigInteger("9223372036854775808");
    BigInteger bgIntLongMinMinus1 = new BigInteger("-9223372036854775809");
    private final Object[] invalidValues  = { "", "12ab", "abcd", "-12ab"
            , bgIntLongMinMinus1, bgIntLongMaxPlus1, Float.MAX_VALUE, Double.MAX_VALUE, -Float.MAX_VALUE, -Double.MAX_VALUE};


    @Test
    public void testLongTypeDefaultValue() {
        Long defValue = longType.createDefaultValue();

        assertEquals(defValue, Long.valueOf(0));
    }

    @Test
    public void testLongTypeIsValidValue() {
        for (Object value : validValues) {
            assertTrue(longType.isValidValue(value), "value=" + value);
        }

        for (Object value : validValuesLimitCheck) {
            assertTrue(longType.isValidValue(value), "value=" + value);
        }

        for (Object value : negativeValues) {
            assertTrue(longType.isValidValue(value), "value=" + value);
        }

        for (Object value : negativeValuesLimitCheck) {
            assertTrue(longType.isValidValue(value), "value=" + value);
        }

        for (Object value : invalidValues) {
            assertFalse(longType.isValidValue(value), "value=" + value);
        }
    }

    @Test
    public void testLongTypeGetNormalizedValue() {
        assertNull(longType.getNormalizedValue(null), "value=" + null);

        for (Object value : validValues) {
            if (value == null) {
                continue;
            }

            Long normalizedValue = longType.getNormalizedValue(value);

            assertNotNull(normalizedValue, "value=" + value);
            assertEquals(normalizedValue, Long.valueOf(1), "value=" + value);
        }

        for (Object value : validValuesLimitCheck) {
            if (value == null) {
                continue;
            }

            Long normalizedValue = longType.getNormalizedValue(value);

            assertNotNull(normalizedValue, "value=" + value);

            long l;
            if (value instanceof Float) {
                l = ((Float) value).longValue();
                assertEquals(normalizedValue, Long.valueOf(l), "value=" + value);
            } else if (value instanceof Double) {
                l = ((Double) value).longValue();
                assertEquals(normalizedValue, Long.valueOf(l), "value=" + value);
            } else {
                assertEquals(normalizedValue, Long.valueOf(value.toString()), "value=" + value);
            }
        }

        for (Object value : negativeValues) {
            Long normalizedValue = longType.getNormalizedValue(value);

            assertNotNull(normalizedValue, "value=" + value);
            assertEquals(normalizedValue, Long.valueOf(-1), "value=" + value);
        }

        for (Object value : negativeValuesLimitCheck) {
            Long normalizedValue = longType.getNormalizedValue(value);
            long l;
            if (value instanceof Float) {
                l = ((Float) value).longValue();
                assertEquals(normalizedValue, Long.valueOf(l), "value=" + value);
            } else if (value instanceof Double) {
                l = ((Double) value).longValue();
                assertEquals(normalizedValue, Long.valueOf(l), "value=" + value);
            }
        }

        for (Object value : invalidValues) {
            assertNull(longType.getNormalizedValue(value), "value=" + value);
        }
    }

    @Test
    public void testLongTypeValidateValue() {
        List<String> messages = new ArrayList<>();
        for (Object value : validValues) {
            assertTrue(longType.validateValue(value, "testObj", messages));
            assertEquals(messages.size(), 0, "value=" + value);
        }

        for (Object value : validValuesLimitCheck) {
            assertTrue(longType.validateValue(value, "testObj", messages));
            assertEquals(messages.size(), 0, "value=" + value);
        }

        for (Object value : negativeValues) {
            assertTrue(longType.validateValue(value, "testObj", messages));
            assertEquals(messages.size(), 0, "value=" + value);
        }

        for (Object value : negativeValuesLimitCheck) {
            assertTrue(longType.validateValue(value, "testObj", messages));
            assertEquals(messages.size(), 0, "value=" + value);
        }

        for (Object value : invalidValues) {
            assertFalse(longType.validateValue(value, "testObj", messages));
            assertEquals(messages.size(), 1, "value=" + value);
            messages.clear();
        }
    }
}
