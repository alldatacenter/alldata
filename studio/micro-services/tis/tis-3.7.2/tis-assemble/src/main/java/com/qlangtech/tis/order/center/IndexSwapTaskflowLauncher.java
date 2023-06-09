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
package com.qlangtech.tis.order.center;

//import com.qlangtech.tis.TisZkClient;

import com.qlangtech.tis.assemble.FullbuildPhase;
import com.qlangtech.tis.cloud.ITISCoordinator;
import com.qlangtech.tis.exec.AbstractActionInvocation;
import com.qlangtech.tis.exec.ActionInvocation;
import com.qlangtech.tis.exec.ExecutePhaseRange;
import com.qlangtech.tis.exec.ExecuteResult;
import com.qlangtech.tis.exec.impl.DefaultChainContext;
import com.qlangtech.tis.extension.impl.XmlFile;
import com.qlangtech.tis.flume.FlumeApplication;
import com.qlangtech.tis.fullbuild.phasestatus.PhaseStatusCollection;
import com.qlangtech.tis.fullbuild.phasestatus.impl.*;
import com.qlangtech.tis.manage.common.Config;
import com.qlangtech.tis.manage.common.SendSMSUtils;
import com.qlangtech.tis.realtime.transfer.IOnsListenerStatus;
import com.qlangtech.tis.rpc.server.FullBuildStatCollectorServer;
import com.qlangtech.tis.rpc.server.IncrStatusServer;
import com.qlangtech.tis.rpc.server.IncrStatusUmbilicalProtocolImpl;
import com.qlangtech.tis.solrj.util.ZkUtils;
import org.apache.commons.daemon.Daemon;
import org.apache.commons.daemon.DaemonContext;
import org.apache.commons.daemon.DaemonInitException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import java.io.File;
import java.util.*;

//import org.apache.solr.cloud.ZkController;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2015年11月5日 下午6:57:19
 */
public class IndexSwapTaskflowLauncher implements Daemon, ServletContextListener {

    private static final Logger logger = LoggerFactory.getLogger(IndexSwapTaskflowLauncher.class);

    public static final String KEY_INDEX_SWAP_TASK_FLOW_LAUNCHER = "IndexSwapTaskflowLauncher";
    //   private TisZkClient zkClient;
    private ITISCoordinator zkClient;
    //private ZkStateReader zkStateReader;

    static {
        initPhaseStatusStatusWriter();
    }

    public static void initPhaseStatusStatusWriter() {
        if (BasicPhaseStatus.statusWriter == null) {
            BasicPhaseStatus.statusWriter = new BasicPhaseStatus.IFlush2Local() {
                @Override
                public void write(File localFile, BasicPhaseStatus status) throws Exception {
                    XmlFile xmlFile = new XmlFile(localFile);
                    xmlFile.write(status, Collections.emptySet());
                }

                @Override
                public BasicPhaseStatus loadPhase(File localFile) throws Exception {
                    XmlFile xmlFile = new XmlFile(localFile);
                    return (BasicPhaseStatus) xmlFile.read();
                }
            };
        }
    }


    public static IndexSwapTaskflowLauncher getIndexSwapTaskflowLauncher(ServletContext context) {
        IndexSwapTaskflowLauncher result = (IndexSwapTaskflowLauncher) context.getAttribute(KEY_INDEX_SWAP_TASK_FLOW_LAUNCHER);
        if (result == null) {
            throw new IllegalStateException("IndexSwapTaskflowLauncher can not be null in servletContext");
        }
        return result;
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
    }


    public void setZkClient(ITISCoordinator zkClient) {
        this.zkClient = zkClient;
    }

    public ITISCoordinator getZkClient() {
        // return zkClient;
        throw new UnsupportedOperationException();
    }
//    public void setZkStateReader(ZkStateReader zkStateReader) {
//        this.zkStateReader = zkStateReader;
//    }

    private Collection<IOnsListenerStatus> incrChannels;

    public Collection<IOnsListenerStatus> getIncrChannels() {
        return incrChannels;
    }

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        // AbstractTisCloudSolrClient.initHashcodeRouter();
        // 构建各阶段持久化
        try {
            this.afterPropertiesSet();
            this.incrChannels = initIncrTransferStateCollect();
            FlumeApplication.startFlume();
            sce.getServletContext().setAttribute(KEY_INDEX_SWAP_TASK_FLOW_LAUNCHER, this);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    // @Override
    public void afterPropertiesSet() throws Exception {

        try {
            // this.setZkClient(new TisZkClient(Config.getZKHost(), 60000));
            // return

            this.setZkClient(ITISCoordinator.create());
        } catch (Exception e) {
            throw new RuntimeException("ZKHost:" + Config.getZKHost(), e);
        }
        // 当初始集群初始化的时候assemble先与solr启动时不执行createClusterZkNodes会出错
        // ZkController.createClusterZkNodes(this.zkClient.getZK());
        // ZkStateReader zkStateReader = new ZkStateReader(zkClient.getZK());
        // zkStateReader.createClusterStateWatchersAndUpdate();
        // this.setZkStateReader(zkStateReader);
    }

    private IncrStatusServer incrStatusServer;

    public IncrStatusServer getIncrStatusUmbilicalProtocol() {
        if (incrStatusServer == null) {
            throw new IllegalStateException("incrStatusUmbilicalProtocolServer can not be null");
        }
        return this.incrStatusServer;
    }

    // 发布增量集群任务收集器
    private Collection<IOnsListenerStatus> initIncrTransferStateCollect() throws Exception {
        // this.incrStatusUmbilicalProtocolServer = new IncrStatusUmbilicalProtocolImpl();
        final int exportPort = ZkUtils.ZK_ASSEMBLE_LOG_COLLECT_PORT; //NetUtils.getFreeSocketPort();
        incrStatusServer = new IncrStatusServer(exportPort);
        incrStatusServer.addService(IncrStatusUmbilicalProtocolImpl.getInstance());
        incrStatusServer.addService(FullBuildStatCollectorServer.getInstance());
        incrStatusServer.start();
        final List<IOnsListenerStatus> result = new ArrayList<>();
        Collection<IOnsListenerStatus> incrChannels = getAllTransferChannel(result);
//        zkClient.addOnReconnect(() -> {
//            try {
//                Thread.sleep(6000);
//            } catch (InterruptedException e) {
//            }
//            getAllTransferChannel(result);
//        });

        ZkUtils.registerAddress2ZK(// "/tis/incr-transfer-group/incr-state-collect"
                this.zkClient, // "/tis/incr-transfer-group/incr-state-collect"
                ZkUtils.ZK_ASSEMBLE_LOG_COLLECT_PATH, exportPort);
        IncrStatusUmbilicalProtocolImpl.getInstance().startLogging();
        return incrChannels;
    }

    //private List<String> indexNames;

//    public boolean containIndex(String collection) {
//        int retry = 0;
//        while (retry++ < 4) {
//            List<String> indexNames = getIndexNames();
//            if (indexNames.contains(collection)) {
//                return true;
//            }
//            try {
//                Thread.sleep(1000);
//            } catch (InterruptedException e) {
//
//            }
//        }
//        return false;
//    }

//    public List<String> getIndexNames() {
//        List<String> result = null;
//        try {
//            int retry = 0;
//            while ((result = indexNames) == null && (retry++) < 5) {
//                Thread.sleep(1000);
//            }
//        } catch (InterruptedException e) {
//            throw new RuntimeException(e);
//        }
//        if (result == null) {
//            throw new IllegalStateException("index name can not be null");
//        }
//        return result;
//    }

    public List<IOnsListenerStatus> getAllTransferChannel(final List<IOnsListenerStatus> result) {
//        try {
//            this.indexNames = zkClient.getChildren("/collections", new AbstractWatcher() {
//
//                @Override
//                protected void process(Watcher watcher) throws KeeperException, InterruptedException {
//                    Thread.sleep(3000);
//                    getAllTransferChannel(result);
//                }
//            }, true);
//
//        } catch (Exception e) {
//            throw new RuntimeException(e);
//        }
//        Set<String> exist = new HashSet<String>();
//        String collectionName = null;
//        Iterator<IOnsListenerStatus> it = result.iterator();
//        while (it.hasNext()) {
//            collectionName = it.next().getCollectionName();
//            if (!indexNames.contains(collectionName)) {
//                it.remove();
//            }
//            exist.add(collectionName);
//        }
//        MasterListenerStatus listenerStatus = null;
//        for (String indexName : indexNames) {
//            if (exist.contains(indexName)) {
//                continue;
//            }
//            listenerStatus = new MasterListenerStatus(indexName, IncrStatusUmbilicalProtocolImpl.getInstance());
//            result.add(listenerStatus);
//        }
        return result;
    }

    /**
     * 由servlet接收到命令之后触发
     *
     * @param execContext
     * @throws Exception
     */
    @SuppressWarnings("all")
    public ExecuteResult startWork(DefaultChainContext chainContext) throws Exception {
        chainContext.rebindLoggingMDCParams();
        ActionInvocation invoke = null;
        ExecutePhaseRange range = chainContext.getExecutePhaseRange();
        logger.info("start component:" + range.getStart() + ",end component:" + range.getEnd());
        // chainContext.setZkClient(zkClient);
        Objects.requireNonNull(this.zkClient, "zkClient can not be null");
        chainContext.setZkClient(this.zkClient);
        // chainContext.setZkStateReader(zkStateReader);
//        Objects.requireNonNull(chainContext.getIndexBuildFileSystem(), "IndexBuildFileSystem of chainContext can not be null");
//        Objects.requireNonNull(chainContext.getTableDumpFactory(), "tableDumpFactory of chainContext can not be null");
//        chainContext.setIndexMetaData(createIndexMetaData(chainContext));
        invoke = AbstractActionInvocation.createExecChain(chainContext);
        ExecuteResult execResult = invoke.invoke();
        if (!execResult.isSuccess()) {
            logger.warn(execResult.getMessage());
            //SendSMSUtils.send("[ERR]fulbud:" + chainContext.getIndexName() + " falid," + execResult.getMessage(), SendSMSUtils.BAISUI_PHONE);
        }
        return execResult;
    }


    // ///daemon/////////////////===========================================
    @Override
    public void init(DaemonContext context) throws DaemonInitException, Exception {
    }

    @Override
    public void start() throws Exception {
        afterPropertiesSet();
        logger.info("index Swap Task ready");
    }

    public static void main(String[] arg) throws Exception {
        IndexSwapTaskflowLauncher launcher = new IndexSwapTaskflowLauncher();
        launcher.start();
        synchronized (launcher) {
            launcher.wait();
        }
    }

    @Override
    public void stop() throws Exception {
    }

    @Override
    public void destroy() {
    }

    /**
     * @param taskid
     * @return
     * @throws Exception
     */
    public static PhaseStatusCollection loadPhaseStatusFromLocal(int taskid) {
        PhaseStatusCollection result = null;
        FullbuildPhase[] phases = FullbuildPhase.values();
        try {
            File localFile = null;
            BasicPhaseStatus phaseStatus;
            for (FullbuildPhase phase : phases) {
                localFile = BasicPhaseStatus.getFullBuildPhaseLocalFile(taskid, phase);
                if (!localFile.exists()) {
                    return result;
                }
                if (result == null) {
                    result = new PhaseStatusCollection(taskid, ExecutePhaseRange.fullRange());
                }
                phaseStatus = BasicPhaseStatus.statusWriter.loadPhase(localFile);
                switch (phase) {
                    case FullDump:
                        result.setDumpPhase((DumpPhaseStatus) phaseStatus);
                        break;
                    case JOIN:
                        result.setJoinPhase((JoinPhaseStatus) phaseStatus);
                        break;
                    case BUILD:
                        result.setBuildPhase((BuildPhaseStatus) phaseStatus);
                        break;
                    case IndexBackFlow:
                        result.setIndexBackFlowPhaseStatus((IndexBackFlowPhaseStatus) phaseStatus);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("taskid:" + taskid, e);
        }
        return result;
    }
}
