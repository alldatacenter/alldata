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

package org.apache.atlas.repository.graphdb.janus.migration;

import org.apache.tinkerpop.shaded.jackson.databind.JsonNode;
import org.apache.atlas.pc.WorkItemBuilder;
import org.apache.atlas.pc.WorkItemConsumer;
import org.apache.atlas.repository.graphdb.janus.migration.JsonNodeParsers.ParseElement;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicLong;

public class JsonNodeProcessManager {
    private static class Consumer extends WorkItemConsumer<JsonNode> {
        private static final Logger LOG = LoggerFactory.getLogger(Consumer.class);

        private static final int WAIT_DURATION_AFTER_COMMIT_EXCEPTION = 1000;

        private   final Graph              graph;
        protected final Graph              bulkLoadGraph;
        protected final ParseElement       parseElement;
        private   final long               batchSize;
        private         AtomicLong         counter;
        private   final MappedElementCache cache;
        private   static ThreadLocal<List<JsonNode>> nodes = ThreadLocal.withInitial(() -> new ArrayList<>());

        public Consumer(BlockingQueue<JsonNode> workQueue, Graph graph, Graph bulkLoadGraph, ParseElement parseElement, long batchSize) {
            super(workQueue);

            this.graph         = graph;
            this.bulkLoadGraph = bulkLoadGraph;
            this.parseElement  = parseElement;
            this.batchSize     = batchSize;
            this.counter       = new AtomicLong(0);
            this.cache         = new MappedElementCache();
        }

        @Override
        public void processItem(JsonNode node) {
            try {
                Map<String, Object> result = parseElement.parse(bulkLoadGraph, cache, node);

                if (result == null) {
                    addNode(node);
                    commitConditionally(counter.getAndIncrement());
                } else {
                    commitBulk();
                    cache.clearAll();
                    updateSchema(result, node);
                }
            } catch (Exception ex) {
                bulkLoadGraph.tx().rollback();
                error("Failed! Retrying...", ex);
                retryBatchCommit();
            }
        }

        private void addNode(JsonNode node) {
            nodes.get().add(node);
        }

        @Override
        protected void commitDirty() {
            super.commitDirty();
            cache.clearAll();
        }

        @Override
        protected void doCommit() {
            commitBulk();
        }

        private void commitConditionally(long index) {
            if (index % batchSize == 0 && nodes.get().size() > 0) {
                commitBulk();
            }
        }

        private void commitBulk() {
            commit(bulkLoadGraph, nodes.get().size());
            nodes.get().clear();
        }

        private void commitRegular() {
            commit(graph, nodes.get().size());
            cache.clearAll();
        }

        private void commit(Graph g, int size) {
            parseElement.commit(g);
            display("commit-size: {}: Done!", size);
        }

        private void updateSchema(Map<String, Object> schema, JsonNode node) {
            synchronized (graph) {
                String typeName = parseElement.getType(node);

                try {
                    display("updateSchema: type: {}: ...", typeName);

                    if (schema.containsKey("oid")) {
                        parseElement.parse(graph, cache, node);
                    } else {
                        Object id = schema.get("id");
                        schema.remove("id");
                        parseElement.update(graph, id, schema);
                    }

                    commitRegular();

                    display("updateSchema: type: {}: Done!", typeName);
                } catch (NoSuchElementException ex) {
                    parseElement.parse(graph, cache, node);
                    commitRegular();
                    display("updateSchema: NoSuchElementException processed!: type: {}: Done!", typeName);
                } catch (Exception ex) {
                    graph.tx().rollback();
                    error("updateSchema: failed!: type: " + typeName, ex);
                }
            }
        }

        private void retryBatchCommit() {
            display("Waiting with [{} nodes] for 1 secs.", nodes.get().size());

            try {
                Thread.sleep(WAIT_DURATION_AFTER_COMMIT_EXCEPTION);
                for (JsonNode n : nodes.get()) {
                    parseElement.parse(bulkLoadGraph, cache, n);
                }
                commitBulk();
                display("Done!: After re-adding {}.", nodes.get().size());
            } catch (Exception ex) {
                error("retryBatchCommit: Failed! Potential data loss.", ex);
            }
        }

        private void display(String message, Object s1, Object s2) {
            LOG.info("{}: [{}]: " + message, parseElement.getMessage(), counter, s1, s2);
        }

        private void display(String message, Object s1) {
            display(message, s1, "");
        }

        private void error(String message, Exception ex) {
            LOG.error("{}: [{}]: " + message, parseElement.getMessage(), counter, ex);
        }
    }

    private static class ResumingConsumer extends Consumer {
        public ResumingConsumer(BlockingQueue<JsonNode> workQueue, Graph graph, Graph bulkLoadGraph, ParseElement parseElement, long batchSize) {
            super(workQueue, graph, bulkLoadGraph, parseElement, batchSize);
        }

        @Override
        public void processItem(JsonNode node) {
            if (!contains(node)) {
                super.processItem(node);
            }
        }

        private boolean contains(JsonNode node) {
            return (parseElement.getByOriginalId(bulkLoadGraph, node) != null);
        }
    }

    private static class ConsumerBuilder implements WorkItemBuilder<Consumer, JsonNode> {
        private final Graph        graph;
        private final Graph        bulkLoadGraph;
        private final ParseElement parseElement;
        private final int          batchSize;
        private final boolean      isResuming;

        public ConsumerBuilder(Graph graph, Graph bulkLoadGraph, ParseElement parseElement, int batchSize, boolean isResuming) {
            this.graph         = graph;
            this.bulkLoadGraph = bulkLoadGraph;
            this.batchSize     = batchSize;
            this.parseElement  = parseElement;
            this.isResuming    = isResuming;
        }

        @Override
        public Consumer build(BlockingQueue<JsonNode> queue) {
            return (isResuming)
                    ? new ResumingConsumer(queue, graph, bulkLoadGraph, parseElement, batchSize)
                    : new Consumer(queue, graph, bulkLoadGraph, parseElement, batchSize);
        }
    }

    static class WorkItemManager extends org.apache.atlas.pc.WorkItemManager {
        public WorkItemManager(WorkItemBuilder builder, int batchSize, int numWorkers) {
            super(builder, batchSize, numWorkers);
        }
    }

    public static WorkItemManager create(Graph rGraph, Graph bGraph,
                                         ParseElement parseElement, int numWorkers, int batchSize, boolean isResuming) {
        ConsumerBuilder cb = new ConsumerBuilder(rGraph, bGraph, parseElement, batchSize, isResuming);

        return new WorkItemManager(cb, batchSize, numWorkers);
    }
}
