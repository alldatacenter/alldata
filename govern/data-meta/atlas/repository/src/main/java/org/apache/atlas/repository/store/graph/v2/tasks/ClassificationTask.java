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
package org.apache.atlas.repository.store.graph.v2.tasks;

import org.apache.atlas.RequestContext;
import org.apache.atlas.exception.AtlasBaseException;
import org.apache.atlas.exception.EntityNotFoundException;
import org.apache.atlas.model.instance.AtlasRelationship;
import org.apache.atlas.model.tasks.AtlasTask;
import org.apache.atlas.repository.graphdb.AtlasEdge;
import org.apache.atlas.repository.graphdb.AtlasElement;
import org.apache.atlas.repository.graphdb.AtlasGraph;
import org.apache.atlas.repository.store.graph.AtlasRelationshipStore;
import org.apache.atlas.repository.store.graph.v1.DeleteHandlerDelegate;
import org.apache.atlas.repository.store.graph.v2.AtlasGraphUtilsV2;
import org.apache.atlas.repository.store.graph.v2.EntityGraphMapper;
import org.apache.atlas.tasks.AbstractTask;
import org.apache.atlas.type.AtlasType;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

import static org.apache.atlas.model.tasks.AtlasTask.Status.COMPLETE;
import static org.apache.atlas.model.tasks.AtlasTask.Status.FAILED;
import static org.apache.atlas.repository.store.graph.v2.tasks.ClassificationPropagateTaskFactory.CLASSIFICATION_PROPAGATION_RELATIONSHIP_UPDATE;
import static org.apache.atlas.type.Constants.PENDING_TASKS_PROPERTY_KEY;

public abstract class ClassificationTask extends AbstractTask {
    private static final Logger LOG = LoggerFactory.getLogger(ClassificationTask.class);

    public static final String PARAM_ENTITY_GUID              = "entityGuid";
    public static final String PARAM_CLASSIFICATION_VERTEX_ID = "classificationVertexId";
    public static final String PARAM_CLASSIFICATION_NAME      = "classificationName";
    public static final String PARAM_RELATIONSHIP_GUID        = "relationshipGuid";
    public static final String PARAM_RELATIONSHIP_OBJECT      = "relationshipObject";
    public static final String PARAM_RELATIONSHIP_EDGE_ID     = "relationshipEdgeId";

    protected final AtlasGraph             graph;
    protected final EntityGraphMapper      entityGraphMapper;
    protected final DeleteHandlerDelegate  deleteDelegate;
    protected final AtlasRelationshipStore relationshipStore;

    public ClassificationTask(AtlasTask task,
                              AtlasGraph graph,
                              EntityGraphMapper entityGraphMapper,
                              DeleteHandlerDelegate deleteDelegate,
                              AtlasRelationshipStore relationshipStore) {
        super(task);

        this.graph             = graph;
        this.entityGraphMapper = entityGraphMapper;
        this.deleteDelegate    = deleteDelegate;
        this.relationshipStore = relationshipStore;
    }

    @Override
    public AtlasTask.Status perform() throws Exception {
        RequestContext.clear();
        Map<String, Object> params = getTaskDef().getParameters();

        if (MapUtils.isEmpty(params)) {
            LOG.warn("Task: {}: Unable to process task: Parameters is not readable!", getTaskGuid());

            return FAILED;
        }

        String userName = getTaskDef().getCreatedBy();

        if (StringUtils.isEmpty(userName)) {
            LOG.warn("Task: {}: Unable to process task as user name is empty!", getTaskGuid());

            return FAILED;
        }

        RequestContext.get().setUser(userName, null);

        try {
            run(params);

            setStatus(COMPLETE);
        } catch (Exception e) {
            LOG.error("Task: {}: Error performing task!", getTaskGuid(), e);

            setStatus(FAILED);

            throw e;
        } finally {
            graph.commit();
            RequestContext.clear();
        }

        return getStatus();
    }

    public static Map<String, Object> toParameters(String entityGuid, String classificationVertexId, String relationshipGuid, String classificationName) {
        return new HashMap<String, Object>() {{
            put(PARAM_ENTITY_GUID, entityGuid);
            put(PARAM_CLASSIFICATION_VERTEX_ID, classificationVertexId);
            put(PARAM_CLASSIFICATION_NAME, classificationName);
            put(PARAM_RELATIONSHIP_GUID, relationshipGuid);
        }};
    }
    public static Map<String, Object> toParameters(String relationshipEdgeId, AtlasRelationship relationship) {
        return new HashMap<String, Object>() {{
            put(PARAM_RELATIONSHIP_EDGE_ID, relationshipEdgeId);
            put(PARAM_RELATIONSHIP_OBJECT, AtlasType.toJson(relationship));
        }};
    }

    protected void setStatus(AtlasTask.Status status) {
        super.setStatus(status);

        try {
            if (getTaskType() == CLASSIFICATION_PROPAGATION_RELATIONSHIP_UPDATE) {
                entityGraphMapper.removePendingTaskFromEdge((String) getTaskDef().getParameters().get(PARAM_RELATIONSHIP_EDGE_ID), getTaskGuid());
            } else {
                entityGraphMapper.removePendingTaskFromEntity((String) getTaskDef().getParameters().get(PARAM_ENTITY_GUID), getTaskGuid());
            }
        } catch (EntityNotFoundException | AtlasBaseException e) {
            LOG.error("Error updating associated element for: {}", getTaskGuid(), e);
        }
    }

    protected abstract void run(Map<String, Object> parameters) throws AtlasBaseException;
}
