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

package org.apache.atlas.falcon.hook;

import com.sun.jersey.api.client.ClientResponse;
import org.apache.atlas.ApplicationProperties;
import org.apache.atlas.AtlasClient;
import org.apache.atlas.AtlasServiceException;
import org.apache.atlas.falcon.bridge.FalconBridge;
import org.apache.atlas.falcon.model.FalconDataTypes;
import org.apache.atlas.hive.bridge.HiveMetaStoreBridge;
import org.apache.atlas.hive.model.HiveDataTypes;
import org.apache.atlas.v1.model.instance.Id;
import org.apache.atlas.v1.model.instance.Referenceable;
import org.apache.atlas.v1.typesystem.types.utils.TypesUtil;
import org.apache.atlas.utils.AuthenticationUtil;
import org.apache.atlas.utils.ParamChecker;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.atlas.falcon.service.AtlasService;
import org.apache.falcon.entity.FeedHelper;
import org.apache.falcon.entity.FileSystemStorage;
import org.apache.falcon.entity.store.ConfigurationStore;
import org.apache.falcon.entity.v0.Entity;
import org.apache.falcon.entity.v0.EntityType;
import org.apache.falcon.entity.v0.cluster.Cluster;
import org.apache.falcon.entity.v0.feed.Feed;
import org.apache.falcon.entity.v0.feed.Location;
import org.apache.falcon.entity.v0.feed.LocationType;
import org.apache.falcon.entity.v0.process.Process;
import org.apache.falcon.security.CurrentUser;
import org.slf4j.Logger;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import javax.xml.bind.JAXBException;
import java.util.List;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.fail;

public class FalconHookIT {
    public static final Logger LOG = org.slf4j.LoggerFactory.getLogger(FalconHookIT.class);

    public static final String CLUSTER_RESOURCE = "/cluster.xml";
    public static final String FEED_RESOURCE = "/feed.xml";
    public static final String FEED_HDFS_RESOURCE = "/feed-hdfs.xml";
    public static final String FEED_REPLICATION_RESOURCE = "/feed-replication.xml";
    public static final String PROCESS_RESOURCE = "/process.xml";

    private AtlasClient atlasClient;

    private static final ConfigurationStore STORE = ConfigurationStore.get();

    @BeforeClass
    public void setUp() throws Exception {
        Configuration atlasProperties = ApplicationProperties.get();
        if (!AuthenticationUtil.isKerberosAuthenticationEnabled()) {
            atlasClient = new AtlasClient(atlasProperties.getStringArray(HiveMetaStoreBridge.ATLAS_ENDPOINT), new String[]{"admin", "admin"});
        } else {
            atlasClient = new AtlasClient(atlasProperties.getStringArray(HiveMetaStoreBridge.ATLAS_ENDPOINT));
        }

        AtlasService service = new AtlasService();
        service.init();
        STORE.registerListener(service);
        CurrentUser.authenticate(System.getProperty("user.name"));
    }

    private boolean isDataModelAlreadyRegistered() throws Exception {
        try {
            atlasClient.getType(FalconDataTypes.FALCON_PROCESS.getName());
            LOG.info("Hive data model is already registered!");
            return true;
        } catch(AtlasServiceException ase) {
            if (ase.getStatus() == ClientResponse.Status.NOT_FOUND) {
                return false;
            }
            throw ase;
        }
    }

    private <T extends Entity> T loadEntity(EntityType type, String resource, String name) throws JAXBException {
        Entity entity = (Entity) type.getUnmarshaller().unmarshal(this.getClass().getResourceAsStream(resource));
        switch (entity.getEntityType()) {
            case CLUSTER:
                ((Cluster) entity).setName(name);
                break;

            case FEED:
                ((Feed) entity).setName(name);
                break;

            case PROCESS:
                ((Process) entity).setName(name);
                break;
        }
        return (T)entity;
    }

    private String random() {
        return RandomStringUtils.randomAlphanumeric(10);
    }

    private String getTableUri(String dbName, String tableName) {
        return String.format("catalog:%s:%s#ds=${YEAR}-${MONTH}-${DAY}-${HOUR}", dbName, tableName);
    }

    @Test
    public void testCreateProcess() throws Exception {
        Cluster cluster = loadEntity(EntityType.CLUSTER, CLUSTER_RESOURCE, "cluster" + random());
        STORE.publish(EntityType.CLUSTER, cluster);
        assertClusterIsRegistered(cluster);

        Feed infeed = getTableFeed(FEED_RESOURCE, cluster.getName(), null);
        String infeedId = atlasClient.getEntity(FalconDataTypes.FALCON_FEED.getName(), AtlasClient.REFERENCEABLE_ATTRIBUTE_NAME,
                FalconBridge.getFeedQualifiedName(infeed.getName(), cluster.getName())).getId()._getId();

        Feed outfeed = getTableFeed(FEED_RESOURCE, cluster.getName());
        String outFeedId = atlasClient.getEntity(FalconDataTypes.FALCON_FEED.getName(), AtlasClient.REFERENCEABLE_ATTRIBUTE_NAME,
                FalconBridge.getFeedQualifiedName(outfeed.getName(), cluster.getName())).getId()._getId();

        Process process = loadEntity(EntityType.PROCESS, PROCESS_RESOURCE, "process" + random());
        process.getClusters().getClusters().get(0).setName(cluster.getName());
        process.getInputs().getInputs().get(0).setFeed(infeed.getName());
        process.getOutputs().getOutputs().get(0).setFeed(outfeed.getName());
        STORE.publish(EntityType.PROCESS, process);

        String pid = assertProcessIsRegistered(process, cluster.getName());
        Referenceable processEntity = atlasClient.getEntity(pid);
        assertNotNull(processEntity);
        assertEquals(processEntity.get(AtlasClient.NAME), process.getName());
        assertEquals(((List<Id>)processEntity.get("inputs")).get(0)._getId(), infeedId);
        assertEquals(((List<Id>)processEntity.get("outputs")).get(0)._getId(), outFeedId);
    }

    private String assertProcessIsRegistered(Process process, String clusterName) throws Exception {
        return assertEntityIsRegistered(FalconDataTypes.FALCON_PROCESS.getName(),
                AtlasClient.REFERENCEABLE_ATTRIBUTE_NAME,
                FalconBridge.getProcessQualifiedName(process.getName(), clusterName));
    }

    private String assertClusterIsRegistered(Cluster cluster) throws Exception {
        return assertEntityIsRegistered(FalconDataTypes.FALCON_CLUSTER.getName(),
                AtlasClient.REFERENCEABLE_ATTRIBUTE_NAME, cluster.getName());
    }

    private TypesUtil.Pair<String, Feed> getHDFSFeed(String feedResource, String clusterName) throws Exception {
        Feed feed = loadEntity(EntityType.FEED, feedResource, "feed" + random());
        org.apache.falcon.entity.v0.feed.Cluster feedCluster = feed.getClusters().getClusters().get(0);
        feedCluster.setName(clusterName);
        STORE.publish(EntityType.FEED, feed);
        String feedId = assertFeedIsRegistered(feed, clusterName);
        assertFeedAttributes(feedId);

        String processId = assertEntityIsRegistered(FalconDataTypes.FALCON_FEED_CREATION.getName(),
                AtlasClient.REFERENCEABLE_ATTRIBUTE_NAME,
                FalconBridge.getFeedQualifiedName(feed.getName(), clusterName));
        Referenceable processEntity = atlasClient.getEntity(processId);
        assertEquals(((List<Id>)processEntity.get("outputs")).get(0).getId(), feedId);

        String inputId = ((List<Id>) processEntity.get("inputs")).get(0).getId();
        Referenceable pathEntity = atlasClient.getEntity(inputId);
        assertEquals(pathEntity.getTypeName(), HiveMetaStoreBridge.HDFS_PATH);

        List<Location> locations = FeedHelper.getLocations(feedCluster, feed);
        Location dataLocation = FileSystemStorage.getLocation(locations, LocationType.DATA);
        assertEquals(pathEntity.get(AtlasClient.REFERENCEABLE_ATTRIBUTE_NAME),
                FalconBridge.normalize(dataLocation.getPath()));

        return TypesUtil.Pair.of(feedId, feed);
    }

    private Feed getTableFeed(String feedResource, String clusterName) throws Exception {
        return getTableFeed(feedResource, clusterName, null);
    }

    private Feed getTableFeed(String feedResource, String clusterName, String secondClusterName) throws Exception {
        Feed feed = loadEntity(EntityType.FEED, feedResource, "feed" + random());
        org.apache.falcon.entity.v0.feed.Cluster feedCluster = feed.getClusters().getClusters().get(0);
        feedCluster.setName(clusterName);
        String dbName = "db" + random();
        String tableName = "table" + random();
        feedCluster.getTable().setUri(getTableUri(dbName, tableName));

        String dbName2 = "db" + random();
        String tableName2 = "table" + random();

        if (secondClusterName != null) {
            org.apache.falcon.entity.v0.feed.Cluster feedCluster2 = feed.getClusters().getClusters().get(1);
            feedCluster2.setName(secondClusterName);
            feedCluster2.getTable().setUri(getTableUri(dbName2, tableName2));
        }

        STORE.publish(EntityType.FEED, feed);
        String feedId = assertFeedIsRegistered(feed, clusterName);
        assertFeedAttributes(feedId);
        verifyFeedLineage(feed.getName(), clusterName, feedId, dbName, tableName);

        if (secondClusterName != null) {
            String feedId2 = assertFeedIsRegistered(feed, secondClusterName);
            assertFeedAttributes(feedId2);
            verifyFeedLineage(feed.getName(), secondClusterName, feedId2, dbName2, tableName2);
        }
        return feed;
    }

    private void assertFeedAttributes(String feedId) throws Exception {
        Referenceable feedEntity = atlasClient.getEntity(feedId);
        assertEquals(feedEntity.get(AtlasClient.OWNER), "testuser");
        assertEquals(feedEntity.get(FalconBridge.FREQUENCY), "hours(1)");
        assertEquals(feedEntity.get(AtlasClient.DESCRIPTION), "test input");
    }

    private void verifyFeedLineage(String feedName, String clusterName, String feedId, String dbName, String tableName)
            throws Exception{
        //verify that lineage from hive table to falcon feed is created
        String processId = assertEntityIsRegistered(FalconDataTypes.FALCON_FEED_CREATION.getName(),
                AtlasClient.REFERENCEABLE_ATTRIBUTE_NAME,
                FalconBridge.getFeedQualifiedName(feedName, clusterName));
        Referenceable processEntity = atlasClient.getEntity(processId);
        assertEquals(((List<Id>)processEntity.get("outputs")).get(0).getId(), feedId);

        String inputId = ((List<Id>) processEntity.get("inputs")).get(0).getId();
        Referenceable tableEntity = atlasClient.getEntity(inputId);
        assertEquals(tableEntity.getTypeName(), HiveDataTypes.HIVE_TABLE.getName());
        assertEquals(tableEntity.get(AtlasClient.REFERENCEABLE_ATTRIBUTE_NAME),
                HiveMetaStoreBridge.getTableQualifiedName(clusterName, dbName, tableName));

    }

    private String assertFeedIsRegistered(Feed feed, String clusterName) throws Exception {
        return assertEntityIsRegistered(FalconDataTypes.FALCON_FEED.getName(), AtlasClient.REFERENCEABLE_ATTRIBUTE_NAME,
                FalconBridge.getFeedQualifiedName(feed.getName(), clusterName));
    }

    @Test
    public void testReplicationFeed() throws Exception {
        Cluster srcCluster = loadEntity(EntityType.CLUSTER, CLUSTER_RESOURCE, "cluster" + random());
        STORE.publish(EntityType.CLUSTER, srcCluster);
        assertClusterIsRegistered(srcCluster);

        Cluster targetCluster = loadEntity(EntityType.CLUSTER, CLUSTER_RESOURCE, "cluster" + random());
        STORE.publish(EntityType.CLUSTER, targetCluster);
        assertClusterIsRegistered(targetCluster);

        Feed feed = getTableFeed(FEED_REPLICATION_RESOURCE, srcCluster.getName(), targetCluster.getName());
        String inId = atlasClient.getEntity(FalconDataTypes.FALCON_FEED.getName(), AtlasClient.REFERENCEABLE_ATTRIBUTE_NAME,
                FalconBridge.getFeedQualifiedName(feed.getName(), srcCluster.getName())).getId()._getId();
        String outId = atlasClient.getEntity(FalconDataTypes.FALCON_FEED.getName(), AtlasClient.REFERENCEABLE_ATTRIBUTE_NAME,
                FalconBridge.getFeedQualifiedName(feed.getName(), targetCluster.getName())).getId()._getId();


        String processId = assertEntityIsRegistered(FalconDataTypes.FALCON_FEED_REPLICATION.getName(),
                AtlasClient.REFERENCEABLE_ATTRIBUTE_NAME, feed.getName());
        Referenceable process = atlasClient.getEntity(processId);
        assertEquals(((List<Id>)process.get("inputs")).get(0)._getId(), inId);
        assertEquals(((List<Id>)process.get("outputs")).get(0)._getId(), outId);
    }

    @Test
    public void testCreateProcessWithHDFSFeed() throws Exception {
        Cluster cluster = loadEntity(EntityType.CLUSTER, CLUSTER_RESOURCE, "cluster" + random());
        STORE.publish(EntityType.CLUSTER, cluster);

        TypesUtil.Pair<String, Feed> result = getHDFSFeed(FEED_HDFS_RESOURCE, cluster.getName());
        Feed infeed = result.right;
        String infeedId = result.left;

        Feed outfeed = getTableFeed(FEED_RESOURCE, cluster.getName());
        String outfeedId = atlasClient.getEntity(FalconDataTypes.FALCON_FEED.getName(), AtlasClient.REFERENCEABLE_ATTRIBUTE_NAME,
                FalconBridge.getFeedQualifiedName(outfeed.getName(), cluster.getName())).getId()._getId();

        Process process = loadEntity(EntityType.PROCESS, PROCESS_RESOURCE, "process" + random());
        process.getClusters().getClusters().get(0).setName(cluster.getName());
        process.getInputs().getInputs().get(0).setFeed(infeed.getName());
        process.getOutputs().getOutputs().get(0).setFeed(outfeed.getName());
        STORE.publish(EntityType.PROCESS, process);

        String pid = assertProcessIsRegistered(process, cluster.getName());
        Referenceable processEntity = atlasClient.getEntity(pid);
        assertEquals(processEntity.get(AtlasClient.NAME), process.getName());
        assertEquals(processEntity.get(AtlasClient.REFERENCEABLE_ATTRIBUTE_NAME),
                FalconBridge.getProcessQualifiedName(process.getName(), cluster.getName()));
        assertEquals(((List<Id>)processEntity.get("inputs")).get(0)._getId(), infeedId);
        assertEquals(((List<Id>)processEntity.get("outputs")).get(0)._getId(), outfeedId);
    }

    private String assertEntityIsRegistered(final String typeName, final String property, final String value) throws Exception {
        waitFor(80000, new Predicate() {
            @Override
            public void evaluate() throws Exception {
                Referenceable entity = atlasClient.getEntity(typeName, property, value);
                assertNotNull(entity);
            }
        });
        Referenceable entity = atlasClient.getEntity(typeName, property, value);
        return entity.getId()._getId();
    }

    public interface Predicate {
        /**
         * Perform a predicate evaluation.
         *
         * @return the boolean result of the evaluation.
         * @throws Exception thrown if the predicate evaluation could not evaluate.
         */
        void evaluate() throws Exception;
    }

    /**
     * Wait for a condition, expressed via a {@link Predicate} to become true.
     *
     * @param timeout maximum time in milliseconds to wait for the predicate to become true.
     * @param predicate predicate waiting on.
     */
    protected void waitFor(int timeout, Predicate predicate) throws Exception {
        ParamChecker.notNull(predicate, "predicate");
        long mustEnd = System.currentTimeMillis() + timeout;

        while (true) {
            try {
                predicate.evaluate();
                return;
            } catch(Error | Exception e) {
                if (System.currentTimeMillis() >= mustEnd) {
                    fail("Assertions failed. Failing after waiting for timeout " + timeout + " msecs", e);
                }
                LOG.debug("Waiting up to {} msec as assertion failed", mustEnd - System.currentTimeMillis(), e);
                Thread.sleep(400);
            }
        }
    }
}
