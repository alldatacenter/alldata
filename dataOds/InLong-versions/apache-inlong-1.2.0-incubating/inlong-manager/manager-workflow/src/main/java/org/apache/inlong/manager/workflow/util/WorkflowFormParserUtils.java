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

package org.apache.inlong.manager.workflow.util;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.inlong.manager.common.exceptions.FormParseException;
import org.apache.inlong.manager.common.pojo.workflow.form.ProcessForm;
import org.apache.inlong.manager.common.pojo.workflow.form.TaskForm;
import org.apache.inlong.manager.common.util.Preconditions;
import org.apache.inlong.manager.dao.entity.WorkflowTaskEntity;
import org.apache.inlong.manager.workflow.definition.UserTask;
import org.apache.inlong.manager.workflow.definition.WorkflowProcess;
import org.apache.inlong.manager.workflow.definition.WorkflowTask;

/**
 * WorkflowProcess form analysis tool
 */
@Slf4j
public class WorkflowFormParserUtils {

    /**
     * Parse the task form in JSON string format into a WorkflowTask instance
     */
    public static <T extends TaskForm> T parseTaskForm(ObjectMapper objectMapper,
            WorkflowTaskEntity workflowTaskEntity, WorkflowProcess process) throws FormParseException {
        Preconditions.checkNotNull(workflowTaskEntity, "workflowTaskEntity cannot be null");
        Preconditions.checkNotNull(process, "process cannot be null");

        if (StringUtils.isEmpty(workflowTaskEntity.getFormData())) {
            return null;
        }

        WorkflowTask task = process.getTaskByName(workflowTaskEntity.getName());
        Preconditions.checkNotNull(task, "user task not exist " + workflowTaskEntity.getName());
        Preconditions.checkTrue(task instanceof UserTask, "task should be userTask " + workflowTaskEntity.getName());

        UserTask userTask = (UserTask) task;
        try {
            JavaType javaType = objectMapper.constructType(userTask.getFormClass());
            return objectMapper.readValue(workflowTaskEntity.getFormData(), javaType);
        } catch (Exception e) {
            log.error("task parsed failed for form {}", workflowTaskEntity.getFormData(), e);
            throw new FormParseException("task form parse failed");
        }
    }

    /**
     * Parse the process form in JSON string format into a WorkflowProcess instance
     */
    public static <T extends ProcessForm> T parseProcessForm(ObjectMapper objectMapper, String form,
            WorkflowProcess process) throws FormParseException {
        Preconditions.checkNotNull(process, "process cannot be null");

        if (StringUtils.isEmpty(form)) {
            return null;
        }

        try {
            JavaType javaType = objectMapper.constructType(process.getFormClass());
            return objectMapper.readValue(form, javaType);
        } catch (Exception e) {
            log.error("process form parse failed, form is: {}", form, e);
            throw new FormParseException("process form parse failed");
        }
    }

}
