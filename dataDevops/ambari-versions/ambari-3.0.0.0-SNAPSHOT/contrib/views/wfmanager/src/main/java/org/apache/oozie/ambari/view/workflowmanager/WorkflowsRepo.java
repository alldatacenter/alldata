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

import org.apache.ambari.view.DataStore;
import org.apache.ambari.view.PersistenceException;
import org.apache.oozie.ambari.view.repo.BaseRepo;
import org.apache.oozie.ambari.view.workflowmanager.model.Workflow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class WorkflowsRepo extends BaseRepo<Workflow> {
  private final static Logger LOGGER = LoggerFactory
          .getLogger(WorkflowsRepo.class);
  public WorkflowsRepo(DataStore dataStore) {
    super(Workflow.class, dataStore);

  }
  public Collection<Workflow> getWorkflows(String userName){
    try {
      Collection<Workflow> workflows = this.dataStore.findAll(Workflow.class,
        "owner='" + userName + "'");
      return  workflows;
    } catch (PersistenceException e) {
      throw new RuntimeException(e);
    }
  }

  public Workflow getWorkflowByPath(String path, String userName) {
    try {
      Collection<Workflow> workflows = this.dataStore.findAll(Workflow.class,
        "workflowDefinitionPath='" + path + "'");
      if (workflows == null || workflows.isEmpty()) {
        return null;
      } else {
        List<Workflow> myWorkflows = filterWorkflows(workflows, userName, true);
        if (myWorkflows.isEmpty()) {
          return null;
        } else if (myWorkflows.size() == 1) {
          return myWorkflows.get(0);
        } else {
          LOGGER.error("Duplicate workflows found having same path");
          throw new RuntimeException("Duplicate workflows. Remove one in Recent Workflows Manager");
        }
      }
    } catch (PersistenceException e) {
      throw new RuntimeException(e);
    }
  }

  private List<Workflow> filterWorkflows(Collection<Workflow> workflows,String userName,boolean matches ) {
    List<Workflow> filteredWorkflows = new ArrayList<>();
    for (Workflow wf : workflows) {
      if (matches && userName.equals(wf.getOwner())) {
        filteredWorkflows.add(wf);
      } else if (!matches && !userName.equals(wf.getOwner())) {
        filteredWorkflows.add(wf);
      }
    }
    return filteredWorkflows;
  }
}
