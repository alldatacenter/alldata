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
package org.apache.atlas.repository.patches;

import org.apache.atlas.exception.AtlasBaseException;
import org.apache.atlas.model.instance.AtlasEntity;
import org.apache.atlas.model.typedef.AtlasStructDef.AtlasAttributeDef;
import org.apache.atlas.pc.WorkItemManager;
import org.apache.atlas.repository.Constants;
import org.apache.atlas.repository.IndexException;
import org.apache.atlas.repository.graph.GraphBackedSearchIndexer.UniqueKind;
import org.apache.atlas.repository.graphdb.AtlasCardinality;
import org.apache.atlas.repository.graphdb.AtlasGraph;
import org.apache.atlas.repository.graphdb.AtlasGraphManagement;
import org.apache.atlas.repository.graphdb.AtlasSchemaViolationException;
import org.apache.atlas.repository.graphdb.AtlasVertex;
import org.apache.atlas.repository.store.graph.v2.AtlasGraphUtilsV2;
import org.apache.atlas.repository.store.graph.v2.EntityGraphRetriever;
import org.apache.atlas.type.AtlasEntityType;
import org.apache.atlas.type.AtlasStructType.AtlasAttribute;
import org.apache.atlas.type.AtlasTypeRegistry;
import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Iterator;

import static org.apache.atlas.model.patches.AtlasPatch.PatchStatus.APPLIED;
import static org.apache.atlas.repository.store.graph.v2.AtlasGraphUtilsV2.getIdFromVertex;

public class UniqueAttributePatch extends AtlasPatchHandler {
    private static final Logger LOG = LoggerFactory.getLogger(UniqueAttributePatch.class);

    private static final String PATCH_ID          = "JAVA_PATCH_0000_001";
    private static final String PATCH_DESCRIPTION = "Add __u_ property for each unique attribute of active entities";

    private final PatchContext context;

    public UniqueAttributePatch(PatchContext context) {
        super(context.getPatchRegistry(), PATCH_ID, PATCH_DESCRIPTION);

        this.context = context;
    }

    @Override
    public void apply() throws AtlasBaseException {
        ConcurrentPatchProcessor patchProcessor = new UniqueAttributePatchProcessor(context);

        patchProcessor.apply();

        setStatus(APPLIED);

        LOG.info("UniqueAttributePatch.apply(): patchId={}, status={}", getPatchId(), getStatus());
    }

    public static class UniqueAttributePatchProcessor extends ConcurrentPatchProcessor {
        public UniqueAttributePatchProcessor(PatchContext context) {
            super(context);
        }

        @Override
        protected void prepareForExecution() {
            //create the new attribute for all unique attributes.
            createIndexForUniqueAttributes();
        }

        @Override
        public void submitVerticesToUpdate(WorkItemManager manager) {
            AtlasTypeRegistry typeRegistry = getTypeRegistry();
            AtlasGraph        graph        = getGraph();

            for (AtlasEntityType entityType : typeRegistry.getAllEntityTypes()) {
                LOG.info("finding entities of type {}", entityType.getTypeName());

                Iterable<Object> iterable = graph.query().has(Constants.ENTITY_TYPE_PROPERTY_KEY, entityType.getTypeName()).vertexIds();
                int              count    = 0;

                for (Iterator<Object> iter = iterable.iterator(); iter.hasNext(); ) {
                    Object vertexId = iter.next();

                    manager.checkProduce((Long) vertexId);

                    count++;
                }

                LOG.info("found {} entities of type {}", count, entityType.getTypeName());
            }
        }

        @Override
        protected void processVertexItem(Long vertexId, AtlasVertex vertex, String typeName, AtlasEntityType entityType) {
            //process the vertex
            processItem(vertexId, vertex, typeName, entityType);
        }

        private void createIndexForUniqueAttributes() {
            for (AtlasEntityType entityType : getTypeRegistry().getAllEntityTypes()) {

                String typeName = entityType.getTypeName();
                Collection<AtlasAttribute> uniqAttributes = entityType.getUniqAttributes().values();

                if (CollectionUtils.isEmpty(uniqAttributes)) {
                    LOG.info("UniqueAttributePatchProcessor.apply(): no unique attribute for entity-type {}", typeName);

                    continue;
                }

                createIndexForUniqueAttributes(typeName, uniqAttributes);
            }
        }

        private void createIndexForUniqueAttributes(String typeName, Collection<AtlasAttribute> attributes) {
            try {
                AtlasGraphManagement management = getGraph().getManagementSystem();

                for (AtlasAttribute attribute : attributes) {
                    String uniquePropertyName = attribute.getVertexUniquePropertyName();

                    if (management.getPropertyKey(uniquePropertyName) != null) {
                        continue;
                    }

                    AtlasAttributeDef attributeDef   = attribute.getAttributeDef();
                    boolean           isIndexable    = attributeDef.getIsIndexable();
                    String            attribTypeName = attributeDef.getTypeName();
                    Class             propertyClass  = getIndexer().getPrimitiveClass(attribTypeName);
                    AtlasCardinality  cardinality    = getIndexer().toAtlasCardinality(attributeDef.getCardinality());

                    getIndexer().createVertexIndex(management,
                                                   uniquePropertyName,
                                                   UniqueKind.PER_TYPE_UNIQUE,
                                                   propertyClass,
                                                   cardinality,
                                                   isIndexable,
                                                   true,
                                                   AtlasAttributeDef.IndexType.STRING.equals(attribute.getIndexType()));
                }

                getIndexer().commit(management);
                getGraph().commit();

                LOG.info("Unique attributes: type: {}: Registered!", typeName);
            } catch (IndexException e) {
                LOG.error("Error creating index: type: {}", typeName, e);
            }
        }

        protected void processItem(Long vertexId, AtlasVertex vertex, String typeName, AtlasEntityType entityType) {
            LOG.debug("processItem(typeName={}, vertexId={})", typeName, vertexId);

            processIndexStringAttribute(vertexId, vertex, typeName, entityType);
            if (AtlasGraphUtilsV2.getState(vertex) == AtlasEntity.Status.ACTIVE) {
                processUniqueAttribute(vertexId, vertex, typeName, entityType);
            }

            LOG.debug("processItem(typeName={}, vertexId={}): Done!", typeName, vertexId);
        }

        private void processIndexStringAttribute(Long vertexId, AtlasVertex vertex, String typeName, AtlasEntityType entityType) {
            for (AtlasAttribute attribute : entityType.getAllAttributes().values()) {
                if (attribute.getAttributeDef().getIndexType() != null &&
                        attribute.getAttributeDef().getIndexType() == AtlasAttributeDef.IndexType.STRING) {

                    String vertexPropertyName = attribute.getVertexPropertyName();
                    if (vertex.getProperty(vertexPropertyName, String.class) != null) {
                        continue;
                    }

                    Object attrVal = AtlasGraphUtilsV2.getEncodedProperty(vertex, attribute.getQualifiedName(), String.class);
                    if (attrVal != null) {
                        AtlasGraphUtilsV2.setEncodedProperty(vertex, vertexPropertyName, attrVal);
                    }
                }
            }

            LOG.debug("processIndexStringAttribute(typeName={}, vertexId={}): Done!", typeName, vertexId);
        }

        private void processUniqueAttribute(Long vertexId, AtlasVertex vertex, String typeName, AtlasEntityType entityType) {
            for (AtlasAttribute attribute : entityType.getUniqAttributes().values()) {
                String uniquePropertyKey = attribute.getVertexUniquePropertyName();
                Collection<? extends String> propertyKeys = vertex.getPropertyKeys();
                Object uniqAttrValue = null;

                if (propertyKeys == null || !propertyKeys.contains(uniquePropertyKey)) {
                    try {
                        String propertyKey = attribute.getVertexPropertyName();

                        uniqAttrValue = EntityGraphRetriever.mapVertexToPrimitive(vertex, propertyKey, attribute.getAttributeDef());

                        AtlasGraphUtilsV2.setEncodedProperty(vertex, uniquePropertyKey, uniqAttrValue);
                    } catch (AtlasSchemaViolationException ex) {
                        LOG.error("Duplicates detected: {}:{}:{}", typeName, uniqAttrValue, getIdFromVertex(vertex));
                        vertex.removeProperty(uniquePropertyKey);
                    }
                }
            }

            LOG.debug("processUniqueAttribute(typeName={}, vertexId={}): Done!", typeName, vertexId);
        }
    }
}
