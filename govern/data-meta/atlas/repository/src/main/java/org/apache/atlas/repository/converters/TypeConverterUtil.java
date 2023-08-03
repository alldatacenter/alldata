/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.atlas.repository.converters;

import static org.apache.atlas.AtlasErrorCode.INVALID_TYPE_DEFINITION;
import static org.apache.atlas.model.typedef.AtlasStructDef.AtlasConstraintDef.CONSTRAINT_TYPE_OWNED_REF;
import static org.apache.atlas.model.typedef.AtlasStructDef.AtlasConstraintDef.CONSTRAINT_TYPE_INVERSE_REF;
import static org.apache.atlas.model.typedef.AtlasStructDef.AtlasConstraintDef.CONSTRAINT_PARAM_ATTRIBUTE;
import static org.apache.atlas.type.AtlasTypeUtil.isArrayType;

import java.util.*;

import org.apache.atlas.exception.AtlasBaseException;
import org.apache.atlas.model.typedef.AtlasClassificationDef;
import org.apache.atlas.model.typedef.AtlasEntityDef;
import org.apache.atlas.model.typedef.AtlasEnumDef;
import org.apache.atlas.model.typedef.AtlasEnumDef.AtlasEnumElementDef;
import org.apache.atlas.model.typedef.AtlasStructDef;
import org.apache.atlas.model.typedef.AtlasStructDef.AtlasAttributeDef;
import org.apache.atlas.model.typedef.AtlasStructDef.AtlasAttributeDef.Cardinality;
import org.apache.atlas.model.typedef.AtlasStructDef.AtlasConstraintDef;
import org.apache.atlas.model.typedef.AtlasTypeDefHeader;
import org.apache.atlas.model.typedef.AtlasTypesDef;
import org.apache.atlas.v1.model.typedef.AttributeDefinition;
import org.apache.atlas.v1.model.typedef.ClassTypeDefinition;
import org.apache.atlas.v1.model.typedef.EnumTypeDefinition;
import org.apache.atlas.v1.model.typedef.Multiplicity;
import org.apache.atlas.v1.model.typedef.StructTypeDefinition;
import org.apache.atlas.v1.model.typedef.TraitTypeDefinition;
import org.apache.atlas.v1.model.typedef.TypesDef;
import org.apache.atlas.repository.store.graph.v2.AtlasStructDefStoreV2;
import org.apache.atlas.type.AtlasClassificationType;
import org.apache.atlas.type.AtlasEntityType;
import org.apache.atlas.type.AtlasEnumType;
import org.apache.atlas.type.AtlasStructType;
import org.apache.atlas.type.AtlasStructType.AtlasAttribute;
import org.apache.atlas.type.AtlasType;
import org.apache.atlas.type.AtlasTypeRegistry;
import org.apache.atlas.type.AtlasTypeUtil;
import org.apache.atlas.v1.model.typedef.EnumTypeDefinition.EnumValue;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public final class TypeConverterUtil {
    private TypeConverterUtil() {}
    private static final Logger LOG = LoggerFactory.getLogger(TypeConverterUtil.class);

    public static TypesDef toTypesDef(AtlasType type, AtlasTypeRegistry typeRegistry) throws AtlasBaseException {
        final TypesDef ret;

        if (type instanceof AtlasEnumType) {
            ret = TypeConverterUtil.enumToTypesDef((AtlasEnumType) type);
        } else if (type instanceof AtlasEntityType) {
            ret = TypeConverterUtil.entityToTypesDef((AtlasEntityType) type, typeRegistry);
        } else if (type instanceof AtlasClassificationType) {
            ret = TypeConverterUtil.classificationToTypesDef((AtlasClassificationType) type, typeRegistry);
        } else if (type instanceof AtlasStructType) {
            ret = TypeConverterUtil.structToTypesDef((AtlasStructType) type, typeRegistry);
        } else {
            ret = new TypesDef();
        }

        return ret;
    }

    private static TypesDef enumToTypesDef(AtlasEnumType enumType) {
        AtlasEnumDef enumDef = enumType.getEnumDef();

        String          enumName    = enumDef.getName();
        String          enumDesc    = enumDef.getDescription();
        String          enumVersion = enumDef.getTypeVersion();
        List<EnumValue> enumValues  = getEnumValues(enumDef.getElementDefs());

        EnumTypeDefinition enumTypeDef = new EnumTypeDefinition(enumName, enumDesc, enumVersion, enumValues);

        TypesDef ret = new TypesDef(Arrays.asList(enumTypeDef), null, null, null);

        return ret;
    }

    private static TypesDef structToTypesDef(AtlasStructType structType, AtlasTypeRegistry registry) {
        String                    typeName    = structType.getStructDef().getName();
        String                    typeDesc    = structType.getStructDef().getDescription();
        String                    typeVersion = structType.getStructDef().getTypeVersion();
        List<AttributeDefinition> attributes  = getAttributes(structType, registry);

        StructTypeDefinition  structTypeDef = new StructTypeDefinition(typeName, typeDesc, typeVersion, attributes);

        TypesDef ret = new TypesDef(null, Arrays.asList(structTypeDef), null, null);

        return ret;
    }

    private static TypesDef entityToTypesDef(AtlasEntityType entityType, AtlasTypeRegistry registry) {
        String                    typeName    = entityType.getEntityDef().getName();
        String                    typeDesc    = entityType.getEntityDef().getDescription();
        String                    typeVersion = entityType.getEntityDef().getTypeVersion();
        Set<String>               superTypes  = entityType.getEntityDef().getSuperTypes();
        List<AttributeDefinition> attributes  = getAttributes(entityType, registry);

        ClassTypeDefinition classTypeDef = new ClassTypeDefinition(typeName, typeDesc, typeVersion, attributes, superTypes);

        TypesDef ret = new TypesDef(null, null, null, Arrays.asList(classTypeDef));

        return ret;
    }

    private static TypesDef classificationToTypesDef(AtlasClassificationType classificationType, AtlasTypeRegistry registry) {
        String                    typeName    = classificationType.getClassificationDef().getName();
        String                    typeDesc    = classificationType.getClassificationDef().getDescription();
        String                    typeVersion = classificationType.getClassificationDef().getTypeVersion();
        Set<String>               superTypes  = new HashSet<>(classificationType.getClassificationDef().getSuperTypes());
        List<AttributeDefinition> attributes  = getAttributes(classificationType, registry);

        TraitTypeDefinition traitTypeDef = new TraitTypeDefinition(typeName, typeDesc, typeVersion, attributes, superTypes);

        TypesDef ret = new TypesDef(null, null, Arrays.asList(traitTypeDef), null);

        return ret;
    }



    public static AtlasTypesDef toAtlasTypesDef(String typeDefinition, AtlasTypeRegistry registry) throws AtlasBaseException {
        AtlasTypesDef ret = new AtlasTypesDef();

        try {
            if (StringUtils.isEmpty(typeDefinition)) {
                throw new AtlasBaseException(INVALID_TYPE_DEFINITION, typeDefinition);
            }

            TypesDef typesDef = AtlasType.fromV1Json(typeDefinition, TypesDef.class);
            if (CollectionUtils.isNotEmpty(typesDef.getEnumTypes())) {
                List<AtlasEnumDef> enumDefs = toAtlasEnumDefs(typesDef.getEnumTypes());
                ret.setEnumDefs(enumDefs);
            }

            if (CollectionUtils.isNotEmpty(typesDef.getStructTypes())) {
                List<AtlasStructDef> structDefs = toAtlasStructDefs(typesDef.getStructTypes());
                ret.setStructDefs(structDefs);
            }

            if (CollectionUtils.isNotEmpty(typesDef.getClassTypes())) {
                List<AtlasEntityDef> entityDefs = toAtlasEntityDefs(typesDef.getClassTypes(), registry);
                ret.setEntityDefs(entityDefs);
            }

            if (CollectionUtils.isNotEmpty(typesDef.getTraitTypes())) {
                List<AtlasClassificationDef> classificationDefs = toAtlasClassificationDefs(typesDef.getTraitTypes());
                ret.setClassificationDefs(classificationDefs);
            }

        } catch (Exception e) {
            LOG.error("Invalid type definition = {}", typeDefinition, e);
            throw new AtlasBaseException(INVALID_TYPE_DEFINITION, typeDefinition);
        }

        return ret;
    }

    public static List<String> getTypeNames(List<AtlasTypeDefHeader> atlasTypesDefs) {
        List<String> ret = new ArrayList<String>();
        if (CollectionUtils.isNotEmpty(atlasTypesDefs)) {
            for (AtlasTypeDefHeader atlasTypesDef : atlasTypesDefs) {
                ret.add(atlasTypesDef.getName());
            }
        }

        return ret;
    }

    public static List<String> getTypeNames(AtlasTypesDef typesDef) {
        List<AtlasTypeDefHeader> atlasTypesDefs = AtlasTypeUtil.toTypeDefHeader(typesDef);
        return getTypeNames(atlasTypesDefs);
    }

    private static List<AtlasEnumDef> toAtlasEnumDefs(List<EnumTypeDefinition> enumTypeDefinitions) {
        List<AtlasEnumDef> ret = new ArrayList<AtlasEnumDef>();

        for (EnumTypeDefinition enumType : enumTypeDefinitions) {
            AtlasEnumDef enumDef = new AtlasEnumDef();
            enumDef.setName(enumType.getName());
            enumDef.setDescription(enumType.getDescription());
            enumDef.setTypeVersion(enumType.getVersion());
            enumDef.setElementDefs(getAtlasEnumElementDefs(enumType.getEnumValues()));

            ret.add(enumDef);
        }

        return ret;
    }

    private static List<AtlasStructDef> toAtlasStructDefs(List<StructTypeDefinition> structTypeDefinitions) {
        List<AtlasStructDef> ret = new ArrayList<>();

        for (StructTypeDefinition structType : structTypeDefinitions) {
            List<AtlasAttributeDef> attrDefs  = new ArrayList<AtlasAttributeDef>();

            if (CollectionUtils.isNotEmpty(structType.getAttributeDefinitions())) {
                for (AttributeDefinition attrDefinition : structType.getAttributeDefinitions()) {
                    attrDefs.add(toAtlasAttributeDef(attrDefinition));
                }
            }

            AtlasStructDef structDef = new AtlasStructDef(structType.getTypeName(), structType.getTypeDescription(), structType.getTypeVersion(), attrDefs);

            ret.add(structDef);
        }

        return ret;
    }

    private static List<AtlasClassificationDef> toAtlasClassificationDefs(List<TraitTypeDefinition> traitTypeDefinitions) {
        List<AtlasClassificationDef> ret = new ArrayList<>();

        for (TraitTypeDefinition traitType : traitTypeDefinitions) {
            List<AtlasAttributeDef> attrDefs   = new ArrayList<AtlasAttributeDef>();

            if (CollectionUtils.isNotEmpty(traitType.getAttributeDefinitions())) {
                for (AttributeDefinition attrDefinition : traitType.getAttributeDefinitions()) {
                    attrDefs.add(toAtlasAttributeDef(attrDefinition));
                }
            }

            AtlasClassificationDef classifDef = new AtlasClassificationDef(traitType.getTypeName(), traitType.getTypeDescription(), traitType.getTypeVersion(), attrDefs, traitType.getSuperTypes());

            ret.add(classifDef);
        }

        return ret;
    }

    private static List<AtlasEntityDef> toAtlasEntityDefs(List<ClassTypeDefinition> classTypeDefinitions, AtlasTypeRegistry registry) {
        List<AtlasEntityDef> ret = new ArrayList<>();

        for (ClassTypeDefinition classType : classTypeDefinitions) {
            List<AtlasAttributeDef> attrDefs         = new ArrayList<AtlasAttributeDef>();

            if (CollectionUtils.isNotEmpty(classType.getAttributeDefinitions())) {
                for (AttributeDefinition oldAttr : classType.getAttributeDefinitions()) {
                    AtlasAttributeDef newAttr = toAtlasAttributeDef(oldAttr);
                    attrDefs.add(newAttr);
                }
            }

            AtlasEntityDef entityDef = new AtlasEntityDef(classType.getTypeName(), classType.getTypeDescription(), classType.getTypeVersion(), attrDefs, classType.getSuperTypes());

            ret.add(entityDef);
        }

        return ret;
    }

    private static String getArrayTypeName(String attrType) {
        String ret = null;
        if (isArrayType(attrType)) {
            Set<String> typeNames = AtlasTypeUtil.getReferencedTypeNames(attrType);
            if (typeNames.size() > 0) {
                ret = typeNames.iterator().next();
            }
        }

        return ret;
    }

    private static List<AtlasEnumElementDef> getAtlasEnumElementDefs(List<EnumValue> enums) {
        List<AtlasEnumElementDef> ret = new ArrayList<>();

        for (EnumValue enumElem : enums) {
            ret.add(new AtlasEnumElementDef(enumElem.getValue(), null, enumElem.getOrdinal()));
        }

        return ret;
    }

    private static List<EnumValue> getEnumValues(List<AtlasEnumElementDef> enumDefs) {
        List<EnumValue> ret = new ArrayList<EnumValue>();

        if (CollectionUtils.isNotEmpty(enumDefs)) {
            for (AtlasEnumElementDef enumDef : enumDefs) {
                if (enumDef != null) {
                    ret.add(new EnumValue(enumDef.getValue(), enumDef.getOrdinal()));
                }
            }
        }

        return ret;
    }

    public static AtlasAttributeDef toAtlasAttributeDef(final AttributeDefinition attrDefinition) {
        AtlasAttributeDef ret = new AtlasAttributeDef(attrDefinition.getName(),
                                                      attrDefinition.getDataTypeName(),
                                                      attrDefinition.getSearchWeight(),
                                                      attrDefinition.getIndexType());

        ret.setIsIndexable(attrDefinition.getIsIndexable());
        ret.setIsUnique(attrDefinition.getIsUnique());
        if (attrDefinition.getIsComposite()) {
            ret.addConstraint(new AtlasConstraintDef(CONSTRAINT_TYPE_OWNED_REF));
        }

        if (StringUtils.isNotBlank(attrDefinition.getReverseAttributeName())) {
            ret.addConstraint(new AtlasConstraintDef(CONSTRAINT_TYPE_INVERSE_REF,
                                       new HashMap<String, Object>() {{
                                           put(CONSTRAINT_PARAM_ATTRIBUTE, attrDefinition.getReverseAttributeName());
                                       }}));
        }

        // Multiplicity attribute mapping
        Multiplicity multiplicity = attrDefinition.getMultiplicity();
        int          minCount     = multiplicity.getLower();
        int          maxCount     = multiplicity.getUpper();
        boolean      isUnique     = multiplicity.getIsUnique();

        if (minCount == 0) {
            ret.setIsOptional(true);
            ret.setValuesMinCount(0);
        } else {
            ret.setIsOptional(false);
            ret.setValuesMinCount(minCount);
        }

        if (maxCount < 2) {
            ret.setCardinality(Cardinality.SINGLE);
            ret.setValuesMaxCount(1);
        } else {
            if (!isUnique) {
                ret.setCardinality(Cardinality.LIST);
            } else {
                ret.setCardinality(Cardinality.SET);
            }

            ret.setValuesMaxCount(maxCount);
        }

        return ret;
    }

    private static List<AttributeDefinition> getAttributes(AtlasStructType structType, AtlasTypeRegistry registry) {
        List<AttributeDefinition> ret      = new ArrayList<>();
        List<AtlasAttributeDef>   attrDefs = structType.getStructDef().getAttributeDefs();

        if (CollectionUtils.isNotEmpty(attrDefs)) {
            for (AtlasAttributeDef attrDef : attrDefs) {
                AtlasAttribute attribute = structType.getAttribute(attrDef.getName());

                AttributeDefinition oldAttrDef = AtlasStructDefStoreV2.toAttributeDefinition(attribute);

                ret.add(new AttributeDefinition(oldAttrDef.getName(),
                                                oldAttrDef.getDataTypeName(),
                                                new Multiplicity(oldAttrDef.getMultiplicity()),
                                                oldAttrDef.getIsComposite(),
                                                oldAttrDef.getIsUnique(),
                                                oldAttrDef.getIsIndexable(),
                                                oldAttrDef.getReverseAttributeName(),
                                                oldAttrDef.getOptions(),
                                                oldAttrDef.getSearchWeight(),
                                                oldAttrDef.getIndexType()));
            }
        }

        return ret;
    }
}
