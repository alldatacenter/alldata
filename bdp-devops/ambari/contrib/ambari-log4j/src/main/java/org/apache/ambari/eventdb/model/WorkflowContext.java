/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ambari.eventdb.model;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;


@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class WorkflowContext {
  
  private String workflowId;
  private String workflowName;
  private String workflowEntityName;
  
  private WorkflowDag workflowDag;
  
  private WorkflowContext parentWorkflowContext;
  
  public WorkflowContext() {
    /* Required by JAXB. */
  }
  
  /* Getters. */
  public String getWorkflowId() {
    return this.workflowId;
  }
  
  public String getWorkflowName() {
    return this.workflowName;
  }
  
  public String getWorkflowEntityName() {
    return this.workflowEntityName;
  }
  
  public WorkflowDag getWorkflowDag() {
    return this.workflowDag;
  }
  
  public WorkflowContext getParentWorkflowContext() {
    return this.parentWorkflowContext;
  }
  
  /* Setters. */
  public void setWorkflowId(String wfId) {
    this.workflowId = wfId;
  }
  
  public void setWorkflowName(String wfName) {
    this.workflowName = wfName;
  }
  
  public void setWorkflowEntityName(String wfEntityName) {
    this.workflowEntityName = wfEntityName;
  }
  
  public void setWorkflowDag(WorkflowDag wfDag) {
    this.workflowDag = wfDag;
  }
  
  public void setParentWorkflowContext(WorkflowContext pWfContext) {
    this.parentWorkflowContext = pWfContext;
  }
}
