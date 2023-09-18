///**
// * Licensed to the Apache Software Foundation (ASF) under one
// * or more contributor license agreements.  See the NOTICE file
// * distributed with this work for additional information
// * regarding copyright ownership.  The ASF licenses this file
// * to you under the Apache License, Version 2.0 (the
// * "License"); you may not use this file except in compliance
// * with the License.  You may obtain a copy of the License at
// * <p>
// * http://www.apache.org/licenses/LICENSE-2.0
// * <p>
// * Unless required by applicable law or agreed to in writing, software
// * distributed under the License is distributed on an "AS IS" BASIS,
// * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// * See the License for the specific language governing permissions and
// * limitations under the License.
// */
//package com.qlangtech.tis.order.center;
//
//import com.google.common.collect.Maps;
//import com.qlangtech.tis.assemble.FullbuildPhase;
//import com.qlangtech.tis.cloud.ITISCoordinator;
//import com.qlangtech.tis.cloud.MockZKUtils;
//import com.qlangtech.tis.exec.ExecChainContextUtils;
//import com.qlangtech.tis.exec.ExecutePhaseRange;
//import com.qlangtech.tis.exec.impl.DefaultChainContext;
//import com.qlangtech.tis.exec.impl.TrackableExecuteInterceptor;
//import com.qlangtech.tis.fs.IPath;
//import com.qlangtech.tis.fs.ITISFileSystem;
//import com.qlangtech.tis.fs.TISFSDataOutputStream;
//import com.qlangtech.tis.fullbuild.indexbuild.*;
//import com.qlangtech.tis.fullbuild.phasestatus.IProcessDetailStatus;
//import com.qlangtech.tis.fullbuild.phasestatus.PhaseStatusCollection;
//import com.qlangtech.tis.fullbuild.phasestatus.impl.BuildPhaseStatus;
//import com.qlangtech.tis.fullbuild.phasestatus.impl.BuildSharedPhaseStatus;
//import com.qlangtech.tis.manage.common.*;
//import com.qlangtech.tis.offline.FileSystemFactory;
//import com.qlangtech.tis.order.dump.task.ITableDumpConstant;
//import com.qlangtech.tis.pubhook.common.RunEnvironment;
//import com.qlangtech.tis.rpc.server.IncrStatusUmbilicalProtocolImpl;
//import com.qlangtech.tis.sql.parser.TabPartitions;
//import com.qlangtech.tis.sql.parser.tuple.creator.EntityName;
//import com.qlangtech.tis.test.TISTestCase;
//import com.qlangtech.tis.trigger.jst.ImportDataProcessInfo;
//import junit.framework.AssertionFailedError;
//import org.apache.commons.io.IOUtils;
//import org.easymock.EasyMock;
//
//import java.io.IOException;
//import java.util.Collection;
//import java.util.Map;
//
//import static com.qlangtech.tis.order.center.TestIndexSwapTaskflowLauncher.TASK_ID;
//import static com.qlangtech.tis.order.center.TestIndexSwapTaskflowLauncher.shardCount;
//
///**
// * @author 百岁（baisui@qlangtech.com）
// * @date 2021-04-06 18:51
// */
//public class TestIndexSwapTaskflowLauncherWithSingleTableIndexBuild extends TISTestCase {
//    String SEARCH_APP_NAME = "search4employee4local";
//    String SINGLE_TABLE = "employees.employees";
//    String pt = "20200616170903";
//
//    static {
//        //  AbstractTisCloudSolrClient.initHashcodeRouter();
//    }
//
//    @Override
//    protected void setUp() throws Exception {
//        super.setUp();
//        this.clearMocks();
//
//    }
//
//    public void testSingleTableIndexBuild() throws Exception {
//        //int order, String testStr, String classpath, Class<?> clazz
//        HttpUtils.addMockApply(0, "search4employee4local/0/daily/schema.xml"
//                , "search4employee4local_schema_response.xml", TestIndexSwapTaskflowLauncherWithSingleTableIndexBuild.class);
//
//
//        final DefaultChainContext chainContext //
//                = TestIndexSwapTaskflowLauncher.createRangeChainContext(SEARCH_APP_NAME
//                , FullbuildPhase.FullDump, FullbuildPhase.IndexBackFlow, pt);
//
//
//        startFullBuild(chainContext, new TestIndexSwapTaskflowLauncher.FullBuildStrategy());
//    }
//
//
//    /**
//     * @param chainContext
//     * @param strategy
//     * @throws Exception
//     */
//    public void startFullBuild(DefaultChainContext chainContext, TestIndexSwapTaskflowLauncher.FullBuildStrategy strategy) throws Exception {
//        ITISCoordinator zkMock = MockZKUtils.createZkMock();
//
//        final String partitionTimestamp = chainContext.getPartitionTimestamp();
//        assertNotNull("has create partitionTimestamp", partitionTimestamp);
//        // final MockFlatTableBuilder flatTableBuilder = new MockFlatTableBuilder();
//        TableDumpFactory tableDumpFactory = mock("tableDumpFactory", TableDumpFactory.class);
//        // FlatTableBuilder flatTableBuilder
//        // = EasyMock.createMock("flatTableBuilder", FlatTableBuilder.class);
//        FileSystemFactory indexBuilderFileSystemFactory = mock("indexBuildFileSystem", FileSystemFactory.class);
//        IndexBuilderTriggerFactory indexBuilderTriggerFactory = mock("indexBuilderTriggerFactory", IndexBuilderTriggerFactory.class);
//        String fsRoot = "/user/admin";
//        ITISFileSystem fileSystem = mock("tisFileSystem", ITISFileSystem.class);
//
//        Map<IDumpTable, ITabPartition> dateParams = Maps.newHashMap();
//        ITabPartition partition = new DftTabPartition(pt);
//        dateParams.put(EntityName.parse(SINGLE_TABLE), partition);
//        chainContext.setAttribute(ExecChainContextUtils.PARTITION_DATA_PARAMS, new TabPartitions(dateParams));
//
//        IndexBuildSourcePathCreator indexBuildSourcePathCreator = mock("indexBuildSourcePathCreator", IndexBuildSourcePathCreator.class);
//
//        EasyMock.expect(indexBuildSourcePathCreator.build("0")).andReturn("0");
//
//        EasyMock.expect(indexBuilderTriggerFactory.createIndexBuildSourcePathCreator(chainContext, partition)).andReturn(indexBuildSourcePathCreator);
//
//        EasyMock.expect(indexBuilderTriggerFactory.getFileSystem()).andReturn(fileSystem).anyTimes();
//        EasyMock.expect(indexBuilderFileSystemFactory.getFileSystem()).andReturn(fileSystem).anyTimes();
//
//        SnapshotDomain domain = HttpConfigFileReader.getResource(SEARCH_APP_NAME, 0
//                , RunEnvironment.getSysRuntime(), ConfigFileReader.FILE_SCHEMA, ConfigFileReader.FILE_SOLR);
//        ImportDataProcessInfo processInfo = new ImportDataProcessInfo(TASK_ID, fileSystem, zkMock);
//        if (!strategy.errorTest()) {
//            for (int groupNum = 0; groupNum < shardCount; groupNum++) {
//                IRemoteJobTrigger builderTrigger = this.mock("indexbuild_" + groupNum, IRemoteJobTrigger.class);
//                builderTrigger.submitJob();
//                RunningStatus runStatus = RunningStatus.SUCCESS;
//                EasyMock.expect(builderTrigger.getRunningStatus()).andReturn(runStatus);
//                EasyMock.expect(indexBuilderTriggerFactory.createBuildJob(
//                        chainContext, partitionTimestamp, SEARCH_APP_NAME, String.valueOf(groupNum), processInfo)).andReturn(builderTrigger);
//                expectSolrMetaOutput(fsRoot, "schema", ConfigFileReader.FILE_SCHEMA, fileSystem, domain, groupNum);
//                expectSolrMetaOutput(fsRoot, "solrconfig", ConfigFileReader.FILE_SOLR, fileSystem, domain, groupNum);
//            }
//        }
//        EasyMock.expect(fileSystem.getRootDir()).andReturn(fsRoot).anyTimes();
//        if (!strategy.errorTest()) {
//            // EasyMock.expect(tableDumpFactory.getJoinTableStorePath(EntityName.parse("tis.totalpay_summary"))).andReturn("xxxx");
//        }
//        ExecutePhaseRange execRange = chainContext.getExecutePhaseRange();
//        if (execRange.contains(FullbuildPhase.FullDump)) {
//            expectCreateSingleTableDumpJob("employees.employees", partitionTimestamp, tableDumpFactory, strategy);
//        }
//        if (strategy.errorTest()) {
//            IncrStatusUmbilicalProtocolImpl.ExecHook execHook = mock("incrStatusUmbilicalProtocolImplExecHook"
//                    , IncrStatusUmbilicalProtocolImpl.ExecHook.class);
//            for (String faildTab : strategy.getTableDumpFaild()) {
//                execHook.reportDumpTableStatusError(TASK_ID, faildTab);
//            }
//            IncrStatusUmbilicalProtocolImpl.execHook = execHook;
//        }
//        replay();
//        chainContext.setTableDumpFactory(tableDumpFactory);
//
//        chainContext.setIndexBuilderTriggerFactory(indexBuilderTriggerFactory);
//        IndexSwapTaskflowLauncher taskflowLauncher = new IndexSwapTaskflowLauncher();
//        taskflowLauncher.afterPropertiesSet();
//        try {
//            taskflowLauncher.startWork(chainContext);
//            if (strategy.errorTest()) {
//                fail();
//            }
//        } catch (AssertionFailedError e) {
//            throw e;
//        } catch (Exception e) {
//            throw e;
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
//    private void expectCreateSingleTableDumpJob(String tableName, String partitionTimestamp
//            , TableDumpFactory tableDumpFactory, TestIndexSwapTaskflowLauncher.FullBuildStrategy strategy) {
//        EntityName table = EntityName.parse(tableName);
//        Map<String, String> params = Maps.newHashMap();
//        params.put(ITableDumpConstant.DUMP_START_TIME, partitionTimestamp);
//        TaskContext taskContext = TaskContext.create(params);
//        EasyMock.expect(tableDumpFactory.createSingleTableDumpJob(table, taskContext)).andReturn(strategy.createRemoteJobTrigger(table));
//    }
//
//    private void expectSolrMetaOutput(String fsRoot, String fileName, PropteryGetter getter, ITISFileSystem fileSystem, SnapshotDomain domain, int groupNum) throws IOException {
//        IPath path = mock("group" + groupNum + "configschema", IPath.class);
//        String p = fsRoot + "/" + SEARCH_APP_NAME + "-" + groupNum + "/config/" + fileName + ".xml";
//        // System.out.println(p);
//        EasyMock.expect(fileSystem.getPath(p)).andReturn(path).anyTimes();
//        TISFSDataOutputStream schemaOutput = mock("group" + groupNum + "_config" + fileName + "OutputStream", TISFSDataOutputStream.class);
//        EasyMock.expect(fileSystem.create(path, true)).andReturn(schemaOutput);
//        // IOUtils.write(ConfigFileReader.FILE_SCHEMA.getContent(domain), schemaOutput);
//        IOUtils.write(getter.getContent(domain), schemaOutput);
//        schemaOutput.close();
//    }
//
//}
