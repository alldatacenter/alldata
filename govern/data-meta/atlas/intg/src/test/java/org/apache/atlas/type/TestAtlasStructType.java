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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import org.apache.atlas.AtlasErrorCode;
import org.apache.atlas.exception.AtlasBaseException;
import org.apache.atlas.model.ModelTestUtil;
import org.apache.atlas.model.instance.AtlasStruct;
import static org.apache.atlas.model.typedef.AtlasBaseTypeDef.ATLAS_TYPE_INT;
import static org.apache.atlas.model.typedef.AtlasBaseTypeDef.ATLAS_TYPE_DATE;
import static org.apache.atlas.model.typedef.AtlasBaseTypeDef.ATLAS_TYPE_BIGINTEGER;

import org.apache.atlas.model.typedef.AtlasBaseTypeDef;
import org.apache.atlas.model.typedef.AtlasStructDef;
import org.apache.atlas.model.typedef.AtlasStructDef.AtlasAttributeDef;
import org.apache.atlas.model.typedef.AtlasStructDef.AtlasAttributeDef.Cardinality;
import org.testng.annotations.Test;

import static org.testng.Assert.*;


public class TestAtlasStructType {
    private static final String MULTI_VAL_ATTR_NAME_MIN_MAX = "multiValMinMax";
    private static final String MULTI_VAL_ATTR_NAME_MIN     = "multiValMin";
    private static final String MULTI_VAL_ATTR_NAME_MAX     = "multiValMax";
    private static final int    MULTI_VAL_ATTR_MIN_COUNT    = 2;
    private static final int    MULTI_VAL_ATTR_MAX_COUNT    = 5;

    private final AtlasStructType structType;
    private final List<Object>    validValues;
    private final List<Object>    invalidValues;

    {
        AtlasAttributeDef multiValuedAttribMinMax = new AtlasAttributeDef();
        AtlasAttributeDef multiValuedAttribMin    = new AtlasAttributeDef();
        AtlasAttributeDef multiValuedAttribMax    = new AtlasAttributeDef();

        multiValuedAttribMinMax.setName(MULTI_VAL_ATTR_NAME_MIN_MAX);
        multiValuedAttribMinMax.setTypeName(AtlasBaseTypeDef.getArrayTypeName(ATLAS_TYPE_INT));
        multiValuedAttribMinMax.setCardinality(Cardinality.LIST);
        multiValuedAttribMinMax.setValuesMinCount(MULTI_VAL_ATTR_MIN_COUNT);
        multiValuedAttribMinMax.setValuesMaxCount(MULTI_VAL_ATTR_MAX_COUNT);

        multiValuedAttribMin.setName(MULTI_VAL_ATTR_NAME_MIN);
        multiValuedAttribMin.setTypeName(AtlasBaseTypeDef.getArrayTypeName(ATLAS_TYPE_INT));
        multiValuedAttribMin.setCardinality(Cardinality.LIST);
        multiValuedAttribMin.setValuesMinCount(MULTI_VAL_ATTR_MIN_COUNT);

        multiValuedAttribMax.setName(MULTI_VAL_ATTR_NAME_MAX);
        multiValuedAttribMax.setTypeName(AtlasBaseTypeDef.getArrayTypeName(ATLAS_TYPE_INT));
        multiValuedAttribMax.setCardinality(Cardinality.SET);
        multiValuedAttribMax.setValuesMaxCount(MULTI_VAL_ATTR_MAX_COUNT);

        AtlasStructDef structDef = ModelTestUtil.newStructDef();

        structDef.addAttribute(multiValuedAttribMinMax);
        structDef.addAttribute(multiValuedAttribMin);
        structDef.addAttribute(multiValuedAttribMax);

        structType    = getStructType(structDef);
        validValues   = new ArrayList<>();
        invalidValues = new ArrayList<>();

        AtlasStruct invalidValue1 = structType.createDefaultValue();
        AtlasStruct invalidValue2 = structType.createDefaultValue();
        AtlasStruct invalidValue3 = structType.createDefaultValue();
        AtlasStruct invalidValue4 = structType.createDefaultValue();
        AtlasStruct invalidValue5 = structType.createDefaultValue();
        AtlasStruct invalidValue6 = structType.createDefaultValue();
        AtlasStruct invalidValue7 = structType.createDefaultValue();

        // invalid value for int
        invalidValue1.setAttribute(ModelTestUtil.getDefaultAttributeName(ATLAS_TYPE_INT), "xyz");

        // invalid value for date
        invalidValue2.setAttribute(ModelTestUtil.getDefaultAttributeName(ATLAS_TYPE_DATE), "xyz");

        // invalid value for bigint
        invalidValue3.setAttribute(ModelTestUtil.getDefaultAttributeName(ATLAS_TYPE_BIGINTEGER), "xyz");

        // minCount is less than required
        invalidValue4.setAttribute(MULTI_VAL_ATTR_NAME_MIN_MAX, new Integer[] { 1 });

        // maxCount is more than allowed
        invalidValue5.setAttribute(MULTI_VAL_ATTR_NAME_MIN_MAX, new Integer[] { 1, 2, 3, 4, 5, 6 });

        // minCount is less than required
        invalidValue6.setAttribute(MULTI_VAL_ATTR_NAME_MIN, new Integer[] { });

        // maxCount is more than allowed
        invalidValue7.setAttribute(MULTI_VAL_ATTR_NAME_MAX, new Integer[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 });

        validValues.add(null);
        validValues.add(structType.createDefaultValue());
        validValues.add(structType.createDefaultValue().getAttributes()); // Map<String, Object>
        invalidValues.add(invalidValue1);
        invalidValues.add(invalidValue2);
        invalidValues.add(invalidValue3);
        invalidValues.add(invalidValue4);
        invalidValues.add(invalidValue5);
        invalidValues.add(invalidValue6);
        invalidValues.add(invalidValue7);
        invalidValues.add(new AtlasStruct());             // no values for mandatory attributes
        invalidValues.add(new HashMap<>()); // no values for mandatory attributes
        invalidValues.add(1);               // incorrect datatype
        invalidValues.add(new HashSet());   // incorrect datatype
        invalidValues.add(new ArrayList()); // incorrect datatype
        invalidValues.add(new String[] {}); // incorrect datatype
    }

    @Test
    public void testStructTypeDefaultValue() {
        AtlasStruct defValue = structType.createDefaultValue();

        assertNotNull(defValue);
        assertEquals(defValue.getTypeName(), structType.getTypeName());
    }

    @Test
    public void testStructTypeIsValidValue() {
        for (Object value : validValues) {
            assertTrue(structType.isValidValue(value), "value=" + value);
        }

        for (Object value : invalidValues) {
            assertFalse(structType.isValidValue(value), "value=" + value);
        }
    }

    @Test
    public void testStructTypeGetNormalizedValue() {
        assertNull(structType.getNormalizedValue(null), "value=" + null);

        for (Object value : validValues) {
            if (value == null) {
                continue;
            }

            Object normalizedValue = structType.getNormalizedValue(value);

            assertNotNull(normalizedValue, "value=" + value);
        }

        for (Object value : invalidValues) {
            assertNull(structType.getNormalizedValue(value), "value=" + value);
        }
    }

    @Test
    public void testStructTypeValidateValue() {
        List<String> messages = new ArrayList<>();
        for (Object value : validValues) {
            assertTrue(structType.validateValue(value, "testObj", messages));
            assertEquals(messages.size(), 0, "value=" + value);
        }

        for (Object value : invalidValues) {
            assertFalse(structType.validateValue(value, "testObj", messages));
            assertTrue(messages.size() > 0, "value=" + value);
            messages.clear();
        }
    }

    @Test
    public void testInvalidStructDef_MultiValuedAttributeNotArray() {
        AtlasAttributeDef invalidMultiValuedAttrib = new AtlasAttributeDef("invalidAttributeDef", ATLAS_TYPE_INT);
        invalidMultiValuedAttrib.setCardinality(Cardinality.LIST);

        AtlasStructDef invalidStructDef = ModelTestUtil.newStructDef();
        invalidStructDef.addAttribute(invalidMultiValuedAttrib);

        try {
            AtlasStructType invalidStructType = new AtlasStructType(invalidStructDef, ModelTestUtil.getTypesRegistry());

            fail("invalidStructDef not detected: structDef=" + invalidStructDef + "; structType=" + invalidStructType);
        } catch (AtlasBaseException excp) {
            assertTrue(excp.getAtlasErrorCode() == AtlasErrorCode.INVALID_ATTRIBUTE_TYPE_FOR_CARDINALITY);
            invalidStructDef.removeAttribute("invalidAttributeDef");
        }
    }

    private static AtlasStructType getStructType(AtlasStructDef structDef) {
        try {
            return new AtlasStructType(structDef, ModelTestUtil.getTypesRegistry());
        } catch (AtlasBaseException excp) {
            return null;
        }
    }
}
