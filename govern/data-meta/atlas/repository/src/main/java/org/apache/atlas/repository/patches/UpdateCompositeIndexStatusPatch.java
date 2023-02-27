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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.atlas.model.patches.AtlasPatch.PatchStatus.UNKNOWN;

public class UpdateCompositeIndexStatusPatch extends AtlasPatchHandler {
    private static final Logger LOG = LoggerFactory.getLogger(UpdateCompositeIndexStatusPatch.class);

    private static final String PATCH_ID          = "JAVA_PATCH_0000_010";
    private static final String PATCH_DESCRIPTION = "updates schema status of composite indexes which are in REGISTERED to ENABLED.";

    private final PatchContext context;

    public UpdateCompositeIndexStatusPatch(PatchContext context) {
        super(context.getPatchRegistry(), PATCH_ID, PATCH_DESCRIPTION);
        this.context = context;
    }

    @Override
    public void apply() throws AtlasBaseException {
        if (!AtlasConfiguration.UPDATE_COMPOSITE_INDEX_STATUS.getBoolean()) {
            LOG.info("UpdateCompositeIndexStatusPatch: Skipped, since not enabled!");
            return;
        }

        AtlasGraph graph = context.getGraph();

        try {
            LOG.info("UpdateCompositeIndexStatusPatch: Starting...");
            graph.getManagementSystem().updateSchemaStatus();
        } finally {
            LOG.info("UpdateCompositeIndexStatusPatch: Done!");
        }

        setStatus(UNKNOWN);

        LOG.info("UpdateCompositeIndexStatusPatch.apply(): patchId={}, status={}", getPatchId(), getStatus());
    }
}
