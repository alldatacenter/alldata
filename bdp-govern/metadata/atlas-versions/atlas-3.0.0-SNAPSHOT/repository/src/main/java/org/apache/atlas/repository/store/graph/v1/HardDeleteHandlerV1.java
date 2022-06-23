/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.atlas.repository.store.graph.v1;

import org.apache.atlas.annotation.ConditionalOnAtlasProperty;
import org.apache.atlas.exception.AtlasBaseException;
import org.apache.atlas.repository.graph.GraphHelper;
import org.apache.atlas.repository.graphdb.AtlasEdge;
import org.apache.atlas.repository.graphdb.AtlasGraph;
import org.apache.atlas.repository.graphdb.AtlasVertex;
import org.apache.atlas.tasks.TaskManagement;
import org.apache.atlas.type.AtlasTypeRegistry;
import org.springframework.stereotype.Component;

import javax.inject.Inject;

@Component
@ConditionalOnAtlasProperty(property = "atlas.DeleteHandlerV1.impl")
public class HardDeleteHandlerV1 extends DeleteHandlerV1 {

    @Inject
    public HardDeleteHandlerV1(AtlasGraph graph, AtlasTypeRegistry typeRegistry, TaskManagement taskManagement) {
        super(graph, typeRegistry, true, false, taskManagement);
    }

    @Override
    protected void _deleteVertex(AtlasVertex instanceVertex, boolean force) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("==> HardDeleteHandlerV1._deleteVertex({}, {})", GraphHelper.string(instanceVertex), force);
        }

        graphHelper.removeVertex(instanceVertex);
    }

    @Override
    protected void deleteEdge(AtlasEdge edge, boolean force) throws AtlasBaseException {
        if (LOG.isDebugEnabled()) {
            LOG.debug("==> HardDeleteHandlerV1.deleteEdge({}, {})", GraphHelper.string(edge), force);
        }

        removeTagPropagation(edge);

        graphHelper.removeEdge(edge);
    }
}
