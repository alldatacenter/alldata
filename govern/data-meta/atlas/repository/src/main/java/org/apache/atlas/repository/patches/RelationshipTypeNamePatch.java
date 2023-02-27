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
import org.apache.atlas.pc.WorkItemManager;
import org.apache.atlas.repository.Constants;
import org.apache.atlas.repository.graphdb.AtlasEdge;
import org.apache.atlas.repository.graphdb.AtlasGraph;
import org.apache.atlas.repository.graphdb.AtlasGraphQuery;
import org.apache.atlas.type.AtlasRelationshipType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;

import static org.apache.atlas.model.patches.AtlasPatch.PatchStatus.APPLIED;
import static org.apache.atlas.repository.Constants.ENTITY_TYPE_PROPERTY_KEY;
import static org.apache.atlas.repository.Constants.RELATIONSHIP_TYPE_PROPERTY_KEY;
import static org.apache.atlas.repository.Constants.TYPE_NAME_PROPERTY_KEY;


public class RelationshipTypeNamePatch extends AtlasPatchHandler {

    private static final Logger LOG = LoggerFactory.getLogger(RelationshipTypeNamePatch.class);

    private static final String PATCH_ID          = "JAVA_PATCH_0000_0011";
    private static final String PATCH_DESCRIPTION = "Populates Relationship typeName as like of Entity TypeName for all Edges.";

    private final PatchContext context;

    public RelationshipTypeNamePatch(PatchContext context) {
        super(context.getPatchRegistry(), PATCH_ID, PATCH_DESCRIPTION);

        this.context = context;
    }

    @Override
    public void apply() throws AtlasBaseException {
        EdgePatchProcessor patchProcessor = new RelationshipTypeNamePatchProcessor(context);

        patchProcessor.apply();

        setStatus(APPLIED);

        LOG.info("EdgePatchProcessor.apply(): patchId={}, status={}", getPatchId(), getStatus());
    }

    public static class RelationshipTypeNamePatchProcessor extends EdgePatchProcessor {

        public RelationshipTypeNamePatchProcessor(PatchContext context) {
            super(context);
        }

        @Override
        protected void prepareForExecution() {
            // relationship typeName mixed and composite index is already created in GraphBackedSearchIndexer @Order(1)
        }

        @Override
        protected void submitEdgesToUpdate(WorkItemManager manager) {

            AtlasGraph graph = getGraph();

            Iterable<AtlasEdge> iterable = graph.getEdges();
            int count = 0;

            for (Iterator<AtlasEdge> iter = iterable.iterator(); iter.hasNext(); ) {
                AtlasEdge edge = iter.next();

                if (edge.getProperty(ENTITY_TYPE_PROPERTY_KEY, String.class) != null) {
                    String edgeId = edge.getId().toString();

                    manager.checkProduce(edgeId);
                    count++;
                }
            }

            LOG.info("found {} edges with typeName != null", count);
        }

        @Override
        protected void processEdgesItem(String edgeId, AtlasEdge edge, String typeName, AtlasRelationshipType type) {

            edge.setProperty(RELATIONSHIP_TYPE_PROPERTY_KEY, typeName);

            if (LOG.isDebugEnabled()) {
                LOG.debug("processItem(typeName={}, edgeId={}): Done!", typeName, edgeId);
            }
        }
    }
}
