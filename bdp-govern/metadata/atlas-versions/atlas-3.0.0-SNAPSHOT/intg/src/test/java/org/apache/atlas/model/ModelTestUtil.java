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
package org.apache.atlas.model;

import org.apache.atlas.exception.AtlasBaseException;
import org.apache.atlas.model.instance.AtlasClassification;
import org.apache.atlas.model.instance.AtlasEntity;
import org.apache.atlas.model.instance.AtlasStruct;
import org.apache.atlas.model.typedef.*;
import org.apache.atlas.model.typedef.AtlasEnumDef.AtlasEnumElementDef;
import org.apache.atlas.model.typedef.AtlasStructDef.AtlasAttributeDef;
import org.apache.atlas.type.AtlasClassificationType;
import org.apache.atlas.type.AtlasEntityType;
import org.apache.atlas.type.AtlasStructType;
import org.apache.atlas.type.AtlasTypeRegistry;
import org.apache.atlas.type.AtlasTypeRegistry.AtlasTransientTypeRegistry;
import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static org.apache.atlas.model.typedef.AtlasBaseTypeDef.ATLAS_BUILTIN_TYPES;
import static org.apache.atlas.model.typedef.AtlasBaseTypeDef.ATLAS_PRIMITIVE_TYPES;
import static org.apache.atlas.model.typedef.AtlasBaseTypeDef.ATLAS_RELATIONSHIP_ATTRIBUTE_TYPES;


public final class  ModelTestUtil {
    private static final Logger LOG = LoggerFactory.getLogger(ModelTestUtil.class);

    private static final String PREFIX_ENUM_DEF           = "testEnumDef-";
    private static final String PREFIX_STRUCT_DEF         = "testStructDef-";
    private static final String PREFIX_ENTITY_DEF         = "testEntityDef-";
    private static final String PREFIX_CLASSIFICATION_DEF = "testClassificationDef-";
    private static final String PREFIX_ATTRIBUTE_NAME     = "attr-";
    private static final String PREFIX_STRUCT             = "testStruct-";
    private static final String PREFIX_ENTITY             = "testEntity-";
    private static final String PREFIX_CLASSIFICATION     = "testClassification-";
    private static final int    MAX_ENUM_ELEMENT_COUNT    = 30;

    private static final AtomicInteger IDX_ENUM_DEF           = new AtomicInteger();
    private static final AtomicInteger IDX_ENTITY_DEF         = new AtomicInteger();
    private static final AtomicInteger IDX_CLASSIFICATION_DEF = new AtomicInteger();
    private static final AtomicInteger IDX_STRUCT_DEF         = new AtomicInteger();
    private static final AtomicInteger IDX_CLASSIFICATION     = new AtomicInteger();
    private static final AtomicInteger IDX_ENTITY             = new AtomicInteger();
    private static final AtomicInteger IDX_STRUCT             = new AtomicInteger();

    private static final AtlasTypeRegistry      TYPE_REGISTRY;
    private static final AtlasEnumDef           ENUM_DEF;
    private static final AtlasEnumDef           ENUM_DEF_WITH_NO_DEFAULT;
    private static final AtlasStructDef         STRUCT_DEF;
    private static final AtlasEntityDef         ENTITY_DEF;
    private static final AtlasEntityDef         ENTITY_DEF_WITH_SUPER_TYPE;
    private static final AtlasEntityDef         ENTITY_DEF_WITH_SUPER_TYPES;
    private static final AtlasClassificationDef CLASSIFICATION_DEF;
    private static final AtlasClassificationDef CLASSIFICATION_DEF_WITH_SUPER_TYPE;
    private static final AtlasClassificationDef CLASSIFICATION_DEF_WITH_SUPER_TYPES;

    static {
        TYPE_REGISTRY = new AtlasTypeRegistry();

        ENUM_DEF                 = newEnumDef(true);
        ENUM_DEF_WITH_NO_DEFAULT = newEnumDef(false);

        STRUCT_DEF = newStructDef();

        ENTITY_DEF                  = newEntityDef();
        ENTITY_DEF_WITH_SUPER_TYPE  = newEntityDef(new AtlasEntityDef[] { ENTITY_DEF });
        ENTITY_DEF_WITH_SUPER_TYPES = newEntityDef(new AtlasEntityDef[] { ENTITY_DEF, ENTITY_DEF_WITH_SUPER_TYPE });

        CLASSIFICATION_DEF                  = newClassificationDef();
        CLASSIFICATION_DEF_WITH_SUPER_TYPE  = newClassificationDef(new AtlasClassificationDef[] { CLASSIFICATION_DEF });
        CLASSIFICATION_DEF_WITH_SUPER_TYPES = newClassificationDef(
                               new AtlasClassificationDef[] { CLASSIFICATION_DEF, CLASSIFICATION_DEF_WITH_SUPER_TYPE });
    }

    private ModelTestUtil() {

    }

    public static AtlasTypeRegistry getTypesRegistry() { return TYPE_REGISTRY; }

    public static AtlasEnumDef getEnumDef() { return ENUM_DEF; }

    public static AtlasEnumDef getEnumDefWithNoDefault() { return ENUM_DEF_WITH_NO_DEFAULT; }

    public static AtlasStructDef getStructDef() { return STRUCT_DEF; }

    public static AtlasEntityDef getEntityDef() { return ENTITY_DEF; }

    public static AtlasEntityDef getEntityDefWithSuperType() { return ENTITY_DEF_WITH_SUPER_TYPE; }

    public static AtlasEntityDef getEntityDefWithSuperTypes() { return ENTITY_DEF_WITH_SUPER_TYPES; }

    public static AtlasClassificationDef getClassificationDef() { return CLASSIFICATION_DEF; }

    public static AtlasClassificationDef getClassificationDefWithSuperType() {
        return CLASSIFICATION_DEF_WITH_SUPER_TYPE;
    }

    public static AtlasClassificationDef getClassificationDefWithSuperTypes() {
        return CLASSIFICATION_DEF_WITH_SUPER_TYPES;
    }


    public static AtlasEnumDef newEnumDef() {
        return newEnumDef(getTypesRegistry(), true);
    }

    public static AtlasEnumDef newEnumDef(boolean hasDefaultValue) {
        return newEnumDef(getTypesRegistry(), hasDefaultValue);
    }

    public static AtlasEnumDef newEnumDef(AtlasTypeRegistry typesRegistry) {
        return newEnumDef(getTypesRegistry(), true);
    }

    public static AtlasEnumDef newEnumDef(AtlasTypeRegistry typesRegistry, boolean hasDefaultValue) {
        int enumDefIdx = IDX_ENUM_DEF.getAndIncrement();

        AtlasEnumDef ret = new AtlasEnumDef();

        ret.setName(PREFIX_ENUM_DEF + enumDefIdx);
        ret.setDescription(ret.getName());

        int numElements = ThreadLocalRandom.current().nextInt(1, MAX_ENUM_ELEMENT_COUNT);

        for (int i = 0; i < numElements; i++) {
            String elementName = "element-" + i;
            ret.addElement(new AtlasEnumElementDef(elementName, elementName.toUpperCase(), i));
        }

        if (hasDefaultValue) {
            int idxDefault = ThreadLocalRandom.current().nextInt(0, numElements);

            ret.setDefaultValue(ret.getElementDefs().get(idxDefault).getValue());
        }

        AtlasTransientTypeRegistry ttr    = null;
        boolean                    commit = false;

        try {
            ttr = typesRegistry.lockTypeRegistryForUpdate();

            ttr.addType(ret);

            commit = true;
        } catch (AtlasBaseException excp) {
            LOG.error("failed to create enum-def", excp);

            ret = null;
        } finally {
            typesRegistry.releaseTypeRegistryForUpdate(ttr, commit);
        }

        return ret;
    }

    public static AtlasStructDef newStructDef() {
        return newStructDef(getTypesRegistry());
    }

    public static AtlasStructDef newStructDef(AtlasTypeRegistry typesRegistry) {
        int structDefIdx = IDX_STRUCT_DEF.getAndIncrement();

        AtlasStructDef ret = new AtlasStructDef();

        ret.setName(PREFIX_STRUCT_DEF + structDefIdx);
        ret.setDescription(ret.getName());
        ret.setAttributeDefs(newAttributeDefsWithAllBuiltInTypes(PREFIX_ATTRIBUTE_NAME));

        AtlasTransientTypeRegistry ttr    = null;
        boolean                    commit = false;

        try {
            ttr = typesRegistry.lockTypeRegistryForUpdate();

            ttr.addType(ret);

            commit = true;
        } catch (AtlasBaseException excp) {
            LOG.error("failed to create struct-def", excp);

            ret = null;
        } finally {
            typesRegistry.releaseTypeRegistryForUpdate(ttr, commit);
        }

        return ret;
    }

    public static AtlasEntityDef newEntityDef() {
        return newEntityDef(getTypesRegistry(), null);
    }

    public static AtlasEntityDef newEntityDef(AtlasEntityDef[] superTypes) {
        return newEntityDef(getTypesRegistry(), superTypes);
    }

    public static AtlasEntityDef newEntityDef(AtlasTypeRegistry typesRegistry) {
        return newEntityDef(typesRegistry, null);
    }

    public static AtlasEntityDef newEntityDef(AtlasTypeRegistry typesRegistry, AtlasEntityDef[] superTypes) {
        int entDefIdx = IDX_ENTITY_DEF.getAndIncrement();

        AtlasEntityDef ret = new AtlasEntityDef();

        ret.setName(PREFIX_ENTITY_DEF + entDefIdx);
        ret.setDescription(ret.getName());
        ret.setAttributeDefs(newAttributeDefsWithAllBuiltInTypes(PREFIX_ATTRIBUTE_NAME));

        List<AtlasAttributeDef> currentTypeAttrDefs = ret.getAttributeDefs();

        if (superTypes != null) {
            for (AtlasEntityDef superType : superTypes) {
                List<AtlasAttributeDef> superTypeAttrDefs = superType.getAttributeDefs();
                removeAttributeIfExistedInSuperType(currentTypeAttrDefs, superTypeAttrDefs);

                ret.addSuperType(superType.getName());
            }
        }

        AtlasTransientTypeRegistry ttr    = null;
        boolean                    commit = false;

        try {
            ttr = typesRegistry.lockTypeRegistryForUpdate();

            ttr.addType(ret);

            commit = true;
        } catch (AtlasBaseException excp) {
            LOG.error("failed to create entity-def", excp);

            ret = null;
        } finally {
            typesRegistry.releaseTypeRegistryForUpdate(ttr, commit);
        }

        return ret;
    }

    public static AtlasEntityDef newEntityDefWithSuperTypes() {
        return newEntityDefWithSuperTypes(getTypesRegistry());
    }

    public static AtlasEntityDef newEntityDefWithSuperTypes(AtlasTypeRegistry typesRegistry) {
        return newEntityDef(typesRegistry, new AtlasEntityDef[] { ENTITY_DEF, ENTITY_DEF_WITH_SUPER_TYPE });
    }

    public static AtlasClassificationDef newClassificationDef() {
        return newClassificationDef(getTypesRegistry(), null);
    }

    public static AtlasClassificationDef newClassificationDef(AtlasClassificationDef[] superTypes) {
        return newClassificationDef(getTypesRegistry(), superTypes);
    }

    public static AtlasClassificationDef newClassificationDef(AtlasTypeRegistry typesRegistry) {
        return newClassificationDef(typesRegistry, null);
    }

    public static AtlasClassificationDef newClassificationDef(AtlasTypeRegistry typesRegistry,
                                                              AtlasClassificationDef[] superTypes) {
        int classificationDefIdx = IDX_CLASSIFICATION_DEF.getAndIncrement();

        AtlasClassificationDef ret = new AtlasClassificationDef();

        ret.setName(PREFIX_CLASSIFICATION_DEF + classificationDefIdx);
        ret.setDescription(ret.getName());
        ret.setAttributeDefs(newAttributeDefsWithAllBuiltInTypes(PREFIX_ATTRIBUTE_NAME));

        List<AtlasAttributeDef> currentTypeAttrDefs = ret.getAttributeDefs();

        if (superTypes != null) {
            for (AtlasClassificationDef superType : superTypes) {
                List<AtlasAttributeDef> superTypeAttrDefs = superType.getAttributeDefs();
                removeAttributeIfExistedInSuperType(currentTypeAttrDefs, superTypeAttrDefs);

                ret.addSuperType(superType.getName());
            }
        }

        AtlasTransientTypeRegistry ttr    = null;
        boolean                    commit = false;

        try {
            ttr = typesRegistry.lockTypeRegistryForUpdate();

            ttr.addType(ret);

            commit = true;
        } catch (AtlasBaseException excp) {
            LOG.error("failed to create classification-def", excp);

            ret = null;
        } finally {
            typesRegistry.releaseTypeRegistryForUpdate(ttr, commit);
        }

        return ret;
    }


    public static AtlasEntity newEntity(AtlasEntityDef entityDef) {
        return newEntity(entityDef, getTypesRegistry());
    }

    public static AtlasEntity newEntity(AtlasEntityDef entityDef, AtlasTypeRegistry typesRegistry) {
        AtlasEntity ret = null;

        AtlasEntityType entityType = typesRegistry.getEntityTypeByName(entityDef.getName());

        if (entityType != null) {
            ret = entityType.createDefaultValue();
        } else {
            LOG.error("failed to get entity-type {}", entityDef.getName());
        }

        return ret;
    }

    public static AtlasStruct newStruct(AtlasStructDef structDef) {
        return newStruct(structDef, getTypesRegistry());
    }

    public static AtlasStruct newStruct(AtlasStructDef structDef, AtlasTypeRegistry typesRegistry) {
        AtlasStruct ret = null;

        AtlasStructType structType = typesRegistry.getStructTypeByName(structDef.getName());

        if (structType != null) {
            ret = structType.createDefaultValue();
        } else {
            LOG.error("failed to get struct-type {}", structDef.getName());
        }

        return ret;
    }

    public static AtlasClassification newClassification(AtlasClassificationDef classificationDef) {
        return newClassification(classificationDef, getTypesRegistry());
    }

    public static AtlasClassification newClassification(AtlasClassificationDef classificationDef,
                                                        AtlasTypeRegistry typesRegistry) {
        AtlasClassification ret = null;

        AtlasClassificationType classificationType = typesRegistry.getClassificationTypeByName(classificationDef.getName());

        if (classificationType != null) {
            ret = classificationType.createDefaultValue();
        } else {
            LOG.error("failed to get classification-type {}", classificationDef.getName());
        }

        return ret;
    }

    public static List<AtlasAttributeDef> newAttributeDefsWithAllBuiltInTypes(String attrNamePrefix) {
        List<AtlasAttributeDef> ret = new ArrayList<>();

        // add all built-in types
        for (String ATLAS_BUILTIN_TYPE2 : ATLAS_BUILTIN_TYPES) {
            ret.add(getAttributeDef(attrNamePrefix, ATLAS_BUILTIN_TYPE2));
        }
        // add enum types
        ret.add(getAttributeDef(attrNamePrefix, ENUM_DEF.getName()));
        ret.add(getAttributeDef(attrNamePrefix, ENUM_DEF_WITH_NO_DEFAULT.getName()));

        // add array of built-in types
        for (String ATLAS_BUILTIN_TYPE1 : ATLAS_BUILTIN_TYPES) {
            ret.add(getAttributeDef(attrNamePrefix, AtlasBaseTypeDef.getArrayTypeName(ATLAS_BUILTIN_TYPE1)));
        }
        // add array of enum types
        ret.add(getAttributeDef(attrNamePrefix, AtlasBaseTypeDef.getArrayTypeName(ENUM_DEF.getName())));
        ret.add(getAttributeDef(attrNamePrefix, AtlasBaseTypeDef.getArrayTypeName(ENUM_DEF_WITH_NO_DEFAULT.getName())));

        // add few map types
        for (String ATLAS_PRIMITIVE_TYPE3 : ATLAS_PRIMITIVE_TYPES) {
            ret.add(getAttributeDef(attrNamePrefix,
                    AtlasBaseTypeDef.getMapTypeName(ATLAS_PRIMITIVE_TYPE3, getRandomBuiltInType())));
        }
        // add map types with enum as key
        ret.add(getAttributeDef(attrNamePrefix,
                                AtlasBaseTypeDef.getMapTypeName(ENUM_DEF.getName(), getRandomBuiltInType())));
        ret.add(getAttributeDef(attrNamePrefix,
                AtlasBaseTypeDef.getMapTypeName(ENUM_DEF_WITH_NO_DEFAULT.getName(), getRandomBuiltInType())));
        // add map types with enum as value
        ret.add(getAttributeDef(attrNamePrefix,
                AtlasBaseTypeDef.getMapTypeName(getRandomPrimitiveType(), ENUM_DEF.getName())));
        ret.add(getAttributeDef(attrNamePrefix,
                AtlasBaseTypeDef.getMapTypeName(getRandomPrimitiveType(), ENUM_DEF_WITH_NO_DEFAULT.getName())));

        // add few array of arrays
        for (String ATLAS_BUILTIN_TYPE : ATLAS_BUILTIN_TYPES) {
            ret.add(getAttributeDef(attrNamePrefix,
                    AtlasBaseTypeDef.getArrayTypeName(AtlasBaseTypeDef.getArrayTypeName(getRandomBuiltInType()))));
        }
        ret.add(getAttributeDef(attrNamePrefix, AtlasBaseTypeDef.getArrayTypeName(ENUM_DEF.getName())));
        ret.add(getAttributeDef(attrNamePrefix, AtlasBaseTypeDef.getArrayTypeName(ENUM_DEF_WITH_NO_DEFAULT.getName())));

        // add few array of maps
        for (String ATLAS_PRIMITIVE_TYPE2 : ATLAS_PRIMITIVE_TYPES) {
            ret.add(getAttributeDef(attrNamePrefix, AtlasBaseTypeDef.getArrayTypeName(
                    AtlasBaseTypeDef.getMapTypeName(ATLAS_PRIMITIVE_TYPE2, getRandomBuiltInType()))));
        }
        ret.add(getAttributeDef(attrNamePrefix, AtlasBaseTypeDef.getArrayTypeName(
                AtlasBaseTypeDef.getMapTypeName(ENUM_DEF.getName(), getRandomBuiltInType()))));
        ret.add(getAttributeDef(attrNamePrefix, AtlasBaseTypeDef.getArrayTypeName(
                AtlasBaseTypeDef.getMapTypeName(ENUM_DEF_WITH_NO_DEFAULT.getName(), getRandomBuiltInType()))));
        ret.add(getAttributeDef(attrNamePrefix, AtlasBaseTypeDef.getArrayTypeName(
                AtlasBaseTypeDef.getMapTypeName(getRandomPrimitiveType(), ENUM_DEF.getName()))));
        ret.add(getAttributeDef(attrNamePrefix, AtlasBaseTypeDef.getArrayTypeName(
                AtlasBaseTypeDef.getMapTypeName(getRandomPrimitiveType(), ENUM_DEF_WITH_NO_DEFAULT.getName()))));

        // add few map of arrays
        for (String ATLAS_PRIMITIVE_TYPE1 : ATLAS_PRIMITIVE_TYPES) {
            ret.add(getAttributeDef(attrNamePrefix, AtlasBaseTypeDef.getMapTypeName(ATLAS_PRIMITIVE_TYPE1,
                    AtlasBaseTypeDef.getArrayTypeName(getRandomBuiltInType()))));
        }

        // add few map of maps
        for (String ATLAS_PRIMITIVE_TYPE : ATLAS_PRIMITIVE_TYPES) {
            ret.add(getAttributeDef(attrNamePrefix, AtlasBaseTypeDef.getMapTypeName(ATLAS_PRIMITIVE_TYPE,
                    AtlasBaseTypeDef.getMapTypeName(ATLAS_PRIMITIVE_TYPE, getRandomBuiltInType()))));
        }

        return ret;
    }

    /**
     * Valid types for attributes in relationships. This has good coverage of all attribute type and includes enums
     * maps and arrays.
     * This test does not use getRandomBuiltInType() style - so that it is deterministic.
     *
     * It does cover very nested maps / arrays.
     * @param attrNamePrefix
     * @return
     */
    public static List<AtlasAttributeDef> newAttributeDefsWithAllBuiltInTypesForRelationship(String attrNamePrefix) {
        List<AtlasAttributeDef> ret = new ArrayList<>();

        // add enum types
        ret.add(getAttributeDef(attrNamePrefix, ENUM_DEF.getName()));
        ret.add(getAttributeDef(attrNamePrefix, ENUM_DEF_WITH_NO_DEFAULT.getName()));
        // add array of enum types
        ret.add(getAttributeDef(attrNamePrefix, AtlasBaseTypeDef.getArrayTypeName(ENUM_DEF.getName())));
        ret.add(getAttributeDef(attrNamePrefix, AtlasBaseTypeDef.getArrayTypeName(ENUM_DEF_WITH_NO_DEFAULT.getName())));


        for (String attributeType : ATLAS_RELATIONSHIP_ATTRIBUTE_TYPES) {

            // simple attributes
            ret.add(getAttributeDef(attrNamePrefix, attributeType));
            // array
            ret.add(getAttributeDef(attrNamePrefix, AtlasBaseTypeDef.getArrayTypeName(attributeType)));
            // map types with enum as key
            ret.add(getAttributeDef(attrNamePrefix,
                    AtlasBaseTypeDef.getMapTypeName(ENUM_DEF.getName(), attributeType)));
            // map types with enum as key no default
            ret.add(getAttributeDef(attrNamePrefix,
                    AtlasBaseTypeDef.getMapTypeName(ENUM_DEF_WITH_NO_DEFAULT.getName(), attributeType)));
             // map types attribute as key enum no default as value
            ret.add(getAttributeDef(attrNamePrefix,
                    AtlasBaseTypeDef.getMapTypeName(attributeType, ENUM_DEF_WITH_NO_DEFAULT.getName())));
            // map types with enum as value
            ret.add(getAttributeDef(attrNamePrefix,
                    AtlasBaseTypeDef.getMapTypeName(attributeType, ENUM_DEF.getName())));

            for (String attributeType2 : ATLAS_RELATIONSHIP_ATTRIBUTE_TYPES) {
                // add map types
                ret.add(getAttributeDef(attrNamePrefix,
                        AtlasBaseTypeDef.getMapTypeName(attributeType, attributeType2)));
                // add array of arrays
                ret.add(getAttributeDef(attrNamePrefix,
                        AtlasBaseTypeDef.getArrayTypeName(AtlasBaseTypeDef.getArrayTypeName(attributeType2))));
                // add array of maps
                ret.add(getAttributeDef(attrNamePrefix, AtlasBaseTypeDef.getArrayTypeName(
                        AtlasBaseTypeDef.getMapTypeName(attributeType, attributeType2))));
                // add map of arrays
                ret.add(getAttributeDef(attrNamePrefix, AtlasBaseTypeDef.getMapTypeName(attributeType,
                        AtlasBaseTypeDef.getArrayTypeName(attributeType2))));
                // add map of maps
                ret.add(getAttributeDef(attrNamePrefix, AtlasBaseTypeDef.getMapTypeName(attributeType,
                        AtlasBaseTypeDef.getMapTypeName(attributeType, attributeType2))));
            }
        }

        return ret;
    }

    public static String getDefaultAttributeName(String attrType) {
        return PREFIX_ATTRIBUTE_NAME + attrType;
    }

    private static AtlasAttributeDef getAttributeDef(String attrNamePrefix, String attrType) {
        return new AtlasAttributeDef(attrNamePrefix + attrType, attrType);
    }

    private static String getRandomPrimitiveType() {
        return ATLAS_PRIMITIVE_TYPES[ThreadLocalRandom.current().nextInt(0, ATLAS_PRIMITIVE_TYPES.length)];
    }

    private static String getRandomBuiltInType() {
        return ATLAS_BUILTIN_TYPES[ThreadLocalRandom.current().nextInt(0, ATLAS_BUILTIN_TYPES.length)];
    }

    private static void removeAttributeIfExistedInSuperType(List<AtlasAttributeDef> currentTypeAttrDefs, List<AtlasAttributeDef> superTypeAttrDefs) {
        if (CollectionUtils.isNotEmpty(superTypeAttrDefs)) {
            List<String> superTypeAttrNames = superTypeAttrDefs.stream()
                    .map(superTypeObj -> superTypeObj.getName()).collect(Collectors.toList());

            currentTypeAttrDefs.removeIf(obj -> superTypeAttrNames.contains(obj.getName()));
        }
    }
}
