/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.inlong.tubemq.server.master;

import com.sleepycat.je.Durability;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import org.apache.inlong.tubemq.server.common.fileconfig.BdbMetaConfig;
import org.apache.inlong.tubemq.server.common.fileconfig.ZKMetaConfig;
import org.junit.Assert;
import org.junit.Test;

public class MasterConfigTest {

    @Test
    public void loadFileSectAttributes() {

    }

    @Test
    public void testMetaBdbConfig() throws Exception {
        final MasterConfig masterConfig = new MasterConfig();
        Path configUrl = Paths.get(Objects.requireNonNull(
                getClass().getClassLoader().getResource("master-meta-bdb.ini")).toURI());
        masterConfig.loadFromFile(configUrl.toString());
        Assert.assertEquals(masterConfig.getMetaDataPath(), "var/meta_data");
        Assert.assertTrue(masterConfig.isUseBdbStoreMetaData());
        final BdbMetaConfig repConfig = masterConfig.getBdbMetaConfig();
        Assert.assertEquals("tubemqMasterGroup", repConfig.getRepGroupName());
        Assert.assertEquals("tubemqMasterGroupNode1", repConfig.getRepNodeName());
        Assert.assertEquals(9001, repConfig.getRepNodePort());
        Assert.assertEquals("127.0.0.1:9001", repConfig.getRepHelperHost());
        Assert.assertEquals(Durability.SyncPolicy.SYNC, repConfig.getMetaLocalSyncPolicy());
        Assert.assertEquals(Durability.SyncPolicy.WRITE_NO_SYNC, repConfig.getMetaReplicaSyncPolicy());
        Assert.assertEquals(Durability.ReplicaAckPolicy.SIMPLE_MAJORITY, repConfig.getRepReplicaAckPolicy());
        Assert.assertEquals(10000, repConfig.getRepStatusCheckTimeoutMs());
    }

    @Test
    public void testMetaZooKeeperConfig() throws Exception {
        final MasterConfig masterConfig = new MasterConfig();
        Path configUrl = Paths.get(Objects.requireNonNull(
                getClass().getClassLoader().getResource("master-meta-zk.ini")).toURI());
        masterConfig.loadFromFile(configUrl.toString());
        Assert.assertFalse(masterConfig.isUseBdbStoreMetaData());
        final ZKMetaConfig zkMetaConfig = masterConfig.getZkMetaConfig();
        Assert.assertEquals("/tubemq", zkMetaConfig.getZkNodeRoot());
        Assert.assertEquals("localhost:2181", zkMetaConfig.getZkServerAddr());
        Assert.assertEquals(30000, zkMetaConfig.getZkSessionTimeoutMs());
        Assert.assertEquals(30000, zkMetaConfig.getZkConnectionTimeoutMs());
        Assert.assertEquals(5000, zkMetaConfig.getZkSyncTimeMs());
        Assert.assertEquals(5000, zkMetaConfig.getZkCommitPeriodMs());
        Assert.assertEquals(4000, zkMetaConfig.getZkMasterCheckPeriodMs());
    }

    @Test
    public void testNormalConfig() throws Exception {
        final MasterConfig masterConfig = new MasterConfig();
        Path configUrl = Paths.get(Objects.requireNonNull(
                getClass().getClassLoader().getResource("master-normal.ini")).toURI());
        masterConfig.loadFromFile(configUrl.toString());
        Assert.assertEquals("127.0.0.1", masterConfig.getHostName());
        Assert.assertEquals(8000, masterConfig.getPort());
        Assert.assertEquals(8080, masterConfig.getWebPort());
        Assert.assertEquals(30000, masterConfig.getConsumerBalancePeriodMs());
        Assert.assertEquals(60000, masterConfig.getFirstBalanceDelayAfterStartMs());
        Assert.assertEquals(30000, masterConfig.getConsumerBalancePeriodMs());
        Assert.assertEquals(45000, masterConfig.getProducerHeartbeatTimeoutMs());
        Assert.assertEquals(25000, masterConfig.getBrokerHeartbeatTimeoutMs());
        Assert.assertEquals("abc", masterConfig.getConfModAuthToken());
        Assert.assertEquals("resources", masterConfig.getWebResourcePath());
        Assert.assertEquals("var/meta_data_1", masterConfig.getMetaDataPath());

        final BdbMetaConfig repConfig = masterConfig.getBdbMetaConfig();
        Assert.assertEquals("gp1", repConfig.getRepGroupName());
        Assert.assertEquals("tubemqMasterGroupNode1", repConfig.getRepNodeName());
        Assert.assertEquals(9999, repConfig.getRepNodePort());
        Assert.assertEquals("127.0.0.1:9999", repConfig.getRepHelperHost());
        Assert.assertEquals(Durability.SyncPolicy.WRITE_NO_SYNC, repConfig.getMetaLocalSyncPolicy());
        Assert.assertEquals(Durability.SyncPolicy.SYNC, repConfig.getMetaReplicaSyncPolicy());
        Assert.assertEquals(Durability.ReplicaAckPolicy.ALL, repConfig.getRepReplicaAckPolicy());
        Assert.assertEquals(15000, repConfig.getRepStatusCheckTimeoutMs());
    }

    @Test
    public void testOptionalReplicationConfig() throws Exception {
        final MasterConfig masterConfig = new MasterConfig();
        Path configUrl = Paths.get(Objects.requireNonNull(
                getClass().getClassLoader().getResource("master-replication-optional.ini")).toURI());
        masterConfig.loadFromFile(configUrl.toString());
        Assert.assertEquals(masterConfig.getMetaDataPath(), "var/meta_data");
        final BdbMetaConfig repConfig = masterConfig.getBdbMetaConfig();
        Assert.assertEquals("tubemqMasterGroup", repConfig.getRepGroupName());
        Assert.assertEquals("tubemqMasterGroupNode1", repConfig.getRepNodeName());
        Assert.assertEquals(9001, repConfig.getRepNodePort());
        Assert.assertEquals("127.0.0.1:9001", repConfig.getRepHelperHost());
        Assert.assertEquals(Durability.SyncPolicy.SYNC, repConfig.getMetaLocalSyncPolicy());
        Assert.assertEquals(Durability.SyncPolicy.WRITE_NO_SYNC, repConfig.getMetaReplicaSyncPolicy());
        Assert.assertEquals(Durability.ReplicaAckPolicy.SIMPLE_MAJORITY, repConfig.getRepReplicaAckPolicy());
        Assert.assertEquals(10000, repConfig.getRepStatusCheckTimeoutMs());
    }

    @Test
    public void testReplicationConfigBackwardCompatibility() throws Exception {
        final MasterConfig masterConfig = new MasterConfig();
        Path configUrl = Paths.get(Objects.requireNonNull(
                getClass().getClassLoader().getResource("master-replication-compatibility.ini")).toURI());
        masterConfig.loadFromFile(configUrl.toString());
        Assert.assertEquals("var/tubemqMasterGroup/master_data", masterConfig.getMetaDataPath());
        final BdbMetaConfig repConfig = masterConfig.getBdbMetaConfig();
        Assert.assertEquals("gp1", repConfig.getRepGroupName());
        Assert.assertEquals("tubemqMasterGroupNode1", repConfig.getRepNodeName());
        Assert.assertEquals(9999, repConfig.getRepNodePort());
        Assert.assertEquals("127.0.0.1:9999", repConfig.getRepHelperHost());
        Assert.assertEquals(Durability.SyncPolicy.WRITE_NO_SYNC, repConfig.getMetaLocalSyncPolicy());
        Assert.assertEquals(Durability.SyncPolicy.SYNC, repConfig.getMetaReplicaSyncPolicy());
        Assert.assertEquals(Durability.ReplicaAckPolicy.ALL, repConfig.getRepReplicaAckPolicy());
        Assert.assertEquals(15000, repConfig.getRepStatusCheckTimeoutMs());
    }
}
