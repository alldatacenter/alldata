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

package com.qlangtech.tis.manage.common;

import com.google.common.collect.Lists;
import com.qlangtech.tis.ajax.AjaxResult;
import com.qlangtech.tis.assemble.ExecResult;
import com.qlangtech.tis.assemble.FullbuildPhase;
import com.qlangtech.tis.assemble.TriggerType;
import com.qlangtech.tis.exec.ExecutePhaseRange;
import com.qlangtech.tis.exec.IExecChainContext;
import com.qlangtech.tis.exec.ITaskPhaseInfo;
import com.qlangtech.tis.fullbuild.IFullBuildContext;
import com.qlangtech.tis.fullbuild.indexbuild.IRemoteTaskTrigger;
import com.qlangtech.tis.fullbuild.indexbuild.RemoteTaskTriggers;
import com.qlangtech.tis.fullbuild.phasestatus.impl.DumpPhaseStatus;
import com.qlangtech.tis.fullbuild.phasestatus.impl.JoinPhaseStatus;
import com.qlangtech.tis.fullbuild.taskflow.DumpTask;
import com.qlangtech.tis.fullbuild.taskflow.JoinTask;
import com.qlangtech.tis.fullbuild.taskflow.TaskAndMilestone;
import com.qlangtech.tis.job.common.JobCommon;
import com.qlangtech.tis.order.center.IParamContext;
import com.qlangtech.tis.sql.parser.DAGSessionSpec;
import com.qlangtech.tis.workflow.pojo.WorkFlowBuildHistory;
import org.apache.commons.lang.StringUtils;

import java.text.MessageFormat;
import java.util.List;
import java.util.Optional;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2021-06-03 14:57
 **/
public class DagTaskUtils {
    public static final MessageFormat WORKFLOW_CONFIG_URL_FORMAT
            = new MessageFormat(Config.getConfigRepositoryHost()
            + "/config/config.ajax?action={0}&event_submit_{1}=true&handler={2}{3}");
    public static final MessageFormat WORKFLOW_CONFIG_URL_POST_FORMAT
            = new MessageFormat(Config.getConfigRepositoryHost()
            + "/config/config.ajax?action={0}&event_submit_{1}=true");


    public static void feedbackAsynTaskStatus(int taskid, String subTaskName, boolean success) {
        String url = WORKFLOW_CONFIG_URL_FORMAT
                .format(new Object[]{"fullbuild_workflow_action", "do_feedback_asyn_task_status", StringUtils.EMPTY,
                        StringUtils.EMPTY});
        List<HttpUtils.PostParam> params = Lists.newArrayList();
        params.add(new HttpUtils.PostParam(IParamContext.KEY_REQUEST_DISABLE_TRANSACTION, true));
        params.add(new HttpUtils.PostParam(JobCommon.KEY_TASK_ID, taskid));
        params.add(new HttpUtils.PostParam(IParamContext.KEY_ASYN_JOB_NAME, subTaskName));
        params.add(new HttpUtils.PostParam(IParamContext.KEY_ASYN_JOB_SUCCESS, success));

        HttpUtils.soapRemote(url, params, CreateNewTaskResult.class, false);
    }

    public static void createTaskComplete(int taskid, IExecChainContext chainContext, ExecResult execResult) {
        if (execResult == null) {
            throw new IllegalArgumentException("param execResult can not be null");
        }
        String url = WORKFLOW_CONFIG_URL_FORMAT
                .format(new Object[]{"fullbuild_workflow_action", "do_task_complete", StringUtils.EMPTY, /* advance_query_result */
                        StringUtils.EMPTY});
        //
        List<HttpUtils.PostParam> params = Lists.newArrayList(//
                new HttpUtils.PostParam("execresult", String.valueOf(execResult.getValue())), //
                new HttpUtils.PostParam(JobCommon.KEY_TASK_ID, String.valueOf(taskid)));

        if (chainContext.containAsynJob()) {
            for (IExecChainContext.AsynSubJob asynJob : chainContext.getAsynSubJobs()) {
                params.add(new HttpUtils.PostParam(IParamContext.KEY_ASYN_JOB_NAME, asynJob.jobName));
            }
        }

        HttpUtils.soapRemote(url, params, CreateNewTaskResult.class);
    }

    /**
     * 开始执行一個新的任務
     *
     * @param newTaskParam taskid
     * @return
     */
    public static Integer createNewTask(NewTaskParam newTaskParam) {
        String url = WORKFLOW_CONFIG_URL_POST_FORMAT
                .format(new Object[]{"fullbuild_workflow_action", "do_create_new_task"});
        AjaxResult<CreateNewTaskResult> result = HttpUtils.soapRemote(url, newTaskParam.params(), CreateNewTaskResult.class);
        return result.getBizresult().getTaskid();
    }

    /**
     * 取得最近一次workflow成功执行的taskid
     *
     * @param appName
     * @return
     */
    public static Optional<WorkFlowBuildHistory> getLatestWFSuccessTaskId(String appName) {
        if (StringUtils.isBlank(appName)) {
            throw new IllegalArgumentException("param appName can not be empty");
        }
        String url = WORKFLOW_CONFIG_URL_POST_FORMAT
                .format(new Object[]{"fullbuild_workflow_action", "do_get_latest_success_workflow"});
        List<HttpUtils.PostParam> params = Lists.newArrayList();
        params.add(new HttpUtils.PostParam(IFullBuildContext.KEY_APP_NAME, appName));

        AjaxResult<WorkFlowBuildHistory> result = HttpUtils.soapRemote(url, params, WorkFlowBuildHistory.class, false);
        if (!result.isSuccess()) {
            return Optional.empty();
        }
        return Optional.of(result.getBizresult());
    }

    /**
     * 取得当前工作流 执行状态
     *
     * @param taskId
     * @return
     */
    public static WorkFlowBuildHistory getWFStatus(Integer taskId) {
        if (taskId == null || taskId < 1) {
            throw new IllegalArgumentException("param taskId can not be empty");
        }
        String url = WORKFLOW_CONFIG_URL_POST_FORMAT
                .format(new Object[]{"fullbuild_workflow_action", "do_get_wf"});
        List<HttpUtils.PostParam> params = Lists.newArrayList();
        params.add(new HttpUtils.PostParam(JobCommon.KEY_TASK_ID, taskId));

        AjaxResult<WorkFlowBuildHistory> result = HttpUtils.soapRemote(url, params, WorkFlowBuildHistory.class, true);
        return result.getBizresult();
    }

    public static List<IRemoteTaskTrigger> createTasks(IExecChainContext execChainContext, ITaskPhaseInfo phaseStatus
            , DAGSessionSpec dagSessionSpec, RemoteTaskTriggers tskTriggers) {
        List<IRemoteTaskTrigger> triggers = Lists.newArrayList();
        for (IRemoteTaskTrigger trigger : tskTriggers.getDumpPhaseTasks()) {
            triggers.add(addDumpTask(execChainContext, phaseStatus, dagSessionSpec, trigger));
        }

        for (IRemoteTaskTrigger trigger : tskTriggers.getJoinPhaseTasks()) {
            triggers.add(addJoinTask(execChainContext, phaseStatus, dagSessionSpec, trigger));
        }
        return triggers;
    }

    private static IRemoteTaskTrigger addDumpTask(IExecChainContext execChainContext
            , ITaskPhaseInfo phaseStatus, DAGSessionSpec dagSessionSpec
            , IRemoteTaskTrigger jobTrigger) {
        // triggers.add(jobTrigger);
        DumpPhaseStatus dumpStatus = phaseStatus.getPhaseStatus(execChainContext, FullbuildPhase.FullDump);
        dagSessionSpec.put(jobTrigger.getTaskName()
                , new TaskAndMilestone(DumpTask.createDumpTask(jobTrigger, dumpStatus.getTable(jobTrigger.getTaskName()))));
        return jobTrigger;
    }

    private static IRemoteTaskTrigger addJoinTask(IExecChainContext execChainContext, ITaskPhaseInfo phaseStatus
            , DAGSessionSpec dagSessionSpec
            , IRemoteTaskTrigger postTaskTrigger) {
        JoinPhaseStatus joinStatus = phaseStatus.getPhaseStatus(execChainContext, FullbuildPhase.JOIN);
        //triggers.add(postTaskTrigger);
        JoinPhaseStatus.JoinTaskStatus taskStatus = joinStatus.getTaskStatus(postTaskTrigger.getTaskName());
        taskStatus.setWaiting(true);
        dagSessionSpec.put(postTaskTrigger.getTaskName()
                , new TaskAndMilestone(JoinTask.createJoinTask(postTaskTrigger, taskStatus)));
        return postTaskTrigger;
    }

    public static class NewTaskParam {

        private Integer workflowid;

        private TriggerType triggerType;

        private String appname;

        // 历史任务ID
        private Integer historyTaskId;

        public void setHistoryTaskId(Integer historyTaskId) {
            this.historyTaskId = historyTaskId;
        }

        private ExecutePhaseRange executeRanage;

        public Integer getWorkflowid() {
            return workflowid;
        }

        public void setWorkflowid(Integer workflowid) {
            this.workflowid = workflowid;
        }

        public TriggerType getTriggerType() {
            return triggerType;
        }

        public void setTriggerType(TriggerType triggerType) {
            this.triggerType = triggerType;
        }

        public String getAppname() {
            return appname;
        }

        public void setAppname(String appname) {
            this.appname = appname;
        }

        public ExecutePhaseRange getExecuteRanage() {
            return executeRanage;
        }

        public void setExecuteRanage(ExecutePhaseRange executeRanage) {
            this.executeRanage = executeRanage;
        }

        private List<HttpUtils.PostParam> params() {
            List<HttpUtils.PostParam> params = Lists.newArrayList( //
                    new HttpUtils.PostParam(IFullBuildContext.KEY_WORKFLOW_ID, workflowid)
                    , new HttpUtils.PostParam(IFullBuildContext.KEY_TRIGGER_TYPE, triggerType.getValue())
                    , new HttpUtils.PostParam(IParamContext.COMPONENT_START, executeRanage.getStart().getValue())
                    , new HttpUtils.PostParam(IParamContext.COMPONENT_END, executeRanage.getEnd().getValue()));
            if (!executeRanage.contains(FullbuildPhase.FullDump)) {
                if (historyTaskId == null) {
                    throw new IllegalStateException("param historyTaskId can not be null");
                }
                params.add(new HttpUtils.PostParam(IFullBuildContext.KEY_BUILD_HISTORY_TASK_ID, historyTaskId));
            }
            if (StringUtils.isNotBlank(appname)) {
                // result.append("&").append(IFullBuildContext.KEY_APP_NAME).append("=").append(appname);
                params.add(new HttpUtils.PostParam(IFullBuildContext.KEY_APP_NAME, appname));
            }
            return params;
        }
    }
}
