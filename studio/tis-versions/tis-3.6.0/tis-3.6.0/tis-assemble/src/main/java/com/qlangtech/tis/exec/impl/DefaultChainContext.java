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
package com.qlangtech.tis.exec.impl;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.qlangtech.tis.assemble.FullbuildPhase;
import com.qlangtech.tis.cloud.ITISCoordinator;
import com.qlangtech.tis.datax.IDataXBatchPost;
import com.qlangtech.tis.datax.IDataxWriter;
import com.qlangtech.tis.datax.impl.DataxProcessor;
import com.qlangtech.tis.exec.ExecChainContextUtils;
import com.qlangtech.tis.exec.ExecutePhaseRange;
import com.qlangtech.tis.exec.IExecChainContext;
import com.qlangtech.tis.fs.ITISFileSystem;
import com.qlangtech.tis.fullbuild.IFullBuildContext;
import com.qlangtech.tis.fullbuild.phasestatus.PhaseStatusCollection;
import com.qlangtech.tis.fullbuild.servlet.IRebindableMDC;
import com.qlangtech.tis.job.common.JobCommon;
import com.qlangtech.tis.manage.IAppSource;
import com.qlangtech.tis.manage.IBasicAppSource;
import com.qlangtech.tis.manage.common.DagTaskUtils;
import com.qlangtech.tis.offline.DataxUtils;
import com.qlangtech.tis.order.center.IAppSourcePipelineController;
import com.qlangtech.tis.order.center.IParamContext;
import com.qlangtech.tis.order.center.IndexSwapTaskflowLauncher;
import com.qlangtech.tis.sql.parser.TabPartitions;
import com.qlangtech.tis.workflow.pojo.WorkFlowBuildHistory;
import org.apache.commons.lang.StringUtils;

import java.util.*;

//import com.qlangtech.tis.fullbuild.workflow.SingleTableDump;

//import com.qlangtech.tis.exec.IIndexMetaData;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2015年12月15日 下午4:39:38
 */
public class DefaultChainContext implements IExecChainContext {

    private final String ps;

    private ITISCoordinator zkClient;

    private ITISFileSystem indexBuildFileSystem;

    private final IParamContext httpExecContext;

    // 执行阶段跨度
    private ExecutePhaseRange executePhaseRange;

    //private IIndexMetaData indexMetaData;

    //   private TableDumpFactory fs2Table;

    private IRebindableMDC mdcParamContext;

    // private IndexBuilderTriggerFactory indexBuilderTriggerFactory;
    private IAppSourcePipelineController appSourcePipelineController;

    public final List<AsynSubJob> asynSubJobs = Lists.newCopyOnWriteArrayList();

    @Override
    public List<AsynSubJob> getAsynSubJobs() {
        return this.asynSubJobs;
    }

    public void addAsynSubJob(AsynSubJob jobName) {
        this.asynSubJobs.add(jobName);
    }

    public boolean containAsynJob() {
        return !this.asynSubJobs.isEmpty();
    }

    @Override
    public int getTaskId() {
        Integer taskid = this.getAttribute(JobCommon.KEY_TASK_ID);
        Objects.requireNonNull(taskid, "taskid can not be null");
        return taskid;
    }

//    @Override
//    public IndexBuilderTriggerFactory getIndexBuilderFactory() {
//        return this.indexBuilderTriggerFactory;
//    }
//
//    public void setIndexBuilderTriggerFactory(IndexBuilderTriggerFactory indexBuilderTriggerFactory) {
//        if (indexBuilderTriggerFactory != null) {
//            this.indexBuilderTriggerFactory = indexBuilderTriggerFactory;
//            this.setIndexBuildFileSystem(indexBuilderTriggerFactory.getFileSystem());
//        }
//    }

    public void setMdcParamContext(IRebindableMDC mdcParamContext) {
        this.mdcParamContext = mdcParamContext;
    }

    public int getIndexShardCount() {
        try {
            return this.getInt(IFullBuildContext.KEY_APP_SHARD_COUNT);
        } catch (Exception e) {
            throw new RuntimeException(IFullBuildContext.KEY_APP_SHARD_COUNT + " is illegal", e);
        }
    }

    @Override
    public void rebindLoggingMDCParams() {
        if (mdcParamContext == null) {
            throw new IllegalStateException("must execute method 'setMdcParamContext'");
        }
        mdcParamContext.rebind();
    }

//    public void setTableDumpFactory(TableDumpFactory factory) {
//        this.fs2Table = factory;
//    }

//    @Override
//    public IIndexMetaData getIndexMetaData() {
//        return this.indexMetaData;
//    }

    //public void setIndexMetaData(IIndexMetaData indexMetaData) {
    //   this.indexMetaData = indexMetaData;
    //}

    @Override
    public ExecutePhaseRange getExecutePhaseRange() {
        if (this.executePhaseRange == null) {

            DataxProcessor appSource = this.getAppSource();
            IDataxWriter writer = appSource.getWriter(null);
            if (writer instanceof IDataXBatchPost) {
                this.executePhaseRange = ((IDataXBatchPost) writer).getPhaseRange();
            }else{
                this.executePhaseRange = new ExecutePhaseRange(FullbuildPhase.FullDump, FullbuildPhase.FullDump);
            }
//            String start = StringUtils.defaultIfEmpty(this.getString(COMPONENT_START), FullbuildPhase.FullDump.getName());
//            String end = StringUtils.defaultIfEmpty(this.getString(COMPONENT_END), FullbuildPhase.IndexBackFlow.getName());
//            this.executePhaseRange = new ExecutePhaseRange(FullbuildPhase.parse(start), FullbuildPhase.parse(end));
        }
        return this.executePhaseRange;
    }

    private void setIndexBuildFileSystem(ITISFileSystem fileSystem) {
        Objects.requireNonNull(fileSystem, "indexBuild fileSystem can not be null");
        this.indexBuildFileSystem = fileSystem;
    }

    /**
     * 每次执行全量会分配一个workflowid對應到 join規則文件
     */
    @Override
    public Integer getWorkflowId() {
        try {
            return Integer.parseInt(this.getString(IFullBuildContext.KEY_WORKFLOW_ID));
        } catch (Throwable e) {
        }
        return null;
    }

    @Override
    public String getWorkflowName() {
        String result = this.getString(IFullBuildContext.KEY_WORKFLOW_NAME);
        if (StringUtils.isEmpty(result)) {
            throw new IllegalStateException(IFullBuildContext.KEY_WORKFLOW_NAME + " can not be empty");
        }
        return result;
    }

    public DefaultChainContext(IParamContext execContext) {
        super();
        // DateTimeFormatter yyyyMMddHHmmss = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
        this.ps = StringUtils.defaultIfEmpty(DataxUtils.getDumpTimeStamp(false), IParamContext.getCurrentTimeStamp());
        this.httpExecContext = execContext;
        ExecChainContextUtils.setDependencyTablesPartitions(this, new TabPartitions(Maps.newHashMap()));
    }

    private IBasicAppSource appSource;

    public <T extends IBasicAppSource> T getAppSource() {
        if (appSource == null) {
            this.appSource = IAppSource.load(this.getIndexName());
        }
        return (T) appSource;
    }

    private final Map<String, Object> attribute = new HashMap<>();

    public void setAttribute(String key, Object v) {
        this.attribute.put(key, v);
    }

    @SuppressWarnings("all")
    public <T> T getAttribute(String key) {
        return (T) this.attribute.get(key);
    }

//    public void setZkStateReader(ZkStateReader zkStateReader) {
//        this.zkStateReader = zkStateReader;
//    }

    @Override
    public ITISCoordinator getZkClient() {
        if (this.zkClient == null) {
            throw new NullPointerException("zkClient can not null");
        }
        return this.zkClient;
    }

    public void setZkClient(ITISCoordinator zkClient) {
        this.zkClient = zkClient;
    }

    @Override
    public ITISFileSystem getIndexBuildFileSystem() {
        return this.indexBuildFileSystem;
    }

    /**
     * 提交的请求参数中是否有索引名称
     *
     * @return
     */
    @Override
    public boolean hasIndexName() {
        String indexName = this.httpExecContext.getString(IFullBuildContext.KEY_APP_NAME);
        return StringUtils.isNotBlank(indexName);
    }

    @Override
    public String getIndexName() {
        String indexName = this.httpExecContext.getString(IFullBuildContext.KEY_APP_NAME);
        if (StringUtils.isBlank(indexName)) {
            throw new IllegalArgumentException(indexName);
        }
        return indexName;
    }

    @Override
    public String getPartitionTimestamp() {
//        String ps = StringUtils.defaultIfEmpty(getString(KEY_PARTITION), this.ps);
//        if (!ps.startsWith("20")) {
//            throw new IllegalArgumentException("ps:" + ps + " shall start with 201");
//        }
//        return ps;
        //  throw new UnsupportedOperationException();
        return ps;
    }

    public String getString(String key) {
        return httpExecContext.getString(key);
    }

    public boolean getBoolean(String key) {
        return httpExecContext.getBoolean(key);
    }

    public int getInt(String key) {
        return httpExecContext.getInt(key);
    }

    public long getLong(String key) {
        return httpExecContext.getLong(key);
    }

//    public void setPs(String ps) {
//        this.ps = ps;
//    }

//    @Override
//    public TableDumpFactory getTableDumpFactory() {
//        Objects.requireNonNull(this.fs2Table, "tableDumpFactory can not be null");
//        return fs2Table;
//    }

    @Override
    public IAppSourcePipelineController getPipelineController() {
        Objects.requireNonNull(this.appSourcePipelineController, "appSourcePipelineController can not be null");
        return this.appSourcePipelineController;
    }

    public void setAppSourcePipelineController(IAppSourcePipelineController appSourcePipelineController) {
        this.appSourcePipelineController = appSourcePipelineController;
    }

    @Override
    public PhaseStatusCollection loadPhaseStatusFromLatest(String appName) {
        Optional<WorkFlowBuildHistory> latestWFSuccessTask = DagTaskUtils.getLatestWFSuccessTaskId(appName);
        if (!latestWFSuccessTask.isPresent()) {
            return null;
        }
        WorkFlowBuildHistory h = latestWFSuccessTask.get();
        PhaseStatusCollection phaseStatusCollection = IndexSwapTaskflowLauncher.loadPhaseStatusFromLocal(h.getId());
        if (phaseStatusCollection == null) {
            return null;
        }
        return phaseStatusCollection;
    }
}
