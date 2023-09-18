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

package com.qlangtech.tis.datax;

import com.qlangtech.tis.assemble.ExecResult;
import com.qlangtech.tis.cloud.ITISCoordinator;
import com.qlangtech.tis.manage.common.Config;
import com.qlangtech.tis.manage.common.DagTaskUtils;
import com.qlangtech.tis.solrj.util.ZkUtils;
import com.qlangtech.tis.workflow.pojo.WorkFlowBuildHistory;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.recipes.queue.DistributedQueue;
import org.apache.curator.framework.recipes.queue.QueueBuilder;
import org.apache.curator.framework.recipes.queue.QueueConsumer;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

/**
 * DataX 执行器
 *
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2021-05-06 14:57
 **/
public class DataXJobConsumer extends DataXJobSingleProcessorExecutor {
    private static final Logger logger = LoggerFactory.getLogger(DataXJobConsumer.class);
    private final CuratorFramework curatorClient;
    private final ITISCoordinator coordinator;

    public DataXJobConsumer(CuratorFramework curatorClient, ITISCoordinator coordinator) {
        //  this.dataxExecutor = dataxExecutor;
        this.curatorClient = curatorClient;
        this.coordinator = coordinator;
    }


    @Override
    protected boolean isCurrentJobProcessing(Integer jobId) {
        WorkFlowBuildHistory wfStatus = DagTaskUtils.getWFStatus(jobId);
        ExecResult execStat = ExecResult.parse(wfStatus.getState());
        return execStat.isProcessing();
    }

    public static void main(String[] args) throws Exception {
        FileUtils.forceMkdir(Config.getDataDir(false));
        logger.info("Start dataX Executor");
        if (args.length < 2) {
            throw new IllegalArgumentException("args length can not small than 2");
        }

        String zkQueuePath = args[1];
        String zkAddress = args[0];

        DataXJobConsumer dataXJobConsume = getDataXJobConsumer(zkQueuePath, zkAddress);

        synchronized (dataXJobConsume) {
            dataXJobConsume.wait();
        }
    }

    public static DataXJobConsumer getDataXJobConsumer(String zkQueuePath, String zkAddress) throws Exception {

        CuratorFramework curatorClient = getCuratorFramework(zkAddress);
        ITISCoordinator coordinator = getCoordinator(zkAddress, curatorClient);

        // String dataxName, Integer jobId, String jobName, String jobPath
        DataXJobConsumer dataXJobConsume = new DataXJobConsumer(curatorClient, coordinator);

        dataXJobConsume.createQueue(zkQueuePath);
        return dataXJobConsume;
    }

    private void createQueue(String zkQueuePath) {
        createQueue(this.curatorClient, zkQueuePath, this);
    }

    public static CuratorFramework getCuratorFramework(String zkAddress) {
        ExponentialBackoffRetry retryPolicy = new ExponentialBackoffRetry(1000, 3);
        CuratorFrameworkFactory.Builder curatorBuilder = CuratorFrameworkFactory.builder();
        curatorBuilder.retryPolicy(retryPolicy);
        CuratorFramework curatorClient = curatorBuilder.connectString(zkAddress).build();
        curatorClient.start();
        return curatorClient;
    }

    private static ITISCoordinator getCoordinator(String zkAddress, CuratorFramework curatorClient) throws Exception {
        throw new UnsupportedOperationException("zookeeper is abandon");
//        ITISCoordinator coordinator = null;
//
//        final ZooKeeper zooKeeper = curatorClient.getZookeeperClient().getZooKeeper();
//        coordinator = new AdapterTisCoordinator() {
//            @Override
//            public List<String> getChildren(String zkPath, boolean b) {
//                try {
//                    return zooKeeper.getChildren(zkPath);
//                } catch (Exception e) {
//                    throw new RuntimeException(e);
//                }
//            }
//
//            @Override
//            public boolean exists(String path, boolean watch) {
//                try {
//                    Stat exists = zooKeeper.exists(path, false);
//                    return exists != null;
//                } catch (Exception e) {
//                    throw new RuntimeException("path:" + path, e);
//                }
//            }
//
//            @Override
//            public void create(String path, byte[] data, boolean persistent, boolean sequential) {
//
//                CreateMode createMode = null;
//                if (persistent) {
//                    createMode = sequential ? CreateMode.PERSISTENT_SEQUENTIAL : CreateMode.PERSISTENT;
//                } else {
//                    createMode = sequential ? CreateMode.EPHEMERAL_SEQUENTIAL : CreateMode.EPHEMERAL;
//                }
//                try {
//                    zooKeeper.create(path, data, ZooDefs.Ids.OPEN_ACL_UNSAFE, createMode);
//                } catch (Exception e) {
//                    throw new RuntimeException("path:" + path, e);
//                }
//            }
//
//            @Override
//            public byte[] getData(String zkPath, Watcher o, Stat stat, boolean b) {
//                try {
//                    return zooKeeper.getData(zkPath, o, stat);
//                } catch (Exception e) {
//                    throw new RuntimeException(e);
//                }
//            }
//        };
//        logger.info("create TIS new zookeeper instance with ,system zkHost:{}", Config.getZKHost());
//
//        return coordinator;
    }

    public static DistributedQueue<CuratorDataXTaskMessage> createQueue(CuratorFramework curatorClient, String zkQueuePath
            , QueueConsumer<CuratorDataXTaskMessage> consumer) {
        try {
            if (StringUtils.isEmpty(zkQueuePath)) {
                throw new IllegalArgumentException("param zkQueuePath can not be null");
            }
            // TaskConfig taskConfig = TaskConfig.getInstance();
            int count = 0;
            while (!curatorClient.getZookeeperClient().isConnected()) {
                if (count++ > 4) {
                    throw new IllegalStateException(" zookeeper server can not be established");
                }
                logger.info("waiting connect to zookeeper server");
                Thread.sleep(5000);
            }

            ITISCoordinator coordinator = getCoordinator(null, curatorClient);


            ZkUtils.guaranteeExist(coordinator, zkQueuePath);

            QueueBuilder<CuratorDataXTaskMessage> builder = QueueBuilder.builder(curatorClient, consumer, new MessageSerializer(), zkQueuePath);
            // .maxItems(4);

            DistributedQueue<CuratorDataXTaskMessage> queue = builder.buildQueue();
            queue.start();
            return queue;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected DataXJobSubmit.InstanceType getExecMode() {
        return DataXJobSubmit.InstanceType.DISTRIBUTE;
    }

    protected String getIncrStateCollectAddress() {
        // return ZkUtils.getFirstChildValue(this.coordinator, ZkUtils.ZK_ASSEMBLE_LOG_COLLECT_PATH);
        // TODO: 如果是分布式环境的话这个getAssembleHost 需要可以支持HA，目前是写死的
        return Config.getAssembleHost() + ":" + ZkUtils.ZK_ASSEMBLE_LOG_COLLECT_PORT;
    }

    protected String getMainClassName() {
        return DataxExecutor.class.getName();
    }

    protected File getWorkingDirectory() {
        return new File("/opt/tis/tis-datax-executor");
    }

    protected String getClasspath() {
        return "./lib/*:./tis-datax-executor.jar:./conf/";
    }

}
