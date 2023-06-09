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
//package com.qlangtech.tis.fullbuild.workflow;
//
//import com.google.common.collect.Lists;
//import com.google.common.collect.Maps;
//import com.qlangtech.tis.assemble.ExecResult;
//import com.qlangtech.tis.assemble.FullbuildPhase;
//import com.qlangtech.tis.cloud.ITISCoordinator;
//import com.qlangtech.tis.exec.ExecChainContextUtils;
//import com.qlangtech.tis.exec.IExecChainContext;
//import com.qlangtech.tis.exec.impl.WorkflowDumpAndJoinInterceptor;
//import com.qlangtech.tis.fullbuild.indexbuild.DftTabPartition;
//import com.qlangtech.tis.fullbuild.indexbuild.RunningStatus;
//import com.qlangtech.tis.fullbuild.phasestatus.impl.DumpPhaseStatus;
//import com.qlangtech.tis.fullbuild.phasestatus.impl.DumpPhaseStatus.TableDumpStatus;
//import com.qlangtech.tis.fullbuild.taskflow.AdapterTask;
//import com.qlangtech.tis.fullbuild.taskflow.DataflowTask;
//import com.qlangtech.tis.manage.common.Config;
//import com.qlangtech.tis.pubhook.common.FileUtils;
//import com.qlangtech.tis.sql.parser.TabPartitions;
//import com.qlangtech.tis.sql.parser.meta.DependencyNode;
//import com.qlangtech.tis.sql.parser.tuple.creator.EntityName;
//import com.qlangtech.tis.utils.Utils;
//import org.apache.commons.lang.StringUtils;
//import org.json.JSONObject;
//import org.json.JSONTokener;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//
//import java.io.File;
//import java.time.format.DateTimeFormatter;
//import java.util.List;
//import java.util.Map;
//import java.util.Optional;
//import java.util.concurrent.CountDownLatch;
//import java.util.concurrent.TimeUnit;
//
////import com.qlangtech.tis.trigger.zk.AbstractWatcher;
//
///**
// * @author 百岁（baisui@qlangtech.com）
// * @date 2020/04/13
// */
//public class SingleTableDump extends DataflowTask {
//
//    private static final Logger logger = LoggerFactory.getLogger(SingleTableDump.class);
//
//    private final int dataSourceTableId;
//
//    private String pt;
//
//    private boolean forceReDump;
//
//    private final TableDumpProgress tableDumpProgress;
//
//    private final EntityName dumpTable;
//
//    private boolean hasValidTableDump;
//
//    private final IExecChainContext execChainContext;
//
//    private final int taskid;
//
//    public static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
//
//    @Override
//    public FullbuildPhase phase() {
//        return FullbuildPhase.FullDump;
//    }
//
//    private final ITISCoordinator zkClient;
//
//    private static final String TABLE_DUMP_ZK_PREFIX = "/tis/table_dump/";
//
//    private final TableDumpStatus tableDumpStatus;
//
//    //  private final TableDumpFactory tableDumpFactory;
//
//    /**
//     * @param dump
//     * @param hasValidTableDump 在之前的导入中是否有
//     * @param pt
//     * @param zkClient
//     * @param execChainContext
//     * @param dumpPhaseStatus
//     */
//    public SingleTableDump(DependencyNode dump, boolean hasValidTableDump, String pt, ITISCoordinator zkClient, IExecChainContext execChainContext, DumpPhaseStatus dumpPhaseStatus) {
//        super(dump.getId());
//        // DumpTable.create(dump.getDbName(), dump.getName());
//        this.dumpTable = dump.parseEntityName();
//        this.tableDumpStatus = dumpPhaseStatus.getTable(String.valueOf(dumpTable));
//        this.dataSourceTableId = Integer.parseInt(dump.getTabid());
//        //  this.tableDumpFactory = execChainContext.getTableDumpFactory();
//        this.hasValidTableDump = hasValidTableDump;
//        this.zkClient = zkClient;
//        if (this.zkClient == null) {
//            throw new NullPointerException("zkClient can not be null");
//        }
//        this.tableDumpProgress = new TableDumpProgress(this.dataSourceTableId);
//        this.tableDumpProgress.setState(ExecuteStatus.NEW);
//        this.forceReDump = false;
//        if (hasValidTableDump) {
//            this.pt = pt;
//        } else {
//            this.pt = execChainContext.getPartitionTimestampWithMillis();
//        }
//        this.execChainContext = execChainContext;
//        this.taskid = this.execChainContext.getTaskId();
//    }
//
//    @Override
//    public String getIdentityName() {
//        return this.dumpTable.getFullName();
//    }
//
//    @Override
//    public void run() throws Exception {
//        DumpTableRunningStatus status = this.call();
//        if (!status.runningStatus.isSuccess()) {
//            this.signTaskFaild();
//            throw new IllegalStateException("table dump faild:" + dumpTable.getFullName());
//        } else {
//            logger.info(this.dumpTable.getFullName() + " dump success, pt:" + status.pt + ",dumpNodeId:" + id + ",taskid:" + this.taskid);
//            this.signTaskSuccess();
//        }
//    }
//
//    @Override
//    protected Map<String, Boolean> getTaskWorkStatus() {
//        return AdapterTask.createTaskWorkStatus(this.execChainContext);
//    }
//
//    /**
//     * description: 提交给线程池
//     */
//    // @Override
//    public DumpTableRunningStatus call() {
//        final CountDownLatch localTaskCountDownLatch = new CountDownLatch(1);
//        if (hasDumpTaskInProgress(localTaskCountDownLatch)) {
//            // 先判断是否有一个正在执行的任务
//            // 直接接到那个任务上去
//            logger.info("table dump task:" + dumpTable.getNameWithPath() + " has dump task in progress wait");
//            tableDumpProgress.setState(ExecuteStatus.IN_PROGRESS);
//            try {
//                // 等待另外的进程将表dump完成
//                if (!localTaskCountDownLatch.await(WorkflowDumpAndJoinInterceptor.TIME_OUT_HOURS, TimeUnit.HOURS)) {
//                    throw new IllegalStateException(dumpTable.getFullName() + " dump faild");
//                }
//            } catch (InterruptedException e) {
//                throw new RuntimeException(e);
//            }
//            if (tableDumpProgress.getState() == ExecuteStatus.SUCCESS) {
//                return new DumpTableRunningStatus(new RunningStatus(1.0f, true, true), dumpTable, this.pt);
//            } else {
//                return triggerRemoteDumpJob();
//            }
//        } else {
//            if (forceReDump) {
//                return triggerRemoteDumpJob();
//            } else {
//                // 判断是否有一个有效的dump
//                if (hasValidTableDump) {
//                    logger.info("table dump task:" + this.dumpTable.getNameWithPath() + " has a valid dump, so do not need dump again");
//                    tableDumpProgress.setState(ExecuteStatus.SUCCESS);
//                    recordPt();
//                    return new DumpTableRunningStatus(new RunningStatus(1f, true, true), dumpTable, this.pt);
//                } else {
//                    return triggerRemoteDumpJob();
//                }
//            }
//        }
//    }
//
//    private DumpTableRunningStatus triggerRemoteDumpJob() {
//        // = HeteroEnum.DS_DUMP.getPlugin();
//        ;
//        Map<String, String> params = Maps.newHashMap();
////        params.put(JobCommon.KEY_TASK_ID, String.valueOf(taskid));
////        params.put(ITableDumpConstant.DUMP_START_TIME, this.pt);
////
////        TaskContext taskContext = TaskContext.create((key) -> params.get(key));
////        taskContext.setCoordinator(this.zkClient);
////
////        IRemoteTaskTrigger job = tableDumpFactory.createSingleTableDumpJob(dumpTable, taskContext);
////        job.submitJob();
////        RunningStatus runningStatus = job.getRunningStatus();
////        while (!runningStatus.isComplete()) {
////            try {
////                Thread.sleep(3000);
////            } catch (InterruptedException e) {
////            }
////            runningStatus = job.getRunningStatus();
////        }
////        if (runningStatus.isSuccess()) {
////            this.addTableDumpRecord(ExecResult.SUCCESS, pt);
////            recordPt();
////        } else {
////            this.addTableDumpRecord(ExecResult.FAILD, pt);
////        }
//        // }
//        return new DumpTableRunningStatus(null, dumpTable, pt);
//    }
//
//    /**
//     * 添加当前任务的pt
//     */
//    private void recordPt() {
//        //  Map<IDumpTable, ITabPartition> dateParams = ExecChainContextUtils.getDependencyTablesPartitions(execChainContext);
//        TabPartitions dateParams = ExecChainContextUtils.getDependencyTablesPartitions(execChainContext);
//        dateParams.putPt(this.dumpTable, new DftTabPartition(pt));
//    }
//
//    /**
//     * 查看是否有正在进行的dump任务
//     *
//     * @return the boolean
//     */
//    private boolean hasDumpTaskInProgress(final CountDownLatch localTaskCountDownLatch) {
//        return true;
////        final String path = TABLE_DUMP_ZK_PREFIX + this.dumpTable.getDbName() + "_" + this.dumpTable.getTableName();
////        try {
////            if (!zkClient.exists(path, true)) {
////                // 不存在的话直接返回
////                return false;
////            }
////            byte[] bytes = zkClient.getData(path, new AbstractWatcher() {
////
////                @Override
////                protected void process(Watcher watcher) throws InterruptedException {
////                    // if (event.getType() == Event.EventType.NodeDeleted) {
////                    Thread.sleep(1000);
////                    if (tableDumpProgress.getState() == ExecuteStatus.IN_PROGRESS) {
////                        // 去数据库看看 这次任务有没有成功
////                        if (isTableDumpSuccess(dumpTable, pt)) {
////                            tableDumpProgress.setState(ExecuteStatus.SUCCESS);
////                            recordPt();
////                        } else {
////                            tableDumpProgress.setState(ExecuteStatus.FAILED);
////                        }
////                        localTaskCountDownLatch.countDown();
////                    }
////                    // }
////                }
////            }, new Stat(), true);
////            pt = new String(bytes);
////            return true;
////        } catch (Exception e) {
////            // System.out.println("zookeeper path " + path + " not exists");
////            logger.warn("zookeeper path " + path + " not exists", e);
////            return false;
////        }
////        catch (InterruptedException e) {
////            // System.out.println( + " InterruptedException");
////            logger.warn("zookeeper path " + path, e);
////            return false;
////        }
//    }
//
//    private void addTableDumpRecord(ExecResult execResult, String pt) {
//        addTableDumpRecord(this.dumpTable, execResult, pt);
//    }
//
//    /**
//     * 向tis-console中发布表dump的結果
//     */
//    private static void addTableDumpRecord(EntityName dumpTable, ExecResult execResult, String pt) {
//        // JSONObject j = new JSONObject();
//        // j.put("pt", pt);
//        // // String url = WorkflowDumpAndJoinInterceptor.WORKFLOW_CONFIG_URL_POST_FORMAT.format(new Object[]{"fullbuild_workflow_action", "do_add_table_dump_record"});
//        // //List<PostParam> postParams = new LinkedList<>();
//        // //postParams.add(new PostParam("datasource_table_id", Integer.toString(dataSourceTableId)));
//        // //postParams.add(new PostParam("hive_table_name", dumpTable.getNameWithPath()));
//        // postParams.add(new PostParam("state", String.valueOf(execResult.getValue())));
//        // //postParams.add(new PostParam("info", j.toString()));
//        // postParams.add(new PostParam("pt", pt));
//        // 向本地文件系统中写入表执行状态
//        JSONObject json = new JSONObject();
//        json.put("state", execResult.getValue());
//        json.put("pt", pt);
//        File tableDumpLog = getTableDumpLog(dumpTable);
//        FileUtils.append(tableDumpLog, json.toString());
//    }
//
//    private static File getTableDumpLog(EntityName dumpTable) {
//        return new File(Config.getDataDir(), "tab_dump_logs/" + dumpTable.getNameWithPath());
//    }
//
//    private static boolean isTableDumpSuccess(EntityName dumpTable, String pt) {
//        File tableDumpLog = getTableDumpLog(dumpTable);
//        if (!tableDumpLog.exists()) {
//            return false;
//        }
//        List<DumpTableStatus> status = Lists.newArrayList();
//        Utils.readLastNLine(tableDumpLog, 4, (line) -> {
//            JSONTokener t = new JSONTokener(line);
//            JSONObject stat = new JSONObject(t);
//            status.add(new DumpTableStatus(stat.getString("pt"), ExecResult.parse(stat.getInt("state"))));
//        });
//        Optional<DumpTableStatus> match = status.stream().filter((r) -> {
//            return r.execResult == ExecResult.SUCCESS && StringUtils.equals(pt, r.pt);
//        }).findFirst();
//        return match.isPresent();
//    }
//
//    private static class DumpTableStatus {
//
//        private final String pt;
//
//        private final ExecResult execResult;
//
//        public DumpTableStatus(String pt, ExecResult execResult) {
//            this.pt = pt;
//            this.execResult = execResult;
//        }
//    }
//
//    public static class DumpTableRunningStatus {
//
//        public final RunningStatus runningStatus;
//
//        public final EntityName dumpTable;
//
//        public final String pt;
//
//        public DumpTableRunningStatus(RunningStatus runningStatus, EntityName dumpTable, String pt) {
//            super();
//            this.runningStatus = runningStatus;
//            this.dumpTable = dumpTable;
//            this.pt = pt;
//        }
//    }
//}
