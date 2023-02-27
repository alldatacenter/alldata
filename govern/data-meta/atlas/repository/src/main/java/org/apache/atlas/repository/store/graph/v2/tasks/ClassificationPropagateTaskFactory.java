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
package org.apache.atlas.repository.store.graph.v2.tasks;

import org.apache.atlas.model.tasks.AtlasTask;
import org.apache.atlas.repository.graphdb.AtlasGraph;
import org.apache.atlas.repository.store.graph.AtlasRelationshipStore;
import org.apache.atlas.repository.store.graph.v1.DeleteHandlerDelegate;
import org.apache.atlas.repository.store.graph.v2.EntityGraphMapper;
import org.apache.atlas.tasks.TaskFactory;
import org.apache.atlas.tasks.TaskManagement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

@Component
public class ClassificationPropagateTaskFactory implements TaskFactory {
    private static final Logger LOG = LoggerFactory.getLogger(ClassificationPropagateTaskFactory.class);

    public static final String CLASSIFICATION_PROPAGATION_ADD                 = "CLASSIFICATION_PROPAGATION_ADD";
    public static final String CLASSIFICATION_PROPAGATION_DELETE              = "CLASSIFICATION_PROPAGATION_DELETE";
    public static final String CLASSIFICATION_PROPAGATION_RELATIONSHIP_UPDATE = "CLASSIFICATION_PROPAGATION_RELATIONSHIP_UPDATE";

    private static final List<String> supportedTypes = new ArrayList<String>() {{
        add(CLASSIFICATION_PROPAGATION_ADD);
        add(CLASSIFICATION_PROPAGATION_DELETE);
        add(CLASSIFICATION_PROPAGATION_RELATIONSHIP_UPDATE);
    }};

    private final AtlasGraph             graph;
    private final EntityGraphMapper      entityGraphMapper;
    private final DeleteHandlerDelegate  deleteDelegate;
    private final AtlasRelationshipStore relationshipStore;

    @Inject
    public ClassificationPropagateTaskFactory(AtlasGraph graph, EntityGraphMapper entityGraphMapper, DeleteHandlerDelegate deleteDelegate, AtlasRelationshipStore relationshipStore) {
        this.graph             = graph;
        this.entityGraphMapper = entityGraphMapper;
        this.deleteDelegate    = deleteDelegate;
        this.relationshipStore = relationshipStore;
    }

    public org.apache.atlas.tasks.AbstractTask create(AtlasTask task) {
        String taskType = task.getType();
        String taskGuid = task.getGuid();

        switch (taskType) {
            case CLASSIFICATION_PROPAGATION_ADD:
                return new ClassificationPropagationTasks.Add(task, graph, entityGraphMapper, deleteDelegate, relationshipStore);

            case CLASSIFICATION_PROPAGATION_DELETE:
                return new ClassificationPropagationTasks.Delete(task, graph, entityGraphMapper, deleteDelegate, relationshipStore);

            case CLASSIFICATION_PROPAGATION_RELATIONSHIP_UPDATE:
                return new ClassificationPropagationTasks.UpdateRelationship(task, graph, entityGraphMapper, deleteDelegate, relationshipStore);

            default:
                LOG.warn("Type: {} - {} not found!. The task will be ignored.", taskType, taskGuid);
                return null;
        }
    }

    @Override
    public List<String> getSupportedTypes() {
        return this.supportedTypes;
    }
}
