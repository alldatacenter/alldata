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

package org.apache.inlong.manager.service.core;

import org.apache.inlong.manager.pojo.common.PageResult;
import org.apache.inlong.manager.pojo.workflow.ApproverPageRequest;
import org.apache.inlong.manager.pojo.workflow.ApproverRequest;
import org.apache.inlong.manager.pojo.workflow.ApproverResponse;

import java.util.List;

/**
 * Workflow approver configuration service
 */
public interface WorkflowApproverService {

    /**
     * Save workflow approver
     *
     * @param request approver request
     * @param operator operator name
     */
    Integer save(ApproverRequest request, String operator);

    /**
     * Get workflow approver by ID
     *
     * @param id approver id
     * @return approver info
     */
    ApproverResponse get(Integer id);

    /**
     * Get process approver by the process name and task name.
     *
     * @param processName workflow process name
     * @param taskName workflow task name
     * @return approver list
     */
    List<String> getApprovers(String processName, String taskName);

    /**
     * List the workflow approvers according to the query request
     *
     * @param request page query request
     * @return approver list
     */
    PageResult<ApproverResponse> listByCondition(ApproverPageRequest request);

    /**
     * Update workflow approve.
     *
     * @param request approver request
     * @param operator operator name
     */
    Integer update(ApproverRequest request, String operator);

    /**
     * Delete workflow approver by ID
     *
     * @param id approver id
     * @param operator operator name
     */
    void delete(Integer id, String operator);

}
