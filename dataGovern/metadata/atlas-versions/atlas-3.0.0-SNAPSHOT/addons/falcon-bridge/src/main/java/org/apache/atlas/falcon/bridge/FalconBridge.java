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

package org.apache.atlas.falcon.bridge;

import org.apache.atlas.AtlasClient;
import org.apache.atlas.AtlasConstants;
import org.apache.atlas.falcon.Util.EventUtil;
import org.apache.atlas.falcon.model.FalconDataTypes;
import org.apache.atlas.hive.bridge.HiveMetaStoreBridge;
import org.apache.atlas.hive.model.HiveDataTypes;
import org.apache.atlas.v1.model.instance.Referenceable;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.falcon.FalconException;
import org.apache.falcon.entity.CatalogStorage;
import org.apache.falcon.entity.FeedHelper;
import org.apache.falcon.entity.FileSystemStorage;
import org.apache.falcon.entity.ProcessHelper;
import org.apache.falcon.entity.store.ConfigurationStore;
import org.apache.falcon.entity.v0.EntityType;
import org.apache.falcon.entity.v0.feed.CatalogTable;
import org.apache.falcon.entity.v0.feed.ClusterType;
import org.apache.falcon.entity.v0.feed.Feed;
import org.apache.falcon.entity.v0.feed.Location;
import org.apache.falcon.entity.v0.feed.LocationType;
import org.apache.falcon.entity.v0.process.Cluster;
import org.apache.falcon.entity.v0.process.Input;
import org.apache.falcon.entity.v0.process.Output;
import org.apache.falcon.entity.v0.process.Workflow;
import org.apache.falcon.workflow.WorkflowExecutionArgs;
import org.apache.hadoop.fs.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A Bridge Utility to register Falcon entities metadata to Atlas.
 */
public class FalconBridge {
    private static final Logger LOG = LoggerFactory.getLogger(FalconBridge.class);

    public static final String COLO = "colo";
    public static final String TAGS = "tags";
    public static final String GROUPS = "groups";
    public static final String PIPELINES = "pipelines";
    public static final String WFPROPERTIES = "workflow-properties";
    public static final String RUNSON = "runs-on";
    public static final String STOREDIN = "stored-in";
    public static final String FREQUENCY = "frequency";
    public static final String ATTRIBUTE_DB = "db";

    /**
     * Creates cluster entity
     *
     * @param cluster ClusterEntity
     * @return cluster instance reference
     */
    public static Referenceable createClusterEntity(final org.apache.falcon.entity.v0.cluster.Cluster cluster) {
        LOG.info("Creating cluster Entity : {}", cluster.getName());

        Referenceable clusterRef = new Referenceable(FalconDataTypes.FALCON_CLUSTER.getName());

        clusterRef.set(AtlasClient.NAME, cluster.getName());
        clusterRef.set(AtlasClient.DESCRIPTION, cluster.getDescription());
        clusterRef.set(AtlasClient.REFERENCEABLE_ATTRIBUTE_NAME, cluster.getName());

        clusterRef.set(FalconBridge.COLO, cluster.getColo());

        if (cluster.getACL() != null) {
            clusterRef.set(AtlasClient.OWNER, cluster.getACL().getGroup());
        }

        if (StringUtils.isNotEmpty(cluster.getTags())) {
            clusterRef.set(FalconBridge.TAGS,
                    EventUtil.convertKeyValueStringToMap(cluster.getTags()));
        }

        return clusterRef;
    }

    private static Referenceable createFeedEntity(Feed feed, Referenceable clusterReferenceable) {
        LOG.info("Creating feed dataset: {}", feed.getName());

        Referenceable feedEntity = new Referenceable(FalconDataTypes.FALCON_FEED.getName());
        feedEntity.set(AtlasClient.NAME, feed.getName());
        feedEntity.set(AtlasClient.DESCRIPTION, feed.getDescription());
        String feedQualifiedName =
                getFeedQualifiedName(feed.getName(), (String) clusterReferenceable.get(AtlasClient.NAME));
        feedEntity.set(AtlasClient.REFERENCEABLE_ATTRIBUTE_NAME, feedQualifiedName);
        feedEntity.set(FalconBridge.FREQUENCY, feed.getFrequency().toString());
        feedEntity.set(FalconBridge.STOREDIN, clusterReferenceable);
        if (feed.getACL() != null) {
            feedEntity.set(AtlasClient.OWNER, feed.getACL().getOwner());
        }

        if (StringUtils.isNotEmpty(feed.getTags())) {
            feedEntity.set(FalconBridge.TAGS,
                    EventUtil.convertKeyValueStringToMap(feed.getTags()));
        }

        if (feed.getGroups() != null) {
            feedEntity.set(FalconBridge.GROUPS, feed.getGroups());
        }

        return feedEntity;
    }

    public static List<Referenceable> createFeedCreationEntity(Feed feed, ConfigurationStore falconStore) throws FalconException, URISyntaxException {
        LOG.info("Creating feed : {}", feed.getName());

        List<Referenceable> entities = new ArrayList<>();

        if (feed.getClusters() != null) {
            List<Referenceable> replicationInputs = new ArrayList<>();
            List<Referenceable> replicationOutputs = new ArrayList<>();

            for (org.apache.falcon.entity.v0.feed.Cluster feedCluster : feed.getClusters().getClusters()) {
                org.apache.falcon.entity.v0.cluster.Cluster cluster = falconStore.get(EntityType.CLUSTER,
                        feedCluster.getName());

                // set cluster
                Referenceable clusterReferenceable = getClusterEntityReference(cluster.getName(), cluster.getColo());
                entities.add(clusterReferenceable);

                // input as hive_table or hdfs_path, output as falcon_feed dataset
                List<Referenceable> inputs = new ArrayList<>();
                List<Referenceable> inputReferenceables = getInputEntities(cluster, feed);
                if (inputReferenceables != null) {
                    entities.addAll(inputReferenceables);
                    inputs.add(inputReferenceables.get(inputReferenceables.size() - 1));
                }

                List<Referenceable> outputs = new ArrayList<>();
                Referenceable feedEntity = createFeedEntity(feed, clusterReferenceable);
                if (feedEntity != null) {
                    entities.add(feedEntity);
                    outputs.add(feedEntity);
                }

                if (!inputs.isEmpty() || !outputs.isEmpty()) {
                    Referenceable feedCreateEntity = new Referenceable(FalconDataTypes.FALCON_FEED_CREATION.getName());
                    String feedQualifiedName = getFeedQualifiedName(feed.getName(), cluster.getName());

                    feedCreateEntity.set(AtlasClient.NAME, feed.getName());
                    feedCreateEntity.set(AtlasClient.DESCRIPTION, "Feed creation - " + feed.getName());
                    feedCreateEntity.set(AtlasClient.REFERENCEABLE_ATTRIBUTE_NAME, feedQualifiedName);

                    if (!inputs.isEmpty()) {
                        feedCreateEntity.set(AtlasClient.PROCESS_ATTRIBUTE_INPUTS, inputs);
                    }
                    if (!outputs.isEmpty()) {
                        feedCreateEntity.set(AtlasClient.PROCESS_ATTRIBUTE_OUTPUTS, outputs);
                    }

                    feedCreateEntity.set(FalconBridge.STOREDIN, clusterReferenceable);
                    entities.add(feedCreateEntity);
                }

                if (ClusterType.SOURCE == feedCluster.getType()) {
                    replicationInputs.add(feedEntity);
                } else if (ClusterType.TARGET == feedCluster.getType()) {
                    replicationOutputs.add(feedEntity);
                }
            }

            if (!replicationInputs.isEmpty() && !replicationInputs.isEmpty()) {
                Referenceable feedReplicationEntity = new Referenceable(FalconDataTypes
                        .FALCON_FEED_REPLICATION.getName());

                feedReplicationEntity.set(AtlasClient.NAME, feed.getName());
                feedReplicationEntity.set(AtlasClient.REFERENCEABLE_ATTRIBUTE_NAME, feed.getName());

                feedReplicationEntity.set(AtlasClient.PROCESS_ATTRIBUTE_INPUTS, replicationInputs);
                feedReplicationEntity.set(AtlasClient.PROCESS_ATTRIBUTE_OUTPUTS, replicationOutputs);
                entities.add(feedReplicationEntity);
            }

        }
        return entities;
    }

    /**
     * Creates process entity
     * 
     * @param process process entity
     * @param falconStore config store
     * @return process instance reference
     *
     * @throws FalconException if retrieving from the configuration store fail
     */
    public static List<Referenceable> createProcessEntity(org.apache.falcon.entity.v0.process.Process process,
                                                          ConfigurationStore falconStore) throws FalconException {
        LOG.info("Creating process Entity : {}", process.getName());

        // The requirement is for each cluster, create a process entity with name
        // clustername.processname
        List<Referenceable> entities = new ArrayList<>();

        if (process.getClusters() != null) {

            for (Cluster processCluster : process.getClusters().getClusters()) {
                org.apache.falcon.entity.v0.cluster.Cluster cluster =
                        falconStore.get(EntityType.CLUSTER, processCluster.getName());
                Referenceable clusterReferenceable = getClusterEntityReference(cluster.getName(), cluster.getColo());
                entities.add(clusterReferenceable);

                List<Referenceable> inputs = new ArrayList<>();
                if (process.getInputs() != null) {
                    for (Input input : process.getInputs().getInputs()) {
                        Feed feed = falconStore.get(EntityType.FEED, input.getFeed());
                        Referenceable inputReferenceable = getFeedDataSetReference(feed, clusterReferenceable);
                        entities.add(inputReferenceable);
                        inputs.add(inputReferenceable);
                    }
                }

                List<Referenceable> outputs = new ArrayList<>();
                if (process.getOutputs() != null) {
                    for (Output output : process.getOutputs().getOutputs()) {
                        Feed feed = falconStore.get(EntityType.FEED, output.getFeed());
                        Referenceable outputReferenceable = getFeedDataSetReference(feed, clusterReferenceable);
                        entities.add(outputReferenceable);
                        outputs.add(outputReferenceable);
                    }
                }

                if (!inputs.isEmpty() || !outputs.isEmpty()) {

                    Referenceable processEntity = new Referenceable(FalconDataTypes.FALCON_PROCESS.getName());
                    processEntity.set(AtlasClient.NAME, process.getName());
                    processEntity.set(AtlasClient.REFERENCEABLE_ATTRIBUTE_NAME,
                            getProcessQualifiedName(process.getName(), cluster.getName()));
                    processEntity.set(FalconBridge.FREQUENCY, process.getFrequency().toString());

                    if (!inputs.isEmpty()) {
                        processEntity.set(AtlasClient.PROCESS_ATTRIBUTE_INPUTS, inputs);
                    }
                    if (!outputs.isEmpty()) {
                        processEntity.set(AtlasClient.PROCESS_ATTRIBUTE_OUTPUTS, outputs);
                    }

                    // set cluster
                    processEntity.set(FalconBridge.RUNSON, clusterReferenceable);

                    // Set user
                    if (process.getACL() != null) {
                        processEntity.set(AtlasClient.OWNER, process.getACL().getOwner());
                    }

                    if (StringUtils.isNotEmpty(process.getTags())) {
                        processEntity.set(FalconBridge.TAGS,
                                EventUtil.convertKeyValueStringToMap(process.getTags()));
                    }

                    if (process.getPipelines() != null) {
                        processEntity.set(FalconBridge.PIPELINES, process.getPipelines());
                    }

                    processEntity.set(FalconBridge.WFPROPERTIES,
                            getProcessEntityWFProperties(process.getWorkflow(),
                                    process.getName()));

                    entities.add(processEntity);
                }

            }
        }
        return entities;
    }

    private static List<Referenceable> getInputEntities(org.apache.falcon.entity.v0.cluster.Cluster cluster,
                                                        Feed feed) throws URISyntaxException {
        org.apache.falcon.entity.v0.feed.Cluster feedCluster = FeedHelper.getCluster(feed, cluster.getName());

        if(feedCluster != null) {
            final CatalogTable table = getTable(feedCluster, feed);
            if (table != null) {
                CatalogStorage storage = new CatalogStorage(cluster, table);
                return createHiveTableInstance(cluster.getName(), storage.getDatabase().toLowerCase(),
                        storage.getTable().toLowerCase());
            } else {
                List<Location> locations = FeedHelper.getLocations(feedCluster, feed);
                if (CollectionUtils.isNotEmpty(locations)) {
                    Location dataLocation = FileSystemStorage.getLocation(locations, LocationType.DATA);
                    if (dataLocation != null) {
                        final String pathUri = normalize(dataLocation.getPath());
                        LOG.info("Registering DFS Path {} ", pathUri);
                        return fillHDFSDataSet(pathUri, cluster.getName());
                    }
                }
            }
        }

        return null;
    }

    private static CatalogTable getTable(org.apache.falcon.entity.v0.feed.Cluster cluster, Feed feed) {
        // check if table is overridden in cluster
        if (cluster.getTable() != null) {
            return cluster.getTable();
        }

        return feed.getTable();
    }

    private static List<Referenceable> fillHDFSDataSet(final String pathUri, final String clusterName) {
        List<Referenceable> entities = new ArrayList<>();
        Referenceable ref = new Referenceable(HiveMetaStoreBridge.HDFS_PATH);
        ref.set("path", pathUri);
        //        Path path = new Path(pathUri);
        //        ref.set("name", path.getName());
        //TODO - Fix after ATLAS-542 to shorter Name
        Path path = new Path(pathUri);
        ref.set(AtlasClient.NAME, Path.getPathWithoutSchemeAndAuthority(path).toString().toLowerCase());
        ref.set(AtlasClient.REFERENCEABLE_ATTRIBUTE_NAME, pathUri);
        ref.set(AtlasConstants.CLUSTER_NAME_ATTRIBUTE, clusterName);
        entities.add(ref);
        return entities;
    }

    private static Referenceable createHiveDatabaseInstance(String clusterName, String dbName) {
        Referenceable dbRef = new Referenceable(HiveDataTypes.HIVE_DB.getName());
        dbRef.set(AtlasConstants.CLUSTER_NAME_ATTRIBUTE, clusterName);
        dbRef.set(AtlasClient.NAME, dbName);
        dbRef.set(AtlasClient.REFERENCEABLE_ATTRIBUTE_NAME,
                HiveMetaStoreBridge.getDBQualifiedName(clusterName, dbName));
        return dbRef;
    }

    private static List<Referenceable> createHiveTableInstance(String clusterName, String dbName,
                                                               String tableName) {
        List<Referenceable> entities = new ArrayList<>();
        Referenceable dbRef = createHiveDatabaseInstance(clusterName, dbName);
        entities.add(dbRef);

        Referenceable tableRef = new Referenceable(HiveDataTypes.HIVE_TABLE.getName());
        tableRef.set(AtlasClient.REFERENCEABLE_ATTRIBUTE_NAME,
                HiveMetaStoreBridge.getTableQualifiedName(clusterName, dbName, tableName));
        tableRef.set(AtlasClient.NAME, tableName.toLowerCase());
        tableRef.set(ATTRIBUTE_DB, dbRef);
        entities.add(tableRef);

        return entities;
    }

    private static Referenceable getClusterEntityReference(final String clusterName,
                                                           final String colo) {
        LOG.info("Getting reference for entity {}", clusterName);
        Referenceable clusterRef = new Referenceable(FalconDataTypes.FALCON_CLUSTER.getName());
        clusterRef.set(AtlasClient.NAME, String.format("%s", clusterName));
        clusterRef.set(AtlasClient.REFERENCEABLE_ATTRIBUTE_NAME, clusterName);
        clusterRef.set(FalconBridge.COLO, colo);
        return clusterRef;
    }


    private static Referenceable getFeedDataSetReference(Feed feed, Referenceable clusterReference) {
        LOG.info("Getting reference for entity {}", feed.getName());
        Referenceable feedDatasetRef = new Referenceable(FalconDataTypes.FALCON_FEED.getName());
        feedDatasetRef.set(AtlasClient.NAME, feed.getName());
        feedDatasetRef.set(AtlasClient.REFERENCEABLE_ATTRIBUTE_NAME, getFeedQualifiedName(feed.getName(),
                (String) clusterReference.get(AtlasClient.REFERENCEABLE_ATTRIBUTE_NAME)));
        feedDatasetRef.set(FalconBridge.STOREDIN, clusterReference);
        feedDatasetRef.set(FalconBridge.FREQUENCY, feed.getFrequency());
        return feedDatasetRef;
    }

    private static Map<String, String> getProcessEntityWFProperties(final Workflow workflow,
                                                                    final String processName) {
        Map<String, String> wfProperties = new HashMap<>();
        wfProperties.put(WorkflowExecutionArgs.USER_WORKFLOW_NAME.getName(),
                ProcessHelper.getProcessWorkflowName(workflow.getName(), processName));
        wfProperties.put(WorkflowExecutionArgs.USER_WORKFLOW_VERSION.getName(),
                workflow.getVersion());
        wfProperties.put(WorkflowExecutionArgs.USER_WORKFLOW_ENGINE.getName(),
                workflow.getEngine().value());

        return wfProperties;
    }

    public static String getFeedQualifiedName(final String feedName, final String clusterName) {
        return String.format("%s@%s", feedName, clusterName);
    }

    public static String getProcessQualifiedName(final String processName, final String clusterName) {
        return String.format("%s@%s", processName, clusterName);
    }

    public static String normalize(final String str) {
        if (StringUtils.isBlank(str)) {
            return null;
        }
        return str.toLowerCase().trim();
    }
}
