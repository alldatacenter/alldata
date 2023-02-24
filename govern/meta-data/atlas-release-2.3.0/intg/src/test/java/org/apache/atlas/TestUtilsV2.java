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

package org.apache.atlas;

import org.apache.atlas.model.instance.AtlasEntity;
import org.apache.atlas.model.instance.AtlasEntity.AtlasEntitiesWithExtInfo;
import org.apache.atlas.model.instance.AtlasEntity.AtlasEntityWithExtInfo;
import org.apache.atlas.model.instance.AtlasObjectId;
import org.apache.atlas.model.instance.AtlasStruct;
import org.apache.atlas.model.typedef.AtlasBaseTypeDef;
import org.apache.atlas.model.typedef.AtlasBusinessMetadataDef;
import org.apache.atlas.model.typedef.AtlasClassificationDef;
import org.apache.atlas.model.typedef.AtlasEntityDef;
import org.apache.atlas.model.typedef.AtlasEnumDef;
import org.apache.atlas.model.typedef.AtlasEnumDef.AtlasEnumElementDef;
import org.apache.atlas.model.typedef.AtlasStructDef;
import org.apache.atlas.model.typedef.AtlasStructDef.AtlasAttributeDef;
import org.apache.atlas.model.typedef.AtlasStructDef.AtlasConstraintDef;
import org.apache.atlas.model.typedef.AtlasTypesDef;
import org.apache.atlas.type.AtlasTypeUtil;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.apache.atlas.model.typedef.AtlasStructDef.AtlasAttributeDef.Cardinality.LIST;
import static org.apache.atlas.model.typedef.AtlasStructDef.AtlasAttributeDef.Cardinality.SINGLE;
import static org.apache.atlas.model.typedef.AtlasStructDef.AtlasConstraintDef.CONSTRAINT_PARAM_ATTRIBUTE;
import static org.apache.atlas.model.typedef.AtlasStructDef.AtlasConstraintDef.CONSTRAINT_TYPE_INVERSE_REF;
import static org.apache.atlas.model.typedef.AtlasStructDef.AtlasConstraintDef.CONSTRAINT_TYPE_OWNED_REF;
import static org.apache.atlas.type.AtlasTypeUtil.createClassTypeDef;
import static org.apache.atlas.type.AtlasTypeUtil.createOptionalAttrDef;
import static org.apache.atlas.type.AtlasTypeUtil.createRequiredAttrDef;
import static org.apache.atlas.type.AtlasTypeUtil.createStructTypeDef;
import static org.apache.atlas.type.AtlasTypeUtil.createUniqueRequiredAttrDef;
import static org.apache.atlas.type.AtlasTypeUtil.getAtlasObjectId;
import static org.apache.atlas.type.AtlasTypeUtil.createBusinessMetadataDef;


/**
 * Test utility class.
 */
public final class TestUtilsV2 {
    private static final Logger LOG = LoggerFactory.getLogger(TestUtilsV2.class);
    public static final long TEST_DATE_IN_LONG = 1418265358440L;

    public static final String TEST_USER   = "testUser";
    public static final String STRUCT_TYPE = "struct_type";
    public static final String ENTITY_TYPE = "entity_type";
    public static final String ENTITY_TYPE_MAP = "map_entity_type";

    private static AtomicInteger seq = new AtomicInteger();

    private TestUtilsV2() {
    }

    /**
     * Class Hierarchy is:
     * Department(name : String, employees : Array[Person])
     * Person(name : String, department : Department, manager : Manager)
     * Manager(subordinates : Array[Person]) extends Person
     * <p/>
     * Persons can have SecurityClearance(level : Int) clearance.
     */
    public static AtlasTypesDef defineDeptEmployeeTypes() {

        String _description = "_description";
        AtlasEnumDef orgLevelEnum =
                new AtlasEnumDef("OrgLevel", "OrgLevel"+_description, "1.0",
                        Arrays.asList(
                                new AtlasEnumElementDef("L1", "Element"+_description, 1),
                                new AtlasEnumElementDef("L2", "Element"+_description, 2)
                        ));

        AtlasStructDef addressDetails =
                createStructTypeDef("Address", "Address"+_description,
                        createRequiredAttrDef("street", "string"),
                        createRequiredAttrDef("city", "string"));

        AtlasEntityDef deptTypeDef =
                createClassTypeDef(DEPARTMENT_TYPE, "Department"+_description, Collections.<String>emptySet(),
                        createUniqueRequiredAttrDef("name", "string"),
                        new AtlasAttributeDef("employees", String.format("array<%s>", "Employee"), true,
                                SINGLE, 0, 1, false, false, false,
                            new ArrayList<AtlasStructDef.AtlasConstraintDef>() {{
                                add(new AtlasStructDef.AtlasConstraintDef(CONSTRAINT_TYPE_OWNED_REF));
                            }}));

        AtlasEntityDef personTypeDef = createClassTypeDef("Person", "Person"+_description, Collections.<String>emptySet(),
                createUniqueRequiredAttrDef("name", "string"),
                createOptionalAttrDef("address", "Address"),
                createOptionalAttrDef("birthday", "date"),
                createOptionalAttrDef("hasPets", "boolean"),
                createOptionalAttrDef("numberOfCars", "byte"),
                createOptionalAttrDef("houseNumber", "short"),
                createOptionalAttrDef("carMileage", "int"),
                createOptionalAttrDef("age", "float"),
                createOptionalAttrDef("numberOfStarsEstimate", "biginteger"),
                createOptionalAttrDef("approximationOfPi", "bigdecimal")
        );

        AtlasEntityDef employeeTypeDef = createClassTypeDef("Employee", "Employee"+_description, Collections.singleton("Person"),
                createOptionalAttrDef("orgLevel", "OrgLevel"),
                new AtlasAttributeDef("department", "Department", false,
                        SINGLE, 1, 1,
                        false, false, false,
                        new ArrayList<>()),
                new AtlasAttributeDef("manager", "Manager", true,
                        SINGLE, 0, 1,
                        false, false, false,
                new ArrayList<AtlasConstraintDef>() {{
                        add(new AtlasConstraintDef(
                            CONSTRAINT_TYPE_INVERSE_REF, new HashMap<String, Object>() {{
                            put(CONSTRAINT_PARAM_ATTRIBUTE, "subordinates");
                        }}));
                    }}),
                new AtlasAttributeDef("mentor", EMPLOYEE_TYPE, true,
                        SINGLE, 0, 1,
                        false, false, false,
                        Collections.<AtlasConstraintDef>emptyList()),
                createOptionalAttrDef("shares", "long"),
                createOptionalAttrDef("salary", "double")
                );

        employeeTypeDef.getAttribute("department").addConstraint(
            new AtlasConstraintDef(
                CONSTRAINT_TYPE_INVERSE_REF, new HashMap<String, Object>() {{
                put(CONSTRAINT_PARAM_ATTRIBUTE, "employees");
            }}));

        AtlasEntityDef managerTypeDef = createClassTypeDef("Manager", "Manager"+_description, Collections.singleton("Employee"),
                new AtlasAttributeDef("subordinates", String.format("array<%s>", "Employee"), false, AtlasAttributeDef.Cardinality.SET,
                        1, 10, false, false, false,
                        Collections.<AtlasConstraintDef>emptyList()));

        AtlasClassificationDef securityClearanceTypeDef =
                AtlasTypeUtil.createTraitTypeDef("SecurityClearance", "SecurityClearance"+_description, Collections.<String>emptySet(),
                        createRequiredAttrDef("level", "int"));

        AtlasTypesDef ret = new AtlasTypesDef(Collections.singletonList(orgLevelEnum), Collections.singletonList(addressDetails),
                Collections.singletonList(securityClearanceTypeDef),
                Arrays.asList(deptTypeDef, personTypeDef, employeeTypeDef, managerTypeDef));

        populateSystemAttributes(ret);

        return ret;
    }

    public static AtlasTypesDef defineInverseReferenceTestTypes() {
        AtlasEntityDef aDef = createClassTypeDef("A", Collections.<String>emptySet(),
            createUniqueRequiredAttrDef("name", "string"),
            new AtlasAttributeDef("b", "B", true, SINGLE, 0, 1, false, false, false, Collections.<AtlasConstraintDef>emptyList()), // 1-1
            new AtlasAttributeDef("oneB", "B", true, SINGLE, 0, 1, false, false, false, Collections.<AtlasConstraintDef>emptyList()), // 1-*
            new AtlasAttributeDef("manyB", AtlasBaseTypeDef.getArrayTypeName("B"), true, SINGLE, 0, 1, false, false, false, Collections.<AtlasConstraintDef>emptyList()),
            new AtlasAttributeDef("mapToB", AtlasBaseTypeDef.getMapTypeName("string", "B"), true, SINGLE, 0, 1, false, false, false,
                Collections.<AtlasConstraintDef>singletonList(new AtlasConstraintDef(
                CONSTRAINT_TYPE_INVERSE_REF, Collections.<String, Object>singletonMap(CONSTRAINT_PARAM_ATTRIBUTE, "mappedFromA"))))); // *-*

        AtlasEntityDef bDef = createClassTypeDef("B", Collections.<String>emptySet(),
            createUniqueRequiredAttrDef("name", "string"),
            new AtlasAttributeDef("a", "A", true, SINGLE, 0, 1, false, false, false,
                Collections.<AtlasConstraintDef>singletonList(new AtlasConstraintDef(
                    CONSTRAINT_TYPE_INVERSE_REF, Collections.<String, Object>singletonMap(CONSTRAINT_PARAM_ATTRIBUTE, "b")))),
            new AtlasAttributeDef("manyA", AtlasBaseTypeDef.getArrayTypeName("A"), true, SINGLE, 0, 1, false, false, false,
                Collections.<AtlasConstraintDef>singletonList(new AtlasConstraintDef(
                    CONSTRAINT_TYPE_INVERSE_REF, Collections.<String, Object>singletonMap(CONSTRAINT_PARAM_ATTRIBUTE, "oneB")))),
            new AtlasAttributeDef("manyToManyA", AtlasBaseTypeDef.getArrayTypeName("A"), true, SINGLE, 0, 1, false, false, false,
                Collections.<AtlasConstraintDef>singletonList(new AtlasConstraintDef(
                    CONSTRAINT_TYPE_INVERSE_REF, Collections.<String, Object>singletonMap(CONSTRAINT_PARAM_ATTRIBUTE, "manyB")))),
            new AtlasAttributeDef("mappedFromA", "A", true, SINGLE, 0, 1, false, false, false, Collections.<AtlasConstraintDef>emptyList()));

        AtlasTypesDef ret = new AtlasTypesDef(Collections.<AtlasEnumDef>emptyList(), Collections.<AtlasStructDef>emptyList(), Collections.<AtlasClassificationDef>emptyList(), Arrays.asList(aDef, bDef));

        populateSystemAttributes(ret);

        return ret;
    }

    public static AtlasTypesDef defineValidUpdatedDeptEmployeeTypes() {
        String _description = "_description_updated";
        AtlasEnumDef orgLevelEnum =
                new AtlasEnumDef("OrgLevel", "OrgLevel"+_description, "1.0",
                        Arrays.asList(
                                new AtlasEnumElementDef("L1", "Element"+ _description, 1),
                                new AtlasEnumElementDef("L2", "Element"+ _description, 2)
                        ));

        AtlasStructDef addressDetails =
                createStructTypeDef("Address", "Address"+_description,
                        createRequiredAttrDef("street", "string"),
                        createRequiredAttrDef("city", "string"),
                        createOptionalAttrDef("zip", "int"));

        AtlasEntityDef deptTypeDef =
                createClassTypeDef(DEPARTMENT_TYPE, "Department"+_description,
                        Collections.<String>emptySet(),
                        createUniqueRequiredAttrDef("name", "string"),
                        createOptionalAttrDef("dep-code", "string"),
                        new AtlasAttributeDef("employees", String.format("array<%s>", "Employee"), true,
                                SINGLE, 0, 1, false, false, false,
                            new ArrayList<AtlasStructDef.AtlasConstraintDef>() {{
                                add(new AtlasStructDef.AtlasConstraintDef(CONSTRAINT_TYPE_OWNED_REF));
                            }}));

        AtlasEntityDef personTypeDef = createClassTypeDef("Person", "Person"+_description,
                Collections.<String>emptySet(),
                createUniqueRequiredAttrDef("name", "string"),
                createOptionalAttrDef("email", "string"),
                createOptionalAttrDef("address", "Address"),
                createOptionalAttrDef("birthday", "date"),
                createOptionalAttrDef("hasPets", "boolean"),
                createOptionalAttrDef("numberOfCars", "byte"),
                createOptionalAttrDef("houseNumber", "short"),
                createOptionalAttrDef("carMileage", "int"),
                createOptionalAttrDef("age", "float"),
                createOptionalAttrDef("numberOfStarsEstimate", "biginteger"),
                createOptionalAttrDef("approximationOfPi", "bigdecimal")
        );

        AtlasEntityDef employeeTypeDef = createClassTypeDef("Employee", "Employee"+_description,
                Collections.singleton("Person"),
                createOptionalAttrDef("orgLevel", "OrgLevel"),
                createOptionalAttrDef("empCode", "string"),
                new AtlasAttributeDef("department", "Department", false,
                        SINGLE, 1, 1,
                        false, false, false,
                        Collections.<AtlasConstraintDef>emptyList()),
                new AtlasAttributeDef("manager", "Manager", true,
                        SINGLE, 0, 1,
                        false, false, false,
                    new ArrayList<AtlasConstraintDef>() {{
                        add(new AtlasConstraintDef(
                            CONSTRAINT_TYPE_INVERSE_REF, new HashMap<String, Object>() {{
                            put(CONSTRAINT_PARAM_ATTRIBUTE, "subordinates");
                        }}));
                    }}),
                new AtlasAttributeDef("mentor", EMPLOYEE_TYPE, true,
                        SINGLE, 0, 1,
                        false, false, false,
                        Collections.<AtlasConstraintDef>emptyList()),
                createOptionalAttrDef("shares", "long"),
                createOptionalAttrDef("salary", "double")

        );

        AtlasEntityDef managerTypeDef = createClassTypeDef("Manager", "Manager"+_description,
                Collections.singleton("Employee"),
                new AtlasAttributeDef("subordinates", String.format("array<%s>", "Employee"), false, AtlasAttributeDef.Cardinality.SET,
                        1, 10, false, false, false,
                        Collections.<AtlasConstraintDef>emptyList()));

        AtlasClassificationDef securityClearanceTypeDef =
                AtlasTypeUtil.createTraitTypeDef("SecurityClearance", "SecurityClearance"+_description, Collections.<String>emptySet(),
                        createRequiredAttrDef("level", "int"));

        AtlasTypesDef ret = new AtlasTypesDef(Collections.singletonList(orgLevelEnum),
                Collections.singletonList(addressDetails),
                Collections.singletonList(securityClearanceTypeDef),
                Arrays.asList(deptTypeDef, personTypeDef, employeeTypeDef, managerTypeDef));

        populateSystemAttributes(ret);

        return ret;
    }

    public static AtlasTypesDef defineInvalidUpdatedDeptEmployeeTypes() {
        String _description = "_description_updated";
        // Test ordinal changes
        AtlasEnumDef orgLevelEnum =
                new AtlasEnumDef("OrgLevel", "OrgLevel"+_description, "1.0",
                        Arrays.asList(
                                new AtlasEnumElementDef("L2", "Element"+ _description, 1),
                                new AtlasEnumElementDef("L1", "Element"+ _description, 2),
                                new AtlasEnumElementDef("L3", "Element"+ _description, 3)
                        ));

        AtlasStructDef addressDetails =
                createStructTypeDef("Address", "Address"+_description,
                        createRequiredAttrDef("street", "string"),
                        createRequiredAttrDef("city", "string"),
                        createRequiredAttrDef("zip", "int"));

        AtlasEntityDef deptTypeDef =
                createClassTypeDef(DEPARTMENT_TYPE, "Department"+_description, Collections.<String>emptySet(),
                        createRequiredAttrDef("name", "string"),
                        createRequiredAttrDef("dep-code", "string"),
                        new AtlasAttributeDef("employees", String.format("array<%s>", "Person"), true,
                                SINGLE, 0, 1, false, false, false,
                            new ArrayList<AtlasStructDef.AtlasConstraintDef>() {{
                                add(new AtlasStructDef.AtlasConstraintDef(CONSTRAINT_TYPE_OWNED_REF));
                            }}));

        AtlasEntityDef personTypeDef = createClassTypeDef("Person", "Person"+_description, Collections.<String>emptySet(),
                createRequiredAttrDef("name", "string"),
                createRequiredAttrDef("emp-code", "string"),
                createOptionalAttrDef("orgLevel", "OrgLevel"),
                createOptionalAttrDef("address", "Address"),
                new AtlasAttributeDef("department", "Department", false,
                        SINGLE, 1, 1,
                        false, false, false,
                        Collections.<AtlasConstraintDef>emptyList()),
                new AtlasAttributeDef("manager", "Manager", true,
                        SINGLE, 0, 1,
                        false, false, false,
                    new ArrayList<AtlasConstraintDef>() {{
                        add(new AtlasConstraintDef(
                            CONSTRAINT_TYPE_INVERSE_REF, new HashMap<String, Object>() {{
                            put(CONSTRAINT_PARAM_ATTRIBUTE, "subordinates");
                        }}));
                    }}),
                new AtlasAttributeDef("mentor", "Person", true,
                        SINGLE, 0, 1,
                        false, false, false,
                        Collections.<AtlasConstraintDef>emptyList()),
                createOptionalAttrDef("birthday", "date"),
                createOptionalAttrDef("hasPets", "boolean"),
                createOptionalAttrDef("numberOfCars", "byte"),
                createOptionalAttrDef("houseNumber", "short"),
                createOptionalAttrDef("carMileage", "int"),
                createOptionalAttrDef("shares", "long"),
                createOptionalAttrDef("salary", "double"),
                createRequiredAttrDef("age", "float"),
                createOptionalAttrDef("numberOfStarsEstimate", "biginteger"),
                createOptionalAttrDef("approximationOfPi", "bigdecimal")
        );

        AtlasTypesDef ret = new AtlasTypesDef(Collections.singletonList(orgLevelEnum),
                                              Collections.singletonList(addressDetails),
                                              Collections.<AtlasClassificationDef>emptyList(),
                                              Arrays.asList(deptTypeDef, personTypeDef));

        populateSystemAttributes(ret);

        return ret;
    }

    public static final String DEPARTMENT_TYPE = "Department";
    public static final String EMPLOYEE_TYPE   = "Employee";
    public static final String MANAGER_TYPE    = "Manager";
    public static final String ADDRESS_TYPE    = "Address";

    public static AtlasEntitiesWithExtInfo createDeptEg2() {
        AtlasEntitiesWithExtInfo entitiesWithExtInfo = new AtlasEntitiesWithExtInfo();

        /******* Department - HR *******/
        AtlasEntity   hrDept   = new AtlasEntity(DEPARTMENT_TYPE, "name", "hr");
        AtlasObjectId hrDeptId = getAtlasObjectId(hrDept);

        /******* Address Entities *******/
        AtlasStruct janeAddr = new AtlasStruct(ADDRESS_TYPE);
            janeAddr.setAttribute("street", "Great America Parkway");
            janeAddr.setAttribute("city", "Santa Clara");

        AtlasStruct juliusAddr = new AtlasStruct(ADDRESS_TYPE);
            juliusAddr.setAttribute("street", "Madison Ave");
            juliusAddr.setAttribute("city", "Newtonville");

        AtlasStruct maxAddr = new AtlasStruct(ADDRESS_TYPE);
            maxAddr.setAttribute("street", "Ripley St");
            maxAddr.setAttribute("city", "Newton");

        AtlasStruct johnAddr = new AtlasStruct(ADDRESS_TYPE);
            johnAddr.setAttribute("street", "Stewart Drive");
            johnAddr.setAttribute("city", "Sunnyvale");

        /******* Manager - Jane (John and Max subordinates) *******/
        AtlasEntity   jane   = new AtlasEntity(MANAGER_TYPE);
        AtlasObjectId janeId = getAtlasObjectId(jane);
            jane.setAttribute("name", "Jane");
            jane.setAttribute("department", hrDeptId);
            jane.setAttribute("address", janeAddr);

        /******* Manager - Julius (no subordinates) *******/
        AtlasEntity   julius   = new AtlasEntity(MANAGER_TYPE);
        AtlasObjectId juliusId = getAtlasObjectId(julius);
            julius.setAttribute("name", "Julius");
            julius.setAttribute("department", hrDeptId);
            julius.setAttribute("address", juliusAddr);
            julius.setAttribute("subordinates", Collections.emptyList());

        /******* Employee - Max (Manager: Jane, Mentor: Julius) *******/
        AtlasEntity   max   = new AtlasEntity(EMPLOYEE_TYPE);
        AtlasObjectId maxId = getAtlasObjectId(max);
            max.setAttribute("name", "Max");
            max.setAttribute("department", hrDeptId);
            max.setAttribute("address", maxAddr);
            max.setAttribute("manager", janeId);
            max.setAttribute("mentor", juliusId);
            max.setAttribute("birthday",new Date(1979, 3, 15));
            max.setAttribute("hasPets", true);
            max.setAttribute("age", 36);
            max.setAttribute("numberOfCars", 2);
            max.setAttribute("houseNumber", 17);
            max.setAttribute("carMileage", 13);
            max.setAttribute("shares", Long.MAX_VALUE);
            max.setAttribute("salary", Double.MAX_VALUE);
            max.setAttribute("numberOfStarsEstimate", new BigInteger("1000000000000000000000000000000"));
            max.setAttribute("approximationOfPi", new BigDecimal("3.1415926535897932"));

        /******* Employee - John (Manager: Jane, Mentor: Max) *******/
        AtlasEntity   john   = new AtlasEntity(EMPLOYEE_TYPE);
        AtlasObjectId johnId = getAtlasObjectId(john);
            john.setAttribute("name", "John");
            john.setAttribute("department", hrDeptId);
            john.setAttribute("address", johnAddr);
            john.setAttribute("manager", janeId);
            john.setAttribute("mentor", maxId);
            john.setAttribute("birthday",new Date(1950, 5, 15));
            john.setAttribute("hasPets", true);
            john.setAttribute("numberOfCars", 1);
            john.setAttribute("houseNumber", 153);
            john.setAttribute("carMileage", 13364);
            john.setAttribute("shares", 15000);
            john.setAttribute("salary", 123345.678);
            john.setAttribute("age", 50);
            john.setAttribute("numberOfStarsEstimate", new BigInteger("1000000000000000000000"));
            john.setAttribute("approximationOfPi", new BigDecimal("3.141592653589793238462643383279502884197169399375105820974944592307816406286"));

        jane.setAttribute("subordinates", Arrays.asList(johnId, maxId));
        hrDept.setAttribute("employees", Arrays.asList(janeId, juliusId, maxId, johnId));

        entitiesWithExtInfo.addEntity(hrDept);
        entitiesWithExtInfo.addEntity(jane);
        entitiesWithExtInfo.addEntity(julius);
        entitiesWithExtInfo.addEntity(max);
        entitiesWithExtInfo.addEntity(john);

        return entitiesWithExtInfo;
    }

    public static Map<String, AtlasEntity> createDeptEg1() {
        Map<String, AtlasEntity> deptEmpEntities = new HashMap<>();

        AtlasEntity hrDept = new AtlasEntity(DEPARTMENT_TYPE);
        AtlasEntity john = new AtlasEntity(EMPLOYEE_TYPE);

        AtlasEntity jane = new AtlasEntity("Manager");
        AtlasEntity johnAddr = new AtlasEntity("Address");
        AtlasEntity janeAddr = new AtlasEntity("Address");
        AtlasEntity julius = new AtlasEntity("Manager");
        AtlasEntity juliusAddr = new AtlasEntity("Address");
        AtlasEntity max = new AtlasEntity(EMPLOYEE_TYPE);
        AtlasEntity maxAddr = new AtlasEntity("Address");

        AtlasObjectId deptId = new AtlasObjectId(hrDept.getGuid(), hrDept.getTypeName());
        hrDept.setAttribute("name", "hr");
        john.setAttribute("name", "John");
        john.setAttribute("department", deptId);
        johnAddr.setAttribute("street", "Stewart Drive");
        johnAddr.setAttribute("city", "Sunnyvale");
        john.setAttribute("address", johnAddr);

        john.setAttribute("birthday",new Date(1950, 5, 15));
        john.setAttribute("hasPets", true);
        john.setAttribute("numberOfCars", 1);
        john.setAttribute("houseNumber", 153);
        john.setAttribute("carMileage", 13364);
        john.setAttribute("shares", 15000);
        john.setAttribute("salary", 123345.678);
        john.setAttribute("age", 50);
        john.setAttribute("numberOfStarsEstimate", new BigInteger("1000000000000000000000"));
        john.setAttribute("approximationOfPi", new BigDecimal("3.141592653589793238462643383279502884197169399375105820974944592307816406286"));

        jane.setAttribute("name", "Jane");
        jane.setAttribute("department", deptId);
        janeAddr.setAttribute("street", "Great America Parkway");
        janeAddr.setAttribute("city", "Santa Clara");
        jane.setAttribute("address", janeAddr);
        janeAddr.setAttribute("street", "Great America Parkway");

        julius.setAttribute("name", "Julius");
        julius.setAttribute("department", deptId);
        juliusAddr.setAttribute("street", "Madison Ave");
        juliusAddr.setAttribute("city", "Newtonville");
        julius.setAttribute("address", juliusAddr);
        julius.setAttribute("subordinates", Collections.emptyList());

        AtlasObjectId janeId = getAtlasObjectId(jane);
        AtlasObjectId johnId = getAtlasObjectId(john);

        //TODO - Change to MANAGER_TYPE for JULIUS
        AtlasObjectId maxId = new AtlasObjectId(max.getGuid(), EMPLOYEE_TYPE);
        AtlasObjectId juliusId = new AtlasObjectId(julius.getGuid(), EMPLOYEE_TYPE);

        max.setAttribute("name", "Max");
        max.setAttribute("department", deptId);
        maxAddr.setAttribute("street", "Ripley St");
        maxAddr.setAttribute("city", "Newton");
        max.setAttribute("address", maxAddr);
        max.setAttribute("manager", janeId);
        max.setAttribute("mentor", juliusId);
        max.setAttribute("birthday",new Date(1979, 3, 15));
        max.setAttribute("hasPets", true);
        max.setAttribute("age", 36);
        max.setAttribute("numberOfCars", 2);
        max.setAttribute("houseNumber", 17);
        max.setAttribute("carMileage", 13);
        max.setAttribute("shares", Long.MAX_VALUE);
        max.setAttribute("salary", Double.MAX_VALUE);
        max.setAttribute("numberOfStarsEstimate", new BigInteger("1000000000000000000000000000000"));
        max.setAttribute("approximationOfPi", new BigDecimal("3.1415926535897932"));

        john.setAttribute("manager", janeId);
        john.setAttribute("mentor", maxId);
        hrDept.setAttribute("employees", Arrays.asList(johnId, janeId, juliusId, maxId));

        jane.setAttribute("subordinates", Arrays.asList(johnId, maxId));

        deptEmpEntities.put(jane.getGuid(), jane);
        deptEmpEntities.put(john.getGuid(), john);
        deptEmpEntities.put(julius.getGuid(), julius);
        deptEmpEntities.put(max.getGuid(), max);
        deptEmpEntities.put(deptId.getGuid(), hrDept);
        return deptEmpEntities;
    }

    public static final String DATABASE_TYPE = "hive_database";
    public static final String DATABASE_NAME = "foo";
    public static final String TABLE_TYPE = "hive_table";
    public static final String PROCESS_TYPE = "hive_process";
    public static final String COLUMN_TYPE = "column_type";
    public static final String TABLE_NAME = "bar";
    public static final String CLASSIFICATION = "classification";
    public static final String FETL_CLASSIFICATION = "fetl" + CLASSIFICATION;

    public static final String PII = "PII";
    public static final String PHI = "PHI";
    public static final String SUPER_TYPE_NAME = "Referenceable";
    public static final String STORAGE_DESC_TYPE = "hive_storagedesc";
    public static final String PARTITION_STRUCT_TYPE = "partition_struct_type";
    public static final String PARTITION_CLASS_TYPE = "partition_class_type";
    public static final String SERDE_TYPE = "serdeType";
    public static final String COLUMNS_MAP = "columnsMap";
    public static final String COLUMNS_ATTR_NAME = "columns";
    public static final String TABLE_ATTR_NAME = "table";
    public static final String ENTITY_TYPE_WITH_SIMPLE_ATTR = "entity_with_simple_attr";
    public static final String ENTITY_TYPE_WITH_NESTED_COLLECTION_ATTR = "entity_with_nested_collection_attr";
    public static final String ENTITY_TYPE_WITH_COMPLEX_COLLECTION_ATTR = "entity_with_complex_collection_attr";
    public static final String ENTITY_TYPE_WITH_COMPLEX_COLLECTION_ATTR_DELETE = "entity_with_complex_collection_attr_delete";

    public static final String NAME = "name";

    public static AtlasTypesDef simpleType(){
        AtlasEntityDef superTypeDefinition =
                createClassTypeDef("h_type", Collections.<String>emptySet(),
                        createOptionalAttrDef("attr", "string"));

        AtlasStructDef structTypeDefinition = new AtlasStructDef("s_type", "structType", "1.0",
                Arrays.asList(createRequiredAttrDef("name", "string")));

        AtlasClassificationDef traitTypeDefinition =
                AtlasTypeUtil.createTraitTypeDef("t_type", "traitType", Collections.<String>emptySet());

        AtlasEnumDef enumTypeDefinition = new AtlasEnumDef("e_type", "enumType", "1.0",
                Arrays.asList(new AtlasEnumElementDef("ONE", "Element Description", 1)));

        AtlasTypesDef ret = AtlasTypeUtil.getTypesDef(Collections.singletonList(enumTypeDefinition), Collections.singletonList(structTypeDefinition),
                Collections.singletonList(traitTypeDefinition), Collections.singletonList(superTypeDefinition));

        populateSystemAttributes(ret);

        return ret;
    }

    public static AtlasTypesDef simpleTypeUpdated(){
        AtlasEntityDef superTypeDefinition =
                createClassTypeDef("h_type", Collections.<String>emptySet(),
                        createOptionalAttrDef("attr", "string"));

        AtlasEntityDef newSuperTypeDefinition =
                createClassTypeDef("new_h_type", Collections.<String>emptySet(),
                        createOptionalAttrDef("attr", "string"));

        AtlasStructDef structTypeDefinition = new AtlasStructDef("s_type", "structType", "1.0",
                Arrays.asList(createRequiredAttrDef("name", "string")));

        AtlasClassificationDef traitTypeDefinition =
                AtlasTypeUtil.createTraitTypeDef("t_type", "traitType", Collections.<String>emptySet());

        AtlasEnumDef enumTypeDefinition = new AtlasEnumDef("e_type", "enumType",
                Arrays.asList(new AtlasEnumElementDef("ONE", "Element Description", 1)));
        AtlasTypesDef ret = AtlasTypeUtil.getTypesDef(Collections.singletonList(enumTypeDefinition), Collections.singletonList(structTypeDefinition),
                Collections.singletonList(traitTypeDefinition), Arrays.asList(superTypeDefinition, newSuperTypeDefinition));

        populateSystemAttributes(ret);

        return ret;
    }

    public static AtlasTypesDef simpleTypeUpdatedDiff() {
        AtlasEntityDef newSuperTypeDefinition =
                createClassTypeDef("new_h_type", Collections.<String>emptySet(),
                        createOptionalAttrDef("attr", "string"));

        AtlasTypesDef ret = AtlasTypeUtil.getTypesDef(Collections.<AtlasEnumDef>emptyList(),
                Collections.<AtlasStructDef>emptyList(),
                Collections.<AtlasClassificationDef>emptyList(),
                Collections.singletonList(newSuperTypeDefinition));

        populateSystemAttributes(ret);

        return ret;
    }

    public static AtlasTypesDef defineSimpleAttrType() {
        AtlasAttributeDef attrPuArray = new AtlasAttributeDef("puArray", "array<string>", true, SINGLE, 1, 1, false, false, false, null);
        AtlasAttributeDef attrPuMap   = new AtlasAttributeDef("puMap", "map<string,string>",  true, SINGLE, 1,1, false, false, false, null);

        attrPuArray.setOption(AtlasAttributeDef.ATTRDEF_OPTION_APPEND_ON_PARTIAL_UPDATE, "true");
        attrPuMap.setOption(AtlasAttributeDef.ATTRDEF_OPTION_APPEND_ON_PARTIAL_UPDATE, "true");


        AtlasEntityDef simpleAttributesEntityType =
            createClassTypeDef(ENTITY_TYPE_WITH_SIMPLE_ATTR, ENTITY_TYPE_WITH_SIMPLE_ATTR + "_description", null,
                createUniqueRequiredAttrDef("name", "string"),

                new AtlasAttributeDef("stringAtrr", "string",
                    true,
                    SINGLE, 1, 1,
                    false, false, false, null),

                new AtlasAttributeDef("arrayOfStrings",
                    "array<string>", true, SINGLE, 1, 1,
                    false, false, false, null),

                new AtlasAttributeDef("mapOfStrings", "map<string,string>",
                    true, SINGLE, 1,1, false, false, false, null),

                attrPuArray,

                attrPuMap
            );

        AtlasTypesDef ret = AtlasTypeUtil.getTypesDef(Collections.<AtlasEnumDef>emptyList(),
            Collections.<AtlasStructDef>emptyList(),
            Collections.<AtlasClassificationDef>emptyList(),
            Collections.singletonList(simpleAttributesEntityType));

        return ret;
    }

    public static AtlasEntityWithExtInfo createSimpleAttrTypeEntity() {
        AtlasEntity entity = new AtlasEntity(ENTITY_TYPE_WITH_SIMPLE_ATTR);

        entity.setAttribute(NAME, ENTITY_TYPE_WITH_SIMPLE_ATTR);
        entity.setAttribute("stringAtrr", "DummyThree");
        entity.setAttribute("arrayOfStrings", Arrays.asList("DummyOne", "DummyTwo"));
        entity.setAttribute("mapOfStrings", Collections.singletonMap("one", "DummyString"));
        entity.setAttribute("puArray", Arrays.asList("DummyOne", "DummyTwo"));
        entity.setAttribute("puMap", Collections.singletonMap("one", "DummyString"));

        return new AtlasEntityWithExtInfo(entity);
    }

    public static AtlasTypesDef defineEnumTypes() {
        String _description = "_description";
        AtlasEnumDef myEnum =
                new AtlasEnumDef("ENUM_1", "ENUM_1" + _description, "1.0",
                        Arrays.asList(
                                new AtlasEnumElementDef("USER", "Element" + _description, 1),
                                new AtlasEnumElementDef("ROLE", "Element" + _description, 2),
                                new AtlasEnumElementDef("GROUP", "Element" + _description, 3)
                        ));

        AtlasTypesDef ret = AtlasTypeUtil.getTypesDef(Collections.singletonList(myEnum),
                new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>());

        populateSystemAttributes(ret);

        return ret;
    }

    public static AtlasTypesDef defineBusinessMetadataTypes() {
        String _description = "_description";
        Map<String, String> options = new HashMap<>();
        options.put("maxStrLength", "20");

        AtlasBusinessMetadataDef bmNoApplicableTypes = createBusinessMetadataDef("bmNoApplicableTypes", _description, "1.0",
                createOptionalAttrDef("attr0", "string", options, _description));

        AtlasBusinessMetadataDef bmNoAttributes = createBusinessMetadataDef("bmNoAttributes", _description, "1.0", null);

        options.put("applicableEntityTypes", "[\"" + DATABASE_TYPE + "\",\"" + TABLE_TYPE + "\"]");

        AtlasBusinessMetadataDef bmWithAllTypes = createBusinessMetadataDef("bmWithAllTypes", _description, "1.0",
                createOptionalAttrDef("attr1", AtlasBusinessMetadataDef.ATLAS_TYPE_BOOLEAN, options, _description),
                createOptionalAttrDef("attr2", AtlasBusinessMetadataDef.ATLAS_TYPE_BYTE, options, _description),
                createOptionalAttrDef("attr3", AtlasBusinessMetadataDef.ATLAS_TYPE_SHORT, options, _description),
                createOptionalAttrDef("attr4", AtlasBusinessMetadataDef.ATLAS_TYPE_INT, options, _description),
                createOptionalAttrDef("attr5", AtlasBusinessMetadataDef.ATLAS_TYPE_LONG, options, _description),
                createOptionalAttrDef("attr6", AtlasBusinessMetadataDef.ATLAS_TYPE_FLOAT, options, _description),
                createOptionalAttrDef("attr7", AtlasBusinessMetadataDef.ATLAS_TYPE_DOUBLE, options, _description),
                createOptionalAttrDef("attr8", AtlasBusinessMetadataDef.ATLAS_TYPE_STRING, options, _description),
                createOptionalAttrDef("attr9", AtlasBusinessMetadataDef.ATLAS_TYPE_DATE, options, _description),
                createOptionalAttrDef("attr10", "ENUM_1", options, _description));

        AtlasBusinessMetadataDef bmWithAllTypesMV = createBusinessMetadataDef("bmWithAllTypesMV", _description, "1.0",
                createOptionalAttrDef("attr11", "array<boolean>", options, _description),
                createOptionalAttrDef("attr12", "array<byte>", options, _description),
                createOptionalAttrDef("attr13", "array<short>", options, _description),
                createOptionalAttrDef("attr14", "array<int>", options, _description),
                createOptionalAttrDef("attr15", "array<long>", options, _description),
                createOptionalAttrDef("attr16", "array<float>", options, _description),
                createOptionalAttrDef("attr17", "array<double>", options, _description),
                createOptionalAttrDef("attr18", "array<string>", options, _description),
                createOptionalAttrDef("attr19", "array<date>", options, _description),
                createOptionalAttrDef("attr20", "array<ENUM_1>", options, _description));

        options.put("applicableEntityTypes", "[\"" + SUPER_TYPE_NAME + "\"]");

        AtlasBusinessMetadataDef bmWithSuperType = createBusinessMetadataDef("bmWithSuperType", _description,"1.0",
                createOptionalAttrDef("attrSuperBoolean", AtlasBusinessMetadataDef.ATLAS_TYPE_BOOLEAN, options, _description ),
                createOptionalAttrDef("attrSuperString", AtlasBusinessMetadataDef.ATLAS_TYPE_STRING, options, _description ));

        AtlasTypesDef ret = AtlasTypeUtil.getTypesDef(new ArrayList<>(),
                new ArrayList<>(), new ArrayList<>(),
                new ArrayList<>(), new ArrayList<>(),
                Arrays.asList(bmNoApplicableTypes, bmNoAttributes, bmWithAllTypes, bmWithAllTypesMV, bmWithSuperType));

        populateSystemAttributes(ret);

        return ret;
    }

    public static AtlasTypesDef defineHiveTypes() {
        String _description = "_description";
        AtlasEntityDef superTypeDefinition =
                createClassTypeDef(SUPER_TYPE_NAME, "SuperType_description", Collections.<String>emptySet(),
                        createOptionalAttrDef("namespace", "string"),
                        createOptionalAttrDef("cluster", "string"),
                        createOptionalAttrDef("colo", "string"));
        AtlasEntityDef databaseTypeDefinition =
                createClassTypeDef(DATABASE_TYPE, DATABASE_TYPE + _description,Collections.singleton(SUPER_TYPE_NAME),
                        createUniqueRequiredAttrDef(NAME, "string"),
                        createOptionalAttrDef("isReplicated", "boolean"),
                        createOptionalAttrDef("created", "string"),
                        createOptionalAttrDef("parameters", "map<string,string>"),
                        createRequiredAttrDef("description", "string"));


        AtlasStructDef structTypeDefinition = new AtlasStructDef("serdeType", "serdeType" + _description, "1.0",
                Arrays.asList(
                        createRequiredAttrDef("name", "string"),
                        createRequiredAttrDef("serde", "string"),
                        createOptionalAttrDef("description", "string")));

        AtlasEnumElementDef values[] = {
                new AtlasEnumElementDef("MANAGED", "Element Description", 1),
                new AtlasEnumElementDef("EXTERNAL", "Element Description", 2)};

        AtlasEnumDef enumTypeDefinition = new AtlasEnumDef("tableType", "tableType" + _description, "1.0", Arrays.asList(values));

        AtlasEntityDef columnsDefinition =
                createClassTypeDef(COLUMN_TYPE, COLUMN_TYPE + "_description",
                        Collections.<String>emptySet(),
                        createUniqueRequiredAttrDef("name", "string"),
                        createRequiredAttrDef("type", "string"),
                        createOptionalAttrDef("description", "string"),
                        new AtlasAttributeDef("table", TABLE_TYPE,
                        true,
                        SINGLE, 0, 1,
                        false, false, false,
                        new ArrayList<AtlasStructDef.AtlasConstraintDef>() {{
                            add(new AtlasStructDef.AtlasConstraintDef(
                                CONSTRAINT_TYPE_INVERSE_REF, new HashMap<String, Object>() {{
                                put(CONSTRAINT_PARAM_ATTRIBUTE, "columns");
                            }}));
                        }})
                        );

        AtlasStructDef partitionDefinition = new AtlasStructDef("partition_struct_type", "partition_struct_type" + _description, "1.0",
                Arrays.asList(createRequiredAttrDef("name", "string")));

        AtlasAttributeDef[] attributeDefinitions = new AtlasAttributeDef[]{
                new AtlasAttributeDef("location", "string", true,
                        SINGLE, 0, 1,
                        false, false, false,
                        Collections.<AtlasConstraintDef>emptyList()),
                new AtlasAttributeDef("inputFormat", "string", true,
                        SINGLE, 0, 1,
                        false, false, false,
                        Collections.<AtlasConstraintDef>emptyList()),
                new AtlasAttributeDef("outputFormat", "string", true,
                        SINGLE, 0, 1,
                        false, false, false,
                        Collections.<AtlasConstraintDef>emptyList()),
                new AtlasAttributeDef("compressed", "boolean", false,
                        SINGLE, 1, 1,
                        false, false, false,
                        Collections.<AtlasConstraintDef>emptyList()),
                new AtlasAttributeDef("numBuckets", "int", true,
                        SINGLE, 0, 1,
                        false, false, false,
                        Collections.<AtlasConstraintDef>emptyList()),
        };

        AtlasEntityDef storageDescClsDef =
                new AtlasEntityDef(STORAGE_DESC_TYPE, STORAGE_DESC_TYPE + _description, "1.0",
                        Arrays.asList(attributeDefinitions), Collections.singleton(SUPER_TYPE_NAME));

        AtlasAttributeDef[] partClsAttributes = new AtlasAttributeDef[]{
                new AtlasAttributeDef("values", "array<string>",
                        true,
                        SINGLE, 0, 1,
                        false, false, false,
                        Collections.<AtlasConstraintDef>emptyList()),
                new AtlasAttributeDef("table", TABLE_TYPE, true,
                        SINGLE, 0, 1,
                        false, false, false,
                        Collections.<AtlasConstraintDef>emptyList()),
                new AtlasAttributeDef("createTime", "long", true,
                        SINGLE, 0, 1,
                        false, false, false,
                        Collections.<AtlasConstraintDef>emptyList()),
                new AtlasAttributeDef("lastAccessTime", "long", true,
                        SINGLE, 0, 1,
                        false, false, false,
                        Collections.<AtlasConstraintDef>emptyList()),
                new AtlasAttributeDef("sd", STORAGE_DESC_TYPE, false,
                        SINGLE, 1, 1,
                        false, false, false,
                        Collections.<AtlasConstraintDef>emptyList()),
                new AtlasAttributeDef("columns", String.format("array<%s>", COLUMN_TYPE),
                        true,
                        SINGLE, 0, 1,
                        false, false, false,
                        Collections.<AtlasConstraintDef>emptyList()),
                new AtlasAttributeDef("parameters", String.format("map<%s,%s>", "string", "string"), true,
                        SINGLE, 0, 1,
                        false, false, false,
                        Collections.<AtlasConstraintDef>emptyList())};

        AtlasEntityDef partClsDef =
                new AtlasEntityDef("partition_class_type", "partition_class_type" + _description, "1.0",
                        Arrays.asList(partClsAttributes), Collections.singleton(SUPER_TYPE_NAME));

        AtlasEntityDef processClsType =
                new AtlasEntityDef(PROCESS_TYPE, PROCESS_TYPE + _description, "1.0",
                        Arrays.asList(new AtlasAttributeDef("outputs", "array<" + TABLE_TYPE + ">", true,
                                SINGLE, 0, 1,
                                false, false, false,
                                Collections.<AtlasConstraintDef>emptyList())),
                        Collections.<String>emptySet());

        AtlasEntityDef tableTypeDefinition =
                createClassTypeDef(TABLE_TYPE, TABLE_TYPE + _description, Collections.singleton(SUPER_TYPE_NAME),
                        createUniqueRequiredAttrDef("name", "string"),
                        createOptionalAttrDef("description", "string"),
                        createRequiredAttrDef("type", "string"),
                        createOptionalAttrDef("created", "string"),
                        // enum
                        new AtlasAttributeDef("tableType", "tableType", false,
                                SINGLE, 1, 1,
                                false, false, false,
                                Collections.<AtlasConstraintDef>emptyList()),
                        // array of strings
                        new AtlasAttributeDef("columnNames",
                                String.format("array<%s>", "string"), true,
                                SINGLE, 0, 1,
                                false, false, false,
                                Collections.<AtlasConstraintDef>emptyList()),
                        // array of classes
                        new AtlasAttributeDef("columns", String.format("array<%s>", COLUMN_TYPE),
                                true,
                                SINGLE, 0, 1,
                                false, false, false,
                                new ArrayList<AtlasStructDef.AtlasConstraintDef>() {{
                                    add(new AtlasStructDef.AtlasConstraintDef(CONSTRAINT_TYPE_OWNED_REF));
                                }}),
                        // array of structs
                        new AtlasAttributeDef("partitions", String.format("array<%s>", "partition_struct_type"),
                                true,
                                SINGLE, 0, 1,
                                false, false, false,
                                Collections.<AtlasConstraintDef>emptyList()),
                        // map of structs
                        new AtlasAttributeDef("partitionsMap", String.format("map<%s,%s>", "string", "partition_struct_type"),
                                true,
                                SINGLE, 0, 1,
                                false, false, false,
                                Collections.<AtlasConstraintDef>emptyList()),
                        // map of primitives
                        new AtlasAttributeDef("parametersMap", String.format("map<%s,%s>", "string", "string"),
                                true,
                                SINGLE, 0, 1,
                                false, false, false,
                                Collections.<AtlasConstraintDef>emptyList()),
                        //map of classes -
                        new AtlasAttributeDef(COLUMNS_MAP,
                                String.format("map<%s,%s>", "string", COLUMN_TYPE),
                                true,
                                SINGLE, 0, 1,
                                false, false, false,
                                 new ArrayList<AtlasStructDef.AtlasConstraintDef>() {{
                                     add(new AtlasStructDef.AtlasConstraintDef(
                                             CONSTRAINT_TYPE_OWNED_REF));
                                     }}
                                ),
                        //map of structs
                        new AtlasAttributeDef("partitionsMap",
                                String.format("map<%s,%s>", "string", "partition_struct_type"),
                                true,
                                SINGLE, 0, 1,
                                false, false, false,
                                Collections.<AtlasConstraintDef>emptyList()),
                        // struct reference
                        new AtlasAttributeDef("serde1", "serdeType", true,
                                SINGLE, 0, 1,
                                false, false, false,
                                Collections.<AtlasConstraintDef>emptyList()),
                        new AtlasAttributeDef("serde2", "serdeType", true,
                                SINGLE, 0, 1,
                                false, false, false,
                                Collections.<AtlasConstraintDef>emptyList()),
                        // class reference
                        new AtlasAttributeDef("database", DATABASE_TYPE, false,
                                SINGLE, 1, 1,
                                false, false, false,
                                Collections.<AtlasConstraintDef>emptyList()),
                        //class reference as composite
                        new AtlasAttributeDef("databaseComposite", DATABASE_TYPE, true,
                                SINGLE, 0, 1,
                                false, false, false,
                                new ArrayList<AtlasStructDef.AtlasConstraintDef>() {{
                                    add(new AtlasStructDef.AtlasConstraintDef(
                                            CONSTRAINT_TYPE_OWNED_REF));
                                }}
                        ));

        AtlasClassificationDef piiTypeDefinition =
                AtlasTypeUtil.createTraitTypeDef(PII, PII + _description, Collections.<String>emptySet());

        AtlasClassificationDef classificationTypeDefinition =
                AtlasTypeUtil.createTraitTypeDef(CLASSIFICATION, CLASSIFICATION + _description, Collections.<String>emptySet(),
                        createRequiredAttrDef("tag", "string"));

        AtlasClassificationDef fetlClassificationTypeDefinition =
                AtlasTypeUtil.createTraitTypeDef("fetl" + CLASSIFICATION, "fetl" + CLASSIFICATION + _description, Collections.singleton(CLASSIFICATION));

        AtlasClassificationDef phiTypeDefinition = AtlasTypeUtil.createTraitTypeDef(PHI, PHI + _description, Collections.<String>emptySet(),
                                                                                    createRequiredAttrDef("stringAttr", "string"),
                                                                                    createRequiredAttrDef("booleanAttr", "boolean"),
                                                                                    createRequiredAttrDef("integerAttr", "int"));

        AtlasTypesDef ret = AtlasTypeUtil.getTypesDef(Collections.singletonList(enumTypeDefinition),
                                                      Arrays.asList(structTypeDefinition, partitionDefinition),
                                                      Arrays.asList(classificationTypeDefinition, fetlClassificationTypeDefinition, piiTypeDefinition, phiTypeDefinition),
                                                      Arrays.asList(superTypeDefinition, databaseTypeDefinition, columnsDefinition, tableTypeDefinition, storageDescClsDef, partClsDef, processClsType));

        populateSystemAttributes(ret);

        return ret;
    }

    public static AtlasTypesDef defineTypeWithNestedCollectionAttributes() {
        AtlasEntityDef nestedCollectionAttributesEntityType =
                createClassTypeDef(ENTITY_TYPE_WITH_NESTED_COLLECTION_ATTR, ENTITY_TYPE_WITH_NESTED_COLLECTION_ATTR + "_description", null,
                        createUniqueRequiredAttrDef("name", "string"),

                        new AtlasAttributeDef("mapOfArrayOfStrings", "map<string,array<string>>", false,
                                SINGLE, 1, 1,
                                false, false, false,
                                Collections.<AtlasConstraintDef>emptyList()),
                        new AtlasAttributeDef("mapOfArrayOfBooleans", "map<string,array<boolean>>", false,
                                SINGLE, 1, 1,
                                false, false, false,
                                Collections.<AtlasConstraintDef>emptyList()),
                        new AtlasAttributeDef("mapOfArrayOfInts", "map<string,array<int>>", false,
                                SINGLE, 1, 1,
                                false, false, false,
                                Collections.<AtlasConstraintDef>emptyList()),
                        new AtlasAttributeDef("mapOfArrayOfFloats", "map<string,array<float>>", false,
                                SINGLE, 1, 1,
                                false, false, false,
                                Collections.<AtlasConstraintDef>emptyList()),
                        new AtlasAttributeDef("mapOfArrayOfDates", "map<string,array<date>>", false,
                                SINGLE, 1, 1,
                                false, false, false,
                                Collections.<AtlasConstraintDef>emptyList()),

                        new AtlasAttributeDef("mapOfMapOfStrings", "map<string,map<string,string>>", false,
                                SINGLE, 1, 1,
                                false, false, false,
                                Collections.<AtlasConstraintDef>emptyList()),
                        new AtlasAttributeDef("mapOfMapOfBooleans", "map<string,map<string,boolean>>", false,
                                SINGLE, 1, 1,
                                false, false, false,
                                Collections.<AtlasConstraintDef>emptyList()),
                        new AtlasAttributeDef("mapOfMapOfInts", "map<string,map<string,int>>", false,
                                SINGLE, 1, 1,
                                false, false, false,
                                Collections.<AtlasConstraintDef>emptyList()),
                        new AtlasAttributeDef("mapOfMapOfFloats", "map<string,map<string,float>>", false,
                                SINGLE, 1, 1,
                                false, false, false,
                                Collections.<AtlasConstraintDef>emptyList()),
                        new AtlasAttributeDef("mapOfMapOfDates", "map<string,map<string,date>>", false,
                                SINGLE, 1, 1,
                                false, false, false,
                                Collections.<AtlasConstraintDef>emptyList()),

                        new AtlasAttributeDef("arrayOfArrayOfStrings", "array<array<string>>", false,
                                SINGLE, 1, 1,
                                false, false, false,
                                Collections.<AtlasConstraintDef>emptyList()),
                        new AtlasAttributeDef("arrayOfArrayOfBooleans", "array<array<boolean>>", false,
                                SINGLE, 1, 1,
                                false, false, false,
                                Collections.<AtlasConstraintDef>emptyList()),
                        new AtlasAttributeDef("arrayOfArrayOfInts", "array<array<int>>", false,
                                SINGLE, 1, 1,
                                false, false, false,
                                Collections.<AtlasConstraintDef>emptyList()),
                        new AtlasAttributeDef("arrayOfArrayOfFloats", "array<array<float>>", false,
                                SINGLE, 1, 1,
                                false, false, false,
                                Collections.<AtlasConstraintDef>emptyList()),
                        new AtlasAttributeDef("arrayOfArrayOfDates", "array<array<date>>", false,
                                SINGLE, 1, 1,
                                false, false, false,
                                Collections.<AtlasConstraintDef>emptyList()),

                        new AtlasAttributeDef("arrayOfMapOfStrings", "array<map<string,string>>", false,
                                SINGLE, 1, 1,
                                false, false, false,
                                Collections.<AtlasConstraintDef>emptyList()),
                        new AtlasAttributeDef("arrayOfMapOfBooleans", "array<map<string,boolean>>", false,
                                SINGLE, 1, 1,
                                false, false, false,
                                Collections.<AtlasConstraintDef>emptyList()),
                        new AtlasAttributeDef("arrayOfMapOfInts", "array<map<string,int>>", false,
                                SINGLE, 1, 1,
                                false, false, false,
                                Collections.<AtlasConstraintDef>emptyList()),
                        new AtlasAttributeDef("arrayOfMapOfFloats", "array<map<string,float>>", false,
                                SINGLE, 1, 1,
                                false, false, false,
                                Collections.<AtlasConstraintDef>emptyList()),
                        new AtlasAttributeDef("arrayOfMapOfDates", "array<map<string,date>>", false,
                                SINGLE, 1, 1,
                                false, false, false,
                                Collections.<AtlasConstraintDef>emptyList())
                );

        AtlasTypesDef ret = AtlasTypeUtil.getTypesDef(Collections.emptyList(),
                                                      Collections.emptyList(),
                                                      Collections.emptyList(),
                                                      Arrays.asList(nestedCollectionAttributesEntityType));

        populateSystemAttributes(ret);

        return ret;
    }

    public static AtlasTypesDef defineTypeWithComplexCollectionAttributes() {
        AtlasStructDef structType =
                new AtlasStructDef(STRUCT_TYPE, "struct_type_description", "1.0", Arrays.asList(createRequiredAttrDef(NAME, "string")));

        AtlasEntityDef entityType =
                createClassTypeDef(ENTITY_TYPE, "entity_type_description", Collections.emptySet(), createUniqueRequiredAttrDef(NAME, "string"), createOptionalAttrDef("isReplicated", "boolean"));

        AtlasEntityDef complexCollectionEntityType =
                createClassTypeDef(ENTITY_TYPE_WITH_COMPLEX_COLLECTION_ATTR, ENTITY_TYPE_WITH_COMPLEX_COLLECTION_ATTR + "_description", null,
                        createUniqueRequiredAttrDef("name", "string"),
                        new AtlasAttributeDef("listOfStructs", String.format("array<%s>", STRUCT_TYPE), true, LIST, 1, 1, false, false, false, Collections.emptyList()),
                        new AtlasAttributeDef("listOfEntities", String.format("array<%s>", ENTITY_TYPE), true, LIST, 1, 1, false, false, false, new ArrayList<AtlasConstraintDef>() {{ add(new AtlasConstraintDef(CONSTRAINT_TYPE_OWNED_REF)); }}),
                        new AtlasAttributeDef("mapOfStructs", String.format("map<%s,%s>", "string", STRUCT_TYPE), true, SINGLE, 1, 1, false, false, false, Collections.emptyList()),
                        new AtlasAttributeDef("mapOfEntities", String.format("map<%s,%s>", "string", ENTITY_TYPE), true, SINGLE, 1, 1, false, false, false, new ArrayList<AtlasConstraintDef>() {{ add(new AtlasConstraintDef(CONSTRAINT_TYPE_OWNED_REF)); }}) );

        AtlasTypesDef ret = AtlasTypeUtil.getTypesDef(Collections.emptyList(),
                                                      Arrays.asList(structType),
                                                      Collections.emptyList(),
                                                      Arrays.asList(entityType, complexCollectionEntityType));

        populateSystemAttributes(ret);

        return ret;
    }

    public static AtlasTypesDef defineTypeWithMapAttributes() {
        AtlasEntityDef entityType = createClassTypeDef(ENTITY_TYPE_MAP, "entity_type_map_description", Collections.emptySet(),
                                        createUniqueRequiredAttrDef("mapAttr1", "map<string,string>"),
                                        createUniqueRequiredAttrDef("mapAttr2", "map<string,int>"),
                                        createUniqueRequiredAttrDef("mapAttr3", "map<string,boolean>"),
                                              createOptionalAttrDef("mapAttr4", "map<string,float>"),
                                              createOptionalAttrDef("mapAttr5", "map<string,date>"));

        AtlasTypesDef ret = AtlasTypeUtil.getTypesDef(Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), Arrays.asList(entityType));

        populateSystemAttributes(ret);

        return ret;
    }

    public static AtlasEntityWithExtInfo createNestedCollectionAttrEntity() {
        AtlasEntity entity = new AtlasEntity(ENTITY_TYPE_WITH_NESTED_COLLECTION_ATTR);

        String[]  arrayOfStrings  = new String[] { "one", "two", "three" };
        boolean[] arrayOfBooleans = new boolean[] { false, true };
        int[]     arrayOfInts     = new int[] { 1, 2, 3 };
        float[]   arrayOfFloats   = new float[] { 1.1f, 2.2f, 3.3f };
        Date[]    arrayOfDates    = new Date[] { new Date() };

        Map<String, String>  mapOfStrings  = Collections.singletonMap("one", "one");
        Map<String, Boolean> mapOfBooleans = Collections.singletonMap("one", true);
        Map<String, Integer> mapOfInts     = Collections.singletonMap("one", 1);
        Map<String, Float>   mapOfFloats   = Collections.singletonMap("one", 1.1f);
        Map<String, Date>    mapOfDates    = Collections.singletonMap("now", new Date());

        entity.setAttribute("name", randomString() + "_" + System.currentTimeMillis());

        entity.setAttribute("mapOfArrayOfStrings", Collections.singletonMap("one", arrayOfStrings));
        entity.setAttribute("mapOfArrayOfBooleans", Collections.singletonMap("one", arrayOfBooleans));
        entity.setAttribute("mapOfArrayOfInts", Collections.singletonMap("one", arrayOfInts));
        entity.setAttribute("mapOfArrayOfFloats", Collections.singletonMap("one", arrayOfFloats));
        entity.setAttribute("mapOfArrayOfDates", Collections.singletonMap("one", arrayOfDates));

        entity.setAttribute("mapOfMapOfStrings", Collections.singletonMap("one", mapOfStrings));
        entity.setAttribute("mapOfMapOfBooleans", Collections.singletonMap("one", mapOfBooleans));
        entity.setAttribute("mapOfMapOfInts", Collections.singletonMap("one", mapOfInts));
        entity.setAttribute("mapOfMapOfFloats", Collections.singletonMap("one", mapOfFloats));
        entity.setAttribute("mapOfMapOfDates", Collections.singletonMap("one", mapOfDates));

        entity.setAttribute("arrayOfArrayOfStrings", Collections.singletonList(arrayOfStrings));
        entity.setAttribute("arrayOfArrayOfBooleans", Collections.singletonList(arrayOfBooleans));
        entity.setAttribute("arrayOfArrayOfInts", Collections.singletonList(arrayOfInts));
        entity.setAttribute("arrayOfArrayOfFloats", Collections.singletonList(arrayOfFloats));
        entity.setAttribute("arrayOfArrayOfDates", Collections.singletonList(arrayOfDates));

        entity.setAttribute("arrayOfMapOfStrings", Collections.singletonList(mapOfStrings));
        entity.setAttribute("arrayOfMapOfBooleans", Collections.singletonList(mapOfBooleans));
        entity.setAttribute("arrayOfMapOfInts", Collections.singletonList(mapOfInts));
        entity.setAttribute("arrayOfMapOfFloats", Collections.singletonList(mapOfFloats));
        entity.setAttribute("arrayOfMapOfDates", Collections.singletonList(mapOfDates));

        return new AtlasEntityWithExtInfo(entity);
    }

    public static AtlasEntityWithExtInfo createComplexCollectionAttrEntity() {
        AtlasEntity entity = new AtlasEntity(ENTITY_TYPE_WITH_COMPLEX_COLLECTION_ATTR);

        entity.setAttribute(NAME, ENTITY_TYPE_WITH_COMPLEX_COLLECTION_ATTR);

        entity.setAttribute("listOfStructs", Arrays.asList(new AtlasStruct("struct_type", "name", "structArray0"),
                                                           new AtlasStruct("struct_type", "name", "structArray1"),
                                                           new AtlasStruct("struct_type", "name", "structArray2")));

        entity.setAttribute("mapOfStructs", new HashMap<String, AtlasStruct>() {{ put("key0", new AtlasStruct("struct_type", "name", "structMap0"));
                                                                                  put("key1", new AtlasStruct("struct_type", "name", "structMap1"));
                                                                                  put("key2", new AtlasStruct("struct_type", "name", "structMap2")); }});

        AtlasEntity e1Array = new AtlasEntity(ENTITY_TYPE, new HashMap<String, Object>() {{ put(NAME, "entityArray0"); put("isReplicated", true); }});
        AtlasEntity e2Array = new AtlasEntity(ENTITY_TYPE, new HashMap<String, Object>() {{ put(NAME, "entityArray1"); put("isReplicated", false); }});
        AtlasEntity e3Array = new AtlasEntity(ENTITY_TYPE, new HashMap<String, Object>() {{ put(NAME, "entityArray2"); put("isReplicated", true); }});

        entity.setAttribute("listOfEntities", Arrays.asList(getAtlasObjectId(e1Array), getAtlasObjectId(e2Array), getAtlasObjectId(e3Array)));

        AtlasEntity e1MapValue = new AtlasEntity(ENTITY_TYPE, new HashMap<String, Object>() {{ put(NAME, "entityMapValue0"); put("isReplicated", false); }});
        AtlasEntity e2MapValue = new AtlasEntity(ENTITY_TYPE, new HashMap<String, Object>() {{ put(NAME, "entityMapValue1"); put("isReplicated", true); }});
        AtlasEntity e3MapValue = new AtlasEntity(ENTITY_TYPE, new HashMap<String, Object>() {{ put(NAME, "entityMapValue2"); put("isReplicated", false); }});

        entity.setAttribute("mapOfEntities", new HashMap<String, Object>() {{ put("key0", getAtlasObjectId(e1MapValue));
                                                                              put("key1", getAtlasObjectId(e2MapValue));
                                                                              put("key2", getAtlasObjectId(e3MapValue)); }});
        AtlasEntityWithExtInfo ret = new AtlasEntityWithExtInfo(entity);

        ret.addReferredEntity(e1Array);
        ret.addReferredEntity(e2Array);
        ret.addReferredEntity(e3Array);
        ret.addReferredEntity(e1MapValue);
        ret.addReferredEntity(e2MapValue);
        ret.addReferredEntity(e3MapValue);

        return ret;
    }

    public static AtlasEntityWithExtInfo createMapAttrEntity() {
        AtlasEntity entity = new AtlasEntity(ENTITY_TYPE_MAP);

        Map<String, String> map1 = new HashMap<>();
        map1.put("map1Key1", "value1");
        map1.put("map1Key2", "value2");
        map1.put("map1Key3", "value3");

        Map<String, Integer> map2 = new HashMap<>();
        map2.put("map2Key1", 100);
        map2.put("map2Key2", 200);
        map2.put("map2Key3", 300);

        Map<String, Boolean> map3 = new HashMap<>();
        map3.put("map3Key1", false);
        map3.put("map3Key2", true);
        map3.put("map3Key3", false);

        Map<String, Float> map4 = new HashMap<>();
        map4.put("map4Key1", 1.0f);
        map4.put("map4Key2", 2.0f);
        map4.put("map4Key3", 3.0f);

        Map<String, Date> map5 = new HashMap<>();
        map5.put("map5Key1", new Date());
        map5.put("map5Key2", new Date());
        map5.put("map5Key3", new Date());

        entity.setAttribute("mapAttr1", map1);
        entity.setAttribute("mapAttr2", map2);
        entity.setAttribute("mapAttr3", map3);
        entity.setAttribute("mapAttr4", map4);
        entity.setAttribute("mapAttr5", map5);

        AtlasEntityWithExtInfo ret = new AtlasEntityWithExtInfo(entity);

        return ret;
    }

    public static final String randomString() {
        return RandomStringUtils.randomAlphanumeric(10);
    }

    public static final String randomString(int count) {
        return RandomStringUtils.randomAlphanumeric(count);
    }

    public static AtlasEntity createDBEntity() {
        String dbName = RandomStringUtils.randomAlphanumeric(10);
        return createDBEntity(dbName);
    }

    public static AtlasEntity createDBEntity(String dbName) {
        AtlasEntity entity = new AtlasEntity(DATABASE_TYPE);
        entity.setAttribute(NAME, dbName);
        entity.setAttribute("description", "us db");

        return entity;
    }

    public static AtlasEntityWithExtInfo createDBEntityV2() {
        AtlasEntity dbEntity = new AtlasEntity(DATABASE_TYPE);

        dbEntity.setAttribute(NAME, RandomStringUtils.randomAlphanumeric(10));
        dbEntity.setAttribute("description", "us db");

        return new AtlasEntityWithExtInfo(dbEntity);
    }

    public static AtlasEntity createTableEntity(AtlasEntity dbEntity) {
        String tableName = RandomStringUtils.randomAlphanumeric(10);
        return createTableEntity(dbEntity, tableName);
    }

    public static AtlasEntity createTableEntity(AtlasEntity dbEntity, String name) {
        AtlasEntity entity = new AtlasEntity(TABLE_TYPE);
        entity.setAttribute(NAME, name);
        entity.setAttribute("description", "random table");
        entity.setAttribute("type", "type");
        entity.setAttribute("tableType", "MANAGED");
        entity.setAttribute("database", getAtlasObjectId(dbEntity));
        entity.setAttribute("created", new Date());

        Map<String, Object> partAttributes = new HashMap<String, Object>() {{
            put("name", "part0");
        }};
        final AtlasStruct partitionStruct  = new AtlasStruct("partition_struct_type", partAttributes);

        entity.setAttribute("partitions", new ArrayList<AtlasStruct>() {{ add(partitionStruct); }});
        entity.setAttribute("parametersMap", new java.util.HashMap<String, String>() {{
            put("key1", "value1");
        }});

        return entity;
    }

    public static AtlasEntityWithExtInfo createTableEntityV2(AtlasEntity dbEntity) {
        AtlasEntity tblEntity = new AtlasEntity(TABLE_TYPE);

        tblEntity.setAttribute(NAME, RandomStringUtils.randomAlphanumeric(10));
        tblEntity.setAttribute("description", "random table");
        tblEntity.setAttribute("type", "type");
        tblEntity.setAttribute("tableType", "MANAGED");
        tblEntity.setAttribute("database", getAtlasObjectId(dbEntity));
        tblEntity.setAttribute("created", new Date());

        final AtlasStruct partitionStruct = new AtlasStruct("partition_struct_type", "name", "part0");

        tblEntity.setAttribute("partitions", new ArrayList<AtlasStruct>() {{ add(partitionStruct); }});

        tblEntity.setAttribute("partitionsMap", new HashMap<String, AtlasStruct>() {{
            put("part0", new AtlasStruct("partition_struct_type", "name", "part0"));
            put("part1", new AtlasStruct("partition_struct_type", "name", "part1"));
            put("part2", new AtlasStruct("partition_struct_type", "name", "part2"));
        }});

        tblEntity.setAttribute("parametersMap",
                new java.util.HashMap<String, String>() {{ put("key1", "value1"); }});


        AtlasEntityWithExtInfo ret = new AtlasEntityWithExtInfo(tblEntity);

        ret.addReferredEntity(dbEntity);

        return ret;
    }

    public static AtlasEntityWithExtInfo createprimitiveEntityV2() {

        AtlasEntity defaultprimitive = new AtlasEntity(createPrimitiveEntityDef());
        defaultprimitive.setAttribute("name", "testname");
        defaultprimitive.setAttribute("description","test");
        defaultprimitive.setAttribute("check","check");

        AtlasEntityWithExtInfo ret = new AtlasEntityWithExtInfo(defaultprimitive);

        return ret;

    }


    public static AtlasEntityDef createPrimitiveEntityDef() {

        AtlasEntityDef newtestEntityDef = new AtlasEntityDef("newtest");
        AtlasAttributeDef attrName = new AtlasAttributeDef("name", AtlasBaseTypeDef.ATLAS_TYPE_STRING);

        AtlasAttributeDef attrDescription = new AtlasAttributeDef("description", AtlasBaseTypeDef.ATLAS_TYPE_STRING);
        attrDescription.setIsOptional(false);

        AtlasAttributeDef attrcheck = new AtlasAttributeDef("check", AtlasBaseTypeDef.ATLAS_TYPE_STRING);
        attrcheck.setIsOptional(true);

        AtlasAttributeDef attrSourceCode = new AtlasAttributeDef("sourcecode", AtlasBaseTypeDef.ATLAS_TYPE_STRING);
        attrSourceCode.setDefaultValue("Hello World");
        attrSourceCode.setIsOptional(true);

        AtlasAttributeDef attrCost = new AtlasAttributeDef("Cost", AtlasBaseTypeDef.ATLAS_TYPE_INT);
        attrCost.setIsOptional(true);
        attrCost.setDefaultValue("30");

        AtlasAttributeDef attrDiskUsage = new AtlasAttributeDef("diskUsage", AtlasBaseTypeDef.ATLAS_TYPE_FLOAT);
        attrDiskUsage.setIsOptional(true);
        attrDiskUsage.setDefaultValue("70.50");

        AtlasAttributeDef attrisStoreUse = new AtlasAttributeDef("isstoreUse", AtlasBaseTypeDef.ATLAS_TYPE_BOOLEAN);
        attrisStoreUse.setIsOptional(true);
        attrisStoreUse.setDefaultValue("true");

        newtestEntityDef.addAttribute(attrName);
        newtestEntityDef.addAttribute(attrDescription);
        newtestEntityDef.addAttribute(attrcheck);
        newtestEntityDef.addAttribute(attrSourceCode);
        newtestEntityDef.addAttribute(attrCost);
        newtestEntityDef.addAttribute(attrDiskUsage);
        newtestEntityDef.addAttribute(attrisStoreUse);

        populateSystemAttributes(newtestEntityDef);

        return newtestEntityDef;
    }

    public static AtlasEntityWithExtInfo createTableEntityDuplicatesV2(AtlasEntity dbEntity) {
        AtlasEntity tblEntity = new AtlasEntity(TABLE_TYPE);

        tblEntity.setAttribute(NAME, RandomStringUtils.randomAlphanumeric(10));
        tblEntity.setAttribute("description", "random table");
        tblEntity.setAttribute("type", "type");
        tblEntity.setAttribute("tableType", "MANAGED");
        tblEntity.setAttribute("database", AtlasTypeUtil.getAtlasObjectId(dbEntity));

        AtlasEntity col1 = createColumnEntity(tblEntity);
        col1.setAttribute(NAME, "col1");

        AtlasEntity col2 = createColumnEntity(tblEntity);
        col2.setAttribute(NAME, "col1");

        AtlasEntity col3 = createColumnEntity(tblEntity);
        col3.setAttribute(NAME, "col1");

        // all 3 columns have different guid but same typeName and unique attributes
        tblEntity.setAttribute(COLUMNS_ATTR_NAME, Arrays.asList(AtlasTypeUtil.getAtlasObjectId(col1),
                AtlasTypeUtil.getAtlasObjectId(col2),
                AtlasTypeUtil.getAtlasObjectId(col3)));

        AtlasEntityWithExtInfo ret = new AtlasEntityWithExtInfo(tblEntity);

        ret.addReferredEntity(dbEntity);
        ret.addReferredEntity(col1);
        ret.addReferredEntity(col2);
        ret.addReferredEntity(col3);

        return ret;
    }

    public static AtlasEntity createColumnEntity(AtlasEntity tableEntity) {
        return createColumnEntity(tableEntity, "col" + seq.addAndGet(1));
    }

    public static AtlasEntity createColumnEntity(AtlasEntity tableEntity, String colName) {
        AtlasEntity entity = new AtlasEntity(COLUMN_TYPE);
        entity.setAttribute(NAME, colName);
        entity.setAttribute("type", "VARCHAR(32)");
        entity.setAttribute("table", getAtlasObjectId(tableEntity));
        return entity;
    }

    public static AtlasEntity createProcessEntity(List<AtlasObjectId> inputs, List<AtlasObjectId> outputs) {

        AtlasEntity entity = new AtlasEntity(PROCESS_TYPE);
        entity.setAttribute(NAME, RandomStringUtils.randomAlphanumeric(10));
        entity.setAttribute("inputs", inputs);
        entity.setAttribute("outputs", outputs);
        return entity;
    }

    public static List<AtlasClassificationDef> getClassificationWithValidSuperType() {
        AtlasClassificationDef securityClearanceTypeDef =
                AtlasTypeUtil.createTraitTypeDef("SecurityClearance1", "SecurityClearance_description", Collections.<String>emptySet(),
                        createRequiredAttrDef("level", "int"));

        AtlasClassificationDef janitorSecurityClearanceTypeDef =
                AtlasTypeUtil.createTraitTypeDef("JanitorClearance", "JanitorClearance_description", Collections.singleton("SecurityClearance1"));

        List<AtlasClassificationDef> ret = Arrays.asList(securityClearanceTypeDef, janitorSecurityClearanceTypeDef);

        populateSystemAttributes(ret);

        return ret;
    }

    public static List<AtlasClassificationDef> getClassificationWithName(String name) {
        AtlasClassificationDef classificationTypeDef =
                AtlasTypeUtil.createTraitTypeDef(name, "s_description", Collections.<String>emptySet(),
                        createRequiredAttrDef("level", "int"));


        List<AtlasClassificationDef> ret = Arrays.asList(classificationTypeDef);

        populateSystemAttributes(ret);

        return ret;
    }

    public static AtlasClassificationDef getSingleClassificationWithName(String name) {
        AtlasClassificationDef classificaitonTypeDef =
                AtlasTypeUtil.createTraitTypeDef(name, "s_description", Collections.<String>emptySet(),
                        createRequiredAttrDef("level", "int"));

        populateSystemAttributes(classificaitonTypeDef);

        return classificaitonTypeDef;
    }

    public static List<AtlasClassificationDef> getClassificationWithValidAttribute(){
        return getClassificationWithValidSuperType();
    }

    public static List<AtlasEntityDef> getEntityWithValidSuperType() {
        AtlasEntityDef developerTypeDef = createClassTypeDef("Developer", "Developer_description", Collections.singleton("Employee"),
                new AtlasAttributeDef("language", String.format("array<%s>", "string"), false, AtlasAttributeDef.Cardinality.SET,
                        1, 10, false, false, false,
                        Collections.<AtlasConstraintDef>emptyList()));

        List<AtlasEntityDef> ret = Arrays.asList(developerTypeDef);

        populateSystemAttributes(ret);

        return ret;
    }

    public static List<AtlasEntityDef> getEntityWithName(String name) {
        AtlasEntityDef developerTypeDef = createClassTypeDef(name, "Developer_description", Collections.<String>emptySet(),
                new AtlasAttributeDef("language", String.format("array<%s>", "string"), false, AtlasAttributeDef.Cardinality.SET,
                        1, 10, false, false, false,
                        Collections.<AtlasConstraintDef>emptyList()));

        List<AtlasEntityDef> ret = Arrays.asList(developerTypeDef);

        populateSystemAttributes(ret);

        return ret;
    }

    public static AtlasEntityDef getSingleEntityWithName(String name) {
        AtlasEntityDef developerTypeDef = createClassTypeDef(name, "Developer_description", Collections.<String>emptySet(),
                new AtlasAttributeDef("language", String.format("array<%s>", "string"), false, AtlasAttributeDef.Cardinality.SET,
                        1, 10, false, false, false,
                        Collections.<AtlasConstraintDef>emptyList()));

        return developerTypeDef;
    }

    public static List<AtlasEntityDef> getEntityWithValidAttribute() {
        List<AtlasEntityDef> entityDefs = getEntityWithValidSuperType();
        entityDefs.get(1).getSuperTypes().clear();
        return entityDefs;
    }

    public static AtlasClassificationDef getClassificationWithInvalidSuperType() {
        AtlasClassificationDef classificationDef = simpleType().getClassificationDefs().get(0);
        classificationDef.getSuperTypes().add("!@#$%");
        return classificationDef;
    }

    public static AtlasEntityDef getEntityWithInvalidSuperType() {
        AtlasEntityDef entityDef = simpleType().getEntityDefs().get(0);
        entityDef.addSuperType("!@#$%");
        return entityDef;
    }

    public static void populateSystemAttributes(AtlasTypesDef typesDef) {
        populateSystemAttributes(typesDef.getEnumDefs());
        populateSystemAttributes(typesDef.getStructDefs());
        populateSystemAttributes(typesDef.getClassificationDefs());
        populateSystemAttributes(typesDef.getEntityDefs());
        populateSystemAttributes(typesDef.getRelationshipDefs());
        populateSystemAttributes(typesDef.getBusinessMetadataDefs());
    }

    public static void populateSystemAttributes(List<? extends AtlasBaseTypeDef> typeDefs) {
        if (CollectionUtils.isNotEmpty(typeDefs)) {
            for (AtlasBaseTypeDef typeDef : typeDefs) {
                populateSystemAttributes(typeDef);
            }
        }
    }

    public static void populateSystemAttributes(AtlasBaseTypeDef typeDef) {
        typeDef.setCreatedBy(TestUtilsV2.TEST_USER);
        typeDef.setUpdatedBy(TestUtilsV2.TEST_USER);
    }

    public static InputStream getFile(String subDir, String fileName){
        final String userDir  = System.getProperty("user.dir");
        String filePath = getTestFilePath(userDir, subDir, fileName);
        File file = new File(filePath);
        InputStream  fs       = null;

        try {
            fs = new FileInputStream(file);
        } catch (FileNotFoundException e) {
            LOG.error("File could not be found at: {}", filePath, e);
        }

        return fs;
    }

    public static String getFileData(String subDir, String fileName)throws IOException {
        final String userDir  = System.getProperty("user.dir");
        String       filePath = getTestFilePath(userDir, subDir, fileName);
        File f        = new File(filePath);
        String ret = FileUtils.readFileToString(f, "UTF-8");
        return ret;
    }

    private static String getTestFilePath(String startPath, String subDir, String fileName) {
        if (StringUtils.isNotEmpty(subDir)) {
            return startPath + "/src/test/resources/" + subDir + "/" + fileName;
        } else {
            return startPath + "/src/test/resources/" + fileName;
        }
    }
}
