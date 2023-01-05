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
package com.qlangtech.tis.exec;

import com.google.common.collect.Sets;
import com.qlangtech.tis.assemble.FullbuildPhase;
import com.qlangtech.tis.common.utils.Assert;
import com.qlangtech.tis.exec.impl.DefaultChainContext;

import com.qlangtech.tis.exec.impl.TrackableExecuteInterceptor;
import com.qlangtech.tis.fullbuild.indexbuild.ITabPartition;
import com.qlangtech.tis.fullbuild.indexbuild.IndexBuildSourcePathCreator;
import com.qlangtech.tis.manage.common.CenterResource;
import com.qlangtech.tis.manage.common.Config;
import com.qlangtech.tis.manage.common.HttpUtils;
import com.qlangtech.tis.sql.parser.SqlTaskNodeMeta;
import junit.framework.TestCase;

import java.io.InputStream;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2020/04/13
 */
public class TestActionInvocation extends TestCase {

    private static final AtomicInteger execCount = new AtomicInteger();

    private static final AtomicInteger buildCount = new AtomicInteger();

    private static final AtomicInteger backflowCount = new AtomicInteger();

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        System.clearProperty(Config.DEFAULT_DATA_DIR);
        Config.setDataDir(Config.DEFAULT_DATA_DIR);
        CenterResource.setNotFetchFromCenterRepository();
    }

    // 工作流執行方式
//    private static final IExecuteInterceptor[] testworkflowBuild = new IExecuteInterceptor[]{ // new WorkflowTableJoinInterceptor(),
//            new TestWorkflowDumpAndJoinInterceptor(execCount), new TestWorkflowIndexBuildInterceptor(buildCount), new TestIndexBackFlowInterceptor(backflowCount)};

    public void testGetWorkflowDetail() throws Exception {
        int workflowId = 1;
        long currentTime = 1;
        String wfname = "totalpay";
        try (InputStream resourceAsStream = TestActionInvocation.class.getResourceAsStream("do_get_workflow_detail.json")) {
            assertNotNull(resourceAsStream);
        }
        HttpUtils.addMockApply(1, "do_get_workflow_detail", "do_get_workflow_detail.json", TestActionInvocation.class);
        SqlTaskNodeMeta.SqlDataFlowTopology workflowDetail = SqlTaskNodeMeta.getSqlDataFlowTopology(wfname);
        // AbstractActionInvocation.getWorkflowDetail(workflowId);
        assertNotNull(workflowDetail);
        assertEquals(45, workflowDetail.getDataflowId());
        assertEquals(wfname, workflowDetail.getName());
        assertEquals(1594780413149l, workflowDetail.getTimestamp());
    }

//    public void testActionInvocation() throws Exception {
//        DefaultChainContext chainContext = TestIndexSwapTaskflowLauncher.createDumpAndJoinChainContext();
//        ActionInvocation invocation = AbstractActionInvocation.createInvocation(chainContext, testworkflowBuild);
//        invocation.invoke();
//        Assert.assertEquals(1, execCount.get());
//        Assert.assertEquals(0, buildCount.get());
//        Assert.assertEquals(0, backflowCount.get());
//    }

    private static class TestIndexBackFlowInterceptor extends TrackableExecuteInterceptor {

        private final AtomicInteger buildCount;

        public TestIndexBackFlowInterceptor(AtomicInteger buildCount) {
            super();
            this.buildCount = buildCount;
        }

        @Override
        protected ExecuteResult execute(IExecChainContext execChainContext) throws Exception {
            buildCount.incrementAndGet();
            Assert.fail("unreasonable to get here");
            return ExecuteResult.SUCCESS;
        }

        @Override
        public Set<FullbuildPhase> getPhase() {
            return Collections.singleton(FullbuildPhase.IndexBackFlow);
        }
    }

    private static class TestWorkflowDumpAndJoinInterceptor extends TrackableExecuteInterceptor {

        private final AtomicInteger execCount;

        public TestWorkflowDumpAndJoinInterceptor(AtomicInteger execCount) {
            super();
            this.execCount = execCount;
        }

        @Override
        protected ExecuteResult execute(IExecChainContext execChainContext) throws Exception {
            execCount.incrementAndGet();
            return ExecuteResult.SUCCESS;
        }

        @Override
        public Set<FullbuildPhase> getPhase() {
            return Sets.newHashSet(FullbuildPhase.FullDump, FullbuildPhase.JOIN);
        }
    }

//    private static class TestWorkflowIndexBuildInterceptor extends IndexBuildInterceptor {
//
//        private final AtomicInteger execCount;
//
//        public TestWorkflowIndexBuildInterceptor(AtomicInteger execCount) {
//            super();
//            this.execCount = execCount;
//        }
//
//        @Override
//        protected IndexBuildSourcePathCreator createIndexBuildSourceCreator(IExecChainContext execContext, ITabPartition ps) {
//            throw new UnsupportedOperationException();
//        }
//
//        @Override
//        protected ExecuteResult execute(IExecChainContext execChainContext) throws Exception {
//            execCount.incrementAndGet();
//            Assert.fail("unreasonable to get here");
//            return ExecuteResult.SUCCESS;
//        }
//
//        @Override
//        public Set<FullbuildPhase> getPhase() {
//            return Collections.singleton(FullbuildPhase.BUILD);
//        }
//    }
}
