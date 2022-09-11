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

package org.apache.inlong.manager.workflow.core;

import org.apache.inlong.manager.common.pojo.workflow.EventLogQuery;
import org.apache.inlong.manager.common.pojo.workflow.ProcessCountQuery;
import org.apache.inlong.manager.common.pojo.workflow.ProcessCountResponse;
import org.apache.inlong.manager.common.pojo.workflow.ProcessDetailResponse;
import org.apache.inlong.manager.common.pojo.workflow.ProcessQuery;
import org.apache.inlong.manager.common.pojo.workflow.TaskCountQuery;
import org.apache.inlong.manager.common.pojo.workflow.TaskCountResponse;
import org.apache.inlong.manager.common.pojo.workflow.TaskQuery;
import org.apache.inlong.manager.dao.entity.WorkflowEventLogEntity;
import org.apache.inlong.manager.dao.entity.WorkflowProcessEntity;
import org.apache.inlong.manager.dao.entity.WorkflowTaskEntity;

import java.util.List;

/**
 * WorkflowProcess query service
 */
public interface WorkflowQueryService {

    /**
     * Get an instance of a process sheet
     *
     * @param processId WorkflowProcess ID
     * @return WorkflowProcess single instance
     */
    WorkflowProcessEntity getProcessEntity(Integer processId);

    /**
     * Obtain the approval history according to the process ticket number
     *
     * @param processId WorkflowProcess ID
     * @return Approval history
     */
    List<WorkflowTaskEntity> listApproveHistory(Integer processId);

    /**
     * Obtain task instance based on task ID
     *
     * @param taskId WorkflowTask ID
     * @return WorkflowTask instance
     */
    WorkflowTaskEntity getTaskEntity(Integer taskId);

    /**
     * Query the list of process sheet
     *
     * @param processRequest Query conditions
     * @return The list of the process sheet
     */
    List<WorkflowProcessEntity> listProcessEntity(ProcessQuery processRequest);

    /**
     * Query task list
     *
     * @param taskQuery Query conditions
     * @return the list of task sheet
     */
    List<WorkflowTaskEntity> listTaskEntity(TaskQuery taskQuery);

    /**
     * WorkflowProcess statistics
     *
     * @param query Query conditions
     * @return statistical results
     */
    ProcessCountResponse countProcess(ProcessCountQuery query);

    /**
     * WorkflowTask statistics
     *
     * @param query Query conditions
     * @return statistical results
     */
    TaskCountResponse countTask(TaskCountQuery query);

    /**
     * Get the details of the process
     *
     * @param processId Process ID
     * @param taskId Task ID
     * @param operator Operator
     * @return Detail
     */
    ProcessDetailResponse detail(Integer processId, Integer taskId, String operator);

    /**
     * Get event log based on ID
     */
    WorkflowEventLogEntity getEventLog(Integer id);

    /**
     * Query event logs based on conditions
     *
     * @param query Query conditions
     * @return the list of log
     */
    List<WorkflowEventLogEntity> listEventLog(EventLogQuery query);

}
