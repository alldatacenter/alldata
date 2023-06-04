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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.inlong.manager.common.exceptions.JsonException;
import org.apache.inlong.manager.common.util.Preconditions;
import org.apache.inlong.manager.dao.entity.WorkflowProcessEntity;
import org.apache.inlong.manager.dao.entity.WorkflowTaskEntity;
import org.apache.inlong.manager.dao.mapper.WorkflowProcessEntityMapper;
import org.apache.inlong.manager.dao.mapper.WorkflowTaskEntityMapper;
import org.apache.inlong.manager.pojo.workflow.form.process.ProcessForm;
import org.apache.inlong.manager.pojo.workflow.form.task.TaskForm;
import org.apache.inlong.manager.workflow.WorkflowAction;
import org.apache.inlong.manager.workflow.WorkflowContext;
import org.apache.inlong.manager.workflow.core.ProcessDefinitionRepository;
import org.apache.inlong.manager.workflow.core.WorkflowContextBuilder;
import org.apache.inlong.manager.workflow.definition.WorkflowProcess;
import org.apache.inlong.manager.workflow.definition.WorkflowTask;
import org.apache.inlong.manager.workflow.util.WorkflowUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * Workflow context builder
 */
@Slf4j
@Service
public class WorkflowContextBuilderImpl implements WorkflowContextBuilder {

    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private ProcessDefinitionRepository definitionRepository;
    @Autowired
    private WorkflowProcessEntityMapper processEntityMapper;
    @Autowired
    private WorkflowTaskEntityMapper taskEntityMapper;

    @SneakyThrows
    @Override
    public WorkflowContext buildContextForProcess(String name, String applicant, ProcessForm form) {
        WorkflowProcess process = definitionRepository.get(name).clone();

        return new WorkflowContext()
                .setOperator(applicant)
                .setProcess(process)
                .setProcessForm(form);
    }

    @SneakyThrows
    @Override
    public WorkflowContext buildContextForProcess(Integer processId) {
        WorkflowProcessEntity processEntity = processEntityMapper.selectById(processId);
        Preconditions.expectNotNull(processEntity, "process not exist with id: " + processId);
        WorkflowProcess process = definitionRepository.get(processEntity.getName()).clone();

        return new WorkflowContext()
                .setOperator(processEntity.getApplicant())
                .setProcess(process)
                .setProcessForm(WorkflowUtils.parseProcessForm(objectMapper, processEntity.getFormData(), process))
                .setProcessEntity(processEntity);
    }

    @Override
    public WorkflowContext buildContextForTask(Integer taskId, WorkflowAction action, String remark, String operator) {
        return buildContextForTask(taskId, action, null, null, remark, operator);
    }

    @Override
    public WorkflowContext buildContextForTask(Integer taskId, WorkflowAction action, TaskForm taskForm, String remark,
            String operator) {
        return buildContextForTask(taskId, action, taskForm, null, remark, operator);
    }

    @Override
    public WorkflowContext buildContextForTask(Integer taskId, WorkflowAction action, List<String> transferToUsers,
            String remark, String operator) {
        return buildContextForTask(taskId, action, null, transferToUsers, remark, operator);
    }

    @SneakyThrows
    @Override
    public WorkflowContext buildContextForTask(Integer taskId, WorkflowAction action) {
        WorkflowTaskEntity taskEntity = taskEntityMapper.selectById(taskId);
        WorkflowProcess process = definitionRepository.get(taskEntity.getProcessName()).clone();
        TaskForm taskForm = WorkflowUtils.parseTaskForm(objectMapper, taskEntity, process);
        List<String> transferToUsers = getTransferToUsers(taskEntity.getExtParams());
        return buildContextForTask(taskId, action, taskForm, transferToUsers, taskEntity.getRemark(),
                taskEntity.getOperator());
    }

    @SneakyThrows
    private WorkflowContext buildContextForTask(Integer taskId, WorkflowAction action, TaskForm taskForm,
            List<String> transferToUsers, String remark, String operator) {
        WorkflowTaskEntity taskEntity = taskEntityMapper.selectById(taskId);
        Preconditions.expectNotNull(taskEntity, "task not exist with id: " + taskId);

        WorkflowProcessEntity processEntity = processEntityMapper.selectById(taskEntity.getProcessId());
        WorkflowProcess process = definitionRepository.get(processEntity.getName()).clone();
        ProcessForm processForm = WorkflowUtils.parseProcessForm(objectMapper, processEntity.getFormData(), process);
        WorkflowTask task = process.getTaskByName(taskEntity.getName());
        return new WorkflowContext().setProcess(process)
                .setOperator(processEntity.getApplicant())
                .setProcessForm(processForm)
                .setProcessEntity(processEntity)
                .setCurrentElement(task)
                .setActionContext(new WorkflowContext.ActionContext()
                        .setAction(action)
                        .setTaskEntity(taskEntity)
                        .setTask(task)
                        .setForm(taskForm)
                        .setTransferToUsers(transferToUsers)
                        .setOperator(operator)
                        .setRemark(remark));
    }

    @SuppressWarnings("unchecked")
    private List<String> getTransferToUsers(String ext) {
        if (StringUtils.isEmpty(ext)) {
            return Lists.newArrayList();
        }
        try {
            Map<String, Object> extMap = objectMapper.readValue(ext,
                    objectMapper.getTypeFactory().constructMapType(Map.class, String.class, Object.class));
            if (!extMap.containsKey(WorkflowTaskEntity.EXT_TRANSFER_USER_KEY)) {
                return Lists.newArrayList();
            }
            if (extMap.get(WorkflowTaskEntity.EXT_TRANSFER_USER_KEY) instanceof List) {
                return (List<String>) extMap.get(WorkflowTaskEntity.EXT_TRANSFER_USER_KEY);
            }
        } catch (JsonProcessingException e) {
            log.error("parse transfer users error: ", e);
            throw new JsonException("parse transfer users error");
        }

        return null;
    }

}
