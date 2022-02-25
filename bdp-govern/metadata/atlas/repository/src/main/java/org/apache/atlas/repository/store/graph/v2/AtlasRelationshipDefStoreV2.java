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

import org.apache.atlas.ApplicationProperties;
import org.apache.atlas.AtlasErrorCode;
import org.apache.atlas.AtlasException;
import org.apache.atlas.RequestContext;
import org.apache.atlas.exception.AtlasBaseException;
import org.apache.atlas.model.typedef.AtlasRelationshipDef;
import org.apache.atlas.model.typedef.AtlasRelationshipDef.RelationshipCategory;
import org.apache.atlas.model.typedef.AtlasRelationshipDef.PropagateTags;
import org.apache.atlas.model.typedef.AtlasRelationshipEndDef;
import org.apache.atlas.query.AtlasDSL;
import org.apache.atlas.repository.Constants;
import org.apache.atlas.repository.graphdb.AtlasEdge;
import org.apache.atlas.repository.graphdb.AtlasVertex;
import org.apache.atlas.type.AtlasRelationshipType;
import org.apache.atlas.type.AtlasType;
import org.apache.atlas.type.AtlasTypeRegistry;
import org.apache.atlas.typesystem.types.DataTypes.TypeCategory;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.atlas.authorize.AtlasPrivilege;
import org.apache.atlas.authorize.AtlasTypeAccessRequest;
import org.apache.atlas.authorize.AtlasAuthorizationUtils;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

/**
 * RelationshipDef store in v1 format.
 */
public class AtlasRelationshipDefStoreV2 extends AtlasAbstractDefStoreV2<AtlasRelationshipDef> {
    private static final Logger LOG = LoggerFactory.getLogger(AtlasRelationshipDefStoreV2.class);

    @Inject
    public AtlasRelationshipDefStoreV2(AtlasTypeDefGraphStoreV2 typeDefStore, AtlasTypeRegistry typeRegistry) {
        super(typeDefStore, typeRegistry);
    }

    @Override
    public AtlasVertex preCreate(AtlasRelationshipDef relationshipDef) throws AtlasBaseException {
        if (LOG.isDebugEnabled()) {
            LOG.debug("==> AtlasRelationshipDefStoreV1.preCreate({})", relationshipDef);
        }

        validateType(relationshipDef);

        AtlasType type = typeRegistry.getType(relationshipDef.getName());

        if (type.getTypeCategory() != org.apache.atlas.model.TypeCategory.RELATIONSHIP) {
            throw new AtlasBaseException(AtlasErrorCode.TYPE_MATCH_FAILED, relationshipDef.getName(), TypeCategory.RELATIONSHIP.name());
        }

        verifyTypeReadAccess(relationshipDef);

        AtlasAuthorizationUtils.verifyAccess(new AtlasTypeAccessRequest(AtlasPrivilege.TYPE_CREATE, relationshipDef), "create relationship-def ", relationshipDef.getName());

        AtlasVertex relationshipDefVertex = typeDefStore.findTypeVertexByName(relationshipDef.getName());

        if (relationshipDefVertex != null) {
            throw new AtlasBaseException(AtlasErrorCode.TYPE_ALREADY_EXISTS, relationshipDef.getName());
        }

        relationshipDefVertex = typeDefStore.createTypeVertex(relationshipDef);

        updateVertexPreCreate(relationshipDef, (AtlasRelationshipType) type, relationshipDefVertex);

        final AtlasRelationshipEndDef endDef1        = relationshipDef.getEndDef1();
        final AtlasRelationshipEndDef endDef2        = relationshipDef.getEndDef2();
        final String                  type1          = endDef1.getType();
        final String                  type2          = endDef2.getType();
        final String                  name1          = endDef1.getName();
        final String                  name2          = endDef2.getName();
        final AtlasVertex             end1TypeVertex = typeDefStore.findTypeVertexByName(type1);
        final AtlasVertex             end2TypeVertex = typeDefStore.findTypeVertexByName(type2);

        if (end1TypeVertex == null) {
            throw new AtlasBaseException(AtlasErrorCode.RELATIONSHIPDEF_END_TYPE_NAME_NOT_FOUND, relationshipDef.getName(), type1);
        }

        if (end2TypeVertex == null) {
            throw new AtlasBaseException(AtlasErrorCode.RELATIONSHIPDEF_END_TYPE_NAME_NOT_FOUND, relationshipDef.getName(), type2);
        }

        // create an edge between the relationshipDef and each of the entityDef vertices.
        AtlasEdge edge1 = typeDefStore.getOrCreateEdge(relationshipDefVertex, end1TypeVertex, AtlasGraphUtilsV2.RELATIONSHIPTYPE_EDGE_LABEL);

        /*
        Where edge1 and edge2 have the same names and types we do not need a second edge.
        We are not invoking the equals method on the AtlasRelationshipedDef, as we only want 1 edge even if propagateTags or other properties are different.
        */

        if (type1.equals(type2) && name1.equals(name2)) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("AtlasRelationshipDefStoreV1.preCreate({}): created relationshipDef vertex {}," +
                        " and one edge as {}, because end1 and end2 have the same type and name", relationshipDef, relationshipDefVertex, edge1);
            }

        } else {
            AtlasEdge edge2 = typeDefStore.getOrCreateEdge(relationshipDefVertex, end2TypeVertex, AtlasGraphUtilsV2.RELATIONSHIPTYPE_EDGE_LABEL);
            if (LOG.isDebugEnabled()) {
                LOG.debug("AtlasRelationshipDefStoreV1.preCreate({}): created relationshipDef vertex {}," +
                        " edge1 as {}, edge2 as {} ", relationshipDef, relationshipDefVertex, edge1, edge2);
            }

        }
        if (LOG.isDebugEnabled()) {
            LOG.debug("<== AtlasRelationshipDefStoreV1.preCreate({}): {}", relationshipDef, relationshipDefVertex);
        }
        return relationshipDefVertex;
    }

    @Override
    public AtlasRelationshipDef create(AtlasRelationshipDef relationshipDef, AtlasVertex preCreateResult)
            throws AtlasBaseException {
        if (LOG.isDebugEnabled()) {
            LOG.debug("==> AtlasRelationshipDefStoreV1.create({}, {})", relationshipDef, preCreateResult);
        }

        AtlasVertex vertex = (preCreateResult == null) ? preCreate(relationshipDef) : preCreateResult;

        AtlasRelationshipDef ret = toRelationshipDef(vertex);

        if (LOG.isDebugEnabled()) {
            LOG.debug("<== AtlasRelationshipDefStoreV1.create({}, {}): {}", relationshipDef, preCreateResult, ret);
        }

        return ret;
    }

    @Override
    public List<AtlasRelationshipDef> getAll() throws AtlasBaseException {
        if (LOG.isDebugEnabled()) {
            LOG.debug("==> AtlasRelationshipDefStoreV1.getAll()");
        }

        List<AtlasRelationshipDef> ret = new ArrayList<>();
        Iterator<AtlasVertex> vertices = typeDefStore.findTypeVerticesByCategory(TypeCategory.RELATIONSHIP);

        while (vertices.hasNext()) {
            ret.add(toRelationshipDef(vertices.next()));
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("<== AtlasRelationshipDefStoreV1.getAll(): count={}", ret.size());
        }

        return ret;
    }

    @Override
    public AtlasRelationshipDef getByName(String name) throws AtlasBaseException {
        if (LOG.isDebugEnabled()) {
            LOG.debug("==> AtlasRelationshipDefStoreV1.getByName({})", name);
        }

        AtlasVertex vertex = typeDefStore.findTypeVertexByNameAndCategory(name, TypeCategory.RELATIONSHIP);

        if (vertex == null) {
            throw new AtlasBaseException(AtlasErrorCode.TYPE_NAME_NOT_FOUND, name);
        }

        vertex.getProperty(Constants.TYPE_CATEGORY_PROPERTY_KEY, TypeCategory.class);

        AtlasRelationshipDef ret = toRelationshipDef(vertex);

        if (LOG.isDebugEnabled()) {
            LOG.debug("<== AtlasRelationshipDefStoreV1.getByName({}): {}", name, ret);
        }

        return ret;
    }

    @Override
    public AtlasRelationshipDef getByGuid(String guid) throws AtlasBaseException {
        if (LOG.isDebugEnabled()) {
            LOG.debug("==> AtlasRelationshipDefStoreV1.getByGuid({})", guid);
        }

        AtlasVertex vertex = typeDefStore.findTypeVertexByGuidAndCategory(guid, TypeCategory.RELATIONSHIP);

        if (vertex == null) {
            throw new AtlasBaseException(AtlasErrorCode.TYPE_GUID_NOT_FOUND, guid);
        }

        AtlasRelationshipDef ret = toRelationshipDef(vertex);

        if (LOG.isDebugEnabled()) {
            LOG.debug("<== AtlasRelationshipDefStoreV1.getByGuid({}): {}", guid, ret);
        }

        return ret;
    }

    @Override
    public AtlasRelationshipDef update(AtlasRelationshipDef relationshipDef) throws AtlasBaseException {
        if (LOG.isDebugEnabled()) {
            LOG.debug("==> AtlasRelationshipDefStoreV1.update({})", relationshipDef);
        }

        verifyTypeReadAccess(relationshipDef);

        validateType(relationshipDef);

        AtlasRelationshipDef ret = StringUtils.isNotBlank(relationshipDef.getGuid())
                ? updateByGuid(relationshipDef.getGuid(), relationshipDef)
                : updateByName(relationshipDef.getName(), relationshipDef);

        if (LOG.isDebugEnabled()) {
            LOG.debug("<== AtlasRelationshipDefStoreV1.update({}): {}", relationshipDef, ret);
        }

        return ret;
    }

    @Override
    public AtlasRelationshipDef updateByName(String name, AtlasRelationshipDef relationshipDef)
            throws AtlasBaseException {
        if (LOG.isDebugEnabled()) {
            LOG.debug("==> AtlasRelationshipDefStoreV1.updateByName({}, {})", name, relationshipDef);
        }

        AtlasRelationshipDef existingDef = typeRegistry.getRelationshipDefByName(name);

        AtlasAuthorizationUtils.verifyAccess(new AtlasTypeAccessRequest(AtlasPrivilege.TYPE_UPDATE, existingDef), "update relationship-def ", name);

        validateType(relationshipDef);

        AtlasType type = typeRegistry.getType(relationshipDef.getName());

        if (type.getTypeCategory() != org.apache.atlas.model.TypeCategory.RELATIONSHIP) {
            throw new AtlasBaseException(AtlasErrorCode.TYPE_MATCH_FAILED, relationshipDef.getName(), TypeCategory.RELATIONSHIP.name());
        }

        AtlasVertex vertex = typeDefStore.findTypeVertexByNameAndCategory(name, TypeCategory.RELATIONSHIP);

        if (vertex == null) {
            throw new AtlasBaseException(AtlasErrorCode.TYPE_NAME_NOT_FOUND, name);
        }

        preUpdateCheck(relationshipDef, (AtlasRelationshipType) type, vertex);

        AtlasRelationshipDef ret = toRelationshipDef(vertex);

        if (LOG.isDebugEnabled()) {
            LOG.debug("<== AtlasRelationshipDefStoreV1.updateByName({}, {}): {}", name, relationshipDef, ret);
        }

        return ret;
    }

    @Override
    public AtlasRelationshipDef updateByGuid(String guid, AtlasRelationshipDef relationshipDef)
            throws AtlasBaseException {
        if (LOG.isDebugEnabled()) {
            LOG.debug("==> AtlasRelationshipDefStoreV1.updateByGuid({})", guid);
        }

        AtlasRelationshipDef existingDef = typeRegistry.getRelationshipDefByGuid(guid);

        AtlasAuthorizationUtils.verifyAccess(new AtlasTypeAccessRequest(AtlasPrivilege.TYPE_UPDATE, existingDef), "update relationship-Def ", (existingDef != null ? existingDef.getName() : guid));

        validateType(relationshipDef);

        AtlasType type = typeRegistry.getTypeByGuid(guid);

        if (type.getTypeCategory() != org.apache.atlas.model.TypeCategory.RELATIONSHIP) {
            throw new AtlasBaseException(AtlasErrorCode.TYPE_MATCH_FAILED, relationshipDef.getName(), TypeCategory.RELATIONSHIP.name());
        }

        AtlasVertex vertex = typeDefStore.findTypeVertexByGuidAndCategory(guid, TypeCategory.RELATIONSHIP);

        if (vertex == null) {
            throw new AtlasBaseException(AtlasErrorCode.TYPE_GUID_NOT_FOUND, guid);
        }

        preUpdateCheck(relationshipDef, (AtlasRelationshipType) type, vertex);
        // updates should not effect the edges between the types as we do not allow updates that change the endpoints.

        AtlasRelationshipDef ret = toRelationshipDef(vertex);

        if (LOG.isDebugEnabled()) {
            LOG.debug("<== AtlasRelationshipDefStoreV1.updateByGuid({}): {}", guid, ret);
        }

        return ret;
    }

    @Override
    public AtlasVertex preDeleteByName(String name) throws AtlasBaseException {
        if (LOG.isDebugEnabled()) {
            LOG.debug("==> AtlasRelationshipDefStoreV1.preDeleteByName({})", name);
        }

        AtlasRelationshipDef existingDef = typeRegistry.getRelationshipDefByName(name);

        AtlasAuthorizationUtils.verifyAccess(new AtlasTypeAccessRequest(AtlasPrivilege.TYPE_DELETE, existingDef), "delete relationship-def ", name);

        AtlasVertex ret = typeDefStore.findTypeVertexByNameAndCategory(name, TypeCategory.RELATIONSHIP);

        if (ret == null) {
            throw new AtlasBaseException(AtlasErrorCode.TYPE_NAME_NOT_FOUND, name);
        }

        if (AtlasGraphUtilsV2.relationshipTypeHasInstanceEdges(name)) {
            throw new AtlasBaseException(AtlasErrorCode.TYPE_HAS_REFERENCES, name);
        }

        typeDefStore.deleteTypeVertexOutEdges(ret);

        if (LOG.isDebugEnabled()) {
            LOG.debug("<== AtlasRelationshipDefStoreV1.preDeleteByName({}): {}", name, ret);
        }

        return ret;
    }

    @Override
    public AtlasVertex preDeleteByGuid(String guid) throws AtlasBaseException {
        if (LOG.isDebugEnabled()) {
            LOG.debug("==> AtlasRelationshipDefStoreV1.preDeleteByGuid({})", guid);
        }

        AtlasRelationshipDef existingDef = typeRegistry.getRelationshipDefByGuid(guid);

        AtlasAuthorizationUtils.verifyAccess(new AtlasTypeAccessRequest(AtlasPrivilege.TYPE_DELETE, existingDef), "delete relationship-def ", (existingDef != null ? existingDef.getName() : guid));

        AtlasVertex ret = typeDefStore.findTypeVertexByGuidAndCategory(guid, TypeCategory.RELATIONSHIP);

        if (ret == null) {
            throw new AtlasBaseException(AtlasErrorCode.TYPE_GUID_NOT_FOUND, guid);
        }

        String typeName = AtlasGraphUtilsV2.getEncodedProperty(ret, Constants.TYPENAME_PROPERTY_KEY, String.class);

        if (AtlasGraphUtilsV2.relationshipTypeHasInstanceEdges(typeName)) {
            throw new AtlasBaseException(AtlasErrorCode.TYPE_HAS_REFERENCES, typeName);
        }

        typeDefStore.deleteTypeVertexOutEdges(ret);

        if (LOG.isDebugEnabled()) {
            LOG.debug("<== AtlasRelationshipDefStoreV1.preDeleteByGuid({}): {}", guid, ret);
        }

        return ret;
    }

    private void updateVertexPreCreate(AtlasRelationshipDef relationshipDef, AtlasRelationshipType relationshipType,
                                       AtlasVertex vertex) throws AtlasBaseException {
        AtlasRelationshipEndDef end1 = relationshipDef.getEndDef1();
        AtlasRelationshipEndDef end2 = relationshipDef.getEndDef2();

        // check whether the names added on the relationship Ends are reserved if required.
        final boolean allowReservedKeywords;
        try {
            allowReservedKeywords = ApplicationProperties.get().getBoolean(ALLOW_RESERVED_KEYWORDS, true);
        } catch (AtlasException e) {
            throw new AtlasBaseException(e);
        }

        if (!allowReservedKeywords) {
            if (AtlasDSL.Parser.isKeyword(end1.getName())) {
                throw new AtlasBaseException(AtlasErrorCode.RELATIONSHIPDEF_END1_NAME_INVALID, end1.getName());
            }

            if (AtlasDSL.Parser.isKeyword(end2.getName())) {
                throw new AtlasBaseException(AtlasErrorCode.RELATIONSHIPDEF_END2_NAME_INVALID, end2.getName());
            }
        }

        AtlasStructDefStoreV2.updateVertexPreCreate(relationshipDef, relationshipType, vertex, typeDefStore);
        // Update ends
        setVertexPropertiesFromRelationshipDef(relationshipDef, vertex);
    }

    private void preUpdateCheck(AtlasRelationshipDef newRelationshipDef, AtlasRelationshipType relationshipType,
                                AtlasVertex vertex) throws AtlasBaseException {
        // We will not support an update to endpoints or category key
        AtlasRelationshipDef existingRelationshipDef = toRelationshipDef(vertex);

        preUpdateCheck(newRelationshipDef, existingRelationshipDef);
        // we do allow change to tag propagation and the addition of new attributes.

        AtlasStructDefStoreV2.updateVertexPreUpdate(newRelationshipDef, relationshipType, vertex, typeDefStore);

        setVertexPropertiesFromRelationshipDef(newRelationshipDef, vertex);
    }

    /**
     * Check ends are the same and relationshipCategory is the same.
     *
     * We do this by comparing 2 relationshipDefs to avoid exposing the AtlasVertex to unit testing.
     *
     * @param newRelationshipDef
     * @param existingRelationshipDef
     * @throws AtlasBaseException
     */
    public static void preUpdateCheck(AtlasRelationshipDef newRelationshipDef, AtlasRelationshipDef existingRelationshipDef) throws AtlasBaseException {
        // do not allow renames of the Def.
        String existingName = existingRelationshipDef.getName();
        String newName      = newRelationshipDef.getName();

        if (!existingName.equals(newName)) {
            throw new AtlasBaseException(AtlasErrorCode.RELATIONSHIPDEF_INVALID_NAME_UPDATE,
                    newRelationshipDef.getGuid(),existingName, newName);
        }

        RelationshipCategory existingRelationshipCategory = existingRelationshipDef.getRelationshipCategory();
        RelationshipCategory newRelationshipCategory      = newRelationshipDef.getRelationshipCategory();

        if ( !existingRelationshipCategory.equals(newRelationshipCategory)){
            if (!RequestContext.get().isInTypePatching()) {
                throw new AtlasBaseException(AtlasErrorCode.RELATIONSHIPDEF_INVALID_CATEGORY_UPDATE,
                        newRelationshipDef.getName(), newRelationshipCategory.name(),
                        existingRelationshipCategory.name());
            } else {
                LOG.warn("RELATIONSHIP UPDATE: relationship category from {} to {} for {}", existingRelationshipCategory.name(), newRelationshipCategory.name(), newRelationshipDef.getName());
            }
        }

        AtlasRelationshipEndDef existingEnd1 = existingRelationshipDef.getEndDef1();
        AtlasRelationshipEndDef existingEnd2 = existingRelationshipDef.getEndDef2();
        AtlasRelationshipEndDef newEnd1      = newRelationshipDef.getEndDef1();
        AtlasRelationshipEndDef newEnd2      = newRelationshipDef.getEndDef2();
        boolean                 endsSwaped   = false;

        if ( !isValidUpdate(existingEnd1, newEnd1) ) {
            if (RequestContext.get().isInTypePatching() && isValidUpdate(existingEnd1, newEnd2)) { // allow swap of ends during type-patch
                endsSwaped = true;
            } else {
                throw new AtlasBaseException(AtlasErrorCode.RELATIONSHIPDEF_INVALID_END1_UPDATE,
                                             newRelationshipDef.getName(), newEnd1.toString(), existingEnd1.toString());
            }
        }

        AtlasRelationshipEndDef newEndToCompareWith = endsSwaped ? newEnd1 : newEnd2;

        if ( !isValidUpdate(existingEnd2, newEndToCompareWith) ) {
                throw new AtlasBaseException(AtlasErrorCode.RELATIONSHIPDEF_INVALID_END2_UPDATE,
                                             newRelationshipDef.getName(), newEndToCompareWith.toString(), existingEnd2.toString());
        }
    }

    public static void setVertexPropertiesFromRelationshipDef(AtlasRelationshipDef relationshipDef, AtlasVertex vertex) {
        vertex.setProperty(Constants.RELATIONSHIPTYPE_END1_KEY, AtlasType.toJson(relationshipDef.getEndDef1()));
        vertex.setProperty(Constants.RELATIONSHIPTYPE_END2_KEY, AtlasType.toJson(relationshipDef.getEndDef2()));

        // default the relationship category to association if it has not been specified.
        String relationshipCategory = RelationshipCategory.ASSOCIATION.name();
        if (relationshipDef.getRelationshipCategory()!=null) {
            relationshipCategory =relationshipDef.getRelationshipCategory().name();
        }

        // Update RelationshipCategory
        vertex.setProperty(Constants.RELATIONSHIPTYPE_CATEGORY_KEY, relationshipCategory);
        if (relationshipDef.getRelationshipLabel() == null) {
            vertex.removeProperty(Constants.RELATIONSHIPTYPE_LABEL_KEY);
        } else {
            vertex.setProperty(Constants.RELATIONSHIPTYPE_LABEL_KEY, relationshipDef.getRelationshipLabel());
        }

        if (relationshipDef.getPropagateTags() == null) {
            vertex.setProperty(Constants.RELATIONSHIPTYPE_TAG_PROPAGATION_KEY, AtlasRelationshipDef.PropagateTags.NONE.name());
        } else {
            vertex.setProperty(Constants.RELATIONSHIPTYPE_TAG_PROPAGATION_KEY, relationshipDef.getPropagateTags().name());
        }
    }

    private AtlasRelationshipDef toRelationshipDef(AtlasVertex vertex) throws AtlasBaseException {
        AtlasRelationshipDef ret = null;

        if (vertex != null && typeDefStore.isTypeVertex(vertex, TypeCategory.RELATIONSHIP)) {
            String name         = vertex.getProperty(Constants.TYPENAME_PROPERTY_KEY, String.class);
            String description  = vertex.getProperty(Constants.TYPEDESCRIPTION_PROPERTY_KEY, String.class);
            String version      = vertex.getProperty(Constants.TYPEVERSION_PROPERTY_KEY, String.class);
            String label        = vertex.getProperty(Constants.RELATIONSHIPTYPE_LABEL_KEY, String.class);
            String end1Str      = vertex.getProperty(Constants.RELATIONSHIPTYPE_END1_KEY, String.class);
            String end2Str      = vertex.getProperty(Constants.RELATIONSHIPTYPE_END2_KEY, String.class);
            String relationStr  = vertex.getProperty(Constants.RELATIONSHIPTYPE_CATEGORY_KEY, String.class);
            String propagateStr = vertex.getProperty(Constants.RELATIONSHIPTYPE_TAG_PROPAGATION_KEY, String.class);

            // set the ends
            AtlasRelationshipEndDef endDef1 = AtlasType.fromJson(end1Str, AtlasRelationshipEndDef.class);
            AtlasRelationshipEndDef endDef2 = AtlasType.fromJson(end2Str, AtlasRelationshipEndDef.class);

            // set the relationship Category
            RelationshipCategory relationshipCategory = null;
            for (RelationshipCategory value : RelationshipCategory.values()) {
                if (value.name().equals(relationStr)) {
                    relationshipCategory = value;
                }
            }

            // set the propagateTags
            PropagateTags propagateTags = null;
            for (PropagateTags value : PropagateTags.values()) {
                if (value.name().equals(propagateStr)) {
                    propagateTags = value;
                }
            }

            ret = new AtlasRelationshipDef(name, description, version, relationshipCategory,  propagateTags, endDef1, endDef2);

            ret.setRelationshipLabel(label);

            // add in the attributes
            AtlasStructDefStoreV2.toStructDef(vertex, ret, typeDefStore);
        }

        return ret;
    }

    private static boolean isValidUpdate(AtlasRelationshipEndDef currentDef, AtlasRelationshipEndDef updatedDef) {
        // permit updates to description and isLegacyAttribute (ref type-patch REMOVE_LEGACY_REF_ATTRIBUTES)
        return Objects.equals(currentDef.getType(), updatedDef.getType()) &&
                Objects.equals(currentDef.getName(), updatedDef.getName()) &&
                Objects.equals(currentDef.getIsContainer(), updatedDef.getIsContainer()) &&
                Objects.equals(currentDef.getCardinality(), updatedDef.getCardinality());
    }

    private void verifyTypeReadAccess(AtlasRelationshipDef relationshipDef) throws AtlasBaseException {
        verifyTypeReadAccess(relationshipDef.getEndDef1().getType());
        verifyTypeReadAccess(relationshipDef.getEndDef2().getType());
    }

}
