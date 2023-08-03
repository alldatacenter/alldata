/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.seatunnel.app.dal.dao.impl;

import org.apache.seatunnel.app.dal.dao.TaskDefinitionDao;
import org.apache.seatunnel.app.dal.entity.ProcessTaskRelation;
import org.apache.seatunnel.app.dal.entity.TaskDefinition;
import org.apache.seatunnel.app.dal.entity.TaskMainInfo;
import org.apache.seatunnel.app.dal.mapper.ProcessTaskRelationMapper;
import org.apache.seatunnel.app.dal.mapper.TaskDefinitionMapper;

import org.apache.commons.collections4.CollectionUtils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class TaskDefinitionDaoImpl implements TaskDefinitionDao {

    @Autowired private TaskDefinitionMapper taskDefinitionMapper;

    @Autowired private ProcessTaskRelationMapper processTaskRelationMapper;

    @Override
    public List<TaskMainInfo> queryByDataSourceId(Long dataSourceId) {
        return processTaskRelationMapper.queryByDataSourceId(dataSourceId);
    }

    @Override
    public List<TaskDefinition> queryTaskDefinitions(Collection<Long> taskCodes) {
        if (CollectionUtils.isEmpty(taskCodes)) {
            return Collections.emptyList();
        }
        return taskDefinitionMapper.queryByCodeList(taskCodes);
    }

    @Override
    public List<TaskDefinition> queryByWorkflowDefinitionCodeAndVersion(
            Long workflowDefinitionCode, Integer workflowDefinitionVersion) {
        List<ProcessTaskRelation> processTaskRelations =
                processTaskRelationMapper.queryProcessTaskRelationsByProcessDefinitionCode(
                        workflowDefinitionCode, workflowDefinitionVersion);
        Set<Long> taskCodes = new HashSet<>();
        processTaskRelations.forEach(
                processTaskRelation -> {
                    taskCodes.add(processTaskRelation.getPreTaskCode());
                    taskCodes.add(processTaskRelation.getPostTaskCode());
                });
        if (CollectionUtils.isEmpty(taskCodes)) {
            return Collections.emptyList();
        }
        return taskDefinitionMapper.queryByCodeList(taskCodes);
    }
}
