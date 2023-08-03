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

import org.apache.atlas.AtlasConfiguration;
import org.apache.atlas.exception.AtlasBaseException;
import org.apache.atlas.repository.graphdb.AtlasGraph;
import org.apache.atlas.type.AtlasTypeRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.atlas.model.patches.AtlasPatch.PatchStatus.APPLIED;

public class IndexConsistencyPatch extends AtlasPatchHandler {
    private static final Logger LOG = LoggerFactory.getLogger(IndexConsistencyPatch.class);

    private static final String PATCH_ID = "JAVA_PATCH_0000_005";
    private static final String PATCH_DESCRIPTION = "Sets index consistency for vertices and edges.";

    private final PatchContext context;

    public IndexConsistencyPatch(PatchContext context) {
        super(context.getPatchRegistry(), PATCH_ID, PATCH_DESCRIPTION);
        this.context = context;
    }

    @Override
    public void apply() throws AtlasBaseException {
        if (AtlasConfiguration.STORAGE_CONSISTENCY_LOCK_ENABLED.getBoolean() == false) {
            LOG.info("IndexConsistencyPatch: Not enabled: Skipped!");
            return;
        }

        AtlasGraph graph = context.getGraph();

        try {
            LOG.info("IndexConsistencyPatch: Starting...");
            graph.getManagementSystem().updateUniqueIndexesForConsistencyLock();
        } finally {
            LOG.info("IndexConsistencyPatch: Done!");
        }

        setStatus(APPLIED);

        LOG.info("IndexConsistencyPatch.apply(): patchId={}, status={}", getPatchId(), getStatus());
    }
}
