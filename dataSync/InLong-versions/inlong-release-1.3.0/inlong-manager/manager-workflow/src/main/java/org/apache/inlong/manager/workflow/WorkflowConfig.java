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

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.apache.inlong.manager.dao.mapper.WorkflowEventLogEntityMapper;
import org.apache.inlong.manager.dao.mapper.WorkflowProcessEntityMapper;
import org.apache.inlong.manager.dao.mapper.WorkflowTaskEntityMapper;
import org.apache.inlong.manager.workflow.core.ProcessDefinitionRepository;
import org.apache.inlong.manager.workflow.core.WorkflowQueryService;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Workflow config
 */
public class WorkflowConfig {

    @JsonIgnore
    private WorkflowQueryService queryService;
    @JsonIgnore
    private WorkflowProcessEntityMapper processEntityMapper;
    @JsonIgnore
    private WorkflowTaskEntityMapper taskEntityMapper;
    @JsonIgnore
    private WorkflowEventLogEntityMapper eventLogMapper;
    @JsonIgnore
    private ProcessDefinitionRepository definitionRepository;
    @JsonIgnore
    private PlatformTransactionManager transactionManager;

    public WorkflowQueryService getQueryService() {
        return queryService;
    }

    public WorkflowConfig setQueryService(WorkflowQueryService queryService) {
        this.queryService = queryService;
        return this;
    }

    public WorkflowProcessEntityMapper getProcessEntityMapper() {
        return processEntityMapper;
    }

    public WorkflowConfig setProcessEntityMapper(WorkflowProcessEntityMapper processEntityMapper) {
        this.processEntityMapper = processEntityMapper;
        return this;
    }

    public WorkflowTaskEntityMapper getTaskEntityMapper() {
        return taskEntityMapper;
    }

    public WorkflowConfig setTaskEntityMapper(WorkflowTaskEntityMapper taskEntityMapper) {
        this.taskEntityMapper = taskEntityMapper;
        return this;
    }

    public WorkflowEventLogEntityMapper getEventLogMapper() {
        return eventLogMapper;
    }

    public WorkflowConfig setEventLogMapper(WorkflowEventLogEntityMapper eventLogMapper) {
        this.eventLogMapper = eventLogMapper;
        return this;
    }

    public ProcessDefinitionRepository getDefinitionRepository() {
        return definitionRepository;
    }

    public WorkflowConfig setDefinitionRepository(ProcessDefinitionRepository definitionRepository) {
        this.definitionRepository = definitionRepository;
        return this;
    }

    public PlatformTransactionManager getTransactionManager() {
        return transactionManager;
    }

    public WorkflowConfig setTransactionManager(PlatformTransactionManager transactionManager) {
        this.transactionManager = transactionManager;
        return this;
    }

}
