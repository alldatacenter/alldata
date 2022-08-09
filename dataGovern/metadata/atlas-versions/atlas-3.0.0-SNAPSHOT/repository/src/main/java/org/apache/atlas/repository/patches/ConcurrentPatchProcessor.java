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

import org.apache.atlas.ApplicationProperties;
import org.apache.atlas.exception.AtlasBaseException;
import org.apache.atlas.model.instance.AtlasEntity;
import org.apache.atlas.pc.WorkItemBuilder;
import org.apache.atlas.pc.WorkItemConsumer;
import org.apache.atlas.pc.WorkItemManager;
import org.apache.atlas.repository.graph.GraphBackedSearchIndexer;
import org.apache.atlas.repository.graphdb.*;
import org.apache.atlas.repository.store.graph.v2.AtlasGraphUtilsV2;
import org.apache.atlas.repository.store.graph.v2.EntityGraphMapper;
import org.apache.atlas.type.AtlasEntityType;
import org.apache.atlas.type.AtlasTypeRegistry;
import org.apache.commons.configuration.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicLong;

public abstract class ConcurrentPatchProcessor {
    private static final Logger LOG = LoggerFactory.getLogger(ConcurrentPatchProcessor.class);

    private static final String NUM_WORKERS_PROPERTY = "atlas.patch.numWorkers";
    private static final String BATCH_SIZE_PROPERTY  = "atlas.patch.batchSize";
    private static final String ATLAS_SOLR_SHARDS    = "ATLAS_SOLR_SHARDS";
    private static final String WORKER_NAME_PREFIX   = "patchWorkItem";
    public static final int    NUM_WORKERS;
    public static final int    BATCH_SIZE;

    private final EntityGraphMapper        entityGraphMapper;
    private final AtlasGraph               graph;
    private final GraphBackedSearchIndexer indexer;
    private final AtlasTypeRegistry        typeRegistry;

    static {
        int numWorkers = 3;
        int batchSize  = 300;

        try {
            Configuration config = ApplicationProperties.get();

            numWorkers = config.getInt(NUM_WORKERS_PROPERTY, config.getInt(ATLAS_SOLR_SHARDS, 1) * 3);
            batchSize  = config.getInt(BATCH_SIZE_PROPERTY, 300);

            LOG.info("ConcurrentPatchProcessor: {}={}, {}={}", NUM_WORKERS_PROPERTY, numWorkers, BATCH_SIZE_PROPERTY, batchSize);
        } catch (Exception e) {
            LOG.error("Error retrieving configuration.", e);
        }

        NUM_WORKERS = numWorkers;
        BATCH_SIZE  = batchSize;
    }

    public ConcurrentPatchProcessor(PatchContext context) {
        this.graph             = context.getGraph();
        this.indexer           = context.getIndexer();
        this.typeRegistry      = context.getTypeRegistry();
        this.entityGraphMapper = context.getEntityGraphMapper();
    }

    public EntityGraphMapper getEntityGraphMapper() {
        return entityGraphMapper;
    }

    public AtlasGraph getGraph() {
        return graph;
    }

    public GraphBackedSearchIndexer getIndexer() {
        return indexer;
    }

    public AtlasTypeRegistry getTypeRegistry() {
        return typeRegistry;
    }

    public void apply() throws AtlasBaseException {
        prepareForExecution();
        execute();
    }

    protected abstract void prepareForExecution() throws AtlasBaseException;
    protected abstract void submitVerticesToUpdate(WorkItemManager manager);
    protected abstract void processVertexItem(Long vertexId, AtlasVertex vertex, String typeName, AtlasEntityType entityType) throws AtlasBaseException;

    private void execute() {
        WorkItemManager manager = new WorkItemManager(new ConsumerBuilder(graph, typeRegistry, this),
                                                      WORKER_NAME_PREFIX, BATCH_SIZE, NUM_WORKERS, false);

        try {
            submitVerticesToUpdate(manager);

            manager.drain();
        } finally {
            try {
                manager.shutdown();
            } catch (InterruptedException e) {
                LOG.error("ConcurrentPatchProcessor.execute(): interrupted during WorkItemManager shutdown.", e);
            }
        }
    }

    private static class ConsumerBuilder implements WorkItemBuilder<Consumer, Long> {
        private final AtlasTypeRegistry typeRegistry;
        private final AtlasGraph graph;
        private final ConcurrentPatchProcessor patchItemProcessor;

        public ConsumerBuilder(AtlasGraph graph, AtlasTypeRegistry typeRegistry, ConcurrentPatchProcessor patchItemProcessor) {
            this.graph = graph;
            this.typeRegistry = typeRegistry;
            this.patchItemProcessor = patchItemProcessor;
        }

        @Override
        public Consumer build(BlockingQueue<Long> queue) {
            return new Consumer(graph, typeRegistry, queue, patchItemProcessor);
        }
    }

    private static class Consumer extends WorkItemConsumer<Long> {
        private int MAX_COMMIT_RETRY_COUNT = 3;
        private final AtlasGraph graph;
        private final AtlasTypeRegistry typeRegistry;

        private final AtomicLong counter;
        private final ConcurrentPatchProcessor individualItemProcessor;

        public Consumer(AtlasGraph graph, AtlasTypeRegistry typeRegistry, BlockingQueue<Long> queue, ConcurrentPatchProcessor individualItemProcessor) {
            super(queue);

            this.graph        = graph;
            this.typeRegistry = typeRegistry;
            this.counter = new AtomicLong(0);
            this.individualItemProcessor = individualItemProcessor;
        }

        @Override
        protected void doCommit() {
            if (counter.get() % BATCH_SIZE == 0) {
                LOG.info("Processed: {}", counter.get());

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
            for (int retryCount = 1; retryCount <= MAX_COMMIT_RETRY_COUNT; retryCount++) {
                try {
                    graph.commit();

                    break;
                } catch(Exception ex) {
                    LOG.error("Commit exception: ", retryCount, ex);

                    try {
                        Thread.currentThread().sleep(300 * retryCount);
                    } catch (InterruptedException e) {
                        LOG.error("Commit exception: Pause: Interrputed!", e);
                    }
                }
            }
        }

        @Override
        protected void processItem(Long vertexId) {
            counter.incrementAndGet();
            AtlasVertex vertex = graph.getVertex(Long.toString(vertexId));

            if (vertex == null) {
                LOG.warn("processItem(vertexId={}): AtlasVertex not found!", vertexId);

                return;
            }

            if (AtlasGraphUtilsV2.isTypeVertex(vertex)) {
                return;
            }

            String          typeName   = AtlasGraphUtilsV2.getTypeName(vertex);
            AtlasEntityType entityType = typeRegistry.getEntityTypeByName(typeName);
            if (entityType == null) {
                return;
            }

            try {
                individualItemProcessor.processVertexItem(vertexId, vertex, typeName, entityType);
                doCommit();
            } catch (AtlasBaseException e) {
                LOG.error("Error processing: {}", vertexId, e);
            }
        }
    }
}
