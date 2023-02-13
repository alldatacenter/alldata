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

/**
 *
 */
package com.qlangtech.tis.trigger;

import java.io.Serializable;
import java.util.Date;

/**
 * @date 2012-7-30
 */
public class JobDesc implements Serializable {

	private static final long serialVersionUID = 1L;

	private final Long jobid;

	private RTriggerKey triggerKey;

	private Date previousFireTime;

	private String crontabExpression;

	private String serverIp;

	public String getServerIp() {
		return serverIp;
	}

	public void setServerIp(String serverIp) {
		this.serverIp = serverIp;
	}

	public String getCrontabExpression() {
		return crontabExpression;
	}

	public void setCrontabExpression(String crontabExpression) {
		this.crontabExpression = crontabExpression;
	}

	public Date getPreviousFireTime() {
		return previousFireTime;
	}

	public void setPreviousFireTime(Date previousFireTime) {
		this.previousFireTime = previousFireTime;
	}

	public JobDesc(Long jobid) {
		super();
		this.jobid = jobid;
	}

	public Long getJobid() {
		return jobid;
	}

	public RTriggerKey getTriggerKey() {
		return triggerKey;
	}

	public void setTriggerKey(RTriggerKey triggerKey) {
		this.triggerKey = triggerKey;
	}

}
