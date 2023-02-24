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

package org.apache.atlas.repository.store.graph.v1;

import org.apache.atlas.RequestContext;
import org.apache.atlas.repository.graphdb.AtlasGraph;
import org.apache.atlas.DeleteType;
import org.apache.atlas.tasks.TaskManagement;
import org.apache.atlas.type.AtlasTypeRegistry;
import org.apache.atlas.util.AtlasRepositoryConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
@Component
public class DeleteHandlerDelegate {
    private static final Logger LOG = LoggerFactory.getLogger(DeleteHandlerDelegate.class);

    private final SoftDeleteHandlerV1 softDeleteHandler;
    private final HardDeleteHandlerV1 hardDeleteHandler;
    private final DeleteHandlerV1     defaultHandler;
    private final AtlasGraph graph;
    private final TaskManagement      taskManagement;

    @Inject
    public DeleteHandlerDelegate(AtlasGraph graph, AtlasTypeRegistry typeRegistry, TaskManagement taskManagement) {
        this.graph = graph;
        this.taskManagement    = taskManagement;
        this.softDeleteHandler = new SoftDeleteHandlerV1(graph, typeRegistry, taskManagement);
        this.hardDeleteHandler = new HardDeleteHandlerV1(graph, typeRegistry, taskManagement);
        this.defaultHandler    = getDefaultConfiguredHandler(typeRegistry);
    }

    public DeleteHandlerV1 getHandler() {
        return getHandler(RequestContext.get().getDeleteType());
    }

    public DeleteHandlerV1 getHandler(DeleteType deleteType) {
        if (deleteType == null) {
            deleteType = DeleteType.DEFAULT;
        }

        switch (deleteType) {
            case SOFT:
                return softDeleteHandler;

            case HARD:
                return hardDeleteHandler;

            default:
                return defaultHandler;
        }
    }

    private DeleteHandlerV1 getDefaultConfiguredHandler(AtlasTypeRegistry typeRegistry) {
        DeleteHandlerV1 ret = null;

        try {
            Class handlerFromProperties = AtlasRepositoryConfiguration.getDeleteHandlerV1Impl();

            LOG.info("Default delete handler set to: {}", handlerFromProperties.getName());

            ret = (DeleteHandlerV1) handlerFromProperties.getConstructor(AtlasGraph.class, AtlasTypeRegistry.class, TaskManagement.class)
                                    .newInstance(this.graph, typeRegistry, taskManagement);
        } catch (Exception ex) {
            LOG.error("Error instantiating default delete handler. Defaulting to: {}", softDeleteHandler.getClass().getName(), ex);

            ret = softDeleteHandler;
        }

        return ret;
    }
}