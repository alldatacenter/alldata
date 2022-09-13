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

package org.apache.inlong.manager.workflow;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.inlong.manager.pojo.workflow.form.process.ProcessForm;
import org.apache.inlong.manager.pojo.workflow.form.task.TaskForm;
import org.apache.inlong.manager.dao.entity.WorkflowProcessEntity;
import org.apache.inlong.manager.dao.entity.WorkflowTaskEntity;
import org.apache.inlong.manager.workflow.definition.Element;
import org.apache.inlong.manager.workflow.definition.WorkflowProcess;
import org.apache.inlong.manager.workflow.definition.WorkflowTask;

import java.util.List;

/**
 * Workflow Context
 */
@Data
@Slf4j
public class WorkflowContext {

    private String operator;

    private WorkflowProcess process;

    private ProcessForm processForm;

    private Element currentElement;

    private WorkflowProcessEntity processEntity;

    private ActionContext actionContext;

    public String getOperator() {
        return operator;
    }

    public WorkflowContext setOperator(String operator) {
        this.operator = operator;
        return this;
    }

    public WorkflowProcess getProcess() {
        return process;
    }

    public WorkflowContext setProcess(WorkflowProcess process) {
        this.process = process;
        return this;
    }

    public WorkflowProcessEntity getProcessEntity() {
        return processEntity;
    }

    public WorkflowContext setProcessEntity(WorkflowProcessEntity processEntity) {
        this.processEntity = processEntity;
        return this;
    }

    public ProcessForm getProcessForm() {
        return processForm;
    }

    public WorkflowContext setProcessForm(ProcessForm processForm) {
        this.processForm = processForm;
        return this;
    }

    public Element getCurrentElement() {
        return currentElement;
    }

    public WorkflowContext setCurrentElement(Element currentElement) {
        this.currentElement = currentElement;
        return this;
    }

    public ActionContext getActionContext() {
        return actionContext;
    }

    public WorkflowContext setActionContext(ActionContext actionContext) {
        this.actionContext = actionContext;
        return this;
    }

    public static class ActionContext {

        private WorkflowAction action;
        private String operator;
        private String remark;
        private TaskForm form;
        private WorkflowTaskEntity taskEntity;
        private WorkflowTask task;
        private List<String> transferToUsers;

        public WorkflowAction getAction() {
            return action;
        }

        public ActionContext setAction(WorkflowAction action) {
            this.action = action;
            return this;
        }

        public String getOperator() {
            return operator;
        }

        public ActionContext setOperator(String operator) {
            this.operator = operator;
            return this;
        }

        public String getRemark() {
            return remark;
        }

        public ActionContext setRemark(String remark) {
            this.remark = remark;
            return this;
        }

        public TaskForm getForm() {
            return form;
        }

        public ActionContext setForm(TaskForm form) {
            this.form = form;
            return this;
        }

        public WorkflowTaskEntity getTaskEntity() {
            return taskEntity;
        }

        public ActionContext setTaskEntity(WorkflowTaskEntity taskEntity) {
            this.taskEntity = taskEntity;
            return this;
        }

        public WorkflowTask getTask() {
            return task;
        }

        public ActionContext setTask(WorkflowTask task) {
            this.task = task;
            return this;
        }

        public List<String> getTransferToUsers() {
            return transferToUsers;
        }

        public ActionContext setTransferToUsers(List<String> transferToUsers) {
            this.transferToUsers = transferToUsers;
            return this;
        }
    }

}
