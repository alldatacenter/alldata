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
import java.util.Map;

import org.apache.atlas.AtlasErrorCode;
import org.apache.atlas.exception.AtlasBaseException;
import org.apache.atlas.model.ModelTestUtil;
import org.apache.atlas.model.instance.AtlasEntity;
import org.apache.atlas.model.typedef.AtlasBaseTypeDef;
import org.apache.atlas.model.typedef.AtlasEntityDef;
import org.apache.atlas.model.typedef.AtlasStructDef.AtlasAttributeDef;
import org.apache.atlas.model.typedef.AtlasStructDef.AtlasConstraintDef;
import org.apache.atlas.type.AtlasTypeRegistry.AtlasTransientTypeRegistry;
import org.testng.annotations.Test;

import static org.testng.Assert.*;


public class TestAtlasEntityType {
    private static final String TYPE_TABLE       = "my_table";
    private static final String TYPE_COLUMN      = "my_column";
    private static final String ATTR_TABLE       = "table";
    private static final String ATTR_COLUMNS     = "columns";
    private static final String ATTR_OWNER       = "owner";
    private static final String ATTR_NAME        = "name";
    private static final String ATTR_DESCRIPTION = "description";
    private static final String ATTR_LOCATION    = "location";

    private final AtlasEntityType entityType;
    private final List<Object>    validValues   = new ArrayList<>();
    private final List<Object>    invalidValues = new ArrayList<>();

    {
        entityType  = getEntityType(ModelTestUtil.getEntityDefWithSuperTypes());

        AtlasEntity         invalidValue1 = entityType.createDefaultValue();
        AtlasEntity         invalidValue2 = entityType.createDefaultValue();
        Map<String, Object> invalidValue3 = entityType.createDefaultValue().getAttributes();

        // invalid value for int
        invalidValue1.setAttribute(ModelTestUtil.getDefaultAttributeName(AtlasBaseTypeDef.ATLAS_TYPE_INT), "xyz");
        // invalid value for date
        invalidValue2.setAttribute(ModelTestUtil.getDefaultAttributeName(AtlasBaseTypeDef.ATLAS_TYPE_DATE), "xyz");
        // invalid value for bigint
        invalidValue3.put(ModelTestUtil.getDefaultAttributeName(AtlasBaseTypeDef.ATLAS_TYPE_BIGINTEGER), "xyz");

        validValues.add(null);
        validValues.add(entityType.createDefaultValue());
        validValues.add(entityType.createDefaultValue().getAttributes()); // Map<String, Object>
        invalidValues.add(invalidValue1);
        invalidValues.add(invalidValue2);
        invalidValues.add(invalidValue3);
        invalidValues.add(new AtlasEntity());             // no values for mandatory attributes
        invalidValues.add(new HashMap<>()); // no values for mandatory attributes
        invalidValues.add(1);               // incorrect datatype
        invalidValues.add(new HashSet());   // incorrect datatype
        invalidValues.add(new ArrayList()); // incorrect datatype
        invalidValues.add(new String[] {}); // incorrect datatype
    }

    @Test
    public void testEntityTypeDefaultValue() {
        AtlasEntity defValue = entityType.createDefaultValue();

        assertNotNull(defValue);
        assertEquals(defValue.getTypeName(), entityType.getTypeName());
    }

    @Test
    public void testEntityTypeIsValidValue() {
        for (Object value : validValues) {
            assertTrue(entityType.isValidValue(value), "value=" + value);
        }

        for (Object value : invalidValues) {
            assertFalse(entityType.isValidValue(value), "value=" + value);
        }
    }

    @Test
    public void testEntityTypeGetNormalizedValue() {
        assertNull(entityType.getNormalizedValue(null), "value=" + null);

        for (Object value : validValues) {
            if (value == null) {
                continue;
            }

            Object normalizedValue = entityType.getNormalizedValue(value);

            assertNotNull(normalizedValue, "value=" + value);
        }

        for (Object value : invalidValues) {
            assertNull(entityType.getNormalizedValue(value), "value=" + value);
        }
    }

    @Test
    public void testEntityTypeValidateValue() {
        List<String> messages = new ArrayList<>();
        for (Object value : validValues) {
            assertTrue(entityType.validateValue(value, "testObj", messages));
            assertEquals(messages.size(), 0, "value=" + value);
        }

        for (Object value : invalidValues) {
            assertFalse(entityType.validateValue(value, "testObj", messages));
            assertTrue(messages.size() > 0, "value=" + value);
            messages.clear();
        }
    }

    @Test
    public void testValidConstraints() {
        AtlasTypeRegistry          typeRegistry = new AtlasTypeRegistry();
        AtlasTransientTypeRegistry ttr          = null;
        boolean                    commit       = false;
        List<AtlasEntityDef>       entityDefs   = new ArrayList<>();
        String                     failureMsg   = null;

        entityDefs.add(createTableEntityDef());
        entityDefs.add(createColumnEntityDef());

        try {
            ttr = typeRegistry.lockTypeRegistryForUpdate();

            ttr.addTypes(entityDefs);

            AtlasEntityType typeTable  = ttr.getEntityTypeByName(TYPE_TABLE);
            AtlasEntityType typeColumn = ttr.getEntityTypeByName(TYPE_COLUMN);

            assertTrue(typeTable.getAttribute(ATTR_COLUMNS).isOwnedRef());
            assertNull(typeTable.getAttribute(ATTR_COLUMNS).getInverseRefAttributeName());
            assertFalse(typeColumn.getAttribute(ATTR_TABLE).isOwnedRef());
            assertEquals(typeColumn.getAttribute(ATTR_TABLE).getInverseRefAttributeName(), ATTR_COLUMNS);
            assertEquals(typeColumn.getAttribute(ATTR_TABLE).getInverseRefAttribute(), typeTable.getAttribute(ATTR_COLUMNS));

            commit = true;
        } catch (AtlasBaseException excp) {
            failureMsg = excp.getMessage();
        } finally {
            typeRegistry.releaseTypeRegistryForUpdate(ttr, commit);
        }
        assertNull(failureMsg, "failed to create types " + TYPE_TABLE + " and " + TYPE_COLUMN);
    }

    @Test
    public void testDynAttributeFlags() {
        AtlasTypeRegistry          typeRegistry = new AtlasTypeRegistry();
        AtlasTransientTypeRegistry ttr          = null;
        boolean                    commit       = false;
        List<AtlasEntityDef>       entityDefs   = new ArrayList<>();
        String                     failureMsg   = null;

        entityDefs.add(createTableEntityDefWithOptions());
        entityDefs.add(createColumnEntityDef());

        try {
            ttr = typeRegistry.lockTypeRegistryForUpdate();

            ttr.addTypes(entityDefs);
            //options are read in the table,
            AtlasEntityType typeTable  = ttr.getEntityTypeByName(TYPE_TABLE);
            AtlasEntityType typeColumn = ttr.getEntityTypeByName(TYPE_COLUMN);

            assertTrue(typeTable.getAttribute(ATTR_NAME).getIsDynAttributeEvalTrigger());
            assertFalse(typeTable.getAttribute(ATTR_NAME).getIsDynAttribute());
            assertFalse(typeTable.getAttribute(ATTR_OWNER).getIsDynAttributeEvalTrigger());
            assertTrue(typeTable.getAttribute(ATTR_OWNER).getIsDynAttribute());

            commit = true;
        } catch (AtlasBaseException excp) {
            failureMsg = excp.getMessage();
        } finally {
            typeRegistry.releaseTypeRegistryForUpdate(ttr, commit);
        }
        assertNull(failureMsg, "failed to create types " + TYPE_TABLE + " and " + TYPE_COLUMN);
    }

    @Test
    public void testReorderDynAttributes() {
        AtlasTypeRegistry          typeRegistry = new AtlasTypeRegistry();
        AtlasTransientTypeRegistry ttr          = null;
        boolean                    commit       = false;
        List<AtlasEntityDef>       entityDefs   = new ArrayList<>();
        String                     failureMsg   = null;

        entityDefs.add(createTableEntityDefForTopSort());

        try {
            ttr = typeRegistry.lockTypeRegistryForUpdate();

            ttr.addTypes(entityDefs);
            //options are read in the table,
            AtlasEntityType typeTable  = ttr.getEntityTypeByName(TYPE_TABLE);

            // Expect attributes in this order: ATTR_DESCRIPTION, ATTR_OWNER, ATTR_NAME, ATTR_LOCATION
            assertEquals(typeTable.getDynEvalAttributes().get(0).getName(), ATTR_DESCRIPTION);
            assertEquals(typeTable.getDynEvalAttributes().get(1).getName(), ATTR_OWNER);
            assertEquals(typeTable.getDynEvalAttributes().get(2).getName(), ATTR_NAME);
            assertEquals(typeTable.getDynEvalAttributes().get(3).getName(), ATTR_LOCATION);

            commit = true;
        } catch (AtlasBaseException excp) {
            failureMsg = excp.getMessage();
        } finally {
            typeRegistry.releaseTypeRegistryForUpdate(ttr, commit);
        }
        assertNull(failureMsg, "failed to create types " + TYPE_TABLE + " and " + TYPE_COLUMN);
    }

    @Test
    public void testConstraintInvalidOwnedRef_InvalidAttributeType() {
        AtlasTypeRegistry          typeRegistry = new AtlasTypeRegistry();
        AtlasTransientTypeRegistry ttr          = null;
        boolean                    commit       = false;
        List<AtlasEntityDef>       entityDefs   = new ArrayList<>();
        AtlasErrorCode             errorCode   = null;

        entityDefs.add(createTableEntityDefWithOwnedRefOnInvalidType());

        try {
            ttr = typeRegistry.lockTypeRegistryForUpdate();

            ttr.addTypes(entityDefs);

            commit = true;
        } catch (AtlasBaseException excp) {
            errorCode = excp.getAtlasErrorCode();
        } finally {
            typeRegistry.releaseTypeRegistryForUpdate(ttr, commit);
        }
        assertEquals(errorCode, AtlasErrorCode.CONSTRAINT_OWNED_REF_ATTRIBUTE_INVALID_TYPE,
                "expected invalid constraint failure - missing refAttribute");
    }

    @Test
    public void testConstraintInValidInverseRef_MissingParams() {
        AtlasTypeRegistry          typeRegistry = new AtlasTypeRegistry();
        AtlasTransientTypeRegistry ttr          = null;
        boolean                    commit       = false;
        List<AtlasEntityDef>       entityDefs   = new ArrayList<>();
        AtlasErrorCode             errorCode   = null;

        entityDefs.add(createTableEntityDef());
        entityDefs.add(createColumnEntityDefWithMissingInverseAttribute());

        try {
            ttr = typeRegistry.lockTypeRegistryForUpdate();

            ttr.addTypes(entityDefs);

            commit = true;
        } catch (AtlasBaseException excp) {
            errorCode = excp.getAtlasErrorCode();
        } finally {
            typeRegistry.releaseTypeRegistryForUpdate(ttr, commit);
        }
        assertEquals(errorCode, AtlasErrorCode.CONSTRAINT_MISSING_PARAMS,
                "expected invalid constraint failure - missing refAttribute");
    }

    @Test
    public void testConstraintInValidInverseRef_InvalidAttributeTypeForInverseAttribute() {
        AtlasTypeRegistry          typeRegistry = new AtlasTypeRegistry();
        AtlasTransientTypeRegistry ttr          = null;
        boolean                    commit       = false;
        List<AtlasEntityDef>       entityDefs   = new ArrayList<>();
        AtlasErrorCode             errorCode   = null;

        entityDefs.add(createTableEntityDef());
        entityDefs.add(createColumnEntityDefWithInvaidAttributeTypeForInverseAttribute());

        try {
            ttr = typeRegistry.lockTypeRegistryForUpdate();

            ttr.addTypes(entityDefs);

            commit = true;
        } catch (AtlasBaseException excp) {
            errorCode = excp.getAtlasErrorCode();
        } finally {
            typeRegistry.releaseTypeRegistryForUpdate(ttr, commit);
        }
        assertEquals(errorCode, AtlasErrorCode.CONSTRAINT_INVERSE_REF_ATTRIBUTE_INVALID_TYPE,
                "expected invalid constraint failure - missing refAttribute");
    }

    @Test
    public void testConstraintInValidInverseRef_InvalidAttributeType() {
        AtlasTypeRegistry          typeRegistry = new AtlasTypeRegistry();
        AtlasTransientTypeRegistry ttr          = null;
        boolean                    commit       = false;
        List<AtlasEntityDef>       entityDefs   = new ArrayList<>();
        AtlasErrorCode             errorCode   = null;

        entityDefs.add(createTableEntityDef());
        entityDefs.add(createColumnEntityDefWithInvalidInverseAttributeType());

        try {
            ttr = typeRegistry.lockTypeRegistryForUpdate();

            ttr.addTypes(entityDefs);

            commit = true;
        } catch (AtlasBaseException excp) {
            errorCode = excp.getAtlasErrorCode();
        } finally {
            typeRegistry.releaseTypeRegistryForUpdate(ttr, commit);
        }
        assertEquals(errorCode, AtlasErrorCode.CONSTRAINT_INVERSE_REF_INVERSE_ATTRIBUTE_INVALID_TYPE,
                "expected invalid constraint failure - invalid refAttribute type");
    }

    @Test
    public void testConstraintInValidInverseRef_NonExistingAttribute() {
        AtlasTypeRegistry          typeRegistry = new AtlasTypeRegistry();
        AtlasTransientTypeRegistry ttr          = null;
        boolean                    commit       = false;
        List<AtlasEntityDef>       entityDefs   = new ArrayList<>();
        AtlasErrorCode             errorCode   = null;

        entityDefs.add(createTableEntityDef());
        entityDefs.add(createColumnEntityDefWithNonExistingInverseAttribute());

        try {
            ttr = typeRegistry.lockTypeRegistryForUpdate();

            ttr.addTypes(entityDefs);

            commit = true;
        } catch (AtlasBaseException excp) {
            errorCode = excp.getAtlasErrorCode();
        } finally {
            typeRegistry.releaseTypeRegistryForUpdate(ttr, commit);
        }
        assertEquals(errorCode, AtlasErrorCode.CONSTRAINT_INVERSE_REF_INVERSE_ATTRIBUTE_NON_EXISTING,
                     "expected invalid constraint failure - non-existing refAttribute");
    }

    private static AtlasEntityType getEntityType(AtlasEntityDef entityDef) {
        try {
            return new AtlasEntityType(entityDef, ModelTestUtil.getTypesRegistry());
        } catch (AtlasBaseException excp) {
            return null;
        }
    }

    private AtlasEntityDef createTableEntityDef() {
        AtlasEntityDef    table       = new AtlasEntityDef(TYPE_TABLE);
        AtlasAttributeDef attrName    = new AtlasAttributeDef(ATTR_NAME, AtlasBaseTypeDef.ATLAS_TYPE_STRING);
        AtlasAttributeDef attrColumns = new AtlasAttributeDef(ATTR_COLUMNS,
                                                              AtlasBaseTypeDef.getArrayTypeName(TYPE_COLUMN));

        attrColumns.addConstraint(new AtlasConstraintDef(AtlasConstraintDef.CONSTRAINT_TYPE_OWNED_REF));

        table.addAttribute(attrName);
        table.addAttribute(attrColumns);

        return table;
    }

    private AtlasEntityDef createTableEntityDefWithOptions() {
        AtlasEntityDef    table       = new AtlasEntityDef(TYPE_TABLE);
        AtlasAttributeDef attrName    = new AtlasAttributeDef(ATTR_NAME, AtlasBaseTypeDef.ATLAS_TYPE_STRING);
        AtlasAttributeDef attrColumns = new AtlasAttributeDef(ATTR_COLUMNS, AtlasBaseTypeDef.getArrayTypeName(TYPE_COLUMN));
        AtlasAttributeDef attrOwner   = new AtlasAttributeDef(ATTR_OWNER, AtlasBaseTypeDef.ATLAS_TYPE_STRING);

        attrColumns.addConstraint(new AtlasConstraintDef(AtlasConstraintDef.CONSTRAINT_TYPE_OWNED_REF));

        table.addAttribute(attrName);
        table.addAttribute(attrColumns);
        table.addAttribute(attrOwner);

        Map<String,String> options = new HashMap<>();
        String             key     = "dynAttribute:" + ATTR_OWNER;
        String             value   = "{" + ATTR_NAME + "}";

        options.put(key,value);

        table.setOptions(options);

        return table;
    }

    private AtlasEntityDef createTableEntityDefForTopSort() {
        AtlasEntityDef    table           = new AtlasEntityDef(TYPE_TABLE);
        AtlasAttributeDef attrName        = new AtlasAttributeDef(ATTR_NAME, AtlasBaseTypeDef.ATLAS_TYPE_STRING);
        AtlasAttributeDef attrOwner       = new AtlasAttributeDef(ATTR_OWNER, AtlasBaseTypeDef.ATLAS_TYPE_STRING);
        AtlasAttributeDef attrDescription = new AtlasAttributeDef(ATTR_DESCRIPTION, AtlasBaseTypeDef.ATLAS_TYPE_STRING);
        AtlasAttributeDef attrLocation    = new AtlasAttributeDef(ATTR_LOCATION, AtlasBaseTypeDef.ATLAS_TYPE_STRING);

        table.addAttribute(attrName);
        table.addAttribute(attrOwner);
        table.addAttribute(attrDescription);
        table.addAttribute(attrLocation);

        Map<String,String> options = new HashMap<>();

        String key1   = "dynAttribute:" + ATTR_OWNER;
        String value1 = "{" + ATTR_DESCRIPTION + "}";

        String key2   = "dynAttribute:" + ATTR_DESCRIPTION;
        String value2 = "template";

        String key3   = "dynAttribute:" + ATTR_NAME;
        String value3 = "{" + ATTR_DESCRIPTION + "}@{" + ATTR_OWNER + "}";

        String key4   = "dynAttribute:" + ATTR_LOCATION;
        String value4 = "{" + ATTR_NAME + "}@{" + ATTR_OWNER + "}";

        options.put(key1, value1);
        options.put(key2, value2);
        options.put(key3, value3);
        options.put(key4, value4);

        table.setOptions(options);

        return table;
    }

    private AtlasEntityDef createTableEntityDefWithOwnedRefOnInvalidType() {
        AtlasEntityDef    table    = new AtlasEntityDef(TYPE_TABLE);
        AtlasAttributeDef attrName = new AtlasAttributeDef(ATTR_NAME, AtlasBaseTypeDef.ATLAS_TYPE_STRING);

        attrName.addConstraint(new AtlasConstraintDef(AtlasConstraintDef.CONSTRAINT_TYPE_OWNED_REF));

        table.addAttribute(attrName);

        return table;
    }

    private AtlasEntityDef createColumnEntityDefWithMissingInverseAttribute() {
        AtlasEntityDef    column    = new AtlasEntityDef(TYPE_COLUMN);
        AtlasAttributeDef attrTable = new AtlasAttributeDef(ATTR_TABLE, TYPE_TABLE);

        attrTable.addConstraint(new AtlasConstraintDef(AtlasConstraintDef.CONSTRAINT_TYPE_INVERSE_REF));
        column.addAttribute(attrTable);

        return column;
    }

    private AtlasEntityDef createColumnEntityDefWithInvaidAttributeTypeForInverseAttribute() {
        AtlasEntityDef    column    = new AtlasEntityDef(TYPE_COLUMN);
        AtlasAttributeDef attrTable = new AtlasAttributeDef(ATTR_NAME, AtlasBaseTypeDef.ATLAS_TYPE_STRING);

        Map<String, Object> params = new HashMap<>();
        params.put(AtlasConstraintDef.CONSTRAINT_PARAM_ATTRIBUTE, ATTR_NAME);

        attrTable.addConstraint(new AtlasConstraintDef(AtlasConstraintDef.CONSTRAINT_TYPE_INVERSE_REF, params));
        column.addAttribute(attrTable);

        return column;
    }

    private AtlasEntityDef createColumnEntityDefWithNonExistingInverseAttribute() {
        AtlasEntityDef    column    = new AtlasEntityDef(TYPE_COLUMN);
        AtlasAttributeDef attrTable = new AtlasAttributeDef(ATTR_TABLE, TYPE_TABLE);

        Map<String, Object> params = new HashMap<>();
        params.put(AtlasConstraintDef.CONSTRAINT_PARAM_ATTRIBUTE, "non-existing:" + ATTR_COLUMNS);

        attrTable.addConstraint(new AtlasConstraintDef(AtlasConstraintDef.CONSTRAINT_TYPE_INVERSE_REF, params));
        column.addAttribute(attrTable);

        return column;
    }

    private AtlasEntityDef createColumnEntityDefWithInvalidInverseAttributeType() {
        AtlasEntityDef    column    = new AtlasEntityDef(TYPE_COLUMN);
        AtlasAttributeDef attrTable = new AtlasAttributeDef(ATTR_TABLE, TYPE_TABLE);

        Map<String, Object> params = new HashMap<>();
        params.put(AtlasConstraintDef.CONSTRAINT_PARAM_ATTRIBUTE, ATTR_NAME);

        attrTable.addConstraint(new AtlasConstraintDef(AtlasConstraintDef.CONSTRAINT_TYPE_INVERSE_REF, params));
        column.addAttribute(attrTable);

        return column;
    }

    private AtlasEntityDef createColumnEntityDef() {
        AtlasEntityDef    column    = new AtlasEntityDef(TYPE_COLUMN);
        AtlasAttributeDef attrTable = new AtlasAttributeDef(ATTR_TABLE, TYPE_TABLE);

        Map<String, Object> params = new HashMap<>();
        params.put(AtlasConstraintDef.CONSTRAINT_PARAM_ATTRIBUTE, ATTR_COLUMNS);

        attrTable.addConstraint(new AtlasConstraintDef(AtlasConstraintDef.CONSTRAINT_TYPE_INVERSE_REF, params));
        column.addAttribute(attrTable);

        return column;
    }
}
