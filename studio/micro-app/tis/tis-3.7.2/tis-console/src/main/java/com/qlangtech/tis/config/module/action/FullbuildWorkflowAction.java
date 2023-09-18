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
package com.qlangtech.tis.config.module.action;

import com.alibaba.citrus.turbine.Context;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.qlangtech.tis.assemble.ExecResult;
import com.qlangtech.tis.assemble.FullbuildPhase;
import com.qlangtech.tis.assemble.TriggerType;
import com.qlangtech.tis.fullbuild.IFullBuildContext;
import com.qlangtech.tis.job.common.JobCommon;
import com.qlangtech.tis.manage.PermissionConstant;
import com.qlangtech.tis.manage.biz.dal.pojo.Application;
import com.qlangtech.tis.manage.common.CreateNewTaskResult;
import com.qlangtech.tis.manage.spring.aop.Func;
import com.qlangtech.tis.order.center.IParamContext;
import com.qlangtech.tis.runtime.module.action.BasicModule;
import com.qlangtech.tis.workflow.dao.IWorkFlowBuildHistoryDAO;
import com.qlangtech.tis.workflow.pojo.WorkFlowBuildHistory;
import com.qlangtech.tis.workflow.pojo.WorkFlowBuildHistoryCriteria;
import org.apache.commons.lang.StringUtils;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2017年9月30日
 */
public class FullbuildWorkflowAction extends BasicModule {

  /**
   *
   */
  private static final long serialVersionUID = 1L;

  /**
   * description: table有效时间
   */
  private static final long VALID_TIME = 4 * 60 * 60 * 1000;

  /**
   * assemble 节点接收到来自console节点的触发任务，开始执行需要创建一个new的workflowbuildhistory记录
   *
   * @param context
   */
  @Func(value = PermissionConstant.DATAFLOW_MANAGE, sideEffect = false)
  public void doCreateNewTask(Context context) {

    final TriggerType triggerType = TriggerType.parse(this.getInt(IFullBuildContext.KEY_TRIGGER_TYPE));
    Application app = null;
    // appname 可以为空
    String appname = this.getString(IFullBuildContext.KEY_APP_NAME);
    Integer workflowId = this.getInt(IFullBuildContext.KEY_WORKFLOW_ID, null, false);

    if (StringUtils.isNotBlank(appname)) {
      app = this.getApplicationDAO().selectByName(appname);
      if (app == null) {
        throw new IllegalStateException("appname:" + appname + " relevant app pojo is not exist");
      }
    }

    WorkFlowBuildHistory task = new WorkFlowBuildHistory();
    task.setCreateTime(new Date());
    task.setStartTime(new Date());
    task.setWorkFlowId(workflowId);
    task.setTriggerType(triggerType.getValue());
    task.setState((byte) ExecResult.DOING.getValue());
    // Integer buildHistoryId = null;
    // 从什么阶段开始执行
    FullbuildPhase fromPhase = FullbuildPhase.parse(getInt(IParamContext.COMPONENT_START, FullbuildPhase.FullDump.getValue()));
    FullbuildPhase endPhase = FullbuildPhase.parse(getInt(IParamContext.COMPONENT_END, FullbuildPhase.IndexBackFlow.getValue()));
    if (app == null) {
      if (endPhase.bigThan(FullbuildPhase.JOIN)) {
        endPhase = FullbuildPhase.JOIN;
      }
    }
    if (fromPhase.getValue() > FullbuildPhase.FullDump.getValue()) {
      // 如果是从非第一步开始执行的话，需要客户端提供依赖的history记录id
      task.setHistoryId(this.getInt(IFullBuildContext.KEY_BUILD_HISTORY_TASK_ID));
    }
    // 说明只有workflow的流程和索引没有关系，所以不可能执行到索引build阶段去
    // task.setEndPhase((app == null) ? FullbuildPhase.JOIN.getValue() : FullbuildPhase.IndexBackFlow.getValue());
    task.setEndPhase(endPhase.getValue());
    task.setStartPhase(fromPhase.getValue());
    if (app != null) {
      task.setAppId(app.getAppId());
      task.setAppName(app.getProjectName());
    }
    // 生成一个新的taskid
    this.setBizResult(context, new CreateNewTaskResult(getHistoryDAO().insertSelective(task), app));
  }

  /**
   * 取得最近一次成功执行的workflowhistory
   *
   * @param context
   */
  @Func(value = PermissionConstant.DATAFLOW_MANAGE, sideEffect = false)
  public void doGetLatestSuccessWorkflow(Context context) {
    String appName = this.getString(IFullBuildContext.KEY_APP_NAME);
    if (StringUtils.isEmpty(appName)) {
      throw new IllegalArgumentException("param appName can not be null");
    }

    WorkFlowBuildHistoryCriteria historyCriteria = new WorkFlowBuildHistoryCriteria();
    historyCriteria.setOrderByClause("id desc");
    historyCriteria.createCriteria()
      .andAppNameEqualTo(appName).andStateEqualTo((byte) ExecResult.SUCCESS.getValue());

    List<WorkFlowBuildHistory> histories
      = this.getWorkflowDAOFacade().getWorkFlowBuildHistoryDAO().selectByExample(historyCriteria, 1, 1);

    for (WorkFlowBuildHistory buildHistory : histories) {
      this.setBizResult(context, buildHistory);
      return;
    }

    this.addErrorMessage(context, "can not find build history by appname:" + appName);
  }

  @Func(value = PermissionConstant.DATAFLOW_MANAGE, sideEffect = false)
  public void doGetWf(Context context) {
    Integer taskId = this.getInt(JobCommon.KEY_TASK_ID);
    this.setBizResult(context, this.getWorkflowDAOFacade().getWorkFlowBuildHistoryDAO().loadFromWriteDB(taskId));
  }

  /**
   * 执行阶段结束
   * do_task_complete
   *
   * @param context
   */
  @Func(value = PermissionConstant.DATAFLOW_MANAGE, sideEffect = false)
  public void doTaskComplete(Context context) {
    Integer taskid = this.getInt(JobCommon.KEY_TASK_ID);
    // 执行结果
    ExecResult execResult = ExecResult.parse(this.getInt(IParamContext.KEY_EXEC_RESULT));
    String[] asynJobsName = this.getStringArray(IParamContext.KEY_ASYN_JOB_NAME);

    updateWfHistory(taskid, execResult, asynJobsName, 0);
  }

  /**
   * 接收异步执行任务执行状态
   *
   * @param context
   */
  @Func(value = PermissionConstant.DATAFLOW_MANAGE, sideEffect = false)
  public void doFeedbackAsynTaskStatus(Context context) {
    Integer taskid = this.getInt(JobCommon.KEY_TASK_ID);
    String jobName = this.getString(IParamContext.KEY_ASYN_JOB_NAME);
    boolean execSuccess = this.getBoolean(IParamContext.KEY_ASYN_JOB_SUCCESS);

    this.updateAsynTaskState(taskid, jobName, execSuccess, 0);
    this.setBizResult(context, new CreateNewTaskResult(taskid, null));
  }

  public static int MAX_CAS_RETRY_COUNT = 5;

  private void updateAsynTaskState(Integer taskid, String jobName, boolean execSuccess, int tryCount) {
    validateMaxCasRetryCount(taskid, tryCount);
    final WorkFlowBuildHistory history = getBuildHistory(taskid);

    if (ExecResult.ASYN_DOING != ExecResult.parse(history.getState())) {
      updateAsynTaskState(taskid, jobName, execSuccess, ++tryCount);
      return;
    }

    JSONObject status = JSON.parseObject(history.getAsynSubTaskStatus());
    JSONObject tskStat = status.getJSONObject(jobName);
    if (tskStat == null) {
      throw new IllegalStateException("jobName:" + jobName
        + " relevant status is not in history,now exist keys:"
        + status.keySet().stream().collect(Collectors.joining(",")));
    }
    tskStat.put(IParamContext.KEY_ASYN_JOB_COMPLETE, true);
    tskStat.put(IParamContext.KEY_ASYN_JOB_SUCCESS, execSuccess);
    status.put(jobName, tskStat);
    boolean[] allComplete = new boolean[]{true};
    boolean[] faild = new boolean[]{false};
    status.forEach((key, val) -> {
      JSONObject s = (JSONObject) val;
      if (s.getBoolean(IParamContext.KEY_ASYN_JOB_COMPLETE)) {
        if (!s.getBoolean(IParamContext.KEY_ASYN_JOB_SUCCESS)) {
          faild[0] = true;
        }
      } else {
        allComplete[0] = false;
      }
    });

    WorkFlowBuildHistory updateHistory = new WorkFlowBuildHistory();
    updateHistory.setAsynSubTaskStatus(status.toJSONString());
    updateHistory.setLastVer(history.getLastVer() + 1);
    ExecResult execResult = null;
    if (faild[0]) {
      // 有任务失败了
      execResult = ExecResult.FAILD;
    } else if (allComplete[0]) {
      execResult = ExecResult.SUCCESS;
    }

    if (execResult != null) {
      updateHistory.setState((byte) execResult.getValue());
      updateHistory.setEndTime(new Date());
    }
    WorkFlowBuildHistoryCriteria hq = new WorkFlowBuildHistoryCriteria();
    hq.createCriteria().andIdEqualTo(taskid).andLastVerEqualTo(history.getLastVer());

    if (getHistoryDAO().updateByExampleSelective(updateHistory, hq) < 1) {

      //  System.out.println("old lastVer:" + history.getLastVer() + ",new UpdateVersion:" + updateHistory.getLastVer());
      updateAsynTaskState(taskid, jobName, execSuccess, ++tryCount);
    }
  }

  private void validateMaxCasRetryCount(Integer taskid, int tryCount) {
    try {
      if (tryCount > 0) {
        Thread.sleep(200);
      }
    } catch (Throwable e) {
    }
    if (tryCount > MAX_CAS_RETRY_COUNT) {
      throw new IllegalStateException("taskId:" + taskid + " exceed max try count " + MAX_CAS_RETRY_COUNT);
    }
  }

  /**
   * CAS更新，尝试4次
   *
   * @param taskid
   * @param execResult
   * @param asynJobsName
   * @param tryCount
   */
  private void updateWfHistory(Integer taskid, final ExecResult execResult, String[] asynJobsName, int tryCount) {
    validateMaxCasRetryCount(taskid, tryCount);

    WorkFlowBuildHistory history = getBuildHistory(taskid);
    WorkFlowBuildHistoryCriteria hq = new WorkFlowBuildHistoryCriteria();
    WorkFlowBuildHistoryCriteria.Criteria criteria = hq.createCriteria().andIdEqualTo(taskid);
    criteria.andLastVerEqualTo(history.getLastVer());
    WorkFlowBuildHistory upHistory = new WorkFlowBuildHistory();
    upHistory.setLastVer(history.getLastVer() + 1);

    JSONObject jobState = null;
    if (asynJobsName != null && asynJobsName.length > 0) {
      JSONObject asynSubTaskStatus = new JSONObject();
      for (String jobName : asynJobsName) {
        jobState = new JSONObject();
        jobState.put(IParamContext.KEY_ASYN_JOB_COMPLETE, false);
        jobState.put(IParamContext.KEY_ASYN_JOB_SUCCESS, false);
        asynSubTaskStatus.put(jobName, jobState);
      }
      upHistory.setState((byte) ExecResult.ASYN_DOING.getValue());
      upHistory.setAsynSubTaskStatus(asynSubTaskStatus.toJSONString());
    } else {
      upHistory.setState((byte) execResult.getValue());
      upHistory.setEndTime(new Date());
    }

    if (getHistoryDAO().updateByExampleSelective(upHistory, hq) < 1) {
      updateWfHistory(taskid, execResult, asynJobsName, ++tryCount);
    }
  }

  private WorkFlowBuildHistory getBuildHistory(Integer taskid) {
    WorkFlowBuildHistory history = this.getHistoryDAO().loadFromWriteDB(taskid);
    if (history == null) {
      throw new IllegalStateException("taskid:" + taskid + " relevant WorkFlowBuildHistory obj can not be null");
    }
    return history;
  }

  protected IWorkFlowBuildHistoryDAO getHistoryDAO() {
    return this.getWorkflowDAOFacade().getWorkFlowBuildHistoryDAO();
  }

//  private DatasourceTable getTable(String tabName) {
//    DatasourceTableCriteria query = new DatasourceTableCriteria();
//    query.createCriteria().andNameEqualTo(tabName);
//    List<DatasourceTable> tabList = this.getWorkflowDAOFacade().getDatasourceTableDAO().selectByExample(query);
//    return tabList.stream().findFirst().get();
//  }

//  public static GitUtils.GitBranchInfo getBranch(WorkFlow workFlow) {
//    RunEnvironment runtime = RunEnvironment.getSysRuntime();
//    if (runtime == RunEnvironment.ONLINE) {
//      return GitBranchInfo.$(GitUtils.GitBranch.MASTER);
//    } else {
//      // : GitBranchInfo.$(workFlow.getName());
//      return GitBranchInfo.$(GitUtils.GitBranch.DEVELOP);
//    }
//  }

  public static class ValidTableDump {

    boolean hasValidTableDump;

    String pt = "";

    public boolean isHasValidTableDump() {
      return hasValidTableDump;
    }

    public void setHasValidTableDump(boolean hasValidTableDump) {
      this.hasValidTableDump = hasValidTableDump;
    }

    public String getPt() {
      return pt;
    }

    public void setPt(String pt) {
      this.pt = pt;
    }
  }
}
