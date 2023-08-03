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

import org.apache.atlas.model.instance.AtlasClassification;
import org.apache.atlas.model.instance.AtlasEntity;
import org.apache.atlas.model.instance.AtlasEntityHeader;
import org.apache.atlas.model.instance.AtlasObjectId;
import org.apache.atlas.model.instance.AtlasRelatedObjectId;
import org.apache.atlas.model.instance.AtlasStruct;
import org.apache.atlas.model.typedef.AtlasBaseTypeDef;
import org.apache.atlas.model.typedef.AtlasClassificationDef;
import org.apache.atlas.model.typedef.AtlasEntityDef;
import org.apache.atlas.model.typedef.AtlasEnumDef;
import org.apache.atlas.model.typedef.AtlasEnumDef.AtlasEnumElementDef;
import org.apache.atlas.model.typedef.AtlasBusinessMetadataDef;
import org.apache.atlas.model.typedef.AtlasRelationshipDef;
import org.apache.atlas.model.typedef.AtlasRelationshipDef.PropagateTags;
import org.apache.atlas.model.typedef.AtlasRelationshipDef.RelationshipCategory;
import org.apache.atlas.model.typedef.AtlasRelationshipEndDef;
import org.apache.atlas.model.typedef.AtlasStructDef;
import org.apache.atlas.model.typedef.AtlasStructDef.AtlasAttributeDef;
import org.apache.atlas.model.typedef.AtlasStructDef.AtlasAttributeDef.Cardinality;
import org.apache.atlas.model.typedef.AtlasStructDef.AtlasConstraintDef;
import org.apache.atlas.model.typedef.AtlasTypeDefHeader;
import org.apache.atlas.model.typedef.AtlasTypesDef;
import org.apache.atlas.type.AtlasStructType.AtlasAttribute;
import org.apache.atlas.v1.model.typedef.AttributeDefinition;
import org.apache.atlas.v1.model.typedef.ClassTypeDefinition;
import org.apache.atlas.v1.model.typedef.Multiplicity;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.StringUtils;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.apache.atlas.model.typedef.AtlasBaseTypeDef.*;

/**
 * Utility methods for AtlasType/AtlasTypeDef.
 */
public class AtlasTypeUtil {

    private static final Set<String> ATLAS_BUILTIN_TYPENAMES = new HashSet<>();
    private static final String  NAME_REGEX         = "[a-zA-Z][a-zA-Z0-9_ ]*";
    private static final String  TRAIT_NAME_REGEX   = "[a-zA-Z][a-zA-Z0-9_ .]*";
    private static final Pattern NAME_PATTERN       = Pattern.compile(NAME_REGEX);
    private static final Pattern TRAIT_NAME_PATTERN = Pattern.compile(TRAIT_NAME_REGEX);

    private static final String InvalidTypeNameErrorMessage      = "Name must consist of a letter followed by a sequence of [ letter, number, '_' ] characters.";
    private static final String InvalidTraitTypeNameErrorMessage = "Name must consist of a letter followed by a sequence of [ letter,  number, '_', '.' ] characters.";

    public static final String ATTRIBUTE_QUALIFIED_NAME = "qualifiedName";

    static {
        Collections.addAll(ATLAS_BUILTIN_TYPENAMES, AtlasBaseTypeDef.ATLAS_BUILTIN_TYPES);
    }

    public static Set<String> getReferencedTypeNames(String typeName) {
        Set<String> ret = new HashSet<>();

        getReferencedTypeNames(typeName, ret);

        return ret;
    }

    public static boolean isBuiltInType(String typeName) {
        return ATLAS_BUILTIN_TYPENAMES.contains(typeName);
    }

    public static boolean isArrayType(String typeName) {
        return StringUtils.startsWith(typeName, ATLAS_TYPE_ARRAY_PREFIX)
            && StringUtils.endsWith(typeName, ATLAS_TYPE_ARRAY_SUFFIX);
    }

    public static boolean isMapType(String typeName) {
        return StringUtils.startsWith(typeName, ATLAS_TYPE_MAP_PREFIX)
            && StringUtils.endsWith(typeName, ATLAS_TYPE_MAP_SUFFIX);
    }

    public static boolean isValidTypeName(String typeName) {
        Matcher m = NAME_PATTERN.matcher(typeName);

        return m.matches();
    }

    public static String getInvalidTypeNameErrorMessage() {
        return InvalidTypeNameErrorMessage;
    }

    public static boolean isValidTraitTypeName(String typeName) {
        Matcher m = TRAIT_NAME_PATTERN.matcher(typeName);

        return m.matches();
    }

    public static String getInvalidTraitTypeNameErrorMessage() {
        return InvalidTraitTypeNameErrorMessage;
    }

    public static String getStringValue(Map map, Object key) {
        Object ret = map != null ? map.get(key) : null;

        return ret != null ? ret.toString() : null;
    }

    private static void getReferencedTypeNames(String typeName, Set<String> referencedTypeNames) {
        if (StringUtils.isNotBlank(typeName) && !referencedTypeNames.contains(typeName)) {
            if (typeName.startsWith(ATLAS_TYPE_ARRAY_PREFIX) && typeName.endsWith(ATLAS_TYPE_ARRAY_SUFFIX)) {
                int    startIdx        = ATLAS_TYPE_ARRAY_PREFIX.length();
                int    endIdx          = typeName.length() - ATLAS_TYPE_ARRAY_SUFFIX.length();
                String elementTypeName = typeName.substring(startIdx, endIdx);

                getReferencedTypeNames(elementTypeName, referencedTypeNames);
            } else if (typeName.startsWith(ATLAS_TYPE_MAP_PREFIX) && typeName.endsWith(ATLAS_TYPE_MAP_SUFFIX)) {
                int      startIdx      = ATLAS_TYPE_MAP_PREFIX.length();
                int      endIdx        = typeName.length() - ATLAS_TYPE_MAP_SUFFIX.length();
                String[] keyValueTypes = typeName.substring(startIdx, endIdx).split(ATLAS_TYPE_MAP_KEY_VAL_SEP, 2);
                String   keyTypeName   = keyValueTypes.length > 0 ? keyValueTypes[0] : null;
                String   valueTypeName = keyValueTypes.length > 1 ? keyValueTypes[1] : null;

                getReferencedTypeNames(keyTypeName, referencedTypeNames);
                getReferencedTypeNames(valueTypeName, referencedTypeNames);
            } else {
                referencedTypeNames.add(typeName);
            }
        }

    }

    public static AtlasRelationshipType findRelationshipWithLegacyRelationshipEnd(String entityTypeName, String attributeName, AtlasTypeRegistry typeRegistry) {
        AtlasRelationshipType ret = null;

        for (AtlasRelationshipDef relationshipDef : typeRegistry.getAllRelationshipDefs()) {
            AtlasRelationshipEndDef end1Def = relationshipDef.getEndDef1();
            AtlasRelationshipEndDef end2Def = relationshipDef.getEndDef2();

            if ((end1Def.getIsLegacyAttribute() && StringUtils.equals(end1Def.getType(), entityTypeName) && StringUtils.equals(end1Def.getName(), attributeName)) ||
                (end2Def.getIsLegacyAttribute() && StringUtils.equals(end2Def.getType(), entityTypeName) && StringUtils.equals(end2Def.getName(), attributeName))) {
                ret = typeRegistry.getRelationshipTypeByName(relationshipDef.getName());

                break;
            }
        }

        return ret;
    }

    public static AtlasAttributeDef createOptionalAttrDef(String name, AtlasType dataType) {
        return new AtlasAttributeDef(name, dataType.getTypeName(), true,
            Cardinality.SINGLE, 0, 1,
            false, false, false,
            Collections.<AtlasConstraintDef>emptyList());
    }

    public static AtlasAttributeDef createOptionalAttrDef(String name, String dataType) {
        return new AtlasAttributeDef(name, dataType, true,
            Cardinality.SINGLE, 0, 1,
            false, false, false,
            Collections.<AtlasConstraintDef>emptyList());
    }

    public static AtlasAttributeDef createOptionalAttrDef(String name, String dataType, Map<String, String> options, String desc) {
        return new AtlasAttributeDef(name, dataType, true,
                Cardinality.SINGLE, 0, 1,
                false, false, false, "",
                Collections.<AtlasConstraintDef>emptyList(), options, desc, 0, null);
    }

    public static AtlasAttributeDef createRequiredAttrDef(String name, String dataType) {
        return new AtlasAttributeDef(name, dataType, false,
            Cardinality.SINGLE, 1, 1,
            false, true, false,
            Collections.<AtlasConstraintDef>emptyList());
    }

    public static AtlasAttributeDef createListRequiredAttrDef(String name, String dataType) {
        return new AtlasAttributeDef(name, dataType, false,
                Cardinality.LIST, 1, Integer.MAX_VALUE,
                false, true, false,
                Collections.<AtlasConstraintDef>emptyList());
    }

    public static AtlasAttributeDef createOptionalListAttrDef(String name, String dataType) {
        return new AtlasAttributeDef(name, dataType, true,
                Cardinality.LIST, 1, Integer.MAX_VALUE,
                false, true, false,
                Collections.<AtlasConstraintDef>emptyList());
    }

    public static AtlasAttributeDef createRequiredListAttrDefWithConstraint(String name, String dataType, String type, Map param) {
        AtlasAttributeDef ret = AtlasTypeUtil.createListRequiredAttrDef(name, dataType);
        ret.addConstraint(new AtlasConstraintDef(type, param));

        return ret;
    }

    public static AtlasAttributeDef createRequiredAttrDefWithConstraint(String name, String typeName, String type, Map param) {
        AtlasAttributeDef ret = AtlasTypeUtil.createRequiredAttrDef(name, typeName);
        ret.addConstraint(new AtlasConstraintDef(type, param));

        return ret;
    }

    public static AtlasAttributeDef createOptionalAttrDefWithConstraint(String name, String typeName, String type, Map param) {
        AtlasAttributeDef ret = AtlasTypeUtil.createOptionalAttrDef(name, typeName);
        ret.addConstraint(new AtlasConstraintDef(type, param));

        return ret;
    }

    public static AtlasAttributeDef createUniqueRequiredAttrDef(String name, AtlasType dataType) {
        return new AtlasAttributeDef(name, dataType.getTypeName(), false,
            Cardinality.SINGLE, 1, 1,
            true, true, false,
            Collections.<AtlasConstraintDef>emptyList());
    }

    public static AtlasAttributeDef createUniqueRequiredAttrDef(String name, String typeName) {
        return new AtlasAttributeDef(name, typeName, false,
            Cardinality.SINGLE, 1, 1,
            true, true, false,
            Collections.<AtlasConstraintDef>emptyList());
    }

    public static AtlasAttributeDef createRequiredAttrDef(String name, AtlasType dataType) {
        return new AtlasAttributeDef(name, dataType.getTypeName(), false,
            Cardinality.SINGLE, 1, 1,
            false, true, false,
            Collections.<AtlasConstraintDef>emptyList());
    }

    public static AtlasEnumDef createEnumTypeDef(String name, String description, AtlasEnumElementDef... enumValues) {
        return new AtlasEnumDef(name, description, "1.0", Arrays.asList(enumValues));
    }

    public static AtlasClassificationDef createTraitTypeDef(String name, Set<String> superTypes, AtlasAttributeDef... attrDefs) {
        return createTraitTypeDef(name, null, superTypes, attrDefs);
    }

    public static AtlasClassificationDef createTraitTypeDef(String name, String description, Set<String> superTypes, AtlasAttributeDef... attrDefs) {
        return createTraitTypeDef(name, description, "1.0", superTypes, attrDefs);
    }

    public static AtlasClassificationDef createTraitTypeDef(String name, String description, String version, Set<String> superTypes, AtlasAttributeDef... attrDefs) {
        return new AtlasClassificationDef(name, description, version, Arrays.asList(attrDefs), superTypes);
    }

    public static AtlasClassificationDef createAtlasClassificationDef(String name, String description, String version, Set<String> superTypes, Set<String> entityTypes, AtlasAttributeDef... attrDefs) {
        return new AtlasClassificationDef(name, description, version, Arrays.asList(attrDefs), superTypes, entityTypes, null);
    }

    public static AtlasStructDef createStructTypeDef(String name, AtlasAttributeDef... attrDefs) {
        return createStructTypeDef(name, null, attrDefs);
    }

    public static AtlasStructDef createStructTypeDef(String name, String description, AtlasAttributeDef... attrDefs) {
        return new AtlasStructDef(name, description, "1.0", Arrays.asList(attrDefs));
    }

    public static AtlasEntityDef createClassTypeDef(String name, Set<String> superTypes, AtlasAttributeDef... attrDefs) {
        return createClassTypeDef(name, null, "1.0", superTypes, attrDefs);
    }

    public static AtlasEntityDef createClassTypeDef(String name, String description, Set<String> superTypes, AtlasAttributeDef... attrDefs) {
        return createClassTypeDef(name, description, "1.0", superTypes, attrDefs);
    }

    public static AtlasEntityDef createClassTypeDef(String name, String description, String version, Set<String> superTypes, AtlasAttributeDef... attrDefs) {
        return new AtlasEntityDef(name, description, version, Arrays.asList(attrDefs), superTypes);
    }

    public static AtlasEntityDef createClassTypeDef(String name, String description, String version, Set<String> superTypes, Map<String, String> options, AtlasAttributeDef... attrDefs) {
        return new AtlasEntityDef(name, description, version, Arrays.asList(attrDefs), superTypes, options);
    }

    public static AtlasBusinessMetadataDef createBusinessMetadataDef(String name, String description, String typeVersion, AtlasAttributeDef... attributeDefs) {
        if (attributeDefs == null || attributeDefs.length == 0) {
            return new AtlasBusinessMetadataDef(name, description, typeVersion);
        }
        return new AtlasBusinessMetadataDef(name, description, typeVersion, Arrays.asList(attributeDefs));
    }

    public static AtlasRelationshipDef createRelationshipTypeDef(String                  name,
                                                                 String                  description,
                                                                 String                  version,
                                                                 RelationshipCategory    relationshipCategory,
                                                                 PropagateTags           propagateTags,
                                                                 AtlasRelationshipEndDef endDef1,
                                                                 AtlasRelationshipEndDef endDef2,
                                                                 AtlasAttributeDef...    attrDefs) {
        return new AtlasRelationshipDef(name, description, version, relationshipCategory, propagateTags,
                                        endDef1, endDef2, Arrays.asList(attrDefs));
    }

    public static AtlasRelationshipEndDef createRelationshipEndDef(String typeName, String name, Cardinality cardinality, boolean isContainer) {
        return new AtlasRelationshipEndDef(typeName, name, cardinality, isContainer);
    }

    public static AtlasTypesDef getTypesDef(List<AtlasEnumDef> enums,
        List<AtlasStructDef> structs,
        List<AtlasClassificationDef> traits,
        List<AtlasEntityDef> classes) {
        return new AtlasTypesDef(enums, structs, traits, classes);
    }

    public static AtlasTypesDef getTypesDef(List<AtlasEnumDef> enums,
                                            List<AtlasStructDef> structs,
                                            List<AtlasClassificationDef> traits,
                                            List<AtlasEntityDef> classes,
                                            List<AtlasRelationshipDef> relations) {
        return new AtlasTypesDef(enums, structs, traits, classes, relations);
    }

    public static AtlasTypesDef getTypesDef(List<AtlasEnumDef> enums,
                                            List<AtlasStructDef> structs,
                                            List<AtlasClassificationDef> traits,
                                            List<AtlasEntityDef> classes,
                                            List<AtlasRelationshipDef> relations,
                                            List<AtlasBusinessMetadataDef> businessMetadataDefs) {
        return new AtlasTypesDef(enums, structs, traits, classes, relations, businessMetadataDefs);
    }

    public static List<AtlasTypeDefHeader> toTypeDefHeader(AtlasTypesDef typesDef) {
        List<AtlasTypeDefHeader> headerList = new LinkedList<>();
        if (CollectionUtils.isNotEmpty(typesDef.getEnumDefs())) {
            for (AtlasEnumDef enumDef : typesDef.getEnumDefs()) {
                headerList.add(new AtlasTypeDefHeader(enumDef));
            }
        }
        if (CollectionUtils.isNotEmpty(typesDef.getStructDefs())) {
            for (AtlasStructDef structDef : typesDef.getStructDefs()) {
                headerList.add(new AtlasTypeDefHeader(structDef));
            }
        }
        if (CollectionUtils.isNotEmpty(typesDef.getClassificationDefs())) {
            for (AtlasClassificationDef classificationDef : typesDef.getClassificationDefs()) {
                headerList.add(new AtlasTypeDefHeader(classificationDef));
            }
        }
        if (CollectionUtils.isNotEmpty(typesDef.getEntityDefs())) {
            for (AtlasEntityDef entityDef : typesDef.getEntityDefs()) {
                headerList.add(new AtlasTypeDefHeader(entityDef));
            }
        }
        if (CollectionUtils.isNotEmpty(typesDef.getRelationshipDefs())) {
            for (AtlasRelationshipDef relationshipDef : typesDef.getRelationshipDefs()) {
                headerList.add(new AtlasTypeDefHeader(relationshipDef));
            }
        }
        if (CollectionUtils.isNotEmpty(typesDef.getBusinessMetadataDefs())) {
            for (AtlasBusinessMetadataDef businessMetadataDef : typesDef.getBusinessMetadataDefs()) {
                headerList.add(new AtlasTypeDefHeader(businessMetadataDef));
            }
        }

        return headerList;
    }

    public static AtlasTypesDef getTypesDef(AtlasBaseTypeDef typeDef) {
        AtlasTypesDef ret = new AtlasTypesDef();

        if (typeDef != null) {
            if (typeDef.getClass().equals(AtlasEntityDef.class)) {
                ret.getEntityDefs().add((AtlasEntityDef) typeDef);
            } else if (typeDef.getClass().equals(AtlasRelationshipDef.class)) {
                ret.getRelationshipDefs().add((AtlasRelationshipDef) typeDef);
            } else if (typeDef.getClass().equals(AtlasClassificationDef.class)) {
                ret.getClassificationDefs().add((AtlasClassificationDef) typeDef);
            } else if (typeDef.getClass().equals(AtlasStructDef.class)) {
                ret.getStructDefs().add((AtlasStructDef) typeDef);
            } else if (typeDef.getClass().equals(AtlasEnumDef.class)) {
                ret.getEnumDefs().add((AtlasEnumDef) typeDef);
            }
        }

        return ret;
    }

    public static Collection<AtlasObjectId> toObjectIds(Collection<AtlasEntity> entities) {
        List<AtlasObjectId> ret = new ArrayList<>();

        if (CollectionUtils.isNotEmpty(entities)) {
            for (AtlasEntity entity : entities) {
                if (entity != null) {
                    ret.add(AtlasTypeUtil.getAtlasObjectId(entity));
                }
            }
        }

        return ret;
    }

    public static Collection<AtlasRelatedObjectId> toAtlasRelatedObjectIds(Collection<AtlasEntity> entities) {
        List<AtlasRelatedObjectId> ret = new ArrayList<>();

        if (CollectionUtils.isNotEmpty(entities)) {
            for (AtlasEntity entity : entities) {
                if (entity != null) {
                    ret.add(toAtlasRelatedObjectId(entity));
                }
            }
        }

        return ret;
    }

    public static Map toStructAttributes(Map map) {
        if (map != null && map.containsKey("typeName") && map.containsKey("attributes") && map.get("attributes") instanceof Map) {
            return (Map)map.get("attributes");
        }

        return map;
    }

    public static Map toRelationshipAttributes(Map map) {
        Map ret = null;

        if (map != null && map.containsKey("typeName") && map.containsKey("relationshipAttributes") && map.get("relationshipAttributes") instanceof Map) {
            ret = (Map) map.get("relationshipAttributes");
        }

        return ret;
    }

    public static AtlasRelatedObjectId toAtlasRelatedObjectId(AtlasEntity entity) {
        return new AtlasRelatedObjectId(getAtlasObjectId(entity));
    }

    public static AtlasRelatedObjectId toAtlasRelatedObjectId(AtlasEntity entity, String relationshipType){
        return new AtlasRelatedObjectId(getAtlasObjectId(entity), relationshipType);
    }

    public static AtlasRelatedObjectId toAtlasRelatedObjectId(AtlasEntity entity, AtlasTypeRegistry typeRegistry) {
        return new AtlasRelatedObjectId(getAtlasObjectId(entity, typeRegistry));
    }

    public static AtlasObjectId getAtlasObjectId(AtlasEntity entity) {
        return new AtlasObjectId(entity.getGuid(), entity.getTypeName());
    }

    public static AtlasObjectId getAtlasObjectId(AtlasEntity entity, AtlasTypeRegistry typeRegistry) {
        String              typeName       = entity.getTypeName();
        AtlasEntityType     entityType     = typeRegistry.getEntityTypeByName(typeName);
        Map<String, Object> uniqAttributes = null;

        if (entityType != null && MapUtils.isNotEmpty(entityType.getUniqAttributes())) {
            for (AtlasAttribute attribute : entityType.getUniqAttributes().values()) {
                Object attrValue = entity.getAttribute(attribute.getName());

                if (attrValue != null) {
                    if (uniqAttributes == null) {
                        uniqAttributes = new HashMap<>();
                    }

                    uniqAttributes.put(attribute.getName(), attrValue);
                }
            }
        }

        return new AtlasObjectId(entity.getGuid(), typeName, uniqAttributes);
    }

    public static AtlasObjectId getAtlasObjectId(AtlasEntityHeader header) {
        return new AtlasObjectId(header.getGuid(), header.getTypeName());
    }

    public static List<AtlasObjectId> getAtlasObjectIds(List<AtlasEntity> entities) {
        final List<AtlasObjectId> ret;

        if (CollectionUtils.isNotEmpty(entities)) {
            ret = new ArrayList<>(entities.size());

            for (AtlasEntity entity : entities) {
                ret.add(getAtlasObjectId(entity));
            }
        } else {
            ret = new ArrayList<>();
        }

        return ret;
    }

    public static AtlasRelatedObjectId getAtlasRelatedObjectId(AtlasEntity entity, String relationshipType) {
        return getAtlasRelatedObjectId(getObjectId(entity), relationshipType);
    }

    public static AtlasRelatedObjectId getAtlasRelatedObjectId(AtlasObjectId objectId, String relationShipType) {
        AtlasRelatedObjectId atlasRelatedObjectId = new AtlasRelatedObjectId(objectId, relationShipType);
        return atlasRelatedObjectId;
    }

    public static List<AtlasRelatedObjectId> getAtlasRelatedObjectIds(List<AtlasEntity> entities, String relationshipType) {
        final List<AtlasRelatedObjectId> ret;
        if (CollectionUtils.isNotEmpty(entities)) {
            ret = new ArrayList<>(entities.size());
            for (AtlasEntity entity : entities) {
                ret.add(getAtlasRelatedObjectId(entity, relationshipType));
            }
        } else {
            ret = Collections.emptyList();
        }
        return ret;
    }

    public static List<AtlasRelatedObjectId> getAtlasRelatedObjectIdList(List<AtlasObjectId> atlasObjectIds, String relationshipType) {
        final List<AtlasRelatedObjectId> ret;
        if (CollectionUtils.isNotEmpty(atlasObjectIds)) {
            ret = new ArrayList<>(atlasObjectIds.size());
            for (AtlasObjectId atlasObjectId : atlasObjectIds) {
                ret.add(getAtlasRelatedObjectId(atlasObjectId, relationshipType));
            }
        } else {
            ret = Collections.emptyList();
        }
        return ret;
    }

    public static AtlasObjectId getObjectId(AtlasEntity entity) {
        String qualifiedName = (String) entity.getAttribute(ATTRIBUTE_QUALIFIED_NAME);
        AtlasObjectId ret = new AtlasObjectId(entity.getGuid(), entity.getTypeName(), Collections.singletonMap(ATTRIBUTE_QUALIFIED_NAME, qualifiedName));

        return ret;
    }

    public static boolean isValidGuid(AtlasObjectId objId) {
        return isValidGuid(objId.getGuid());
    }

    public static boolean isAssignedGuid(AtlasObjectId objId) {
        return isAssignedGuid(objId.getGuid());
    }

    public static boolean isUnAssignedGuid(AtlasObjectId objId) {
        return isUnAssignedGuid(objId.getGuid());
    }

    public static boolean isValidGuid(String guid) {
        return isAssignedGuid(guid) || isUnAssignedGuid(guid);
    }

    public static boolean isAssignedGuid(String guid) {
        /**
         * The rule for whether a GUID is 'assigned' is that it must always be non-null, non-empty
         * and must not start with a '-' character, because in Atlas the '-' prefix character
         * signifies an Atlas 'unassigned' GUID. There are no other GUID formatting constraints.
         *
         * An object from a remote repository can be saved into Atlas with its existing (external) GUID
         * if that GUID conforms to the same 3 conditions. If, in future, it is required to save objects from
         * a remote repository that assigns GUIDs that can start with the '-' character, then it will be
         * necessary to enhance this isAssignedGUID() method to accepts and check the object's homeId, such
         * that if homeId is not null (the object is from a remote repository), then the '-' prefix constraint
         * is relaxed. Such a change would require a pervasive change to Atlas classes and therefore should
         * only be implemented if it is found to be necessary.
         */
        if (guid != null) {
            return guid != null && guid.length() > 0 && guid.charAt(0) != '-';
        }
        return false;
    }

    public static boolean isUnAssignedGuid(String guid) {
        return guid != null && guid.length() > 0 && guid.charAt(0) == '-';
    }

    public static boolean isValid(AtlasObjectId objId) {
        if (isAssignedGuid(objId) || isUnAssignedGuid(objId)) {
            return true;
        } else if (StringUtils.isNotEmpty(objId.getTypeName()) && MapUtils.isNotEmpty(objId.getUniqueAttributes())) {
            return true;
        }

        return false;
    }

    public static String toDebugString(AtlasTypesDef typesDef) {
        StringBuilder sb = new StringBuilder();

        sb.append("typesDef={");
        if (typesDef != null) {
            sb.append("enumDefs=[");
            dumpTypeNames(typesDef.getEnumDefs(), sb);
            sb.append("],");

            sb.append("structDefs=[");
            dumpTypeNames(typesDef.getStructDefs(), sb);
            sb.append("],");

            sb.append("classificationDefs=[");
            dumpTypeNames(typesDef.getClassificationDefs(), sb);
            sb.append("],");

            sb.append("entityDefs=[");
            dumpTypeNames(typesDef.getEntityDefs(), sb);
            sb.append("]");

            sb.append("relationshipDefs=[");
            dumpTypeNames(typesDef.getRelationshipDefs(), sb);
            sb.append("]");
        }
        sb.append("}");

        return sb.toString();
    }

    public static ClassTypeDefinition toClassTypeDefinition(final AtlasEntityType entityType) {
        ClassTypeDefinition ret = null;

        if (entityType != null) {
            AtlasEntityDef entityDef = entityType.getEntityDef();
            ret = new ClassTypeDefinition();
            ret.setTypeName(entityDef.getName());
            ret.setTypeDescription(entityDef.getDescription());
            ret.setTypeVersion(entityDef.getTypeVersion());
            ret.setSuperTypes(entityDef.getSuperTypes());

            if (MapUtils.isNotEmpty(entityType.getAllAttributes())) {
                List<AttributeDefinition> attributeDefinitions = entityType.getAllAttributes()
                                                                           .entrySet()
                                                                           .stream()
                                                                           .map(e -> toV1AttributeDefinition(e.getValue()))
                                                                           .collect(Collectors.toList());

                ret.setAttributeDefinitions(attributeDefinitions);
            }
        }

        return ret;
    }

    public static AttributeDefinition toV1AttributeDefinition(AtlasStructType.AtlasAttribute attribute) {
        AtlasAttributeDef   attributeDef = attribute.getAttributeDef();
        AttributeDefinition ret = new AttributeDefinition();

        ret.setName(attributeDef.getName());
        ret.setDataTypeName(attributeDef.getTypeName());
        ret.setIsUnique(attributeDef.getIsUnique());
        ret.setIsIndexable(attributeDef.getIsIndexable());
        ret.setIsComposite(attribute.isOwnedRef());
        ret.setReverseAttributeName(attribute.getInverseRefAttributeName());
        ret.setDefaultValue(attributeDef.getDefaultValue());
        ret.setDescription(attributeDef.getDescription());
        ret.setOptions(attributeDef.getOptions());
        ret.setMultiplicity(getMultiplicity(attributeDef));

        return ret;
    }

    public static Multiplicity getMultiplicity(AtlasAttributeDef attributeDef) {
        int lower;
        int upper;
        if (attributeDef.getCardinality() == Cardinality.SINGLE) {
            lower = attributeDef.getIsOptional() ? 0 : 1;
            upper = 1;
        } else {
            if(attributeDef.getIsOptional()) {
                lower = 0;
            } else {
                lower = attributeDef.getValuesMinCount() < 1 ? 1 : attributeDef.getValuesMinCount();
            }

            upper = attributeDef.getValuesMaxCount() < 2 ? Integer.MAX_VALUE : attributeDef.getValuesMaxCount();
        }

        return new Multiplicity(lower, upper, Cardinality.SET.equals(attributeDef.getCardinality()));
    }

    public static Map<String, Object> toMap(AtlasEntity entity) {
        Map<String, Object> ret = null;

        if (entity != null) {
            ret = new LinkedHashMap<>();

            // Id type
            ret.put("$typeName$", entity.getTypeName());
            ret.put("$id$", new LinkedHashMap<String, Object>(){{
                put("id", entity.getGuid());
                put("$typeName$", entity.getTypeName());
                put("version", entity.getVersion().intValue());
                put("state", entity.getStatus().name());
            }});

            // System attributes
            ret.put("$systemAttributes$", new LinkedHashMap<String, String>() {{
                put("createdBy", entity.getCreatedBy());
                put("modifiedBy", entity.getUpdatedBy());
                put("createdTime", entity.getCreateTime().toString());
                put("modifiedTime", entity.getUpdateTime().toString());
            }});

            // Traits
            if (CollectionUtils.isNotEmpty(entity.getClassifications())) {
                Map<String, HashMap> traitDetails = entity.getClassifications()
                                                          .stream()
                                                          .collect(Collectors.toMap(AtlasStruct::getTypeName, AtlasTypeUtil::getNestedTraitDetails));
                ret.put("$traits$", traitDetails);
            }

            // All attributes
            if (MapUtils.isNotEmpty(entity.getAttributes())) {
                for (Map.Entry<String, Object> entry : entity.getAttributes().entrySet()) {
                    if (entry.getValue() instanceof AtlasObjectId) {
                        ret.put(entry.getKey(), new LinkedHashMap<String, Object>(){{
                            put("id", ((AtlasObjectId) entry.getValue()).getGuid());
                            put("$typeName$", ((AtlasObjectId) entry.getValue()).getTypeName());
//                        put("version", entity.getVersion().intValue());
//                        put("state", entity.getStatus().name());
                        }});
                    } else {
                        ret.put(entry.getKey(), entry.getValue());
                    }
                }
            }

        }

        return ret;
    }

    private static HashMap getNestedTraitDetails(final AtlasClassification atlasClassification) {
        return new HashMap<String, Object>() {{
            put("$typeName$", atlasClassification.getTypeName());

            if (MapUtils.isNotEmpty(atlasClassification.getAttributes())) {
                putAll(atlasClassification.getAttributes());
            }
        }};
    }

    private static void dumpTypeNames(List<? extends AtlasBaseTypeDef> typeDefs, StringBuilder sb) {
        if (CollectionUtils.isNotEmpty(typeDefs)) {
            for (int i = 0; i < typeDefs.size(); i++) {
                AtlasBaseTypeDef typeDef = typeDefs.get(i);

                if (i > 0) {
                    sb.append(",");
                }

                sb.append(typeDef.getName());
            }
        }
    }
}