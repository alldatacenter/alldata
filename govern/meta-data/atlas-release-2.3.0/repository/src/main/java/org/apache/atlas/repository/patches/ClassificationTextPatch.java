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
import org.apache.atlas.pc.WorkItemManager;
import org.apache.atlas.repository.Constants;
import org.apache.atlas.repository.graphdb.AtlasEdge;
import org.apache.atlas.repository.graphdb.AtlasEdgeDirection;
import org.apache.atlas.repository.graphdb.AtlasGraph;
import org.apache.atlas.repository.graphdb.AtlasVertex;
import org.apache.atlas.repository.store.graph.v2.AtlasGraphUtilsV2;
import org.apache.atlas.type.AtlasClassificationType;
import org.apache.atlas.type.AtlasEntityType;
import org.apache.atlas.type.AtlasTypeRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import static org.apache.atlas.model.patches.AtlasPatch.PatchStatus.APPLIED;

public class ClassificationTextPatch extends AtlasPatchHandler {
    private static final Logger LOG = LoggerFactory.getLogger(ClassificationTextPatch.class);

    private static final String PATCH_ID          = "JAVA_PATCH_0000_002";
    private static final String PATCH_DESCRIPTION = "Populates Classification Text attribute for entities from classifications applied on them.";

    private final PatchContext context;

    public ClassificationTextPatch(PatchContext context) {
        super(context.getPatchRegistry(), PATCH_ID, PATCH_DESCRIPTION);

        this.context = context;
    }

    @Override
    public void apply() throws AtlasBaseException {
        ConcurrentPatchProcessor patchProcessor = new ClassificationTextPatchProcessor(context);

        patchProcessor.apply();

        setStatus(APPLIED);

        LOG.info("ClassificationTextPatch.apply(): patchId={}, status={}", getPatchId(), getStatus());
    }

    public static class ClassificationTextPatchProcessor extends ConcurrentPatchProcessor {

        public ClassificationTextPatchProcessor(PatchContext context) {
            super(context);
        }

        @Override
        protected void prepareForExecution() {
            //do nothing
        }

        @Override
        public void submitVerticesToUpdate(WorkItemManager manager) {
            AtlasTypeRegistry typeRegistry = getTypeRegistry();
            AtlasGraph        graph        = getGraph();
            Set<Long>         vertexIds    = new HashSet<>();

            for (AtlasClassificationType classificationType : typeRegistry.getAllClassificationTypes()) {
                LOG.info("finding classification of type {}", classificationType.getTypeName());

                Iterable<AtlasVertex> iterable = graph.query().has(Constants.ENTITY_TYPE_PROPERTY_KEY, classificationType.getTypeName()).vertices();
                int                   count    = 0;

                for (Iterator<AtlasVertex> iter = iterable.iterator(); iter.hasNext(); ) {
                    AtlasVertex         classificationVertex = iter.next();
                    Iterable<AtlasEdge> edges                = classificationVertex.getEdges(AtlasEdgeDirection.IN);

                    for (AtlasEdge edge : edges) {
                        AtlasVertex entityVertex = edge.getOutVertex();
                        Long        vertexId     = (Long) entityVertex.getId();

                        if (vertexIds.contains(vertexId)) {
                            continue;
                        }

                        vertexIds.add(vertexId);

                        manager.checkProduce(vertexId);
                    }

                    count++;
                }

                LOG.info("found {} classification of type {}", count, classificationType.getTypeName());
            }

            LOG.info("found {} entities with classifications", vertexIds.size());
        }

        @Override
        protected void processVertexItem(Long vertexId, AtlasVertex vertex, String typeName, AtlasEntityType entityType) throws AtlasBaseException {
            processItem(vertexId, vertex, typeName, entityType);
        }

        private void processItem(Long vertexId, AtlasVertex vertex, String typeName, AtlasEntityType entityType) throws AtlasBaseException {
            if(LOG.isDebugEnabled()) {
                LOG.debug("processItem(typeName={}, vertexId={})", typeName, vertexId);
            }

            if (AtlasGraphUtilsV2.getState(vertex) != AtlasEntity.Status.ACTIVE) {
                return;
            }

            getEntityGraphMapper().updateClassificationTextAndNames(vertex);

            if(LOG.isDebugEnabled()) {
                LOG.debug("processItem(typeName={}, vertexId={}): Done!", typeName, vertexId);
            }
        }
    }
}
