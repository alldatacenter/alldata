/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.oozie.ambari.view.workflowmanager;

import org.apache.ambari.view.ViewContext;
import org.apache.oozie.ambari.view.HDFSFileUtils;
import org.apache.oozie.ambari.view.JobType;
import org.apache.oozie.ambari.view.WorkflowFilesService;
import org.apache.oozie.ambari.view.workflowmanager.model.Workflow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;

public class WorkflowManagerService {
  private final static Logger LOGGER = LoggerFactory
    .getLogger(WorkflowManagerService.class);
  private final WorkflowsRepo workflowsRepository;
  private final WorkflowFilesService workflowFilesService;

  public WorkflowManagerService(ViewContext viewContext) {
    workflowsRepository = new WorkflowsRepo(viewContext.getDataStore());

    workflowFilesService = new WorkflowFilesService(new HDFSFileUtils(viewContext));
  }

  public void saveWorkflow(String projectId, String path, JobType jobType,
                           String descripton, String userName, String name) {
    LOGGER.debug("save workflow called");
    if (projectId != null) {
      Workflow workflowById = workflowsRepository.findById(projectId);
      if (workflowById == null) {
        throw new RuntimeException("could not find project with id :"
          + projectId);
      }
      setWorkflowAttributes(jobType, userName, name, workflowById);
      workflowsRepository.update(workflowById);

    } else {
      String workflowFileName = workflowFilesService.getWorkflowFileName(path, jobType);
      Workflow workflowByPath = workflowsRepository.getWorkflowByPath(workflowFileName,userName);
      if (workflowByPath != null) {
        setWorkflowAttributes(jobType, userName, name, workflowByPath);
        workflowsRepository.update(workflowByPath);
      } else {
        Workflow wf = new Workflow();
        wf.setId(workflowsRepository.generateId());
        setWorkflowAttributes(jobType, userName, name, wf);
        wf.setWorkflowDefinitionPath(workflowFileName);
        workflowsRepository.create(wf);
      }
    }
  }

  private void setWorkflowAttributes(JobType jobType, String userName,
                                     String name, Workflow wf) {
    wf.setOwner(userName);
    wf.setName(name);
    wf.setType(jobType.name());
  }

  public Collection<Workflow> getAllWorkflows(String username) {
    return workflowsRepository.getWorkflows(username);
  }

  public void deleteWorkflow(String projectId, Boolean deleteDefinition) {
    Workflow workflow = workflowsRepository.findById(projectId);
    if (deleteDefinition) {
      workflowFilesService.deleteWorkflowFile(workflow.getWorkflowDefinitionPath());
    }
    workflowsRepository.delete(workflow);
  }
}