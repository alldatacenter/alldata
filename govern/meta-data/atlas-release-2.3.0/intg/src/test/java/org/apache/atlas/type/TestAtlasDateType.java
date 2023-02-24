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

import org.apache.atlas.model.typedef.AtlasBaseTypeDef;
import org.apache.atlas.type.AtlasBuiltInTypes.AtlasDateType;
import org.testng.annotations.Test;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Date;
import java.util.ArrayList;
import java.util.List;

import static org.testng.Assert.*;


public class TestAtlasDateType {
    private final AtlasDateType dateType = new AtlasDateType();

    private final Object[] validValues    = {
        null, "", Byte.valueOf((byte)1), Short.valueOf((short)1), Integer.valueOf(1), Long.valueOf(1L), Float.valueOf(1),
        Double.valueOf(1), BigInteger.valueOf(1), BigDecimal.valueOf(1), "1",
    };

    private final Object[] negativeValues = {
        Byte.valueOf((byte)-1), Short.valueOf((short)-1), Integer.valueOf(-1), Long.valueOf(-1L), Float.valueOf(-1),
        Double.valueOf(-1), BigInteger.valueOf(-1), BigDecimal.valueOf(-1), "-1",
    };

    private final Object[] invalidValues  = { "12ab", "abcd", "-12ab", };

    private final Date   now    = new Date();
    private final String strNow = AtlasBaseTypeDef.getDateFormatter().format(now);

    @Test
    public void testDateTypeDefaultValue() {
        Date defValue = dateType.createDefaultValue();

        assertEquals(defValue, new Date(0));
    }

    @Test
    public void testDateTypeIsValidValue() {
        for (Object value : validValues) {
            assertTrue(dateType.isValidValue(value), "value=" + value);
        }

        assertTrue(dateType.isValidValue(now), "value=" + now);
        assertTrue(dateType.isValidValue(strNow), "value=" + strNow);

        for (Object value : negativeValues) {
            assertTrue(dateType.isValidValue(value), "value=" + value);
        }

        for (Object value : invalidValues) {
            assertFalse(dateType.isValidValue(value), "value=" + value);
        }
    }

    @Test
    public void testDateTypeGetNormalizedValue() {
        assertNull(dateType.getNormalizedValue(null), "value=" + null);

        for (Object value : validValues) {
            if (value == null || value == "") {
                continue;
            }

            Date normalizedValue = dateType.getNormalizedValue(value);

            assertNotNull(normalizedValue, "value=" + value);
            assertEquals(normalizedValue, new Date(1), "value=" + value);
        }

        assertNotNull(dateType.getNormalizedValue(now), "value=" + now);
        assertEquals(dateType.getNormalizedValue(now), now, "value=" + now);
        assertNotNull(dateType.getNormalizedValue(strNow), "value=" + strNow);
        assertEquals(dateType.getNormalizedValue(now), now, "value=" + now);

        for (Object value : negativeValues) {
            Date normalizedValue = dateType.getNormalizedValue(value);

            assertNotNull(normalizedValue, "value=" + value);
            assertEquals(normalizedValue, new Date(-1), "value=" + value);
        }

        for (Object value : invalidValues) {
            assertNull(dateType.getNormalizedValue(value), "value=" + value);
        }
    }

    @Test
    public void testDateTypeValidateValue() {
        List<String> messages = new ArrayList<>();
        for (Object value : validValues) {
            assertTrue(dateType.validateValue(value, "testObj", messages));
            assertEquals(messages.size(), 0, "value=" + value);
        }

        assertTrue(dateType.validateValue(now, "testObj", messages));
        assertEquals(messages.size(), 0, "value=" + now);
        assertTrue(dateType.validateValue(strNow, "testObj", messages));
        assertEquals(messages.size(), 0, "value=" + strNow);

        for (Object value : negativeValues) {
            assertTrue(dateType.validateValue(value, "testObj", messages));
            assertEquals(messages.size(), 0, "value=" + value);
        }

        for (Object value : invalidValues) {
            assertFalse(dateType.validateValue(value, "testObj", messages));
            assertEquals(messages.size(), 1, "value=" + value);
            messages.clear();
        }
    }
}
