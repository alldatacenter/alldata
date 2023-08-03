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

import org.apache.atlas.pc.WorkItemBuilder;
import org.apache.atlas.pc.WorkItemConsumer;
import org.apache.atlas.pc.WorkItemManager;
import org.apache.atlas.repository.graphdb.janus.migration.postProcess.PostProcessListProperty;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.apache.tinkerpop.gremlin.structure.VertexProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;

import static org.apache.atlas.repository.Constants.ENTITY_TYPE_PROPERTY_KEY;
import static org.apache.atlas.repository.Constants.TYPENAME_PROPERTY_KEY;

public class PostProcessManager {
    static class Consumer extends WorkItemConsumer<Object> {
        private static final Logger LOG = LoggerFactory.getLogger(Consumer.class);

        private final Graph                                  bulkLoadGraph;
        private final Map<String, Map<String, List<String>>> typePropertiesMap;
        private final int                                    batchSize;
        private       long                                   counter;
        private       long                                   batchCounter;
        private final PostProcessListProperty                processor;
        private final String[]                               nonPrimitiveCategoryKeys;

        public Consumer(BlockingQueue<Object> queue, Graph bulkLoadGraph, Map<String, Map<String, List<String>>> typePropertiesMap, int batchSize) {
            super(queue);

            this.bulkLoadGraph            = bulkLoadGraph;
            this.typePropertiesMap        = typePropertiesMap;
            this.batchSize                = batchSize;
            this.counter                  = 0;
            this.batchCounter             = 0;
            this.processor                = new PostProcessListProperty();
            this.nonPrimitiveCategoryKeys = ElementProcessors.getNonPrimitiveCategoryKeys();
        }

        @Override
        public void processItem(Object vertexId) {
            batchCounter++;
            counter++;

            try {
                Vertex         vertex           = bulkLoadGraph.traversal().V(vertexId).next();
                boolean        isTypeVertex     = vertex.property(TYPENAME_PROPERTY_KEY).isPresent();
                VertexProperty typeNameProperty = vertex.property(ENTITY_TYPE_PROPERTY_KEY);

                if (!isTypeVertex && typeNameProperty.isPresent()) {
                    String typeName = (String) typeNameProperty.value();
                    if (!typePropertiesMap.containsKey(typeName)) {
                        return;
                    }

                    Map<String, List<String>> collectionTypeProperties = typePropertiesMap.get(typeName);
                    for (String key : nonPrimitiveCategoryKeys) {
                        if (!collectionTypeProperties.containsKey(key)) {
                            continue;
                        }

                        for(String propertyName : collectionTypeProperties.get(key)) {
                            processor.process(vertex, typeName, propertyName);
                        }
                    }
                }

                commitBatch();
            } catch (Exception ex) {
                LOG.error("processItem: v[{}] error!", vertexId, ex);
            }
        }

        private void commitBatch() {
            if (batchCounter >= batchSize) {
                LOG.info("[{}]: batch: {}: commit", counter, batchCounter);

                commit();

                batchCounter = 0;
            }
        }

        @Override
        protected void doCommit() {
            bulkLoadGraph.tx().commit();
        }
    }

    private static class ConsumerBuilder implements WorkItemBuilder<Consumer, Object> {
        private final Graph                                  bulkLoadGraph;
        private final int                                    batchSize;
        private final Map<String, Map<String, List<String>>> vertexPropertiesToPostProcess;

        public ConsumerBuilder(Graph bulkLoadGraph, Map<String, Map<String, List<String>>> propertiesToPostProcess, int batchSize) {
            this.bulkLoadGraph                 = bulkLoadGraph;
            this.batchSize                     = batchSize;
            this.vertexPropertiesToPostProcess = propertiesToPostProcess;
        }

        @Override
        public Consumer build(BlockingQueue<Object> queue) {
            return new Consumer(queue, bulkLoadGraph, vertexPropertiesToPostProcess, batchSize);
        }
    }

    static class WorkItemsManager extends WorkItemManager<Object, Consumer> {
        public WorkItemsManager(WorkItemBuilder builder, int batchSize, int numWorkers) {
            super(builder, batchSize, numWorkers);
        }
    }

    public static WorkItemsManager create(Graph bGraph, Map<String, Map<String, List<String>>> propertiesToPostProcess,
                                          int batchSize, int numWorkers) {
        ConsumerBuilder cb = new ConsumerBuilder(bGraph, propertiesToPostProcess, batchSize);

        return new WorkItemsManager(cb, batchSize, numWorkers);
    }
}
