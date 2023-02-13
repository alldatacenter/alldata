/**
 *   Licensed to the Apache Software Foundation (ASF) under one
 *   or more contributor license agreements.  See the NOTICE file
 *   distributed with this work for additional information
 *   regarding copyright ownership.  The ASF licenses this file
 *   to you under the Apache License, Version 2.0 (the
 *   "License"); you may not use this file except in compliance
 *   with the License.  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package com.qlangtech.tis.trigger.biz.dal.pojo;

import java.io.Serializable;
import java.util.Date;

public class Task implements Serializable {
	private Long taskId;

	private Long jobId;

	private String triggerFrom;

	private String execState;

	private String domain;

	private Date gmtCreate;

	private Date gmtModified;

	private String runtime;
	private String fromIp;

	private Long phrase;

	private String errLogId;

	private String errMsg;



	public String getErrMsg() {
		return errMsg;
	}

	public void setErrMsg(String errMsg) {
		this.errMsg = errMsg;
	}

	public Long getPhrase() {
		return phrase;
	}

	public void setPhrase(Long phrase) {
		this.phrase = phrase;
	}


	public String getErrLogId() {
		return errLogId;
	}

	public void setErrLogId(String errLogId) {
		this.errLogId = errLogId == null ? null : errLogId.trim();
	}

	public String getFromIp() {
		return fromIp;
	}

	public void setFromIp(String fromIp) {
		this.fromIp = fromIp;
	}

	public String getRuntime() {
		return runtime;
	}

	public void setRuntime(String runtime) {
		this.runtime = runtime == null ? null : runtime.trim();
	}

	private static final long serialVersionUID = 1L;

	public Long getTaskId() {
		return taskId;
	}

	public void setTaskId(Long taskId) {
		this.taskId = taskId;
	}

	public Long getJobId() {
		return jobId;
	}

	public void setJobId(Long jobId) {
		this.jobId = jobId;
	}

	public String getTriggerFrom() {
		return triggerFrom;
	}

	public void setTriggerFrom(String triggerFrom) {
		this.triggerFrom = triggerFrom == null ? null : triggerFrom.trim();
	}

	public String getExecState() {
		return execState;
	}

	public void setExecState(String execState) {
		this.execState = execState == null ? null : execState.trim();
	}

	public String getDomain() {
		return domain;
	}

	public void setDomain(String domain) {
		this.domain = domain == null ? null : domain.trim();
	}

	public Date getGmtCreate() {
		return gmtCreate;
	}

	public void setGmtCreate(Date gmtCreate) {
		this.gmtCreate = gmtCreate;
	}

	public Date getGmtModified() {
		return gmtModified;
	}

	public void setGmtModified(Date gmtModified) {
		this.gmtModified = gmtModified;
	}
}
