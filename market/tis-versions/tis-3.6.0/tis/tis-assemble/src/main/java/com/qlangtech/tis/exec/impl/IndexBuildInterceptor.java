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
//package com.qlangtech.tis.exec.impl;
//
//import com.qlangtech.tis.assemble.FullbuildPhase;
//import com.qlangtech.tis.cloud.dump.DumpJobStatus;
//import com.qlangtech.tis.exec.ExecChainContextUtils;
//import com.qlangtech.tis.exec.ExecuteResult;
//import com.qlangtech.tis.exec.IExecChainContext;
//import com.qlangtech.tis.exec.IIndexMetaData;
//import com.qlangtech.tis.fullbuild.indexbuild.IRemoteJobTrigger;
//import com.qlangtech.tis.fullbuild.indexbuild.ITabPartition;
//import com.qlangtech.tis.fullbuild.indexbuild.IndexBuildSourcePathCreator;
//import com.qlangtech.tis.fullbuild.indexbuild.RunningStatus;
//import com.qlangtech.tis.fullbuild.phasestatus.impl.BuildPhaseStatus;
//import com.qlangtech.tis.fullbuild.phasestatus.impl.BuildSharedPhaseStatus;
//import com.qlangtech.tis.fullbuild.phasestatus.impl.JoinPhaseStatus;
//import com.qlangtech.tis.manage.common.SnapshotDomain;
//import com.qlangtech.tis.offline.IndexBuilderTriggerFactory;
//import com.qlangtech.tis.order.center.IndexBackflowManager;
//import com.qlangtech.tis.trigger.jst.AbstractIndexBuildJob;
//import com.qlangtech.tis.trigger.jst.AbstractIndexBuildJob.BuildResult;
//import com.qlangtech.tis.trigger.jst.ImportDataProcessInfo;
//import org.apache.solr.common.cloud.Replica;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//
//import java.lang.Thread.UncaughtExceptionHandler;
//import java.util.Collections;
//import java.util.List;
//import java.util.Objects;
//import java.util.Set;
//import java.util.concurrent.*;
//
////import com.qlangtech.tis.manage.common.ConfigFileReader;
////import com.qlangtech.tis.manage.common.HttpConfigFileReader;
//
///**
// * 索引buid执行器
// *
// * @author 百岁（baisui@qlangtech.com）
// * @date 2015年12月15日 下午5:08:07
// */
//public abstract class IndexBuildInterceptor extends TrackableExecuteInterceptor {
//
//    public static final String NAME = "indexBuild";
//
//    public static final String KEY_INDEX_BACK_FLOW_QUEUE = "indexBackFlowQueue";
//
//    protected static final ExecutorService executorService = Executors.newCachedThreadPool();
//
//    private static final Logger logger = LoggerFactory.getLogger(IndexBuildInterceptor.class);
//
//    /**
//     * 判断是否从索引build流程調用傳播過來的
//     *
//     * @param execContext
//     * @return
//     */
//    public static boolean isPropagateFromIndexBuild(IExecChainContext execContext) {
//        return execContext.getAttribute(KEY_INDEX_BACK_FLOW_QUEUE) != null;
//    }
//
//    public static IndexBackflowManager getIndeBackFlowQueue(IExecChainContext execContext) {
//        IndexBackflowManager buildResultQueue = execContext.getAttribute(KEY_INDEX_BACK_FLOW_QUEUE);
//        if (buildResultQueue == null) {
//            throw new IllegalStateException("execContext.getAttribute('" + KEY_INDEX_BACK_FLOW_QUEUE + "') is null");
//        }
//        return buildResultQueue;
//    }
//
//    @Override
//    protected ExecuteResult execute(final IExecChainContext execContext) throws Exception {
//        // 如果上游是join的话，那么在这里就需要将上游的join阶段标示为成功
//        final JoinPhaseStatus joinPhaseState = this.getPhaseStatus(execContext, FullbuildPhase.JOIN);
//        joinPhaseState.setAllComplete();
//        final ITabPartition ps = ExecChainContextUtils.getDependencyTablesMINPartition(execContext);
//        // ▼▼▼▼ 触发索引构建
//        final IndexBuildSourcePathCreator pathCreator = createIndexBuildSourceCreator(execContext, ps);
//        final int groupSize = execContext.getIndexShardCount();
//        if (groupSize < 1) {
//            return ExecuteResult.createFaild().setMessage(" build source ps:" + ps.getPt() + " is null");
//        }
////        SnapshotDomain domain = HttpConfigFileReader.getResource(execContext.getIndexName(), 0
////                , RunEnvironment.getSysRuntime(), ConfigFileReader.FILE_SCHEMA, ConfigFileReader.FILE_SOLR);
//        try {
//            if (!triggerIndexBuildJob(execContext.getIndexName(), pathCreator, ps, groupSize, execContext, null)) {
//                String msg = "index build faild,ps:" + ps.getPt() + ",groupsize:" + groupSize;
//                logger.info(msg);
//                return ExecuteResult.createFaild().setMessage(msg);
//            }
//        } catch (Exception e) {
//            throw new RuntimeException(e);
//        }
//        // ▲▲▲▲
//        return ExecuteResult.SUCCESS;
//    }
//
//    protected abstract IndexBuildSourcePathCreator createIndexBuildSourceCreator(final IExecChainContext execContext, ITabPartition ps);
//
//    /**
//     * 触发索引build
//     *
//     * @throws Exception
//     */
//    private boolean triggerIndexBuildJob(String indexName, IndexBuildSourcePathCreator pathCreator, final ITabPartition timepoint, int groupSize
//            , IExecChainContext execContext, SnapshotDomain domain) throws Exception {
//        Objects.requireNonNull(execContext, "execContext can not be null");
//
//        final ImportDataProcessInfo processInfo
//                = new ImportDataProcessInfo(execContext.getTaskId(), execContext.getIndexBuildFileSystem(), execContext.getZkClient());
//        processInfo.setBuildSourcePathCreator(pathCreator);
//        IIndexMetaData indexMetaData = null; //execContext.getIndexMetaData();
//        IIndexMetaData idexMeta = null;// execContext.getIndexMetaData();
//        String indexBuilder = idexMeta.getSchemaParseResult().getIndexBuilder();
//        if (indexBuilder != null) {
//            processInfo.setIndexBuilder(indexBuilder);
//        }
//        processInfo.setTimepoint(timepoint.getPt());
//        processInfo.setIndexName(indexName);
//        //processInfo.setIndexBuildSourcePathCreator(indexBuildSourcePathCreator);
//        processInfo.setLuceneVersion(indexMetaData.getLuceneVersion());
//        setBuildTableTitleItems(indexName, processInfo, execContext);
//        final ExecutorCompletionService<BuildResult> completionService = new ExecutorCompletionService<BuildResult>(executorService);
//        final BuildPhaseStatus phaseStatus = this.getPhaseStatus(execContext, FullbuildPhase.BUILD);
//        for (int grouIndex = 0; grouIndex < groupSize; grouIndex++) {
//            phaseStatus.getBuildSharedPhaseStatus(processInfo.getCoreName(grouIndex));
//        }
//        for (int grouIndex = 0; grouIndex < groupSize; grouIndex++) {
//            AbstractIndexBuildJob indexBuildJob = createRemoteIndexBuildJob(execContext, processInfo, grouIndex, domain, phaseStatus);
//            completionService.submit(indexBuildJob);
//        }
//        Future<BuildResult> result = completionService.poll(7, TimeUnit.HOURS);
//        if (result == null) {
//            logger.error("completionService.poll(7, TimeUnit.HOURS) is null");
//            return false;
//        }
////        DocCollection collection = ZkStateReader.getCollectionLive(execContext.getZkStateReader(), execContext.getIndexName());
////        if (collection == null) {
////            throw new IllegalStateException("indexName:" + execContext.getIndexName() + " collection can not be null in solr cluster");
////        }
//        final IndexBackflowManager indexBackflowManager = new IndexBackflowManager(null, execContext, this);
//        execContext.setAttribute(KEY_INDEX_BACK_FLOW_QUEUE, indexBackflowManager);
//        // 当有多个分组,為了實現同步執行索引回流，这里需要再启动一个线程
//        if ((groupSize - 1) > 0) {
//            // 里面会创建一个线程
//            createFeedbackJob(execContext, groupSize - 1, completionService, indexBackflowManager);
//        }
//        return processBuildResult(result, indexBackflowManager);
//    }
//
//    private boolean processBuildResult(Future<BuildResult> result, final IndexBackflowManager indexBackflowManager) throws InterruptedException, ExecutionException {
//        BuildResult buildResult;
//        buildResult = result.get();
//        if (!buildResult.isSuccess()) {
//            //logger.error("sourpath:" + buildResult.getHdfsSourcePath(indexBackflowManager.getExecContext()) + " build faild.");
//            // build失败
//            return false;
//        }
//        List<Replica> shardReplica = indexBackflowManager.getReplicByShard(buildResult.getGroupIndex());
//        for (Replica r : shardReplica) {
//            indexBackflowManager.addBackFlowTask(BuildResult.clone(buildResult).setReplica(r));
//            logger.info("group:" + buildResult.getGroupIndex() + ",indexsize:" + buildResult.getIndexSize() + ",node:" + r.getCoreUrl());
//        }
//        return true;
//    }
//
//    private void createFeedbackJob(IExecChainContext execContext, int groupSize, ExecutorCompletionService<BuildResult> completionService
//            , final IndexBackflowManager indexBackflowManager) {
//        final ExecutorService asynIndexBuildTask = Executors.newSingleThreadExecutor(new ThreadFactory() {
//
//            @Override
//            public Thread newThread(Runnable r) {
//                Thread t = new Thread(r);
//                t.setUncaughtExceptionHandler(new UncaughtExceptionHandler() {
//
//                    @Override
//                    public void uncaughtException(Thread t, Throwable e) {
//                        // 终止build任务执行
//                        logger.error(e.getMessage(), e);
//                        indexBackflowManager.shortCircuit();
//                    }
//                });
//                return t;
//            }
//        });
//        asynIndexBuildTask.execute(() -> {
//            execContext.rebindLoggingMDCParams();
//            try {
//                Future<BuildResult> result = null;
//                for (int grouIndex = 0; grouIndex < groupSize; grouIndex++) {
//                    result = completionService.poll(7, TimeUnit.HOURS);
//                    if (result == null) {
//                        continue;
//                    }
//                    if (!processBuildResult(result, indexBackflowManager)) {
//                        return;
//                    }
//                }
//            } catch (Exception e) {
//                throw new RuntimeException(e);
//            }
//        });
//    }
//
//    /**
//     * @param indexName
//     * @param processinfo
//     */
//    protected void setBuildTableTitleItems(String indexName, ImportDataProcessInfo processinfo, IExecChainContext execContext) {
//        throw new UnsupportedOperationException();
//    }
//
//    /**
//     * @param processinfo
//     * @param grouIndex
//     * @return
//     */
//    protected final AbstractIndexBuildJob createRemoteIndexBuildJob(final IExecChainContext execContext, ImportDataProcessInfo processinfo
//            , int grouIndex, SnapshotDomain domain, BuildPhaseStatus phaseStatus) {
//        // 暂时全部提交到32G机器上构建索引吧
//        IndexBuilderTriggerFactory indexBuilderFactory = execContext.getIndexBuilderFactory();
//        return new AbstractIndexBuildJob(execContext, processinfo, grouIndex, domain) {
//
//            @Override
//            protected BuildResult buildSliceIndex(String coreName, String timePoint, DumpJobStatus status, String outPath, String serviceName) throws Exception {
//                execContext.rebindLoggingMDCParams();
//                BuildSharedPhaseStatus buildStatus = phaseStatus.getBuildSharedPhaseStatus(coreName);
//                try {
//                    IRemoteJobTrigger buildJob = indexBuilderFactory.createBuildJob(execContext, timePoint, serviceName, String.valueOf(grouIndex), processinfo);
//                    buildJob.submitJob();
//                    BuildResult result = new BuildResult((groupNum), this.state);
//                    RunningStatus runningStatus = buildJob.getRunningStatus();
//                    while (!runningStatus.isComplete()) {
//                        Thread.sleep(3000);
//                        runningStatus = buildJob.getRunningStatus();
//                    }
//                    buildStatus.setFaild(!runningStatus.isSuccess());
//                    return result.setSuccess(runningStatus.isSuccess());
//                } catch (Throwable e) {
//                    buildStatus.setFaild(true);
//                    throw new RuntimeException(e);
//                } finally {
//                    buildStatus.setComplete(true);
//                }
//            }
//        };
//    }
//
//    @Override
//    public Set<FullbuildPhase> getPhase() {
//        return Collections.singleton(FullbuildPhase.BUILD);
//    }
//}
