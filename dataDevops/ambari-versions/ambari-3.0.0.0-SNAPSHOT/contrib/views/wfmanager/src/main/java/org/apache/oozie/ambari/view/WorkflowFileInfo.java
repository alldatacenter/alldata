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
package org.apache.oozie.ambari.view;

public class WorkflowFileInfo {
	private String workflowPath;
	private String draftPath;
	private Boolean draftExists;
	private Boolean isDraftCurrent=false;
	private Boolean workflowDefinitionExists=false;
	public Boolean getIsDraftCurrent() {
		return isDraftCurrent;
	}
	public void setIsDraftCurrent(Boolean isDraftCurrent) {
		this.isDraftCurrent = isDraftCurrent;
	}
	private Long workflowModificationTime;
	private Long draftModificationTime;
	public String getWorkflowPath() {
		return workflowPath;
	}
	public void setWorkflowPath(String workflowPath) {
		this.workflowPath = workflowPath;
	}
	public String getDraftPath() {
		return draftPath;
	}
	public void setDraftPath(String draftPath) {
		this.draftPath = draftPath;
	}
	public Boolean getDraftExists() {
		return draftExists;
	}
	public void setDraftExists(Boolean draftExists) {
		this.draftExists = draftExists;
	}
	public void setWorkflowModificationTime(Long modificationTime) {
		this.workflowModificationTime=modificationTime;
	}
	public void setDraftModificationTime(Long modificationTime) {
		this.draftModificationTime=modificationTime;		
	}
	public Long getWorkflowModificationTime() {
		return workflowModificationTime;
	}
	public Long getDraftModificationTime() {
		return draftModificationTime;
	}

	public void setWorkflowDefinitionExists(Boolean workflowDefinitionExists) {
		this.workflowDefinitionExists = workflowDefinitionExists;
	}
}
