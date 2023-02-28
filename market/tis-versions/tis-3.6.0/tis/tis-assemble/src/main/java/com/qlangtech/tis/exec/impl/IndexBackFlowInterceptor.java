///**
// *   Licensed to the Apache Software Foundation (ASF) under one
// *   or more contributor license agreements.  See the NOTICE file
// *   distributed with this work for additional information
// *   regarding copyright ownership.  The ASF licenses this file
// *   to you under the Apache License, Version 2.0 (the
// *   "License"); you may not use this file except in compliance
// *   with the License.  You may obtain a copy of the License at
// *
// *       http://www.apache.org/licenses/LICENSE-2.0
// *
// *   Unless required by applicable law or agreed to in writing, software
// *   distributed under the License is distributed on an "AS IS" BASIS,
// *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// *   See the License for the specific language governing permissions and
// *   limitations under the License.
// */
//package com.qlangtech.tis.exec.impl;
//
//import com.qlangtech.tis.assemble.FullbuildPhase;
//import com.qlangtech.tis.cloud.ITISCoordinator;
//import com.qlangtech.tis.exec.ExecuteResult;
//import com.qlangtech.tis.exec.IExecChainContext;
//import com.qlangtech.tis.fullbuild.jmx.IRemoteIncrControl;
//import com.qlangtech.tis.fullbuild.jmx.impl.DefaultRemoteIncrControl;
//import com.qlangtech.tis.order.center.IndexBackflowManager;
//import com.qlangtech.tis.trigger.jst.AbstractIndexBuildJob.BuildResult;
//import com.qlangtech.tis.trigger.jst.ImportDataProcessInfo;
//import org.apache.zookeeper.KeeperException;
//import org.apache.zookeeper.data.Stat;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//
//import javax.management.remote.JMXConnector;
//import java.io.IOException;
//import java.util.ArrayList;
//import java.util.Collections;
//import java.util.List;
//import java.util.Set;
//
///**
// * 索引回流处理流程
// *
// * @author 百岁（baisui@qlangtech.com）
// * @date 2015年12月15日 下午5:26:57
// */
//public class IndexBackFlowInterceptor extends TrackableExecuteInterceptor {
//
//    private static final int INCR_NODE_PORT = 9998;
//
//    private static final Logger logger = LoggerFactory.getLogger(IndexBackFlowInterceptor.class);
//
//    public static final String NAME = "indexBackflow";
//
//    private IRemoteIncrControl remoteIncrControl;
//
//    /**
//     *
//     */
//    public IndexBackFlowInterceptor() {
//        super();
//        this.remoteIncrControl = new DefaultRemoteIncrControl();
//    }
//
//    @Override
//    protected ExecuteResult execute(IExecChainContext context) throws Exception {
//        ITISCoordinator zookeeper = context.getZkClient();
//        // 在回流索引的時候需要写一个标记位到zk中,这样监控在发起抱紧之前判断是否在回流索引,如回流索引的话直接退出了
//        final String zkBackIndexSignalPath = createZkSignalToken(context, zookeeper);
//        // ▼▼▼▼ 索引回流
//        // 先停止增量
//        List<JMXConnector> jmxConns = pauseIncrFlow(context);
//        try {
//            // << 回流索引 开始>>
//            int taskid = context.getTaskId();
//            IndexBackflowManager indexBackFlowQueue = null;
//            if (IndexBuildInterceptor.isPropagateFromIndexBuild(context)) {
//                // 索引build阶段有一个索引build完成就开始執行回流為了實現流水線執行方式
//                indexBackFlowQueue = IndexBuildInterceptor.getIndeBackFlowQueue(context);
//            } else {
//                // 直接开始回流
//                // DocCollection collection = context.getZkStateReader().getClusterState().getCollection(context.getIndexName());
//                IndexBackflowManager indexBackFlowQueueTmp = new IndexBackflowManager(null, context, this);
//                ImportDataProcessInfo state = new ImportDataProcessInfo(taskid, context.getIndexBuildFileSystem(), context.getZkClient());
//                state.setTimepoint(context.getPartitionTimestamp());
//                indexBackFlowQueueTmp.vistAllReplica((replic) -> {
//                    BuildResult buildResult = new BuildResult(replic, state);
//                    buildResult.setSuccess(true);
//                    buildResult.setReplica(replic);
//                    indexBackFlowQueueTmp.addBackFlowTask(buildResult);
//                });
//                indexBackFlowQueue = indexBackFlowQueueTmp;
//            }
//            indexBackFlowQueue.startSwapClusterIndex(taskid);
//            indexBackFlowQueue.await();
//            if (!indexBackFlowQueue.isExecuteSuccess()) {
//                return ExecuteResult.createFaild().setMessage("indexBackFlowQueue.isExecuteSuccess() is false");
//            }
//            logger.info("all node feedback successful,ps:" + context.getPartitionTimestamp());
//        } finally {
//            removeZkSignal(zookeeper, zkBackIndexSignalPath);
//            // 重新开启增量执行
//            resumeIncrFlow(jmxConns, context.getIndexName());
//        }
//        return ExecuteResult.SUCCESS;
//    }
//
//    protected String createZkSignalToken(IExecChainContext context, ITISCoordinator zookeeper) throws KeeperException, InterruptedException {
////        final String zkBackIndexSignalPath = ZkPathUtils.getIndexBackflowSignalPath(context.getIndexName());
////        if (!zookeeper.exists(zkBackIndexSignalPath, true)) {
////            zookeeper.create(zkBackIndexSignalPath, "".getBytes(), CreateMode.PERSISTENT, true);
////        }
////        zookeeper.create(zkBackIndexSignalPath + "/" + ZkPathUtils.INDEX_BACKFLOW_SIGNAL_PATH_SEQNODE_NAME, String.valueOf(System.currentTimeMillis()).getBytes(), CreateMode.EPHEMERAL_SEQUENTIAL, true);
////        return zkBackIndexSignalPath;
//        throw new UnsupportedOperationException("useless");
//    }
//
//    private void removeZkSignal(ITISCoordinator zookeeper, final String zkBackIndexSignalPath) throws KeeperException, InterruptedException {
////        try {
////            List<String> children = zookeeper.getChildren(zkBackIndexSignalPath, null, true);
////            Stat stat = new Stat();
////            for (String c : children) {
////                zookeeper.getData(zkBackIndexSignalPath + "/" + c, null, stat, true);
////                zookeeper.delete(zkBackIndexSignalPath + "/" + c, stat.getVersion(), true);
////            }
////        } catch (Throwable e) {
////            logger.error(zkBackIndexSignalPath, e);
////        }
//        throw new UnsupportedOperationException();
//    }
//
//    /**
//     * @param jmxConns
//     * @throws IOException
//     */
//    protected void resumeIncrFlow(List<JMXConnector> jmxConns, String collectionName) throws IOException {
//        for (JMXConnector c : jmxConns) {
//            try {
//                this.remoteIncrControl.resumeIncrFlow(c, collectionName);
//                c.close();
//            } catch (Exception e) {
//            }
//        }
//    }
//
//    /**
//     * @return
//     * @throws
//     * @throws Exception
//     */
//    private List<JMXConnector> pauseIncrFlow(IExecChainContext context) throws Exception {
//        logger.info("start pause incr process");
//        List<JMXConnector> jmxConns = new ArrayList<JMXConnector>();
//        try {
//            final String incrNodeParent = "/tis/incr_transfer/" + context.getIndexName();
//            if (!context.getZkClient().exists(incrNodeParent, true)) {
//                return jmxConns;
//            }
//            List<String> incrNodes = context.getZkClient().getChildren(incrNodeParent, null, true);
//            for (String incrNode : incrNodes) {
//                jmxConns.add(DefaultRemoteIncrControl.createConnector(new String(context.getZkClient().getData(incrNodeParent + "/" + incrNode, null, new Stat(), true)), INCR_NODE_PORT));
//            }
//            for (JMXConnector c : jmxConns) {
//                this.remoteIncrControl.pauseIncrFlow(c, context.getIndexName());
//            }
//            logger.info("success pause " + jmxConns.size() + "incr nodes");
//        } catch (Exception e) {
//            logger.warn(e.getMessage(), e);
//        }
//        return jmxConns;
//    }
//
//    @Override
//    public Set<FullbuildPhase> getPhase() {
//        return Collections.singleton(FullbuildPhase.IndexBackFlow);
//    }
//}
