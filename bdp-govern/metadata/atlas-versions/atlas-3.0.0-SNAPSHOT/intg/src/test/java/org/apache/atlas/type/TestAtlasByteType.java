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

import org.apache.atlas.type.AtlasBuiltInTypes.AtlasByteType;
import org.testng.annotations.Test;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import static org.testng.Assert.*;


public class TestAtlasByteType {
    private final AtlasByteType byteType = new AtlasByteType();

    private final Object[] validValues = {
        null, Byte.valueOf((byte)1), Short.valueOf((short)1), Integer.valueOf(1), Long.valueOf(1L), Float.valueOf(1),
        Double.valueOf(1), BigInteger.valueOf(1), BigDecimal.valueOf(1), "1",
    };

    private final Object[] validValuesLimitCheck = {Byte.MIN_VALUE, Byte.MAX_VALUE, Float.MIN_VALUE, Double.MIN_VALUE};

    private final Object[] negativeValues = {
        Byte.valueOf((byte)-1), Short.valueOf((short)-1), Integer.valueOf(-1), Long.valueOf(-1L), Float.valueOf(-1),
        Double.valueOf(-1), BigInteger.valueOf(-1), BigDecimal.valueOf(-1), "-1",
    };

    private  final Object[] negativeValuesLimitCheck = {-Float.MIN_VALUE, -Double.MIN_VALUE};

    private final Object[] invalidValues = {"", Byte.MIN_VALUE - 1, Byte.MAX_VALUE + 1, Short.MIN_VALUE, Short.MAX_VALUE, Integer.MIN_VALUE, Integer.MAX_VALUE,
    Long.MIN_VALUE, Long.MAX_VALUE, Float.MAX_VALUE, Double.MAX_VALUE, -Float.MAX_VALUE, -Double.MAX_VALUE};


    @Test
    public void testByteTypeDefaultValue() {
        Byte defValue = byteType.createDefaultValue();

        assertEquals(defValue, Byte.valueOf((byte)0));
    }

    @Test
    public void testByteTypeIsValidValue() {
        for (Object value : validValues) {
            assertTrue(byteType.isValidValue(value), "value=" + value);
        }

        for (Object value : validValuesLimitCheck) {
            assertTrue(byteType.isValidValue(value), "value=" + value);
        }

        for (Object value : negativeValues) {
            assertTrue(byteType.isValidValue(value), "value=" + value);
        }

        for (Object value : negativeValuesLimitCheck) {
            assertTrue(byteType.isValidValue(value), "value=" + value);
        }

        for (Object value : invalidValues) {
            assertFalse(byteType.isValidValue(value), "value=" + value);
        }
    }

    @Test
    public void testByteTypeGetNormalizedValue() {
        assertNull(byteType.getNormalizedValue(null), "value=" + null);

        for (Object value : validValues) {
            if (value == null) {
                continue;
            }

            Byte normalizedValue = byteType.getNormalizedValue(value);

            assertNotNull(normalizedValue, "value=" + value);
            assertEquals(normalizedValue, Byte.valueOf((byte)1), "value=" + value);
        }

        for (Object value : validValuesLimitCheck) {
            if (value == null) {
                continue;
            }

            Byte normalizedValue = byteType.getNormalizedValue(value);

            assertNotNull(normalizedValue, "value=" + value);

            byte b;
            if (value instanceof Float) {
                b = ((Float) value).byteValue();
                assertEquals(normalizedValue, Byte.valueOf(b), "value=" + value);
            } else if (value instanceof Double) {
                b = ((Double) value).byteValue();
                assertEquals(normalizedValue, Byte.valueOf(b), "value=" + value);
            } else {
                assertEquals(normalizedValue, Byte.valueOf((byte) value), "value=" + value);
            }
        }

        for (Object value : negativeValues) {
            Byte normalizedValue = byteType.getNormalizedValue(value);

            assertNotNull(normalizedValue, "value=" + value);
            assertEquals(normalizedValue, Byte.valueOf((byte) -1), "value=" + value);
        }

        for (Object value : negativeValuesLimitCheck) {
            Byte normalizedValue = byteType.getNormalizedValue(value);
            byte b;
            if (value instanceof Float) {
                b = ((Float) value).byteValue();
                assertEquals(normalizedValue, Byte.valueOf(b), "value=" + value);
            } else if (value instanceof Double) {
                b = ((Double) value).byteValue();
                assertEquals(normalizedValue, Byte.valueOf(b), "value=" + value);
            }
        }

        for (Object value : invalidValues) {
            assertNull(byteType.getNormalizedValue(value), "value=" + value);
        }
    }

    @Test
    public void testByteTypeValidateValue() {
        List<String> messages = new ArrayList<>();
        for (Object value : validValues) {
            assertTrue(byteType.validateValue(value, "testObj", messages));
            assertEquals(messages.size(), 0, "value=" + value);
        }

        for (Object value : validValuesLimitCheck) {
            assertTrue(byteType.validateValue(value, "testObj", messages));
            assertEquals(messages.size(), 0, "value=" + value);
        }

        for (Object value : negativeValues) {
            assertTrue(byteType.validateValue(value, "testObj", messages));
            assertEquals(messages.size(), 0, "value=" + value);
        }

        for (Object value : negativeValuesLimitCheck) {
            assertTrue(byteType.validateValue(value, "testObj", messages));
            assertEquals(messages.size(), 0, "value=" + value);
        }

        for (Object value : invalidValues) {
            assertFalse(byteType.validateValue(value, "testObj", messages));
            assertEquals(messages.size(), 1, "value=" + value);
            messages.clear();
        }
    }
}
