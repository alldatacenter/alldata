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

package org.apache.inlong.manager.service.workflow;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.google.common.collect.Maps;
import org.apache.inlong.manager.common.enums.ErrorCodeEnum;
import org.apache.inlong.manager.common.enums.ProcessName;
import org.apache.inlong.manager.common.enums.TaskStatus;
import org.apache.inlong.manager.common.util.Preconditions;
import org.apache.inlong.manager.dao.entity.WorkflowProcessEntity;
import org.apache.inlong.manager.dao.entity.WorkflowTaskEntity;
import org.apache.inlong.manager.pojo.common.PageResult;
import org.apache.inlong.manager.pojo.workflow.EventLogRequest;
import org.apache.inlong.manager.pojo.workflow.ListenerExecuteLog;
import org.apache.inlong.manager.pojo.workflow.ProcessCountRequest;
import org.apache.inlong.manager.pojo.workflow.ProcessCountResponse;
import org.apache.inlong.manager.pojo.workflow.ProcessDetailResponse;
import org.apache.inlong.manager.pojo.workflow.ProcessRequest;
import org.apache.inlong.manager.pojo.workflow.ProcessResponse;
import org.apache.inlong.manager.pojo.workflow.TaskCountRequest;
import org.apache.inlong.manager.pojo.workflow.TaskCountResponse;
import org.apache.inlong.manager.pojo.workflow.TaskExecuteLog;
import org.apache.inlong.manager.pojo.workflow.TaskLogRequest;
import org.apache.inlong.manager.pojo.workflow.TaskRequest;
import org.apache.inlong.manager.pojo.workflow.TaskResponse;
import org.apache.inlong.manager.pojo.workflow.WorkflowExecuteLog;
import org.apache.inlong.manager.pojo.workflow.WorkflowResult;
import org.apache.inlong.manager.pojo.workflow.form.process.ProcessForm;
import org.apache.inlong.manager.pojo.workflow.form.task.TaskForm;
import org.apache.inlong.manager.workflow.WorkflowContext;
import org.apache.inlong.manager.workflow.core.ProcessDefinitionService;
import org.apache.inlong.manager.workflow.core.ProcessService;
import org.apache.inlong.manager.workflow.core.TaskService;
import org.apache.inlong.manager.workflow.core.WorkflowQueryService;
import org.apache.inlong.manager.workflow.definition.UserTask;
import org.apache.inlong.manager.workflow.definition.WorkflowProcess;
import org.apache.inlong.manager.workflow.util.WorkflowUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Workflow service
 */
@Service
public class WorkflowServiceImpl implements WorkflowService {

    private static final Logger LOGGER = LoggerFactory.getLogger(WorkflowServiceImpl.class);

    @Autowired
    private WorkflowQueryService queryService;
    @Autowired
    private ProcessDefinitionService processDefService;
    @Autowired
    private ProcessService processService;
    @Autowired
    private TaskService taskService;
    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public WorkflowResult start(ProcessName process, String operator, ProcessForm form) {
        WorkflowContext context = processService.start(process.name(), operator, form);
        return WorkflowUtils.getResult(context);
    }

    @Override
    public WorkflowResult continueProcess(Integer processId, String operator, String remark) {
        WorkflowContext context = processService.continueProcess(processId, operator, remark);
        return WorkflowUtils.getResult(context);
    }

    @Override
    public WorkflowResult cancel(Integer processId, String operator, String remark) {
        WorkflowContext context = processService.cancel(processId, operator, remark);
        return WorkflowUtils.getResult(context);
    }

    @Override
    public WorkflowResult approve(Integer taskId, String remark, TaskForm form, String operator) {
        WorkflowContext context = taskService.approve(taskId, remark, form, operator);
        return WorkflowUtils.getResult(context);
    }

    @Override
    public WorkflowResult reject(Integer taskId, String remark, String operator) {
        WorkflowContext context = taskService.reject(taskId, remark, operator);
        return WorkflowUtils.getResult(context);
    }

    @Override
    public WorkflowResult transfer(Integer taskId, String remark, List<String> to, String operator) {
        WorkflowContext context = taskService.transfer(taskId, remark, to, operator);
        return WorkflowUtils.getResult(context);
    }

    @Override
    public WorkflowResult complete(Integer taskId, String remark, String operator) {
        WorkflowContext context = taskService.complete(taskId, remark, operator);
        return WorkflowUtils.getResult(context);
    }

    @Override
    public ProcessDetailResponse detail(Integer processId, Integer taskId, String operator) {
        return queryService.detail(processId, taskId, operator);
    }

    @Override
    public PageResult<ProcessResponse> listProcess(ProcessRequest query) {
        PageHelper.startPage(query.getPageNum(), query.getPageSize());
        Page<WorkflowProcessEntity> result = (Page<WorkflowProcessEntity>) queryService.listProcessEntity(query);
        PageInfo<ProcessResponse> pageInfo = result.toPageInfo(entity -> {
            ProcessResponse response = WorkflowUtils.getProcessResponse(entity);
            if (query.getIncludeShowInList()) {
                response.setShowInList(getShowInList(entity));
            }
            return response;
        });

        PageResult<ProcessResponse> pageResult = new PageResult<>(pageInfo.getList(),
                pageInfo.getTotal(), pageInfo.getPageNum(), pageInfo.getPageSize());

        if (query.getIncludeCurrentTask()) {
            TaskRequest taskQuery = TaskRequest.builder()
                    .type(UserTask.class.getSimpleName())
                    .statusSet(Collections.singleton(TaskStatus.PENDING))
                    .build();
            PageHelper.startPage(0, 100);
            pageResult.getList().forEach(this.addCurrentTask(taskQuery));
        }
        return pageResult;
    }

    @Override
    public PageResult<TaskResponse> listTask(TaskRequest query) {
        PageHelper.startPage(query.getPageNum(), query.getPageSize());
        Page<WorkflowTaskEntity> result = (Page<WorkflowTaskEntity>) queryService.listTaskEntity(query);

        PageInfo<TaskResponse> pageInfo = result.toPageInfo(WorkflowUtils::getTaskResponse);
        addShowInListForEachTask(pageInfo.getList());

        return new PageResult<>(pageInfo.getList(), pageInfo.getTotal(), pageInfo.getPageNum(), pageInfo.getPageSize());
    }

    @Override
    public ProcessCountResponse countProcess(ProcessCountRequest query) {
        return queryService.countProcess(query);
    }

    @Override
    public TaskCountResponse countTask(TaskCountRequest query) {
        return queryService.countTask(query);
    }

    @Override
    public PageResult<WorkflowExecuteLog> listTaskLogs(TaskLogRequest query) {
        Preconditions.expectNotNull(query, "task execute log query params cannot be null");

        String groupId = query.getInlongGroupId();
        List<String> processNameList = query.getProcessNames();
        Preconditions.expectNotBlank(groupId, ErrorCodeEnum.GROUP_ID_IS_EMPTY);
        Preconditions.expectNotEmpty(processNameList, "process name list cannot be null");

        ProcessRequest processRequest = new ProcessRequest();
        processRequest.setInlongGroupId(groupId);
        processRequest.setInlongStreamId(query.getInlongStreamId());
        processRequest.setNameList(processNameList);
        processRequest.setHidden(1);

        // Paging query process instance, construct process execution log
        PageHelper.startPage(query.getPageNum(), query.getPageSize());
        Page<WorkflowProcessEntity> entityPage = (Page<WorkflowProcessEntity>) queryService.listProcessEntity(
                processRequest);

        PageInfo<WorkflowExecuteLog> pageInfo = entityPage.toPageInfo(inst -> WorkflowExecuteLog.builder()
                .processId(inst.getId())
                .processDisplayName(inst.getDisplayName())
                .status(inst.getStatus())
                .startTime(inst.getStartTime())
                .endTime(inst.getEndTime())
                .build());

        // According to the process execution log, query the execution log of each task in the process
        for (WorkflowExecuteLog executeLog : pageInfo.getList()) {
            TaskRequest taskQuery = new TaskRequest();
            taskQuery.setProcessId(executeLog.getProcessId());
            taskQuery.setType(taskQuery.getType());
            List<TaskExecuteLog> taskExecuteLogs = queryService.listTaskEntity(taskQuery)
                    .stream()
                    .map(WorkflowUtils::getTaskExecuteLog)
                    .collect(Collectors.toList());

            // Set the listener execution logs
            for (TaskExecuteLog taskLog : taskExecuteLogs) {
                EventLogRequest eventLogQuery = new EventLogRequest();
                eventLogQuery.setTaskId(taskLog.getTaskId());
                List<ListenerExecuteLog> logs = queryService.listEventLog(eventLogQuery)
                        .stream()
                        .map(WorkflowUtils::getListenerExecuteLog)
                        .collect(Collectors.toList());
                taskLog.setListenerExecuteLogs(logs);
            }

            executeLog.setTaskExecuteLogs(taskExecuteLogs);
        }

        LOGGER.info("success to page list task execute logs for " + query);

        return new PageResult<>(pageInfo.getList(), pageInfo.getTotal(), pageInfo.getPageNum(),
                pageInfo.getPageSize());
    }

    private Consumer<ProcessResponse> addCurrentTask(TaskRequest query) {
        return plv -> {
            query.setProcessId(plv.getId());
            plv.setCurrentTasks(this.listTask(query).getList());
        };
    }

    private Map<String, Object> getShowInList(WorkflowProcessEntity processEntity) {
        WorkflowProcess process = processDefService.getByName(processEntity.getName());
        if (process == null || process.getFormClass() == null) {
            return null;
        }

        try {
            ProcessForm form = WorkflowUtils.parseProcessForm(objectMapper, processEntity.getFormData(), process);
            assert form != null;
            return form.showInList();
        } catch (Exception e) {
            LOGGER.error("get show list err: ", e);
        }
        return null;
    }

    private void addShowInListForEachTask(List<TaskResponse> taskList) {
        if (CollectionUtils.isEmpty(taskList)) {
            return;
        }
        PageHelper.clearPage();
        List<Integer> list = taskList.stream().map(TaskResponse::getProcessId).distinct().collect(Collectors.toList());
        ProcessRequest query = new ProcessRequest();
        query.setIdList(list);

        List<WorkflowProcessEntity> processEntities = queryService.listProcessEntity(query);
        Map<Integer, Map<String, Object>> processShowInListMap = Maps.newHashMap();
        processEntities.forEach(entity -> processShowInListMap.put(entity.getId(), getShowInList(entity)));
        taskList.forEach(task -> task.setShowInList(processShowInListMap.get(task.getProcessId())));
    }

}
