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

import org.apache.atlas.model.instance.AtlasObjectId;
import org.apache.atlas.type.AtlasBuiltInTypes.AtlasObjectIdType;
import org.testng.annotations.Test;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.testng.Assert.*;


public class TestAtlasObjectIdType {
    private final AtlasObjectIdType objectIdType = new AtlasObjectIdType();
    private final Object[] validValues;
    private final Object[] invalidValues;

    {
        Map<String, String> validObj1 = new HashMap<>();
        Map<Object, Object> validObj2 = new HashMap<>();
        Map<Object, Object> validObj3 = new HashMap<>();

        Map<Object, Object> invalidObj1 = new HashMap<>();
        Map<Object, Object> invalidObj2 = new HashMap<>();
        Map<Object, Object> invalidObj3 = new HashMap<>();
        Map<Object, Object> invalidObj4 = new HashMap<>();

        Map<String, Object> uniqAttribs = new HashMap<String, Object>();
        uniqAttribs.put("name", "testTypeInstance-1");

        // guid
        validObj1.put(AtlasObjectId.KEY_GUID, "guid-1234");

        // typeName & unique-attributes
        validObj2.put(AtlasObjectId.KEY_TYPENAME, "testType");
        validObj2.put(AtlasObjectId.KEY_UNIQUE_ATTRIBUTES, uniqAttribs);

        // guid, typeName & unique-attributes
        validObj3.put(AtlasObjectId.KEY_GUID, "guid-1234");
        validObj3.put(AtlasObjectId.KEY_TYPENAME, "testType");
        validObj3.put(AtlasObjectId.KEY_UNIQUE_ATTRIBUTES, uniqAttribs);

        // no guid or typeName/unique-attributes
        invalidObj1.put(AtlasObjectId.KEY_GUID + "-invalid", "guid-1234"); // no guid or typename or uniqueAttribute

        // no unique-attributes
        invalidObj2.put(AtlasObjectId.KEY_TYPENAME, "testType"); // no guid

        // empty uniqueAttribute
        invalidObj3.put(AtlasObjectId.KEY_TYPENAME, "testType");
        invalidObj3.put(AtlasObjectId.KEY_UNIQUE_ATTRIBUTES, new HashMap<String, Object>());

        // non-map uniqueAttribute
        invalidObj4.put(AtlasObjectId.KEY_TYPENAME, "testType");
        invalidObj4.put(AtlasObjectId.KEY_UNIQUE_ATTRIBUTES, new ArrayList<String>());

        validValues = new Object[] {
            null, validObj1, validObj2, validObj3, new AtlasObjectId(), new AtlasObjectId("guid-1234", "testType"), };

        invalidValues = new Object[] {
            invalidObj1, invalidObj2, invalidObj3, invalidObj4,
            Byte.valueOf((byte)1), Short.valueOf((short)1), Integer.valueOf(1),
            Long.valueOf(1L), Float.valueOf(1), Double.valueOf(1), BigInteger.valueOf(1), BigDecimal.valueOf(1), "1",
            "", "12ab", "abcd", "-12ab",
        };
    }

    @Test
    public void testObjectIdTypeDefaultValue() {
        AtlasObjectId defValue = objectIdType.createDefaultValue();

        assertNotNull(defValue);
    }

    @Test
    public void testObjectIdTypeIsValidValue() {
        for (Object value : validValues) {
            assertTrue(objectIdType.isValidValue(value), "value=" + value);
        }

        for (Object value : invalidValues) {
            assertFalse(objectIdType.isValidValue(value), "value=" + value);
        }
    }

    @Test
    public void testObjectIdTypeGetNormalizedValue() {
        assertNull(objectIdType.getNormalizedValue(null), "value=" + null);

        for (Object value : validValues) {
            if (value == null) {
                continue;
            }

            AtlasObjectId normalizedValue = objectIdType.getNormalizedValue(value);

            assertNotNull(normalizedValue, "value=" + value);

            if (value instanceof AtlasObjectId) {
                assertEquals(normalizedValue, value, "value=" + value);
            } else if (value instanceof Map) {
                assertEquals(normalizedValue.getTypeName(), ((Map)value).get(AtlasObjectId.KEY_TYPENAME),
                             "value=" + value);
                if (((Map)value).get(AtlasObjectId.KEY_GUID) == null) {
                    assertEquals(normalizedValue.getGuid(), ((Map)value).get(AtlasObjectId.KEY_GUID),  "value=" + value);
                } else {
                    assertEquals(normalizedValue.getGuid(), ((Map) value).get(AtlasObjectId.KEY_GUID), "value=" + value);
                }

                assertEquals(normalizedValue.getUniqueAttributes(), ((Map)value).get(AtlasObjectId.KEY_UNIQUE_ATTRIBUTES),
                        "value=" + value);
            }
        }
    }

    @Test
    public void testObjectIdTypeValidateValue() {
        List<String> messages = new ArrayList<>();
        for (Object value : validValues) {
            assertTrue(objectIdType.validateValue(value, "testObj", messages));
            assertEquals(messages.size(), 0, "value=" + value);
        }

        for (Object value : invalidValues) {
            assertFalse(objectIdType.validateValue(value, "testObj", messages));
            assertEquals(messages.size(), 1, "value=" + value);
            messages.clear();
        }
    }
}
