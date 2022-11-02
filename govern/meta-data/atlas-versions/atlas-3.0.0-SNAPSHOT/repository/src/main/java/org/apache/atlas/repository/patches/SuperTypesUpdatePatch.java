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
import org.apache.atlas.repository.graphdb.AtlasGraph;
import org.apache.atlas.repository.graphdb.AtlasVertex;
import org.apache.atlas.repository.store.graph.v2.AtlasGraphUtilsV2;
import org.apache.atlas.type.AtlasEntityType;
import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import static org.apache.atlas.model.patches.AtlasPatch.PatchStatus.APPLIED;
import static org.apache.atlas.repository.Constants.ENTITY_TYPE_PROPERTY_KEY;
import static org.apache.atlas.repository.Constants.SUPER_TYPES_PROPERTY_KEY;

public class SuperTypesUpdatePatch extends AtlasPatchHandler {
    private static final Logger LOG               = LoggerFactory.getLogger(SuperTypesUpdatePatch.class);
    private static final String PATCH_ID          = "JAVA_PATCH_0000_007";
    private static final String PATCH_DESCRIPTION = "Update supertypes for all existing entities for given typeName";

    private final PatchContext context;
    private final String       typeName;

    public SuperTypesUpdatePatch(PatchContext context, String typeDefPatchId, String typeName) {
        super(context.getPatchRegistry(), PATCH_ID + "_" + typeDefPatchId, PATCH_DESCRIPTION);

        this.context  = context;
        this.typeName = typeName;
    }

    @Override
    public void apply() throws AtlasBaseException {
        LOG.info("==> SuperTypesUpdatePatch.apply(): patchId={}", getPatchId());

        ConcurrentPatchProcessor patchProcessor = new SuperTypesUpdatePatchProcessor(context, typeName);

        patchProcessor.apply();

        setStatus(APPLIED);

        LOG.info("<== SuperTypesUpdatePatch.apply(): patchId={}, status={}", getPatchId(), getStatus());
    }

    public static class SuperTypesUpdatePatchProcessor extends ConcurrentPatchProcessor {
        private final String      typeName;
        private final Set<String> typeAndAllSubTypes;

        public SuperTypesUpdatePatchProcessor(PatchContext context, String typeName) {
            super(context);

            AtlasEntityType entityType = getTypeRegistry().getEntityTypeByName(typeName);

            this.typeName           = typeName;
            this.typeAndAllSubTypes = entityType != null ? entityType.getTypeAndAllSubTypes() : Collections.emptySet();
        }

        @Override
        public void submitVerticesToUpdate(WorkItemManager manager) {
            if (CollectionUtils.isNotEmpty(typeAndAllSubTypes)) {
                LOG.info("Entity types to be updated with supertypes :{}", typeAndAllSubTypes.size());

                for (String typeName : typeAndAllSubTypes) {
                    LOG.info("finding entities of type {}", typeName);

                    AtlasGraph       graph     = getGraph();
                    Iterable<Object> vertexIds = graph.query().has(ENTITY_TYPE_PROPERTY_KEY, typeName).vertexIds();
                    int              count     = 0;

                    for (Iterator<Object> iterator = vertexIds.iterator(); iterator.hasNext(); ) {
                        Object vertexId = iterator.next();

                        manager.checkProduce(vertexId);

                        count++;
                    }

                    LOG.info("found {} entities of type {}", count, typeName);
                }
            }
        }

        @Override
        protected void processVertexItem(Long vertexId, AtlasVertex vertex, String typeName, AtlasEntityType entityType) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("processVertexItem(typeName={}, vertexId={})", typeName, vertexId);
            }

            Set<String> allSuperTypes = entityType.getAllSuperTypes();

            if (allSuperTypes != null) {
                // remove and update all entity super types
                vertex.removeProperty(SUPER_TYPES_PROPERTY_KEY);

                for (String superType : allSuperTypes) {
                    AtlasGraphUtilsV2.addEncodedProperty(vertex, SUPER_TYPES_PROPERTY_KEY, superType);
                }

                if (LOG.isDebugEnabled()) {
                    LOG.debug("Updated superTypes for entity of typeName={}, vertexId={}): Done!", typeName, vertex.getId());
                }
            }
        }

        @Override
        protected void prepareForExecution() {
            //do nothing
        }
    }
}
