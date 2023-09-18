package com.qlangtech.tis.exec.impl;

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

import com.google.common.collect.Maps;
import com.qlangtech.tis.assemble.FullbuildPhase;
import com.qlangtech.tis.cloud.ITISCoordinator;
import com.qlangtech.tis.common.utils.Assert;
import com.qlangtech.tis.datax.DataXJobSubmit;
import com.qlangtech.tis.datax.IDataxProcessor;
import com.qlangtech.tis.datax.TimeFormat;
import com.qlangtech.tis.datax.impl.DataxProcessor;
import com.qlangtech.tis.exec.ExecChainContextUtils;
import com.qlangtech.tis.exec.ExecutePhaseRange;
import com.qlangtech.tis.exec.ExecuteResult;
import com.qlangtech.tis.exec.IExecChainContext;
import com.qlangtech.tis.fullbuild.indexbuild.IDumpTable;
import com.qlangtech.tis.fullbuild.indexbuild.ITabPartition;
import com.qlangtech.tis.fullbuild.taskflow.AdapterTask;
import com.qlangtech.tis.offline.DataxUtils;
import com.qlangtech.tis.order.center.IndexSwapTaskflowLauncher;
import com.qlangtech.tis.plugin.StoreResourceType;
import com.qlangtech.tis.sql.parser.TabPartitions;
import com.qlangtech.tis.test.TISTestCase;
import org.easymock.EasyMock;

import java.util.HashMap;
import java.util.Map;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2023-02-06 11:09
 **/
public class TestWorkflowDumpAndJoinInterceptor extends TISTestCase {

    public void testExecute() throws Exception {
        System.setProperty(DataxUtils.EXEC_TIMESTAMP, String.valueOf(TimeFormat.getCurrentTimeStamp()));
         // String wfName = "tttt71";
        String wfName = "ttttt6";

        int taskId = 999;
        final boolean isDryRun = false;


        IDataxProcessor processor = DataxProcessor.load(null, StoreResourceType.DataFlow, wfName);
        TrackableExecuteInterceptor.initialTaskPhase(taskId);
        WorkflowDumpAndJoinInterceptor interceptor = new WorkflowDumpAndJoinInterceptor();

        IExecChainContext execContext = this.mock("execContext", IExecChainContext.class);

        // Dry Run
        EasyMock.expect(execContext.isDryRun()).andReturn(isDryRun).anyTimes();

        EasyMock.expect(execContext.getExecutePhaseRange())
                .andReturn(new ExecutePhaseRange(FullbuildPhase.FullDump, FullbuildPhase.JOIN)).anyTimes();
        EasyMock.expect(execContext.getProcessor()).andReturn(processor).anyTimes();

        Map<IDumpTable, ITabPartition> tabPs = Maps.newHashMap();
        EasyMock.expect(execContext.getAttribute(ExecChainContextUtils.PARTITION_DATA_PARAMS)).andReturn(new TabPartitions(tabPs)).anyTimes();
        EasyMock.expect(execContext.getAttribute(AdapterTask.KEY_TASK_WORK_STATUS)).andReturn(new HashMap<String, Boolean>()).anyTimes();


        execContext.rebindLoggingMDCParams();
        EasyMock.expectLastCall().anyTimes();

        EasyMock.expect(execContext.getAttribute(EasyMock.eq(DataXJobSubmit.KEY_DATAX_READERS), EasyMock.anyObject()))
                .andReturn(processor.getReaders(null)).anyTimes();

        //   execContext.getWorkflowName()
        EasyMock.expect(execContext.hasIndexName()).andReturn(false).anyTimes();
        EasyMock.expect(execContext.getZkClient()).andReturn(ITISCoordinator.create()).anyTimes();
        EasyMock.expect(execContext.getTaskId()).andReturn(taskId).anyTimes();
        EasyMock.expect(execContext.getWorkflowName()).andReturn(wfName);
        EasyMock.expect(execContext.getPartitionTimestampWithMillis()).andReturn(TimeFormat.getCurrentTimeStamp()).anyTimes();
        EasyMock.expect(execContext.loadPhaseStatusFromLatest())
                .andReturn(IndexSwapTaskflowLauncher.loadPhaseStatusFromLocal(taskId)).anyTimes();


        this.replay();
        ExecuteResult result = interceptor.execute(execContext);
        Assert.assertTrue(result.isSuccess());

        this.verifyAll();
    }

}
