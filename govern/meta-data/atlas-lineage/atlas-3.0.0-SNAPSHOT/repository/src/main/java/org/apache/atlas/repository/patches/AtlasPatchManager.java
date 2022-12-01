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

package org.apache.atlas.repository.patches;

import org.apache.atlas.model.patches.AtlasPatch.AtlasPatches;
import org.apache.atlas.model.patches.AtlasPatch.PatchStatus;
import org.apache.atlas.repository.graph.GraphBackedSearchIndexer;
import org.apache.atlas.repository.graphdb.AtlasGraph;
import org.apache.atlas.repository.store.graph.v2.EntityGraphMapper;
import org.apache.atlas.type.AtlasTypeRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

import static org.apache.atlas.model.patches.AtlasPatch.PatchStatus.APPLIED;
import static org.apache.atlas.model.patches.AtlasPatch.PatchStatus.SKIPPED;

@Component
public class AtlasPatchManager {
    private static final Logger LOG = LoggerFactory.getLogger(AtlasPatchManager.class);

    private final List<AtlasPatchHandler> handlers = new ArrayList<>();
    private final AtlasGraph atlasGraph;
    private final AtlasTypeRegistry typeRegistry;
    private final GraphBackedSearchIndexer indexer;
    private final EntityGraphMapper entityGraphMapper;
    private PatchContext            context;

    @Inject
    public AtlasPatchManager(AtlasGraph atlasGraph, AtlasTypeRegistry typeRegistry, GraphBackedSearchIndexer indexer, EntityGraphMapper entityGraphMapper) {
        this.atlasGraph = atlasGraph;
        this.typeRegistry = typeRegistry;
        this.indexer = indexer;
        this.entityGraphMapper = entityGraphMapper;
    }

    public AtlasPatches getAllPatches() {
        return context.getPatchRegistry().getAllPatches();
    }

    public void applyAll() {
        LOG.info("==> AtlasPatchManager.applyAll()");
        init();

        try {
            for (AtlasPatchHandler handler : handlers) {
                PatchStatus patchStatus = handler.getStatusFromRegistry();

                if (patchStatus == APPLIED || patchStatus == SKIPPED) {
                    LOG.info("Ignoring java handler: {}; status: {}", handler.getPatchId(), patchStatus);
                } else {
                    LOG.info("Applying java handler: {}; status: {}", handler.getPatchId(), patchStatus);

                    handler.apply();
                }
            }
        } catch (Exception ex) {
            LOG.error("Error applying patches.", ex);
        }

        LOG.info("<== AtlasPatchManager.applyAll()");
    }

    private void init() {
        LOG.info("==> AtlasPatchManager.init()");

        this.context = new PatchContext(atlasGraph, typeRegistry, indexer, entityGraphMapper);

        // register all java patches here
        handlers.add(new UniqueAttributePatch(context));
        handlers.add(new ClassificationTextPatch(context));
        handlers.add(new FreeTextRequestHandlerPatch(context));
        handlers.add(new SuggestionsRequestHandlerPatch(context));
        handlers.add(new IndexConsistencyPatch(context));
        handlers.add(new ReIndexPatch(context));
        handlers.add(new ProcessNamePatch(context));
        handlers.add(new UpdateCompositeIndexStatusPatch(context));

        LOG.info("<== AtlasPatchManager.init()");
    }

    public void addPatchHandler(AtlasPatchHandler patchHandler) {
        handlers.add(patchHandler);
    }

    public PatchContext getContext() {
        return this.context;
    }
}
