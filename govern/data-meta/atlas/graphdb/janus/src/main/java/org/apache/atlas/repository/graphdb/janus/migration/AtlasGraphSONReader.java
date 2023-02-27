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

import org.apache.atlas.ApplicationProperties;
import org.apache.atlas.AtlasException;
import org.apache.atlas.repository.graphdb.janus.migration.JsonNodeParsers.ParseElement;
import org.apache.atlas.repository.graphdb.janus.migration.JsonNodeProcessManager.WorkItemManager;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.apache.tinkerpop.gremlin.structure.io.graphson.GraphSONMapper;
import org.apache.tinkerpop.gremlin.structure.io.graphson.TypeInfo;
import org.apache.tinkerpop.shaded.jackson.core.JsonFactory;
import org.apache.tinkerpop.shaded.jackson.core.JsonParser;
import org.apache.tinkerpop.shaded.jackson.core.JsonToken;
import org.apache.tinkerpop.shaded.jackson.databind.JsonNode;
import org.apache.tinkerpop.shaded.jackson.databind.ObjectMapper;
import org.janusgraph.core.JanusGraph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.atomic.AtomicLong;

public final class AtlasGraphSONReader {
    private static final Logger LOG = LoggerFactory.getLogger(AtlasGraphSONReader.class);

    private static String APPLICATION_PROPERTY_MIGRATION_START_INDEX      = "atlas.migration.mode.start.index";
    private static String APPLICATION_PROPERTY_MIGRATION_NUMER_OF_WORKERS = "atlas.migration.mode.workers";
    private static String APPLICATION_PROPERTY_MIGRATION_BATCH_SIZE       = "atlas.migration.mode.batch.size";

    private final ObjectMapper        mapper;
    private final ElementProcessors   relationshipCache;
    private final Graph               graph;
    private final Graph               bulkLoadGraph;
    private final int                 numWorkers;
    private final int                 batchSize;
    private final long                suppliedStartIndex;
    private final GraphSONUtility     graphSONUtility;
    private       ReaderStatusManager readerStatusManager;
    private       AtomicLong          counter;

    private AtlasGraphSONReader(ObjectMapper mapper, ElementProcessors relationshipLookup, Graph graph,
                                Graph bulkLoadGraph, int numWorkers, int batchSize, long suppliedStartIndex) {
        this.mapper                 = mapper;
        this.relationshipCache      = relationshipLookup;
        this.graph                  = graph;
        this.bulkLoadGraph          = bulkLoadGraph;
        this.numWorkers             = numWorkers;
        this.batchSize              = batchSize;
        this.suppliedStartIndex     = suppliedStartIndex;
        this.graphSONUtility        = new GraphSONUtility(relationshipCache);
    }

    public void readGraph(final InputStream inputStream) throws IOException {
        counter = new AtomicLong(0);

        final long        startIndex = initStatusManager();
        final JsonFactory factory    = mapper.getFactory();

        LOG.info("AtlasGraphSONReader.readGraph: numWorkers: {}: batchSize: {}: startIndex: {}", numWorkers, batchSize, startIndex);

        try (JsonParser parser = factory.createParser(inputStream)) {
            if (parser.nextToken() != JsonToken.START_OBJECT) {
                throw new IOException("Expected data to start with an Object");
            }

            readerStatusManager.update(bulkLoadGraph, counter.get(), ReaderStatusManager.STATUS_IN_PROGRESS);

            while (parser.nextToken() != JsonToken.END_OBJECT) {
                final String fieldName = parser.getCurrentName() == null ? "" : parser.getCurrentName();

                switch (fieldName) {
                    case GraphSONTokensTP2.MODE:
                        parser.nextToken();

                        final String mode = parser.getText();

                        if (!mode.equals("EXTENDED")) {
                            throw new IllegalStateException("The legacy GraphSON must be generated with GraphSONMode.EXTENDED");
                        }

                        counter.getAndIncrement();
                        break;

                    case GraphSONTokensTP2.VERTICES:
                        processElement(parser, new JsonNodeParsers.ParseVertex(), startIndex);
                        break;

                    case GraphSONTokensTP2.EDGES:
                        processElement(parser, new JsonNodeParsers.ParseEdge(), startIndex);
                        break;

                    case GraphSONTokensTP2.VERTEX_COUNT:
                        parser.nextToken();
                        LOG.info("Vertex count: {}", parser.getLongValue());
                        break;

                    case GraphSONTokensTP2.EDGE_COUNT:
                        parser.nextToken();
                        LOG.info("Edge count: {}", parser.getLongValue());
                        break;

                    default:
                        throw new IllegalStateException(String.format("Unexpected token in GraphSON - %s", fieldName));
                }
            }

            postProcess(startIndex);

            readerStatusManager.end(bulkLoadGraph, counter.get(), ReaderStatusManager.STATUS_SUCCESS);
        } catch (Exception ex) {
            readerStatusManager.end(bulkLoadGraph, counter.get(), ReaderStatusManager.STATUS_FAILED);
            throw new IOException(ex);
        } finally {
            LOG.info("AtlasGraphSONReader.readGraph: Done!: {}", counter.get());
        }
    }

    private long initStatusManager() {
        readerStatusManager = new ReaderStatusManager(graph, bulkLoadGraph);

        return (this.suppliedStartIndex == 0) ? readerStatusManager.getStartIndex() : this.suppliedStartIndex;
    }

    private void processElement(JsonParser parser, ParseElement parseElement, long startIndex) throws InterruptedException {
        LOG.info("processElement: {}: Starting... : counter at: {}", parseElement.getMessage(), counter.get());

        try {
            readerStatusManager.update(graph, counter.get(), true);

            parseElement.setContext(graphSONUtility);

            WorkItemManager wim = JsonNodeProcessManager.create(graph, bulkLoadGraph, parseElement,
                                                                numWorkers, batchSize, shouldSkip(startIndex, counter.get()));

            parser.nextToken();

            while (parser.nextToken() != JsonToken.END_ARRAY) {
                handleInterrupt(bulkLoadGraph, counter.incrementAndGet());

                final JsonNode node = parser.readValueAsTree();

                if (shouldSkip(startIndex, counter.get()) || parseElement.isTypeNode(node)) {
                    continue;
                }

                updateStatusConditionally(bulkLoadGraph, counter.get());

                wim.produce(node);
            }

            wim.shutdown();
        } catch (InterruptedException ex) {
            throw ex;
        } catch (Exception ex) {
            LOG.error("processElement: {}: failed!", parseElement.getMessage(), ex);
        } finally {
            LOG.info("processElement: {}: Done! : [{}]", parseElement.getMessage(), counter);

            readerStatusManager.update(bulkLoadGraph, counter.get(), true);
        }
    }

    private void postProcess(long startIndex) {
        LOG.info("postProcess: Starting... : counter at: {}", counter.get());

        try {
            PostProcessManager.WorkItemsManager wim   = PostProcessManager.create(bulkLoadGraph, relationshipCache.getPropertiesToPostProcess(), batchSize, numWorkers);
            GraphTraversal                      query = bulkLoadGraph.traversal().V();

            while (query.hasNext()) {
                handleInterrupt(bulkLoadGraph, counter.incrementAndGet());

                if(shouldSkip(startIndex, counter.get())) {
                    continue;
                }

                Vertex v = (Vertex) query.next();

                wim.produce(v.id());

                updateStatusConditionally(bulkLoadGraph, counter.get());
            }

            wim.shutdown();
        } catch (Exception ex) {
            LOG.error("postProcess: failed!", ex);
        } finally {
            LOG.info("postProcess: Done! : [{}]", counter.get());

            readerStatusManager.update(bulkLoadGraph, counter.get(), true);
        }
    }

    private boolean shouldSkip(long startIndex, long index) {
        return (startIndex != 0) && (index <= startIndex);
    }

    private void handleInterrupt(Graph graph, long counter) throws InterruptedException {
        if (!Thread.interrupted()) {
            return;
        }

        readerStatusManager.update(graph, counter, false);
        LOG.error("Thread interrupted: {}", counter);
        throw new InterruptedException();
    }

    private void updateStatusConditionally(Graph graph, long counter) {
        if(counter % batchSize == 0) {
            readerStatusManager.update(graph, counter, false);
        }
    }

    public static Builder build() {
        return new Builder();
    }

    public final static class Builder {
        private int               batchSize = 500;
        private ElementProcessors relationshipCache;
        private Graph             graph;
        private Graph             bulkLoadGraph;
        private int               numWorkers;
        private long              suppliedStartIndex;

        private Builder() {
        }

        private void setDefaults() {
            try {
                this.startIndex(ApplicationProperties.get().getLong(APPLICATION_PROPERTY_MIGRATION_START_INDEX, 0L))
                        .numWorkers(ApplicationProperties.get().getInt(APPLICATION_PROPERTY_MIGRATION_NUMER_OF_WORKERS, 4))
                        .batchSize(ApplicationProperties.get().getInt(APPLICATION_PROPERTY_MIGRATION_BATCH_SIZE, 3000));
            } catch (AtlasException ex) {
                LOG.error("setDefaults: failed!", ex);
            }
        }

        public AtlasGraphSONReader create() {
            setDefaults();
            if(bulkLoadGraph == null) {
                bulkLoadGraph = graph;
            }

            final GraphSONMapper.Builder builder = GraphSONMapper.build();
            final GraphSONMapper         mapper  = builder.create();

            return new AtlasGraphSONReader(mapper.createMapper(), relationshipCache, graph, bulkLoadGraph,
                                                                    numWorkers, batchSize, suppliedStartIndex);
        }

        public Builder relationshipCache(ElementProcessors relationshipCache) {
            this.relationshipCache = relationshipCache;

            return this;
        }

        public Builder schemaDB(JanusGraph graph) {
            this.graph = graph;

            return this;
        }

        public Builder bulkLoadingDB(Graph graph) {
            this.bulkLoadGraph = graph;

            return this;
        }

        public Builder numWorkers(int numWorkers) {
            if(bulkLoadGraph == null || graph == null) {
                this.numWorkers = 1;

                LOG.info("numWorkers: {}, since one of the 2 graphs is null.", this.numWorkers);
            } else {
                this.numWorkers = numWorkers;
            }

            return this;
        }

        public Builder batchSize(int batchSize) {
            this.batchSize = batchSize;

            return this;
        }

        public Builder startIndex(long suppliedStartIndex) {
            this.suppliedStartIndex = suppliedStartIndex;

            return this;
        }
    }
}
