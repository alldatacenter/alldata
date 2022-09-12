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

package org.apache.inlong.manager.service.core.operation;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import lombok.extern.slf4j.Slf4j;
import org.apache.inlong.manager.common.enums.ErrorCodeEnum;
import org.apache.inlong.manager.common.enums.GroupOperateType;
import org.apache.inlong.manager.common.enums.GroupStatus;
import org.apache.inlong.manager.common.enums.ProcessStatus;
import org.apache.inlong.manager.common.enums.StreamStatus;
import org.apache.inlong.manager.common.exceptions.BusinessException;
import org.apache.inlong.manager.common.pojo.group.InlongGroupInfo;
import org.apache.inlong.manager.common.pojo.stream.InlongStreamInfo;
import org.apache.inlong.manager.common.pojo.workflow.WorkflowResult;
import org.apache.inlong.manager.common.pojo.workflow.form.StreamResourceProcessForm;
import org.apache.inlong.manager.service.core.InlongStreamService;
import org.apache.inlong.manager.service.group.InlongGroupService;
import org.apache.inlong.manager.service.workflow.ProcessName;
import org.apache.inlong.manager.service.workflow.WorkflowService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy;
import java.util.concurrent.TimeUnit;

/**
 * Operation related to inlong stream process
 */
@Service
@Slf4j
public class InlongStreamProcessOperation {

    private final ExecutorService executorService = new ThreadPoolExecutor(
            20,
            40,
            0L,
            TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue<>(),
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
        log.info("StartProcess for groupId={}, streamId={}", groupId, streamId);
        InlongGroupInfo groupInfo = groupService.get(groupId);
        if (groupInfo == null) {
            throw new BusinessException(ErrorCodeEnum.GROUP_NOT_FOUND);
        }
        GroupStatus groupStatus = GroupStatus.forCode(groupInfo.getStatus());
        if (groupStatus != GroupStatus.CONFIG_SUCCESSFUL && groupStatus != GroupStatus.RESTARTED) {
            throw new BusinessException(
                    String.format("GroupId=%s, status=%s not correct for stream start", groupId, groupStatus));
        }
        InlongStreamInfo streamInfo = streamService.get(groupId, streamId);
        if (streamInfo == null) {
            throw new BusinessException(ErrorCodeEnum.STREAM_NOT_FOUND);
        }
        StreamStatus status = StreamStatus.forCode(streamInfo.getStatus());
        if (status == StreamStatus.CONFIG_ING) {
            log.warn("GroupId={}, StreamId={} is already in {}", groupId, streamId, status);
            return true;
        }
        if (status != StreamStatus.NEW && status != StreamStatus.CONFIG_FAILED
                && status != StreamStatus.CONFIG_SUCCESSFUL) {
            throw new BusinessException(
                    String.format("GroupId=%s, StreamId=%s, status=%s not correct for stream start", groupId, streamId,
                            status));
        }
        StreamResourceProcessForm processForm = genStreamProcessForm(groupInfo, streamInfo, GroupOperateType.INIT);
        ProcessName processName = ProcessName.CREATE_STREAM_RESOURCE;
        if (sync) {
            WorkflowResult workflowResult = workflowService.start(processName, operator,
                    processForm);
            ProcessStatus processStatus = workflowResult.getProcessInfo().getStatus();
            return processStatus == ProcessStatus.COMPLETED;
        } else {
            executorService.execute(
                    () -> workflowService.start(processName, operator, processForm));
            return true;
        }
    }

    /**
     * Suspend stream in synchronous/asynchronous way.
     */
    public boolean suspendProcess(String groupId, String streamId, String operator, boolean sync) {
        log.info("SuspendProcess for groupId={}, streamId={}", groupId, streamId);
        InlongGroupInfo groupInfo = groupService.get(groupId);
        if (groupInfo == null) {
            throw new BusinessException(ErrorCodeEnum.GROUP_NOT_FOUND);
        }
        GroupStatus groupStatus = GroupStatus.forCode(groupInfo.getStatus());
        if (groupStatus != GroupStatus.CONFIG_SUCCESSFUL
                && groupStatus != GroupStatus.RESTARTED
                && groupStatus != GroupStatus.SUSPENDED) {
            throw new BusinessException(
                    String.format("GroupId=%s, status=%s not correct for stream suspend", groupId, groupStatus));
        }
        InlongStreamInfo streamInfo = streamService.get(groupId, streamId);
        if (streamInfo == null) {
            throw new BusinessException(ErrorCodeEnum.STREAM_NOT_FOUND);
        }
        StreamStatus status = StreamStatus.forCode(streamInfo.getStatus());
        if (status == StreamStatus.SUSPENDED || status == StreamStatus.SUSPENDING) {
            log.warn("GroupId={}, StreamId={} is already in {}", groupId, streamId, status);
            return true;
        }
        if (status != StreamStatus.CONFIG_SUCCESSFUL && status != StreamStatus.RESTARTED) {
            throw new BusinessException(
                    String.format("GroupId=%s, StreamId=%s, status=%s not correct for stream suspend", groupId,
                            streamId,
                            status));
        }
        StreamResourceProcessForm processForm = genStreamProcessForm(groupInfo, streamInfo, GroupOperateType.SUSPEND);
        ProcessName processName = ProcessName.SUSPEND_STREAM_RESOURCE;
        if (sync) {
            WorkflowResult workflowResult = workflowService.start(processName, operator,
                    processForm);
            ProcessStatus processStatus = workflowResult.getProcessInfo().getStatus();
            return processStatus == ProcessStatus.COMPLETED;
        } else {
            executorService.execute(
                    () -> workflowService.start(processName, operator, processForm));
            return true;
        }
    }

    /**
     * Restart stream in synchronous/asynchronous way.
     */
    public boolean restartProcess(String groupId, String streamId, String operator, boolean sync) {
        log.info("RestartProcess for groupId={}, streamId={}", groupId, streamId);
        InlongGroupInfo groupInfo = groupService.get(groupId);
        if (groupInfo == null) {
            throw new BusinessException(ErrorCodeEnum.GROUP_NOT_FOUND);
        }
        GroupStatus groupStatus = GroupStatus.forCode(groupInfo.getStatus());
        if (groupStatus != GroupStatus.CONFIG_SUCCESSFUL
                && groupStatus != GroupStatus.RESTARTED) {
            throw new BusinessException(
                    String.format("GroupId=%s, status=%s not correct for stream restart", groupId, groupStatus));
        }
        InlongStreamInfo streamInfo = streamService.get(groupId, streamId);
        if (streamInfo == null) {
            throw new BusinessException(ErrorCodeEnum.STREAM_NOT_FOUND);
        }
        StreamStatus status = StreamStatus.forCode(streamInfo.getStatus());
        if (status == StreamStatus.RESTARTED || status == StreamStatus.RESTARTING) {
            log.warn("GroupId={}, StreamId={} is already in {}", groupId, streamId, status);
            return true;
        }
        if (status != StreamStatus.SUSPENDED) {
            throw new BusinessException(
                    String.format("GroupId=%s, StreamId=%s, status=%s not correct for stream restart", groupId,
                            streamId,
                            status));
        }
        StreamResourceProcessForm processForm = genStreamProcessForm(groupInfo, streamInfo, GroupOperateType.RESTART);
        ProcessName processName = ProcessName.RESTART_STREAM_RESOURCE;
        if (sync) {
            WorkflowResult workflowResult = workflowService.start(processName, operator,
                    processForm);
            ProcessStatus processStatus = workflowResult.getProcessInfo().getStatus();
            return processStatus == ProcessStatus.COMPLETED;
        } else {
            executorService.execute(
                    () -> workflowService.start(processName, operator, processForm));
            return true;
        }
    }

    /**
     * Restart stream in synchronous/asynchronous way.
     */
    public boolean deleteProcess(String groupId, String streamId, String operator, boolean sync) {
        log.info("DeleteProcess for groupId={}, streamId={}", groupId, streamId);
        InlongGroupInfo groupInfo = groupService.get(groupId);
        if (groupInfo == null) {
            throw new BusinessException(ErrorCodeEnum.GROUP_NOT_FOUND);
        }
        GroupStatus groupStatus = GroupStatus.forCode(groupInfo.getStatus());
        if (groupStatus != GroupStatus.CONFIG_SUCCESSFUL
                && groupStatus != GroupStatus.RESTARTED
                && groupStatus != GroupStatus.SUSPENDED
                && groupStatus != GroupStatus.DELETING) {
            throw new BusinessException(
                    String.format("GroupId=%s, status=%s not correct for stream delete", groupId, groupStatus));
        }
        InlongStreamInfo streamInfo = streamService.get(groupId, streamId);
        if (streamInfo == null) {
            throw new BusinessException(ErrorCodeEnum.STREAM_NOT_FOUND);
        }
        StreamStatus status = StreamStatus.forCode(streamInfo.getStatus());
        if (status == StreamStatus.DELETED || status == StreamStatus.DELETING) {
            log.warn("GroupId={}, StreamId={} is already in {}", groupId, streamId, status);
            return true;
        }
        if (status == StreamStatus.CONFIG_ING
                || status == StreamStatus.RESTARTING
                || status == StreamStatus.SUSPENDING) {
            throw new BusinessException(
                    String.format("GroupId=%s, StreamId=%s, status=%s not correct for stream delete", groupId,
                            streamId,
                            status));
        }
        StreamResourceProcessForm processForm = genStreamProcessForm(groupInfo, streamInfo, GroupOperateType.DELETE);
        ProcessName processName = ProcessName.DELETE_STREAM_RESOURCE;
        if (sync) {
            WorkflowResult workflowResult = workflowService.start(processName, operator,
                    processForm);
            ProcessStatus processStatus = workflowResult.getProcessInfo().getStatus();
            if (processStatus == ProcessStatus.COMPLETED) {
                return streamService.delete(groupId, streamId, operator);
            } else {
                return false;
            }
        } else {
            executorService.execute(
                    () -> {
                        WorkflowResult workflowResult = workflowService.start(processName, operator, processForm);
                        ProcessStatus processStatus = workflowResult.getProcessInfo().getStatus();
                        if (processStatus == ProcessStatus.COMPLETED) {
                            streamService.delete(groupId, streamId, operator);
                        }
                    });
            return true;
        }
    }

    private StreamResourceProcessForm genStreamProcessForm(InlongGroupInfo groupInfo, InlongStreamInfo streamInfo,
            GroupOperateType operateType) {
        StreamResourceProcessForm processForm = new StreamResourceProcessForm();
        processForm.setGroupInfo(groupInfo);
        processForm.setStreamInfo(streamInfo);
        processForm.setGroupOperateType(operateType);
        return processForm;
    }
}
