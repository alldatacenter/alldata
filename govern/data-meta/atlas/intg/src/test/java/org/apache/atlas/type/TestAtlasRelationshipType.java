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

import org.apache.atlas.AtlasErrorCode;
import org.apache.atlas.exception.AtlasBaseException;
import org.apache.atlas.model.typedef.AtlasBaseTypeDef;
import org.apache.atlas.type.AtlasStructType.AtlasAttribute;
import org.apache.atlas.type.AtlasTypeRegistry.AtlasTransientTypeRegistry;
import org.apache.atlas.model.typedef.AtlasEntityDef;
import org.apache.atlas.model.typedef.AtlasRelationshipDef;
import org.apache.atlas.model.typedef.AtlasRelationshipDef.PropagateTags;
import org.apache.atlas.model.typedef.AtlasRelationshipDef.RelationshipCategory;
import org.apache.atlas.model.typedef.AtlasRelationshipEndDef;
import org.apache.atlas.model.typedef.AtlasStructDef.AtlasAttributeDef.Cardinality;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.*;

import static org.testng.Assert.fail;

public class TestAtlasRelationshipType {

    private AtlasTypeRegistry typeRegistry;

    private static final String EMPLOYEE_TYPE                  = "employee";
    private static final String DEPARTMENT_TYPE                = "department";
    private static final String ADDRESS_TYPE                   = "address";
    private static final String PHONE_TYPE                     = "phone";
    private static final String DEPT_EMPLOYEE_RELATION_TYPE    = "departmentEmployee";
    private static final String EMPLOYEE_ADDRESS_RELATION_TYPE = "employeeAddress";
    private static final String EMPLOYEE_PHONE_RELATION_TYPE   = "employeePhone";

    @BeforeMethod
    public void setUp() throws AtlasBaseException {
        typeRegistry = new AtlasTypeRegistry();
        createEmployeeTypes();
        createRelationshipTypes();
    }

    @Test
    public void testvalidateAtlasRelationshipDef() throws AtlasBaseException {
        AtlasRelationshipEndDef ep_single = new AtlasRelationshipEndDef("typeA", "attr1", Cardinality.SINGLE);
        AtlasRelationshipEndDef ep_single_container = new AtlasRelationshipEndDef("typeB", "attr2", Cardinality.SINGLE);
        AtlasRelationshipEndDef ep_single_container_2 = new AtlasRelationshipEndDef("typeC", "attr3", Cardinality.SINGLE, true);
        AtlasRelationshipEndDef ep_single_container_3 = new AtlasRelationshipEndDef("typeD", "attr4", Cardinality.SINGLE, true);
        AtlasRelationshipEndDef ep_SET = new AtlasRelationshipEndDef("typeD", "attr4", Cardinality.SET,false);
        AtlasRelationshipEndDef ep_LIST = new AtlasRelationshipEndDef("typeE", "attr5", Cardinality.LIST,true);
        AtlasRelationshipEndDef ep_SET_container = new AtlasRelationshipEndDef("typeF", "attr6", Cardinality.SET,true);
        AtlasRelationshipDef relationshipDef1 = new AtlasRelationshipDef("emptyRelationshipDef", "desc 1", "version1",
                RelationshipCategory.ASSOCIATION, PropagateTags.ONE_TO_TWO, ep_single, ep_SET);
        AtlasRelationshipType.validateAtlasRelationshipDef(relationshipDef1);
        AtlasRelationshipDef relationshipDef2 = new AtlasRelationshipDef("emptyRelationshipDef", "desc 1", "version1",
                RelationshipCategory.COMPOSITION, PropagateTags.ONE_TO_TWO, ep_SET_container, ep_single);
        AtlasRelationshipType.validateAtlasRelationshipDef(relationshipDef2);
        AtlasRelationshipDef relationshipDef3 = new AtlasRelationshipDef("emptyRelationshipDef", "desc 1", "version1",
                RelationshipCategory.AGGREGATION, PropagateTags.ONE_TO_TWO, ep_SET_container, ep_single);
        AtlasRelationshipType.validateAtlasRelationshipDef(relationshipDef3);

        try {
            AtlasRelationshipDef relationshipDef = new AtlasRelationshipDef("emptyRelationshipDef", "desc 1", "version1",
                    RelationshipCategory.ASSOCIATION, PropagateTags.ONE_TO_TWO, ep_single_container_2, ep_single_container);
            AtlasRelationshipType.validateAtlasRelationshipDef(relationshipDef);
            fail("This call is expected to fail");
        } catch (AtlasBaseException abe) {
            if (!abe.getAtlasErrorCode().equals(AtlasErrorCode.RELATIONSHIPDEF_ASSOCIATION_AND_CONTAINER)) {
                fail("This call expected a different error");
            }
        }
        try {
            AtlasRelationshipDef relationshipDef = new AtlasRelationshipDef("emptyRelationshipDef", "desc 1", "version1",
                    RelationshipCategory.COMPOSITION, PropagateTags.ONE_TO_TWO, ep_single, ep_single_container);
            AtlasRelationshipType.validateAtlasRelationshipDef(relationshipDef);
            fail("This call is expected to fail");
        } catch (AtlasBaseException abe) {
            if (!abe.getAtlasErrorCode().equals(AtlasErrorCode.RELATIONSHIPDEF_COMPOSITION_NO_CONTAINER)) {
                fail("This call expected a different error");
            }
        }
        try {
            AtlasRelationshipDef relationshipDef = new AtlasRelationshipDef("emptyRelationshipDef", "desc 1", "version1",
                    RelationshipCategory.AGGREGATION, PropagateTags.ONE_TO_TWO, ep_single, ep_single_container);
            AtlasRelationshipType.validateAtlasRelationshipDef(relationshipDef);
            fail("This call is expected to fail");
        } catch (AtlasBaseException abe) {
            if (!abe.getAtlasErrorCode().equals(AtlasErrorCode.RELATIONSHIPDEF_AGGREGATION_NO_CONTAINER)) {
                fail("This call expected a different error");
            }
        }

        try {
            AtlasRelationshipDef relationshipDef = new AtlasRelationshipDef("emptyRelationshipDef", "desc 1", "version1",
                    RelationshipCategory.COMPOSITION, PropagateTags.ONE_TO_TWO, ep_SET_container, ep_SET);
            AtlasRelationshipType.validateAtlasRelationshipDef(relationshipDef);
            fail("This call is expected to fail");
        } catch (AtlasBaseException abe) {
            if (!abe.getAtlasErrorCode().equals(AtlasErrorCode.RELATIONSHIPDEF_COMPOSITION_MULTIPLE_PARENTS)) {
                fail("This call expected a different error");
            }
        }
        try {
            AtlasRelationshipDef relationshipDef = new AtlasRelationshipDef("emptyRelationshipDef", "desc 1", "version1",
                    RelationshipCategory.COMPOSITION, PropagateTags.ONE_TO_TWO, ep_single, ep_LIST);
            AtlasRelationshipType.validateAtlasRelationshipDef(relationshipDef);
            fail("This call is expected to fail");
        } catch (AtlasBaseException abe) {
            if (!abe.getAtlasErrorCode().equals(AtlasErrorCode.RELATIONSHIPDEF_LIST_ON_END)) {
                fail("This call expected a different error");
            }
        }
        try {
            AtlasRelationshipDef relationshipDef = new AtlasRelationshipDef("emptyRelationshipDef", "desc 1", "version1",
                    RelationshipCategory.COMPOSITION, PropagateTags.ONE_TO_TWO, ep_LIST, ep_single);
            AtlasRelationshipType.validateAtlasRelationshipDef(relationshipDef);
            fail("This call is expected to fail");
        } catch (AtlasBaseException abe) {
            if (!abe.getAtlasErrorCode().equals(AtlasErrorCode.RELATIONSHIPDEF_LIST_ON_END)) {
                fail("This call expected a different error");
            }
        }

    }

    @Test
    public void testRelationshipAttributes() {
        Map<String, Map<String, AtlasAttribute>> employeeRelationAttrs = getRelationAttrsForType(EMPLOYEE_TYPE);

        Assert.assertNotNull(employeeRelationAttrs);
        Assert.assertEquals(employeeRelationAttrs.size(), 2);

        Assert.assertTrue(employeeRelationAttrs.containsKey("department"));
        Assert.assertTrue(employeeRelationAttrs.containsKey("address"));

        AtlasAttribute deptAttr = employeeRelationAttrs.get("department").values().iterator().next();
        Assert.assertEquals(deptAttr.getTypeName(), DEPARTMENT_TYPE);

        AtlasAttribute addrAttr = employeeRelationAttrs.get("address").values().iterator().next();
        Assert.assertEquals(addrAttr.getTypeName(), ADDRESS_TYPE);

        Map<String, Map<String, AtlasAttribute>> deptRelationAttrs = getRelationAttrsForType(DEPARTMENT_TYPE);

        Assert.assertNotNull(deptRelationAttrs);
        Assert.assertEquals(deptRelationAttrs.size(), 1);
        Assert.assertTrue(deptRelationAttrs.containsKey("employees"));

        AtlasAttribute employeesAttr = deptRelationAttrs.get("employees").values().iterator().next();
        Assert.assertEquals(employeesAttr.getTypeName(),AtlasBaseTypeDef.getArrayTypeName(EMPLOYEE_TYPE));

        Map<String, Map<String, AtlasAttribute>> addressRelationAttrs = getRelationAttrsForType(ADDRESS_TYPE);

        Assert.assertNotNull(addressRelationAttrs);
        Assert.assertEquals(addressRelationAttrs.size(), 1);
        Assert.assertTrue(addressRelationAttrs.containsKey("employees"));

        AtlasAttribute employeesAttr1 = addressRelationAttrs.get("employees").values().iterator().next();
        Assert.assertEquals(employeesAttr1.getTypeName(),AtlasBaseTypeDef.getArrayTypeName(EMPLOYEE_TYPE));
    }

    @Test(dependsOnMethods = "testRelationshipAttributes")
    public void testRelationshipAttributesOnExistingAttributes() throws Exception {
        AtlasRelationshipDef employeePhoneRelationDef = new AtlasRelationshipDef(EMPLOYEE_PHONE_RELATION_TYPE, getDescription(EMPLOYEE_PHONE_RELATION_TYPE), "1.0",
                                                                                 RelationshipCategory.ASSOCIATION, PropagateTags.ONE_TO_TWO,
                                                                                 new AtlasRelationshipEndDef(EMPLOYEE_TYPE, "phone_no", Cardinality.SINGLE),
                                                                                 new AtlasRelationshipEndDef(PHONE_TYPE, "owner", Cardinality.SINGLE));

        createType(employeePhoneRelationDef);

        Map<String, Map<String, AtlasAttribute>> employeeRelationshipAttrs = getRelationAttrsForType(EMPLOYEE_TYPE);
        Map<String, AtlasAttribute>              employeeAttrs             = getAttrsForType(EMPLOYEE_TYPE);

        // validate if phone_no exists in both relationAttributes and attributes
        Assert.assertTrue(employeeRelationshipAttrs.containsKey("phone_no"));
        Assert.assertTrue(employeeAttrs.containsKey("phone_no"));
    }

    private void createEmployeeTypes() throws AtlasBaseException {
        AtlasEntityDef phoneDef      = AtlasTypeUtil.createClassTypeDef(PHONE_TYPE, getDescription(PHONE_TYPE), Collections.<String>emptySet(),
                                                                        AtlasTypeUtil.createRequiredAttrDef("phone_number", "int"),
                                                                        AtlasTypeUtil.createOptionalAttrDef("area_code", "int"),
                                                                        AtlasTypeUtil.createOptionalAttrDef("owner", EMPLOYEE_TYPE));

        AtlasEntityDef employeeDef   = AtlasTypeUtil.createClassTypeDef(EMPLOYEE_TYPE, getDescription(EMPLOYEE_TYPE), Collections.<String>emptySet(),
                                                                        AtlasTypeUtil.createRequiredAttrDef("name", "string"),
                                                                        AtlasTypeUtil.createOptionalAttrDef("dob", "date"),
                                                                        AtlasTypeUtil.createOptionalAttrDef("age", "int"),
                                                                        AtlasTypeUtil.createRequiredAttrDef("phone_no", PHONE_TYPE));

        AtlasEntityDef departmentDef = AtlasTypeUtil.createClassTypeDef(DEPARTMENT_TYPE, getDescription(DEPARTMENT_TYPE), Collections.<String>emptySet(),
                                                                        AtlasTypeUtil.createRequiredAttrDef("name", "string"),
                                                                        AtlasTypeUtil.createOptionalAttrDef("count", "int"));

        AtlasEntityDef addressDef    = AtlasTypeUtil.createClassTypeDef(ADDRESS_TYPE, getDescription(ADDRESS_TYPE), Collections.<String>emptySet(),
                                                                        AtlasTypeUtil.createOptionalAttrDef("street", "string"),
                                                                        AtlasTypeUtil.createRequiredAttrDef("city", "string"),
                                                                        AtlasTypeUtil.createRequiredAttrDef("state", "string"),
                                                                        AtlasTypeUtil.createOptionalAttrDef("zip", "int"));

        createTypes(new ArrayList<>(Arrays.asList(phoneDef, employeeDef, departmentDef, addressDef)));
    }

    private void createRelationshipTypes() throws AtlasBaseException {
        AtlasRelationshipDef deptEmployeeRelationDef = new AtlasRelationshipDef(DEPT_EMPLOYEE_RELATION_TYPE, getDescription(DEPT_EMPLOYEE_RELATION_TYPE), "1.0",
                                                                                RelationshipCategory.ASSOCIATION, PropagateTags.ONE_TO_TWO,
                                                                                new AtlasRelationshipEndDef(EMPLOYEE_TYPE, "department", Cardinality.SINGLE),
                                                                                new AtlasRelationshipEndDef(DEPARTMENT_TYPE, "employees", Cardinality.SET));

        AtlasRelationshipDef employeeAddrRelationDef = new AtlasRelationshipDef(EMPLOYEE_ADDRESS_RELATION_TYPE, getDescription(EMPLOYEE_ADDRESS_RELATION_TYPE), "1.0",
                                                                                RelationshipCategory.ASSOCIATION, PropagateTags.ONE_TO_TWO,
                                                                                new AtlasRelationshipEndDef(EMPLOYEE_TYPE, "address", Cardinality.SINGLE),
                                                                                new AtlasRelationshipEndDef(ADDRESS_TYPE, "employees", Cardinality.SET));

        createTypes(new ArrayList<>(Arrays.asList(deptEmployeeRelationDef, employeeAddrRelationDef)));
    }

    private void createType(AtlasBaseTypeDef typeDef) throws AtlasBaseException {
        createTypes(new ArrayList<>(Arrays.asList(typeDef)));
    }

    private void createTypes(List<? extends AtlasBaseTypeDef> typeDefs) throws AtlasBaseException {
        AtlasTransientTypeRegistry ttr = typeRegistry.lockTypeRegistryForUpdate();

        ttr.addTypes(typeDefs);

        typeRegistry.releaseTypeRegistryForUpdate(ttr, true);
    }

    private String getDescription(String typeName) {
        return typeName + " description";
    }

    private Map<String, Map<String, AtlasAttribute>> getRelationAttrsForType(String typeName) {
        return typeRegistry.getEntityTypeByName(typeName).getRelationshipAttributes();
    }

    private Map<String, AtlasAttribute> getAttrsForType(String typeName) {
        return typeRegistry.getEntityTypeByName(typeName).getAllAttributes();
    }
}
