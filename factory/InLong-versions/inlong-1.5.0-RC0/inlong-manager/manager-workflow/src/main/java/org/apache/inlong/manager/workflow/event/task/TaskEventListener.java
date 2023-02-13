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

package org.apache.inlong.manager.workflow.event.task;

import com.google.common.collect.Lists;
import org.apache.inlong.manager.common.enums.TaskEvent;
import org.apache.inlong.manager.workflow.WorkflowContext;
import org.apache.inlong.manager.workflow.event.EventListener;

import java.util.List;

/**
 * WorkflowTask event listener
 */
public interface TaskEventListener extends EventListener<TaskEvent> {

    /**
     * Empty event listeners.
     */
    List<TaskEventListener> EMPTY_LISTENERS = Lists.newArrayList();

    /**
     * Whether the current listener needs to operate the workflow.
     *
     * @param context workflow context
     * @return true if the current listener needs to operate the workflow, false if not
     */
    default boolean accept(WorkflowContext context) {
        return true;
    }

}
