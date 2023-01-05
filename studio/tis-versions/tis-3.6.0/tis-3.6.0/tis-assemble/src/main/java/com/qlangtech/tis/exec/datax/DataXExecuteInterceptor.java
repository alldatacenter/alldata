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

package com.qlangtech.tis.exec.datax;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.qlangtech.tis.assemble.FullbuildPhase;
import com.qlangtech.tis.datax.DataXJobSubmit;
import com.qlangtech.tis.datax.IDataXBatchPost;
import com.qlangtech.tis.datax.IDataxReader;
import com.qlangtech.tis.datax.IDataxWriter;
import com.qlangtech.tis.datax.impl.DataXCfgGenerator;
import com.qlangtech.tis.datax.impl.DataxProcessor;
import com.qlangtech.tis.exec.ExecuteResult;
import com.qlangtech.tis.exec.IExecChainContext;
import com.qlangtech.tis.exec.impl.TrackableExecuteInterceptor;
import com.qlangtech.tis.fullbuild.indexbuild.IRemoteTaskTrigger;
import com.qlangtech.tis.fullbuild.phasestatus.impl.AbstractChildProcessStatus;
import com.qlangtech.tis.fullbuild.phasestatus.impl.DumpPhaseStatus;
import com.qlangtech.tis.fullbuild.phasestatus.impl.JoinPhaseStatus;
import com.qlangtech.tis.fullbuild.taskflow.DataflowTask;
import com.qlangtech.tis.fullbuild.taskflow.DumpTask;
import com.qlangtech.tis.fullbuild.taskflow.TISReactor;
import com.qlangtech.tis.plugin.ds.ISelectedTab;
import com.qlangtech.tis.realtime.yarn.rpc.IncrStatusUmbilicalProtocol;
import com.qlangtech.tis.realtime.yarn.rpc.impl.AdapterStatusUmbilicalProtocol;
import com.qlangtech.tis.rpc.server.IncrStatusUmbilicalProtocolImpl;
import com.tis.hadoop.rpc.ITISRpcService;
import com.tis.hadoop.rpc.RpcServiceReference;
import org.apache.commons.collections.CollectionUtils;
import org.jvnet.hudson.reactor.ReactorListener;
import org.jvnet.hudson.reactor.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

//import com.qlangtech.tis.fullbuild.indexbuild.RunningStatus;

/**
 * DataX 执行器
 *
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2021-04-27 15:42
 **/
public class DataXExecuteInterceptor extends TrackableExecuteInterceptor {
    private static final Logger logger = LoggerFactory.getLogger(DataXExecuteInterceptor.class);

    @Override
    protected ExecuteResult execute(IExecChainContext execChainContext) throws Exception {


        final Map<String, TISReactor.TaskAndMilestone> taskMap = Maps.newHashMap();
        RpcServiceReference statusRpc = getDataXExecReporter();

        DataxProcessor appSource = execChainContext.getAppSource();
        IRemoteTaskTrigger jobTrigger = null;
        // RunningStatus runningStatus = null;

        List<IRemoteTaskTrigger> triggers = Lists.newArrayList();


        IDataxReader reader = appSource.getReader(null);

        List<ISelectedTab> selectedTabs = reader.getSelectedTabs();
        // Map<String, IDataxProcessor.TableAlias> tabAlias = appSource.getTabAlias();

        IDataxWriter writer = appSource.getWriter(null);

        DataXCfgGenerator.GenerateCfgs cfgFileNames = appSource.getDataxCfgFileNames(null);
        if (CollectionUtils.isEmpty(cfgFileNames.getDataXCfgFiles())) {
            throw new IllegalStateException("dataX cfgFileNames can not be empty");
        }

        if (writer instanceof IDataXBatchPost) {
            IDataXBatchPost batchPostTask = (IDataXBatchPost) writer;

            for (ISelectedTab entry : selectedTabs) {
                IRemoteTaskTrigger postTaskTrigger = batchPostTask.createPostTask(execChainContext, entry, cfgFileNames);
                addJoinTask(execChainContext, taskMap, triggers, postTaskTrigger);

                IRemoteTaskTrigger preExec = batchPostTask.createPreExecuteTask(execChainContext, entry);
                if (preExec != null) {
                    addDumpTask(execChainContext, taskMap, preExec, triggers);
                }
            }
        }


        DataXJobSubmit.InstanceType expectDataXJobSumit = getDataXTriggerType();
        Optional<DataXJobSubmit> jobSubmit = DataXJobSubmit.getDataXJobSubmit(expectDataXJobSumit);
        // 如果分布式worker ready的话
        if (!jobSubmit.isPresent()) {
            throw new IllegalStateException("can not find expect jobSubmit by type:" + expectDataXJobSumit);
        }

        DataXJobSubmit submit = jobSubmit.get();
        final DataXJobSubmit.IDataXJobContext dataXJobContext = submit.createJobContext(execChainContext);
        Objects.requireNonNull(dataXJobContext, "dataXJobContext can not be null");
        int nThreads = 1;
        final ExecutorService executorService = new ThreadPoolExecutor(
                nThreads, nThreads, 0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>(DataXJobSubmit.MAX_TABS_NUM_IN_PER_JOB),
                new ThreadFactory() {
                    @Override
                    public Thread newThread(Runnable r) {
                        Thread t = new Thread(() -> {
                            execChainContext.rebindLoggingMDCParams();
                            r.run();
                        });
                        t.setUncaughtExceptionHandler((thread, ex) -> {
                            logger.error("DataX Name:" + execChainContext.getIndexName()
                                    + ",taskid:" + execChainContext.getTaskId() + " has been canceled", ex);
                        });
                        return t;
                    }
                });
        try {
            List<String> dataXCfgsOfTab = null;
            List<String> dptTasks = null;
            for (ISelectedTab entry : selectedTabs) {

                dataXCfgsOfTab = cfgFileNames.getDataXTaskDependencies(entry.getName());
                dptTasks = Collections.emptyList();
                if (taskMap.get(IDataXBatchPost.getPreExecuteTaskName(entry)) != null) {
                    // 说明有前置任务
                    dptTasks = Collections.singletonList(IDataXBatchPost.getPreExecuteTaskName(entry));
                }

                for (String fileName : dataXCfgsOfTab) {


                    jobTrigger = createDataXJob(dataXJobContext, submit
                            , expectDataXJobSumit, statusRpc, appSource, fileName, dptTasks);


                    addDumpTask(execChainContext, taskMap, jobTrigger, triggers);
                }
            }

            // for (File fileName : cfgFileNames) {

            //StatusRpcClient.AssembleSvcCompsite svc = statusRpc.get();
            // 将任务注册，可供页面展示
//                svc.reportDumpJobStatus(false, false, true, execChainContext.getTaskId()
//                        , fileName.getName(), 0, 0);

            //}

//            logger.info("trigger dataX jobs by mode:{},with:{}", this.getDataXTriggerType()
//                    , cfgFileNames.stream().map((f) -> f.getName()).collect(Collectors.joining(",")));
//            for (IRemoteJobTrigger t : triggers) {
//                t.submitJob();
//            }


            // example: "->a ->b a,b->c"
            String dagSessionSpec = triggers.stream().map((trigger) -> {
                List<String> dpts = Objects.requireNonNull(trigger.getTaskDependencies()
                        , "trigger:" + trigger.getTaskName() + " relevant task dependencies can not be null");
                return dpts.stream().collect(Collectors.joining(",")) + "->" + trigger.getTaskName();
            }).collect(Collectors.joining(" "));
            logger.info("dataX:{} of dagSessionSpec:{}", execChainContext.getIndexName(), dagSessionSpec);


            ExecuteResult[] faildResult = new ExecuteResult[]{ExecuteResult.createSuccess()};


            this.executeDAG(executorService, execChainContext, dagSessionSpec, taskMap, new ReactorListener() {

                @Override
                public void onTaskCompleted(Task t) {
                    // dumpPhaseStatus.isComplete();
                    // joinPhaseStatus.isComplete();
                }

                @Override
                public void onTaskFailed(Task t, Throwable err, boolean fatal) {
                    logger.error(t.getDisplayName(), err);
                    faildResult[0] = ExecuteResult.createFaild().setMessage("status.runningStatus.isComplete():" + err.getMessage());
                    if (err instanceof InterruptedException) {
                        logger.warn("DataX Name:{},taskid:{} has been canceled"
                                , execChainContext.getIndexName(), execChainContext.getTaskId());
                        // this job has been cancel, trigger from TisServlet.doDelete()
                        for (IRemoteTaskTrigger tt : triggers) {
                            try {
                                tt.cancel();
                            } catch (Throwable ex) {
                            }
                        }
                    }
                }
            });

            for (IRemoteTaskTrigger trigger : triggers) {
                if (trigger.isAsyn()) {
                    execChainContext.addAsynSubJob(new IExecChainContext.AsynSubJob(trigger.getAsynJobName()));
                }
            }
            return faildResult[0];
        } finally {
            try {
                dataXJobContext.destroy();
            } catch (Throwable e) {
                logger.error(e.getMessage(), e);
            }
            executorService.shutdown();
        }
    }

    private void addDumpTask(IExecChainContext execChainContext, Map<String, TISReactor.TaskAndMilestone> taskMap
            , IRemoteTaskTrigger jobTrigger, List<IRemoteTaskTrigger> triggers) {
        triggers.add(jobTrigger);
        DumpPhaseStatus dumpStatus = this.getPhaseStatus(execChainContext, FullbuildPhase.FullDump);
        taskMap.put(jobTrigger.getTaskName()
                , new TISReactor.TaskAndMilestone(DumpTask.createDumpTask(jobTrigger, dumpStatus.getTable(jobTrigger.getTaskName()))));
    }

    private void addJoinTask(IExecChainContext execChainContext, Map<String, TISReactor.TaskAndMilestone> taskMap
            , List<IRemoteTaskTrigger> triggers, IRemoteTaskTrigger postTaskTrigger) {
        JoinPhaseStatus phaseStatus = this.getPhaseStatus(execChainContext, FullbuildPhase.JOIN);
        triggers.add(postTaskTrigger);
        JoinPhaseStatus.JoinTaskStatus taskStatus = phaseStatus.getTaskStatus(postTaskTrigger.getTaskName());
        taskStatus.setWaiting(true);
        taskMap.put(postTaskTrigger.getTaskName()
                , new TISReactor.TaskAndMilestone(createJoinTask(postTaskTrigger, taskStatus)));
    }


    private void executeDAG(ExecutorService executorService, IExecChainContext execChainContext, String dagSessionSpec
            , Map<String, TISReactor.TaskAndMilestone> taskMap, ReactorListener reactorListener) {
        try {
            TISReactor reactor = new TISReactor(execChainContext, taskMap);
            // String dagSessionSpec = topology.getDAGSessionSpec();
            logger.info("dagSessionSpec:" + dagSessionSpec);

            //  final PrintWriter w = new PrintWriter(sw, true);
            ReactorListener listener = new ReactorListener() {
                // TODO: Does it really needs handlers to be synchronized?
//                @Override
//                public synchronized void onTaskStarted(Task t) {
//            //        w.println("Started " + t.getDisplayName());
//                }

                @Override
                public synchronized void onTaskCompleted(Task t) {
                    //   w.println("Ended " + t.getDisplayName());
//                    processTaskResult(execChainContext, (TISReactor.TaskImpl) t, new ITaskResultProcessor() {
//                        @Override
//                        public void process(DumpPhaseStatus dumpPhase, TISReactor.TaskImpl task) {
//                        }
//
//                        @Override
//                        public void process(JoinPhaseStatus joinPhase, TISReactor.TaskImpl task) {
//                        }
//                    });
                }

                @Override
                public synchronized void onTaskFailed(Task t, Throwable err, boolean fatal) {
                    // w.println("Failed " + t.getDisplayName() + " with " + err);
                }
//
//                @Override
//                public synchronized void onAttained(Milestone milestone) {
//                    w.println("Attained " + milestone);
//                }
            };


            // 执行DAG地调度
            //executorService
            reactor.execute(executorService, reactor.buildSession(dagSessionSpec), listener, reactorListener);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    protected IRemoteTaskTrigger createDataXJob(
            DataXJobSubmit.IDataXJobContext execChainContext
            , DataXJobSubmit submit, DataXJobSubmit.InstanceType expectDataXJobSumit, RpcServiceReference statusRpc
            , DataxProcessor appSource, String fileName, List<String> dependencyTasks) {

        if (expectDataXJobSumit == DataXJobSubmit.InstanceType.DISTRIBUTE) {
            IncrStatusUmbilicalProtocolImpl statCollect = IncrStatusUmbilicalProtocolImpl.getInstance();
            // 将指标纬度统计向注册到内存中，下一步可提供给DataX终止功能使用
            statCollect.getAppSubExecNodeMetrixStatus(execChainContext.getTaskContext().getIndexName(), fileName);
        }
        return submit.createDataXJob(execChainContext, statusRpc, appSource, fileName, dependencyTasks);
    }

    protected DataXJobSubmit.InstanceType getDataXTriggerType() {
//        DataXJobWorker jobWorker = DataXJobWorker.getJobWorker(DataXJobWorker.K8S_DATAX_INSTANCE_NAME);
//        boolean dataXWorkerServiceOnDuty = jobWorker != null && jobWorker.inService();//.isDataXWorkerServiceOnDuty();
//        return dataXWorkerServiceOnDuty ? DataXJobSubmit.InstanceType.DISTRIBUTE : DataXJobSubmit.InstanceType.LOCAL;
        return DataXJobSubmit.getDataXTriggerType();
    }

    protected RpcServiceReference getDataXExecReporter() {
        IncrStatusUmbilicalProtocolImpl statusServer = IncrStatusUmbilicalProtocolImpl.getInstance();
        IncrStatusUmbilicalProtocol statReceiveSvc = new AdapterStatusUmbilicalProtocol() {
            @Override
            public void reportDumpTableStatus(DumpPhaseStatus.TableDumpStatus tableDumpStatus) {
//                statusServer.reportDumpTableStatus(tableDumpStatus.getTaskid(), tableDumpStatus.isComplete()
//                        , tableDumpStatus.isWaiting(), tableDumpStatus.isFaild(), tableDumpStatus.getName());

                statusServer.reportDumpTableStatus(tableDumpStatus);
            }
        };
        AtomicReference<ITISRpcService> ref = new AtomicReference<>();
        ref.set(new DataXAssembleSvcCompsite(statReceiveSvc));
        return new RpcServiceReference(ref, () -> {
        });
    }

    @Override
    public Set<FullbuildPhase> getPhase() {
        return Sets.newHashSet(FullbuildPhase.FullDump, FullbuildPhase.JOIN);
    }

    public static DataflowTask createJoinTask(
            IRemoteTaskTrigger jobTrigger, JoinPhaseStatus.JoinTaskStatus taskStatus) {
        return new JoinTask(jobTrigger, taskStatus);
    }

    private static class JoinTask extends DumpTask {


        public JoinTask(IRemoteTaskTrigger jobTrigger, AbstractChildProcessStatus taskStatus) {
            super(jobTrigger, taskStatus);
        }


        @Override
        public FullbuildPhase phase() {
            return FullbuildPhase.JOIN;
        }

    }
}
