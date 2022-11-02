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
import org.apache.atlas.model.typedef.AtlasStructDef.AtlasAttributeDef;
import org.apache.atlas.pc.WorkItemManager;
import org.apache.atlas.repository.graphdb.AtlasGraph;
import org.apache.atlas.repository.graphdb.AtlasVertex;
import org.apache.atlas.type.AtlasEntityType;
import org.apache.atlas.type.AtlasStructType.AtlasAttribute;
import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.Iterator;
import java.util.Set;

import static org.apache.atlas.model.patches.AtlasPatch.PatchStatus.APPLIED;
import static org.apache.atlas.repository.Constants.ENTITY_TYPE_PROPERTY_KEY;

public class AddMandatoryAttributesPatch extends AtlasPatchHandler {
    private static final Logger LOG = LoggerFactory.getLogger(AddMandatoryAttributesPatch.class);

    private static final String PATCH_ID          = "JAVA_PATCH_0000_008";
    private static final String PATCH_DESCRIPTION = "Add mandatory attributes for all existing entities for given typeName";

    private final PatchContext            context;
    private final String                  typeName;
    private final List<AtlasAttributeDef> attributesToAdd;

    public AddMandatoryAttributesPatch(PatchContext context, String typedefPatchId, String typeName, List<AtlasAttributeDef> attributesToAdd) {
        super(context.getPatchRegistry(), PATCH_ID + "_" + typedefPatchId, PATCH_DESCRIPTION);

        this.context         = context;
        this.typeName        = typeName;
        this.attributesToAdd = attributesToAdd;
    }

    @Override
    public void apply() throws AtlasBaseException {
        LOG.info("==> MandatoryAttributePatch.apply(): patchId={}", getPatchId());

        ConcurrentPatchProcessor patchProcessor = new AddMandatoryAttributesPatchProcessor(context, typeName, attributesToAdd);

        patchProcessor.apply();

        setStatus(APPLIED);

        LOG.info("<== MandatoryAttributePatch.apply(): patchId={}, status={}", getPatchId(), getStatus());
    }

    public static class AddMandatoryAttributesPatchProcessor extends ConcurrentPatchProcessor {
        private final String                  typeName;
        private final Set<String>             typeAndAllSubTypes;
        private final List<AtlasAttributeDef> attributesToAdd;

        public AddMandatoryAttributesPatchProcessor(PatchContext context, String typeName, List<AtlasAttributeDef> attributesToAdd) {
            super(context);

            AtlasEntityType entityType = getTypeRegistry().getEntityTypeByName(typeName);

            this.typeName        = typeName;
            this.attributesToAdd = attributesToAdd;

            if (entityType != null) {
                this.typeAndAllSubTypes = entityType.getTypeAndAllSubTypes();
            } else {
                LOG.warn("AddMandatoryAttributesPatchProcessor(): failed to find entity-type {}", typeName);

                this.typeAndAllSubTypes = Collections.emptySet();
            }
        }

        @Override
        public void submitVerticesToUpdate(WorkItemManager manager) {
            if (CollectionUtils.isNotEmpty(typeAndAllSubTypes)) {
                LOG.info("Entity types to be updated with mandatory attributes: {}", typeAndAllSubTypes.size());

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
                LOG.debug("==> AddMandatoryAttributesPatchProcessor.processVertexItem(typeName={}, vertexId={})", typeName, vertexId);
            }

            for (AtlasAttributeDef attributeDef : attributesToAdd) {
                AtlasAttribute attribute = entityType.getAttribute(attributeDef.getName());

                if (attribute != null) {
                    Object existingValue = vertex.getProperty(attribute.getVertexPropertyName(), Object.class);

                    if (existingValue == null) {
                        vertex.setProperty(attribute.getVertexPropertyName(), attributeDef.getDefaultValue());
                    }
                }
            }

            if (LOG.isDebugEnabled()) {
                LOG.debug("<== AddMandatoryAttributesPatchProcessor.processVertexItem(typeName={}, vertexId={})", typeName, vertexId);
            }
        }

        @Override
        protected void prepareForExecution() {
            //do nothing
        }
    }
}
