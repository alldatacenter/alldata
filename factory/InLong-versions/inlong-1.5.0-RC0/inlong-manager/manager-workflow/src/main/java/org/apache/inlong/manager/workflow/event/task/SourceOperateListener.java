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

import org.apache.inlong.manager.common.enums.TaskEvent;
import org.apache.inlong.manager.pojo.workflow.form.process.GroupResourceProcessForm;
import org.apache.inlong.manager.workflow.WorkflowContext;
import org.apache.inlong.manager.workflow.event.ListenerResult;

/**
 * Listener of operate Source.
 */
public interface SourceOperateListener extends TaskEventListener {

    SourceOperateListener DEFAULT_SOURCE_OPERATE_LISTENER = new SourceOperateListener() {

        @Override
        public TaskEvent event() {
            return TaskEvent.COMPLETE;
        }

        @Override
        public ListenerResult listen(WorkflowContext context) {
            return ListenerResult.success();
        }

    };

    /**
     * Check whether the process form from the workflow context is {@link GroupResourceProcessForm}
     *
     * @param context workflow context
     * @return true if the process form is instanceof GroupResourceProcessForm
     */
    default boolean isGroupProcessForm(WorkflowContext context) {
        if (context == null) {
            return false;
        }
        return context.getProcessForm() instanceof GroupResourceProcessForm;
    }

}
