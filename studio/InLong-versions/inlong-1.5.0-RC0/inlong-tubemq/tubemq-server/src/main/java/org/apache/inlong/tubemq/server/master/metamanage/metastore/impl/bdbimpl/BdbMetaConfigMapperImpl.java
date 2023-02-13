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

package org.apache.inlong.tubemq.server.master.metamanage.metastore.impl.bdbimpl;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import com.sleepycat.je.DatabaseException;
import com.sleepycat.je.Durability;
import com.sleepycat.je.EnvironmentConfig;
import com.sleepycat.je.EnvironmentFailureException;
import com.sleepycat.je.rep.InsufficientLogException;
import com.sleepycat.je.rep.NetworkRestore;
import com.sleepycat.je.rep.NetworkRestoreConfig;
import com.sleepycat.je.rep.NodeState;
import com.sleepycat.je.rep.ReplicatedEnvironment;
import com.sleepycat.je.rep.ReplicationConfig;
import com.sleepycat.je.rep.ReplicationGroup;
import com.sleepycat.je.rep.ReplicationMutableConfig;
import com.sleepycat.je.rep.ReplicationNode;
import com.sleepycat.je.rep.StateChangeEvent;
import com.sleepycat.je.rep.StateChangeListener;
import com.sleepycat.je.rep.TimeConsistencyPolicy;
import com.sleepycat.je.rep.UnknownMasterException;
import com.sleepycat.je.rep.util.ReplicationGroupAdmin;
import com.sleepycat.je.rep.utilint.ServiceDispatcher;
import com.sleepycat.persist.StoreConfig;
import org.apache.inlong.tubemq.corebase.TBaseConstants;
import org.apache.inlong.tubemq.corebase.TokenConstants;
import org.apache.inlong.tubemq.corebase.utils.TStringUtils;
import org.apache.inlong.tubemq.corebase.utils.Tuple2;
import org.apache.inlong.tubemq.server.common.fileconfig.BdbMetaConfig;
import org.apache.inlong.tubemq.server.master.MasterConfig;
import org.apache.inlong.tubemq.server.master.bdbstore.MasterGroupStatus;
import org.apache.inlong.tubemq.server.master.bdbstore.MasterNodeInfo;
import org.apache.inlong.tubemq.server.master.metamanage.metastore.impl.AbsMetaConfigMapperImpl;
import org.apache.inlong.tubemq.server.master.utils.MetaConfigSamplePrint;
import org.apache.inlong.tubemq.server.master.web.model.ClusterGroupVO;
import org.apache.inlong.tubemq.server.master.web.model.ClusterNodeVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BdbMetaConfigMapperImpl extends AbsMetaConfigMapperImpl {

    private static final int REP_HANDLE_RETRY_MAX = 1;
    protected static final Logger logger =
            LoggerFactory.getLogger(BdbMetaConfigMapperImpl.class);
    private final MetaConfigSamplePrint metaSamplePrint =
            new MetaConfigSamplePrint(logger);
    // bdb meta store configure
    private final BdbMetaConfig bdbMetaConfig;
    // bdb environment configure
    private final EnvironmentConfig envConfig;
    // meta data store file
    private File envHome;
    // bdb replication configure
    private final ReplicationConfig repConfig;
    // bdb replicated environment
    private ReplicatedEnvironment repEnv;
    // bdb replication group admin info
    private final ReplicationGroupAdmin replicationGroupAdmin;
    // master node name
    private String masterNodeName;
    // node connect failure count
    private int connectNodeFailCount = 0;
    // replication nodes
    private Set<String> replicas4Transfer = new HashSet<>();
    private final Listener listener = new Listener();
    // ha check thread
    private ExecutorService executorService = null;
    // bdb data store configure
    private final StoreConfig storeConfig = new StoreConfig();

    /**
     * Constructor of BdbMetaConfigMapperImpl.
     * @param masterConfig
     */
    public BdbMetaConfigMapperImpl(MasterConfig masterConfig) {
        super(masterConfig);
        bdbMetaConfig = masterConfig.getBdbMetaConfig();
        // build replicationGroupAdmin info
        Set<InetSocketAddress> helpers = new HashSet<>();
        for (int i = 1; i <= 3; i++) {
            helpers.add(new InetSocketAddress(this.masterConfig.getHostName(),
                    bdbMetaConfig.getRepNodePort() + i));
        }
        this.replicationGroupAdmin =
                new ReplicationGroupAdmin(bdbMetaConfig.getRepGroupName(), helpers);
        // Initialize configuration for BDB-JE replication environment.
        // Set envHome and generate a ReplicationConfig. Note that ReplicationConfig and
        // EnvironmentConfig values could all be specified in the je.properties file,
        // as is shown in the properties file included in the example.
        this.repConfig = new ReplicationConfig();
        // Set consistency policy for replica.
        this.repConfig.setConsistencyPolicy(new TimeConsistencyPolicy(3,
                TimeUnit.SECONDS, 3, TimeUnit.SECONDS));
        // Wait up to 3 seconds for commitConsumed acknowledgments.
        this.repConfig.setReplicaAckTimeout(3, TimeUnit.SECONDS);
        this.repConfig.setConfigParam(ReplicationConfig.TXN_ROLLBACK_LIMIT, "1000");
        this.repConfig.setGroupName(bdbMetaConfig.getRepGroupName());
        this.repConfig.setNodeName(bdbMetaConfig.getRepNodeName());
        this.repConfig.setNodeHostPort(this.masterConfig.getHostName() + TokenConstants.ATTR_SEP
                + bdbMetaConfig.getRepNodePort());
        if (TStringUtils.isNotEmpty(bdbMetaConfig.getRepHelperHost())) {
            logger.info("[BDB Impl] ADD HELP HOST");
            this.repConfig.setHelperHosts(bdbMetaConfig.getRepHelperHost());
        }
        // A replicated environment must be opened with transactions enabled.
        // Environments on a master must be read/write, while environments
        // on a client can be read/write or read/only. Since the master's
        // identity may change, it's most convenient to open the environment in the default
        // read/write mode. All write operations will be refused on the client though.
        this.envConfig = new EnvironmentConfig();
        this.envConfig.setTransactional(true);
        this.envConfig.setDurability(new Durability(
                bdbMetaConfig.getMetaLocalSyncPolicy(),
                bdbMetaConfig.getMetaReplicaSyncPolicy(),
                bdbMetaConfig.getRepReplicaAckPolicy()));
        this.envConfig.setAllowCreate(true);
        // Set transactional for the replicated environment.
        this.storeConfig.setTransactional(true);
        // Set both Master and Replica open the store for write.
        this.storeConfig.setReadOnly(false);
        this.storeConfig.setAllowCreate(true);
    }

    @Override
    public void start() throws Exception {
        logger.info("[BDB Impl] Start MetaConfigService, begin");
        if (!srvStatus.compareAndSet(0, 1)) {
            logger.info("[BDB Impl] Start MetaConfigService, started");
            return;
        }
        logger.info("[BDB Impl] Starting MetaConfigService...");
        try {
            if (executorService != null) {
                executorService.shutdownNow();
                executorService = null;
            }
            executorService = Executors.newSingleThreadExecutor();
            // build envHome file

            envHome = new File(bdbMetaConfig.getMetaDataPath());
            repEnv = getEnvironment();
            initMetaStore(null);
            repEnv.setStateChangeListener(listener);
            srvStatus.compareAndSet(1, 2);
        } catch (Throwable ee) {
            srvStatus.compareAndSet(1, 0);
            logger.error("[BDB Impl] Start MetaConfigService failure, error", ee);
            return;
        }
        logger.info("[BDB Impl] Start MetaConfigService, success");
    }

    @Override
    public void stop() throws Exception {
        logger.info("[BDB Impl] Stop MetaConfigService, begin");
        if (!srvStatus.compareAndSet(2, 3)) {
            logger.info("[BDB Impl] Stop MetaConfigService, stopped");
            return;
        }
        logger.info("[BDB Impl] Stopping MetaConfigService...");
        // close bdb configure
        closeMetaStore();
        /* evn close */
        if (repEnv != null) {
            try {
                repEnv.close();
                repEnv = null;
            } catch (Throwable ee) {
                logger.error("[BDB Impl] Close repEnv throw error ", ee);
            }
        }
        if (executorService != null) {
            executorService.shutdownNow();
            executorService = null;
        }
        srvStatus.set(0);
        logger.info("[BDB Impl] Stop MetaConfigService, success");
    }

    @Override
    public boolean isMasterNow() {
        return isMaster;
    }

    @Override
    public long getMasterSinceTime() {
        return this.masterSinceTime.get();
    }

    @Override
    public String getMasterAddress() {
        ReplicationGroup replicationGroup = getCurrReplicationGroup();
        if (replicationGroup == null) {
            logger.info("[BDB Impl] ReplicationGroup is null...please check the group status!");
            return null;
        }
        for (ReplicationNode node : replicationGroup.getNodes()) {
            try {
                NodeState nodeState =
                        replicationGroupAdmin.getNodeState(node, 2000);
                if (nodeState != null) {
                    if (nodeState.getNodeState().isMaster()) {
                        return node.getSocketAddress().getAddress().getHostAddress();
                    }
                }
            } catch (Throwable e) {
                logger.error("[BDB Impl] Get nodeState Throwable error", e);
            }
        }
        return null;
    }

    @Override
    public boolean isPrimaryNodeActive() {
        if (repEnv == null) {
            return false;
        }
        ReplicationMutableConfig tmpConfig = repEnv.getRepMutableConfig();
        return tmpConfig != null && tmpConfig.getDesignatedPrimary();
    }

    @Override
    public void transferMaster() throws Exception {
        if (!isServiceStarted()) {
            throw new Exception("The BDB store StoreService is reboot now!");
        }
        if (isMasterNow()) {
            if (!isPrimaryNodeActive()) {
                if ((replicas4Transfer != null) && (!replicas4Transfer.isEmpty())) {
                    logger.info(new StringBuilder(TBaseConstants.BUILDER_DEFAULT_SIZE)
                            .append("[BDB Impl] start transferMaster to replicas: ")
                            .append(replicas4Transfer).toString());
                    repEnv.transferMaster(replicas4Transfer, 5, TimeUnit.MINUTES);
                    logger.info("[BDB Impl] transferMaster end...");
                } else {
                    throw new Exception("The replicate nodes is empty!");
                }
            } else {
                throw new Exception("DesignatedPrimary happened...please check if the other member is down!");
            }
        } else {
            throw new Exception("Please send your request to the master Node!");
        }
    }

    @Override
    public ClusterGroupVO getGroupAddressStrInfo() {
        ClusterGroupVO clusterGroupVO = new ClusterGroupVO();
        clusterGroupVO.setGroupStatus("Abnormal");
        clusterGroupVO.setGroupName(replicationGroupAdmin.getGroupName());
        // query current replication group info
        ReplicationGroup replicationGroup = getCurrReplicationGroup();
        if (replicationGroup == null) {
            return clusterGroupVO;
        }
        // translate replication group info to ClusterGroupVO structure
        Tuple2<Boolean, List<ClusterNodeVO>> transResult =
                transReplicateNodes(replicationGroup);
        clusterGroupVO.setNodeData(transResult.getF1());
        clusterGroupVO.setPrimaryNodeActive(isPrimaryNodeActive());
        if (transResult.getF0()) {
            if (isPrimaryNodeActive()) {
                clusterGroupVO.setGroupStatus("Running-ReadOnly");
            } else {
                clusterGroupVO.setGroupStatus("Running-ReadWrite");
            }
        }
        return clusterGroupVO;
    }

    @Override
    public MasterGroupStatus getMasterGroupStatus(boolean isFromHeartbeat) {
        // #lizard forgives
        if (repEnv == null) {
            return null;
        }
        ReplicationGroup replicationGroup = null;
        try {
            replicationGroup = repEnv.getGroup();
        } catch (DatabaseException e) {
            if (e instanceof EnvironmentFailureException) {
                if (isFromHeartbeat) {
                    logger.error("[BDB Error] Check found EnvironmentFailureException", e);
                    try {
                        stop();
                        start();
                        replicationGroup = repEnv.getGroup();
                    } catch (Throwable e1) {
                        logger.error("[BDB Error] close and reopen storeManager error", e1);
                    }
                } else {
                    logger.error(
                            "[BDB Error] Get EnvironmentFailureException error while non heartBeat request", e);
                }
            } else {
                logger.error("[BDB Error] Get replication group info error", e);
            }
        } catch (Throwable ee) {
            logger.error("[BDB Error] Get replication group throw error", ee);
        }
        if (replicationGroup == null) {
            logger.error(
                    "[BDB Error] ReplicationGroup is null...please check the status of the group!");
            return null;
        }
        int activeNodes = 0;
        boolean isMasterActive = false;
        Set<String> tmp = new HashSet<>();
        for (ReplicationNode node : replicationGroup.getNodes()) {
            MasterNodeInfo masterNodeInfo =
                    new MasterNodeInfo(replicationGroup.getName(),
                            node.getName(), node.getHostName(), node.getPort());
            try {
                NodeState nodeState = replicationGroupAdmin.getNodeState(node, 2000);
                if (nodeState != null) {
                    if (nodeState.getNodeState().isActive()) {
                        activeNodes++;
                        if (nodeState.getNodeName().equals(masterNodeName)) {
                            isMasterActive = true;
                            masterNodeInfo.setNodeStatus(1);
                        }
                    }
                    if (nodeState.getNodeState().isReplica()) {
                        tmp.add(nodeState.getNodeName());
                        replicas4Transfer = tmp;
                        masterNodeInfo.setNodeStatus(0);
                    }
                }
            } catch (IOException e) {
                connectNodeFailCount++;
                masterNodeInfo.setNodeStatus(-1);
                metaSamplePrint.printExceptionCaught(e, node.getHostName(), node.getName());
                continue;
            } catch (ServiceDispatcher.ServiceConnectFailedException e) {
                masterNodeInfo.setNodeStatus(-2);
                metaSamplePrint.printExceptionCaught(e, node.getHostName(), node.getName());
                continue;
            } catch (Throwable ee) {
                masterNodeInfo.setNodeStatus(-3);
                metaSamplePrint.printExceptionCaught(ee, node.getHostName(), node.getName());
                continue;
            }
        }
        MasterGroupStatus masterGroupStatus = new MasterGroupStatus(isMasterActive);
        int groupSize = replicationGroup.getElectableNodes().size();
        int majoritySize = groupSize / 2 + 1;
        if ((activeNodes >= majoritySize) && isMasterActive) {
            masterGroupStatus.setMasterGroupStatus(true, true, true);
            connectNodeFailCount = 0;
            if (isPrimaryNodeActive()) {
                repEnv.setRepMutableConfig(repEnv.getRepMutableConfig().setDesignatedPrimary(false));
            }
        }
        if (groupSize == 2 && connectNodeFailCount >= 3) {
            masterGroupStatus.setMasterGroupStatus(true, false, true);
            if (connectNodeFailCount > 1000) {
                connectNodeFailCount = 3;
            }
            if (!isPrimaryNodeActive()) {
                logger.error("[BDB Error] DesignatedPrimary happened...please check if the other member is down");
                repEnv.setRepMutableConfig(repEnv.getRepMutableConfig().setDesignatedPrimary(true));
            }
        }
        return masterGroupStatus;
    }

    protected void initMetaStore(StringBuilder strBuff) {
        clusterConfigMapper = new BdbClusterConfigMapperImpl(repEnv, storeConfig);
        brokerConfigMapper = new BdbBrokerConfigMapperImpl(repEnv, storeConfig);
        topicDeployMapper = new BdbTopicDeployMapperImpl(repEnv, storeConfig);
        groupResCtrlMapper = new BdbGroupResCtrlMapperImpl(repEnv, storeConfig);
        topicCtrlMapper = new BdbTopicCtrlMapperImpl(repEnv, storeConfig);
        consumeCtrlMapper = new BdbConsumeCtrlMapperImpl(repEnv, storeConfig);
    }

    /**
     * State Change Listener,
     * through this object, it complete the metadata cache cleaning
     * and loading of the latest data.
     *
     * */
    public class Listener implements StateChangeListener {

        @Override
        public void stateChange(StateChangeEvent stateChangeEvent) throws RuntimeException {
            if (repConfig != null) {
                logger.warn(new StringBuilder(TBaseConstants.BUILDER_DEFAULT_SIZE)
                        .append("[BDB Impl][").append(repConfig.getGroupName())
                        .append("Receive a group status changed event]... stateChangeEventTime: ")
                        .append(stateChangeEvent.getEventTime()).toString());
            }
            doWork(stateChangeEvent);
        }

        /**
         * process replicate nodes status event
         *
         * @param stateChangeEvent status change event
         */
        public void doWork(final StateChangeEvent stateChangeEvent) {

            final String currentNode = new StringBuilder(TBaseConstants.BUILDER_DEFAULT_SIZE)
                    .append("GroupName:").append(repConfig.getGroupName())
                    .append(",nodeName:").append(repConfig.getNodeName())
                    .append(",hostName:").append(repConfig.getNodeHostPort()).toString();
            if (executorService == null) {
                logger.error("[BDB Impl] found  executorService is null while doWork!");
                return;
            }
            executorService.submit(() -> {
                StringBuilder sBuilder =
                        new StringBuilder(TBaseConstants.BUILDER_DEFAULT_SIZE);
                switch (stateChangeEvent.getState()) {
                    case MASTER:
                        if (!isMaster) {
                            try {
                                reloadMetaStore(sBuilder);
                                isMaster = true;
                                masterSinceTime.set(System.currentTimeMillis());
                                masterNodeName = stateChangeEvent.getMasterNodeName();
                                logger.info(sBuilder.append("[BDB Impl] ")
                                        .append(currentNode).append(" is a master.").toString());
                            } catch (Throwable e) {
                                isMaster = false;
                                logger.error("[BDB Impl] fatal error when Reloading Info ", e);
                            }
                        }
                        break;
                    case REPLICA:
                        isMaster = false;
                        masterNodeName = stateChangeEvent.getMasterNodeName();
                        logger.info(sBuilder.append("[BDB Impl] ")
                                .append(currentNode).append(" is a slave.").toString());
                        break;
                    default:
                        isMaster = false;
                        logger.info(sBuilder.append("[BDB Impl] ")
                                .append(currentNode).append(" is Unknown state ")
                                .append(stateChangeEvent.getState().name()).toString());
                        break;
                }
                sBuilder.delete(0, sBuilder.length());
            });
        }
    }

    /**
     * Creates the replicated environment handle and returns it. It will retry indefinitely if a
     * master could not be established because a sufficient number of nodes were not available, or
     * there were networking issues, etc.
     *
     * @return the newly created replicated environment handle
     * @throws InterruptedException if the operation was interrupted
     */
    private ReplicatedEnvironment getEnvironment() throws InterruptedException {
        DatabaseException exception = null;
        // In this example we retry REP_HANDLE_RETRY_MAX times, but a production HA application may
        // retry indefinitely.
        for (int i = 0; i < REP_HANDLE_RETRY_MAX; i++) {
            try {
                return new ReplicatedEnvironment(envHome, repConfig, envConfig);
            } catch (UnknownMasterException unknownMaster) {
                exception = unknownMaster;
                // Indicates there is a group level problem: insufficient nodes for an election,
                // network connectivity issues, etc. Wait and retry to allow the problem
                // to be resolved.
                logger.error(new StringBuilder(TBaseConstants.BUILDER_DEFAULT_SIZE)
                        .append("[BDB Impl] master could not be established. ")
                        .append("Exception message:").append(unknownMaster.getMessage())
                        .append(" Will retry after 5 seconds.").toString());
                Thread.sleep(5 * 1000);
                continue;
            } catch (InsufficientLogException insufficientLogEx) {
                logger.error(new StringBuilder(TBaseConstants.BUILDER_DEFAULT_SIZE)
                        .append("[BDB Impl] [Restoring data please wait....] ")
                        .append("Obtains logger files for a Replica from other members of ")
                        .append("the replication group. A Replica may need to do so if it ")
                        .append("has been offline for some time, and has fallen behind in ")
                        .append("its execution of the replication stream.").toString());
                NetworkRestore restore = new NetworkRestore();
                NetworkRestoreConfig config = new NetworkRestoreConfig();
                // delete obsolete logger files.
                config.setRetainLogFiles(false);
                restore.execute(insufficientLogEx, config);
                // retry
                return new ReplicatedEnvironment(envHome, repConfig, envConfig);
            }
        }
        // Failed despite retries.
        throw exception;
    }

    private ReplicationGroup getCurrReplicationGroup() {
        ReplicationGroup replicationGroup;
        try {
            replicationGroup = repEnv.getGroup();
        } catch (Throwable e) {
            logger.error("[BDB Impl] get current master group info error", e);
            return null;
        }
        return replicationGroup;
    }

    /**
     * Query replication group nodes status and translate to ClusterNodeVO type
     *
     * @param replicationGroup  the replication group
     * @return if has master, replication nodes info
     */
    private Tuple2<Boolean, List<ClusterNodeVO>> transReplicateNodes(
            ReplicationGroup replicationGroup) {
        boolean hasMaster = false;
        List<ClusterNodeVO> clusterNodeVOList = new ArrayList<>();
        for (ReplicationNode node : replicationGroup.getNodes()) {
            ClusterNodeVO clusterNodeVO = new ClusterNodeVO();
            clusterNodeVO.setHostName(node.getHostName());
            clusterNodeVO.setNodeName(node.getName());
            clusterNodeVO.setPort(node.getPort());
            try {
                NodeState nodeState =
                        replicationGroupAdmin.getNodeState(node, 2000);
                if (nodeState != null) {
                    if (nodeState.getNodeState() == ReplicatedEnvironment.State.MASTER) {
                        hasMaster = true;
                    }
                    clusterNodeVO.setNodeStatus(nodeState.getNodeState().toString());
                    clusterNodeVO.setJoinTime(nodeState.getJoinTime());
                } else {
                    clusterNodeVO.setNodeStatus("Not-found");
                    clusterNodeVO.setJoinTime(0);
                }
            } catch (IOException e) {
                clusterNodeVO.setNodeStatus("Error");
                clusterNodeVO.setJoinTime(0);
            } catch (ServiceDispatcher.ServiceConnectFailedException e) {
                clusterNodeVO.setNodeStatus("Unconnected");
                clusterNodeVO.setJoinTime(0);
            }
            clusterNodeVOList.add(clusterNodeVO);
        }
        return new Tuple2<>(hasMaster, clusterNodeVOList);
    }
}
