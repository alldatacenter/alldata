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

package org.apache.inlong.manager.workflow.core.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Maps;
import lombok.extern.slf4j.Slf4j;
import org.apache.inlong.manager.common.enums.ProcessStatus;
import org.apache.inlong.manager.common.enums.TaskStatus;
import org.apache.inlong.manager.common.exceptions.WorkflowException;
import org.apache.inlong.manager.common.pojo.common.CountInfo;
import org.apache.inlong.manager.common.pojo.workflow.ElementDTO;
import org.apache.inlong.manager.common.pojo.workflow.EventLogQuery;
import org.apache.inlong.manager.common.pojo.workflow.ProcessCountQuery;
import org.apache.inlong.manager.common.pojo.workflow.ProcessCountResponse;
import org.apache.inlong.manager.common.pojo.workflow.ProcessDetailResponse;
import org.apache.inlong.manager.common.pojo.workflow.ProcessQuery;
import org.apache.inlong.manager.common.pojo.workflow.TaskCountQuery;
import org.apache.inlong.manager.common.pojo.workflow.TaskCountResponse;
import org.apache.inlong.manager.common.pojo.workflow.TaskQuery;
import org.apache.inlong.manager.common.pojo.workflow.TaskResponse;
import org.apache.inlong.manager.common.pojo.workflow.WorkflowApproverQuery;
import org.apache.inlong.manager.common.pojo.workflow.WorkflowBriefDTO;
import org.apache.inlong.manager.common.pojo.workflow.form.TaskForm;
import org.apache.inlong.manager.dao.entity.WorkflowApproverEntity;
import org.apache.inlong.manager.dao.entity.WorkflowEventLogEntity;
import org.apache.inlong.manager.dao.entity.WorkflowProcessEntity;
import org.apache.inlong.manager.dao.entity.WorkflowTaskEntity;
import org.apache.inlong.manager.dao.mapper.WorkflowApproverEntityMapper;
import org.apache.inlong.manager.dao.mapper.WorkflowEventLogEntityMapper;
import org.apache.inlong.manager.dao.mapper.WorkflowProcessEntityMapper;
import org.apache.inlong.manager.dao.mapper.WorkflowTaskEntityMapper;
import org.apache.inlong.manager.workflow.WorkflowContext;
import org.apache.inlong.manager.workflow.core.ProcessDefinitionRepository;
import org.apache.inlong.manager.workflow.core.WorkflowQueryService;
import org.apache.inlong.manager.workflow.definition.Element;
import org.apache.inlong.manager.workflow.definition.NextableElement;
import org.apache.inlong.manager.workflow.definition.StartEvent;
import org.apache.inlong.manager.workflow.definition.UserTask;
import org.apache.inlong.manager.workflow.definition.WorkflowProcess;
import org.apache.inlong.manager.workflow.definition.WorkflowTask;
import org.apache.inlong.manager.workflow.util.WorkflowBeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Query service
 */
@Slf4j
@Service
public class WorkflowQueryServiceImpl implements WorkflowQueryService {

    @Autowired
    private ProcessDefinitionRepository definitionRepository;
    @Autowired
    private WorkflowProcessEntityMapper processEntityMapper;
    @Autowired
    private WorkflowTaskEntityMapper taskEntityMapper;
    @Autowired
    private WorkflowEventLogEntityMapper eventLogMapper;
    @Autowired
    private WorkflowApproverEntityMapper approverMapper;
    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public WorkflowProcessEntity getProcessEntity(Integer processId) {
        return processEntityMapper.selectById(processId);
    }

    @Override
    public List<WorkflowTaskEntity> listApproveHistory(Integer processId) {
        TaskQuery request = new TaskQuery();
        request.setProcessId(processId);
        request.setStatusSet(TaskStatus.COMPLETED_STATUS);
        return taskEntityMapper.selectByQuery(request);
    }

    @Override
    public WorkflowTaskEntity getTaskEntity(Integer taskId) {
        return taskEntityMapper.selectById(taskId);
    }

    @Override
    public List<WorkflowProcessEntity> listProcessEntity(ProcessQuery query) {
        return processEntityMapper.selectByCondition(query);
    }

    @Override
    public List<WorkflowTaskEntity> listTaskEntity(TaskQuery taskQuery) {
        return taskEntityMapper.selectByQuery(taskQuery);
    }

    @Override
    public ProcessCountResponse countProcess(ProcessCountQuery request) {
        List<CountInfo> result = processEntityMapper.countByQuery(request);

        Map<String, Integer> countByState = result.stream()
                .collect(Collectors.toMap(CountInfo::getKey, CountInfo::getValue));

        return ProcessCountResponse.builder()
                .totalApplyCount(countByState.values().stream().mapToInt(c -> c).sum())
                .totalApproveCount(countByState.getOrDefault(ProcessStatus.COMPLETED.name(), 0))
                .totalRejectCount(countByState.getOrDefault(ProcessStatus.REJECTED.name(), 0))
                .totalProcessingCount(countByState.getOrDefault(ProcessStatus.PROCESSING.name(), 0))
                .totalCancelCount(countByState.getOrDefault(ProcessStatus.CANCELED.name(), 0))
                .build();
    }

    @Override
    public TaskCountResponse countTask(TaskCountQuery query) {
        List<CountInfo> result = taskEntityMapper.countByQuery(query);
        TaskCountResponse response = new TaskCountResponse();
        for (CountInfo info : result) {
            String status = info.getKey();
            int total = info.getValue();

            if (TaskStatus.PENDING.name().equals(status)) {
                response.setTotalPendingCount(total);
            } else if (TaskStatus.REJECTED.name().equals(status)) {
                response.setTotalRejectCount(total);
            } else if (TaskStatus.APPROVED.name().equals(status)) {
                response.setTotalApproveCount(total);
            } else if (TaskStatus.TRANSFERRED.name().equals(status)) {
                response.setTotalTransferCount(total);
            }
        }

        return response;
    }

    @Override
    public ProcessDetailResponse detail(Integer processId, Integer taskId, String operator) {
        WorkflowProcessEntity processEntity = this.getProcessEntity(processId);
        if (processEntity == null) {
            return null;
        }

        WorkflowTaskEntity taskEntity = null;
        if (taskId == null) {
            if (!operator.equals(processEntity.getApplicant())) {
                throw new WorkflowException("current user is not the applicant of the process");
            }
        } else {
            taskEntity = this.getTaskEntity(taskId);
            List<String> taskApprovers = Arrays.asList(taskEntity.getApprovers().split(","));
            if (!taskApprovers.contains(operator)) {
                WorkflowApproverQuery query = new WorkflowApproverQuery();
                query.setProcessName(processEntity.getName());
                List<WorkflowApproverEntity> approverList = approverMapper.selectByQuery(query);
                boolean match = approverList.stream().anyMatch(approverEntity -> {
                    String[] approverArr = approverEntity.getApprovers().split(",");
                    for (String approver : approverArr) {
                        if (Objects.equals(approver, operator)) {
                            return true;
                        }
                    }
                    return false;
                });
                if (!match) {
                    throw new WorkflowException("current user is not the approver of the process");
                }
            }
        }

        WorkflowProcess process = definitionRepository.get(processEntity.getName());
        TaskResponse currentTask = null;
        if (taskEntity != null) {
            currentTask = WorkflowBeanUtils.fromTaskEntity(taskEntity);
            if (process != null && TaskStatus.PENDING.equals(currentTask.getStatus())) {
                WorkflowTask task = process.getTaskByName(currentTask.getName());
                currentTask.setFormData(this.getEmptyTaskForm(task));
            }
            if (!processId.equals(currentTask.getProcessId())) {
                throw new WorkflowException("task [" + taskId + "] not belongs to process [" + processId + "]");
            }
        }

        ProcessDetailResponse detailResponse = this.getProcessDetail(processId, processEntity);
        detailResponse.setCurrentTask(currentTask);

        if (process == null || process.getProcessDetailHandler() == null) {
            return detailResponse;
        }

        return process.getProcessDetailHandler().handle(detailResponse);
    }

    @Override
    public WorkflowEventLogEntity getEventLog(Integer id) {
        return eventLogMapper.selectById(id);
    }

    @Override
    public List<WorkflowEventLogEntity> listEventLog(EventLogQuery request) {
        return eventLogMapper.selectByCondition(request);
    }

    private ProcessDetailResponse getProcessDetail(Integer processId, WorkflowProcessEntity processEntity) {
        List<WorkflowTaskEntity> taskList = this.listApproveHistory(processId);
        List<TaskResponse> history = taskList.stream().map(WorkflowBeanUtils::fromTaskEntity)
                .collect(Collectors.toList());

        WorkflowBriefDTO workflowDTO = this.getBriefFromProcessEntity(processEntity);
        ProcessDetailResponse processDetail = new ProcessDetailResponse();
        processDetail.setProcessInfo(WorkflowBeanUtils.fromProcessEntity(processEntity));
        processDetail.setTaskHistory(history);
        processDetail.setWorkflow(workflowDTO);
        return processDetail;
    }

    private WorkflowBriefDTO getBriefFromProcessEntity(WorkflowProcessEntity processEntity) {
        WorkflowProcess process = definitionRepository.get(processEntity.getName());
        if (process == null) {
            return null;
        }

        Map<String, TaskStatus> nameStatusMap = this.getTaskNameStatusMap(processEntity);
        ElementDTO elementDTO = new ElementDTO();
        StartEvent startEvent = process.getStartEvent();
        elementDTO.setName(startEvent.getName());
        elementDTO.setDisplayName(startEvent.getDisplayName());

        WorkflowContext context = WorkflowBeanUtils.buildContext(objectMapper, process, processEntity);
        addNext(startEvent, elementDTO, context, nameStatusMap);

        WorkflowBriefDTO briefDTO = new WorkflowBriefDTO();
        briefDTO.setName(process.getName());
        briefDTO.setDisplayName(process.getDisplayName());
        briefDTO.setType(process.getType());
        briefDTO.setStartEvent(elementDTO);
        return briefDTO;
    }

    private void addNext(NextableElement nextableElement, ElementDTO elementDTO, WorkflowContext context,
            Map<String, TaskStatus> nameToStatusMap) {
        for (Element element : nextableElement.getNextList(context)) {
            ElementDTO nextElement = new ElementDTO();
            nextElement.setName(element.getName());
            nextElement.setDisplayName(element.getDisplayName());

            if (element instanceof UserTask) {
                nextElement.setApprovers(((UserTask) element).getApproverAssign().assign(context));
                nextElement.setStatus(nameToStatusMap.get(element.getName()));
            }

            elementDTO.getNext().add(nextElement);
            if (!(element instanceof NextableElement)) {
                continue;
            }
            addNext((NextableElement) element, nextElement, context, nameToStatusMap);
        }
    }

    private Map<String, TaskStatus> getTaskNameStatusMap(WorkflowProcessEntity processEntity) {
        TaskQuery request = TaskQuery.builder().processId(processEntity.getId()).build();
        List<WorkflowTaskEntity> allTasks = taskEntityMapper.selectByQuery(request)
                .stream()
                .sorted(Comparator.comparing(WorkflowTaskEntity::getId)
                        .thenComparing(Comparator.nullsLast(Comparator.comparing(WorkflowTaskEntity::getEndTime))))
                .collect(Collectors.toList());

        Map<String, TaskStatus> nameStatusMap = Maps.newHashMap();
        allTasks.forEach(task -> nameStatusMap.put(task.getName(), TaskStatus.valueOf(task.getStatus())));
        return nameStatusMap;
    }

    private TaskForm getEmptyTaskForm(WorkflowTask task) {
        if (!(task instanceof UserTask)) {
            return null;
        }
        UserTask userTask = (UserTask) task;
        if (userTask.getFormClass() == null) {
            return null;
        }

        try {
            return userTask.getFormClass().newInstance();
        } catch (Exception e) {
            throw new WorkflowException("get form name failed with name " + userTask.getFormClass().getName());
        }
    }

}
