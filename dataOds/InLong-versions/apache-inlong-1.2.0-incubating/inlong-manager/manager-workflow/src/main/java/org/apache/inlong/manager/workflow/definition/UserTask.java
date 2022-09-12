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

import com.google.common.collect.ImmutableSet;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;
import org.apache.inlong.manager.common.exceptions.WorkflowException;
import org.apache.inlong.manager.common.pojo.workflow.form.TaskForm;
import org.apache.inlong.manager.common.util.Preconditions;
import org.apache.inlong.manager.workflow.WorkflowAction;
import org.apache.inlong.manager.workflow.WorkflowContext;

import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * User task
 */
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class UserTask extends WorkflowTask {

    private static final Set<WorkflowAction> SUPPORTED_ACTIONS = ImmutableSet.of(WorkflowAction.APPROVE,
            WorkflowAction.REJECT, WorkflowAction.TRANSFER, WorkflowAction.TERMINATE, WorkflowAction.CANCEL);

    private ApproverAssign approverAssign;
    private Class<? extends TaskForm> formClass;

    @Override
    public WorkflowAction defaultNextAction() {
        return WorkflowAction.APPROVE;
    }

    @Override
    protected Set<WorkflowAction> supportedActions() {
        return SUPPORTED_ACTIONS;
    }

    @Override
    public List<Element> getNextList(WorkflowAction action, WorkflowContext context) {
        Preconditions.checkTrue(supportedActions().contains(action),
                "not support workflow action " + action + ", it should in one of " + supportedActions());
        switch (action) {
            case APPROVE:
                return super.getNextList(action, context);
            case REJECT:
            case CANCEL:
            case TERMINATE:
                return Collections.singletonList(context.getProcess().getEndEvent());
            case TRANSFER:
                List<String> transferToUsers = context.getActionContext().getTransferToUsers();
                this.setApproverAssign(c -> transferToUsers);
                return Collections.singletonList(this);
            default:
                throw new WorkflowException("unknown workflow action: " + action);
        }
    }

    @SneakyThrows
    @Override
    public UserTask clone() {
        UserTask userTask = (UserTask) super.clone();
        userTask.setApproverAssign(this.approverAssign);
        userTask.setFormClass(this.formClass);
        return userTask;
    }
}
