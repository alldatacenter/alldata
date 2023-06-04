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

package org.apache.inlong.manager.service.stream;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import lombok.extern.slf4j.Slf4j;
import org.apache.inlong.manager.common.enums.ErrorCodeEnum;
import org.apache.inlong.manager.common.enums.GroupOperateType;
import org.apache.inlong.manager.common.enums.GroupStatus;
import org.apache.inlong.manager.common.enums.ProcessName;
import org.apache.inlong.manager.common.enums.ProcessStatus;
import org.apache.inlong.manager.common.enums.StreamStatus;
import org.apache.inlong.manager.common.exceptions.BusinessException;
import org.apache.inlong.manager.common.util.Preconditions;
import org.apache.inlong.manager.pojo.group.InlongGroupInfo;
import org.apache.inlong.manager.pojo.stream.InlongStreamInfo;
import org.apache.inlong.manager.pojo.workflow.WorkflowResult;
import org.apache.inlong.manager.pojo.workflow.form.process.StreamResourceProcessForm;
import org.apache.inlong.manager.service.group.InlongGroupService;
import org.apache.inlong.manager.service.workflow.WorkflowService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy;
import java.util.concurrent.TimeUnit;

import static org.apache.inlong.manager.common.consts.InlongConstants.ALIVE_TIME_MS;
import static org.apache.inlong.manager.common.consts.InlongConstants.CORE_POOL_SIZE;
import static org.apache.inlong.manager.common.consts.InlongConstants.MAX_POOL_SIZE;
import static org.apache.inlong.manager.common.consts.InlongConstants.QUEUE_SIZE;

/**
 * Operation related to inlong stream process
 */
@Slf4j
@Service
public class InlongStreamProcessService {

    private static final ExecutorService EXECUTOR_SERVICE = new ThreadPoolExecutor(
            CORE_POOL_SIZE,
            MAX_POOL_SIZE,
            ALIVE_TIME_MS,
            TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue<>(QUEUE_SIZE),
            new ThreadFactoryBuilder().setNameFormat("inlong-stream-process-%s").build(),
            new CallerRunsPolicy());

    @Autowired
    private InlongGroupService groupService;
    @Autowired
    private InlongStreamService streamService;
    @Autowired
    private WorkflowService workflowService;

    /**
     * Create stream in synchronous/asynchronous way.
     */
    public boolean startProcess(String groupId, String streamId, String operator, boolean sync) {
        log.info("begin to start stream process for groupId={} streamId={}", groupId, streamId);

        InlongGroupInfo groupInfo = groupService.get(groupId);
        Preconditions.expectNotNull(groupInfo, ErrorCodeEnum.GROUP_NOT_FOUND.getMessage());
        GroupStatus groupStatus = GroupStatus.forCode(groupInfo.getStatus());
        if (groupStatus != GroupStatus.CONFIG_SUCCESSFUL && groupStatus != GroupStatus.RESTARTED) {
            throw new BusinessException(String.format("group status=%s not support start stream"
                    + " for groupId=%s", groupStatus, groupId));
        }

        InlongStreamInfo streamInfo = streamService.get(groupId, streamId);
        Preconditions.expectNotNull(streamInfo, ErrorCodeEnum.STREAM_NOT_FOUND.getMessage());
        StreamStatus status = StreamStatus.forCode(streamInfo.getStatus());
        if (status == StreamStatus.CONFIG_ING) {
            log.warn("stream status={}, not need restart for groupId={} streamId={}", status, groupId, streamId);
            return true;
        }

        if (StreamStatus.notAllowedUpdate(status)) {
            String errMsg = String.format("stream status=%s not support start stream for groupId=%s streamId=%s",
                    status, groupId, streamId);
            log.error(errMsg);
            throw new BusinessException(errMsg);
        }

        StreamResourceProcessForm processForm = StreamResourceProcessForm.getProcessForm(groupInfo,
                streamInfo, GroupOperateType.INIT);
        ProcessName processName = ProcessName.CREATE_STREAM_RESOURCE;
        if (sync) {
            WorkflowResult workflowResult = workflowService.start(processName, operator, processForm);
            ProcessStatus processStatus = workflowResult.getProcessInfo().getStatus();
            return processStatus == ProcessStatus.COMPLETED;
        } else {
            EXECUTOR_SERVICE.execute(() -> workflowService.start(processName, operator, processForm));
            return true;
        }
    }

    /**
     * Suspend stream in synchronous/asynchronous way.
     */
    public boolean suspendProcess(String groupId, String streamId, String operator, boolean sync) {
        log.info("begin to suspend stream process for groupId={} streamId={}", groupId, streamId);

        InlongGroupInfo groupInfo = groupService.get(groupId);
        Preconditions.expectNotNull(groupInfo, ErrorCodeEnum.GROUP_NOT_FOUND.getMessage());
        GroupStatus groupStatus = GroupStatus.forCode(groupInfo.getStatus());
        if (!GroupStatus.allowedSuspend(groupStatus)) {
            throw new BusinessException(String.format("group status=%s not support suspend stream"
                    + " for groupId=%s", groupStatus, groupId));
        }

        InlongStreamInfo streamInfo = streamService.get(groupId, streamId);
        Preconditions.expectNotNull(streamInfo, ErrorCodeEnum.STREAM_NOT_FOUND.getMessage());
        StreamStatus status = StreamStatus.forCode(streamInfo.getStatus());
        if (status == StreamStatus.SUSPENDED || status == StreamStatus.SUSPENDING) {
            log.warn("groupId={}, streamId={} is already in {}", groupId, streamId, status);
            return true;
        }

        if (status != StreamStatus.CONFIG_SUCCESSFUL && status != StreamStatus.RESTARTED) {
            throw new BusinessException(String.format("stream status=%s not support suspend stream"
                    + " for groupId=%s streamId=%s", status, groupId, streamId));
        }

        StreamResourceProcessForm processForm = StreamResourceProcessForm.getProcessForm(groupInfo, streamInfo,
                GroupOperateType.SUSPEND);
        ProcessName processName = ProcessName.SUSPEND_STREAM_RESOURCE;
        if (sync) {
            WorkflowResult workflowResult = workflowService.start(processName, operator, processForm);
            ProcessStatus processStatus = workflowResult.getProcessInfo().getStatus();
            return processStatus == ProcessStatus.COMPLETED;
        } else {
            EXECUTOR_SERVICE.execute(() -> workflowService.start(processName, operator, processForm));
            return true;
        }
    }

    /**
     * Restart stream in synchronous/asynchronous way.
     */
    public boolean restartProcess(String groupId, String streamId, String operator, boolean sync) {
        log.info("begin to restart stream process for groupId={} streamId={}", groupId, streamId);

        InlongGroupInfo groupInfo = groupService.get(groupId);
        Preconditions.expectNotNull(groupInfo, ErrorCodeEnum.GROUP_NOT_FOUND.getMessage());
        GroupStatus groupStatus = GroupStatus.forCode(groupInfo.getStatus());
        if (groupStatus != GroupStatus.CONFIG_SUCCESSFUL && groupStatus != GroupStatus.RESTARTED) {
            throw new BusinessException(
                    String.format("group status=%s not support restart stream for groupId=%s", groupStatus, groupId));
        }

        InlongStreamInfo streamInfo = streamService.get(groupId, streamId);
        Preconditions.expectNotNull(streamInfo, ErrorCodeEnum.STREAM_NOT_FOUND.getMessage());
        StreamStatus status = StreamStatus.forCode(streamInfo.getStatus());
        if (status == StreamStatus.RESTARTED || status == StreamStatus.RESTARTING) {
            log.warn("inlong stream was already in {} for groupId={}, streamId={}", status, groupId, streamId);
            return true;
        }

        if (status != StreamStatus.SUSPENDED) {
            throw new BusinessException(String.format("stream status=%s not support restart stream"
                    + " for groupId=%s streamId=%s", status, groupId, streamId));
        }

        StreamResourceProcessForm processForm = StreamResourceProcessForm.getProcessForm(groupInfo, streamInfo,
                GroupOperateType.RESTART);
        ProcessName processName = ProcessName.RESTART_STREAM_RESOURCE;
        if (sync) {
            WorkflowResult workflowResult = workflowService.start(processName, operator, processForm);
            ProcessStatus processStatus = workflowResult.getProcessInfo().getStatus();
            return processStatus == ProcessStatus.COMPLETED;
        } else {
            EXECUTOR_SERVICE.execute(() -> workflowService.start(processName, operator, processForm));
            return true;
        }
    }

    /**
     * Restart stream in synchronous/asynchronous way.
     */
    public boolean deleteProcess(String groupId, String streamId, String operator, boolean sync) {
        log.debug("begin to delete stream process for groupId={} streamId={}", groupId, streamId);

        InlongGroupInfo groupInfo = groupService.get(groupId);
        if (groupInfo == null) {
            throw new BusinessException(ErrorCodeEnum.GROUP_NOT_FOUND,
                    ErrorCodeEnum.GROUP_NOT_FOUND.getMessage() + " : " + groupId);
        }
        GroupStatus groupStatus = GroupStatus.forCode(groupInfo.getStatus());
        if (GroupStatus.notAllowedTransition(groupStatus, GroupStatus.DELETING)) {
            throw new BusinessException(ErrorCodeEnum.GROUP_DELETE_NOT_ALLOWED,
                    String.format("group status=%s not support delete stream for groupId=%s", groupStatus, groupId));
        }

        InlongStreamInfo streamInfo = streamService.get(groupId, streamId);
        Preconditions.expectNotNull(streamInfo, ErrorCodeEnum.STREAM_NOT_FOUND.getMessage());
        StreamStatus status = StreamStatus.forCode(streamInfo.getStatus());
        if (status == StreamStatus.DELETED || status == StreamStatus.DELETING) {
            log.debug("groupId={}, streamId={} is already in {}", groupId, streamId, status);
            return true;
        }

        if (StreamStatus.notAllowedDelete(status)) {
            throw new BusinessException(ErrorCodeEnum.STREAM_OPT_NOT_ALLOWED,
                    String.format("stream status=%s not support delete stream for groupId=%s streamId=%s", status,
                            groupId, streamId));
        }

        StreamResourceProcessForm processForm =
                StreamResourceProcessForm.getProcessForm(groupInfo, streamInfo, GroupOperateType.DELETE);
        ProcessName processName = ProcessName.DELETE_STREAM_RESOURCE;
        if (sync) {
            WorkflowResult workflowResult = workflowService.start(processName, operator, processForm);
            ProcessStatus processStatus = workflowResult.getProcessInfo().getStatus();
            if (processStatus == ProcessStatus.COMPLETED) {
                return streamService.delete(groupId, streamId, operator);
            } else {
                return false;
            }
        } else {
            EXECUTOR_SERVICE.execute(() -> {
                WorkflowResult workflowResult = workflowService.start(processName, operator, processForm);
                ProcessStatus processStatus = workflowResult.getProcessInfo().getStatus();
                if (processStatus == ProcessStatus.COMPLETED) {
                    streamService.delete(groupId, streamId, operator);
                }
            });
            return true;
        }
    }

}
