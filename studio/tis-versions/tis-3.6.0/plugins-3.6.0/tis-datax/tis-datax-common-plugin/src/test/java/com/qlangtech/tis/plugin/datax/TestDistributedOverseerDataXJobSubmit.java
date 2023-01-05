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
import com.qlangtech.tis.datax.impl.DataxProcessor;
import com.qlangtech.tis.datax.job.DataXJobWorker;
import com.qlangtech.tis.fullbuild.indexbuild.IRemoteTaskTrigger;
import com.qlangtech.tis.manage.IAppSource;
import com.qlangtech.tis.order.center.IJoinTaskContext;
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
    public static final String DATAX_JOB_FILE_NAME = "customer_order_relation_1.json";

    public void testPushMsgToDistributeQueue() {

        DataXJobWorker dataxJobWorker = DataXJobWorker.getJobWorker(DataXJobWorker.K8S_DATAX_INSTANCE_NAME);
        assertEquals("/datax/jobs", dataxJobWorker.getZkQueuePath());
        assertEquals("192.168.28.200:2181/tis/cloud", dataxJobWorker.getZookeeperAddress());


        DataxProcessor dataxProcessor = IAppSource.load(DATAX_NAME);
        assertNotNull(dataxProcessor);

        //IDataxProcessor dataxProcessor = EasyMock.createMock("dataxProcessor", IDataxProcessor.class);
        // EasyMock.expect(dataxProcessor.getDataxCfgDir()).andReturn();

        IJoinTaskContext taskContext = EasyMock.createMock("joinTaskContext", IJoinTaskContext.class);


        EasyMock.expect(taskContext.getIndexName()).andReturn(DATAX_NAME);
        EasyMock.expect(taskContext.getTaskId()).andReturn(DATAX_TASK_ID);

        AtomicReference<ITISRpcService> ref = new AtomicReference<>();
        ref.set(StatusRpcClient.AssembleSvcCompsite.MOCK_PRC);
        RpcServiceReference svcRef = new RpcServiceReference(ref, () -> {
        });

        Optional<DataXJobSubmit> jobSubmit = DataXJobSubmit.getDataXJobSubmit(DataXJobSubmit.InstanceType.DISTRIBUTE);
        assertTrue(jobSubmit.isPresent());
        DataXJobSubmit submit = jobSubmit.get();

        DataXJobSubmit.IDataXJobContext jobContext = submit.createJobContext(taskContext);
        EasyMock.replay(taskContext);
        //IJoinTaskContext taskContext
        //            , RpcServiceReference statusRpc, IDataxProcessor dataxProcessor, String dataXfileName
        IRemoteTaskTrigger dataXJob = submit.createDataXJob(jobContext, svcRef, dataxProcessor, DATAX_JOB_FILE_NAME, Collections.emptyList());
        dataXJob.run();
        EasyMock.verify(taskContext);
    }
}
