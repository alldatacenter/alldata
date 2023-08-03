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
package org.apache.atlas.repository.graphdb.janus;

import com.google.common.base.Preconditions;
import org.apache.atlas.AtlasConfiguration;
import org.apache.atlas.repository.graphdb.AtlasCardinality;
import org.apache.atlas.repository.graphdb.AtlasEdgeDirection;
import org.apache.atlas.repository.graphdb.AtlasEdgeLabel;
import org.apache.atlas.repository.graphdb.AtlasElement;
import org.apache.atlas.repository.graphdb.AtlasGraphIndex;
import org.apache.atlas.repository.graphdb.AtlasGraphManagement;
import org.apache.atlas.repository.graphdb.AtlasPropertyKey;
import org.apache.commons.lang.StringUtils;
import org.apache.tinkerpop.gremlin.structure.Direction;
import org.apache.tinkerpop.gremlin.structure.Edge;
import org.apache.tinkerpop.gremlin.structure.Element;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.janusgraph.core.Cardinality;
import org.janusgraph.core.EdgeLabel;
import org.janusgraph.core.JanusGraph;
import org.janusgraph.core.JanusGraphElement;
import org.janusgraph.core.JanusGraphFactory;
import org.janusgraph.core.PropertyKey;
import org.janusgraph.core.log.TransactionRecovery;
import org.janusgraph.core.schema.ConsistencyModifier;
import org.janusgraph.core.schema.JanusGraphIndex;
import org.janusgraph.core.schema.JanusGraphManagement;
import org.janusgraph.core.schema.JanusGraphManagement.IndexBuilder;
import org.janusgraph.core.schema.Mapping;
import org.janusgraph.core.schema.Parameter;
import org.janusgraph.core.schema.PropertyKeyMaker;
import org.janusgraph.core.schema.SchemaStatus;
import org.janusgraph.diskstorage.BackendTransaction;
import org.janusgraph.diskstorage.indexing.IndexEntry;
import org.janusgraph.graphdb.database.IndexSerializer;
import org.janusgraph.graphdb.database.StandardJanusGraph;
import org.janusgraph.graphdb.database.management.GraphIndexStatusReport;
import org.janusgraph.graphdb.database.management.ManagementSystem;
import org.janusgraph.graphdb.internal.Token;
import org.janusgraph.graphdb.log.StandardTransactionLogProcessor;
import org.janusgraph.graphdb.transaction.StandardJanusGraphTx;
import org.janusgraph.graphdb.types.IndexType;
import org.janusgraph.graphdb.types.MixedIndexType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import static org.janusgraph.core.schema.SchemaAction.ENABLE_INDEX;
import static org.janusgraph.core.schema.SchemaStatus.ENABLED;
import static org.janusgraph.core.schema.SchemaStatus.INSTALLED;
import static org.janusgraph.core.schema.SchemaStatus.REGISTERED;

/**
 * Janus implementation of AtlasGraphManagement.
 */
public class AtlasJanusGraphManagement implements AtlasGraphManagement {
    private static final boolean lockEnabled = AtlasConfiguration.STORAGE_CONSISTENCY_LOCK_ENABLED.getBoolean();
    private static final Parameter[] STRING_PARAMETER_ARRAY = new Parameter[]{Mapping.STRING.asParameter()};

    private static final Logger LOG            = LoggerFactory.getLogger(AtlasJanusGraphManagement.class);
    private static final char[] RESERVED_CHARS = { '{', '}', '"', '$', Token.SEPARATOR_CHAR };

    private AtlasJanusGraph      graph;
    private JanusGraphManagement management;
    private Set<String>          newMultProperties = new HashSet<>();

    public AtlasJanusGraphManagement(AtlasJanusGraph graph, JanusGraphManagement managementSystem) {
        this.management = managementSystem;
        this.graph      = graph;
    }

    @Override
    public void createVertexMixedIndex(String indexName, String backingIndex, List<AtlasPropertyKey> propertyKeys) {
        IndexBuilder indexBuilder = management.buildIndex(indexName, Vertex.class);

        for (AtlasPropertyKey key : propertyKeys) {
            PropertyKey janusKey = AtlasJanusObjectFactory.createPropertyKey(key);
            indexBuilder.addKey(janusKey);
        }

        indexBuilder.buildMixedIndex(backingIndex);
    }

    @Override
    public void createEdgeMixedIndex(String indexName, String backingIndex, List<AtlasPropertyKey> propertyKeys) {
        IndexBuilder indexBuilder = management.buildIndex(indexName, Edge.class);

        for (AtlasPropertyKey key : propertyKeys) {
            PropertyKey janusKey = AtlasJanusObjectFactory.createPropertyKey(key);
            indexBuilder.addKey(janusKey);
        }

        indexBuilder.buildMixedIndex(backingIndex);
    }

    @Override
    public void createEdgeIndex(String label, String indexName, AtlasEdgeDirection edgeDirection, List<AtlasPropertyKey> propertyKeys) {
        EdgeLabel edgeLabel = management.getEdgeLabel(label);

        if (edgeLabel == null) {
            edgeLabel = management.makeEdgeLabel(label).make();
        }

        Direction     direction = AtlasJanusObjectFactory.createDirection(edgeDirection);
        PropertyKey[] keys      = AtlasJanusObjectFactory.createPropertyKeys(propertyKeys);

        if (management.getRelationIndex(edgeLabel, indexName) == null) {
            management.buildEdgeIndex(edgeLabel, indexName, direction, keys);
        }
    }

    @Override
    public void createFullTextMixedIndex(String indexName, String backingIndex, List<AtlasPropertyKey> propertyKeys) {
        IndexBuilder indexBuilder = management.buildIndex(indexName, Vertex.class);

        for (AtlasPropertyKey key : propertyKeys) {
            PropertyKey janusKey = AtlasJanusObjectFactory.createPropertyKey(key);
            indexBuilder.addKey(janusKey, org.janusgraph.core.schema.Parameter.of("mapping", Mapping.TEXT));
        }

        indexBuilder.buildMixedIndex(backingIndex);
    }

    @Override
    public boolean containsPropertyKey(String propertyName) {
        return management.containsPropertyKey(propertyName);
    }

    @Override
    public void rollback() {
        management.rollback();

    }

    @Override
    public void commit() {
        graph.addMultiProperties(newMultProperties);
        newMultProperties.clear();
        management.commit();
    }

    private static void checkName(String name) {
        //for some reason, name checking was removed from StandardPropertyKeyMaker.make()
        //in Janus.  For consistency, do the check here.
        Preconditions.checkArgument(StringUtils.isNotBlank(name), "Need to specify name");

        for (char c : RESERVED_CHARS) {
            Preconditions.checkArgument(name.indexOf(c) < 0, "Name can not contains reserved character %s: %s", c, name);
        }

    }

    @Override
    public AtlasPropertyKey makePropertyKey(String propertyName, Class propertyClass, AtlasCardinality cardinality) {
        if (cardinality.isMany()) {
            newMultProperties.add(propertyName);
        }

        PropertyKeyMaker propertyKeyBuilder = management.makePropertyKey(propertyName).dataType(propertyClass);
        if (cardinality != null) {
            Cardinality janusCardinality = AtlasJanusObjectFactory.createCardinality(cardinality);
            propertyKeyBuilder.cardinality(janusCardinality);
        }

        PropertyKey propertyKey = propertyKeyBuilder.make();

        return GraphDbObjectFactory.createPropertyKey(propertyKey);
    }

    @Override
    public AtlasEdgeLabel makeEdgeLabel(String label) {
        EdgeLabel edgeLabel = management.makeEdgeLabel(label).make();

        return GraphDbObjectFactory.createEdgeLabel(edgeLabel);
    }

    @Override
    public void deletePropertyKey(String propertyKey) {
        PropertyKey janusPropertyKey = management.getPropertyKey(propertyKey);

        if (null == janusPropertyKey) return;

        for (int i = 0;; i++) {
            String deletedKeyName = janusPropertyKey + "_deleted_" + i;

            if (null == management.getPropertyKey(deletedKeyName)) {
                management.changeName(janusPropertyKey, deletedKeyName);
                break;
            }
        }
    }

    @Override
    public AtlasPropertyKey getPropertyKey(String propertyName) {
        checkName(propertyName);

        return GraphDbObjectFactory.createPropertyKey(management.getPropertyKey(propertyName));
    }

    @Override
    public AtlasEdgeLabel getEdgeLabel(String label) {
        return GraphDbObjectFactory.createEdgeLabel(management.getEdgeLabel(label));
    }

    @Override
    public String addMixedIndex(String indexName, AtlasPropertyKey propertyKey, boolean isStringField) {
        PropertyKey     janusKey        = AtlasJanusObjectFactory.createPropertyKey(propertyKey);
        JanusGraphIndex janusGraphIndex = management.getGraphIndex(indexName);

        if(isStringField) {
            management.addIndexKey(janusGraphIndex, janusKey, Mapping.STRING.asParameter());
            LOG.debug("created a string type for {} with janueKey {}.", propertyKey.getName(), janusKey);
        } else {
            management.addIndexKey(janusGraphIndex, janusKey);
            LOG.debug("created a default type for {} with janueKey {}.", propertyKey.getName(), janusKey);
        }

        String encodedName = "";
        if(isStringField) {
            encodedName = graph.getIndexFieldName(propertyKey, janusGraphIndex, STRING_PARAMETER_ARRAY);
        } else {
            encodedName = graph.getIndexFieldName(propertyKey, janusGraphIndex);
        }


        LOG.info("property '{}' is encoded to '{}'.", propertyKey.getName(), encodedName);

        return encodedName;
    }

    @Override
    public String getIndexFieldName(String indexName, AtlasPropertyKey propertyKey, boolean isStringField) {
        JanusGraphIndex janusGraphIndex = management.getGraphIndex(indexName);

        if(isStringField) {
            return graph.getIndexFieldName(propertyKey, janusGraphIndex, STRING_PARAMETER_ARRAY);
        } else {
            return graph.getIndexFieldName(propertyKey, janusGraphIndex);
        }

    }

    public AtlasGraphIndex getGraphIndex(String indexName) {
        JanusGraphIndex index = management.getGraphIndex(indexName);

        return GraphDbObjectFactory.createGraphIndex(index);
    }

    @Override
    public boolean edgeIndexExist(String label, String indexName) {
        EdgeLabel edgeLabel = management.getEdgeLabel(label);

        return edgeLabel != null && management.getRelationIndex(edgeLabel, indexName) != null;
    }

    @Override
    public void createVertexCompositeIndex(String propertyName, boolean isUnique, List<AtlasPropertyKey> propertyKeys) {
        createCompositeIndex(propertyName, isUnique, propertyKeys, Vertex.class);
    }

    @Override
    public void createEdgeCompositeIndex(String propertyName, boolean isUnique, List<AtlasPropertyKey> propertyKeys) {
        createCompositeIndex(propertyName, isUnique, propertyKeys, Edge.class);
    }

    private void createCompositeIndex(String propertyName, boolean isUnique, List<AtlasPropertyKey> propertyKeys, Class<? extends Element> elementType) {
        IndexBuilder indexBuilder = management.buildIndex(propertyName, elementType);

        for (AtlasPropertyKey key : propertyKeys) {
            PropertyKey janusKey = AtlasJanusObjectFactory.createPropertyKey(key);
            indexBuilder.addKey(janusKey);
        }

        if (isUnique) {
            indexBuilder.unique();
        }

        JanusGraphIndex index = indexBuilder.buildCompositeIndex();

        if (lockEnabled && isUnique) {
            management.setConsistency(index, ConsistencyModifier.LOCK);
        }
    }

    @Override
    public void updateUniqueIndexesForConsistencyLock() {
        try {
            setConsistency(this.management, Vertex.class);
            setConsistency(this.management, Edge.class);
        } finally {
            commit();
        }
    }

    @Override
    public void updateSchemaStatus() {
        updateSchemaStatus(this.management, this.graph.getGraph(), Vertex.class);
        updateSchemaStatus(this.management, this.graph.getGraph(), Edge.class);
    }

    public static void updateSchemaStatus(JanusGraphManagement mgmt, JanusGraph graph, Class<? extends Element> elementType) {
        LOG.info("updating SchemaStatus for {}: Starting...", elementType.getSimpleName());
        int count = 0;

        Iterable<JanusGraphIndex> iterable = mgmt.getGraphIndexes(elementType);

        for (JanusGraphIndex index : iterable) {

            if (index.isCompositeIndex()) {
                PropertyKey[] propertyKeys = index.getFieldKeys();
                SchemaStatus status       = index.getIndexStatus(propertyKeys[0]);
                String        indexName    = index.name();

                try {
                    if (status == REGISTERED) {
                        JanusGraphManagement management = graph.openManagement();
                        JanusGraphIndex indexToUpdate   = management.getGraphIndex(indexName);
                        management.updateIndex(indexToUpdate, ENABLE_INDEX).get();
                        management.commit();

                        GraphIndexStatusReport report = ManagementSystem.awaitGraphIndexStatus(graph, indexName).status(ENABLED).call();

                        if (!report.getConvergedKeys().isEmpty() && report.getConvergedKeys().containsKey(indexName)) {
                            LOG.info("SchemaStatus updated for index: {}, from {} to {}.", index.name(), REGISTERED, ENABLED);

                            count++;
                        } else if (!report.getNotConvergedKeys().isEmpty() && report.getNotConvergedKeys().containsKey(indexName)) {
                            LOG.error("SchemaStatus failed to update index: {}, from {} to {}.", index.name(), REGISTERED, ENABLED);
                        }

                    } else if (status == INSTALLED) {
                        LOG.warn("SchemaStatus {} found for index: {}", INSTALLED, indexName);

                    }
                } catch (InterruptedException e) {
                    LOG.error("IllegalStateException for indexName : {}, Exception: ", indexName, e);
                } catch (ExecutionException e) {
                    LOG.error("ExecutionException for indexName : {}, Exception: ", indexName, e);
                }
            }
        }

        LOG.info("updating SchemaStatus for {}: {}: Done!", elementType.getSimpleName(), count);
    }

    private static void setConsistency(JanusGraphManagement mgmt, Class<? extends Element> elementType) {
        LOG.info("setConsistency: {}: Starting...", elementType.getSimpleName());
        int count = 0;

        try {
            Iterable<JanusGraphIndex> iterable = mgmt.getGraphIndexes(elementType);
            for (JanusGraphIndex index : iterable) {
                if (!index.isCompositeIndex() || !index.isUnique() || mgmt.getConsistency(index) == ConsistencyModifier.LOCK) {
                    continue;
                }

                for (PropertyKey propertyKey : index.getFieldKeys()) {
                    LOG.info("setConsistency: {}: {}", count, propertyKey.name());
                }

                mgmt.setConsistency(index, ConsistencyModifier.LOCK);
                count++;
            }
        }
        finally {
            LOG.info("setConsistency: {}: {}: Done!", elementType.getSimpleName(), count);
        }
    }

    @Override
    public void reindex(String indexName, List<AtlasElement> elements) throws Exception {
        try {
            JanusGraphIndex index = management.getGraphIndex(indexName);
            if (index == null || !(management instanceof ManagementSystem) || !(graph.getGraph() instanceof StandardJanusGraph)) {
                LOG.error("Could not retrieve index for name: {} ", indexName);
                return;
            }

            ManagementSystem managementSystem = (ManagementSystem) management;
            IndexType indexType = managementSystem.getSchemaVertex(index).asIndexType();
            if (!(indexType instanceof MixedIndexType)) {
                LOG.warn("Index: {}: Not of MixedIndexType ", indexName);
                return;
            }

            IndexSerializer indexSerializer = ((StandardJanusGraph) graph.getGraph()).getIndexSerializer();
            reindexElement(managementSystem, indexSerializer, (MixedIndexType) indexType, elements);
        } catch (Exception exception) {
            throw exception;
        } finally {
            management.commit();
        }
    }

    @Override
    public Object startIndexRecovery(long recoveryStartTime) {
        Instant    recoveryStartInstant = Instant.ofEpochMilli(recoveryStartTime);
        JanusGraph janusGraph           = this.graph.getGraph();

        return JanusGraphFactory.startTransactionRecovery(janusGraph, recoveryStartInstant);
    }

    @Override
    public void stopIndexRecovery(Object txRecoveryObject) {
        if (txRecoveryObject == null) {
            return;
        }

        try {
            if (txRecoveryObject instanceof TransactionRecovery) {
                TransactionRecovery txRecovery = (TransactionRecovery) txRecoveryObject;
                StandardJanusGraph  janusGraph = (StandardJanusGraph) this.graph.getGraph();

                LOG.info("stopIndexRecovery: Index Client is unhealthy. Index recovery: Paused!");

                janusGraph.getBackend().getSystemTxLog().close();

                txRecovery.shutdown();
            } else {
                LOG.error("stopIndexRecovery({}): Invalid transaction recovery object!", txRecoveryObject);
            }
        } catch (Exception e) {
            LOG.warn("stopIndexRecovery: Error while shutting down transaction recovery", e);
        }
    }

    @Override
    public void printIndexRecoveryStats(Object txRecoveryObject) {
        if (txRecoveryObject == null) {
            return;
        }

        try {
            if (txRecoveryObject instanceof TransactionRecovery) {
                StandardTransactionLogProcessor txRecovery = (StandardTransactionLogProcessor) txRecoveryObject;
                long[]                          statistics = txRecovery.getStatistics();

                if (statistics.length >= 2) {
                    LOG.info("Index Recovery: Stats: Success:{}: Failed: {}", statistics[0], statistics[1]);
                } else {
                    LOG.info("Index Recovery: Stats: {}", statistics);
                }
            } else {
                LOG.error("Transaction stats: Invalid transaction recovery object!: Unexpected type: {}: Details: {}", txRecoveryObject.getClass().toString(), txRecoveryObject);
            }
        } catch (Exception e) {
            LOG.error("Error: Retrieving log transaction stats!", e);
        }
    }

    private void reindexElement(ManagementSystem managementSystem, IndexSerializer indexSerializer, MixedIndexType indexType, List<AtlasElement> elements) throws Exception {
        Map<String, Map<String, List<IndexEntry>>> documentsPerStore = new HashMap<>();
        StandardJanusGraphTx tx = managementSystem.getWrappedTx();
        BackendTransaction txHandle = tx.getTxHandle();

        try {
            JanusGraphElement janusGraphElement = null;
            for (AtlasElement element : elements) {
                try {
                    if (element == null || element.getWrappedElement() == null) {
                        continue;
                    }

                    janusGraphElement = element.getWrappedElement();
                    indexSerializer.reindexElement(janusGraphElement, indexType, documentsPerStore);
                } catch (Exception e) {
                    LOG.warn("{}: Exception: {}:{}", indexType.getName(), e.getClass().getSimpleName(), e.getMessage());
                }
            }
        } finally {
            if (txHandle != null) {
                txHandle.getIndexTransaction(indexType.getBackingIndexName()).restore(documentsPerStore);
            }
        }
    }
}
