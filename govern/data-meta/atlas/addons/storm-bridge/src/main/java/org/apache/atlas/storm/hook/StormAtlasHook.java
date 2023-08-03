/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.atlas.storm.hook;

import org.apache.atlas.model.instance.AtlasEntity;
import org.apache.atlas.model.instance.AtlasEntity.AtlasEntitiesWithExtInfo;
import org.apache.atlas.model.instance.AtlasEntity.AtlasEntityExtInfo;
import org.apache.atlas.model.notification.HookNotification;
import org.apache.atlas.model.notification.HookNotification.EntityCreateRequestV2;
import org.apache.atlas.type.AtlasTypeUtil;
import org.apache.atlas.utils.HdfsNameServiceResolver;
import org.apache.commons.collections.CollectionUtils;
import org.apache.storm.ISubmitterHook;
import org.apache.storm.generated.Bolt;
import org.apache.storm.generated.SpoutSpec;
import org.apache.storm.generated.StormTopology;
import org.apache.storm.generated.TopologyInfo;
import org.apache.storm.utils.Utils;
import org.apache.atlas.AtlasClient;
import org.apache.atlas.AtlasConstants;
import org.apache.atlas.hive.bridge.HiveMetaStoreBridge;
import org.apache.atlas.hook.AtlasHook;
import org.apache.atlas.storm.model.StormDataTypes;
import org.apache.commons.lang.StringUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hive.conf.HiveConf;
import org.slf4j.Logger;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Date;

import static org.apache.atlas.repository.Constants.STORM_SOURCE;

/**
 * StormAtlasHook sends storm topology metadata information to Atlas
 * via a Kafka Broker for durability.
 * <p/>
 * This is based on the assumption that the same topology name is used
 * for the various lifecycle stages.
 */
public class StormAtlasHook extends AtlasHook implements ISubmitterHook {
    public static final Logger LOG = org.slf4j.LoggerFactory.getLogger(StormAtlasHook.class);

    public  static final String ANONYMOUS_OWNER         = "anonymous"; // if Storm topology does not contain the owner instance; possible if Storm is running in unsecure mode.
    public  static final String HBASE_NAMESPACE_DEFAULT = "default";
    public  static final String ATTRIBUTE_DB            = "db";

    public static final String RELATIONSHIP_STORM_TOPOLOGY_NODES = "storm_topology_nodes";
    public static final String RELATIONSHIP_DATASET_PROCESS_INPUTS = "dataset_process_inputs";
    public static final String RELATIONSHIP_PROCESS_DATASET_OUTPUTS = "process_dataset_outputs";
    public static final String HBASE_TABLE_QUALIFIED_NAME_FORMAT    = "%s:%s@%s";

    /**
     * This is the client-side hook that storm fires when a topology is added.
     *
     * @param topologyInfo topology info
     * @param stormConf configuration
     * @param stormTopology a storm topology
     */
    @Override
    public void notify(TopologyInfo topologyInfo, Map stormConf, StormTopology stormTopology) {
        LOG.info("Collecting metadata for a new storm topology: {}", topologyInfo.get_name());

        try {
            String                   user     = getUser(topologyInfo.get_owner(), null);
            AtlasEntity              topology = createTopologyInstance(topologyInfo, stormConf);
            AtlasEntitiesWithExtInfo entity   = new AtlasEntitiesWithExtInfo(topology);

            addTopologyDataSets(stormTopology, topologyInfo.get_owner(), stormConf, topology, entity);

            // create the graph for the topology
            List<AtlasEntity> graphNodes = createTopologyGraph(stormTopology, stormTopology.get_spouts(), stormTopology.get_bolts());

            if (CollectionUtils.isNotEmpty(graphNodes)) {
                // add the connection from topology to the graph
                topology.setRelationshipAttribute("nodes", AtlasTypeUtil.getAtlasRelatedObjectIds(graphNodes, RELATIONSHIP_STORM_TOPOLOGY_NODES));

                for (AtlasEntity graphNode : graphNodes) {
                    entity.addReferredEntity(graphNode);
                }
            }

            List<HookNotification> hookNotifications = Collections.singletonList(new EntityCreateRequestV2(user, entity));

            notifyEntities(hookNotifications, null);
        } catch (Exception e) {
            throw new RuntimeException("Atlas hook is unable to process the topology.", e);
        }
    }

    private AtlasEntity createTopologyInstance(TopologyInfo topologyInfo, Map stormConf) {
        AtlasEntity topology = new AtlasEntity(StormDataTypes.STORM_TOPOLOGY.getName());
        String      owner    = topologyInfo.get_owner();

        if (StringUtils.isEmpty(owner)) {
            owner = ANONYMOUS_OWNER;
        }

        topology.setAttribute("id", topologyInfo.get_id());
        topology.setAttribute(AtlasClient.NAME, topologyInfo.get_name());
        topology.setAttribute(AtlasClient.REFERENCEABLE_ATTRIBUTE_NAME, topologyInfo.get_name());
        topology.setAttribute(AtlasClient.OWNER, owner);
        topology.setAttribute("startTime", new Date(System.currentTimeMillis()));
        topology.setAttribute(AtlasConstants.CLUSTER_NAME_ATTRIBUTE, getMetadataNamespace());

        return topology;
    }

    private void addTopologyDataSets(StormTopology stormTopology, String topologyOwner, Map stormConf, AtlasEntity topology, AtlasEntityExtInfo entityExtInfo) {
        // add each spout as an input data set
        addTopologyInputs(stormTopology.get_spouts(), stormConf, topologyOwner, topology, entityExtInfo);

        // add the appropriate bolts as output data sets
        addTopologyOutputs(stormTopology, topologyOwner, stormConf, topology, entityExtInfo);
    }

    private void addTopologyInputs(Map<String, SpoutSpec> spouts, Map stormConf, String topologyOwner, AtlasEntity topology, AtlasEntityExtInfo entityExtInfo) {
        List<AtlasEntity> inputs = new ArrayList<>();

        for (Map.Entry<String, SpoutSpec> entry : spouts.entrySet()) {
            Serializable instance = Utils.javaDeserialize(entry.getValue().get_spout_object().get_serialized_java(), Serializable.class);
            String       dsType   = instance.getClass().getSimpleName();
            AtlasEntity  dsEntity = addDataSet(dsType, topologyOwner, instance, stormConf, entityExtInfo);

            if (dsEntity != null) {
                inputs.add(dsEntity);
            }
        }

        topology.setRelationshipAttribute("inputs", AtlasTypeUtil.getAtlasRelatedObjectIds(inputs, RELATIONSHIP_DATASET_PROCESS_INPUTS));
    }

    private void addTopologyOutputs(StormTopology stormTopology, String topologyOwner, Map stormConf, AtlasEntity topology, AtlasEntityExtInfo entityExtInfo) {
        List<AtlasEntity> outputs   = new ArrayList<>();
        Map<String, Bolt> bolts     = stormTopology.get_bolts();
        Set<String>       boltNames = StormTopologyUtil.getTerminalUserBoltNames(stormTopology);

        for (String boltName : boltNames) {
            Serializable instance = Utils.javaDeserialize(bolts.get(boltName).get_bolt_object().get_serialized_java(), Serializable.class);
            String       dsType   = instance.getClass().getSimpleName();
            AtlasEntity  dsEntity = addDataSet(dsType, topologyOwner, instance, stormConf, entityExtInfo);

            if (dsEntity != null) {
                outputs.add(dsEntity);
            }
        }

        topology.setRelationshipAttribute("outputs", AtlasTypeUtil.getAtlasRelatedObjectIds(outputs, RELATIONSHIP_PROCESS_DATASET_OUTPUTS));
    }

    private AtlasEntity addDataSet(String dataSetType, String topologyOwner, Serializable instance, Map stormConf, AtlasEntityExtInfo entityExtInfo) {
        Map<String, String> config            = StormTopologyUtil.getFieldValues(instance, true, null);
        AtlasEntity         ret               = null;
        String              metadataNamespace = getMetadataNamespace();

        // todo: need to redo this with a config driven approach
        switch (dataSetType) {
            case "KafkaSpout": {
                String topicName = config.get("KafkaSpout.kafkaSpoutConfig.translator.topic");
                String uri       = config.get("KafkaSpout.kafkaSpoutConfig.kafkaProps.bootstrap.servers");

                if (StringUtils.isEmpty(topicName)) {
                    topicName = config.get("KafkaSpout._spoutConfig.topic");
                }

                if (StringUtils.isEmpty(uri)) {
                    uri = config.get("KafkaSpout._spoutConfig.hosts.brokerZkStr");
                }

                if (StringUtils.isEmpty(topologyOwner)) {
                    topologyOwner = ANONYMOUS_OWNER;
                }

                if (topicName == null) {
                    LOG.error("Kafka topic name not found");
                } else {
                    ret = new AtlasEntity(StormDataTypes.KAFKA_TOPIC.getName());

                    ret.setAttribute("topic", topicName);
                    ret.setAttribute("uri", uri);
                    ret.setAttribute(AtlasClient.OWNER, topologyOwner);
                    ret.setAttribute(AtlasClient.REFERENCEABLE_ATTRIBUTE_NAME, getKafkaTopicQualifiedName(metadataNamespace, topicName));
                    ret.setAttribute(AtlasClient.NAME, topicName);
                }
            }
            break;

            case "HBaseBolt": {
                final String hbaseTableName = config.get("HBaseBolt.tableName");
                String       uri            = config.get("hbase.rootdir");

                if (StringUtils.isEmpty(uri)) {
                    uri = hbaseTableName;
                }

                metadataNamespace = extractComponentMetadataNamespace(HBaseConfiguration.create(), stormConf);

                if (hbaseTableName == null) {
                    LOG.error("HBase table name not found");
                } else {
                    ret = new AtlasEntity(StormDataTypes.HBASE_TABLE.getName());

                    ret.setAttribute("uri", hbaseTableName);
                    ret.setAttribute(AtlasClient.NAME, uri);
                    ret.setAttribute(AtlasClient.OWNER, stormConf.get("storm.kerberos.principal"));
                    //TODO - Hbase Namespace is hardcoded to 'default'. need to check how to get this or is it already part of tableName
                    ret.setAttribute(AtlasClient.REFERENCEABLE_ATTRIBUTE_NAME, getHbaseTableQualifiedName(metadataNamespace, HBASE_NAMESPACE_DEFAULT, hbaseTableName));
                }
            }
            break;

            case "HdfsBolt": {
                final String hdfsUri       = config.get("HdfsBolt.rotationActions") == null ? config.get("HdfsBolt.fileNameFormat.path") : config.get("HdfsBolt.rotationActions");
                final String hdfsPathStr   = config.get("HdfsBolt.fsUrl") + hdfsUri;
                final Path   hdfsPath      = new Path(hdfsPathStr);
                final String nameServiceID = HdfsNameServiceResolver.getNameServiceIDForPath(hdfsPathStr);

                ret = new AtlasEntity(HiveMetaStoreBridge.HDFS_PATH);

                ret.setAttribute(AtlasConstants.CLUSTER_NAME_ATTRIBUTE, metadataNamespace);
                ret.setAttribute(AtlasClient.OWNER, stormConf.get("hdfs.kerberos.principal"));
                ret.setAttribute(AtlasClient.NAME, Path.getPathWithoutSchemeAndAuthority(hdfsPath).toString().toLowerCase());

                if (StringUtils.isNotEmpty(nameServiceID)) {
                    String updatedPath = HdfsNameServiceResolver.getPathWithNameServiceID(hdfsPathStr);

                    ret.setAttribute("path", updatedPath);
                    ret.setAttribute("nameServiceId", nameServiceID);
                    ret.setAttribute(AtlasClient.REFERENCEABLE_ATTRIBUTE_NAME, getHdfsPathQualifiedName(metadataNamespace, updatedPath));
                } else {
                    ret.setAttribute("path", hdfsPathStr);
                    ret.setAttribute(AtlasClient.REFERENCEABLE_ATTRIBUTE_NAME, getHdfsPathQualifiedName(metadataNamespace, hdfsPathStr));
                }
            }
            break;

            case "HiveBolt": {
                metadataNamespace = extractComponentMetadataNamespace(new HiveConf(), stormConf);

                final String dbName  = config.get("HiveBolt.options.databaseName");
                final String tblName = config.get("HiveBolt.options.tableName");

                if (dbName == null || tblName ==null) {
                    LOG.error("Hive database or table name not found");
                } else {
                    AtlasEntity dbEntity = new AtlasEntity("hive_db");

                    dbEntity.setAttribute(AtlasClient.NAME, dbName);
                    dbEntity.setAttribute(AtlasClient.REFERENCEABLE_ATTRIBUTE_NAME, HiveMetaStoreBridge.getDBQualifiedName(metadataNamespace, dbName));
                    dbEntity.setAttribute(AtlasConstants.CLUSTER_NAME_ATTRIBUTE, metadataNamespace);

                    entityExtInfo.addReferredEntity(dbEntity);

                    // todo: verify if hive table has everything needed to retrieve existing table
                    ret = new AtlasEntity("hive_table");

                    ret.setAttribute(AtlasClient.NAME, tblName);
                    ret.setRelationshipAttribute(ATTRIBUTE_DB, AtlasTypeUtil.getAtlasRelatedObjectId(dbEntity, "hive_table_db"));
                    ret.setAttribute(AtlasClient.REFERENCEABLE_ATTRIBUTE_NAME, HiveMetaStoreBridge.getTableQualifiedName(metadataNamespace, dbName, tblName));
                }
            }
            break;

            default:
                // custom node - create a base dataset class with name attribute
                //TODO - What should we do for custom data sets. Not sure what name we can set here?
                return null;
        }

        if (ret != null) {
            entityExtInfo.addReferredEntity(ret);
        }

        return ret;
    }

    private List<AtlasEntity> createTopologyGraph(StormTopology stormTopology, Map<String, SpoutSpec> spouts, Map<String, Bolt> bolts) {
        // Add graph of nodes in the topology
        Map<String, AtlasEntity> nodeEntities = new HashMap<>();

        addSpouts(spouts, nodeEntities);
        addBolts(bolts, nodeEntities);

        addGraphConnections(stormTopology, nodeEntities);

        return new ArrayList<>(nodeEntities.values());
    }

    private void addSpouts(Map<String, SpoutSpec> spouts, Map<String, AtlasEntity> nodeEntities) {
        for (Map.Entry<String, SpoutSpec> entry : spouts.entrySet()) {
            String      spoutName = entry.getKey();
            AtlasEntity spout     = createSpoutInstance(spoutName, entry.getValue());

            nodeEntities.put(spoutName, spout);
        }
    }

    private void addBolts(Map<String, Bolt> bolts, Map<String, AtlasEntity> nodeEntities) {
        for (Map.Entry<String, Bolt> entry : bolts.entrySet()) {
            String      boltName     = entry.getKey();
            AtlasEntity boltInstance = createBoltInstance(boltName, entry.getValue());

            nodeEntities.put(boltName, boltInstance);
        }
    }

    private AtlasEntity createSpoutInstance(String spoutName, SpoutSpec stormSpout) {
        AtlasEntity         spout         = new AtlasEntity(StormDataTypes.STORM_SPOUT.getName());
        Serializable        instance      = Utils.javaDeserialize(stormSpout.get_spout_object().get_serialized_java(), Serializable.class);
        Map<String, String> flatConfigMap = StormTopologyUtil.getFieldValues(instance, true, null);

        spout.setAttribute(AtlasClient.NAME, spoutName);
        spout.setAttribute("driverClass", instance.getClass().getName());
        spout.setAttribute("conf", flatConfigMap);

        return spout;
    }

    private AtlasEntity createBoltInstance(String boltName, Bolt stormBolt) {
        AtlasEntity         bolt          = new AtlasEntity(StormDataTypes.STORM_BOLT.getName());
        Serializable        instance      = Utils.javaDeserialize(stormBolt.get_bolt_object().get_serialized_java(), Serializable.class);
        Map<String, String> flatConfigMap = StormTopologyUtil.getFieldValues(instance, true, null);

        bolt.setAttribute(AtlasClient.NAME, boltName);
        bolt.setAttribute("driverClass", instance.getClass().getName());
        bolt.setAttribute("conf", flatConfigMap);

        return bolt;
    }

    private void addGraphConnections(StormTopology stormTopology, Map<String, AtlasEntity> nodeEntities) {
        // adds connections between spouts and bolts
        Map<String, Set<String>> adjacencyMap = StormTopologyUtil.getAdjacencyMap(stormTopology, true);

        for (Map.Entry<String, Set<String>> entry : adjacencyMap.entrySet()) {
            String      nodeName      = entry.getKey();
            Set<String> adjacencyList = adjacencyMap.get(nodeName);

            if (CollectionUtils.isEmpty(adjacencyList)) {
                continue;
            }

            // add outgoing links
            AtlasEntity  node    = nodeEntities.get(nodeName);
            List<String> outputs = new ArrayList<>(adjacencyList.size());

            outputs.addAll(adjacencyList);
            node.setAttribute("outputs", outputs);

            // add incoming links
            for (String adjacentNodeName : adjacencyList) {
                AtlasEntity adjacentNode = nodeEntities.get(adjacentNodeName);
                @SuppressWarnings("unchecked")
                List<String> inputs = (List<String>) adjacentNode.getAttribute("inputs");

                if (inputs == null) {
                    inputs = new ArrayList<>();
                }

                inputs.add(nodeName);
                adjacentNode.setAttribute("inputs", inputs);
            }
        }
    }

    public static String getKafkaTopicQualifiedName(String metadataNamespace, String topicName) {
        return String.format("%s@%s", topicName.toLowerCase(), metadataNamespace);
    }

    public static String getHbaseTableQualifiedName(String metadataNamespace, String nameSpace, String tableName) {
        return String.format(HBASE_TABLE_QUALIFIED_NAME_FORMAT, nameSpace.toLowerCase(), tableName.toLowerCase(), metadataNamespace);
    }

    public static String getHdfsPathQualifiedName(String metadataNamespace, String hdfsPath) {
        return String.format("%s@%s", hdfsPath.toLowerCase(), metadataNamespace);
    }

    private String extractComponentMetadataNamespace(Configuration configuration, Map stormConf) {
        String clusterName = configuration.get(CLUSTER_NAME_KEY, null);

        if (clusterName == null) {
            clusterName = getMetadataNamespace();
        }

        return clusterName;
    }

    @Override
    public String getMessageSource() {
        return STORM_SOURCE;
    }
}