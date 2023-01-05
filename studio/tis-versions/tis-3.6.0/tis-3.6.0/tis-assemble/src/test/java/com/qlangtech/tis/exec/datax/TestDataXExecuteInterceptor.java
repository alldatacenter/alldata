/**
 *   Licensed to the Apache Software Foundation (ASF) under one
 *   or more contributor license agreements.  See the NOTICE file
 *   distributed with this work for additional information
 *   regarding copyright ownership.  The ASF licenses this file
 *   to you under the Apache License, Version 2.0 (the
 *   "License"); you may not use this file except in compliance
 *   with the License.  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package com.qlangtech.tis.exec.datax;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.qlangtech.tis.assemble.FullbuildPhase;
import com.qlangtech.tis.datax.DataXJobSubmit;
import com.qlangtech.tis.datax.IDataXBatchPost;
import com.qlangtech.tis.datax.IDataxGlobalCfg;
import com.qlangtech.tis.datax.IDataxProcessor;
import com.qlangtech.tis.datax.impl.DataXCfgGenerator;
import com.qlangtech.tis.datax.impl.DataxProcessor;
import com.qlangtech.tis.datax.impl.DataxReader;
import com.qlangtech.tis.datax.impl.DataxWriter;
import com.qlangtech.tis.exec.ExecuteResult;
import com.qlangtech.tis.exec.IExecChainContext;
import com.qlangtech.tis.exec.impl.TrackableExecuteInterceptor;
import com.qlangtech.tis.fullbuild.indexbuild.IRemoteTaskTrigger;
import com.qlangtech.tis.fullbuild.phasestatus.PhaseStatusCollection;
import com.qlangtech.tis.fullbuild.phasestatus.impl.DumpPhaseStatus;
import com.qlangtech.tis.fullbuild.phasestatus.impl.JoinPhaseStatus;
import com.qlangtech.tis.manage.biz.dal.pojo.Application;
import com.qlangtech.tis.manage.common.Config;
import com.qlangtech.tis.order.center.TestIndexSwapTaskflowLauncherWithDataXTrigger;
import com.qlangtech.tis.plugin.PluginStubUtils;
import com.qlangtech.tis.plugin.ds.ISelectedTab;
import com.qlangtech.tis.test.TISTestCase;
import org.apache.commons.io.FileUtils;
import org.easymock.EasyMock;
import org.junit.Assert;

import java.io.File;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2021-05-03 14:56
 **/
public class TestDataXExecuteInterceptor extends TISTestCase {
    private static final String AP_NAME = "testDataxProcessor";
    private static File dataxCfgDir;
    private static final String tableName = "customer_order_relation";
    private static final String dataCfgTaskName = tableName + "_1";
    private static final String dataCfgFileName
            = dataCfgTaskName + IDataxProcessor.DATAX_CREATE_DATAX_CFG_FILE_NAME_SUFFIX;
    static final int testTaskId = 999;

    BatchPostDataXWriter dataxWriter;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        this.clearMocks();
        System.clearProperty(Config.KEY_DATA_DIR);
        Config.setTestDataDir();
        PluginStubUtils.setTISField();
        dataxCfgDir = new File(Config.getDataDir(), "/cfg_repo/tis_plugin_config/ap/" + AP_NAME + "/dataxCfg");
        FileUtils.forceMkdir(dataxCfgDir);

        DataXCfgGenerator.GenerateCfgs genCfg = new DataXCfgGenerator.GenerateCfgs(dataxCfgDir);
        genCfg.setGenTime(System.currentTimeMillis());
        Map<String, List<String>> groupedChildTask = Maps.newHashMap();
        groupedChildTask.put(tableName, Lists.newArrayList(dataCfgTaskName));
        genCfg.setGroupedChildTask(groupedChildTask);
        genCfg.write2GenFile(dataxCfgDir);

        try (InputStream res = TestDataXExecuteInterceptor.class.getResourceAsStream(dataCfgFileName)) {
            Objects.requireNonNull(res, dataCfgFileName + " can not be null");
            FileUtils.copyInputStreamToFile(res, new File(dataxCfgDir, dataCfgFileName));
        }


        DataxReader dataxReader = mock(AP_NAME + "DataXReader", DataxReader.class);
        ISelectedTab tab = new TestSelectedTab(tableName);
        EasyMock.expect(dataxReader.getSelectedTabs()).andReturn(Collections.singletonList(tab)).anyTimes();

        DataxReader.dataxReaderGetter = (name) -> {
            return dataxReader;
        };
        dataxWriter = new BatchPostDataXWriter(Collections.singletonList(dataCfgFileName));
        DataxWriter.dataxWriterGetter = (name) -> {
            // DataxWriter dataxWriter = mock(name + "DataXWriter", DataxWriter.class);
            return dataxWriter;
        };
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        /**
         * 开始校验
         */
        dataxWriter.verify();

    }

    public void testExecute() throws Exception {


        IRemoteTaskTrigger jobTrigger
                = mock(dataCfgTaskName + "_" + IRemoteTaskTrigger.class.getSimpleName(), IRemoteTaskTrigger.class);
        //

        final String preExecuteTaskName = IDataXBatchPost.getPreExecuteTaskName(new TestSelectedTab(tableName));
        EasyMock.expect(jobTrigger.getTaskDependencies())
                .andReturn(Collections.singletonList(preExecuteTaskName)).anyTimes();
        EasyMock.expect(jobTrigger.getTaskName()).andReturn(dataCfgFileName).anyTimes();
        EasyMock.expect(jobTrigger.isAsyn()).andReturn(false).anyTimes();
        jobTrigger.run();
        //   RunningStatus runningStatus = RunningStatus.SUCCESS;
        // EasyMock.expect(jobTrigger.getRunningStatus()).andReturn(runningStatus);

        /**
         * 开始执行
         */
        executeJobTrigger(jobTrigger, true);


        PhaseStatusCollection taskPhaseRef = TrackableExecuteInterceptor.getTaskPhaseReference(testTaskId);
        Assert.assertNotNull(taskPhaseRef);

        DumpPhaseStatus dumpStatus = taskPhaseRef.getDumpPhase();
        Assert.assertNotNull(dumpStatus);

        DumpPhaseStatus.TableDumpStatus tableDumpStatus = dumpStatus.getTable(dataCfgFileName);
        Assert.assertNotNull(tableName + "must exist", tableDumpStatus);
        Assert.assertTrue(tableDumpStatus.isComplete());
        Assert.assertTrue(tableDumpStatus.isSuccess());
        Assert.assertFalse(tableDumpStatus.isWaiting());
        Assert.assertFalse(tableDumpStatus.isFaild());
        Assert.assertEquals(dataCfgFileName, tableDumpStatus.getName());

        DumpPhaseStatus.TableDumpStatus preExecuteStatus = dumpStatus.getTable(preExecuteTaskName);
        Assert.assertNotNull(preExecuteTaskName + "must exist", preExecuteStatus);

        Assert.assertTrue(preExecuteStatus.isComplete());
        Assert.assertTrue(preExecuteStatus.isSuccess());
        Assert.assertFalse(preExecuteStatus.isWaiting());
        Assert.assertFalse(preExecuteStatus.isFaild());
        Assert.assertEquals(preExecuteTaskName, preExecuteStatus.getName());


        JoinPhaseStatus joinStatus = taskPhaseRef.getJoinPhase();
        Assert.assertNotNull(joinStatus);
        JoinPhaseStatus.JoinTaskStatus tableProcess = joinStatus.getTaskStatus(tableName);
        Assert.assertNotNull(tableProcess);

        Assert.assertTrue(tableProcess.isComplete());
        Assert.assertTrue(tableProcess.isSuccess());
        Assert.assertFalse(tableProcess.isWaiting());
        Assert.assertFalse(tableProcess.isFaild());

        Assert.assertEquals(tableName, tableProcess.getName());


    }

    public void testExecuteWithExcpetionWhenSubmitJob() throws Exception {
        IRemoteTaskTrigger jobTrigger = mock("remoteJobTrigger", IRemoteTaskTrigger.class);
        //
        EasyMock.expect(jobTrigger.getTaskName()).andReturn(dataCfgFileName).anyTimes();
        EasyMock.expect(jobTrigger.getTaskDependencies()).andReturn(Collections.emptyList()).anyTimes();
        EasyMock.expect(jobTrigger.isAsyn()).andReturn(false).anyTimes();
        jobTrigger.run();
        EasyMock.expectLastCall().andThrow(new RuntimeException("throw a exception"));
        //  RunningStatus runningStatus = RunningStatus.SUCCESS;
        // EasyMock.expect(jobTrigger.getRunningStatus()).andReturn(runningStatus);

        try {
            executeJobTrigger(jobTrigger, false);

        } catch (Exception e) {
            fail("shall not throw an exception");
        }
    }

    public void testExecuteWithGetRunningStatusFaild() throws Exception {
        IRemoteTaskTrigger jobTrigger = mock("remoteJobTrigger", IRemoteTaskTrigger.class);
        EasyMock.expect(jobTrigger.getTaskName()).andReturn(dataCfgFileName).anyTimes();
        EasyMock.expect(jobTrigger.getTaskDependencies()).andReturn(Collections.emptyList()).anyTimes();
        //
        EasyMock.expect(jobTrigger.isAsyn()).andReturn(false).anyTimes();
        jobTrigger.run();
        EasyMock.expectLastCall().andThrow(new RuntimeException("throw a exception"));
        // EasyMock.expectLastCall().andThrow(new RuntimeException("throw a exception"));
        // RunningStatus runningStatus = RunningStatus.FAILD;
        // EasyMock.expect(jobTrigger.getRunningStatus()).andReturn(runningStatus).anyTimes();


        executeJobTrigger(jobTrigger, false);

    }

    private void executeJobTrigger(IRemoteTaskTrigger jobTrigger, boolean finalSuccess) throws Exception {

        TrackableExecuteInterceptor.initialTaskPhase(testTaskId);

        DataXJobSubmit.mockGetter = () -> new TestIndexSwapTaskflowLauncherWithDataXTrigger.MockDataXJobSubmit(jobTrigger);

        DataXExecuteInterceptor executeInterceptor = new DataXExecuteInterceptor();

        IExecChainContext execChainContext = mock("execChainContext", IExecChainContext.class);
        execChainContext.rebindLoggingMDCParams();
        EasyMock.expectLastCall().anyTimes();
        EasyMock.expect(execChainContext.getIndexName()).andReturn(AP_NAME);
        EasyMock.expect(execChainContext.getTaskId()).andReturn(testTaskId).anyTimes();
        //  getTaskId

        MockDataxProcessor dataxProcessor = new MockDataxProcessor();

        EasyMock.expect(execChainContext.getAppSource()).andReturn(dataxProcessor);

        this.replay();
        /**
         * ================================================================
         * 开始执行
         * ================================================================
         */
        ExecuteResult executeResult = executeInterceptor.execute(execChainContext);


        executeInterceptor.getPhaseStatus(execChainContext, FullbuildPhase.FullDump);

        assertEquals("execute must be " + (finalSuccess ? "success" : "faild"), finalSuccess, executeResult.isSuccess());
        this.verifyAll();
    }


    private static class MockDataxProcessor extends DataxProcessor {
        @Override
        public Application buildApp() {
            return null;
        }

        @Override
        public String identityValue() {
            return AP_NAME;
        }


        @Override
        public IDataxGlobalCfg getDataXGlobalCfg() {
            return null;
        }
    }

}
