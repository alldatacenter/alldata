/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.inlong.manager.service.group;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.apache.inlong.manager.common.enums.ErrorCodeEnum;
import org.apache.inlong.manager.common.enums.GroupOperateType;
import org.apache.inlong.manager.common.enums.GroupStatus;
import org.apache.inlong.manager.common.enums.ProcessName;
import org.apache.inlong.manager.common.util.Preconditions;
import org.apache.inlong.manager.dao.entity.WorkflowProcessEntity;
import org.apache.inlong.manager.pojo.group.InlongGroupInfo;
import org.apache.inlong.manager.pojo.group.InlongGroupResetRequest;
import org.apache.inlong.manager.pojo.stream.InlongStreamBriefInfo;
import org.apache.inlong.manager.pojo.stream.InlongStreamInfo;
import org.apache.inlong.manager.pojo.workflow.ProcessRequest;
import org.apache.inlong.manager.pojo.workflow.WorkflowResult;
import org.apache.inlong.manager.pojo.workflow.form.process.ApplyGroupProcessForm;
import org.apache.inlong.manager.pojo.workflow.form.process.GroupResourceProcessForm;
import org.apache.inlong.manager.service.stream.InlongStreamService;
import org.apache.inlong.manager.service.workflow.WorkflowService;
import org.apache.inlong.manager.workflow.core.WorkflowQueryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy;
import java.util.concurrent.TimeUnit;

/**
 * Operation related to inlong group process
 */
@Service
public class InlongGroupProcessService {

    private static final Logger LOGGER = LoggerFactory.getLogger(InlongGroupProcessService.class);

    private final ExecutorService executorService = new ThreadPoolExecutor(
            20,
            40,
            0L,
            TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue<>(),
            new ThreadFactoryBuilder().setNameFormat("inlong-group-process-%s").build(),
            new CallerRunsPolicy());

    @Autowired
    private InlongGroupService groupService;
    @Autowired
    private WorkflowQueryService workflowQueryService;
    @Autowired
    private WorkflowService workflowService;
    @Autowired
    private InlongStreamService streamService;

    /**
     * Start a New InlongGroup for the specified inlong group id.
     *
     * @param groupId inlong group id
     * @param operator name of operator
     * @return workflow result
     */
    public WorkflowResult startProcess(String groupId, String operator) {
        LOGGER.info("begin to start approve process for groupId={} by operator={}", groupId, operator);

        groupService.updateStatus(groupId, GroupStatus.TO_BE_APPROVAL.getCode(), operator);
        ApplyGroupProcessForm form = genApplyGroupProcessForm(groupId);
        WorkflowResult result = workflowService.start(ProcessName.APPLY_GROUP_PROCESS, operator, form);

        LOGGER.info("success to start approve process for groupId={} by operator={}", groupId, operator);
        return result;
    }

    /**
     * Suspend InlongGroup in an asynchronous way.
     *
     * @param groupId inlong group id
     * @param operator name of operator
     * @return inlong group id
     * @apiNote Stop source and sort task related to the inlong group asynchronously, persist the status if
     *         necessary.
     */
    public String suspendProcessAsync(String groupId, String operator) {
        LOGGER.info("begin to suspend process asynchronously for groupId={} by operator={}", groupId, operator);

        groupService.updateStatus(groupId, GroupStatus.SUSPENDING.getCode(), operator);
        InlongGroupInfo groupInfo = groupService.get(groupId);
        GroupResourceProcessForm form = genGroupResourceProcessForm(groupInfo, GroupOperateType.SUSPEND);
        executorService.execute(() -> workflowService.start(ProcessName.SUSPEND_GROUP_PROCESS, operator, form));

        LOGGER.info("success to suspend process asynchronously for groupId={} by operator={}", groupId, operator);
        return groupId;
    }

    /**
     * Suspend InlongGroup which is started up successfully.
     *
     * @param groupId inlong group id
     * @param operator name of operator
     * @return workflow result
     * @apiNote Stop source and sort task related to the inlong group asynchronously, persist the status if
     *         necessary.
     */
    public WorkflowResult suspendProcess(String groupId, String operator) {
        LOGGER.info("begin to suspend process for groupId={} by operator={}", groupId, operator);

        groupService.updateStatus(groupId, GroupStatus.SUSPENDING.getCode(), operator);
        InlongGroupInfo groupInfo = groupService.get(groupId);
        GroupResourceProcessForm form = genGroupResourceProcessForm(groupInfo, GroupOperateType.SUSPEND);
        WorkflowResult result = workflowService.start(ProcessName.SUSPEND_GROUP_PROCESS, operator, form);

        LOGGER.info("success to suspend process for groupId={} by operator={}", groupId, operator);
        return result;
    }

    /**
     * Restart InlongGroup in an asynchronous way, starting from the last persist snapshot.
     *
     * @param groupId inlong group id
     * @param operator name of operator
     * @return workflow result
     */
    public String restartProcessAsync(String groupId, String operator) {
        LOGGER.info("begin to restart process asynchronously for groupId={} by operator={}", groupId, operator);

        groupService.updateStatus(groupId, GroupStatus.RESTARTING.getCode(), operator);
        InlongGroupInfo groupInfo = groupService.get(groupId);
        GroupResourceProcessForm form = genGroupResourceProcessForm(groupInfo, GroupOperateType.RESTART);
        executorService.execute(() -> workflowService.start(ProcessName.RESTART_GROUP_PROCESS, operator, form));

        LOGGER.info("success to restart process asynchronously for groupId={} by operator={}", groupId, operator);
        return groupId;
    }

    /**
     * Restart InlongGroup which is started up successfully, starting from the last persist snapshot.
     *
     * @param groupId inlong group id
     * @param operator name of operator
     * @return workflow result
     */
    public WorkflowResult restartProcess(String groupId, String operator) {
        LOGGER.info("begin to restart process for groupId={} by operator={}", groupId, operator);

        groupService.updateStatus(groupId, GroupStatus.RESTARTING.getCode(), operator);
        InlongGroupInfo groupInfo = groupService.get(groupId);
        GroupResourceProcessForm form = genGroupResourceProcessForm(groupInfo, GroupOperateType.RESTART);
        WorkflowResult result = workflowService.start(ProcessName.RESTART_GROUP_PROCESS, operator, form);

        LOGGER.info("success to restart process for groupId={} by operator={}", groupId, operator);
        return result;
    }

    /**
     * Delete InlongGroup logically and delete related resource in an asynchronous way.
     *
     * @param groupId inlong group id
     * @param operator name of operator
     * @return inlong group id
     */
    public String deleteProcessAsync(String groupId, String operator) {
        LOGGER.info("begin to delete process asynchronously for groupId={} by operator={}", groupId, operator);
        executorService.execute(() -> {
            try {
                invokeDeleteProcess(groupId, operator);
            } catch (Exception ex) {
                LOGGER.error("exception while delete process for groupId={} by operator={}", groupId, operator, ex);
                throw ex;
            }
            groupService.delete(groupId, operator);
        });

        LOGGER.info("success to delete process asynchronously for groupId={} by operator={}", groupId, operator);
        return groupId;
    }

    /**
     * Delete InlongGroup logically and delete related resource in an asynchronous way.
     */
    public boolean deleteProcess(String groupId, String operator) {
        LOGGER.info("begin to delete process for groupId={} by operator={}", groupId, operator);
        try {
            invokeDeleteProcess(groupId, operator);
        } catch (Exception ex) {
            LOGGER.error("exception while delete process for groupId={} by operator={}", groupId, operator, ex);
            throw ex;
        }

        boolean result = groupService.delete(groupId, operator);
        LOGGER.info("success to delete process for groupId={} by operator={}", groupId, operator);
        return result;
    }

    /**
     * Reset InlongGroup status when group is staying CONFIG_ING|SUSPENDING|RESTARTING|DELETING for a long time.
     * This api is side effect, must be used carefully.
     *
     * @param request reset inlong group request
     * @param operator name of operator
     * @return success or false
     */
    public boolean resetGroupStatus(InlongGroupResetRequest request, String operator) {
        LOGGER.info("begin to reset group status by operator={} for request={}", operator, request);
        final String groupId = request.getInlongGroupId();
        InlongGroupInfo groupInfo = groupService.get(groupId);
        Preconditions.checkNotNull(groupInfo, ErrorCodeEnum.GROUP_NOT_FOUND.getMessage());

        GroupStatus status = GroupStatus.forCode(groupInfo.getStatus());
        boolean result;
        switch (status) {
            case CONFIG_ING:
            case SUSPENDING:
            case RESTARTING:
            case DELETING:
                final int rerunProcess = request.getRerunProcess();
                final int resetFinalStatus = request.getResetFinalStatus();
                result = pendingGroupOpt(groupInfo, operator, status, rerunProcess, resetFinalStatus);
                break;
            default:
                throw new IllegalStateException(String.format("Unsupported status to reset groupId=%s and status=%s",
                        request.getInlongGroupId(), status));
        }

        LOGGER.info("finish to reset group status by operator={}, result={} for request={}", operator, result, request);
        return result;
    }

    private boolean pendingGroupOpt(InlongGroupInfo groupInfo, String operator, GroupStatus status,
            int rerunProcess, int resetFinalStatus) {
        final String groupId = groupInfo.getInlongGroupId();
        if (rerunProcess == 1) {
            ProcessRequest processQuery = new ProcessRequest();
            processQuery.setInlongGroupId(groupId);
            List<WorkflowProcessEntity> entities = workflowQueryService.listProcessEntity(processQuery);
            entities.sort(Comparator.comparingInt(WorkflowProcessEntity::getId));
            WorkflowProcessEntity lastProcess = entities.get(entities.size() - 1);
            executorService.execute(() -> {
                workflowService.continueProcess(lastProcess.getId(), operator, "Reset group status");
            });
            return true;
        }
        if (resetFinalStatus == 1) {
            GroupStatus finalStatus = getFinalStatus(status);
            return groupService.updateStatus(groupId, finalStatus.getCode(), operator);
        } else {
            return groupService.updateStatus(groupId, GroupStatus.CONFIG_FAILED.getCode(), operator);
        }
    }

    private GroupStatus getFinalStatus(GroupStatus pendingStatus) {
        switch (pendingStatus) {
            case CONFIG_ING:
                return GroupStatus.CONFIG_SUCCESSFUL;
            case SUSPENDING:
                return GroupStatus.SUSPENDED;
            case RESTARTING:
                return GroupStatus.RESTARTED;
            default:
                return GroupStatus.DELETED;
        }
    }

    private void invokeDeleteProcess(String groupId, String operator) {
        InlongGroupInfo groupInfo = groupService.get(groupId);
        GroupResourceProcessForm form = genGroupResourceProcessForm(groupInfo, GroupOperateType.DELETE);
        workflowService.start(ProcessName.DELETE_GROUP_PROCESS, operator, form);
    }

    /**
     * Generate the form of [Apply Group Workflow]
     */
    private ApplyGroupProcessForm genApplyGroupProcessForm(String groupId) {
        ApplyGroupProcessForm form = new ApplyGroupProcessForm();
        InlongGroupInfo groupInfo = groupService.get(groupId);
        form.setGroupInfo(groupInfo);
        List<InlongStreamBriefInfo> infoList = streamService.listBriefWithSink(groupInfo.getInlongGroupId());
        form.setStreamInfoList(infoList);
        return form;
    }

    /**
     * Generate the form of [Group Resource Workflow]
     */
    private GroupResourceProcessForm genGroupResourceProcessForm(InlongGroupInfo groupInfo,
            GroupOperateType operateType) {
        GroupResourceProcessForm form = new GroupResourceProcessForm();
        String groupId = groupInfo.getInlongGroupId();
        List<InlongStreamInfo> streamList = streamService.list(groupId);
        form.setStreamInfos(streamList);
        form.setGroupInfo(groupInfo);
        form.setGroupOperateType(operateType);
        return form;
    }

}
