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

import com.qlangtech.tis.assemble.FullbuildPhase;
import com.qlangtech.tis.cloud.ITISCoordinator;
import com.qlangtech.tis.datax.DataXJobSubmit;
import com.qlangtech.tis.datax.IDataxProcessor;
import com.qlangtech.tis.exec.ExecutePhaseRange;
import com.qlangtech.tis.exec.IExecChainContext;
import com.qlangtech.tis.fullbuild.indexbuild.IRemoteTaskTrigger;
import com.qlangtech.tis.fullbuild.phasestatus.PhaseStatusCollection;
import com.qlangtech.tis.fullbuild.phasestatus.impl.DumpPhaseStatus;
import com.qlangtech.tis.manage.common.CenterResource;
import com.qlangtech.tis.manage.common.HttpUtils;
import com.qlangtech.tis.manage.common.TisUTF8;
import com.qlangtech.tis.solrj.util.ZkUtils;
import com.tis.hadoop.rpc.ITISRpcService;
import com.tis.hadoop.rpc.RpcServiceReference;
import com.tis.hadoop.rpc.StatusRpcClient;
import junit.framework.Assert;
import junit.framework.TestCase;
import org.apache.zookeeper.data.Stat;
import org.easymock.EasyMock;

import java.io.File;
import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2021-05-21 16:39
 **/
public class TestLocalDataXJobSubmit extends TestCase {

    public void setUp() throws Exception {
        super.setUp();
        HttpUtils.addMockGlobalParametersConfig();
        // Config.setNotFetchFromCenterRepository();
        CenterResource.setNotFetchFromCenterRepository();
    }

    public static final int TaskId = 1;
    public static final String dataXfileName = "customer_order_relation_0.json";
    public static final String dataXName = "baisuitestTestcase";
    public static final String statusCollectorHost = "127.0.0.1:3489";

    public void testCreateDataXJob() throws Exception {


        Optional<DataXJobSubmit> dataXJobSubmit = DataXJobSubmit.getDataXJobSubmit(DataXJobSubmit.InstanceType.LOCAL);
        Assert.assertTrue("dataXJobSubmit shall present", dataXJobSubmit.isPresent());

        LocalDataXJobSubmit jobSubmit = (LocalDataXJobSubmit) dataXJobSubmit.get();
        jobSubmit.setMainClassName(LocalDataXJobMainEntrypoint.class.getName());
        jobSubmit.setWorkingDirectory(new File("."));
        jobSubmit.setClasspath("target/classes:target/test-classes");

        AtomicReference<ITISRpcService> ref = new AtomicReference<>();
        ref.set(StatusRpcClient.AssembleSvcCompsite.MOCK_PRC);
        RpcServiceReference statusRpc = new RpcServiceReference(ref, () -> {
        });

        DataXJobSubmit.IDataXJobContext dataXJobContext = EasyMock.createMock("dataXJobContext", DataXJobSubmit.IDataXJobContext.class);


        IExecChainContext taskContext = EasyMock.createMock("taskContext", IExecChainContext.class);
        EasyMock.expect(dataXJobContext.getTaskContext()).andReturn(taskContext).anyTimes();
        IDataxProcessor dataxProcessor = EasyMock.createMock("dataxProcessor", IDataxProcessor.class);
        EasyMock.expect(taskContext.getIndexName()).andReturn(dataXName).anyTimes();
        EasyMock.expect(taskContext.getTaskId()).andReturn(TaskId).anyTimes();

        int preSuccessTaskId = 99;
        PhaseStatusCollection preSuccessTask = new PhaseStatusCollection(preSuccessTaskId, new ExecutePhaseRange(FullbuildPhase.FullDump, FullbuildPhase.FullDump));
        DumpPhaseStatus preDumpStatus = new DumpPhaseStatus(preSuccessTaskId);
        DumpPhaseStatus.TableDumpStatus tableDumpStatus = preDumpStatus.getTable(dataXfileName);
        tableDumpStatus.setAllRows(LocalDataXJobMainEntrypoint.testAllRows);

        preSuccessTask.setDumpPhase(preDumpStatus);
        EasyMock.expect(taskContext.loadPhaseStatusFromLatest(dataXName)).andReturn(preSuccessTask).times(3);

        ITISCoordinator zkClient = EasyMock.createMock("TisZkClient", ITISCoordinator.class);

        String zkSubPath = "nodes0000000020";
        EasyMock.expect(zkClient.getChildren(
                ZkUtils.ZK_ASSEMBLE_LOG_COLLECT_PATH, null, true))
                .andReturn(Collections.singletonList(zkSubPath)).times(3);
        EasyMock.expect(zkClient.getData(EasyMock.eq(ZkUtils.ZK_ASSEMBLE_LOG_COLLECT_PATH + "/" + zkSubPath)
                , EasyMock.isNull(), EasyMock.anyObject(Stat.class), EasyMock.eq(true)))
                .andReturn(statusCollectorHost.getBytes(TisUTF8.get())).times(3);

        EasyMock.expect(taskContext.getZkClient()).andReturn(zkClient).anyTimes();

        EasyMock.replay(taskContext, dataxProcessor, zkClient, dataXJobContext);
//        DataXJobSubmit.IDataXJobContext taskContext
//         RpcServiceReference statusRpc
//           IDataxProcessor dataxProcessor
//        , String dataXfileName,
        IRemoteTaskTrigger dataXJob = jobSubmit.createDataXJob(dataXJobContext, statusRpc, dataxProcessor, dataXfileName, Collections.emptyList());

        // RunningStatus running = getRunningStatus(dataXJob);
        // assertTrue("running.isSuccess", running.isSuccess());

        jobSubmit.setMainClassName(LocalDataXJobMainEntrypointThrowException.class.getName());
        dataXJob = jobSubmit.createDataXJob(dataXJobContext, statusRpc, dataxProcessor, dataXfileName, Collections.emptyList());

//        running = getRunningStatus(dataXJob);
//        assertFalse("shall faild", running.isSuccess());
//        assertTrue("shall complete", running.isComplete());

        jobSubmit.setMainClassName(LocalDataXJobMainEntrypointCancellable.class.getName());
        dataXJob = jobSubmit.createDataXJob(dataXJobContext, statusRpc, dataxProcessor, dataXfileName, Collections.emptyList());
        //  running = getRunningStatus(dataXJob, false);
        Thread.sleep(2000);
        dataXJob.cancel();
        int i = 0;

//        while (i++ < 3 && !(running = dataXJob.getRunningStatus()).isComplete()) {
//            Thread.sleep(1000);
//        }
//        assertFalse("shall faild", running.isSuccess());
//        assertTrue("shall complete", running.isComplete());

        EasyMock.verify(taskContext, dataxProcessor, zkClient);
    }

//    protected RunningStatus getRunningStatus(IRemoteTaskTrigger dataXJob) {
//        return this.getRunningStatus(dataXJob, true);
//    }
//
//    protected RunningStatus getRunningStatus(IRemoteTaskTrigger dataXJob, boolean waitting) {
//        dataXJob.run();
//        RunningStatus running = null;
//        while ((running = dataXJob.getRunningStatus()) != null && waitting) {
//            if (running.isComplete()) {
//                break;
//            }
//        }
//        assertNotNull(running);
//        return running;
//    }
}
