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

package org.apache.inlong.manager.service.mocks;

import org.apache.inlong.manager.common.consts.InlongConstants;
import org.apache.inlong.manager.common.enums.GroupOperateType;
import org.apache.inlong.manager.common.enums.TaskEvent;
import org.apache.inlong.manager.pojo.workflow.form.process.GroupResourceProcessForm;
import org.apache.inlong.manager.workflow.WorkflowContext;
import org.apache.inlong.manager.workflow.event.ListenerResult;
import org.apache.inlong.manager.workflow.event.task.SourceOperateListener;

/**
 * Test class for listen restart source event.
 */
public class MockRestartSourceListener implements SourceOperateListener {

    @Override
    public TaskEvent event() {
        return TaskEvent.COMPLETE;
    }

    @Override
    public boolean accept(WorkflowContext context) {
        if (!isGroupProcessForm(context)) {
            return false;
        }
        GroupResourceProcessForm processForm = (GroupResourceProcessForm) context.getProcessForm();
        return InlongConstants.STANDARD_MODE.equals(processForm.getGroupInfo().getLightweight())
                && processForm.getGroupOperateType() == GroupOperateType.RESTART;
    }

    @Override
    public ListenerResult listen(WorkflowContext context) {
        return ListenerResult.success("Mock restart source success");
    }

}
