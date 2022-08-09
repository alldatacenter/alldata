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

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;

import static org.testng.Assert.*;


public class TestAtlasArrayType {
    private final AtlasArrayType intArrayType  = new AtlasArrayType(new AtlasIntType());
    private final Object[]       validValues;
    private final Object[]       invalidValues;

    {
        List<Integer> intList  = new ArrayList<>();
        Set<Integer>  intSet   = new HashSet<>();
        Integer[]     intArray = new Integer[] { 1, 2, 3 };
        List<Object>  objList  = new ArrayList<>();
        Set<Object>   objSet   = new HashSet<>();
        Object[]      objArray = new Object[] { 1, 2, 3 };
        List<String>  strList  = new ArrayList<>();
        Set<String>   strSet   = new HashSet<>();
        String[]      strArray = new String[] { "1", "2", "3" };

        for (int i = 0; i < 10; i++) {
            intList.add(i);
            intSet.add(i);
            objList.add(i);
            objSet.add(i);
            strList.add(Integer.toString(i));
            strSet.add(Integer.toString(i));
        }

        validValues = new Object[] {
            null, new Integer[] { }, intList, intSet, intArray, objList, objSet, objArray, strList, strSet, strArray,
            new byte[] { 1 }, new short[] { 1 }, new int[] { 1 }, new long[] { 1 }, new float[] { 1 },
            new double[] { 1 }, new BigInteger[] { BigInteger.valueOf(1) }, new BigDecimal[] { BigDecimal.valueOf(1)},
        };

        invalidValues = new Object[] {
            Byte.valueOf((byte)1), Short.valueOf((short)1),
            Integer.valueOf(1), Long.valueOf(1L), Float.valueOf(1), Double.valueOf(1), BigInteger.valueOf(1),
            BigDecimal.valueOf(1),
        };
    }


    @Test
    public void testArrayTypeDefaultValue() {
        Collection defValue = intArrayType.createDefaultValue();

        assertEquals(defValue.size(), 1);
    }

    @Test
    public void testArrayTypeIsValidValue() {
        for (Object value : validValues) {
            assertTrue(intArrayType.isValidValue(value), "value=" + value);
        }

        for (Object value : invalidValues) {
            assertFalse(intArrayType.isValidValue(value), "value=" + value);
        }
    }

    @Test
    public void testArrayTypeGetNormalizedValue() {
        assertNull(intArrayType.getNormalizedValue(null), "value=" + null);

        for (Object value : validValues) {
            if (value == null) {
                continue;
            }

            Collection normalizedValue = intArrayType.getNormalizedValue(value);

            assertNotNull(normalizedValue, "value=" + value);
        }

        for (Object value : invalidValues) {
            assertNull(intArrayType.getNormalizedValue(value), "value=" + value);
        }
    }

    @Test
    public void testArrayTypeValidateValue() {
        List<String> messages = new ArrayList<>();
        for (Object value : validValues) {
            assertTrue(intArrayType.validateValue(value, "testObj", messages));
            assertEquals(messages.size(), 0, "value=" + value);
        }

        for (Object value : invalidValues) {
            assertFalse(intArrayType.validateValue(value, "testObj", messages));
            assertTrue(messages.size() > 0, "value=" + value);
            messages.clear();
        }
    }
}
