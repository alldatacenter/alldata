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

package org.apache.inlong.manager.workflow.definition;

import org.apache.inlong.manager.workflow.WorkflowContext;
import org.apache.inlong.manager.workflow.event.task.TaskEventListener;

import java.util.List;

/**
 * The factory interface of TaskEventListener.
 */
public interface TaskListenerFactory {

    /**
     * Get the task event listeners from the given workflow context and the specified task type.
     *
     * @param workflowContext the workflow context
     * @param taskType the task type
     * @return list of the task event listeners
     */
    List<? extends TaskEventListener> get(WorkflowContext workflowContext, ServiceTaskType taskType);

}
