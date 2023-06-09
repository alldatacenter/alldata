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

package com.qlangtech.tis.trigger;

import java.io.Serializable;

/**
 * 任务执行计划
 * @date 2012-6-19
 */
public class JobSchedule implements Serializable {

	private static final long serialVersionUID = 1L;
	private final Long jobid;
	// 执行任务
	private final String crobexp;
	private final String indexName;

	/**
	 * 是否是暂停状态？
	 */
	private final boolean paused;

	public JobSchedule(String indexName, Long jobid, String crobexp// , boolean
                       // isStop
	) {
		super();
		this.jobid = jobid;
		this.indexName = indexName;
		this.crobexp = crobexp;
		this.paused = false;
	}

	public Long getJobid() {
		return jobid;
	}

	public boolean isPaused() {
		return paused;
	}

	public String getIndexName() {
		return indexName;
	}

	public String getCrobexp() {
		return crobexp;
	}

}
