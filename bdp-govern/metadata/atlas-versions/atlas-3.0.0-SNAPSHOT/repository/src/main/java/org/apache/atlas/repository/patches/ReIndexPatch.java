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

import org.apache.atlas.AtlasConfiguration;
import org.apache.atlas.exception.AtlasBaseException;
import org.apache.atlas.pc.WorkItemBuilder;
import org.apache.atlas.pc.WorkItemConsumer;
import org.apache.atlas.pc.WorkItemManager;
import org.apache.atlas.repository.Constants;
import org.apache.atlas.repository.graphdb.AtlasEdge;
import org.apache.atlas.repository.graphdb.AtlasElement;
import org.apache.atlas.repository.graphdb.AtlasGraph;
import org.apache.atlas.repository.graphdb.AtlasVertex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiConsumer;

import static org.apache.atlas.model.patches.AtlasPatch.PatchStatus.UNKNOWN;

public class ReIndexPatch extends AtlasPatchHandler {
    private static final Logger LOG = LoggerFactory.getLogger(ReIndexPatch.class);

    private static final String PATCH_ID = "JAVA_PATCH_0000_006";
    private static final String PATCH_DESCRIPTION = "Performs reindex on all the indexes.";

    private final PatchContext context;

    public ReIndexPatch(PatchContext context) {
        super(context.getPatchRegistry(), PATCH_ID, PATCH_DESCRIPTION);
        this.context = context;
    }

    @Override
    public void apply() throws AtlasBaseException {
        if (AtlasConfiguration.REBUILD_INDEX.getBoolean() == false) {
            LOG.info("ReIndexPatch: Skipped, since not enabled!");
            return;
        }

        try {
            LOG.info("ReIndexPatch: Starting...");
            ReindexPatchProcessor reindexPatchProcessor = new ReindexPatchProcessor(context);

            reindexPatchProcessor.repairVertices();
            reindexPatchProcessor.repairEdges();
        } catch (Exception exception) {
            LOG.error("Error while reindexing.", exception);
        } finally {
            LOG.info("ReIndexPatch: Done!");
        }

        setStatus(UNKNOWN);

        LOG.info("ReIndexPatch.apply(): patchId={}, status={}", getPatchId(), getStatus());
    }

    public static class ReindexPatchProcessor {
        private static String[] vertexIndexNames = new String[]{ Constants.VERTEX_INDEX, Constants.FULLTEXT_INDEX };
        private static String[] edgeIndexNames = new String[]{ Constants.EDGE_INDEX };
        private static String WORKER_PREFIX = "reindex";

        private PatchContext context;

        public ReindexPatchProcessor(PatchContext context) {
            this.context = context;
        }

        public void repairVertices() {
            repairElements(ReindexPatchProcessor::vertices, vertexIndexNames);
        }

        public void repairEdges() {
            repairElements(ReindexPatchProcessor::edges, edgeIndexNames);
        }

        private void repairElements(BiConsumer<WorkItemManager, AtlasGraph> action, String[] indexNames) {
            WorkItemManager manager = new WorkItemManager(new ReindexConsumerBuilder(context.getGraph(), indexNames),
                    WORKER_PREFIX, ConcurrentPatchProcessor.BATCH_SIZE, ConcurrentPatchProcessor.NUM_WORKERS, false);

            try {
                LOG.info("repairElements.execute(): {}: Starting...", indexNames);
                action.accept(manager, context.getGraph());
                manager.drain();
            } finally {
                try {
                    manager.shutdown();
                } catch (InterruptedException e) {
                    LOG.error("repairEdges.execute(): interrupted during WorkItemManager shutdown.", e);
                }

                LOG.info("repairElements.execute(): {}: Done!", indexNames);
            }
        }

        private static void edges(WorkItemManager manager, AtlasGraph graph) {
            Iterable<AtlasEdge> iterable = graph.getEdges();
            for (Iterator<AtlasEdge> iter = iterable.iterator(); iter.hasNext(); ) {
                manager.checkProduce(iter.next());
            }
        }

        private static void vertices(WorkItemManager manager, AtlasGraph graph) {
            Iterable<AtlasVertex> iterable = graph.getVertices();
            for (Iterator<AtlasVertex> iter = iterable.iterator(); iter.hasNext(); ) {
                AtlasVertex vertex = iter.next();
                manager.checkProduce(vertex);
            }
        }
    }

    private static class ReindexConsumerBuilder implements WorkItemBuilder<ReindexConsumer, AtlasElement> {
        private AtlasGraph graph;
        private String[] indexNames;

        public ReindexConsumerBuilder(AtlasGraph graph, String[] indexNames) {
            this.graph = graph;
            this.indexNames = indexNames;
        }

        @Override
        public ReindexConsumer build(BlockingQueue queue) {
            return new ReindexConsumer(queue, this.graph, this.indexNames);
        }
    }

    private static class ReindexConsumer extends WorkItemConsumer<AtlasElement> {
        private final List<AtlasElement> list = new ArrayList();
        private final String[] indexNames;
        private final AtlasGraph graph;
        private final AtomicLong counter;

        public ReindexConsumer(BlockingQueue queue, AtlasGraph graph, String[] indexNames) {
            super(queue);
            this.graph = graph;
            this.indexNames = indexNames;
            this.counter = new AtomicLong(0);
        }

        @Override
        protected void doCommit() {
            if (list.size() >= ConcurrentPatchProcessor.BATCH_SIZE) {
                attemptCommit();
            }
        }

        @Override
        protected void commitDirty() {
            attemptCommit();

            LOG.info("Total: Commit: {}", counter.get());
            super.commitDirty();
        }

        private void attemptCommit() {
            for (String indexName : indexNames) {
                try {
                    this.graph.getManagementSystem().reindex(indexName, list);
                }
                catch (IllegalStateException e) {
                    LOG.error("IllegalStateException: Exception", e);
                    return;
                }
                catch (Exception exception) {
                    LOG.error("Exception: {}", indexName, exception);
                }
            }

            list.clear();
            LOG.info("Processed: {}", counter.get());
        }

        @Override
        protected void processItem(AtlasElement item) {
            counter.incrementAndGet();
            list.add(item);
            commit();
        }
    }
}
