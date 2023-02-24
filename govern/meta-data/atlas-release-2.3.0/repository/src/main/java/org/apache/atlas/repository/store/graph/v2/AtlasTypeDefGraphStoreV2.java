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
import com.google.common.base.Preconditions;
import org.apache.atlas.AtlasErrorCode;
import org.apache.atlas.RequestContext;
import org.apache.atlas.annotation.GraphTransaction;
import org.apache.atlas.discovery.EntityDiscoveryService;
import org.apache.atlas.exception.AtlasBaseException;
import org.apache.atlas.listener.TypeDefChangeListener;
import org.apache.atlas.model.typedef.*;
import org.apache.atlas.repository.Constants;
import org.apache.atlas.repository.graphdb.AtlasEdge;
import org.apache.atlas.repository.graphdb.AtlasEdgeDirection;
import org.apache.atlas.repository.graphdb.AtlasGraph;
import org.apache.atlas.repository.graphdb.AtlasVertex;
import org.apache.atlas.repository.store.graph.AtlasDefStore;
import org.apache.atlas.repository.store.graph.AtlasTypeDefGraphStore;
import org.apache.atlas.type.AtlasType;
import org.apache.atlas.type.AtlasTypeRegistry;
import org.apache.atlas.typesystem.types.DataTypes.TypeCategory;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.apache.atlas.repository.Constants.TYPE_CATEGORY_PROPERTY_KEY;
import static org.apache.atlas.repository.Constants.VERTEX_TYPE_PROPERTY_KEY;
import static org.apache.atlas.repository.store.graph.v2.AtlasGraphUtilsV2.VERTEX_TYPE;


/**
 * Graph persistence store for TypeDef - v1
 */
@Singleton
@Component
public class AtlasTypeDefGraphStoreV2 extends AtlasTypeDefGraphStore {
    private static final Logger LOG = LoggerFactory.getLogger(AtlasTypeDefGraphStoreV2.class);

    protected final AtlasGraph atlasGraph;

    private EntityDiscoveryService entityDiscoveryService;

    @Inject
    public AtlasTypeDefGraphStoreV2(AtlasTypeRegistry typeRegistry,
                                    Set<TypeDefChangeListener> typeDefChangeListeners,
                                    AtlasGraph atlasGraph,
                                    EntityDiscoveryService entityDiscoveryService) {
        super(typeRegistry, typeDefChangeListeners);
        this.atlasGraph = atlasGraph;
        this.entityDiscoveryService = entityDiscoveryService;

        LOG.debug("<== AtlasTypeDefGraphStoreV1()");
    }

    @Override
    protected AtlasDefStore<AtlasEnumDef> getEnumDefStore(AtlasTypeRegistry typeRegistry) {
        return new AtlasEnumDefStoreV2(this, typeRegistry);
    }

    @Override
    protected AtlasDefStore<AtlasStructDef> getStructDefStore(AtlasTypeRegistry typeRegistry) {
        return new AtlasStructDefStoreV2(this, typeRegistry);
    }

    @Override
    protected AtlasDefStore<AtlasClassificationDef> getClassificationDefStore(AtlasTypeRegistry typeRegistry) {
        return new AtlasClassificationDefStoreV2(this, typeRegistry);
    }

    @Override
    protected AtlasDefStore<AtlasEntityDef> getEntityDefStore(AtlasTypeRegistry typeRegistry) {
        return new AtlasEntityDefStoreV2(this, typeRegistry);
    }

    @Override
    protected AtlasDefStore<AtlasRelationshipDef> getRelationshipDefStore(AtlasTypeRegistry typeRegistry) {
        return new AtlasRelationshipDefStoreV2(this, typeRegistry);
    }

    @Override
    protected AtlasDefStore<AtlasBusinessMetadataDef> getBusinessMetadataDefStore(AtlasTypeRegistry typeRegistry) {
        return new AtlasBusinessMetadataDefStoreV2(this, typeRegistry, this.entityDiscoveryService);
    }

    @Override
    @GraphTransaction
    public void init() throws AtlasBaseException {
        LOG.info("==> AtlasTypeDefGraphStoreV1.init()");

        super.init();

        LOG.info("<== AtlasTypeDefGraphStoreV1.init()");
    }

    AtlasGraph getAtlasGraph() { return atlasGraph; }

    @VisibleForTesting
    public AtlasVertex findTypeVertexByName(String typeName) {
        Iterator results = atlasGraph.query().has(VERTEX_TYPE_PROPERTY_KEY, VERTEX_TYPE)
                .has(Constants.TYPENAME_PROPERTY_KEY, typeName)
                .vertices().iterator();

        return (results != null && results.hasNext()) ? (AtlasVertex) results.next() : null;
    }

    AtlasVertex findTypeVertexByNameAndCategory(String typeName, TypeCategory category) {
        Iterator results = atlasGraph.query().has(VERTEX_TYPE_PROPERTY_KEY, VERTEX_TYPE)
                .has(Constants.TYPENAME_PROPERTY_KEY, typeName)
                .has(TYPE_CATEGORY_PROPERTY_KEY, category)
                .vertices().iterator();

        return (results != null && results.hasNext()) ? (AtlasVertex) results.next() : null;
    }

    AtlasVertex findTypeVertexByGuid(String typeGuid) {
        Iterator<AtlasVertex> vertices = atlasGraph.query().has(VERTEX_TYPE_PROPERTY_KEY, VERTEX_TYPE)
                .has(Constants.GUID_PROPERTY_KEY, typeGuid)
                .vertices().iterator();

        return (vertices != null && vertices.hasNext()) ? vertices.next() : null;
    }

    AtlasVertex findTypeVertexByGuidAndCategory(String typeGuid, TypeCategory category) {
        Iterator<AtlasVertex> vertices = atlasGraph.query().has(VERTEX_TYPE_PROPERTY_KEY, VERTEX_TYPE)
                .has(Constants.GUID_PROPERTY_KEY, typeGuid)
                .has(TYPE_CATEGORY_PROPERTY_KEY, category)
                .vertices().iterator();

        return (vertices != null && vertices.hasNext()) ? vertices.next() : null;
    }

    Iterator<AtlasVertex> findTypeVerticesByCategory(TypeCategory category) {

        return (Iterator<AtlasVertex>) atlasGraph.query().has(VERTEX_TYPE_PROPERTY_KEY, VERTEX_TYPE)
                .has(TYPE_CATEGORY_PROPERTY_KEY, category)
                .vertices().iterator();
    }

    AtlasVertex createTypeVertex(AtlasBaseTypeDef typeDef) {
        // Validate all the required checks
        Preconditions.checkArgument(StringUtils.isNotBlank(typeDef.getName()), "Type name can't be null/empty");

        AtlasVertex ret = atlasGraph.addVertex();

        if (StringUtils.isBlank(typeDef.getTypeVersion())) {
            typeDef.setTypeVersion("1.0");
        }

        if (typeDef.getVersion() == null) {
            typeDef.setVersion(1L);
        }

        if (StringUtils.isBlank(typeDef.getGuid())) {
            typeDef.setGuid(UUID.randomUUID().toString());
        }

        if (typeDef.getCreateTime() == null) {
            typeDef.setCreateTime(new Date());
        }

        if (typeDef.getUpdateTime() == null) {
            typeDef.setUpdateTime(new Date());
        }

        ret.setProperty(VERTEX_TYPE_PROPERTY_KEY, VERTEX_TYPE); // Mark as type vertex
        ret.setProperty(TYPE_CATEGORY_PROPERTY_KEY, getTypeCategory(typeDef));

        ret.setProperty(Constants.TYPENAME_PROPERTY_KEY, typeDef.getName());
        ret.setProperty(Constants.TYPEDESCRIPTION_PROPERTY_KEY,
                StringUtils.isNotBlank(typeDef.getDescription()) ? typeDef.getDescription() : typeDef.getName());

        if (StringUtils.isNotEmpty(typeDef.getServiceType())) {
            ret.setProperty(Constants.TYPESERVICETYPE_PROPERTY_KEY, typeDef.getServiceType());
        }

        ret.setProperty(Constants.TYPEVERSION_PROPERTY_KEY, typeDef.getTypeVersion());
        ret.setProperty(Constants.GUID_PROPERTY_KEY, typeDef.getGuid());
        ret.setProperty(Constants.CREATED_BY_KEY, getCurrentUser());
        ret.setProperty(Constants.TIMESTAMP_PROPERTY_KEY, typeDef.getCreateTime().getTime());
        ret.setProperty(Constants.MODIFIED_BY_KEY, getCurrentUser());
        ret.setProperty(Constants.MODIFICATION_TIMESTAMP_PROPERTY_KEY, typeDef.getUpdateTime().getTime());
        ret.setProperty(Constants.VERSION_PROPERTY_KEY, typeDef.getVersion());
        ret.setProperty(Constants.TYPEOPTIONS_PROPERTY_KEY, AtlasType.toJson(typeDef.getOptions()));

        return ret;
    }

    void updateTypeVertex(AtlasBaseTypeDef typeDef, AtlasVertex vertex) {
        if (!isTypeVertex(vertex)) {
            LOG.warn("updateTypeVertex(): not a type-vertex - {}", vertex);

            return;
        }

        updateVertexProperty(vertex, Constants.GUID_PROPERTY_KEY, typeDef.getGuid());
        /*
         * rename of a type is supported yet - as the typename is used to in the name of the edges from this vertex
         * To support rename of types, he edge names should be derived from an internal-name - not directly the typename
         *
        updateVertexProperty(vertex, Constants.TYPENAME_PROPERTY_KEY, typeDef.getName());
         */
        updateVertexProperty(vertex, Constants.TYPEDESCRIPTION_PROPERTY_KEY, typeDef.getDescription());
        updateVertexProperty(vertex, Constants.TYPEVERSION_PROPERTY_KEY, typeDef.getTypeVersion());
        updateVertexProperty(vertex, Constants.TYPEOPTIONS_PROPERTY_KEY, AtlasType.toJson(typeDef.getOptions()));

        if (StringUtils.isNotEmpty(typeDef.getServiceType())) {
            updateVertexProperty(vertex, Constants.TYPESERVICETYPE_PROPERTY_KEY, typeDef.getServiceType());
        }

        markVertexUpdated(vertex);
    }

    void deleteTypeVertexOutEdges(AtlasVertex vertex) throws AtlasBaseException {
        Iterable<AtlasEdge> edges = vertex.getEdges(AtlasEdgeDirection.OUT);

        for (AtlasEdge edge : edges) {
            atlasGraph.removeEdge(edge);
        }
    }

    /**
     * Look to see if there are any IN edges with the supplied label
     * @param vertex
     * @param label
     * @return
     * @throws AtlasBaseException
     */
    boolean hasIncomingEdgesWithLabel(AtlasVertex vertex, String label) throws AtlasBaseException {
        boolean foundEdges = false;
        Iterator<AtlasEdge> inEdges = vertex.getEdges(AtlasEdgeDirection.IN).iterator();

        while (inEdges.hasNext()) {
            AtlasEdge edge = inEdges.next();

            if (label.equals(edge.getLabel())) {
                foundEdges = true;
                break;
            }
        }
        return foundEdges;
    }

    void deleteTypeVertex(AtlasVertex vertex) throws AtlasBaseException {
        Iterator<AtlasEdge> inEdges = vertex.getEdges(AtlasEdgeDirection.IN).iterator();
        if (inEdges.hasNext()) {
            String name        = vertex.getProperty(Constants.TYPENAME_PROPERTY_KEY, String.class);
            throw new AtlasBaseException(AtlasErrorCode.TYPE_HAS_REFERENCES, name);
        }

        Iterable<AtlasEdge> edges = vertex.getEdges(AtlasEdgeDirection.OUT);

        for (AtlasEdge edge : edges) {
            atlasGraph.removeEdge(edge);
        }
        atlasGraph.removeVertex(vertex);
    }

    void vertexToTypeDef(AtlasVertex vertex, AtlasBaseTypeDef typeDef) {
        String name        = vertex.getProperty(Constants.TYPENAME_PROPERTY_KEY, String.class);
        String description = vertex.getProperty(Constants.TYPEDESCRIPTION_PROPERTY_KEY, String.class);
        String serviceType = vertex.getProperty(Constants.TYPESERVICETYPE_PROPERTY_KEY, String.class);
        String typeVersion = vertex.getProperty(Constants.TYPEVERSION_PROPERTY_KEY, String.class);
        String guid        = vertex.getProperty(Constants.GUID_PROPERTY_KEY, String.class);
        String createdBy   = vertex.getProperty(Constants.CREATED_BY_KEY, String.class);
        String updatedBy   = vertex.getProperty(Constants.MODIFIED_BY_KEY, String.class);
        Long   createTime  = vertex.getProperty(Constants.TIMESTAMP_PROPERTY_KEY, Long.class);
        Long   updateTime  = vertex.getProperty(Constants.MODIFICATION_TIMESTAMP_PROPERTY_KEY, Long.class);
        Object versionObj  = vertex.getProperty(Constants.VERSION_PROPERTY_KEY, Object.class);
        String options     = vertex.getProperty(Constants.TYPEOPTIONS_PROPERTY_KEY, String.class);

        Long version = null;

        if(versionObj instanceof Number) {
            version = ((Number)versionObj).longValue();
        } else if (versionObj != null) {
            version = Long.valueOf(versionObj.toString());
        } else {
            version = Long.valueOf(0);
        }


        typeDef.setName(name);
        typeDef.setDescription(description);
        typeDef.setServiceType(serviceType);
        typeDef.setTypeVersion(typeVersion);
        typeDef.setGuid(guid);
        typeDef.setCreatedBy(createdBy);
        typeDef.setUpdatedBy(updatedBy);

        if (createTime != null) {
            typeDef.setCreateTime(new Date(createTime));
        }

        if (updateTime != null) {
            typeDef.setUpdateTime(new Date(updateTime));
        }

        if (version != null) {
            typeDef.setVersion(version);
        }

        if (options != null) {
            typeDef.setOptions(AtlasType.fromJson(options, Map.class));
        }
    }

    boolean isTypeVertex(AtlasVertex vertex) {
        String vertexType = vertex.getProperty(Constants.VERTEX_TYPE_PROPERTY_KEY, String.class);

        return VERTEX_TYPE.equals(vertexType);
    }

    @VisibleForTesting
    public boolean isTypeVertex(AtlasVertex vertex, TypeCategory category) {
        boolean ret = false;

        if (isTypeVertex(vertex)) {
            Object objTypeCategory = vertex.getProperty(Constants.TYPE_CATEGORY_PROPERTY_KEY, Object.class);

            TypeCategory vertexCategory = null;

            if(objTypeCategory instanceof TypeCategory) {
                vertexCategory = (TypeCategory) objTypeCategory;
            } else if (objTypeCategory != null) {
                vertexCategory = TypeCategory.valueOf(objTypeCategory.toString());
            }

            ret = category.equals(vertexCategory);
        }

        return ret;
    }

    boolean isTypeVertex(AtlasVertex vertex, TypeCategory[] categories) {
        boolean ret = false;

        if (isTypeVertex(vertex)) {
            TypeCategory vertexCategory = vertex.getProperty(TYPE_CATEGORY_PROPERTY_KEY, TypeCategory.class);

            for (TypeCategory category : categories) {
                if (category.equals(vertexCategory)) {
                    ret = true;

                    break;
                }
            }
        }

        return ret;
    }

    AtlasEdge getOrCreateEdge(AtlasVertex outVertex, AtlasVertex inVertex, String edgeLabel) {
        AtlasEdge           ret   = null;
        Iterable<AtlasEdge> edges = outVertex.getEdges(AtlasEdgeDirection.OUT, edgeLabel);

        for (AtlasEdge edge : edges) {
            if (edge.getInVertex().getId().equals(inVertex.getId())) {
                ret = edge;
                break;
            }
        }

        if (ret == null) {
            ret = addEdge(outVertex, inVertex, edgeLabel);
        }

        return ret;
    }

    AtlasEdge addEdge(AtlasVertex outVertex, AtlasVertex inVertex, String edgeLabel) {

        return atlasGraph.addEdge(outVertex, inVertex, edgeLabel);
    }

    void removeEdge(AtlasVertex outVertex, AtlasVertex inVertex, String edgeLabel) {
        Iterable<AtlasEdge> edges = outVertex.getEdges(AtlasEdgeDirection.OUT, edgeLabel);

        for (AtlasEdge edge : edges) {
            if (edge.getInVertex().getId().equals(inVertex.getId())) {
                atlasGraph.removeEdge(edge);
            }
        }
    }

    void createSuperTypeEdges(AtlasVertex vertex, Set<String> superTypes, TypeCategory typeCategory)
            throws AtlasBaseException {
        Set<String> currentSuperTypes = getSuperTypeNames(vertex);

        if (CollectionUtils.isNotEmpty(superTypes)) {
            if (! superTypes.containsAll(currentSuperTypes)) {
                throw new AtlasBaseException(AtlasErrorCode.SUPERTYPE_REMOVAL_NOT_SUPPORTED);
            }

            for (String superType : superTypes) {
                AtlasVertex superTypeVertex = findTypeVertexByNameAndCategory(superType, typeCategory);

                getOrCreateEdge(vertex, superTypeVertex, AtlasGraphUtilsV2.SUPERTYPE_EDGE_LABEL);
            }
        } else if (CollectionUtils.isNotEmpty(currentSuperTypes)) {
            throw new AtlasBaseException(AtlasErrorCode.SUPERTYPE_REMOVAL_NOT_SUPPORTED);
        }
    }

    public void createEntityTypeEdges(AtlasVertex classificationVertex, Set<String> entityTypes) throws AtlasBaseException {
        Set<String> currentEntityTypes     = getEntityTypeNames(classificationVertex);
        String      classificationTypeName = classificationVertex.getProperty(Constants.TYPENAME_PROPERTY_KEY, String.class);

        if (CollectionUtils.isNotEmpty(entityTypes)) {
            if (!entityTypes.containsAll(currentEntityTypes)) {
                throw new AtlasBaseException(AtlasErrorCode.ENTITYTYPE_REMOVAL_NOT_SUPPORTED, classificationTypeName);
            }

            for (String entityType : entityTypes) {
                AtlasVertex entityTypeVertex = findTypeVertexByNameAndCategory(entityType, TypeCategory.CLASS);
                if (entityTypeVertex == null) {
                    throw new AtlasBaseException(AtlasErrorCode.CLASSIFICATIONDEF_INVALID_ENTITYTYPES, classificationTypeName,entityType);

                }
                getOrCreateEdge(classificationVertex, entityTypeVertex, AtlasGraphUtilsV2.ENTITYTYPE_EDGE_LABEL);
            }
        } else if (CollectionUtils.isNotEmpty(currentEntityTypes)) { // remove the restrictions, if present
            for (String entityType : currentEntityTypes) {
                AtlasVertex entityTypeVertex = findTypeVertexByNameAndCategory(entityType, TypeCategory.CLASS);

                if (entityTypeVertex == null) {
                    throw new AtlasBaseException(AtlasErrorCode.CLASSIFICATIONDEF_INVALID_ENTITYTYPES, classificationTypeName,entityType);

                }

                removeEdge(classificationVertex, entityTypeVertex, AtlasGraphUtilsV2.ENTITYTYPE_EDGE_LABEL);
            }

        }
    }

    Set<String>  getSuperTypeNames(AtlasVertex vertex) {
        return getTypeNamesFromEdges(vertex, AtlasGraphUtilsV2.SUPERTYPE_EDGE_LABEL);
    }

    Set<String>  getEntityTypeNames(AtlasVertex vertex) {
        return getTypeNamesFromEdges(vertex, AtlasGraphUtilsV2.ENTITYTYPE_EDGE_LABEL);
    }

    /**
     * Get the typename properties from the edges, that are associated with the vertex and have the supplied edge label.
     * @param vertex
     * @param edgeLabel
     * @return set of type names
     */
    private Set<String> getTypeNamesFromEdges(AtlasVertex vertex,String edgeLabel) {
        Set<String>    ret   = new HashSet<>();
        Iterable<AtlasEdge> edges = vertex.getEdges(AtlasEdgeDirection.OUT, edgeLabel);

        for (AtlasEdge edge : edges) {
            ret.add(edge.getInVertex().getProperty(Constants.TYPENAME_PROPERTY_KEY, String.class));
        }

        return ret;
    }

    TypeCategory getTypeCategory(AtlasBaseTypeDef typeDef) {
        switch (typeDef.getCategory()) {
            case ENTITY:
                return TypeCategory.CLASS;

            case CLASSIFICATION:
                return TypeCategory.TRAIT;

            case STRUCT:
                return TypeCategory.STRUCT;

            case ENUM:
                return TypeCategory.ENUM;

            case RELATIONSHIP:
                return TypeCategory.RELATIONSHIP;

            case BUSINESS_METADATA:
                return TypeCategory.BUSINESS_METADATA;
        }

        return null;
    }

    /*
     * update the given vertex property, if the new value is not-blank
     */
    private void updateVertexProperty(AtlasVertex vertex, String propertyName, String newValue) {
        if (StringUtils.isNotBlank(newValue)) {
            String currValue = vertex.getProperty(propertyName, String.class);

            if (!StringUtils.equals(currValue, newValue)) {
                vertex.setProperty(propertyName, newValue);
            }
        }
    }

    /*
     * update the given vertex property, if the new value is not-null
     */
    private void updateVertexProperty(AtlasVertex vertex, String propertyName, Date newValue) {
        if (newValue != null) {
            Number currValue = vertex.getProperty(propertyName, Number.class);

            if (currValue == null || !currValue.equals(newValue.getTime())) {
                vertex.setProperty(propertyName, newValue.getTime());
            }
        }
    }

    /*
     * increment the version value for this vertex
     */
    private void markVertexUpdated(AtlasVertex vertex) {
        Number currVersion = vertex.getProperty(Constants.VERSION_PROPERTY_KEY, Number.class);
        long   newVersion  = currVersion == null ? 1 : (currVersion.longValue() + 1);

        vertex.setProperty(Constants.MODIFIED_BY_KEY, getCurrentUser());
        vertex.setProperty(Constants.MODIFICATION_TIMESTAMP_PROPERTY_KEY, System.currentTimeMillis());
        vertex.setProperty(Constants.VERSION_PROPERTY_KEY, newVersion);
    }

    public static String getCurrentUser() {
        return RequestContext.getCurrentUser();
    }
}
