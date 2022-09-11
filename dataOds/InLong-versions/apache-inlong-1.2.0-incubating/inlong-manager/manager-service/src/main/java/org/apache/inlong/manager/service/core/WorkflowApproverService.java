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

import org.apache.inlong.manager.common.pojo.workflow.WorkflowApprover;
import org.apache.inlong.manager.common.pojo.workflow.WorkflowApproverFilterContext;
import org.apache.inlong.manager.common.pojo.workflow.WorkflowApproverQuery;

import java.util.List;

/**
 * Workflow approver configuration service
 */
public interface WorkflowApproverService {

    /**
     * Get process approver
     *
     * @param processName WorkflowProcess name
     * @param taskName WorkflowTask name
     * @param context Context
     * @return Approver
     */
    List<String> getApprovers(String processName, String taskName, WorkflowApproverFilterContext context);

    /**
     * Obtain the approver configuration list according to the query conditions
     *
     * @param query Query conditions
     * @return Approver list
     */
    List<WorkflowApprover> list(WorkflowApproverQuery query);

    /**
     * Add approver configuration
     *
     * @param config Configuration details
     * @param operator Operator
     */
    void add(WorkflowApprover config, String operator);

    /**
     * Update approver configuration
     *
     * @param approver Approver info
     * @param operator Operator
     */
    void update(WorkflowApprover approver, String operator);

    /**
     * Delete approver configuration
     *
     * @param id Approver id
     * @param operator Operator
     */
    void delete(Integer id, String operator);

}
