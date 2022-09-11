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

package org.apache.inlong.manager.service.core.impl;

import org.apache.commons.lang3.StringUtils;
import org.apache.inlong.manager.common.enums.GlobalConstants;
import org.apache.inlong.manager.common.pojo.workflow.FilterKey;
import org.apache.inlong.manager.common.pojo.workflow.WorkflowApprover;
import org.apache.inlong.manager.common.pojo.workflow.WorkflowApproverFilterContext;
import org.apache.inlong.manager.common.pojo.workflow.WorkflowApproverQuery;
import org.apache.inlong.manager.common.util.CommonBeanUtils;
import org.apache.inlong.manager.common.util.Preconditions;
import org.apache.inlong.manager.dao.entity.WorkflowApproverEntity;
import org.apache.inlong.manager.dao.mapper.WorkflowApproverEntityMapper;
import org.apache.inlong.manager.service.core.WorkflowApproverService;
import org.apache.inlong.manager.workflow.core.ProcessDefinitionService;
import org.apache.inlong.manager.workflow.definition.UserTask;
import org.apache.inlong.manager.workflow.definition.WorkflowProcess;
import org.apache.inlong.manager.workflow.definition.WorkflowTask;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Operation of workflow approvers
 */
@Service
public class WorkflowApproverServiceImpl implements WorkflowApproverService {

    @Autowired
    private WorkflowApproverEntityMapper workflowApproverMapper;
    @Autowired
    private ProcessDefinitionService processDefinitionService;

    @Override
    public List<String> getApprovers(String processName, String taskName, WorkflowApproverFilterContext context) {
        WorkflowApproverQuery approverQuery = WorkflowApproverQuery.builder()
                .processName(processName)
                .taskName(taskName)
                .build();
        List<WorkflowApproverEntity> configs = workflowApproverMapper.selectByQuery(approverQuery);
        Map<String, List<WorkflowApproverEntity>> groupByFilterKey = configs.stream()
                .collect(Collectors.groupingBy(WorkflowApproverEntity::getFilterKey));

        Map<FilterKey, String> filterKey2ValueMap = context.toFilterKeyMap();
        return FilterKey.getFilterKeyByOrder()
                .stream()
                .map(FilterKey::name)
                .map(groupByFilterKey::get)
                .filter(Objects::nonNull)
                .flatMap(List::stream)
                .filter(config -> checkFilterCondition(filterKey2ValueMap, config))
                .findFirst()
                .map(WorkflowApproverEntity::getApprovers)
                .map(approvers -> Arrays.asList(approvers.split(",")))
                .orElse(null);
    }

    @Override
    public List<WorkflowApprover> list(WorkflowApproverQuery query) {
        List<WorkflowApproverEntity> entityList = workflowApproverMapper.selectByQuery(query);
        List<WorkflowApprover> approverList = CommonBeanUtils.copyListProperties(entityList, WorkflowApprover::new);
        approverList.forEach(config -> {
            WorkflowProcess process = processDefinitionService.getByName(config.getProcessName());
            if (process != null) {
                config.setProcessDisplayName(process.getDisplayName());
                config.setTaskDisplayName(Optional.ofNullable(process.getTaskByName(config.getTaskName())).map(
                        WorkflowTask::getDisplayName).orElse(null));
            }
        });

        return approverList;
    }

    @Override
    public void add(WorkflowApprover approver, String operator) {
        Date now = new Date();
        approver.setCreateTime(now);
        approver.setModifyTime(now);
        approver.setModifier(operator);
        approver.setCreator(operator);

        WorkflowProcess process = processDefinitionService.getByName(approver.getProcessName());
        Preconditions.checkNotNull(process, "process not exit with name: " + approver.getProcessName());
        WorkflowTask task = process.getTaskByName(approver.getTaskName());
        Preconditions.checkNotNull(task, "task not exit with name: " + approver.getTaskName());
        Preconditions.checkTrue(task instanceof UserTask, "task should be UserTask");

        List<WorkflowApproverEntity> exist = this.workflowApproverMapper.selectByQuery(
                WorkflowApproverQuery.builder()
                        .processName(approver.getProcessName())
                        .taskName(approver.getTaskName())
                        .filterKey(approver.getFilterKey().name())
                        .filterValue(approver.getFilterValue())
                        .build());

        Preconditions.checkEmpty(exist, "already exit the same config");

        WorkflowApproverEntity entity = CommonBeanUtils.copyProperties(approver, WorkflowApproverEntity::new);
        entity.setIsDeleted(GlobalConstants.UN_DELETED);
        int success = this.workflowApproverMapper.insert(entity);
        Preconditions.checkTrue(success == 1, "add failed");
    }

    @Override
    public void update(WorkflowApprover config, String operator) {
        Preconditions.checkNotNull(config, "config cannot be null");
        Preconditions.checkNotNull(config.getId(), "id cannot be null");

        WorkflowApproverEntity entity = workflowApproverMapper.selectByPrimaryKey(config.getId());
        Preconditions.checkNotNull(entity, "not exist with id:" + config.getId());

        WorkflowApproverEntity update = new WorkflowApproverEntity();
        update.setId(config.getId());
        update.setModifyTime(new Date());
        update.setModifier(operator);
        update.setApprovers(config.getApprovers());
        update.setFilterKey(config.getFilterKey().name());
        update.setFilterValue(config.getFilterValue());

        int success = this.workflowApproverMapper.updateByPrimaryKeySelective(update);
        Preconditions.checkTrue(success == 1, "update failed");
    }

    @Override
    public void delete(Integer id, String operator) {
        WorkflowApproverEntity entity = workflowApproverMapper.selectByPrimaryKey(id);
        Preconditions.checkNotNull(entity, "not exist with id:" + id);
        int success = this.workflowApproverMapper.deleteByPrimaryKey(id, operator);
        Preconditions.checkTrue(success == 1, "delete failed");
    }

    private boolean checkFilterCondition(Map<FilterKey, String> filterKey2ValueMap,
            WorkflowApproverEntity config) {
        FilterKey filterKey = FilterKey.fromName(config.getFilterKey());

        if (filterKey == null) {
            return false;
        }

        return FilterKey.DEFAULT.name().equals(config.getFilterKey())
                || StringUtils.equals(filterKey2ValueMap.get(filterKey), config.getFilterValue());
    }

}
