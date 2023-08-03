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

package org.apache.seatunnel.app.dal.mapper;

import org.apache.seatunnel.app.dal.entity.TaskDefinition;
import org.apache.seatunnel.app.dal.entity.TaskDefinitionExpand;
import org.apache.seatunnel.app.dal.entity.TaskMainInfo;

import org.apache.ibatis.annotations.Param;

import org.springframework.context.annotation.Primary;

import java.util.List;
import java.util.Set;

@Primary
public interface SeatunnalTaskDefinitionMapper extends TaskDefinitionMapper {

    /**
     * Query all specific task type from single process definition
     *
     * @param processCode process definition code
     * @param taskType Task type of this process definition code
     * @return List of ProcessTaskRelationMapper
     */
    List<TaskDefinitionExpand> queryProcessTaskType(
            @Param("processCode") Long processCode, @Param("taskType") String taskType);

    /**
     * query all task definition list
     *
     * @param taskCodesList taskCodesList
     * @return task definition list
     */
    List<TaskDefinition> queryAllTaskProcessDefinition(
            @Param("taskCodesList") List<Long> taskCodesList);

    /**
     * query task definition by project codes and task types
     *
     * @param projectCodes
     * @param definitionCodes
     * @param taskTypes
     * @return
     */
    List<TaskDefinition> queryTaskDefinitionByProjectCodesAndTaskTypes(
            @Param("projectCodes") Set<Long> projectCodes,
            @Param("definitionCodes") Set<Long> definitionCodes,
            @Param("taskTypes") List<String> taskTypes);

    List<TaskMainInfo> queryTaskDefinitionBySubprocessTask(
            @Param("processDefinitionCode") Long processDefinitionCode);

    List<TaskMainInfo> queryTaskDefinitionByDependentTaskWithTaskCode(
            @Param("taskCode") Long taskCode);

    List<TaskMainInfo> queryTaskDefinitionByDependentTaskWithProcessCode(
            @Param("processDefinitionCode") Long processDefinitionCode);
}
