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

package org.apache.atlas.repository.graph;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import org.apache.atlas.ApplicationProperties;
import org.apache.atlas.AtlasErrorCode;
import org.apache.atlas.AtlasException;
import org.apache.atlas.discovery.SearchIndexer;
import org.apache.atlas.exception.AtlasBaseException;
import org.apache.atlas.ha.HAConfiguration;
import org.apache.atlas.listener.ActiveStateChangeHandler;
import org.apache.atlas.listener.ChangedTypeDefs;
import org.apache.atlas.listener.TypeDefChangeListener;
import org.apache.atlas.model.TypeCategory;
import org.apache.atlas.model.typedef.AtlasBaseTypeDef;
import org.apache.atlas.model.typedef.AtlasEntityDef;
import org.apache.atlas.model.typedef.AtlasEnumDef;
import org.apache.atlas.model.typedef.AtlasRelationshipDef;
import org.apache.atlas.model.typedef.AtlasStructDef;
import org.apache.atlas.model.typedef.AtlasStructDef.AtlasAttributeDef;
import org.apache.atlas.repository.Constants;
import org.apache.atlas.repository.IndexException;
import org.apache.atlas.repository.RepositoryException;
import org.apache.atlas.repository.graphdb.*;
import org.apache.atlas.repository.store.graph.v2.AtlasGraphUtilsV2;
import org.apache.atlas.type.*;
import org.apache.atlas.type.AtlasStructType.AtlasAttribute;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;

import static org.apache.atlas.model.typedef.AtlasBaseTypeDef.*;
import static org.apache.atlas.repository.Constants.*;
import static org.apache.atlas.repository.graphdb.AtlasCardinality.LIST;
import static org.apache.atlas.repository.graphdb.AtlasCardinality.SET;
import static org.apache.atlas.repository.graphdb.AtlasCardinality.SINGLE;
import static org.apache.atlas.repository.store.graph.v2.AtlasGraphUtilsV2.isReference;
import static org.apache.atlas.type.AtlasStructType.UNIQUE_ATTRIBUTE_SHADE_PROPERTY_PREFIX;
import static org.apache.atlas.type.AtlasTypeUtil.isArrayType;
import static org.apache.atlas.type.AtlasTypeUtil.isMapType;
import static org.apache.atlas.type.Constants.PENDING_TASKS_PROPERTY_KEY;


/**
 * Adds index for properties of a given type when its added before any instances are added.
 */
@Component
@Order(1)
public class GraphBackedSearchIndexer implements SearchIndexer, ActiveStateChangeHandler, TypeDefChangeListener {

    private static final Logger LOG = LoggerFactory.getLogger(GraphBackedSearchIndexer.class);

    private static final String VERTEX_ID_IN_IMPORT_KEY = "__vIdInImport";
    private static final String EDGE_ID_IN_IMPORT_KEY   = "__eIdInImport";
    private static final List<Class> INDEX_EXCLUSION_CLASSES = new ArrayList() {
        {
            add(Boolean.class);
            add(BigDecimal.class);
            add(BigInteger.class);
        }
    };

    // Added for type lookup when indexing the new typedefs
    private final AtlasTypeRegistry typeRegistry;
    private final List<IndexChangeListener> indexChangeListeners = new ArrayList<>();

    //allows injection of a dummy graph for testing
    private IAtlasGraphProvider provider;

    private boolean     recomputeIndexedKeys     = true;
    private boolean     recomputeEdgeIndexedKeys = true;
    private Set<String> vertexIndexKeys          = new HashSet<>();
    private Set<String> edgeIndexKeys            = new HashSet<>();

    public static boolean isValidSearchWeight(int searchWeight) {
        if (searchWeight != -1 ) {
            if (searchWeight < 1 || searchWeight > 10) {
                return false;
            }
        }
        return true;
    }

    public static boolean isStringAttribute(AtlasAttribute attribute) {
        return AtlasBaseTypeDef.ATLAS_TYPE_STRING.equals(attribute.getTypeName());
    }

    public enum UniqueKind { NONE, GLOBAL_UNIQUE, PER_TYPE_UNIQUE }

    @Inject
    public GraphBackedSearchIndexer(AtlasTypeRegistry typeRegistry) throws AtlasException {
        this(new AtlasGraphProvider(), ApplicationProperties.get(), typeRegistry);
    }

    @VisibleForTesting
    GraphBackedSearchIndexer(IAtlasGraphProvider provider, Configuration configuration, AtlasTypeRegistry typeRegistry)
            throws IndexException, RepositoryException {
        this.provider     = provider;
        this.typeRegistry = typeRegistry;

        //make sure solr index follows graph backed index listener
        addIndexListener(new SolrIndexHelper(typeRegistry));

        if (!HAConfiguration.isHAEnabled(configuration)) {
            initialize(provider.get());
        }
        notifyInitializationStart();
    }

    public void addIndexListener(IndexChangeListener listener) {
        indexChangeListeners.add(listener);
    }


    /**
     * Initialize global indices for JanusGraph on server activation.
     *
     * Since the indices are shared state, we need to do this only from an active instance.
     */
    @Override
    public void instanceIsActive() throws AtlasException {
        LOG.info("Reacting to active: initializing index");
        try {
            initialize();
        } catch (RepositoryException | IndexException e) {
            throw new AtlasException("Error in reacting to active on initialization", e);
        }
    }

    @Override
    public void instanceIsPassive() {
        LOG.info("Reacting to passive state: No action right now.");
    }

    @Override
    public int getHandlerOrder() {
        return HandlerOrder.GRAPH_BACKED_SEARCH_INDEXER.getOrder();
    }

    @Override
    public void onChange(ChangedTypeDefs changedTypeDefs) throws AtlasBaseException {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Processing changed typedefs {}", changedTypeDefs);
        }

        AtlasGraphManagement management = null;

        try {
            management = provider.get().getManagementSystem();

            // Update index for newly created types
            if (CollectionUtils.isNotEmpty(changedTypeDefs.getCreatedTypeDefs())) {
                for (AtlasBaseTypeDef typeDef : changedTypeDefs.getCreatedTypeDefs()) {
                    updateIndexForTypeDef(management, typeDef);
                }
            }

            // Update index for updated types
            if (CollectionUtils.isNotEmpty(changedTypeDefs.getUpdatedTypeDefs())) {
                for (AtlasBaseTypeDef typeDef : changedTypeDefs.getUpdatedTypeDefs()) {
                    updateIndexForTypeDef(management, typeDef);
                }
            }

            // Invalidate the property key for deleted types
            if (CollectionUtils.isNotEmpty(changedTypeDefs.getDeletedTypeDefs())) {
                for (AtlasBaseTypeDef typeDef : changedTypeDefs.getDeletedTypeDefs()) {
                    deleteIndexForType(management, typeDef);
                }
            }

            //resolve index fields names for the new entity attributes.
            resolveIndexFieldNames(management, changedTypeDefs);

            createEdgeLabels(management, changedTypeDefs.getCreatedTypeDefs());
            createEdgeLabels(management, changedTypeDefs.getUpdatedTypeDefs());

            //Commit indexes
            commit(management);
        } catch (RepositoryException | IndexException e) {
            LOG.error("Failed to update indexes for changed typedefs", e);
            attemptRollback(changedTypeDefs, management);
        }

        notifyChangeListeners(changedTypeDefs);
    }

    @Override
    public void onLoadCompletion() throws AtlasBaseException {
        if(LOG.isDebugEnabled()) {
            LOG.debug("Type definition load completed. Informing the completion to IndexChangeListeners.");
        }

        Collection<AtlasBaseTypeDef> typeDefs = new ArrayList<>();

        typeDefs.addAll(typeRegistry.getAllEntityDefs());
        typeDefs.addAll(typeRegistry.getAllBusinessMetadataDefs());

        ChangedTypeDefs      changedTypeDefs = new ChangedTypeDefs(null, new ArrayList<>(typeDefs), null);
        AtlasGraphManagement management      = null;

        try {
            management = provider.get().getManagementSystem();

            //resolve index fields names
            resolveIndexFieldNames(management, changedTypeDefs);

            //Commit indexes
            commit(management);

            notifyInitializationCompletion(changedTypeDefs);
        } catch (RepositoryException | IndexException e) {
            LOG.error("Failed to update indexes for changed typedefs", e);
            attemptRollback(changedTypeDefs, management);
        }
    }

    public Set<String> getVertexIndexKeys() {
        if (recomputeIndexedKeys) {
            AtlasGraphManagement management = null;

            try {
                management = provider.get().getManagementSystem();

                if (management != null) {
                    AtlasGraphIndex vertexIndex = management.getGraphIndex(VERTEX_INDEX);

                    if (vertexIndex != null) {
                        recomputeIndexedKeys = false;

                        Set<String> indexKeys = new HashSet<>();

                        for (AtlasPropertyKey fieldKey : vertexIndex.getFieldKeys()) {
                            indexKeys.add(fieldKey.getName());
                        }

                        vertexIndexKeys = indexKeys;
                    }

                    management.commit();
                }
            } catch (Exception excp) {
                LOG.error("getVertexIndexKeys(): failed to get indexedKeys from graph", excp);

                if (management != null) {
                    try {
                        management.rollback();
                    } catch (Exception e) {
                        LOG.error("getVertexIndexKeys(): rollback failed", e);
                    }
                }
            }
        }

        return vertexIndexKeys;
    }

    public Set<String> getEdgeIndexKeys() {
        if (recomputeEdgeIndexedKeys) {
            AtlasGraphManagement management = null;

            try {
                management = provider.get().getManagementSystem();

                if (management != null) {
                    AtlasGraphIndex edgeIndex = management.getGraphIndex(EDGE_INDEX);

                    if (edgeIndex != null) {
                        recomputeEdgeIndexedKeys = false;

                        Set<String> indexKeys = new HashSet<>();

                        for (AtlasPropertyKey fieldKey : edgeIndex.getFieldKeys()) {
                            indexKeys.add(fieldKey.getName());
                        }

                        edgeIndexKeys = indexKeys;
                    }

                    management.commit();
                }
            } catch (Exception excp) {
                LOG.error("getEdgeIndexKeys(): failed to get indexedKeys from graph", excp);

                if (management != null) {
                    try {
                        management.rollback();
                    } catch (Exception e) {
                        LOG.error("getEdgeIndexKeys(): rollback failed", e);
                    }
                }
            }
        }

        return edgeIndexKeys;
    }

    /**
     * Initializes the indices for the graph - create indices for Global AtlasVertex Keys
     */
    private void initialize() throws RepositoryException, IndexException {
        initialize(provider.get());
    }
    
    /**
     * Initializes the indices for the graph - create indices for Global AtlasVertex and AtlasEdge Keys
     */
    private void initialize(AtlasGraph graph) throws RepositoryException, IndexException {
        AtlasGraphManagement management = graph.getManagementSystem();

        try {
            LOG.info("Creating indexes for graph.");

            if (management.getGraphIndex(VERTEX_INDEX) == null) {
                management.createVertexMixedIndex(VERTEX_INDEX, BACKING_INDEX, Collections.emptyList());

                LOG.info("Created index : {}", VERTEX_INDEX);
            }

            if (management.getGraphIndex(EDGE_INDEX) == null) {
                management.createEdgeMixedIndex(EDGE_INDEX, BACKING_INDEX, Collections.emptyList());

                LOG.info("Created index : {}", EDGE_INDEX);
            }

            if (management.getGraphIndex(FULLTEXT_INDEX) == null) {
                management.createFullTextMixedIndex(FULLTEXT_INDEX, BACKING_INDEX, Collections.emptyList());

                LOG.info("Created index : {}", FULLTEXT_INDEX);
            }

            // create vertex indexes
            createCommonVertexIndex(management, GUID_PROPERTY_KEY, UniqueKind.GLOBAL_UNIQUE, String.class, SINGLE, true, false);
            createCommonVertexIndex(management, HISTORICAL_GUID_PROPERTY_KEY, UniqueKind.GLOBAL_UNIQUE, String.class, SINGLE, true, false);

            createCommonVertexIndex(management, TYPENAME_PROPERTY_KEY, UniqueKind.GLOBAL_UNIQUE, String.class, SINGLE, true, false);
            createCommonVertexIndex(management, TYPESERVICETYPE_PROPERTY_KEY, UniqueKind.NONE, String.class, SINGLE, true, false);
            createCommonVertexIndex(management, VERTEX_TYPE_PROPERTY_KEY, UniqueKind.NONE, String.class, SINGLE, true, false);
            createCommonVertexIndex(management, VERTEX_ID_IN_IMPORT_KEY, UniqueKind.NONE, Long.class, SINGLE, true, false);

            createCommonVertexIndex(management, ENTITY_TYPE_PROPERTY_KEY, UniqueKind.NONE, String.class, SINGLE, true, false);
            createCommonVertexIndex(management, SUPER_TYPES_PROPERTY_KEY, UniqueKind.NONE, String.class, SET, true, false);
            createCommonVertexIndex(management, TIMESTAMP_PROPERTY_KEY, UniqueKind.NONE, Long.class, SINGLE, false, false);
            createCommonVertexIndex(management, MODIFICATION_TIMESTAMP_PROPERTY_KEY, UniqueKind.NONE, Long.class, SINGLE, false, false);
            createCommonVertexIndex(management, STATE_PROPERTY_KEY, UniqueKind.NONE, String.class, SINGLE, false, false);
            createCommonVertexIndex(management, CREATED_BY_KEY, UniqueKind.NONE, String.class, SINGLE, false, false, true);
            createCommonVertexIndex(management, CLASSIFICATION_TEXT_KEY, UniqueKind.NONE, String.class, SINGLE, false, false);
            createCommonVertexIndex(management, MODIFIED_BY_KEY, UniqueKind.NONE, String.class, SINGLE, false, false, true);
            createCommonVertexIndex(management, CLASSIFICATION_NAMES_KEY, UniqueKind.NONE, String.class, SINGLE, true, false);
            createCommonVertexIndex(management, PROPAGATED_CLASSIFICATION_NAMES_KEY, UniqueKind.NONE, String.class, SINGLE, true, false);
            createCommonVertexIndex(management, TRAIT_NAMES_PROPERTY_KEY, UniqueKind.NONE, String.class, SET, true, true);
            createCommonVertexIndex(management, PROPAGATED_TRAIT_NAMES_PROPERTY_KEY, UniqueKind.NONE, String.class, LIST, true, true);
            createCommonVertexIndex(management, PENDING_TASKS_PROPERTY_KEY, UniqueKind.NONE, String.class, SET, true, false);
            createCommonVertexIndex(management, IS_INCOMPLETE_PROPERTY_KEY, UniqueKind.NONE, Integer.class, SINGLE, true, true);
            createCommonVertexIndex(management, CUSTOM_ATTRIBUTES_PROPERTY_KEY, UniqueKind.NONE, String.class, SINGLE, true, false);
            createCommonVertexIndex(management, LABELS_PROPERTY_KEY, UniqueKind.NONE, String.class, SINGLE, true, false);
            createCommonVertexIndex(management, ENTITY_DELETED_TIMESTAMP_PROPERTY_KEY, UniqueKind.NONE, Long.class, SINGLE, true, false);

            createCommonVertexIndex(management, PATCH_ID_PROPERTY_KEY, UniqueKind.GLOBAL_UNIQUE, String.class, SINGLE, true, false);
            createCommonVertexIndex(management, PATCH_DESCRIPTION_PROPERTY_KEY, UniqueKind.NONE, String.class, SINGLE, true, false);
            createCommonVertexIndex(management, PATCH_TYPE_PROPERTY_KEY, UniqueKind.NONE, String.class, SINGLE, true, false);
            createCommonVertexIndex(management, PATCH_ACTION_PROPERTY_KEY, UniqueKind.NONE, String.class, SINGLE, true, false);
            createCommonVertexIndex(management, PATCH_STATE_PROPERTY_KEY, UniqueKind.NONE, String.class, SINGLE, true, false);

            // tasks
            createCommonVertexIndex(management, TASK_GUID, UniqueKind.GLOBAL_UNIQUE, String.class, SINGLE, true, false);
            createCommonVertexIndex(management, TASK_TYPE_PROPERTY_KEY, UniqueKind.NONE, String.class, SINGLE, true, false);
            createCommonVertexIndex(management, TASK_CREATED_TIME, UniqueKind.NONE, Long.class, SINGLE, true, false);
            createCommonVertexIndex(management, TASK_STATUS, UniqueKind.NONE, String.class, SINGLE, true, false);

            // index recovery
            createCommonVertexIndex(management, PROPERTY_KEY_INDEX_RECOVERY_NAME, UniqueKind.GLOBAL_UNIQUE, String.class, SINGLE, true, false);

            //metrics
            createCommonVertexIndex(management," __AtlasMetricsStat.metricsId", UniqueKind.GLOBAL_UNIQUE, String.class, SINGLE, true, false);
            createCommonVertexIndex(management," __AtlasMetricsStat.__u_metricsId", UniqueKind.GLOBAL_UNIQUE, String.class, SINGLE, true, false);
            createCommonVertexIndex(management," __AtlasMetricsStat.metrics", UniqueKind.NONE, String.class, SINGLE, true, false);
            createCommonVertexIndex(management," __AtlasMetricsStat.collectionTime", UniqueKind.GLOBAL_UNIQUE, String.class, SINGLE, true, false);
            createCommonVertexIndex(management," __AtlasMetricsStat.timeToLiveMillis", UniqueKind.NONE, String.class, SINGLE, true, false);

            // create vertex-centric index
            createVertexCentricIndex(management, CLASSIFICATION_LABEL, AtlasEdgeDirection.BOTH, CLASSIFICATION_EDGE_NAME_PROPERTY_KEY, String.class, SINGLE);
            createVertexCentricIndex(management, CLASSIFICATION_LABEL, AtlasEdgeDirection.BOTH, CLASSIFICATION_EDGE_IS_PROPAGATED_PROPERTY_KEY, Boolean.class, SINGLE);
            createVertexCentricIndex(management, CLASSIFICATION_LABEL, AtlasEdgeDirection.BOTH, Arrays.asList(CLASSIFICATION_EDGE_NAME_PROPERTY_KEY, CLASSIFICATION_EDGE_IS_PROPAGATED_PROPERTY_KEY));

            // create edge indexes
            createEdgeIndex(management, RELATIONSHIP_GUID_PROPERTY_KEY, String.class, SINGLE, true);
            createEdgeIndex(management, EDGE_ID_IN_IMPORT_KEY, String.class, SINGLE, true);
            createEdgeIndex(management, RELATIONSHIP_TYPE_PROPERTY_KEY, String.class, SINGLE, true);

            // create fulltext indexes
            createFullTextIndex(management, ENTITY_TEXT_PROPERTY_KEY, String.class, SINGLE);

            createPropertyKey(management, IS_PROXY_KEY, Boolean.class, SINGLE);
            createPropertyKey(management, PROVENANCE_TYPE_KEY, Integer.class, SINGLE);
            createPropertyKey(management, HOME_ID_KEY, String.class, SINGLE);

            commit(management);

            LOG.info("Index creation for global keys complete.");
        } catch (Throwable t) {
            LOG.error("GraphBackedSearchIndexer.initialize() failed", t);

            rollback(management);
            throw new RepositoryException(t);
        }
    }

    private void resolveIndexFieldNames(AtlasGraphManagement managementSystem, ChangedTypeDefs changedTypeDefs) {
        List<? extends AtlasBaseTypeDef> createdTypeDefs = changedTypeDefs.getCreatedTypeDefs();

        if(createdTypeDefs != null) {
            resolveIndexFieldNames(managementSystem, createdTypeDefs);
        }

        List<? extends AtlasBaseTypeDef> updatedTypeDefs = changedTypeDefs.getUpdatedTypeDefs();

        if(updatedTypeDefs != null) {
            resolveIndexFieldNames(managementSystem, updatedTypeDefs);
        }
    }

    private void resolveIndexFieldNames(AtlasGraphManagement managementSystem, List<? extends AtlasBaseTypeDef> typeDefs) {
        for(AtlasBaseTypeDef baseTypeDef: typeDefs) {
            if(TypeCategory.ENTITY.equals(baseTypeDef.getCategory())) {
                AtlasEntityType entityType = typeRegistry.getEntityTypeByName(baseTypeDef.getName());

                resolveIndexFieldNames(managementSystem, entityType);
            } else if(TypeCategory.BUSINESS_METADATA.equals(baseTypeDef.getCategory())) {
                AtlasBusinessMetadataType businessMetadataType = typeRegistry.getBusinessMetadataTypeByName(baseTypeDef.getName());

                resolveIndexFieldNames(managementSystem, businessMetadataType);
            } else {
                LOG.debug("Ignoring type definition {}", baseTypeDef.getName());
            }
        }
    }

    private void resolveIndexFieldNames(AtlasGraphManagement managementSystem, AtlasStructType structType) {
        for(AtlasAttribute attribute: structType.getAllAttributes().values()) {
            resolveIndexFieldName(managementSystem, attribute);
        }
    }

    private void resolveIndexFieldName(AtlasGraphManagement managementSystem, AtlasAttribute attribute) {
        try {
            if (attribute.getIndexFieldName() == null && TypeCategory.PRIMITIVE.equals(attribute.getAttributeType().getTypeCategory())) {
                AtlasStructType definedInType = attribute.getDefinedInType();
                AtlasAttribute  baseInstance  = definedInType != null ? definedInType.getAttribute(attribute.getName()) : null;

                if (baseInstance != null && baseInstance.getIndexFieldName() != null) {
                    attribute.setIndexFieldName(baseInstance.getIndexFieldName());
                } else if (isIndexApplicable(getPrimitiveClass(attribute.getTypeName()), toAtlasCardinality(attribute.getAttributeDef().getCardinality()))) {
                    AtlasPropertyKey propertyKey = managementSystem.getPropertyKey(attribute.getVertexPropertyName());
                    boolean isStringField = AtlasAttributeDef.IndexType.STRING.equals(attribute.getIndexType());
                    if (propertyKey != null) {
                        String indexFieldName = managementSystem.getIndexFieldName(Constants.VERTEX_INDEX, propertyKey, isStringField);

                        attribute.setIndexFieldName(indexFieldName);

                        if (baseInstance != null) {
                            baseInstance.setIndexFieldName(indexFieldName);
                        }

                        typeRegistry.addIndexFieldName(attribute.getVertexPropertyName(), indexFieldName);

                        LOG.info("Property {} is mapped to index field name {}", attribute.getQualifiedName(), attribute.getIndexFieldName());
                    } else {
                        LOG.warn("resolveIndexFieldName(attribute={}): propertyKey is null for vertextPropertyName={}", attribute.getQualifiedName(), attribute.getVertexPropertyName());
                    }
                }
            }
        } catch (Exception excp) {
            LOG.warn("resolveIndexFieldName(attribute={}) failed.", attribute.getQualifiedName(), excp);
        }
    }

    private void createCommonVertexIndex(AtlasGraphManagement management,
                                         String propertyName,
                                         UniqueKind uniqueKind,
                                         Class propertyClass,
                                         AtlasCardinality cardinality,
                                         boolean createCompositeIndex,
                                         boolean createCompositeIndexWithTypeAndSuperTypes) {
        createCommonVertexIndex(management, propertyName, uniqueKind, propertyClass, cardinality, createCompositeIndex, createCompositeIndexWithTypeAndSuperTypes, false);
    }

    private void createCommonVertexIndex(AtlasGraphManagement management,
                                         String propertyName,
                                         UniqueKind uniqueKind,
                                         Class propertyClass,
                                         AtlasCardinality cardinality,
                                         boolean createCompositeIndex,
                                         boolean createCompositeIndexWithTypeAndSuperTypes,
                                         boolean isStringField) {
        if(isStringField && String.class.equals(propertyClass)) {

            propertyName = AtlasAttribute.VERTEX_PROPERTY_PREFIX_STRING_INDEX_TYPE +propertyName;
            LOG.debug("Creating the common attribute '{}' as string field.", propertyName);
        }

        final String indexFieldName = createVertexIndex(management,
                                                        propertyName,
                                                        uniqueKind,
                                                        propertyClass,
                                                        cardinality,
                                                        createCompositeIndex,
                                                        createCompositeIndexWithTypeAndSuperTypes, isStringField);
        if(indexFieldName != null) {
            typeRegistry.addIndexFieldName(propertyName, indexFieldName);
        }
    }

    private void addIndexForType(AtlasGraphManagement management, AtlasBaseTypeDef typeDef) {
        if (typeDef instanceof AtlasEnumDef) {
            // Only handle complex types like Struct, Classification and Entity
            return;
        }
        if (typeDef instanceof AtlasStructDef) {
            AtlasStructDef structDef = (AtlasStructDef) typeDef;
            List<AtlasAttributeDef> attributeDefs = structDef.getAttributeDefs();
            if (CollectionUtils.isNotEmpty(attributeDefs)) {
                for (AtlasAttributeDef attributeDef : attributeDefs) {
                    createIndexForAttribute(management, structDef, attributeDef);
                }
            }
        } else if (!AtlasTypeUtil.isBuiltInType(typeDef.getName())){
            throw new IllegalArgumentException("bad data type" + typeDef.getName());
        }
    }

    private void deleteIndexForType(AtlasGraphManagement management, AtlasBaseTypeDef typeDef) {
        Preconditions.checkNotNull(typeDef, "Cannot process null typedef");

        if (LOG.isDebugEnabled()) {
            LOG.debug("Deleting indexes for type {}", typeDef.getName());
        }

        if (typeDef instanceof AtlasStructDef) {
            AtlasStructDef          structDef     = (AtlasStructDef) typeDef;
            List<AtlasAttributeDef> attributeDefs = structDef.getAttributeDefs();

            if (CollectionUtils.isNotEmpty(attributeDefs)) {
                for (AtlasAttributeDef attributeDef : attributeDefs) {
                    deleteIndexForAttribute(management, typeDef.getName(), attributeDef);
                }
            }
        }

        LOG.info("Completed deleting indexes for type {}", typeDef.getName());
    }

    private void createIndexForAttribute(AtlasGraphManagement management, AtlasStructDef structDef, AtlasAttributeDef attributeDef) {
        String           qualifiedName  = AtlasAttribute.getQualifiedAttributeName(structDef, attributeDef.getName());
        final String     propertyName   = AtlasAttribute.generateVertexPropertyName(structDef, attributeDef, qualifiedName);
        AtlasCardinality cardinality    = toAtlasCardinality(attributeDef.getCardinality());
        boolean          isUnique       = attributeDef.getIsUnique();
        boolean          isIndexable    = attributeDef.getIsIndexable();
        String           attribTypeName = attributeDef.getTypeName();
        boolean          isBuiltInType  = AtlasTypeUtil.isBuiltInType(attribTypeName);
        boolean          isArrayType    = isArrayType(attribTypeName);
        boolean          isMapType      = isMapType(attribTypeName);
        final String     uniqPropName   = isUnique ? AtlasGraphUtilsV2.encodePropertyKey(structDef.getName() + "." + UNIQUE_ATTRIBUTE_SHADE_PROPERTY_PREFIX + attributeDef.getName()) : null;
        final AtlasAttributeDef.IndexType indexType      = attributeDef.getIndexType();

        try {
            AtlasType atlasType     = typeRegistry.getType(structDef.getName());
            AtlasType attributeType = typeRegistry.getType(attribTypeName);

            if (isClassificationType(attributeType)) {
                LOG.warn("Ignoring non-indexable attribute {}", attribTypeName);
            }

            if (isArrayType) {
                createLabelIfNeeded(management, propertyName, attribTypeName);

                AtlasArrayType arrayType   = (AtlasArrayType) attributeType;
                boolean        isReference = isReference(arrayType.getElementType());

                if (!isReference) {
                    createPropertyKey(management, propertyName, ArrayList.class, SINGLE);
                }
            }

            if (isMapType) {
                createLabelIfNeeded(management, propertyName, attribTypeName);

                AtlasMapType mapType     = (AtlasMapType) attributeType;
                boolean      isReference = isReference(mapType.getValueType());

                if (!isReference) {
                    createPropertyKey(management, propertyName, HashMap.class, SINGLE);
                }
            }

            if (isEntityType(attributeType)) {
                createEdgeLabel(management, propertyName);

            } else if (isBuiltInType) {
                if (isRelationshipType(atlasType)) {
                    createEdgeIndex(management, propertyName, getPrimitiveClass(attribTypeName), cardinality, isIndexable);
                } else {
                    Class primitiveClassType = getPrimitiveClass(attribTypeName);
                    boolean isStringField = false;
                    if(primitiveClassType == String.class) {
                        isStringField = AtlasAttributeDef.IndexType.STRING.equals(indexType);

                    }
                    createVertexIndex(management, propertyName, UniqueKind.NONE, getPrimitiveClass(attribTypeName), cardinality, isIndexable, false, isStringField);

                    if (uniqPropName != null) {
                        createVertexIndex(management, uniqPropName, UniqueKind.PER_TYPE_UNIQUE, getPrimitiveClass(attribTypeName), cardinality, isIndexable, true, isStringField);
                    }
                }
            } else if (isEnumType(attributeType)) {
                if (isRelationshipType(atlasType)) {
                    createEdgeIndex(management, propertyName, String.class, cardinality, false);
                } else {
                    createVertexIndex(management, propertyName, UniqueKind.NONE, String.class, cardinality, isIndexable, false, false);

                    if (uniqPropName != null) {
                        createVertexIndex(management, uniqPropName, UniqueKind.PER_TYPE_UNIQUE, String.class, cardinality, isIndexable, true, false);
                    }
                }
            } else if (isStructType(attributeType)) {
                AtlasStructDef attribureStructDef = typeRegistry.getStructDefByName(attribTypeName);
                updateIndexForTypeDef(management, attribureStructDef);
            }
        } catch (AtlasBaseException e) {
            LOG.error("No type exists for {}", attribTypeName, e);
        }
    }

    private void deleteIndexForAttribute(AtlasGraphManagement management, String typeName, AtlasAttributeDef attributeDef) {
        final String propertyName = AtlasGraphUtilsV2.encodePropertyKey(typeName + "." + attributeDef.getName());

        try {
            if (management.containsPropertyKey(propertyName)) {
                LOG.info("Deleting propertyKey {}, for attribute {}.{}", propertyName, typeName, attributeDef.getName());

                management.deletePropertyKey(propertyName);
            }
        } catch (Exception excp) {
            LOG.warn("Failed to delete propertyKey {}, for attribute {}.{}", propertyName, typeName, attributeDef.getName());
        }
    }

    /**
     * gets the encoded property name for the attribute passed in.
     * @param baseTypeDef the type system of the attribute
     * @param attributeDef the attribute definition
     * @return the encoded property name for the attribute passed in.
     */
    public static String getEncodedPropertyName(AtlasStructDef baseTypeDef, AtlasAttributeDef attributeDef) {
        return AtlasAttribute.getQualifiedAttributeName(baseTypeDef, attributeDef.getName());
    }

    private void createLabelIfNeeded(final AtlasGraphManagement management, final String propertyName, final String attribTypeName) {
        // If any of the referenced typename is of type Entity or Struct then the edge label needs to be created
        for (String typeName : AtlasTypeUtil.getReferencedTypeNames(attribTypeName)) {
            if (typeRegistry.getEntityDefByName(typeName) != null || typeRegistry.getStructDefByName(typeName) != null) {
                // Create the edge label upfront to avoid running into concurrent call issue (ATLAS-2092)
                createEdgeLabel(management, propertyName);
            }
        }
    }

    private boolean isEntityType(AtlasType type) {
        return type instanceof AtlasEntityType;
    }

    private boolean isClassificationType(AtlasType type) {
        return type instanceof AtlasClassificationType;
    }

    private boolean isEnumType(AtlasType type) {
        return type instanceof AtlasEnumType;
    }

    private boolean isStructType(AtlasType type) {
        return type instanceof AtlasStructType;
    }

    private boolean isRelationshipType(AtlasType type) {
        return type instanceof AtlasRelationshipType;
    }

    public Class getPrimitiveClass(String attribTypeName) {
        String attributeTypeName = attribTypeName.toLowerCase();

        switch (attributeTypeName) {
            case ATLAS_TYPE_BOOLEAN:
                return Boolean.class;
            case ATLAS_TYPE_BYTE:
                return Byte.class;
            case ATLAS_TYPE_SHORT:
                return Short.class;
            case ATLAS_TYPE_INT:
                return Integer.class;
            case ATLAS_TYPE_LONG:
            case ATLAS_TYPE_DATE:
                return Long.class;
            case ATLAS_TYPE_FLOAT:
                return Float.class;
            case ATLAS_TYPE_DOUBLE:
                return Double.class;
            case ATLAS_TYPE_BIGINTEGER:
                return BigInteger.class;
            case ATLAS_TYPE_BIGDECIMAL:
                return BigDecimal.class;
            case ATLAS_TYPE_STRING:
                return String.class;
        }

        throw new IllegalArgumentException(String.format("Unknown primitive typename %s", attribTypeName));
    }

    public AtlasCardinality toAtlasCardinality(AtlasAttributeDef.Cardinality cardinality) {
        switch (cardinality) {
            case SINGLE:
                return SINGLE;
            case LIST:
                return LIST;
            case SET:
                return SET;
        }
        // Should never reach this point
        throw new IllegalArgumentException(String.format("Bad cardinality %s", cardinality));
    }

    private void createEdgeLabel(final AtlasGraphManagement management, final String propertyName) {
        // Create the edge label upfront to avoid running into concurrent call issue (ATLAS-2092)
        // ATLAS-2092 addresses this problem by creating the edge label upfront while type creation
        // which resolves the race condition during the entity creation

        String label = Constants.INTERNAL_PROPERTY_KEY_PREFIX + propertyName;

        createEdgeLabelUsingLabelName(management, label);
    }

    private void createEdgeLabelUsingLabelName(final AtlasGraphManagement management, final String label) {
        if (StringUtils.isEmpty(label)) {
            return;
        }

        org.apache.atlas.repository.graphdb.AtlasEdgeLabel edgeLabel = management.getEdgeLabel(label);

        if (edgeLabel == null) {
            management.makeEdgeLabel(label);

            LOG.info("Created edge label {} ", label);
        }
    }

    private AtlasPropertyKey createPropertyKey(AtlasGraphManagement management, String propertyName, Class propertyClass, AtlasCardinality cardinality) {
        AtlasPropertyKey propertyKey = management.getPropertyKey(propertyName);

        if (propertyKey == null) {
            propertyKey = management.makePropertyKey(propertyName, propertyClass, cardinality);
        }

        return propertyKey;
    }

    public String createVertexIndex(AtlasGraphManagement management, String propertyName, UniqueKind uniqueKind, Class propertyClass,
                                  AtlasCardinality cardinality, boolean createCompositeIndex, boolean createCompositeIndexWithTypeAndSuperTypes, boolean isStringField) {
        String indexFieldName = null;

        if (propertyName != null) {
            AtlasPropertyKey propertyKey = management.getPropertyKey(propertyName);

            if (propertyKey == null) {
                propertyKey = management.makePropertyKey(propertyName, propertyClass, cardinality);

                if (isIndexApplicable(propertyClass, cardinality)) {
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Creating backing index for vertex property {} of type {} ", propertyName, propertyClass.getName());
                    }

                    indexFieldName = management.addMixedIndex(VERTEX_INDEX, propertyKey, isStringField);
                    LOG.info("Created backing index for vertex property {} of type {} ", propertyName, propertyClass.getName());
                }
            }

            if(indexFieldName == null && isIndexApplicable(propertyClass, cardinality)) {
                indexFieldName = management.getIndexFieldName(VERTEX_INDEX, propertyKey, isStringField);
            }

            if (propertyKey != null) {
                if (createCompositeIndex || uniqueKind == UniqueKind.GLOBAL_UNIQUE || uniqueKind == UniqueKind.PER_TYPE_UNIQUE) {
                    createVertexCompositeIndex(management, propertyClass, propertyKey, uniqueKind == UniqueKind.GLOBAL_UNIQUE);
                }

                if (createCompositeIndexWithTypeAndSuperTypes) {
                    createVertexCompositeIndexWithTypeName(management, propertyClass, propertyKey, uniqueKind == UniqueKind.PER_TYPE_UNIQUE);
                    createVertexCompositeIndexWithSuperTypeName(management, propertyClass, propertyKey);
                }
            } else {
                LOG.warn("Index not created for {}: propertyKey is null", propertyName);
            }
        }

        return indexFieldName;
    }

    private void createVertexCentricIndex(AtlasGraphManagement management, String edgeLabel, AtlasEdgeDirection edgeDirection,
                                          String propertyName, Class propertyClass, AtlasCardinality cardinality) {
        AtlasPropertyKey propertyKey = management.getPropertyKey(propertyName);

        if (propertyKey == null) {
            propertyKey = management.makePropertyKey(propertyName, propertyClass, cardinality);
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("Creating vertex-centric index for edge label: {} direction: {} for property: {} of type: {} ",
                    edgeLabel, edgeDirection.name(), propertyName, propertyClass.getName());
        }

        final String indexName = edgeLabel + propertyKey.getName();

        if (!management.edgeIndexExist(edgeLabel, indexName)) {
            management.createEdgeIndex(edgeLabel, indexName, edgeDirection, Collections.singletonList(propertyKey));

            LOG.info("Created vertex-centric index for edge label: {} direction: {} for property: {} of type: {}",
                    edgeLabel, edgeDirection.name(), propertyName, propertyClass.getName());
        }
    }

    private void createVertexCentricIndex(AtlasGraphManagement management, String edgeLabel, AtlasEdgeDirection edgeDirection, List<String> propertyNames) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Creating vertex-centric index for edge label: {} direction: {} for properties: {}",
                    edgeLabel, edgeDirection.name(), propertyNames);
        }

        String                 indexName    = edgeLabel;
        List<AtlasPropertyKey> propertyKeys = new ArrayList<>();

        for (String propertyName : propertyNames) {
            AtlasPropertyKey propertyKey = management.getPropertyKey(propertyName);

            if (propertyKey != null) {
                propertyKeys.add(propertyKey);
                indexName = indexName + propertyKey.getName();
            }
        }

        if (!management.edgeIndexExist(edgeLabel, indexName) && CollectionUtils.isNotEmpty(propertyKeys)) {
            management.createEdgeIndex(edgeLabel, indexName, edgeDirection, propertyKeys);

            LOG.info("Created vertex-centric index for edge label: {} direction: {} for properties: {}", edgeLabel, edgeDirection.name(), propertyNames);
        }
    }


    public void createEdgeIndex(AtlasGraphManagement management, String propertyName, Class propertyClass,
                                 AtlasCardinality cardinality, boolean createCompositeIndex) {
        if (propertyName != null) {
            AtlasPropertyKey propertyKey = management.getPropertyKey(propertyName);

            if (propertyKey == null) {
                propertyKey = management.makePropertyKey(propertyName, propertyClass, cardinality);

                if (isIndexApplicable(propertyClass, cardinality)) {
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Creating backing index for edge property {} of type {} ", propertyName, propertyClass.getName());
                    }

                    management.addMixedIndex(EDGE_INDEX, propertyKey, false);

                    LOG.info("Created backing index for edge property {} of type {} ", propertyName, propertyClass.getName());
                }
            }

            if (propertyKey != null) {
                if (createCompositeIndex) {
                    createEdgeCompositeIndex(management, propertyClass, propertyKey);
                }
            } else {
                LOG.warn("Index not created for {}: propertyKey is null", propertyName);
            }
        }
    }

    private AtlasPropertyKey createFullTextIndex(AtlasGraphManagement management, String propertyName, Class propertyClass,
                                                 AtlasCardinality cardinality) {
        AtlasPropertyKey propertyKey = management.getPropertyKey(propertyName);

        if (propertyKey == null) {
            propertyKey = management.makePropertyKey(propertyName, propertyClass, cardinality);

            if (isIndexApplicable(propertyClass, cardinality)) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Creating backing index for vertex property {} of type {} ", propertyName, propertyClass.getName());
                }

                management.addMixedIndex(FULLTEXT_INDEX, propertyKey, false);

                LOG.info("Created backing index for vertex property {} of type {} ", propertyName, propertyClass.getName());
            }

            LOG.info("Created index {}", FULLTEXT_INDEX);
        }

        return propertyKey;
    }
    
    private void createVertexCompositeIndex(AtlasGraphManagement management, Class propertyClass, AtlasPropertyKey propertyKey,
                                            boolean enforceUniqueness) {
        String propertyName = propertyKey.getName();

        if (LOG.isDebugEnabled()) {
            LOG.debug("Creating composite index for property {} of type {}; isUnique={} ", propertyName, propertyClass.getName(), enforceUniqueness);
        }

        AtlasGraphIndex existingIndex = management.getGraphIndex(propertyName);

        if (existingIndex == null) {
            management.createVertexCompositeIndex(propertyName, enforceUniqueness, Collections.singletonList(propertyKey));

            LOG.info("Created composite index for property {} of type {}; isUnique={} ", propertyName, propertyClass.getName(), enforceUniqueness);
        }
    }

    private void createEdgeCompositeIndex(AtlasGraphManagement management, Class propertyClass, AtlasPropertyKey propertyKey) {
        String propertyName = propertyKey.getName();

        if (LOG.isDebugEnabled()) {
            LOG.debug("Creating composite index for property {} of type {}", propertyName, propertyClass.getName());
        }

        AtlasGraphIndex existingIndex = management.getGraphIndex(propertyName);

        if (existingIndex == null) {
            management.createEdgeCompositeIndex(propertyName, false, Collections.singletonList(propertyKey));

            LOG.info("Created composite index for property {} of type {}", propertyName, propertyClass.getName());
        }
    }

    private void createVertexCompositeIndexWithTypeName(AtlasGraphManagement management, Class propertyClass, AtlasPropertyKey propertyKey, boolean isUnique) {
        createVertexCompositeIndexWithSystemProperty(management, propertyClass, propertyKey, ENTITY_TYPE_PROPERTY_KEY, SINGLE, isUnique);
    }

    private void createVertexCompositeIndexWithSuperTypeName(AtlasGraphManagement management, Class propertyClass, AtlasPropertyKey propertyKey) {
        createVertexCompositeIndexWithSystemProperty(management, propertyClass, propertyKey, SUPER_TYPES_PROPERTY_KEY, SET, false);
    }

    private void createVertexCompositeIndexWithSystemProperty(AtlasGraphManagement management, Class propertyClass, AtlasPropertyKey propertyKey,
                                                              final String systemPropertyKey, AtlasCardinality cardinality, boolean isUnique) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Creating composite index for property {} of type {} and {}", propertyKey.getName(), propertyClass.getName(),  systemPropertyKey);
        }

        AtlasPropertyKey typePropertyKey = management.getPropertyKey(systemPropertyKey);
        if (typePropertyKey == null) {
            typePropertyKey = management.makePropertyKey(systemPropertyKey, String.class, cardinality);
        }

        final String indexName = propertyKey.getName() + systemPropertyKey;
        AtlasGraphIndex existingIndex = management.getGraphIndex(indexName);

        if (existingIndex == null) {
            List<AtlasPropertyKey> keys = new ArrayList<>(2);
            keys.add(typePropertyKey);
            keys.add(propertyKey);
            management.createVertexCompositeIndex(indexName, isUnique, keys);

            LOG.info("Created composite index for property {} of type {} and {}", propertyKey.getName(), propertyClass.getName(), systemPropertyKey);
        }
    }

    private boolean isIndexApplicable(Class propertyClass, AtlasCardinality cardinality) {
        return !(INDEX_EXCLUSION_CLASSES.contains(propertyClass) || cardinality.isMany());
    }
    
    public void commit(AtlasGraphManagement management) throws IndexException {
        try {
            management.commit();

            recomputeIndexedKeys = true;
        } catch (Exception e) {
            LOG.error("Index commit failed", e);
            throw new IndexException("Index commit failed ", e);
        }
    }

    public void rollback(AtlasGraphManagement management) throws IndexException {
        try {
            management.rollback();

            recomputeIndexedKeys = true;
        } catch (Exception e) {
            LOG.error("Index rollback failed ", e);
            throw new IndexException("Index rollback failed ", e);
        }
    }

    private void attemptRollback(ChangedTypeDefs changedTypeDefs, AtlasGraphManagement management)
            throws AtlasBaseException {
        if (null != management) {
            try {
                rollback(management);
            } catch (IndexException e) {
                LOG.error("Index rollback has failed", e);
                throw new AtlasBaseException(AtlasErrorCode.INDEX_ROLLBACK_FAILED, e,
                        changedTypeDefs.toString());
            }
        }
    }

    private void updateIndexForTypeDef(AtlasGraphManagement management, AtlasBaseTypeDef typeDef) {
        Preconditions.checkNotNull(typeDef, "Cannot index on null typedefs");
        if (LOG.isDebugEnabled()) {
            LOG.debug("Creating indexes for type name={}, definition={}", typeDef.getName(), typeDef.getClass());
        }
        addIndexForType(management, typeDef);
        LOG.info("Index creation for type {} complete", typeDef.getName());
    }

    private void notifyChangeListeners(ChangedTypeDefs changedTypeDefs) {
        for (IndexChangeListener indexChangeListener : indexChangeListeners) {
            try {
                indexChangeListener.onChange(changedTypeDefs);
            } catch (Throwable t) {
                LOG.error("Error encountered in notifying the index change listener {}.", indexChangeListener.getClass().getName(), t);
                //we need to throw exception if any of the listeners throw execption.
                throw new RuntimeException("Error encountered in notifying the index change listener " + indexChangeListener.getClass().getName(), t);
            }
        }
    }

    private void notifyInitializationStart() {
        for (IndexChangeListener indexChangeListener : indexChangeListeners) {
            try {
                indexChangeListener.onInitStart();
            } catch (Throwable t) {
                LOG.error("Error encountered in notifying the index change listener {}.", indexChangeListener.getClass().getName(), t);
                //we need to throw exception if any of the listeners throw execption.
                throw new RuntimeException("Error encountered in notifying the index change listener " + indexChangeListener.getClass().getName(), t);
            }
        }
    }

    private void notifyInitializationCompletion(ChangedTypeDefs changedTypeDefs) {
        for (IndexChangeListener indexChangeListener : indexChangeListeners) {
            try {
                indexChangeListener.onInitCompletion(changedTypeDefs);
            } catch (Throwable t) {
                LOG.error("Error encountered in notifying the index change listener {}.", indexChangeListener.getClass().getName(), t);
                //we need to throw exception if any of the listeners throw execption.
                throw new RuntimeException("Error encountered in notifying the index change listener " + indexChangeListener.getClass().getName(), t);
            }
        }
    }


    private void createEdgeLabels(AtlasGraphManagement management, List<? extends AtlasBaseTypeDef> typeDefs) {
        if (CollectionUtils.isEmpty(typeDefs)) {
            return;
        }

        for (AtlasBaseTypeDef typeDef : typeDefs) {
            if (typeDef instanceof AtlasEntityDef) {
                AtlasEntityDef entityDef = (AtlasEntityDef) typeDef;
                createEdgeLabelsForStruct(management, entityDef);
            } else if (typeDef instanceof AtlasRelationshipDef) {
                createEdgeLabels(management, (AtlasRelationshipDef) typeDef);
            }
        }
    }

    private void createEdgeLabelsForStruct(AtlasGraphManagement management, AtlasEntityDef entityDef) {
        try {
            AtlasType type = typeRegistry.getType(entityDef.getName());
            if (!(type instanceof AtlasEntityType)) {
                return;
            }

            AtlasEntityType entityType = (AtlasEntityType) type;
            for (AtlasAttributeDef attributeDef : entityDef.getAttributeDefs()) {
                AtlasAttribute attribute = entityType.getAttribute(attributeDef.getName());
                if (attribute.getAttributeType().getTypeCategory() == TypeCategory.STRUCT) {
                    String relationshipLabel = attribute.getRelationshipEdgeLabel();
                    createEdgeLabelUsingLabelName(management, relationshipLabel);
                }
            }
        } catch (AtlasBaseException e) {
            LOG.error("Error fetching type: {}", entityDef.getName(), e);
        }
    }

    private void createEdgeLabels(AtlasGraphManagement management, AtlasRelationshipDef relationshipDef) {
        String relationshipTypeName = relationshipDef.getName();
        AtlasRelationshipType relationshipType = typeRegistry.getRelationshipTypeByName(relationshipTypeName);
        String relationshipLabel = relationshipType.getRelationshipLabel();

        createEdgeLabelUsingLabelName(management, relationshipLabel);
    }
}
