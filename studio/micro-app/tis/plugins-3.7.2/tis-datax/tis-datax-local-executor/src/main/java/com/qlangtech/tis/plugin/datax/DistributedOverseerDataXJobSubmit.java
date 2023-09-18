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

package com.qlangtech.tis.plugin.datax;

import com.qlangtech.tis.annotation.Public;
import com.qlangtech.tis.datax.*;
import com.qlangtech.tis.datax.job.DataXJobWorker;
import com.qlangtech.tis.extension.TISExtension;
import com.qlangtech.tis.fullbuild.indexbuild.IRemoteTaskTrigger;
import com.qlangtech.tis.fullbuild.indexbuild.impl.AsynRemoteJobTrigger;
import com.qlangtech.tis.order.center.IAppSourcePipelineController;
import com.qlangtech.tis.order.center.IJoinTaskContext;
import com.tis.hadoop.rpc.RpcServiceReference;
//import org.apache.curator.framework.CuratorFramework;
//import org.apache.curator.framework.recipes.queue.DistributedQueue;

import java.util.List;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2021-04-27 21:41
 **/
@TISExtension()
@Public
public class DistributedOverseerDataXJobSubmit extends DataXJobSubmit {

//    private CuratorFramework curatorClient = null;
//    private DistributedQueue<CuratorDataXTaskMessage> curatorDistributedQueue = null;

    public DistributedOverseerDataXJobSubmit() {

    }

    @Override
    public InstanceType getType() {
        return InstanceType.DISTRIBUTE;
    }

    @Override
    public IDataXJobContext createJobContext(IJoinTaskContext parentContext) {
        return new IDataXJobContext() {
            @Override
            public IJoinTaskContext getTaskContext() {
                return parentContext;
            }



            @Override
            public void destroy() {

            }
        };
    }

    @Override
    protected IRemoteTaskTrigger createDataXJob(IDataXJobContext dataXJobContext
            , RpcServiceReference statusRpc, DataXJobInfo jobName, IDataxProcessor processor, CuratorDataXTaskMessage msg) {

return null;
//        IJoinTaskContext taskContext = dataXJobContext.getTaskContext();
//        IAppSourcePipelineController pipelineController = taskContext.getPipelineController();
//      //  DistributedQueue<CuratorDataXTaskMessage> distributedQueue = getCuratorDistributedQueue();
//        // File jobPath = new File(dataxProcessor.getDataxCfgDir(null), dataXfileName);
//        return new AsynRemoteJobTrigger(jobName.jobFileName) {
//            @Override
//            public void run() {
//                try {
//                    //  IDataxReader reader = dataxProcessor.getReader(null);
//                  //  CuratorDataXTaskMessage msg = getDataXJobDTO(taskContext, jobName);
//                    distributedQueue.put(msg);
//                    pipelineController.registerAppSubExecNodeMetrixStatus(
//                            IAppSourcePipelineController.DATAX_FULL_PIPELINE + taskContext.getIndexName(), jobName.jobFileName);
//                } catch (Exception e) {
//                    throw new RuntimeException(e);
//                }
//            }
//
//
//            @Override
//            public void cancel() {
//                pipelineController.stop(
//                        IAppSourcePipelineController.DATAX_FULL_PIPELINE + taskContext.getIndexName());
//            }
//        };
    }

//
//    private DistributedQueue<CuratorDataXTaskMessage> getCuratorDistributedQueue() {
//        synchronized (this) {
//            if (curatorClient != null && !curatorClient.getZookeeperClient().isConnected()) {
//                curatorClient.close();
//                curatorClient = null;
//                curatorDistributedQueue = null;
//            }
//            if (curatorDistributedQueue == null) {
//                DataXJobWorker dataxJobWorker = DataXJobWorker.getJobWorker(DataXJobWorker.K8S_DATAX_INSTANCE_NAME);
//                if (curatorClient == null) {
//                    this.curatorClient = DataXJobConsumer.getCuratorFramework(dataxJobWorker.getZookeeperAddress());
//                }
//                this.curatorDistributedQueue = DataXJobConsumer.createQueue(curatorClient, dataxJobWorker.getZkQueuePath(), null);
//            }
//            return this.curatorDistributedQueue;
//        }
//    }
}
