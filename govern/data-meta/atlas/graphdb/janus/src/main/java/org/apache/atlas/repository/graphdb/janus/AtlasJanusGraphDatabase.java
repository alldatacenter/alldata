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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;
import org.apache.atlas.ApplicationProperties;
import org.apache.atlas.AtlasException;
import org.apache.atlas.repository.graphdb.AtlasGraph;
import org.apache.atlas.repository.graphdb.GraphDatabase;
import org.apache.atlas.repository.graphdb.janus.serializer.BigDecimalSerializer;
import org.apache.atlas.repository.graphdb.janus.serializer.BigIntegerSerializer;
import org.apache.atlas.repository.graphdb.janus.serializer.TypeCategorySerializer;
import org.apache.atlas.typesystem.types.DataTypes.TypeCategory;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationConverter;
import org.apache.tinkerpop.gremlin.structure.io.graphson.GraphSONMapper;
import org.janusgraph.core.JanusGraph;
import org.janusgraph.core.JanusGraphException;
import org.janusgraph.core.JanusGraphFactory;
import org.janusgraph.core.schema.JanusGraphManagement;
import org.janusgraph.diskstorage.StandardIndexProvider;
import org.janusgraph.diskstorage.StandardStoreManager;
import org.janusgraph.diskstorage.es.ElasticSearch7Index;
import org.janusgraph.diskstorage.solr.Solr6Index;
import org.janusgraph.graphdb.database.serialize.attribute.SerializableSerializer;
import org.janusgraph.graphdb.tinkerpop.JanusGraphIoRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import static org.apache.atlas.ApplicationProperties.DEFAULT_INDEX_RECOVERY;
import static org.apache.atlas.ApplicationProperties.INDEX_RECOVERY_CONF;

/**
 * Default implementation for Graph Provider that doles out JanusGraph.
 */
public class AtlasJanusGraphDatabase implements GraphDatabase<AtlasJanusVertex, AtlasJanusEdge> {
    private static final Logger LOG      = LoggerFactory.getLogger(AtlasJanusGraphDatabase.class);

    private static final String OLDER_STORAGE_EXCEPTION = "Storage version is incompatible with current client";

    /**
     * Constant for the configuration property that indicates the prefix.
     */
    public static final String GRAPH_PREFIX               = "atlas.graph";
    public static final String INDEX_BACKEND_CONF         = "index.search.backend";
    public static final String SOLR_ZOOKEEPER_URL         = "atlas.graph.index.search.solr.zookeeper-url";
    public static final String SOLR_ZOOKEEPER_URLS        = "atlas.graph.index.search.solr.zookeeper-urls";
    public static final String INDEX_BACKEND_LUCENE       = "lucene";
    public static final String INDEX_BACKEND_ES           = "elasticsearch";
    public static final String GRAPH_TX_LOG_CONF          = "tx.log-tx";
    public static final String GRAPH_TX_LOG_VERBOSE_CONF  = "tx.recovery.verbose";
    public static final String SOLR_INDEX_TX_LOG_TTL_CONF = "write.ahead.log.ttl.in.hours";
    public static final String GRAPH_TX_LOG_TTL_CONF      = "log.tx.ttl";
    public static final long   DEFAULT_GRAPH_TX_LOG_TTL   = 72; //Hrs

    private static volatile AtlasJanusGraph atlasGraphInstance = null;
    private static volatile JanusGraph graphInstance;

    public AtlasJanusGraphDatabase() {
        //update registry
        GraphSONMapper.build().addRegistry(JanusGraphIoRegistry.getInstance()).create();
    }

    public static Configuration getConfiguration() throws AtlasException {
        Configuration configProperties = ApplicationProperties.get();

        if (isEmbeddedSolr()) { // AtlasJanusGraphIndexClient.performRequestHandlerAction() fails for embedded-solr; disable freetext until this issue is resolved
            startEmbeddedSolr();

            configProperties.setProperty(ApplicationProperties.ENABLE_FREETEXT_SEARCH_CONF, false);
        }

        configProperties.setProperty(SOLR_ZOOKEEPER_URLS, configProperties.getStringArray(SOLR_ZOOKEEPER_URL));

        Configuration janusConfig = ApplicationProperties.getSubsetConfiguration(configProperties, GRAPH_PREFIX);

        //add serializers for non-standard property value types that Atlas uses
        janusConfig.setProperty("attributes.custom.attribute1.attribute-class", TypeCategory.class.getName());
        janusConfig.setProperty("attributes.custom.attribute1.serializer-class", TypeCategorySerializer.class.getName());

        //not ideal, but avoids making large changes to Atlas
        janusConfig.setProperty("attributes.custom.attribute2.attribute-class", ArrayList.class.getName());
        janusConfig.setProperty("attributes.custom.attribute2.serializer-class", SerializableSerializer.class.getName());

        janusConfig.setProperty("attributes.custom.attribute3.attribute-class", BigInteger.class.getName());
        janusConfig.setProperty("attributes.custom.attribute3.serializer-class", BigIntegerSerializer.class.getName());

        janusConfig.setProperty("attributes.custom.attribute4.attribute-class", BigDecimal.class.getName());
        janusConfig.setProperty("attributes.custom.attribute4.serializer-class", BigDecimalSerializer.class.getName());

        return janusConfig;
    }

    static {
        addHBase2Support();

        addSolr6Index();

        addElasticSearch7Index();
    }

    private static void addHBase2Support() {
        try {
            Field field = StandardStoreManager.class.getDeclaredField("ALL_MANAGER_CLASSES");
            field.setAccessible(true);

            Field modifiersField = Field.class.getDeclaredField("modifiers");
            modifiersField.setAccessible(true);
            modifiersField.setInt(field, field.getModifiers() & ~Modifier.FINAL);

            Map<String, String> customMap = new HashMap<>(StandardStoreManager.getAllManagerClasses());
            customMap.put("hbase2", org.janusgraph.diskstorage.hbase2.HBaseStoreManager.class.getName());
            ImmutableMap<String, String> immap = ImmutableMap.copyOf(customMap);
            field.set(null, immap);

            LOG.debug("Injected HBase2 support - {}", org.janusgraph.diskstorage.hbase2.HBaseStoreManager.class.getName());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static void addSolr6Index() {
        try {
            Field field = StandardIndexProvider.class.getDeclaredField("ALL_MANAGER_CLASSES");
            field.setAccessible(true);

            Field modifiersField = Field.class.getDeclaredField("modifiers");
            modifiersField.setAccessible(true);
            modifiersField.setInt(field, field.getModifiers() & ~Modifier.FINAL);

            Map<String, String> customMap = new HashMap<>(StandardIndexProvider.getAllProviderClasses());
            customMap.put("solr", Solr6Index.class.getName());
            ImmutableMap<String, String> immap = ImmutableMap.copyOf(customMap);
            field.set(null, immap);

            LOG.debug("Injected solr6 index - {}", Solr6Index.class.getName());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static void addElasticSearch7Index() {
        try {
            Field field = StandardIndexProvider.class.getDeclaredField("ALL_MANAGER_CLASSES");
            field.setAccessible(true);

            Field modifiersField = Field.class.getDeclaredField("modifiers");
            modifiersField.setAccessible(true);
            modifiersField.setInt(field, field.getModifiers() & ~Modifier.FINAL);

            Map<String, String> customMap = new HashMap<>(StandardIndexProvider.getAllProviderClasses());
            customMap.put("elasticsearch", ElasticSearch7Index.class.getName());
            ImmutableMap<String, String> immap = ImmutableMap.copyOf(customMap);
            field.set(null, immap);

            LOG.debug("Injected es7 index - {}", ElasticSearch7Index.class.getName());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    public static JanusGraph getGraphInstance() {
        if (graphInstance == null) {
            synchronized (AtlasJanusGraphDatabase.class) {
                if (graphInstance == null) {
                    Configuration config;
                    try {
                        config = getConfiguration();
                    } catch (AtlasException e) {
                        throw new RuntimeException(e);
                    }

                    configureTxLogBasedIndexRecovery();

                    graphInstance      = initJanusGraph(config);
                    atlasGraphInstance = new AtlasJanusGraph();

                    validateIndexBackend(config);

                }
            }
        }
        return graphInstance;
    }

    @VisibleForTesting
    static JanusGraph initJanusGraph(Configuration config) {

        org.apache.commons.configuration2.Configuration conf2 = createConfiguration2(config);
        try {
            return JanusGraphFactory.open(conf2);
        } catch (JanusGraphException e) {
            LOG.warn("JanusGraphException: {}", e.getMessage());
            if (e.getMessage().startsWith(OLDER_STORAGE_EXCEPTION)) {
                LOG.info("Newer client is being used with older janus storage version. Setting allow-upgrade=true and reattempting connection");
                config.addProperty("graph.allow-upgrade", true);
                return JanusGraphFactory.open(conf2);
            } else {
                throw new RuntimeException(e);
            }
        }
    }

    public static void configureTxLogBasedIndexRecovery() {
        try {
            boolean  recoveryEnabled = ApplicationProperties.get().getBoolean(INDEX_RECOVERY_CONF, DEFAULT_INDEX_RECOVERY);
            long     ttl             = ApplicationProperties.get().getLong(SOLR_INDEX_TX_LOG_TTL_CONF, DEFAULT_GRAPH_TX_LOG_TTL);
            Duration txLogTtlSecs    = Duration.ofSeconds(Duration.ofHours(ttl).getSeconds());

            Map<String, Object> properties = new HashMap<String, Object>() {{
                put(GRAPH_TX_LOG_CONF, recoveryEnabled);
                put(GRAPH_TX_LOG_VERBOSE_CONF, recoveryEnabled);
                put(GRAPH_TX_LOG_TTL_CONF, txLogTtlSecs);
            }};

            updateGlobalConfiguration(properties);

            LOG.info("Tx Log-based Index Recovery: {}!", recoveryEnabled ? "Enabled" : "Disabled");
        } catch (Exception e) {
            LOG.error("Error: Failed!", e);
        }
    }

    private static void updateGlobalConfiguration(Map<String, Object> map) {
        JanusGraph           graph            = null;
        JanusGraphManagement managementSystem = null;

        try {
            graph            = initJanusGraph(getConfiguration());
            managementSystem = graph.openManagement();

            for (Map.Entry<String, Object> entry : map.entrySet()) {
                managementSystem.set(entry.getKey(), entry.getValue());
            }

            LOG.info("Global properties updated!: {}", map);
        } catch (Exception ex) {
            LOG.error("Error updating global configuration: {}", map, ex);
        } finally {
            if (managementSystem != null) {
                managementSystem.commit();
            }

            if (graph != null) {
                graph.close();
            }
        }
    }

    public static JanusGraph getBulkLoadingGraphInstance() {
        try {
            Configuration cfg = getConfiguration();
            cfg.setProperty("storage.batch-loading", true);

            org.apache.commons.configuration2.Configuration conf2 = createConfiguration2(cfg);
            return JanusGraphFactory.open(conf2);
        } catch (IllegalArgumentException ex) {
            LOG.error("getBulkLoadingGraphInstance: Failed!", ex);
        } catch (AtlasException ex) {
            LOG.error("getBulkLoadingGraphInstance: Failed!", ex);
        }

        return null;
    }

    private static org.apache.commons.configuration2.Configuration createConfiguration2(Configuration conf) {
        Properties properties = ConfigurationConverter.getProperties(conf);

        return org.apache.commons.configuration2.ConfigurationConverter.getConfiguration(properties);
    }

    public static void unload() {
        synchronized (AtlasJanusGraphDatabase.class) {

            if (graphInstance == null) {
                return;
            }
            graphInstance.tx().commit();
            graphInstance.close();
            graphInstance = null;
        }
    }

    static void validateIndexBackend(Configuration config) {
        String configuredIndexBackend = config.getString(INDEX_BACKEND_CONF);

        JanusGraphManagement managementSystem = getGraphInstance().openManagement();
        String currentIndexBackend = managementSystem.get(INDEX_BACKEND_CONF);
        managementSystem.commit();

        if (!configuredIndexBackend.equals(currentIndexBackend)) {
            throw new RuntimeException("Configured Index Backend " + configuredIndexBackend
                    + " differs from earlier configured Index Backend " + currentIndexBackend + ". Aborting!");
        }

    }

    @Override
    public boolean isGraphLoaded() {
        return graphInstance != null;
    }

    @Override
    public void initializeTestGraph() {
        //nothing to do

    }

    @Override
    public void cleanup() {
        JanusGraph g = getGraphInstance();
        try {
            if(g != null) {
                g.close();
            }
        } catch (Throwable t) {
            LOG.warn("Could not close test JanusGraph", t);
            t.printStackTrace();
        }

        try {
            if(g != null) {
                JanusGraphFactory.drop(g);
            }
        } catch (Throwable t) {
            LOG.warn("Could not clear test JanusGraph", t);
            t.printStackTrace();
        }

        if (isEmbeddedSolr()) {
            try {
                stopEmbeddedSolr();
            } catch (Throwable t) {
                LOG.warn("Could not stop local solr server", t);
            }
        }
    }

    @Override
    public AtlasGraph<AtlasJanusVertex, AtlasJanusEdge> getGraph() {
        getGraphInstance();
        return atlasGraphInstance;
    }

    @Override
    public AtlasGraph<AtlasJanusVertex, AtlasJanusEdge> getGraphBulkLoading() {
        return new AtlasJanusGraph(getBulkLoadingGraphInstance());
    }

    private static void startEmbeddedSolr() throws AtlasException {
        LOG.info("==> startEmbeddedSolr()");

        try {
            Class<?> localSolrRunnerClz = Class.forName("org.apache.atlas.runner.LocalSolrRunner");
            Method   startMethod        = localSolrRunnerClz.getMethod("start");

            startMethod.invoke(null);
        } catch (Exception excp) {
            LOG.error("startEmbeddedSolr(): failed", excp);

            throw new AtlasException("startEmbeddedSolr(): failed", excp);
        }

        LOG.info("<== startEmbeddedSolr()");
    }

    private static void stopEmbeddedSolr() throws AtlasException {
        LOG.info("==> stopEmbeddedSolr()");

        try {
            Class<?> localSolrRunnerClz = Class.forName("org.apache.atlas.runner.LocalSolrRunner");
            Method   stopMethod         = localSolrRunnerClz.getMethod("stop");

            stopMethod.invoke(null);
        } catch (Exception excp) {
            LOG.error("stopEmbeddedSolr(): failed", excp);

            throw new AtlasException("stopEmbeddedSolr(): failed", excp);
        }

        LOG.info("<== stopEmbeddedSolr()");
    }

    public static boolean isEmbeddedSolr() {
        boolean ret = false;

        try {
            Configuration conf     = ApplicationProperties.get();
            Object        property = conf.getProperty("atlas.graph.index.search.solr.embedded");

            if (property != null && property instanceof String) {
                ret = Boolean.valueOf((String) property);
            }
        } catch (AtlasException ignored) { }

        return ret;
    }
}
