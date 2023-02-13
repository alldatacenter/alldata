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

package org.apache.inlong.manager.client.api;

import org.apache.inlong.manager.pojo.common.PageResult;
import org.apache.inlong.manager.pojo.workflow.ProcessDetailResponse;
import org.apache.inlong.manager.pojo.workflow.ProcessRequest;
import org.apache.inlong.manager.pojo.workflow.ProcessResponse;
import org.apache.inlong.manager.pojo.workflow.TaskRequest;
import org.apache.inlong.manager.pojo.workflow.TaskResponse;
import org.apache.inlong.manager.pojo.workflow.WorkflowOperationRequest;
import org.apache.inlong.manager.pojo.workflow.WorkflowResult;

public interface Workflow {

    /**
     * Initiation process
     *
     * @param request workflow operation request
     * @return workflow result info
     */
    WorkflowResult start(WorkflowOperationRequest request);

    /**
     * Initiation process
     *
     * @param processId process id
     * @param request workflow operation request
     * @return workflow result info
     */
    WorkflowResult cancel(Integer processId, WorkflowOperationRequest request);

    /**
     * Continue process when pending or failed
     *
     * @param processId process id
     * @param request workflow operation request
     * @return workflow result info
     */
    WorkflowResult continueProcess(Integer processId, WorkflowOperationRequest request);

    /**
     * Cancellation process application
     *
     * @param taskId taskId
     * @param request workflow operation request
     * @return workflow result info
     */
    WorkflowResult reject(Integer taskId, WorkflowOperationRequest request);

    /**
     * Complete task-true to automatic task
     *
     * @param taskId taskId
     * @param request workflow operation request
     * @return workflow result info
     */
    WorkflowResult complete(Integer taskId, WorkflowOperationRequest request);

    /**
     * Query process details according to the tracking number
     *
     * @param processId processId
     * @param taskId taskId
     * @return process detail response
     */
    ProcessDetailResponse detail(Integer processId, Integer taskId);

    /**
     * Get process list
     *
     * @param request workflow process request
     * @return process response list
     */
    PageResult<ProcessResponse> listProcess(ProcessRequest request);

    /**
     * Get task list
     *
     * @param request workflow task query request
     * @return task response list
     */
    PageResult<TaskResponse> listTask(TaskRequest request);
}
