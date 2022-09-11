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

import org.apache.inlong.manager.workflow.WorkflowContext;
import org.apache.inlong.manager.common.pojo.workflow.form.ProcessForm;

/**
 * WorkflowProcess definition service
 */
public interface ProcessService {

    /**
     * Initiation process
     *
     * @param name WorkflowProcess name
     * @param applicant Applicant
     * @param form Form information
     * @return Workflow context
     */
    WorkflowContext start(String name, String applicant, ProcessForm form);

    /**
     * Cancellation process application
     *
     * @param processId WorkflowProcess instance ID
     * @param operator Operator
     * @param remark Remarks information
     * @return Workflow context
     */
    WorkflowContext cancel(Integer processId, String operator, String remark);

}
