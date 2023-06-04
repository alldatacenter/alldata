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

package org.apache.inlong.tubemq.server.master.metamanage.metastore.impl.zkimpl;

import java.net.BindException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import org.apache.inlong.tubemq.corebase.TBaseConstants;
import org.apache.inlong.tubemq.corebase.TokenConstants;
import org.apache.inlong.tubemq.corebase.cluster.NodeAddrInfo;
import org.apache.inlong.tubemq.corebase.utils.Tuple2;
import org.apache.inlong.tubemq.server.broker.stats.BrokerSrvStatsHolder;
import org.apache.inlong.tubemq.server.common.zookeeper.ZKUtil;
import org.apache.inlong.tubemq.server.common.zookeeper.ZooKeeperWatcher;
import org.apache.inlong.tubemq.server.master.MasterConfig;
import org.apache.inlong.tubemq.server.master.bdbstore.MasterGroupStatus;
import org.apache.inlong.tubemq.server.master.metamanage.metastore.impl.AbsMetaConfigMapperImpl;
import org.apache.inlong.tubemq.server.master.utils.MetaConfigSamplePrint;
import org.apache.inlong.tubemq.server.master.web.model.ClusterGroupVO;
import org.apache.inlong.tubemq.server.master.web.model.ClusterNodeVO;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ZKMetaConfigMapperImpl extends AbsMetaConfigMapperImpl {

    private static final Logger logger =
            LoggerFactory.getLogger(ZKMetaConfigMapperImpl.class);
    private final MetaConfigSamplePrint metaSamplePrint =
            new MetaConfigSamplePrint(logger);
    // the meta data path in ZooKeeper
    private final String metaZkRoot;
    // the ha path in ZooKeeper
    private final String haParentPath;
    // the ha path in ZooKeeper
    private final String haNodesPath;

    // whether is the first start
    private volatile boolean isFirstChk = true;
    // the ZooKeeper watcher
    private ZooKeeperWatcher zkWatcher;
    // the local node address
    private final NodeAddrInfo localNodeAdd;
    private final ScheduledExecutorService executorService;

    static {
        if (Thread.getDefaultUncaughtExceptionHandler() == null) {
            Thread.setDefaultUncaughtExceptionHandler((t, e) -> {
                if (e instanceof BindException) {
                    logger.error("[ZK Impl] Bind failed.", e);
                    // System.exit(1);
                }
                if (e instanceof IllegalStateException
                        && e.getMessage().contains("Shutdown in progress")) {
                    return;
                }
                logger.warn("[ZK Impl] Thread terminated with exception: " + t.getName(), e);
            });
        }
    }

    /**
     * Constructor of ZKMetaConfigMapperImpl.
     * @param masterConfig
     */
    public ZKMetaConfigMapperImpl(MasterConfig masterConfig) {
        super(masterConfig);
        this.localNodeAdd = new NodeAddrInfo(
                masterConfig.getHostName(), masterConfig.getPort());
        String tubeZkRoot = ZKUtil.normalizePath(masterConfig.getZkMetaConfig().getZkNodeRoot());
        StringBuilder strBuff =
                new StringBuilder(TBaseConstants.BUILDER_DEFAULT_SIZE);
        this.metaZkRoot = strBuff.append(tubeZkRoot).append(TokenConstants.SLASH)
                .append(TZKNodeKeys.ZK_BRANCH_META_DATA).toString();
        strBuff.delete(0, strBuff.length());
        this.haParentPath = strBuff.append(tubeZkRoot).append(TokenConstants.SLASH)
                .append(TZKNodeKeys.ZK_BRANCH_MASTER_HA).toString();
        strBuff.delete(0, strBuff.length());
        this.haNodesPath = strBuff.append(this.haParentPath).append(TokenConstants.SLASH)
                .append(TZKNodeKeys.ZK_LEAF_MASTER_HA_NODEID).append(TokenConstants.ATTR_SEP).toString();
        strBuff.delete(0, strBuff.length());
        try {
            this.zkWatcher = new ZooKeeperWatcher(masterConfig.getZkMetaConfig());
        } catch (Throwable e) {
            BrokerSrvStatsHolder.incZKExcCnt();
            logger.error(strBuff.append("[ZK Impl] Failed to connect ZooKeeper server (")
                    .append(masterConfig.getZkMetaConfig().getZkServerAddr())
                    .append(") !").toString(), e);
            System.exit(1);
        }
        initMetaStore(strBuff);
        this.executorService =
                Executors.newSingleThreadScheduledExecutor(new ThreadFactory() {

                    @Override
                    public Thread newThread(Runnable r) {
                        return new Thread(r, "Master selector thread");
                    }
                });
    }

    @Override
    public void start() throws Exception {
        logger.info("[ZK Impl] Start MetaConfigService, begin");
        if (!srvStatus.compareAndSet(0, 1)) {
            logger.info("[ZK Impl] Start MetaConfigService, started");
            return;
        }
        try {
            logger.info("[ZK Impl] Starting MetaConfigService...");
            // start Master select thread
            executorService.scheduleWithFixedDelay(new MasterSelectorTask(), 5L,
                    masterConfig.getZkMetaConfig().getZkMasterCheckPeriodMs(), TimeUnit.MILLISECONDS);
            // sleep 1 second for select
            Thread.sleep(1000);
            srvStatus.compareAndSet(1, 2);
        } catch (Throwable ee) {
            srvStatus.compareAndSet(1, 0);
            logger.error("[ZK Impl] Start MetaConfigService failure, error", ee);
            return;
        }
        logger.info("[ZK Impl] Start MetaConfigService, success");
    }

    @Override
    public void stop() throws Exception {
        logger.info("[ZK Impl] Stop MetaConfigService, begin");
        if (!srvStatus.compareAndSet(2, 3)) {
            logger.info("[ZK Impl] Stop MetaConfigService, stopped");
            return;
        }
        logger.info("[ZK Impl] Stopping MetaConfigService...");
        // close Master select thread
        executorService.shutdownNow();
        // close ZooKeeper watcher
        zkWatcher.close();
        // clear configure
        closeMetaStore();
        // set status
        srvStatus.set(0);
        logger.info("[ZK Impl] Stop MetaConfigService, success");
    }

    @Override
    public boolean isMasterNow() {
        return isMaster;
    }

    @Override
    public long getMasterSinceTime() {
        return masterSinceTime.get();
    }

    @Override
    public String getMasterAddress() {
        Tuple2<Boolean, Long> queryResult;
        Map<Long, String> clusterNodeMap = new HashMap<>();
        try {
            queryResult = getCusterNodes(clusterNodeMap);
        } catch (Throwable e) {
            logger.error("[ZK Impl] Get Master Address Throwable error", e);
            return null;
        }
        if (!clusterNodeMap.isEmpty()) {
            return clusterNodeMap.get(queryResult.getF1()).split(TokenConstants.ATTR_SEP)[0];
        }
        return null;
    }

    @Override
    public boolean isPrimaryNodeActive() {
        return false;
    }

    @Override
    public void transferMaster() {
        // ignore
    }

    @Override
    public ClusterGroupVO getGroupAddressStrInfo() {
        ClusterGroupVO clusterGroupVO = new ClusterGroupVO();
        clusterGroupVO.setGroupStatus("Abnormal");
        clusterGroupVO.setGroupName("ZooKeeper HA Cluster");
        Tuple2<Boolean, Long> queryResult;
        Map<Long, String> clusterNodeMap = new HashMap<>();
        try {
            queryResult = getCusterNodes(clusterNodeMap);
        } catch (Throwable e) {
            logger.error("[ZK Impl] Get Master Address Throwable error", e);
            return clusterGroupVO;
        }
        if (clusterNodeMap.isEmpty()) {
            return clusterGroupVO;
        }
        String nodeAdd;
        List<ClusterNodeVO> clusterNodeVOs = new ArrayList<>();
        for (Map.Entry<Long, String> entry : clusterNodeMap.entrySet()) {
            nodeAdd = entry.getValue();
            clusterNodeVOs.add(new ClusterNodeVO(nodeAdd,
                    nodeAdd.split(TokenConstants.ATTR_SEP)[0],
                    Integer.parseInt(nodeAdd.split(TokenConstants.ATTR_SEP)[1]),
                    entry.getKey().equals(queryResult.getF1()) ? "Master" : "Slave", 0));
        }
        if (clusterNodeMap.isEmpty()) {
            return clusterGroupVO;
        }
        clusterGroupVO.setNodeData(clusterNodeVOs);
        clusterGroupVO.setGroupStatus("Running-ReadWrite");
        return clusterGroupVO;
    }

    @Override
    public MasterGroupStatus getMasterGroupStatus(boolean isFromHeartbeat) {
        return null;
    }

    protected void initMetaStore(StringBuilder strBuff) {
        clusterConfigMapper = new ZKClusterConfigMapperImpl(metaZkRoot, zkWatcher, strBuff);
        brokerConfigMapper = new ZKBrokerConfigMapperImpl(metaZkRoot, zkWatcher, strBuff);
        topicDeployMapper = new ZKTopicDeployMapperImpl(metaZkRoot, zkWatcher, strBuff);
        groupResCtrlMapper = new ZKGroupResCtrlMapperImpl(metaZkRoot, zkWatcher, strBuff);
        topicCtrlMapper = new ZKTopicCtrlMapperImpl(metaZkRoot, zkWatcher, strBuff);
        consumeCtrlMapper = new ZKConsumeCtrlMapperImpl(metaZkRoot, zkWatcher, strBuff);
    }

    /**
     * Master selector logic
     */
    private class MasterSelectorTask implements Runnable {

        @Override
        public void run() {
            long startTime = System.currentTimeMillis();
            StringBuilder strBuff = new StringBuilder(TBaseConstants.BUILDER_DEFAULT_SIZE);
            try {
                if (isFirstChk) {
                    // check whether the HA directory already exists on ZK
                    if (ZKUtil.checkExists(zkWatcher, haParentPath) == -1) {
                        // create path if not exists
                        ZKUtil.createWithParents(zkWatcher, haParentPath);
                    } else {
                        isFirstChk = false;
                    }
                }
                int totalCnt = 0;
                Tuple2<Boolean, Long> queryResult;
                Map<Long, String> clusterNodeMap = new HashMap<>();
                do {
                    queryResult = getCusterNodes(clusterNodeMap);
                    if (!queryResult.getF0()) {
                        ZKUtil.createEphemeralNodeAndWatch(zkWatcher, haNodesPath,
                                localNodeAdd.getHostPortStr().getBytes(),
                                CreateMode.EPHEMERAL_SEQUENTIAL);
                    }
                    if (totalCnt++ > 1) {
                        break;
                    }
                } while (clusterNodeMap.isEmpty());
                // judge whether the current master node is the current node
                if (!clusterNodeMap.isEmpty()) {
                    String masterAdd = clusterNodeMap.get(queryResult.getF1());
                    if (localNodeAdd.getHostPortStr().equals(masterAdd)) {
                        if (!isMaster) {
                            isMaster = true;
                            reloadMetaStore(strBuff);
                            masterSinceTime.set(System.currentTimeMillis());
                            logger.warn(strBuff.append("[ZK Impl] HA switched, ")
                                    .append(localNodeAdd.getHostPortStr())
                                    .append(" has changed to Master role.").toString());
                            strBuff.delete(0, strBuff.length());
                        }
                    } else {
                        if (isMaster) {
                            isMaster = false;
                            closeMetaStore();
                            logger.warn(strBuff.append("[ZK Impl] HA switched, ")
                                    .append(localNodeAdd.getHostPortStr())
                                    .append(" has changed to Slave role.").toString());
                            strBuff.delete(0, strBuff.length());
                        }
                    }
                }
            } catch (KeeperException e) {
                metaSamplePrint.printExceptionCaught(e,
                        masterConfig.getHostName(), masterConfig.getHostName());
                if (isMaster) {
                    isMaster = false;
                    closeMetaStore();
                    logger.warn(strBuff.append("[ZK Impl] HA select exception, ")
                            .append(localNodeAdd.getHostPortStr())
                            .append(" has changed to Slave role.").toString());
                    strBuff.delete(0, strBuff.length());
                }
            }
            // log the case which delta time over 30 seconds
            if (System.currentTimeMillis() - startTime > 30000) {
                logger.warn(strBuff.append("[ZK Impl] HA Select cost:")
                        .append(System.currentTimeMillis() - startTime)
                        .append("ms, please make sure the time cost is below 30seconds(zk session timeout).")
                        .toString());
                strBuff.delete(0, strBuff.length());
            }
        }
    }

    private Tuple2<Boolean, Long> getCusterNodes(
            Map<Long, String> clusterNodeMap) throws KeeperException {
        String nodeAdd;
        long materNodeId;
        boolean foundSelf = false;
        long minNodeId = Long.MAX_VALUE;
        List<ZKUtil.NodeAndData> childNodes =
                ZKUtil.getChildDataAndWatchForNewChildren(zkWatcher, haParentPath);
        for (ZKUtil.NodeAndData child : childNodes) {
            // select the first registered node as Master
            if (child == null) {
                continue;
            }
            nodeAdd = new String(child.getData());
            materNodeId = Long.parseLong(child.getNode().split(TokenConstants.ATTR_SEP)[1]);
            if (minNodeId > materNodeId) {
                minNodeId = materNodeId;
            }
            if (localNodeAdd.getHostPortStr().equals(nodeAdd)) {
                foundSelf = true;
            }
            clusterNodeMap.put(materNodeId, nodeAdd);
        }
        return new Tuple2<>(foundSelf, minNodeId);
    }

}
