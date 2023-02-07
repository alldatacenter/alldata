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

package org.apache.inlong.manager.dao.mapper;

import org.apache.ibatis.annotations.Param;
import org.apache.inlong.manager.common.enums.TaskStatus;
import org.apache.inlong.manager.dao.entity.WorkflowTaskEntity;
import org.apache.inlong.manager.pojo.common.CountInfo;
import org.apache.inlong.manager.pojo.workflow.TaskCountRequest;
import org.apache.inlong.manager.pojo.workflow.TaskRequest;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Workflow task mapper
 */
@Repository
public interface WorkflowTaskEntityMapper {

    int insert(WorkflowTaskEntity workflowTaskEntity);

    WorkflowTaskEntity selectById(Integer id);

    List<WorkflowTaskEntity> selectByProcess(@Param("processId") Integer processId, @Param("status") TaskStatus status);

    List<WorkflowTaskEntity> selectByQuery(TaskRequest query);

    int countByStatus(@Param("processId") Integer processId, @Param("name") String name,
            @Param("status") TaskStatus status);

    List<CountInfo> countByQuery(TaskCountRequest query);

    int update(WorkflowTaskEntity workflowTaskEntity);

    /**
     * Physically delete all task infos based on process ids
     *
     * @return rows deleted
     */
    int deleteByProcessIds(@Param("processIdList") List<Integer> processIdList);

}
