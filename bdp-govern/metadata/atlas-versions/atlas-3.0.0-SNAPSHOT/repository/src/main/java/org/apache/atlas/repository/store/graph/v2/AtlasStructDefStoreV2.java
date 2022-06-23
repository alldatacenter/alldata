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
package org.apache.atlas.repository.store.graph.v2;

import com.google.common.annotations.VisibleForTesting;
import org.apache.atlas.AtlasErrorCode;
import org.apache.atlas.RequestContext;
import org.apache.atlas.authorize.AtlasPrivilege;
import org.apache.atlas.authorize.AtlasTypeAccessRequest;
import org.apache.atlas.authorize.AtlasAuthorizationUtils;
import org.apache.atlas.exception.AtlasBaseException;
import org.apache.atlas.model.typedef.AtlasStructDef;
import org.apache.atlas.model.typedef.AtlasStructDef.AtlasAttributeDef;
import org.apache.atlas.model.typedef.AtlasStructDef.AtlasConstraintDef;
import org.apache.atlas.v1.model.typedef.AttributeDefinition;
import org.apache.atlas.repository.Constants;
import org.apache.atlas.repository.graphdb.AtlasVertex;
import org.apache.atlas.type.AtlasRelationshipType;
import org.apache.atlas.type.AtlasStructType;
import org.apache.atlas.type.AtlasStructType.AtlasAttribute;
import org.apache.atlas.type.AtlasType;
import org.apache.atlas.type.AtlasTypeRegistry;
import org.apache.atlas.type.AtlasTypeUtil;
import org.apache.atlas.typesystem.types.DataTypes.TypeCategory;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.apache.atlas.type.AtlasStructType.AtlasAttribute.encodePropertyKey;

/**
 * StructDef store in v1 format.
 */
public class AtlasStructDefStoreV2 extends AtlasAbstractDefStoreV2<AtlasStructDef> {
    private static final Logger LOG = LoggerFactory.getLogger(AtlasStructDefStoreV2.class);

    public AtlasStructDefStoreV2(AtlasTypeDefGraphStoreV2 typeDefStore, AtlasTypeRegistry typeRegistry) {
        super(typeDefStore, typeRegistry);
    }

    @Override
    public AtlasVertex preCreate(AtlasStructDef structDef) throws AtlasBaseException {
        if (LOG.isDebugEnabled()) {
            LOG.debug("==> AtlasStructDefStoreV1.preCreate({})", structDef);
        }

        validateType(structDef);

        AtlasType type = typeRegistry.getType(structDef.getName());

        if (type.getTypeCategory() != org.apache.atlas.model.TypeCategory.STRUCT) {
            throw new AtlasBaseException(AtlasErrorCode.TYPE_MATCH_FAILED, structDef.getName(), TypeCategory.STRUCT.name());
        }

        AtlasAuthorizationUtils.verifyAccess(new AtlasTypeAccessRequest(AtlasPrivilege.TYPE_CREATE, structDef), "create struct-def ", structDef.getName());

        AtlasVertex ret = typeDefStore.findTypeVertexByName(structDef.getName());

        if (ret != null) {
            throw new AtlasBaseException(AtlasErrorCode.TYPE_ALREADY_EXISTS, structDef.getName());
        }

        ret = typeDefStore.createTypeVertex(structDef);

        AtlasStructDefStoreV2.updateVertexPreCreate(structDef, (AtlasStructType)type, ret, typeDefStore);

        if (LOG.isDebugEnabled()) {
            LOG.debug("<== AtlasStructDefStoreV1.preCreate({}): {}", structDef, ret);
        }

        return ret;
    }

    @Override
    public AtlasStructDef create(AtlasStructDef structDef, AtlasVertex preCreateResult) throws AtlasBaseException {
        if (LOG.isDebugEnabled()) {
            LOG.debug("==> AtlasStructDefStoreV1.create({}, {})", structDef, preCreateResult);
        }

        verifyAttributeTypeReadAccess(structDef.getAttributeDefs());


        if (CollectionUtils.isEmpty(structDef.getAttributeDefs())) {
            throw new AtlasBaseException(AtlasErrorCode.BAD_REQUEST, "Missing attributes for structdef");
        }

        AtlasVertex vertex = (preCreateResult == null) ? preCreate(structDef) : preCreateResult;

        AtlasStructDefStoreV2.updateVertexAddReferences(structDef, vertex, typeDefStore);

        AtlasStructDef ret = toStructDef(vertex);

        if (LOG.isDebugEnabled()) {
            LOG.debug("<== AtlasStructDefStoreV1.create({}, {}): {}", structDef, preCreateResult, ret);
        }

        return ret;
    }

    @Override
    public List<AtlasStructDef> getAll() throws AtlasBaseException {
        if (LOG.isDebugEnabled()) {
            LOG.debug("==> AtlasStructDefStoreV1.getAll()");
        }

        List<AtlasStructDef> ret = new ArrayList<>();

        Iterator<AtlasVertex> vertices = typeDefStore.findTypeVerticesByCategory(TypeCategory.STRUCT);
        while (vertices.hasNext()) {
            ret.add(toStructDef(vertices.next()));
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("<== AtlasStructDefStoreV1.getAll(): count={}", ret.size());
        }
        return ret;
    }

    @Override
    public AtlasStructDef getByName(String name) throws AtlasBaseException {
        if (LOG.isDebugEnabled()) {
            LOG.debug("==> AtlasStructDefStoreV1.getByName({})", name);
        }

        AtlasVertex vertex = typeDefStore.findTypeVertexByNameAndCategory(name, TypeCategory.STRUCT);

        if (vertex == null) {
            throw new AtlasBaseException(AtlasErrorCode.TYPE_NAME_NOT_FOUND, name);
        }

        vertex.getProperty(Constants.TYPE_CATEGORY_PROPERTY_KEY, String.class);

        AtlasStructDef ret = toStructDef(vertex);

        if (LOG.isDebugEnabled()) {
            LOG.debug("<== AtlasStructDefStoreV1.getByName({}): {}", name, ret);
        }

        return ret;
    }

    @Override
    public AtlasStructDef getByGuid(String guid) throws AtlasBaseException {
        if (LOG.isDebugEnabled()) {
            LOG.debug("==> AtlasStructDefStoreV1.getByGuid({})", guid);
        }

        AtlasVertex vertex = typeDefStore.findTypeVertexByGuidAndCategory(guid, TypeCategory.STRUCT);

        if (vertex == null) {
            throw new AtlasBaseException(AtlasErrorCode.TYPE_GUID_NOT_FOUND, guid);
        }

        AtlasStructDef ret = toStructDef(vertex);

        if (LOG.isDebugEnabled()) {
            LOG.debug("<== AtlasStructDefStoreV1.getByGuid({}): {}", guid, ret);
        }

        return ret;
    }

    @Override
    public AtlasStructDef update(AtlasStructDef structDef) throws AtlasBaseException {
        if (LOG.isDebugEnabled()) {
            LOG.debug("==> AtlasStructDefStoreV1.update({})", structDef);
        }

        verifyAttributeTypeReadAccess(structDef.getAttributeDefs());


        validateType(structDef);

        AtlasStructDef ret = StringUtils.isNotBlank(structDef.getGuid()) ? updateByGuid(structDef.getGuid(), structDef)
                                                                         : updateByName(structDef.getName(), structDef);

        if (LOG.isDebugEnabled()) {
            LOG.debug("<== AtlasStructDefStoreV1.update({}): {}", structDef, ret);
        }

        return ret;
    }

    @Override
    public AtlasStructDef updateByName(String name, AtlasStructDef structDef) throws AtlasBaseException {
        if (LOG.isDebugEnabled()) {
            LOG.debug("==> AtlasStructDefStoreV1.updateByName({}, {})", name, structDef);
        }

        AtlasStructDef existingDef = typeRegistry.getStructDefByName(name);

        AtlasAuthorizationUtils.verifyAccess(new AtlasTypeAccessRequest(AtlasPrivilege.TYPE_UPDATE, existingDef), "update struct-def ", name);

        validateType(structDef);

        AtlasType type = typeRegistry.getType(structDef.getName());

        if (type.getTypeCategory() != org.apache.atlas.model.TypeCategory.STRUCT) {
            throw new AtlasBaseException(AtlasErrorCode.TYPE_MATCH_FAILED, structDef.getName(), TypeCategory.STRUCT.name());
        }

        AtlasVertex vertex = typeDefStore.findTypeVertexByNameAndCategory(name, TypeCategory.STRUCT);

        if (vertex == null) {
            throw new AtlasBaseException(AtlasErrorCode.TYPE_NAME_NOT_FOUND, name);
        }

        AtlasStructDefStoreV2.updateVertexPreUpdate(structDef, (AtlasStructType)type, vertex, typeDefStore);
        AtlasStructDefStoreV2.updateVertexAddReferences(structDef, vertex, typeDefStore);

        AtlasStructDef ret = toStructDef(vertex);

        if (LOG.isDebugEnabled()) {
            LOG.debug("<== AtlasStructDefStoreV1.updateByName({}, {}): {}", name, structDef, ret);
        }

        return ret;
    }

    @Override
    public AtlasStructDef updateByGuid(String guid, AtlasStructDef structDef) throws AtlasBaseException {
        if (LOG.isDebugEnabled()) {
            LOG.debug("==> AtlasStructDefStoreV1.updateByGuid({})", guid);
        }

        AtlasStructDef existingDef = typeRegistry.getStructDefByGuid(guid);

        AtlasAuthorizationUtils.verifyAccess(new AtlasTypeAccessRequest(AtlasPrivilege.TYPE_UPDATE, existingDef), "update struct-def ", (existingDef != null ? existingDef.getName() : guid));

        validateType(structDef);

        AtlasType type = typeRegistry.getTypeByGuid(guid);

        if (type.getTypeCategory() != org.apache.atlas.model.TypeCategory.STRUCT) {
            throw new AtlasBaseException(AtlasErrorCode.TYPE_MATCH_FAILED, structDef.getName(), TypeCategory.STRUCT.name());
        }

        AtlasVertex vertex = typeDefStore.findTypeVertexByGuidAndCategory(guid, TypeCategory.STRUCT);

        if (vertex == null) {
            throw new AtlasBaseException(AtlasErrorCode.TYPE_GUID_NOT_FOUND, guid);
        }

        AtlasStructDefStoreV2.updateVertexPreUpdate(structDef, (AtlasStructType)type, vertex, typeDefStore);
        AtlasStructDefStoreV2.updateVertexAddReferences(structDef, vertex, typeDefStore);

        AtlasStructDef ret = toStructDef(vertex);

        if (LOG.isDebugEnabled()) {
            LOG.debug("<== AtlasStructDefStoreV1.updateByGuid({}): {}", guid, ret);
        }

        return ret;
    }

    @Override
    public AtlasVertex preDeleteByName(String name) throws AtlasBaseException {
        if (LOG.isDebugEnabled()) {
            LOG.debug("==> AtlasStructDefStoreV1.preDeleteByName({})", name);
        }

        AtlasStructDef existingDef = typeRegistry.getStructDefByName(name);

        AtlasAuthorizationUtils.verifyAccess(new AtlasTypeAccessRequest(AtlasPrivilege.TYPE_DELETE, existingDef), "delete struct-def ", name);

        AtlasVertex ret = typeDefStore.findTypeVertexByNameAndCategory(name, TypeCategory.STRUCT);

        if (AtlasGraphUtilsV2.typeHasInstanceVertex(name)) {
            throw new AtlasBaseException(AtlasErrorCode.TYPE_HAS_REFERENCES, name);
        }

        if (ret == null) {
            throw new AtlasBaseException(AtlasErrorCode.TYPE_NAME_NOT_FOUND, name);
        }

        typeDefStore.deleteTypeVertexOutEdges(ret);

        if (LOG.isDebugEnabled()) {
            LOG.debug("<== AtlasStructDefStoreV1.preDeleteByName({}): {}", name, ret);
        }

        return ret;
    }

    @Override
    public AtlasVertex preDeleteByGuid(String guid) throws AtlasBaseException {
        if (LOG.isDebugEnabled()) {
            LOG.debug("==> AtlasStructDefStoreV1.preDeleteByGuid({})", guid);
        }

        AtlasStructDef existingDef = typeRegistry.getStructDefByGuid(guid);

        AtlasAuthorizationUtils.verifyAccess(new AtlasTypeAccessRequest(AtlasPrivilege.TYPE_DELETE, existingDef), "delete struct-def ", (existingDef != null ? existingDef.getName() : guid));

        AtlasVertex ret = typeDefStore.findTypeVertexByGuidAndCategory(guid, TypeCategory.STRUCT);

        String typeName = AtlasGraphUtilsV2.getEncodedProperty(ret, Constants.TYPENAME_PROPERTY_KEY, String.class);

        if (AtlasGraphUtilsV2.typeHasInstanceVertex(typeName)) {
            throw new AtlasBaseException(AtlasErrorCode.TYPE_HAS_REFERENCES, typeName);
        }

        if (ret == null) {
            throw new AtlasBaseException(AtlasErrorCode.TYPE_GUID_NOT_FOUND, guid);
        }

        typeDefStore.deleteTypeVertexOutEdges(ret);

        if (LOG.isDebugEnabled()) {
            LOG.debug("<== AtlasStructDefStoreV1.preDeleteByGuid({}): {}", guid, ret);
        }

        return ret;
    }

    private AtlasStructDef toStructDef(AtlasVertex vertex) throws AtlasBaseException {
        AtlasStructDef ret = null;

        if (vertex != null && typeDefStore.isTypeVertex(vertex, TypeCategory.STRUCT)) {
            ret = toStructDef(vertex, new AtlasStructDef(), typeDefStore);
        }

        return ret;
    }

    public static void updateVertexPreCreate(AtlasStructDef structDef, AtlasStructType structType,
                                             AtlasVertex vertex, AtlasTypeDefGraphStoreV2 typeDefStore) throws AtlasBaseException {
        List<String> attrNames = new ArrayList<>(structDef.getAttributeDefs().size());

        for (AtlasAttributeDef attributeDef : structDef.getAttributeDefs()) {
            // Validate the mandatory features of an attribute (compatibility with legacy type system)
            if (StringUtils.isEmpty(attributeDef.getName())) {
                throw new AtlasBaseException(AtlasErrorCode.MISSING_MANDATORY_ATTRIBUTE, structDef.getName(), "name");
            }

            if (StringUtils.isEmpty(attributeDef.getTypeName())) {
                throw new AtlasBaseException(AtlasErrorCode.MISSING_MANDATORY_ATTRIBUTE, structDef.getName(), "typeName");
            }

            String propertyKey        = AtlasGraphUtilsV2.getTypeDefPropertyKey(structDef, attributeDef.getName());
            String encodedPropertyKey = AtlasGraphUtilsV2.encodePropertyKey(propertyKey);

            vertex.setProperty(encodedPropertyKey, toJsonFromAttribute(structType.getAttribute(attributeDef.getName())));

            attrNames.add(attributeDef.getName());
        }

        String typeNamePropertyKey        = AtlasGraphUtilsV2.getTypeDefPropertyKey(structDef);
        String encodedtypeNamePropertyKey = AtlasGraphUtilsV2.encodePropertyKey(typeNamePropertyKey);

        vertex.setProperty(encodedtypeNamePropertyKey, attrNames);
    }

    public static void updateVertexPreUpdate(AtlasStructDef structDef, AtlasStructType structType,
                                             AtlasVertex vertex, AtlasTypeDefGraphStoreV2 typeDefStore)
        throws AtlasBaseException {

        List<String> attrNames = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(structDef.getAttributeDefs())) {
            for (AtlasAttributeDef attributeDef : structDef.getAttributeDefs()) {
                attrNames.add(attributeDef.getName());
            }
        }

        String       structDefPropertyKey        = AtlasGraphUtilsV2.getTypeDefPropertyKey(structDef);
        String       encodedStructDefPropertyKey = encodePropertyKey(structDefPropertyKey);
        List<String> currAttrNames               = vertex.getProperty(encodedStructDefPropertyKey, List.class);

        // delete attributes that are not present in updated structDef
        if (CollectionUtils.isNotEmpty(currAttrNames)) {
            List<String> removedAttributes = null;

            for (String currAttrName : currAttrNames) {
                if (!attrNames.contains(currAttrName)) {
                    if (RequestContext.get().isInTypePatching()) {
                        String propertyKey = AtlasGraphUtilsV2.getTypeDefPropertyKey(structDef, currAttrName);

                        AtlasGraphUtilsV2.setProperty(vertex, propertyKey, null);

                        if (removedAttributes == null) {
                            removedAttributes = new ArrayList<>();
                        }

                        removedAttributes.add(currAttrName);

                        LOG.warn("REMOVED ATTRIBUTE: {}.{}", structDef.getName(), currAttrName);
                    } else {
                        throw new AtlasBaseException(AtlasErrorCode.ATTRIBUTE_DELETION_NOT_SUPPORTED,
                                structDef.getName(), currAttrName);
                    }
                }
            }

            if (removedAttributes != null) {
                currAttrNames.removeAll(removedAttributes);

                vertex.setListProperty(encodedStructDefPropertyKey, currAttrNames);
            }
        }

        typeDefStore.updateTypeVertex(structDef, vertex);

        // Load up current struct definition for matching attributes
        AtlasStructDef currentStructDef = toStructDef(vertex, new AtlasStructDef(), typeDefStore);

        // add/update attributes that are present in updated structDef
        if (CollectionUtils.isNotEmpty(structDef.getAttributeDefs())) {
            for (AtlasAttributeDef attributeDef : structDef.getAttributeDefs()) {
                if (CollectionUtils.isEmpty(currAttrNames) || !currAttrNames.contains(attributeDef.getName())) {

                    // this could have been an attribute removed by a REMOVE_LEGACY_REF_ATTRIBUTE patch
                    // in such case, don't add this attribute; ignore and continue
                    AtlasRelationshipType relationship = AtlasTypeUtil.findRelationshipWithLegacyRelationshipEnd(structDef.getName(), attributeDef.getName(), typeDefStore.getTypeRegistry());

                    if (relationship != null) {
                        attrNames.remove(attributeDef.getName());

                        LOG.warn("Ignoring attempt to add legacy attribute {}.{}, which is already present in relationship {}", structDef.getName(), attributeDef.getName(), relationship.getTypeName());

                        continue;
                    }

                    // new attribute - allow optional by default or allow mandatory only with typedef patch ADD_MANDATORY_ATTRIBUTE
                    if (!attributeDef.getIsOptional() && !isInAddMandatoryAttributePatch()) {
                        throw new AtlasBaseException(AtlasErrorCode.CANNOT_ADD_MANDATORY_ATTRIBUTE, structDef.getName(), attributeDef.getName());
                    }
                }

                // Validate the mandatory features of an attribute (compatibility with legacy type system)
                if (StringUtils.isEmpty(attributeDef.getName())) {
                    throw new AtlasBaseException(AtlasErrorCode.MISSING_MANDATORY_ATTRIBUTE, structDef.getName(), "name");
                }
                if (StringUtils.isEmpty(attributeDef.getTypeName())) {
                    throw new AtlasBaseException(AtlasErrorCode.MISSING_MANDATORY_ATTRIBUTE, structDef.getName(), "typeName");
                }

                AtlasAttributeDef existingAttribute = currentStructDef.getAttribute(attributeDef.getName());
                if (null != existingAttribute && !attributeDef.getTypeName().equals(existingAttribute.getTypeName())) {
                    throw new AtlasBaseException(AtlasErrorCode.BAD_REQUEST, "Data type update for attribute is not supported");
                }

                String propertyKey = AtlasGraphUtilsV2.getTypeDefPropertyKey(structDef, attributeDef.getName());

                AtlasGraphUtilsV2.setProperty(vertex, propertyKey, toJsonFromAttribute(structType.getAttribute(attributeDef.getName())));
            }
        }

        AtlasGraphUtilsV2.setEncodedProperty(vertex, encodedStructDefPropertyKey, attrNames);
    }

    public static boolean isInAddMandatoryAttributePatch() {
        return RequestContext.get().isInTypePatching() &&
                StringUtils.equals(Constants.TYPEDEF_PATCH_ADD_MANDATORY_ATTRIBUTE, RequestContext.get().getCurrentTypePatchAction());
    }

    public static void updateVertexAddReferences(AtlasStructDef structDef, AtlasVertex vertex,
                                                 AtlasTypeDefGraphStoreV2 typeDefStore) throws AtlasBaseException {
        for (AtlasAttributeDef attributeDef : structDef.getAttributeDefs()) {
            addReferencesForAttribute(vertex, attributeDef, typeDefStore);
        }
    }

    public static AtlasStructDef toStructDef(AtlasVertex vertex, AtlasStructDef structDef, AtlasTypeDefGraphStoreV2 typeDefStore)
                                             throws AtlasBaseException {
        AtlasStructDef ret = (structDef != null) ? structDef : new AtlasStructDef();

        typeDefStore.vertexToTypeDef(vertex, ret);

        List<AtlasAttributeDef> attributeDefs          = new ArrayList<>();
        String                  typePropertyKey        = AtlasGraphUtilsV2.getTypeDefPropertyKey(ret);
        String                  encodedTypePropertyKey = AtlasGraphUtilsV2.encodePropertyKey(typePropertyKey);
        List<String>            attrNames              = vertex.getProperty(encodedTypePropertyKey, List.class);

        if (CollectionUtils.isNotEmpty(attrNames)) {
            for (String attrName : attrNames) {
                String attrPropertyKey        = AtlasGraphUtilsV2.getTypeDefPropertyKey(ret, attrName);
                String encodedAttrPropertyKey = AtlasGraphUtilsV2.encodePropertyKey(attrPropertyKey);
                String attrJson               = vertex.getProperty(encodedAttrPropertyKey, String.class);

                if (StringUtils.isEmpty(attrJson)) {
                    LOG.warn("attribute not found {}.{}. Ignoring..", structDef.getName(), attrName);

                    continue;
                }

                attributeDefs.add(toAttributeDefFromJson(structDef, AtlasType.fromJson(attrJson, Map.class), typeDefStore));
            }
        }

        ret.setAttributeDefs(attributeDefs);

        return ret;
    }

    private static void addReferencesForAttribute(AtlasVertex vertex, AtlasAttributeDef attributeDef,
                                                  AtlasTypeDefGraphStoreV2 typeDefStore) throws AtlasBaseException {
        Set<String> referencedTypeNames = AtlasTypeUtil.getReferencedTypeNames(attributeDef.getTypeName());

        String typeName = vertex.getProperty(Constants.TYPENAME_PROPERTY_KEY, String.class);

        for (String referencedTypeName : referencedTypeNames) {
            if (!AtlasTypeUtil.isBuiltInType(referencedTypeName)) {
                AtlasVertex referencedTypeVertex = typeDefStore.findTypeVertexByName(referencedTypeName);

                if (referencedTypeVertex == null) {
                    throw new AtlasBaseException(AtlasErrorCode.UNKNOWN_TYPE, referencedTypeName, typeName, attributeDef.getName());
                }

                String label = AtlasGraphUtilsV2.getEdgeLabel(typeName, attributeDef.getName());

                typeDefStore.getOrCreateEdge(vertex, referencedTypeVertex, label);
            }
        }
    }

    @VisibleForTesting
    public static String toJsonFromAttribute(AtlasAttribute attribute) {
        AtlasAttributeDef   attributeDef = attribute.getAttributeDef();
        Map<String, Object> attribInfo   = new HashMap<>();

        attribInfo.put("name", attributeDef.getName());
        attribInfo.put("dataType", attributeDef.getTypeName());
        attribInfo.put("isUnique", attributeDef.getIsUnique());
        attribInfo.put("isIndexable", attributeDef.getIsIndexable());
        attribInfo.put("includeInNotification", attributeDef.getIncludeInNotification());
        attribInfo.put("isComposite", attribute.isOwnedRef());
        attribInfo.put("reverseAttributeName", attribute.getInverseRefAttributeName());
        attribInfo.put("defaultValue", attributeDef.getDefaultValue());
        attribInfo.put("description", attributeDef.getDescription());
        attribInfo.put("searchWeight", attributeDef.getSearchWeight());
        attribInfo.put("indexType", attributeDef.getIndexType());

        if(attributeDef.getOptions() != null) {
            attribInfo.put("options", AtlasType.toJson(attributeDef.getOptions()));
        }

        attribInfo.put("displayName", attributeDef.getDisplayName());

        final int lower;
        final int upper;

        if (attributeDef.getCardinality() == AtlasAttributeDef.Cardinality.SINGLE) {
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

        Map<String, Object> multiplicity = new HashMap<>();
        multiplicity.put("lower", lower);
        multiplicity.put("upper", upper);
        multiplicity.put("isUnique", AtlasAttributeDef.Cardinality.SET.equals(attributeDef.getCardinality()));

        attribInfo.put("multiplicity", AtlasType.toJson(multiplicity));

        return AtlasType.toJson(attribInfo);
    }

    @VisibleForTesting
    public static AtlasAttributeDef toAttributeDefFromJson(AtlasStructDef           structDef,
                                                           Map                      attribInfo,
                                                           AtlasTypeDefGraphStoreV2 typeDefStore)
        throws AtlasBaseException {
        AtlasAttributeDef ret = new AtlasAttributeDef();

        ret.setName((String) attribInfo.get("name"));
        ret.setTypeName((String) attribInfo.get("dataType"));
        ret.setIsUnique((Boolean) attribInfo.get("isUnique"));
        ret.setIsIndexable((Boolean) attribInfo.get("isIndexable"));
        ret.setIncludeInNotification((Boolean) attribInfo.get("includeInNotification"));
        ret.setDefaultValue((String) attribInfo.get("defaultValue"));
        ret.setDescription((String) attribInfo.get("description"));

        if(attribInfo.get("options") != null) {
            ret.setOptions(AtlasType.fromJson((String) attribInfo.get("options"), Map.class));
        }

        ret.setDisplayName((String) attribInfo.get("displayName"));

        if ((Boolean)attribInfo.get("isComposite")) {
            ret.addConstraint(new AtlasConstraintDef(AtlasConstraintDef.CONSTRAINT_TYPE_OWNED_REF));
        }

        final String reverseAttributeName = (String) attribInfo.get("reverseAttributeName");
        if (StringUtils.isNotBlank(reverseAttributeName)) {
            ret.addConstraint(new AtlasConstraintDef(AtlasConstraintDef.CONSTRAINT_TYPE_INVERSE_REF,
                    new HashMap<String, Object>() {{
                        put(AtlasConstraintDef.CONSTRAINT_PARAM_ATTRIBUTE, reverseAttributeName);
                    }}));
        }

        Map     multiplicity = AtlasType.fromJson((String) attribInfo.get("multiplicity"), Map.class);
        Number  minCount     = (Number) multiplicity.get("lower");
        Number  maxCount     = (Number) multiplicity.get("upper");
        Boolean isUnique     = (Boolean) multiplicity.get("isUnique");

        if (minCount == null || minCount.intValue() == 0) {
            ret.setIsOptional(true);
            ret.setValuesMinCount(0);
        } else {
            ret.setIsOptional(false);
            ret.setValuesMinCount(minCount.intValue());
        }

        if (maxCount == null || maxCount.intValue() < 2) {
            ret.setCardinality(AtlasAttributeDef.Cardinality.SINGLE);
            ret.setValuesMaxCount(1);
        } else {
            if (isUnique == null || isUnique == Boolean.FALSE) {
                ret.setCardinality(AtlasAttributeDef.Cardinality.LIST);
            } else {
                ret.setCardinality(AtlasAttributeDef.Cardinality.SET);
            }

            ret.setValuesMaxCount(maxCount.intValue());
        }

        Number searchWeight = (Number) attribInfo.get("searchWeight");
        if( searchWeight != null ) {
            ret.setSearchWeight(searchWeight.intValue());
        } else {
            ret.setSearchWeight(-1);
        }

        String indexType = (String) attribInfo.get("indexType");
        if(!StringUtils.isEmpty(indexType)) {
            ret.setIndexType(AtlasAttributeDef.IndexType.valueOf(indexType));
        }
        return ret;
    }

    public static AttributeDefinition toAttributeDefinition(AtlasAttribute attribute) {
        final AtlasAttributeDef attrDef = attribute.getAttributeDef();

        AttributeDefinition ret = new AttributeDefinition();

        ret.setName(attrDef.getName());
        ret.setDataTypeName(attrDef.getTypeName());
        ret.setMultiplicity(AtlasTypeUtil.getMultiplicity(attrDef));
        ret.setIsComposite(attribute.isOwnedRef());
        ret.setIsUnique(attrDef.getIsUnique());
        ret.setIsIndexable(attrDef.getIsIndexable());
        ret.setReverseAttributeName(attribute.getInverseRefAttributeName());
        ret.setDescription(attrDef.getDescription());
        ret.setDefaultValue(attrDef.getDefaultValue());
        ret.setSearchWeight(attrDef.getSearchWeight());
        ret.setIndexType(attrDef.getIndexType());
        return ret;
    }
}

