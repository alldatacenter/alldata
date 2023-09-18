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

//import junit.framework.TestCase;

package com.qlangtech.tis.order.center;

import com.google.common.collect.Maps;
import com.qlangtech.tis.assemble.FullbuildPhase;
import com.qlangtech.tis.exec.ExecChainContextUtils;
import com.qlangtech.tis.exec.ExecutePhaseRange;
import com.qlangtech.tis.exec.IExecChainContext;
import com.qlangtech.tis.exec.impl.DefaultChainContext;
import com.qlangtech.tis.exec.impl.TrackableExecuteInterceptor;
import com.qlangtech.tis.fullbuild.IFullBuildContext;
import com.qlangtech.tis.fullbuild.indexbuild.IDumpTable;
import com.qlangtech.tis.fullbuild.indexbuild.ITabPartition;
import com.qlangtech.tis.fullbuild.phasestatus.PhaseStatusCollection;
import com.qlangtech.tis.fullbuild.phasestatus.impl.DumpPhaseStatus;
import com.qlangtech.tis.fullbuild.taskflow.TestParamContext;
import com.qlangtech.tis.job.common.JobCommon;
import com.qlangtech.tis.sql.parser.TabPartitions;
import junit.framework.TestCase;
import org.junit.Assert;

import java.util.Map;

//
//import com.google.common.collect.Lists;
//import com.google.common.collect.Maps;
//import com.qlangtech.tis.TIS;
//import com.qlangtech.tis.assemble.FullbuildPhase;
//import com.qlangtech.tis.cloud.ITISCoordinator;
//import com.qlangtech.tis.cloud.MockZKUtils;
//import com.qlangtech.tis.exec.ExecChainContextUtils;
//import com.qlangtech.tis.exec.ExecutePhaseRange;
//import com.qlangtech.tis.exec.IExecChainContext;
//import com.qlangtech.tis.exec.impl.DefaultChainContext;
//import com.qlangtech.tis.exec.impl.TrackableExecuteInterceptor;
//import com.qlangtech.tis.fs.IPath;
//import com.qlangtech.tis.fs.ITISFileSystem;
//import com.qlangtech.tis.fs.TISFSDataOutputStream;
//import com.qlangtech.tis.fullbuild.IFullBuildContext;
//import com.qlangtech.tis.fullbuild.indexbuild.*;
//import com.qlangtech.tis.fullbuild.phasestatus.IProcessDetailStatus;
//import com.qlangtech.tis.fullbuild.phasestatus.PhaseStatusCollection;
//import com.qlangtech.tis.fullbuild.phasestatus.impl.BuildPhaseStatus;
//import com.qlangtech.tis.fullbuild.phasestatus.impl.BuildSharedPhaseStatus;
//import com.qlangtech.tis.fullbuild.taskflow.TestParamContext;
//import com.qlangtech.tis.manage.common.*;
//import com.qlangtech.tis.offline.FileSystemFactory;
//
//import com.qlangtech.tis.order.dump.task.ITableDumpConstant;
//import com.qlangtech.tis.plugin.PluginStore;
//import com.qlangtech.tis.plugin.PluginStubUtils;
//import com.qlangtech.tis.pubhook.common.RunEnvironment;
//import com.qlangtech.tis.realtime.s4totalpay.S4Totalpay;
//import com.qlangtech.tis.rpc.server.IncrStatusUmbilicalProtocolImpl;
//import com.qlangtech.tis.sql.parser.SqlTaskNodeMeta;
//import com.qlangtech.tis.sql.parser.SqlTaskNodeMeta.SqlDataFlowTopology;
//import com.qlangtech.tis.sql.parser.TabPartitions;
//import com.qlangtech.tis.sql.parser.meta.DependencyNode;
//import com.qlangtech.tis.sql.parser.tuple.creator.EntityName;
//import com.qlangtech.tis.trigger.jst.ImportDataProcessInfo;
//import junit.framework.Assert;
//import junit.framework.AssertionFailedError;
//import junit.framework.TestCase;
//import org.apache.commons.io.IOUtils;
//import org.apache.commons.lang3.StringUtils;
//import org.easymock.EasyMock;
//
//import java.io.IOException;
//import java.text.SimpleDateFormat;
//import java.util.*;
//import java.util.regex.Matcher;
//import java.util.regex.Pattern;
//
///**
// * @author 百岁（baisui@qlangtech.com）
// * @date 2019年8月22日
// */
public class TestIndexSwapTaskflowLauncher extends TestCase {
    //
    static final int TASK_ID = 253;
    static final int shardCount = 1;
    private static final String WF_ID = "45";

    public void testLoadPhaseStatusFromLocal() {

        // 确保 static 块执行
        //  Assert.assertNotNull(IndexSwapTaskflowLauncher.class);
        IndexSwapTaskflowLauncher.initPhaseStatusStatusWriter();

        PhaseStatusCollection status = TrackableExecuteInterceptor.initialTaskPhase(TASK_ID);
        DumpPhaseStatus dump = new DumpPhaseStatus(TASK_ID);
        DumpPhaseStatus.TableDumpStatus user = dump.getTable("user");
        user.setAllRows(55);
        user.setWaiting(false);
        user.setComplete(true);
        user.setFaild(true);

        DumpPhaseStatus.TableDumpStatus group = dump.getTable("group");
        group.setAllRows(66);
        group.setWaiting(false);
        group.setComplete(true);
        group.setFaild(false);

        status.setDumpPhase(dump);

        status.flushStatus2Local();


        PhaseStatusCollection loadStatus = IndexSwapTaskflowLauncher.loadPhaseStatusFromLocal(TASK_ID);
        Assert.assertNotNull(loadStatus);

        DumpPhaseStatus actualDump = loadStatus.getDumpPhase();
        Assert.assertNotNull(actualDump);

        DumpPhaseStatus.TableDumpStatus actualUser = actualDump.getTable(user.getName());
        assertDumpStatus(user, actualUser);

        assertDumpStatus(group, actualDump.getTable(group.getName()));
//        Assert.assertNotNull(actualUser);
//        Assert.assertEquals(user.isFaild(), actualUser.isFaild());
    }

    private void assertDumpStatus(DumpPhaseStatus.TableDumpStatus expect, DumpPhaseStatus.TableDumpStatus actual) {
        Assert.assertNotNull(expect);
        Assert.assertNotNull(actual);
        Assert.assertEquals(expect.getAll(), actual.getAll());
        Assert.assertEquals(expect.getName(), actual.getName());
        Assert.assertEquals(expect.getTaskid(),actual.getTaskid());
        Assert.assertEquals(expect.isFaild(),actual.isFaild());
        Assert.assertEquals(expect.isComplete(),actual.isComplete());
        Assert.assertEquals(expect.isSuccess(),actual.isSuccess());
        Assert.assertEquals(expect.isWaiting(),actual.isWaiting());
    }

    //
//    private static final String TAB_TOTALPYINFO = "order.totalpayinfo";
//
//    public void testZkHostGetter() {
//        assertTrue("getZKHost can not null", StringUtils.isNotBlank(Config.getZKHost()));
//        assertTrue("getTisHost can not null", StringUtils.isNotBlank(Config.getConfigRepositoryHost()));
//        assertTrue("getAssembleHost can not null", StringUtils.isNotBlank(Config.getAssembleHost()));
//    }
//
//    public void setUp() throws Exception {
//        HttpUtils.mockConnMaker = new HttpUtils.DefaultMockConnectionMaker();
//        S4Totalpay.stubSchemaXStream();
//        CenterResource.setNotFetchFromCenterRepository();
//        HttpUtils.addMockGlobalParametersConfig();
//        clearMocks();
//        TIS.clean();
//        PluginStubUtils.setTISField();
//    }
//
//
//    public void testIndexBuilder() throws Exception {
//        String pt = "20200616170903";
//        final DefaultChainContext chainContext = createRangeChainContext(FullbuildPhase.BUILD, FullbuildPhase.IndexBackFlow, pt);
//        startFullBuild(chainContext, new FullBuildStrategy());
//    }
//
//    public void testFullDumpUntilIndexBuilder() throws Exception {
//
//        final DefaultChainContext chainContext = createRangeChainContext(FullbuildPhase.FullDump, FullbuildPhase.BUILD);
//        startFullBuild(chainContext, new FullBuildStrategy());
//    }
//
////    public void testFullDumpUntilIndexBuilderTotalpayFaild() throws Exception {
////        S4Totalpay.stubSchemaXStream();
////       // final Set<String> dumpFaildTabs = Sets.newHashSet(TAB_TOTALPYINFO);
////        final Set<String> dumpFaildTabs = Sets.newHashSet("employees.employees");
////
////        final DefaultChainContext chainContext = createRangeChainContext(FullbuildPhase.FullDump, FullbuildPhase.BUILD);
////
////        startFullBuild(chainContext, new FullBuildStrategy() {
////            @Override
////            public Set<String> getTableDumpFaild() {
////                return dumpFaildTabs;
////            }
////        });
////        this.verifyAll();
////    }
//
//    public void startFullBuild(DefaultChainContext chainContext, FullBuildStrategy strategy, Runnable... runnable) throws Exception {
//        ITISCoordinator zkMock = MockZKUtils.createZkMock();
//        mocks.add(zkMock);
//        final String partitionTimestamp = chainContext.getPartitionTimestamp();
//        assertNotNull("has create partitionTimestamp", partitionTimestamp);
//        // final MockFlatTableBuilder flatTableBuilder = new MockFlatTableBuilder();
//        TableDumpFactory tableDumpFactory = mock("tableDumpFactory", TableDumpFactory.class);
//
//        FileSystemFactory indexBuilderFileSystemFactory = mock("indexBuildFileSystem", FileSystemFactory.class);
//        IndexBuilderTriggerFactory indexBuilderTriggerFactory = mock("indexBuilderTriggerFactory", IndexBuilderTriggerFactory.class);
//
//        IndexBuildSourcePathCreator indexBuildSourcePathCreator = mock("indexBuildSourcePathCreator", IndexBuildSourcePathCreator.class);
//
//        EasyMock.expect(indexBuilderTriggerFactory.createIndexBuildSourcePathCreator(EasyMock.eq(chainContext), (EasyMock.anyObject())))
//                .andReturn(indexBuildSourcePathCreator).anyTimes();
//
//        String fsRoot = "/user/admin";
//        ITISFileSystem fileSystem = mock("tisFileSystem", ITISFileSystem.class);
//
//        EasyMock.expect(indexBuilderTriggerFactory.getFileSystem()).andReturn(fileSystem).anyTimes();
//
//        EasyMock.expect(indexBuilderFileSystemFactory.getFileSystem()).andReturn(fileSystem).anyTimes();
//
//        SnapshotDomain domain = HttpConfigFileReader.getResource(SEARCH_APP_NAME, 0
//                , RunEnvironment.getSysRuntime(), ConfigFileReader.FILE_SCHEMA, ConfigFileReader.FILE_SOLR);
//        ImportDataProcessInfo processInfo = new ImportDataProcessInfo(TASK_ID, fileSystem, zkMock);
//        //if (!strategy.errorTest()) {
//        for (int groupNum = 0; groupNum < shardCount; groupNum++) {
//            IRemoteJobTrigger builderTrigger = this.mock("indexbuild_" + groupNum, IRemoteJobTrigger.class);
//            builderTrigger.submitJob();
//            RunningStatus runStatus = RunningStatus.SUCCESS;
//            EasyMock.expect(builderTrigger.getRunningStatus()).andReturn(runStatus).anyTimes();
//            EasyMock.expect(indexBuilderTriggerFactory.createBuildJob(
//                    chainContext, partitionTimestamp, SEARCH_APP_NAME, String.valueOf(groupNum), processInfo)).andReturn(builderTrigger).anyTimes();
//            expectSolrMetaOutput(fsRoot, "schema", ConfigFileReader.FILE_SCHEMA, fileSystem, domain, groupNum);
//            expectSolrMetaOutput(fsRoot, "solrconfig", ConfigFileReader.FILE_SOLR, fileSystem, domain, groupNum);
//        }
//        //}
//        EasyMock.expect(fileSystem.getRootDir()).andReturn(fsRoot).anyTimes();
//        if (!strategy.errorTest()) {
//            // EasyMock.expect(tableDumpFactory.getJoinTableStorePath(EntityName.parse("tis.totalpay_summary"))).andReturn("xxxx");
//        }
//        ExecutePhaseRange execRange = chainContext.getExecutePhaseRange();
//        if (execRange.contains(FullbuildPhase.FullDump)) {
//            expectCreateSingleTableDumpJob("shop.mall_shop", partitionTimestamp, tableDumpFactory, strategy);
//            expectCreateSingleTableDumpJob("order.takeout_order_extra", partitionTimestamp, tableDumpFactory, strategy);
//            expectCreateSingleTableDumpJob("order.orderdetail", partitionTimestamp, tableDumpFactory, strategy);
//            expectCreateSingleTableDumpJob("order.instancedetail", partitionTimestamp, tableDumpFactory, strategy);
//            expectCreateSingleTableDumpJob("order.specialfee", partitionTimestamp, tableDumpFactory, strategy);
//            expectCreateSingleTableDumpJob("order.payinfo", partitionTimestamp, tableDumpFactory, strategy);
//            expectCreateSingleTableDumpJob("member.card", partitionTimestamp, tableDumpFactory, strategy);
//            expectCreateSingleTableDumpJob("order.order_bill", partitionTimestamp, tableDumpFactory, strategy);
//            expectCreateSingleTableDumpJob(TAB_TOTALPYINFO, partitionTimestamp, tableDumpFactory, strategy);
//            expectCreateSingleTableDumpJob("member.customer", partitionTimestamp, tableDumpFactory, strategy);
//            expectCreateSingleTableDumpJob("cardcenter.ent_expense_order", partitionTimestamp, tableDumpFactory, strategy);
//            expectCreateSingleTableDumpJob("cardcenter.ent_expense", partitionTimestamp, tableDumpFactory, strategy);
//            expectCreateSingleTableDumpJob("order.servicebillinfo", partitionTimestamp, tableDumpFactory, strategy);
//
//            expectCreateSingleTableDumpJob("employees.employees", partitionTimestamp, tableDumpFactory, strategy);
//        }
//        if (strategy.errorTest()) {
//            IncrStatusUmbilicalProtocolImpl.ExecHook execHook = mock("incrStatusUmbilicalProtocolImplExecHook", IncrStatusUmbilicalProtocolImpl.ExecHook.class);
//            for (String faildTab : strategy.getTableDumpFaild()) {
//                execHook.reportDumpTableStatusError(TASK_ID, faildTab);
//            }
//            IncrStatusUmbilicalProtocolImpl.execHook = execHook;
//        }
//        replay();
//        for (Runnable run : runnable) {
//            run.run();
//        }
//        chainContext.setTableDumpFactory(tableDumpFactory);
//
//        chainContext.setIndexBuilderTriggerFactory(indexBuilderTriggerFactory);
//        IndexSwapTaskflowLauncher taskflowLauncher = new IndexSwapTaskflowLauncher();
//        taskflowLauncher.afterPropertiesSet();
//        try {
//
//            taskflowLauncher.startWork(chainContext);
//            if (strategy.errorTest()) {
//                fail();
//            }
//        } catch (AssertionFailedError e) {
//            throw e;
//        } catch (Exception e) {
//        }
//        verifyAll();
//        if (execRange.contains(FullbuildPhase.BUILD)) {
//            PhaseStatusCollection phaseStatusCollection = TrackableExecuteInterceptor.taskPhaseReference.get(TASK_ID);
//            assertNotNull("phaseStatusCollection can not be null", phaseStatusCollection);
//            BuildPhaseStatus buildPhase = phaseStatusCollection.getBuildPhase();
//            assertNotNull("buildPhase can not be null", buildPhase);
//            IProcessDetailStatus<BuildSharedPhaseStatus> processStatus = buildPhase.getProcessStatus();
//            processStatus.getProcessPercent();
//            Collection<BuildSharedPhaseStatus> details = processStatus.getDetails();
//            assertEquals(shardCount, details.size());
//            for (int i = 0; i < shardCount; i++) {
//                String sharedName = SEARCH_APP_NAME + "-" + i;
//                BuildSharedPhaseStatus buildSharedPhaseStatus = buildPhase.getBuildSharedPhaseStatus(sharedName);
//                assertNotNull("buildSharedPhaseStatus-" + sharedName, buildSharedPhaseStatus);
//                assertTrue("buildSharedPhaseStatus.isComplete()", buildSharedPhaseStatus.isComplete());
//                assertFalse("buildSharedPhaseStatus.isWaiting()", buildSharedPhaseStatus.isWaiting());
//            }
//        }
//    }
//
//    public static class FullBuildStrategy {
//
//        boolean errorTest() {
//            return getTableDumpFaild().size() > 0;
//        }
//
//        public Set<String> getTableDumpFaild() {
//            return Collections.emptySet();
//        }
//
//        public IRemoteJobTrigger createRemoteJobTrigger(EntityName table) {
//            boolean dumpFaild = getTableDumpFaild().contains(table.getFullName());
//            return new MockRemoteJobTrigger(!dumpFaild);
//        }
//    }
//
//    private void expectSolrMetaOutput(String fsRoot, String fileName, PropteryGetter getter
//            , ITISFileSystem fileSystem, SnapshotDomain domain, int groupNum) throws IOException {
//        IPath path = mock("group" + groupNum + "configschema", IPath.class);
//        String p = fsRoot + "/" + SEARCH_APP_NAME + "-" + groupNum + "/config/" + fileName + ".xml";
//
//        EasyMock.expect(fileSystem.getPath(p)).andReturn(path).anyTimes();
//        TISFSDataOutputStream schemaOutput = mock("group" + groupNum + "_config" + fileName + "OutputStream", TISFSDataOutputStream.class);
//        EasyMock.expect(fileSystem.create(path, true)).andReturn(schemaOutput).anyTimes();
//        // IOUtils.write(ConfigFileReader.FILE_SCHEMA.getContent(domain), schemaOutput);
//        IOUtils.write(getter.getContent(domain), schemaOutput);
//        schemaOutput.close();
//    }
//
//    private void clearMocks() {
//        mocks.clear();
//    }
//
//    private void verifyAll() {
//        mocks.forEach((r) -> {
//            EasyMock.verify(r);
//        });
//    }
//
//    static List<Object> mocks = Lists.newArrayList();
//
//    public <T> T mock(String name, Class<?> toMock) {
//        Object mock = EasyMock.createMock(name, toMock);
//        mocks.add(mock);
//        return (T) mock;
//    }
//
//    public void replay() {
////        mocks.forEach((r) -> {
////            EasyMock.replay(r);
////        });
//        EasyMock.replay(mocks.toArray(new Object[mocks.size()]));
//    }
//
//    private void expectCreateSingleTableDumpJob(String tableName, String partitionTimestamp, TableDumpFactory tableDumpFactory, FullBuildStrategy strategy) {
//        EntityName table = EntityName.parse(tableName);
//        Map<String, String> params = Maps.newHashMap();
//        params.put(ITableDumpConstant.DUMP_START_TIME, partitionTimestamp);
//        TaskContext taskContext = TaskContext.create(params);
//        EasyMock.expect(tableDumpFactory.createSingleTableDumpJob(table, taskContext)).andReturn(strategy.createRemoteJobTrigger(table)).anyTimes();
//    }
//
//    /**
//     * 执行dump和Joiner
//     *
//     * @throws Exception
//     */
//    public void testFullDumpAndJoiner() throws Exception {
//        // System.out.println();
//        assertFalse(TIS.initialized);
//        // resource is in
//        S4Totalpay.stubSchemaXStream();
//        // HttpUtils.addMockApply(-1, "search4totalpay/0/daily/schema.xml", "schema-xstream.xml", S4Totalpay.class);
//        IndexBuilderTriggerFactory indexBuilderTriggerFactory = this.mock("indexBuilderTriggerFactory", IndexBuilderTriggerFactory.class);
//
//        ITISFileSystem fileSystem = this.mock("fileSystem", ITISFileSystem.class);
//        EasyMock.expect(indexBuilderTriggerFactory.getFileSystem()).andReturn(fileSystem);
//
//        TableDumpFactory tableDumpFactory = this.mock("tableDumpFactory", TableDumpFactory.class);
//
//        // EasyMock.anyObject()
//
//        EntityName employees = EntityName.create("employees", "employees");
//        IRemoteJobTrigger remoteJobTrigger = mock("remoteJobTrigger", IRemoteJobTrigger.class);
//
//        EasyMock.expect(tableDumpFactory.createSingleTableDumpJob(EasyMock.eq(employees), EasyMock.anyObject()))
//                .andReturn(remoteJobTrigger);
//        remoteJobTrigger.submitJob();
//
//        //float progress, boolean complete, boolean success
//        RunningStatus runningStatus = new RunningStatus(0, false, false);
//        EasyMock.expect(remoteJobTrigger.getRunningStatus()).andReturn(runningStatus);
//        //  会调第二次成功
//        EasyMock.expect(remoteJobTrigger.getRunningStatus()).andReturn(new RunningStatus(0, true, true));
//
//        replay();
//        final DefaultChainContext chainContext = createDumpAndJoinChainContext();
//        chainContext.setIndexBuilderTriggerFactory(indexBuilderTriggerFactory);
//        chainContext.setTableDumpFactory(tableDumpFactory);
//        IndexSwapTaskflowLauncher taskflowLauncher = new IndexSwapTaskflowLauncher();
//        taskflowLauncher.afterPropertiesSet();
//        taskflowLauncher.startWork(chainContext);
//        verifyAll();
//    }
//
//
    static final String SEARCH_APP_NAME = "search4totalpay";

    //
    public static DefaultChainContext createRangeChainContext(FullbuildPhase start, FullbuildPhase end, String... pts) throws Exception {
        return createRangeChainContext(SEARCH_APP_NAME, start, end, pts);
    }

    //
    public static DefaultChainContext createRangeChainContext(String collectionName
            , FullbuildPhase start, FullbuildPhase end, String... pts) throws Exception {
        TestParamContext params = new TestParamContext();
        params.set(IFullBuildContext.KEY_APP_SHARD_COUNT, String.valueOf(shardCount));
        params.set(IFullBuildContext.KEY_APP_NAME, collectionName);
        params.set(IFullBuildContext.KEY_WORKFLOW_NAME, "totalpay");
        params.set(IFullBuildContext.KEY_WORKFLOW_ID, WF_ID);
        params.set(IExecChainContext.COMPONENT_START, start.getName());
        params.set(IExecChainContext.COMPONENT_END, end.getName());
        final DefaultChainContext chainContext = new DefaultChainContext(params);

        ExecutePhaseRange range = chainContext.getExecutePhaseRange();
        Assert.assertEquals(start, range.getStart());
        Assert.assertEquals(end, range.getEnd());
        Map<IDumpTable, ITabPartition> dateParams = Maps.newHashMap();
        chainContext.setAttribute(ExecChainContextUtils.PARTITION_DATA_PARAMS, new TabPartitions(dateParams));
        chainContext.setAttribute(JobCommon.KEY_TASK_ID, TASK_ID);
//        final PluginStore<IndexBuilderTriggerFactory> buildTriggerFactory = TIS.getPluginStore(IndexBuilderTriggerFactory.class);
//        assertNotNull(buildTriggerFactory.getPlugin());
//        if (pts.length > 0) {
//            chainContext.setPs(pts[0]);
//        } else {
//            SimpleDateFormat format = new SimpleDateFormat("yyyyMMddHHmmss");
//            String pt = format.format(new Date());
//            chainContext.setPs(pt);
//        }
        chainContext.setMdcParamContext(() -> {
        });
        return chainContext;
    }

    //
    public static DefaultChainContext createDumpAndJoinChainContext() throws Exception {
        return createRangeChainContext(FullbuildPhase.FullDump, FullbuildPhase.JOIN);
    }
//
//    // b3eb0-636c-535e-044a-0d8083d6036b
//    static final Pattern idpattern = Pattern.compile("[a-z0-9]+-[a-z0-9]+-[a-z0-9]+-[a-z0-9]+-[a-z0-9]+");
//
//    public void testIDpattern() {
//        Matcher matcher = idpattern.matcher("a3eb0-636c-535e-044a-0d8083d6036b");
//        Assert.assertTrue(matcher.matches());
//    }
//
//    /**
//     * 从远端取
//     *
//     * @throws Exception
//     */
//    public void testGetSqlDataFlowTopologyFromConsole() throws Exception {
//        // SqlDataFlowTopology topology = RunEnvironment.getSysRuntime(ion.getWorkflowDetail(Integer.parseInt(WF_ID));
//        // valiateTopology(topology);
//    }
//
//    // 从本地取
//    public void testGetSqlDataFlowTopology() throws Exception {
//        SqlDataFlowTopology topology = SqlTaskNodeMeta.getSqlDataFlowTopology("totalpay");
//        valiateTopology(topology);
//        // @SuppressWarnings("all")
//        // public static SqlDataFlowTopology getSqlDataFlowTopology(String topologyName)
//        // throws Exception {
//    }
//
//    protected void valiateTopology(SqlDataFlowTopology topology) {
//        Assert.assertNotNull(topology);
//        for (DependencyNode node : topology.getDumpNodes()) {
//            Assert.assertTrue(StringUtils.isNotBlank(node.getDbName()));
//            Assert.assertTrue(StringUtils.isNotBlank(node.getName()));
//        }
//        String spec = topology.getDAGSessionSpec();
//        System.out.println(spec);
//        Matcher matcher = null;
//        String[] groups = StringUtils.split(spec, " ");
//        String[] tuple = null;
//        String[] ids = null;
//        for (String g : groups) {
//            tuple = g.split("->");
//            for (String t : tuple) {
//                if (StringUtils.isEmpty(t)) {
//                    continue;
//                }
//                ids = t.split(",");
//                for (String id : ids) {
//                    matcher = idpattern.matcher(id);
//                    Assert.assertTrue(t, matcher.matches());
//                }
//            }
//        }
//    }
}
