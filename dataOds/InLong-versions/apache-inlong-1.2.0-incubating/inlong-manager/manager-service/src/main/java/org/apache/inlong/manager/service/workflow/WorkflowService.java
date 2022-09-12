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

import com.github.pagehelper.PageInfo;
import org.apache.inlong.manager.common.pojo.workflow.ProcessDetailResponse;
import org.apache.inlong.manager.common.pojo.workflow.ProcessQuery;
import org.apache.inlong.manager.common.pojo.workflow.ProcessResponse;
import org.apache.inlong.manager.common.pojo.workflow.ProcessCountQuery;
import org.apache.inlong.manager.common.pojo.workflow.ProcessCountResponse;
import org.apache.inlong.manager.common.pojo.workflow.TaskCountQuery;
import org.apache.inlong.manager.common.pojo.workflow.TaskExecuteLogQuery;
import org.apache.inlong.manager.common.pojo.workflow.TaskQuery;
import org.apache.inlong.manager.common.pojo.workflow.TaskResponse;
import org.apache.inlong.manager.common.pojo.workflow.TaskCountResponse;
import org.apache.inlong.manager.common.pojo.workflow.WorkflowResult;
import org.apache.inlong.manager.common.pojo.workflow.form.ProcessForm;
import org.apache.inlong.manager.common.pojo.workflow.form.TaskForm;

import java.util.List;

/**
 * Workflow service
 */
public interface WorkflowService {

    /**
     * Initiation process
     *
     * @param process Process name
     * @param applicant Applicant
     * @param form Process form
     * @return result
     */
    WorkflowResult start(ProcessName process, String applicant, ProcessForm form);

    /**
     * Cancellation process application
     *
     * @param processId Process id.
     * @param operator Operator.
     * @param remark Remarks information.
     * @return Workflow result.
     */
    WorkflowResult cancel(Integer processId, String operator, String remark);

    /**
     * Approval the process.
     *
     * @param taskId Task id.
     * @param form Form information.
     * @param operator Operator.
     * @return Workflow result.
     */
    WorkflowResult approve(Integer taskId, String remark, TaskForm form, String operator);

    /**
     * reject
     *
     * @param taskId Task ID
     * @param remark Remarks information
     * @param operator Operator
     * @return result
     */
    WorkflowResult reject(Integer taskId, String remark, String operator);

    /**
     * Change approver
     *
     * @param taskId Task ID
     * @param remark Remarks
     * @param to Transfer to
     * @param operator Operator
     * @return result
     */
    WorkflowResult transfer(Integer taskId, String remark, List<String> to, String operator);

    /**
     * Complete task-true to automatic task
     *
     * @param taskId Task id.
     * @param remark Remarks.
     * @param operator Operator.
     * @return Workflow result.
     */
    WorkflowResult complete(Integer taskId, String remark, String operator);

    /**
     * Query process details according to the tracking number
     *
     * @param processId Process id.
     * @param taskId Task id.
     * @return Detail info.
     */
    ProcessDetailResponse detail(Integer processId, Integer taskId, String operator);

    /**
     * Get a list of process.
     *
     * @param query Query conditions.
     * @return Process list.
     */
    PageInfo<ProcessResponse> listProcess(ProcessQuery query);

    /**
     * Get task list
     *
     * @param query Query conditions
     * @return List
     */
    PageInfo<TaskResponse> listTask(TaskQuery query);

    /**
     * Get process statistics
     *
     * @param query Query conditions
     * @return Statistical data
     */
    ProcessCountResponse countProcess(ProcessCountQuery query);

    /**
     * Get task statistics
     *
     * @param query Query conditions
     * @return Statistical data
     */
    TaskCountResponse countTask(TaskCountQuery query);

    /**
     * Get task execution log
     *
     * @param query Query conditions
     * @return Execution log
     */
    PageInfo<WorkflowExecuteLog> listTaskExecuteLogs(TaskExecuteLogQuery query);

}
