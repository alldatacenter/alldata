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

import org.apache.atlas.type.AtlasBuiltInTypes.AtlasShortType;
import org.testng.annotations.Test;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import static org.testng.Assert.*;


public class TestAtlasShortType {
    private final AtlasShortType shortType = new AtlasShortType();

    private final Object[] validValues = {
        null, Byte.valueOf((byte)1), Short.valueOf((short)1), Integer.valueOf(1), Long.valueOf(1L), Float.valueOf(1),
        Double.valueOf(1), BigInteger.valueOf(1), BigDecimal.valueOf(1), "1",
    };

    private final Object[] validValuesLimitCheck = {Byte.MIN_VALUE, Byte.MAX_VALUE, Short.MIN_VALUE, Short.MAX_VALUE, Float.MIN_VALUE, Double.MIN_VALUE};

    private final Object[] negativeValues = {
        Byte.valueOf((byte)-1), Short.valueOf((short)-1), Integer.valueOf(-1), Long.valueOf(-1L), Float.valueOf(-1),
        Double.valueOf(-1), BigInteger.valueOf(-1), BigDecimal.valueOf(-1), "-1",
    };

    private  final Object[] negativeValuesLimitCheck = {-Float.MIN_VALUE, -Double.MIN_VALUE};

    private final Object[] invalidValues = {"", "12ab", "abcd", "-12ab", Short.MIN_VALUE - 1, Short.MAX_VALUE + 1, Integer.MIN_VALUE, Integer.MAX_VALUE,
            Long.MIN_VALUE, Long.MAX_VALUE, Float.MAX_VALUE, Double.MAX_VALUE, -Float.MAX_VALUE, -Double.MAX_VALUE
    };


    @Test
    public void testShortTypeDefaultValue() {
        Short defValue = shortType.createDefaultValue();

        assertEquals(defValue, Short.valueOf((short)0));
    }

    @Test
    public void testShortTypeIsValidValue() {
        for (Object value : validValues) {
            assertTrue(shortType.isValidValue(value), "value=" + value);
        }

        for (Object value : validValuesLimitCheck) {
            assertTrue(shortType.isValidValue(value), "value=" + value);
        }

        for (Object value : negativeValues) {
            assertTrue(shortType.isValidValue(value), "value=" + value);
        }

        for (Object value : negativeValuesLimitCheck) {
            assertTrue(shortType.isValidValue(value), "value=" + value);
        }

        for (Object value : invalidValues) {
            assertFalse(shortType.isValidValue(value), "value=" + value);
        }
    }

    @Test
    public void testShortTypeGetNormalizedValue() {
        assertNull(shortType.getNormalizedValue(null), "value=" + null);

        for (Object value : validValues) {
            if (value == null) {
                continue;
            }

            Short normalizedValue = shortType.getNormalizedValue(value);

            assertNotNull(normalizedValue, "value=" + value);
            assertEquals(normalizedValue, Short.valueOf((short)1), "value=" + value);
        }

        for (Object value : validValuesLimitCheck) {
            if (value == null) {
                continue;
            }

            Short normalizedValue = shortType.getNormalizedValue(value);

            assertNotNull(normalizedValue, "value=" + value);

            short s;
            if (value instanceof Float) {
                s = ((Float) value).shortValue();
                assertEquals(normalizedValue, Short.valueOf(s), "value=" + value);
            } else if (value instanceof Double) {
                s = ((Double) value).shortValue();
                assertEquals(normalizedValue, Short.valueOf(s), "value=" + value);
            } else {
                assertEquals(normalizedValue, Short.valueOf(value.toString()), "value=" + value);
            }
        }

        for (Object value : negativeValues) {
            Short normalizedValue = shortType.getNormalizedValue(value);

            assertNotNull(normalizedValue, "value=" + value);
            assertEquals(normalizedValue, Short.valueOf((short)-1), "value=" + value);
        }

        for (Object value : negativeValuesLimitCheck) {
            Short normalizedValue = shortType.getNormalizedValue(value);
            short s;
            if (value instanceof Float) {
                s = ((Float) value).shortValue();
                assertEquals(normalizedValue, Short.valueOf(s), "value=" + value);
            } else if (value instanceof Double) {
                s = ((Double) value).shortValue();
                assertEquals(normalizedValue, Short.valueOf(s), "value=" + value);
            }
        }

        for (Object value : invalidValues) {
            assertNull(shortType.getNormalizedValue(value), "value=" + value);
        }
    }

    @Test
    public void testShortTypeValidateValue() {
        List<String> messages = new ArrayList<>();
        for (Object value : validValues) {
            assertTrue(shortType.validateValue(value, "testObj", messages));
            assertEquals(messages.size(), 0, "value=" + value);
        }

        for (Object value : validValuesLimitCheck) {
            assertTrue(shortType.validateValue(value, "testObj", messages));
            assertEquals(messages.size(), 0, "value=" + value);
        }

        for (Object value : negativeValues) {
            assertTrue(shortType.validateValue(value, "testObj", messages));
            assertEquals(messages.size(), 0, "value=" + value);
        }

        for (Object value : negativeValuesLimitCheck) {
            assertTrue(shortType.validateValue(value, "testObj", messages));
            assertEquals(messages.size(), 0, "value=" + value);
        }

        for (Object value : invalidValues) {
            assertFalse(shortType.validateValue(value, "testObj", messages));
            assertEquals(messages.size(), 1, "value=" + value);
            messages.clear();
        }
    }
}
