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

import org.apache.inlong.manager.workflow.WorkflowAction;
import org.apache.inlong.manager.workflow.WorkflowContext;
import org.apache.inlong.manager.common.pojo.workflow.form.ProcessForm;
import org.apache.inlong.manager.common.pojo.workflow.form.TaskForm;

import java.util.List;

/**
 * Workflow context builder
 */
public interface WorkflowContextBuilder {

    /**
     * Build process context information when initiating a process
     *
     * @param name Process name
     * @param applicant Application
     * @param form Form
     */
    WorkflowContext buildContextForProcess(String name, String applicant, ProcessForm form);

    /**
     * WorkflowProcess ID construction context information
     *
     * @param processId WorkflowProcess ID
     * @return Context
     */
    WorkflowContext buildContextForProcess(Integer processId);

    /**
     * Build context information based on task ID
     */
    WorkflowContext buildContextForTask(Integer taskId, WorkflowAction action, String remark, String operator);

    /**
     * Build task context information
     */
    WorkflowContext buildContextForTask(Integer taskId, WorkflowAction action, TaskForm taskForm, String remark,
            String operator);

    /**
     * Build task context information
     */
    WorkflowContext buildContextForTask(Integer taskId, WorkflowAction action, List<String> transferToUsers,
            String remark, String operator);

    /**
     * Build context information based on task ID
     */
    WorkflowContext buildContextForTask(Integer taskId, WorkflowAction action);

}
