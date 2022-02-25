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

import org.apache.atlas.exception.AtlasBaseException;
import org.apache.atlas.model.instance.AtlasEntity;
import org.apache.atlas.model.instance.AtlasEntity.AtlasEntitiesWithExtInfo;
import org.apache.atlas.model.instance.AtlasObjectId;
import org.apache.atlas.model.instance.AtlasStruct;
import org.apache.atlas.model.typedef.AtlasBusinessMetadataDef;
import org.apache.atlas.model.typedef.AtlasClassificationDef;
import org.apache.atlas.model.typedef.AtlasEntityDef;
import org.apache.atlas.model.typedef.AtlasEnumDef;
import org.apache.atlas.model.typedef.AtlasEnumDef.AtlasEnumElementDef;
import org.apache.atlas.model.typedef.AtlasRelationshipDef;
import org.apache.atlas.model.typedef.AtlasRelationshipEndDef;
import org.apache.atlas.model.typedef.AtlasStructDef;
import org.apache.atlas.model.typedef.AtlasTypesDef;
import org.apache.atlas.type.AtlasEntityType;
import org.apache.atlas.type.AtlasType;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;

import static org.apache.atlas.model.typedef.AtlasBusinessMetadataDef.ATTR_OPTION_APPLICABLE_ENTITY_TYPES;
import static org.apache.atlas.model.typedef.AtlasRelationshipDef.PropagateTags.BOTH;
import static org.apache.atlas.model.typedef.AtlasRelationshipDef.PropagateTags.ONE_TO_TWO;
import static org.apache.atlas.model.typedef.AtlasRelationshipDef.RelationshipCategory.AGGREGATION;
import static org.apache.atlas.model.typedef.AtlasRelationshipDef.RelationshipCategory.ASSOCIATION;
import static org.apache.atlas.model.typedef.AtlasStructDef.AtlasAttributeDef.Cardinality.SET;
import static org.apache.atlas.model.typedef.AtlasStructDef.AtlasAttributeDef.Cardinality.SINGLE;
import static org.apache.atlas.type.AtlasTypeUtil.createClassTypeDef;
import static org.apache.atlas.type.AtlasTypeUtil.createOptionalAttrDef;
import static org.apache.atlas.type.AtlasTypeUtil.createRequiredAttrDef;
import static org.apache.atlas.type.AtlasTypeUtil.createStructTypeDef;
import static org.apache.atlas.type.AtlasTypeUtil.createTraitTypeDef;
import static org.apache.atlas.type.AtlasTypeUtil.createUniqueRequiredAttrDef;
import static org.apache.atlas.type.AtlasTypeUtil.getAtlasObjectId;

/**
 * Test utility class for relationship.
 */
public final class TestRelationshipUtilsV2 {

    public static final String ORG_LEVEL_TYPE           = "OrgLevel";
    public static final String SECURITY_CLEARANCE_TYPE  = "SecurityClearance";
    public static final String ADDRESS_TYPE             = "Address";
    public static final String PERSON_TYPE              = "Person";
    public static final String MANAGER_TYPE             = "Manager";
    public static final String DEPARTMENT_TYPE          = "Department";
    public static final String EMPLOYEE_TYPE            = "Employee";
    public static final String EMPLOYEE_DEPARTMENT_TYPE = "EmployeeDepartment";
    public static final String EMPLOYEE_MANAGER_TYPE    = "EmployeeManager";
    public static final String EMPLOYEE_MENTORS_TYPE    = "EmployeeMentors";
    public static final String EMPLOYEE_FRIENDS_TYPE    = "EmployeeFriends";
    public static final String PERSON_SIBLING_TYPE      = "PersonSibling";
    public static final String TYPE_A                   = "A";
    public static final String TYPE_B                   = "B";
    public static final String DEFAULT_VERSION          = "1.0";

    private TestRelationshipUtilsV2() { }

    public static AtlasTypesDef getDepartmentEmployeeTypes() throws AtlasBaseException {

        /******* Person Type *******/
        AtlasEntityDef personType = createClassTypeDef(PERSON_TYPE, description(PERSON_TYPE), superType(null),
                                                        createUniqueRequiredAttrDef("name", "string"),
                                                        createOptionalAttrDef("address", ADDRESS_TYPE),
                                                        createOptionalAttrDef("birthday", "date"),
                                                        createOptionalAttrDef("hasPets", "boolean"),
                                                        createOptionalAttrDef("numberOfCars", "byte"),
                                                        createOptionalAttrDef("houseNumber", "short"),
                                                        createOptionalAttrDef("carMileage", "int"),
                                                        createOptionalAttrDef("age", "float"),
                                                        createOptionalAttrDef("numberOfStarsEstimate", "biginteger"),
                                                        createOptionalAttrDef("approximationOfPi", "bigdecimal"));
        /******* Employee Type *******/
        AtlasEntityDef employeeType = createClassTypeDef(EMPLOYEE_TYPE, description(EMPLOYEE_TYPE), superType(PERSON_TYPE),
                                                        createOptionalAttrDef("orgLevel", ORG_LEVEL_TYPE),
                                                        createOptionalAttrDef("shares", "long"),
                                                        createOptionalAttrDef("salary", "double"));
        employeeType.setOption(AtlasEntityType.OPTION_APPEND_RELATIONSHIPS_ON_PARTIAL_UPDATE, "[\"friends\"]");
        /******* Department Type *******/
        AtlasEntityDef departmentType = createClassTypeDef(DEPARTMENT_TYPE, description(DEPARTMENT_TYPE), superType(null),
                                                        createUniqueRequiredAttrDef("name", "string"));
        /******* Manager Type *******/
        AtlasEntityDef managerType = createClassTypeDef(MANAGER_TYPE, description(MANAGER_TYPE), superType(EMPLOYEE_TYPE));
        /******* Address Type *******/
        AtlasStructDef addressType = createStructTypeDef(ADDRESS_TYPE, description(ADDRESS_TYPE),
                                                        createRequiredAttrDef("street", "string"),
                                                        createRequiredAttrDef("city", "string"));
        /******* Organization Level Type *******/
        AtlasEnumDef orgLevelType = new AtlasEnumDef(ORG_LEVEL_TYPE, description(ORG_LEVEL_TYPE), DEFAULT_VERSION,
                                                        getOrgLevelElements());

        /******* Security Clearance Type *******/
        AtlasClassificationDef securityClearanceType = createTraitTypeDef(SECURITY_CLEARANCE_TYPE, description(SECURITY_CLEARANCE_TYPE),
                                                        superType(null), createRequiredAttrDef("level", "int"));

        /******* [Department -> Employee] Relationship *******/
        AtlasRelationshipDef employeeDepartmentType = new AtlasRelationshipDef(EMPLOYEE_DEPARTMENT_TYPE, description(EMPLOYEE_DEPARTMENT_TYPE),
                                                        DEFAULT_VERSION, AGGREGATION, ONE_TO_TWO,
                                                        new AtlasRelationshipEndDef(EMPLOYEE_TYPE, "department", SINGLE),
                                                        new AtlasRelationshipEndDef(DEPARTMENT_TYPE, "employees", SET, true));

        /******* [Manager -> Employee] Relationship *******/
        AtlasRelationshipDef employeeManagerType    = new AtlasRelationshipDef(EMPLOYEE_MANAGER_TYPE, description(EMPLOYEE_MANAGER_TYPE),
                                                        DEFAULT_VERSION, AGGREGATION, ONE_TO_TWO,
                                                        new AtlasRelationshipEndDef(EMPLOYEE_TYPE, "manager", SINGLE),
                                                        new AtlasRelationshipEndDef(MANAGER_TYPE, "subordinates", SET, true));

        /******* [Mentors -> Employee] Relationship *******/
        AtlasRelationshipDef employeeMentorsType    = new AtlasRelationshipDef(EMPLOYEE_MENTORS_TYPE, description(EMPLOYEE_MENTORS_TYPE),
                                                        DEFAULT_VERSION, AGGREGATION, ONE_TO_TWO,
                                                        new AtlasRelationshipEndDef(EMPLOYEE_TYPE, "mentors", SET),
                                                        new AtlasRelationshipEndDef(EMPLOYEE_TYPE, "mentees", SET, true));

        /******* [Friends -> Employee] Relationship *******/
        AtlasRelationshipDef employeeFriendsType    = new AtlasRelationshipDef(EMPLOYEE_FRIENDS_TYPE, description(EMPLOYEE_FRIENDS_TYPE),
                                                        DEFAULT_VERSION, ASSOCIATION, ONE_TO_TWO,
                                                        new AtlasRelationshipEndDef(EMPLOYEE_TYPE, "friends", SET),
                                                        new AtlasRelationshipEndDef(EMPLOYEE_TYPE, "friends", SET));

        /******* [Person -> Sibling] Relationship *******/
        AtlasRelationshipDef personSiblingType    = new AtlasRelationshipDef(PERSON_SIBLING_TYPE, description(PERSON_SIBLING_TYPE),
                                                        DEFAULT_VERSION, ASSOCIATION, BOTH,
                                                        new AtlasRelationshipEndDef(PERSON_TYPE, "sibling", SINGLE),
                                                        new AtlasRelationshipEndDef(PERSON_TYPE, "sibling", SINGLE));

        AtlasStructDef.AtlasAttributeDef nsAttr1 = new AtlasStructDef.AtlasAttributeDef("attr1", "int");
        AtlasStructDef.AtlasAttributeDef nsAttr2 = new AtlasStructDef.AtlasAttributeDef("attr2", "int");

        nsAttr1.setOption(ATTR_OPTION_APPLICABLE_ENTITY_TYPES, AtlasType.toJson(Collections.singleton(DEPARTMENT_TYPE)));
        nsAttr1.setIsOptional(true);
        nsAttr1.setIsUnique(false);

        nsAttr2.setOption(ATTR_OPTION_APPLICABLE_ENTITY_TYPES, AtlasType.toJson(Collections.singleton(DEPARTMENT_TYPE)));
        nsAttr2.setIsOptional(true);
        nsAttr2.setIsUnique(false);

        AtlasBusinessMetadataDef businessMetadataDef = new AtlasBusinessMetadataDef("test_businessMetadata", "test_description", DEFAULT_VERSION, Arrays.asList(nsAttr1, nsAttr2));

        return new AtlasTypesDef(Collections.singletonList(orgLevelType),
                                 Collections.singletonList(addressType),
                                 Collections.singletonList(securityClearanceType),
                                 Arrays.asList(personType, employeeType, departmentType, managerType),
                                 Arrays.asList(employeeDepartmentType, employeeManagerType, employeeMentorsType, employeeFriendsType, personSiblingType),
                                 Collections.singletonList(businessMetadataDef));
    }

    public static AtlasEntitiesWithExtInfo getDepartmentEmployeeInstances() {
        AtlasEntitiesWithExtInfo ret = new AtlasEntitiesWithExtInfo();

        /******* Department - HR *******/
        AtlasEntity hrDept = new AtlasEntity(DEPARTMENT_TYPE, "name", "hr");

        /******* Address *******/
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

        AtlasStruct mikeAddr = new AtlasStruct(ADDRESS_TYPE);
            mikeAddr.setAttribute("street", "Casa Verde St");
            mikeAddr.setAttribute("city", "San Jose");

        /******* Manager - Jane (Subordinates: [John, Max]) *******/
        AtlasEntity jane = new AtlasEntity(MANAGER_TYPE);
            jane.setAttribute("name", "Jane");
            jane.setRelationshipAttribute("department", getAtlasObjectId(hrDept));
            jane.setAttribute("address", janeAddr);

        /******* Manager - Julius (Subordinates: [], Sibling: Jane) *******/
        AtlasEntity julius = new AtlasEntity(MANAGER_TYPE);
            julius.setAttribute("name", "Julius");
            julius.setRelationshipAttribute("department", getAtlasObjectId(hrDept));
            julius.setRelationshipAttribute("sibling", getAtlasObjectId(jane));
            julius.setAttribute("address", juliusAddr);

        /******* Employee - Max (Manager: Jane, Mentors: [Julius], Sibling: Julius) *******/
        AtlasEntity max = new AtlasEntity(EMPLOYEE_TYPE);
            max.setAttribute("name", "Max");
            max.setRelationshipAttribute("department", getAtlasObjectId(hrDept));
            max.setAttribute("address", maxAddr);
            max.setRelationshipAttribute("manager", getAtlasObjectId(jane));
            max.setRelationshipAttribute("mentors", getAtlasObjectIds(julius));
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

        /******* Employee - John (Manager: Jane, Mentors: [Max], Friends: [Max]) *******/
        AtlasEntity john = new AtlasEntity(EMPLOYEE_TYPE);
            john.setAttribute("name", "John");
            john.setRelationshipAttribute("department", getAtlasObjectId(hrDept));
            john.setAttribute("address", johnAddr);
            john.setRelationshipAttribute("manager", getAtlasObjectId(jane));
            john.setRelationshipAttribute("mentors", getAtlasObjectIds(max, julius));
            john.setRelationshipAttribute("friends", getAtlasObjectIds(max));
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

        /******* Employee - Mike (Manager: Julius, Friends: [Max, John]) *******/
        AtlasEntity mike = new AtlasEntity(EMPLOYEE_TYPE);
            mike.setAttribute("name", "Mike");
            mike.setRelationshipAttribute("department", getAtlasObjectId(hrDept));
            mike.setAttribute("address", mikeAddr);
            mike.setRelationshipAttribute("manager", getAtlasObjectId(julius));
            mike.setRelationshipAttribute("friends", getAtlasObjectIds(max, john));
            mike.setAttribute("birthday",new Date(1947, 8, 15));
            mike.setAttribute("hasPets", false);
            mike.setAttribute("numberOfCars", 2);
            mike.setAttribute("houseNumber", 3737);
            mike.setAttribute("carMileage", 25000);
            mike.setAttribute("shares", Long.MIN_VALUE);
            mike.setAttribute("salary", Double.MIN_VALUE);
            mike.setAttribute("age", 37);
            mike.setAttribute("numberOfStarsEstimate", new BigInteger("5000050000050000050005"));
            mike.setAttribute("approximationOfPi", new BigDecimal("3.14159"));

        ret.addEntity(hrDept);
        ret.addEntity(jane);
        ret.addEntity(julius);
        ret.addEntity(max);
        ret.addEntity(john);
        ret.addEntity(mike);

        return ret;
    }

    public static AtlasTypesDef getInverseReferenceTestTypes() throws AtlasBaseException {
        AtlasEntityDef aType = createClassTypeDef(TYPE_A, superType(null), createUniqueRequiredAttrDef("name", "string"));
        AtlasEntityDef bType = createClassTypeDef(TYPE_B, superType(null), createUniqueRequiredAttrDef("name", "string"));

        AtlasRelationshipDef relationshipType1 = new AtlasRelationshipDef("TypeA_to_TypeB_on_b", description("TypeA_to_TypeB_on_b"),
                                                        DEFAULT_VERSION, ASSOCIATION, ONE_TO_TWO,
                                                        new AtlasRelationshipEndDef(TYPE_A, "b", SINGLE),
                                                        new AtlasRelationshipEndDef(TYPE_B, "a", SINGLE));

        AtlasRelationshipDef relationshipType2 = new AtlasRelationshipDef("TypeA_to_TypeB_on_oneB", description("TypeA_to_TypeB_on_oneB"),
                                                        DEFAULT_VERSION, ASSOCIATION, ONE_TO_TWO,
                                                        new AtlasRelationshipEndDef(TYPE_A, "oneB", SINGLE),
                                                        new AtlasRelationshipEndDef(TYPE_B, "manyA", SET));

        AtlasRelationshipDef relationshipType3 = new AtlasRelationshipDef("TypeA_to_TypeB_on_manyB", description("TypeA_to_TypeB_on_manyB"),
                                                        DEFAULT_VERSION, ASSOCIATION, ONE_TO_TWO,
                                                        new AtlasRelationshipEndDef(TYPE_A, "manyB", SET),
                                                        new AtlasRelationshipEndDef(TYPE_B, "manyToManyA", SET));

        AtlasRelationshipDef relationshipType4 = new AtlasRelationshipDef("TypeB_to_TypeA_on_mappedFromA", description("TypeB_to_TypeA_on_mappedFromA"),
                                                        DEFAULT_VERSION, ASSOCIATION, ONE_TO_TWO,
                                                        new AtlasRelationshipEndDef(TYPE_B, "mappedFromA", SINGLE),
                                                        new AtlasRelationshipEndDef(TYPE_A, "mapToB", SET));

        return new AtlasTypesDef(Collections.<AtlasEnumDef>emptyList(), Collections.<AtlasStructDef>emptyList(), Collections.<AtlasClassificationDef>emptyList(),  Arrays.asList(aType, bType),
                                 Arrays.asList(relationshipType1, relationshipType2, relationshipType3, relationshipType4), Collections.<AtlasBusinessMetadataDef>emptyList());
    }

    private static List<AtlasEnumElementDef> getOrgLevelElements() {
        return Arrays.asList(
                new AtlasEnumElementDef("L1", description("L1"), 1),
                new AtlasEnumElementDef("L2", description("L2"), 2),
                new AtlasEnumElementDef("L3", description("L3"), 3)
        );
    }

    private static String description(String typeName) {
        return typeName + " description";
    }

    private static Set<String> superType(String superTypeName) {
        return StringUtils.isNotEmpty(superTypeName) ? Collections.singleton(superTypeName) : Collections.<String>emptySet();
    }

    private static List<AtlasObjectId> getAtlasObjectIds(AtlasEntity... entities) {
        List<AtlasObjectId> ret = new ArrayList<>();

        if (ArrayUtils.isNotEmpty(entities)) {
            for (AtlasEntity entity : entities) {
                ret.add(getAtlasObjectId(entity));
            }
        }

        return ret;
    }
}