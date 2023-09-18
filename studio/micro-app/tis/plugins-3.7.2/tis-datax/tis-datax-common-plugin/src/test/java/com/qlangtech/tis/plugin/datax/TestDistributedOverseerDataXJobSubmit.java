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

import com.qlangtech.tis.datax.DataXJobSubmit;
import com.qlangtech.tis.datax.IDataxProcessor;
import com.qlangtech.tis.datax.job.DataXJobWorker;
import com.qlangtech.tis.fullbuild.indexbuild.IRemoteTaskTrigger;
import com.qlangtech.tis.order.center.IAppSourcePipelineController;
import com.qlangtech.tis.order.center.IJoinTaskContext;
import com.qlangtech.tis.plugin.ds.ISelectedTab;
import com.tis.hadoop.rpc.ITISRpcService;
import com.tis.hadoop.rpc.RpcServiceReference;
import com.tis.hadoop.rpc.StatusRpcClient;
import junit.framework.TestCase;
import org.easymock.EasyMock;

import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2021-05-07 13:58
 **/
public class TestDistributedOverseerDataXJobSubmit extends TestCase {

    public static final String DATAX_NAME = "baisuitest";
    public static final Integer DATAX_TASK_ID = 123;
    public static final String DUMP_TABLE = "customer_order_relation";
    public static final String DATAX_JOB_FILE_NAME = DUMP_TABLE + "_1.json";

    public void testPushMsgToDistributeQueue() {

        DataXJobWorker dataxJobWorker = DataXJobWorker.getJobWorker(DataXJobWorker.K8S_DATAX_INSTANCE_NAME);
        assertEquals("/datax/jobs", dataxJobWorker.getZkQueuePath());
        assertEquals("192.168.28.200:2181/tis/cloud", dataxJobWorker.getZookeeperAddress());


        IDataxProcessor dataxProcessor = EasyMock.createMock("dataXProcessor", IDataxProcessor.class); //IAppSource.load(DATAX_NAME);
        assertNotNull(dataxProcessor);

        //IDataxProcessor dataxProcessor = EasyMock.createMock("dataxProcessor", IDataxProcessor.class);
        // EasyMock.expect(dataxProcessor.getDataxCfgDir()).andReturn();

        IJoinTaskContext taskContext = EasyMock.createMock("joinTaskContext", IJoinTaskContext.class);
        ISelectedTab selectTab = EasyMock.createMock("selectTab", ISelectedTab.class);

        // EasyMock.expect(taskContext.getIndexName()).andReturn(DATAX_NAME);
        //  EasyMock.expect(taskContext.getTaskId()).andReturn(DATAX_TASK_ID);
        IAppSourcePipelineController pipelineController = EasyMock.createMock("appSourcePipelineController", IAppSourcePipelineController.class);
        EasyMock.expect(taskContext.getPipelineController()).andReturn(pipelineController);

        AtomicReference<ITISRpcService> ref = new AtomicReference<>();
        ref.set(StatusRpcClient.AssembleSvcCompsite.MOCK_PRC);
        RpcServiceReference svcRef = new RpcServiceReference(ref, () -> {
        });

        Optional<DataXJobSubmit> jobSubmit = DataXJobSubmit.getDataXJobSubmit(false, DataXJobSubmit.InstanceType.DISTRIBUTE);
        assertTrue(jobSubmit.isPresent());
        DataXJobSubmit submit = jobSubmit.get();

        DataXJobSubmit.IDataXJobContext jobContext = submit.createJobContext(taskContext);
        EasyMock.replay(taskContext, selectTab, dataxProcessor, pipelineController);

        DataXJobSubmit.TableDataXEntity tableEntity
                = DataXJobSubmit.TableDataXEntity.createTableEntity4Test(DATAX_JOB_FILE_NAME, DUMP_TABLE);

        IRemoteTaskTrigger dataXJob = submit.createDataXJob(jobContext, svcRef, dataxProcessor, tableEntity);
        dataXJob.run();
        EasyMock.verify(taskContext, selectTab, dataxProcessor, pipelineController);
    }


}
