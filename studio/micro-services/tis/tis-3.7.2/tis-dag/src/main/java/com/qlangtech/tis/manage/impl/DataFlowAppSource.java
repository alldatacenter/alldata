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
package com.qlangtech.tis.manage.impl;

import com.alibaba.citrus.turbine.Context;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.qlangtech.tis.assemble.FullbuildPhase;
import com.qlangtech.tis.compiler.streamcode.IDBTableNamesGetter;
import com.qlangtech.tis.datax.DataXJobSubmit;
import com.qlangtech.tis.datax.IDataxWriter;
import com.qlangtech.tis.exec.ExecuteResult;
import com.qlangtech.tis.exec.IExecChainContext;
import com.qlangtech.tis.exec.ITaskPhaseInfo;
import com.qlangtech.tis.fullbuild.IFullBuildContext;
import com.qlangtech.tis.fullbuild.indexbuild.IDumpTable;
import com.qlangtech.tis.fullbuild.indexbuild.RemoteTaskTriggers;
import com.qlangtech.tis.fullbuild.phasestatus.PhaseStatusCollection;
import com.qlangtech.tis.fullbuild.phasestatus.impl.DumpPhaseStatus;
import com.qlangtech.tis.fullbuild.phasestatus.impl.JoinPhaseStatus;
import com.qlangtech.tis.fullbuild.taskflow.*;
import com.qlangtech.tis.manage.IDataFlowAppSource;
import com.qlangtech.tis.manage.ISolrAppSource;
import com.qlangtech.tis.manage.common.Config;
import com.qlangtech.tis.manage.common.DagTaskUtils;
import com.qlangtech.tis.plugin.StoreResourceType;
import com.qlangtech.tis.plugin.ds.ColumnMetaData;
import com.qlangtech.tis.plugin.ds.IDataSourceFactoryGetter;
import com.qlangtech.tis.runtime.module.misc.IMessageHandler;
import com.qlangtech.tis.sql.parser.DAGSessionSpec;
import com.qlangtech.tis.sql.parser.DBNode;
import com.qlangtech.tis.sql.parser.SqlTaskNodeMeta;
import com.qlangtech.tis.sql.parser.er.*;
import com.qlangtech.tis.sql.parser.meta.DependencyNode;
import com.qlangtech.tis.sql.parser.tuple.creator.EntityName;
import com.qlangtech.tis.sql.parser.tuple.creator.IEntityNameGetter;
import com.qlangtech.tis.sql.parser.tuple.creator.IValChain;
import com.qlangtech.tis.sql.parser.tuple.creator.impl.TableTupleCreator;
import com.qlangtech.tis.sql.parser.tuple.creator.impl.TaskNodeTraversesCreatorVisitor;
import com.qlangtech.tis.workflow.pojo.WorkFlow;
import org.apache.commons.lang.StringUtils;
import org.jvnet.hudson.reactor.Milestone;
import org.jvnet.hudson.reactor.MilestoneImpl;
import org.jvnet.hudson.reactor.ReactorListener;
import org.jvnet.hudson.reactor.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Supplier;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2021-03-31 11:20
 */
public class DataFlowAppSource implements ISolrAppSource, IDataFlowAppSource {
    private static final Logger logger = LoggerFactory.getLogger("fullbuild");
    public static final File parent = new File(Config.getPluginCfgDir(), IFullBuildContext.NAME_APP_DIR);
    private final String dataflowName;
    private final WorkFlow dataflow;
    private final IFlatTableBuilder flatTableBuilder;
    private final IDataSourceFactoryGetter dsGetter;


    // protected static final ExecutorService executorService = Executors.newCachedThreadPool();

    //  protected static final ExecutorService executorService = Executors.newSingleThreadScheduledExecutor();

    public DataFlowAppSource(WorkFlow dataflow, IDataxWriter writer) {
        this.dataflowName = dataflow.getName();
        this.dataflow = dataflow;
        if (!(writer instanceof IFlatTableBuilder)) {
            throw new IllegalStateException(writer.getClass() + " must be type of " + IFlatTableBuilder.class.getSimpleName());
        }
        if (!(writer instanceof IDataSourceFactoryGetter)) {
            throw new IllegalStateException(writer.getClass() + " must be type of " + IDataSourceFactoryGetter.class.getSimpleName());
        }
        this.flatTableBuilder = (IFlatTableBuilder) writer;
        this.dsGetter = (IDataSourceFactoryGetter) writer;
    }

    public static ExecutorService createExecutorService(IExecChainContext execChainContext) {
        int nThreads = 1;
        return new ThreadPoolExecutor(
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
    }

    @Override
    public StoreResourceType getResType() {
        return StoreResourceType.DataFlow;
    }

    public Integer getDfId() {
        return this.dataflow.getId();
    }


    @Override
    public boolean isExcludeFacadeDAOSupport() {
        try {
            SqlTaskNodeMeta.SqlDataFlowTopology wfTopology = SqlTaskNodeMeta.getSqlDataFlowTopology(dataflowName);
            return wfTopology.isSingleDumpTableDependency();
        } catch (Exception e) {
            throw new RuntimeException("dataflow:" + this.dataflowName, e);
        }
    }

    // @Override
    public Map<IEntityNameGetter, List<IValChain>> getTabTriggerLinker() {

        try {
            SqlTaskNodeMeta.SqlDataFlowTopology topology = SqlTaskNodeMeta.getSqlDataFlowTopology(dataflowName);

            TableTupleCreator finalTableNode = topology.parseFinalSqlTaskNode();
            ERRules erR = getErRules();
            TaskNodeTraversesCreatorVisitor visitor = new TaskNodeTraversesCreatorVisitor(erR);
            finalTableNode.accept(visitor);

            Map<IEntityNameGetter, List<IValChain>> tabTriggers = visitor.getTabTriggerLinker();
            return tabTriggers;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private boolean containErRules() {
        return getOptionalErRules().isPresent();
    }

    private ERRules getErRules() {

        Optional<ERRules> erRules = getOptionalErRules();
        if (!erRules.isPresent()) {
            throw new IllegalStateException("topology:" + dataflowName + " relevant erRule can not be null");
        }
        return erRules.get();

    }

    private Optional<ERRules> getOptionalErRules() {
        try {
            SqlTaskNodeMeta.SqlDataFlowTopology topology = SqlTaskNodeMeta.getSqlDataFlowTopology(this.dataflowName);

            if (topology.isSingleTableModel()) {
                Optional<ERRules> erRule = ERRules.getErRule(this.dataflowName);
                if (!erRule.isPresent()) {
                    ERRules.createDefaultErRule(topology);
                }
            }

            return ERRules.getErRule(this.dataflowName);
        } catch (Exception e) {
            throw new RuntimeException("dataflowName:" + this.dataflowName, e);
        }
    }

    //    @Override
    public List<PrimaryTableMeta> getPrimaryTabs() {
        try {
            return getErRules().getPrimaryTabs();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public ExecuteResult getProcessDataResults(IExecChainContext execChainContext
            , ISingleTableDumpFactory singleTableDumpFactory, IDataProcessFeedback dataProcessFeedback, ITaskPhaseInfo taskPhaseInfo) throws Exception {
        // 执行工作流数据结构
        SqlTaskNodeMeta.SqlDataFlowTopology topology = SqlTaskNodeMeta.getSqlDataFlowTopology(dataflowName);
        DAGSessionSpec dagSessionSpec = topology.getDAGSessionSpec();

        // 取得workflowdump需要依赖的表
        Collection<DependencyNode> tables = topology.getDumpNodes();
        StringBuffer dumps = new StringBuffer("dependency table:\n");
        dumps.append("\t\t=======================\n");
        for (DependencyNode t : tables) {
            dumps.append("\t\t").append(t.getDbName()).append(".").append(t.getName())
                    .append("[").append(t.getTabid()).append(",").append("] \n");
        }
        dumps.append("\t\t=======================\n");
        logger.info(dumps.toString());
        // 将所有的表的状态先初始化出来
        DumpPhaseStatus dumpPhaseStatus = taskPhaseInfo.getPhaseStatus(execChainContext, FullbuildPhase.FullDump);
        final ExecutorService executorService = createExecutorService(execChainContext);
        RemoteTaskTriggers trigger = new RemoteTaskTriggers(executorService);
        execChainContext.setTskTriggers(trigger);

        for (DependencyNode dump : topology.getDumpNodes()) {
            singleTableDumpFactory.createSingleTableDump(trigger, dump, false, /* isHasValidTableDump */
                    "tableDump.getPt()", execChainContext.getZkClient()
                    , execChainContext, dumpPhaseStatus, taskPhaseInfo, dagSessionSpec);
        }

        DagTaskUtils.createTasks(execChainContext, taskPhaseInfo, dagSessionSpec, trigger);

        JoinPhaseStatus joinPhaseStatus = taskPhaseInfo.getPhaseStatus(execChainContext, FullbuildPhase.JOIN);

        Supplier<IPrimaryTabFinder> primaryTabFinder
                = () -> DataFlowAppSource.this.containErRules()
                ? DataFlowAppSource.this.getErRules()
                : new IPrimaryTabFinder() {
        };

        return flatTableBuilder.startTask((context) -> {
            DataflowTask process = null;
            for (SqlTaskNodeMeta pnode : topology.getNodeMetas()) {
                /**
                 * ***********************************
                 * 构建宽表构建任务节点
                 * ************************************
                 */
                process = flatTableBuilder.createTask(pnode, false//StringUtils.equals(fNode.getId(), pnode.getId())
                        , execChainContext, context, joinPhaseStatus.getTaskStatus(pnode.getExportName())
                        , this.dsGetter, primaryTabFinder);

                dagSessionSpec.put(pnode.getId(), new TaskAndMilestone(process));
            }
            final ExecuteResult faildResult = executeDAG(executorService, execChainContext, topology, dataProcessFeedback, dagSessionSpec.getTaskMap());
            return faildResult;
        });
    }


    private ExecuteResult executeDAG(ExecutorService executorService, IExecChainContext execChainContext, SqlTaskNodeMeta.SqlDataFlowTopology topology, IDataProcessFeedback dataProcessFeedback
            , Map<String, TaskAndMilestone> taskMap) {
        final ExecuteResult[] faildResult = new ExecuteResult[1];

        try {
            TISReactor reactor = new TISReactor(execChainContext, taskMap);
            StringBuffer dagSessionSpec = topology.getDAGSessionSpec().buildSpec();
            logger.info("dagSessionSpec:" + dagSessionSpec);

            //  final PrintWriter w = new PrintWriter(sw, true);
            ReactorListener listener = new ReactorListener() {
                @Override
                public void onAttained(Milestone milestone) {
                    MilestoneImpl m = (MilestoneImpl) milestone;
                    String mId = m.toString();
                    if (!StringUtils.startsWith(mId, TaskAndMilestone.MILESTONE_PREFIX)) {
                        AdapterTask.createTaskWorkStatus(execChainContext).put(mId, true);
                    }
                }

                // TODO: Does it really needs handlers to be synchronized?
                @Override
                public synchronized void onTaskCompleted(Task t) {
                    processTaskResult(execChainContext, (TISReactor.TaskImpl) t, dataProcessFeedback, new ITaskResultProcessor() {
                        @Override
                        public void process(DumpPhaseStatus dumpPhase, TISReactor.TaskImpl task) {
                        }

                        @Override
                        public void process(JoinPhaseStatus joinPhase, TISReactor.TaskImpl task) {
                        }
                    });
                }

                @Override
                public synchronized void onTaskFailed(Task t, Throwable err, boolean fatal) {
                    // w.println("Failed " + t.getDisplayName() + " with " + err);
                    processTaskResult(execChainContext, (TISReactor.TaskImpl) t, dataProcessFeedback, new ITaskResultProcessor() {
                        @Override
                        public void process(DumpPhaseStatus dumpPhase, TISReactor.TaskImpl task) {
                            dataProcessFeedback.reportDumpTableStatusError(execChainContext, dumpPhase, task);
                        }

                        @Override
                        public void process(JoinPhaseStatus joinPhase, TISReactor.TaskImpl task) {
                            JoinPhaseStatus.JoinTaskStatus stat = joinPhase.getTaskStatus(task.getIdentityName());
                            // statReceiver.reportBuildIndexStatErr(execContext.getTaskId(),task.getIdentityName());
                            stat.setWaiting(false);
                            stat.setFaild(true);
                            stat.setComplete(true);
                        }
                    });
                }
            };


            // 执行DAG地调度
            reactor.execute(executorService, reactor.buildSession(dagSessionSpec.toString()), listener, new ReactorListener() {

                @Override
                public void onTaskCompleted(Task t) {
                    // dumpPhaseStatus.isComplete();
                    // joinPhaseStatus.isComplete();
                }

                @Override
                public void onTaskFailed(Task t, Throwable err, boolean fatal) {
                    logger.error(t.getDisplayName(), err);
                    faildResult[0] = ExecuteResult.createFaild().setMessage("status.runningStatus.isComplete():" + err.getMessage());
                }
            });
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            executorService.shutdown();
        }
        return faildResult[0];
    }

    private void processTaskResult(IExecChainContext execContext, TISReactor.TaskImpl t, IDataProcessFeedback dataProcessFeedback, ITaskResultProcessor resultProcessor) {
        TISReactor.TaskImpl task = t;
        PhaseStatusCollection pstats = dataProcessFeedback.getPhaseStatusSet(execContext);// TrackableExecuteInterceptor.taskPhaseReference.get(execContext.getTaskId());
        if (pstats != null) {
            switch (task.getPhase()) {
                case FullDump:
                    // pstats.getDumpPhase()
                    // IncrStatusUmbilicalProtocolImpl statReceiver = IncrStatusUmbilicalProtocolImpl.getInstance();
                    // statReceiver.reportDumpTableStatusError(execContext.getTaskId(), task.getIdentityName());
                    pstats.getDumpPhase().isComplete();
                    resultProcessor.process(pstats.getDumpPhase(), task);
                    return;
                case JOIN:
                    // JoinPhaseStatus.JoinTaskStatus stat
                    // = pstats.getJoinPhase().getTaskStatus(task.getIdentityName());
                    // //statReceiver.reportBuildIndexStatErr(execContext.getTaskId(),task.getIdentityName());
                    // stat.setWaiting(false);
                    // stat.setFaild(true);
                    // stat.setComplete(true);
                    pstats.getJoinPhase().isComplete();
                    resultProcessor.process(pstats.getJoinPhase(), task);
                    return;
                default:
                    throw new IllegalStateException("taskphase:" + task.getPhase() + " is illegal");
            }
        }
    }

    // @Override
//    public IERRules getERRule() {
//        try {
//            return this.getErRules();
//        } catch (Exception e) {
//            throw new RuntimeException(e);
//        }
//    }

    /**
     * 取得依赖的db->table映射关系
     *
     * @return
     */
    //  @Override
    public Map<DBNode, List<String>> getDependencyTables(IDBTableNamesGetter dbTableNamesGetter) {
        try {
            SqlTaskNodeMeta.SqlDataFlowTopology topology = SqlTaskNodeMeta.getSqlDataFlowTopology(dataflowName);

            Map<DBNode, List<String>> /* tables */dbNameMap = Maps.newHashMap();
            List<String> tables = null;
            DBNode dbNode = null;
            for (DependencyNode node : topology.getDumpNodes()) {
                dbNode = new DBNode(node.getDbName(), Integer.parseInt(node.getDbid()));
                node.parseEntityName();
                tables = dbNameMap.get(dbNode);
                if (tables == null) {
                    // DB 下的全部table
                    tables = Lists.newArrayList();
                    dbNameMap.put(dbNode, tables);
                }
                tables.add(node.getName());
            }
            for (Map.Entry<DBNode, List<String>> /* tables */
                    entry : dbNameMap.entrySet()) {
                entry.setValue(dbTableNamesGetter.getTableNames(entry.getKey().getDbId(), entry.getValue()));
            }
            return dbNameMap;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    interface ITaskResultProcessor {

        void process(DumpPhaseStatus dumpPhase, TISReactor.TaskImpl task);

        void process(JoinPhaseStatus joinPhase, TISReactor.TaskImpl task);
    }

    @Override
    public IPrimaryTabFinder getPrimaryTabFinder() {
        Optional<ERRules> erRule = ERRules.getErRule(dataflowName);
        IPrimaryTabFinder pTabFinder = null;
        if (!erRule.isPresent()) {
            pTabFinder = new DftTabFinder();
        } else {
            pTabFinder = erRule.get();
        }
        return pTabFinder;
    }


    static class DftTabFinder implements IPrimaryTabFinder {
        @Override
        public Optional<TableMeta> getPrimaryTab(IDumpTable entityName) {
            return Optional.empty();
        }

        @Override
        public final Map<EntityName, TabFieldProcessor> getTabFieldProcessorMap() {
            return Collections.emptyMap();
        }
    }

    @Override
    public EntityName getTargetEntity() {

        try {
            SqlTaskNodeMeta.SqlDataFlowTopology workflowDetail = SqlTaskNodeMeta.getSqlDataFlowTopology(dataflowName);
            Objects.requireNonNull(workflowDetail, "workflowDetail can not be null");
            EntityName targetEntity = null;
            if (workflowDetail.isSingleTableModel()) {
                DependencyNode dumpNode = workflowDetail.getDumpNodes().get(0);
                targetEntity = dumpNode.parseEntityName();
            } else {
                SqlTaskNodeMeta finalN = workflowDetail.getFinalNode();
                targetEntity = EntityName.parse(finalN.getExportName());
            }
            return targetEntity;
        } catch (Exception e) {
            throw new RuntimeException(dataflowName, e);
        }
    }


    @Override
    public List<ColumnMetaData> reflectCols() {
        try {
            SqlTaskNodeMeta.SqlDataFlowTopology dfTopology = SqlTaskNodeMeta.getSqlDataFlowTopology(dataflowName);
            return dfTopology.getFinalTaskNodeCols();
        } catch (Exception e) {
            throw new RuntimeException("dataflowName:" + dataflowName, e);
        }
    }


    @Override
    public boolean triggerFullIndexSwapeValidate(IMessageHandler module, Context context) {
        try {
            SqlTaskNodeMeta.SqlDataFlowTopology topology = SqlTaskNodeMeta.getSqlDataFlowTopology(dataflowName);
            Objects.requireNonNull(topology, "topology:" + dataflowName + " relevant topology can not be be null");

            Optional<ERRules> erRule = ERRules.getErRule(dataflowName);// module.getErRules(dataflowName);
            if (!topology.isSingleTableModel()) {
                if (!erRule.isPresent()) {
                    module.addErrorMessage(context, "请为数据流:[" + dataflowName + "]定义ER Rule");
                    return false;
                } else {
                    ERRules erRules = erRule.get();
                    List<PrimaryTableMeta> pTabs = erRules.getPrimaryTabs();
                    Optional<PrimaryTableMeta> prTableMeta = pTabs.stream().findFirst();
                    if (!TableMeta.hasValidPrimayTableSharedKey(prTableMeta.isPresent() ? Optional.of(prTableMeta.get()) : Optional.empty())) {
                        module.addErrorMessage(context, "请为数据流:[" + dataflowName + "]定义ERRule 选择主表并且设置分区键");
                        return false;
                    }
                }
            }
            return true;
        } catch (Exception e) {
            throw new RuntimeException(this.dataflowName, e);
        }
    }
}
