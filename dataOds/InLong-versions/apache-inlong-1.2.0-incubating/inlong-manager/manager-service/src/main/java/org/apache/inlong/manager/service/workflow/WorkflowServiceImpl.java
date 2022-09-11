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
import org.apache.inlong.manager.common.enums.TaskStatus;
import org.apache.inlong.manager.common.exceptions.WorkflowNoRollbackException;
import org.apache.inlong.manager.common.pojo.workflow.EventLogQuery;
import org.apache.inlong.manager.common.pojo.workflow.ProcessCountQuery;
import org.apache.inlong.manager.common.pojo.workflow.ProcessCountResponse;
import org.apache.inlong.manager.common.pojo.workflow.ProcessDetailResponse;
import org.apache.inlong.manager.common.pojo.workflow.ProcessQuery;
import org.apache.inlong.manager.common.pojo.workflow.ProcessResponse;
import org.apache.inlong.manager.common.pojo.workflow.TaskCountQuery;
import org.apache.inlong.manager.common.pojo.workflow.TaskCountResponse;
import org.apache.inlong.manager.common.pojo.workflow.TaskExecuteLogQuery;
import org.apache.inlong.manager.common.pojo.workflow.TaskQuery;
import org.apache.inlong.manager.common.pojo.workflow.TaskResponse;
import org.apache.inlong.manager.common.pojo.workflow.WorkflowResult;
import org.apache.inlong.manager.common.pojo.workflow.form.ProcessForm;
import org.apache.inlong.manager.common.pojo.workflow.form.TaskForm;
import org.apache.inlong.manager.common.util.Preconditions;
import org.apache.inlong.manager.dao.entity.WorkflowProcessEntity;
import org.apache.inlong.manager.dao.entity.WorkflowTaskEntity;
import org.apache.inlong.manager.service.workflow.WorkflowExecuteLog.ListenerExecutorLog;
import org.apache.inlong.manager.service.workflow.WorkflowExecuteLog.TaskExecutorLog;
import org.apache.inlong.manager.workflow.WorkflowContext;
import org.apache.inlong.manager.workflow.core.ProcessDefinitionService;
import org.apache.inlong.manager.workflow.core.ProcessService;
import org.apache.inlong.manager.workflow.core.TaskService;
import org.apache.inlong.manager.workflow.core.WorkflowQueryService;
import org.apache.inlong.manager.workflow.definition.UserTask;
import org.apache.inlong.manager.workflow.definition.WorkflowProcess;
import org.apache.inlong.manager.workflow.util.WorkflowBeanUtils;
import org.apache.inlong.manager.workflow.util.WorkflowFormParserUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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
    @Transactional(noRollbackFor = WorkflowNoRollbackException.class, rollbackFor = Exception.class)
    public WorkflowResult start(ProcessName process, String applicant, ProcessForm form) {
        WorkflowContext context = processService.start(process.name(), applicant, form);
        return WorkflowBeanUtils.result(context);
    }

    @Override
    @Transactional(noRollbackFor = WorkflowNoRollbackException.class, rollbackFor = Exception.class)
    public WorkflowResult cancel(Integer processId, String operator, String remark) {
        WorkflowContext context = processService.cancel(processId, operator, remark);
        return WorkflowBeanUtils.result(context);
    }

    @Override
    @Transactional(noRollbackFor = WorkflowNoRollbackException.class, rollbackFor = Exception.class)
    public WorkflowResult approve(Integer taskId, String remark, TaskForm form, String operator) {
        WorkflowContext context = taskService.approve(taskId, remark, form, operator);
        return WorkflowBeanUtils.result(context);
    }

    @Override
    @Transactional(noRollbackFor = WorkflowNoRollbackException.class, rollbackFor = Exception.class)
    public WorkflowResult reject(Integer taskId, String remark, String operator) {
        WorkflowContext context = taskService.reject(taskId, remark, operator);
        return WorkflowBeanUtils.result(context);
    }

    @Override
    @Transactional(noRollbackFor = WorkflowNoRollbackException.class, rollbackFor = Exception.class)
    public WorkflowResult transfer(Integer taskId, String remark, List<String> to, String operator) {
        WorkflowContext context = taskService.transfer(taskId, remark, to, operator);
        return WorkflowBeanUtils.result(context);
    }

    @Override
    @Transactional(noRollbackFor = WorkflowNoRollbackException.class, rollbackFor = Exception.class)
    public WorkflowResult complete(Integer taskId, String remark, String operator) {
        WorkflowContext context = taskService.complete(taskId, remark, operator);
        return WorkflowBeanUtils.result(context);
    }

    @Override
    public ProcessDetailResponse detail(Integer processId, Integer taskId, String operator) {
        return queryService.detail(processId, taskId, operator);
    }

    @Override
    public PageInfo<ProcessResponse> listProcess(ProcessQuery query) {
        PageHelper.startPage(query.getPageNum(), query.getPageSize());
        Page<WorkflowProcessEntity> result = (Page<WorkflowProcessEntity>) queryService.listProcessEntity(query);
        PageInfo<ProcessResponse> pageInfo = result.toPageInfo(entity -> {
            ProcessResponse response = WorkflowBeanUtils.fromProcessEntity(entity);
            if (query.getIncludeShowInList()) {
                response.setShowInList(getShowInList(entity));
            }
            return response;
        });

        pageInfo.setTotal(result.getTotal());

        if (query.getIncludeCurrentTask()) {
            TaskQuery taskQuery = TaskQuery.builder()
                    .type(UserTask.class.getSimpleName())
                    .statusSet(Collections.singleton(TaskStatus.PENDING))
                    .build();
            PageHelper.startPage(0, 100);
            pageInfo.getList().forEach(this.addCurrentTask(taskQuery));
        }
        return pageInfo;
    }

    @Override
    public PageInfo<TaskResponse> listTask(TaskQuery query) {
        PageHelper.startPage(query.getPageNum(), query.getPageSize());
        Page<WorkflowTaskEntity> result = (Page<WorkflowTaskEntity>) queryService.listTaskEntity(query);
        PageInfo<TaskResponse> pageInfo = result.toPageInfo(WorkflowBeanUtils::fromTaskEntity);
        addShowInListForEachTask(pageInfo.getList());
        pageInfo.setTotal(result.getTotal());

        return pageInfo;
    }

    @Override
    public ProcessCountResponse countProcess(ProcessCountQuery query) {
        return queryService.countProcess(query);
    }

    @Override
    public TaskCountResponse countTask(TaskCountQuery query) {
        return queryService.countTask(query);
    }

    @Override
    public PageInfo<WorkflowExecuteLog> listTaskExecuteLogs(TaskExecuteLogQuery query) {
        Preconditions.checkNotNull(query, "task execute log query params cannot be null");

        String groupId = query.getInlongGroupId();
        List<String> processNameList = query.getProcessNames();
        Preconditions.checkNotEmpty(groupId, "inlong group id cannot be null");
        Preconditions.checkNotEmpty(processNameList, "process name list cannot be null");

        ProcessQuery processRequest = new ProcessQuery();
        processRequest.setInlongGroupId(groupId);
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
                .build()
        );

        // According to the process execution log, query the execution log of each task in the process
        for (WorkflowExecuteLog executeLog : pageInfo.getList()) {
            TaskQuery taskQuery = new TaskQuery();
            taskQuery.setProcessId(executeLog.getProcessId());
            taskQuery.setType(taskQuery.getType());
            List<TaskExecutorLog> executorLogs = queryService.listTaskEntity(taskQuery)
                    .stream()
                    .map(TaskExecutorLog::buildFromTaskInst)
                    .collect(Collectors.toList());

            // Set the execution log of the task's listener
            for (TaskExecutorLog taskExecutorLog : executorLogs) {
                EventLogQuery eventLogQuery = new EventLogQuery();
                eventLogQuery.setTaskId(taskExecutorLog.getTaskId());
                List<ListenerExecutorLog> logs = queryService.listEventLog(eventLogQuery)
                        .stream()
                        .map(ListenerExecutorLog::fromEventLog)
                        .collect(Collectors.toList());
                taskExecutorLog.setListenerExecutorLogs(logs);
            }

            executeLog.setTaskExecutorLogs(executorLogs);
        }

        LOGGER.info("success to page list task execute logs for " + query);
        pageInfo.setTotal(entityPage.getTotal());
        return pageInfo;
    }

    private List<TaskExecutorLog> getTaskExecutorLogs(Integer processId, String taskType) {
        TaskQuery taskQuery = new TaskQuery();
        taskQuery.setProcessId(processId);
        taskQuery.setType(taskType);
        return queryService.listTaskEntity(taskQuery)
                .stream()
                .map(TaskExecutorLog::buildFromTaskInst)
                .collect(Collectors.toList());
    }

    private List<ListenerExecutorLog> getListenerExecutorLogs(TaskExecutorLog executorLog) {
        EventLogQuery query = EventLogQuery.builder().taskId(executorLog.getTaskId()).build();
        return queryService.listEventLog(query)
                .stream()
                .map(ListenerExecutorLog::fromEventLog)
                .collect(Collectors.toList());
    }

    private Consumer<ProcessResponse> addCurrentTask(TaskQuery query) {
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
            ProcessForm processForm = WorkflowFormParserUtils.parseProcessForm(objectMapper,
                    processEntity.getFormData(), process);
            assert processForm != null;
            return processForm.showInList();
        } catch (Exception e) {
            LOGGER.error("get showIn list err", e);
        }
        return null;
    }

    private void addShowInListForEachTask(List<TaskResponse> taskList) {
        if (CollectionUtils.isEmpty(taskList)) {
            return;
        }
        PageHelper.clearPage();
        List<Integer> list = taskList.stream().map(TaskResponse::getProcessId).distinct().collect(Collectors.toList());
        ProcessQuery query = new ProcessQuery();
        query.setIdList(list);

        List<WorkflowProcessEntity> processEntities = queryService.listProcessEntity(query);
        Map<Integer, Map<String, Object>> processShowInListMap = Maps.newHashMap();
        processEntities.forEach(entity -> processShowInListMap.put(entity.getId(), getShowInList(entity)));
        taskList.forEach(task -> task.setShowInList(processShowInListMap.get(task.getProcessId())));
    }

}
