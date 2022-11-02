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

import org.apache.atlas.type.AtlasBuiltInTypes.AtlasIntType;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.testng.Assert.*;


public class TestAtlasMapType {
    private final AtlasMapType intIntMapType = new AtlasMapType(new AtlasIntType(), new AtlasIntType());
    private final Object[]     validValues;
    private final Object[]     invalidValues;

    {
        Map<String, Integer>  strIntMap     = new HashMap<>();
        Map<String, Double>   strDoubleMap  = new HashMap<>();
        Map<String, String>   strStringMap  = new HashMap<>();
        Map<Integer, Integer> intIntMap     = new HashMap<>();
        Map<Object, Object>   objObjMap     = new HashMap<>();
        Map<Object, Object>   invObjObjMap1 = new HashMap<>();
        Map<Object, Object>   invObjObjMap2 = new HashMap<>();

        for (int i = 0; i < 10; i++) {
            strIntMap.put(Integer.toString(i), i);
            strDoubleMap.put(Integer.toString(i), Double.valueOf(i));
            strStringMap.put(Integer.toString(i), Integer.toString(i));
            intIntMap.put(i, i);
            objObjMap.put(i, i);
        }

        invObjObjMap1.put("xyz", "123"); // invalid key
        invObjObjMap2.put("123", "xyz"); // invalid value

        validValues = new Object[] {
            null, new HashMap<String, Integer>(), new HashMap<>(), strIntMap, strDoubleMap, strStringMap,
            intIntMap, objObjMap,
        };

        invalidValues = new Object[] { invObjObjMap1, invObjObjMap2, };
    }


    @Test
    public void testMapTypeDefaultValue() {
        Map<Object, Object> defValue = intIntMapType.createDefaultValue();

        assertEquals(defValue.size(), 1);
    }

    @Test
    public void testMapTypeIsValidValue() {
        for (Object value : validValues) {
            assertTrue(intIntMapType.isValidValue(value), "value=" + value);
        }

        for (Object value : invalidValues) {
            assertFalse(intIntMapType.isValidValue(value), "value=" + value);
        }
    }

    @Test
    public void testMapTypeGetNormalizedValue() {
        assertNull(intIntMapType.getNormalizedValue(null), "value=" + null);

        for (Object value : validValues) {
            if (value == null) {
                continue;
            }

            Map<Object, Object> normalizedValue = intIntMapType.getNormalizedValue(value);

            assertNotNull(normalizedValue, "value=" + value);
        }

        for (Object value : invalidValues) {
            assertNull(intIntMapType.getNormalizedValue(value), "value=" + value);
        }
    }

    @Test
    public void testMapTypeValidateValue() {
        List<String> messages = new ArrayList<>();
        for (Object value : validValues) {
            assertTrue(intIntMapType.validateValue(value, "testObj", messages));
            assertEquals(messages.size(), 0, "value=" + value);
        }

        for (Object value : invalidValues) {
            assertFalse(intIntMapType.validateValue(value, "testObj", messages));
            assertTrue(messages.size() > 0, "value=" + value);
            messages.clear();
        }
    }
}
