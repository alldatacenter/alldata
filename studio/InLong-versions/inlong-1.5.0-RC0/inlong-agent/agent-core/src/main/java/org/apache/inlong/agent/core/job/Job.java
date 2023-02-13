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

package org.apache.inlong.agent.core.job;

import org.apache.inlong.agent.conf.JobProfile;
import org.apache.inlong.agent.constant.JobConstants;

/**
 * job meta definition, job will be split into several tasks.
 */
public class Job {

    protected final JobProfile jobConf;
    // job name
    private String name;
    // job description
    private String description;
    protected String jobInstanceId;

    public Job(JobProfile jobConf) {
        this.jobConf = jobConf;
        this.name = jobConf.get(JobConstants.JOB_NAME, JobConstants.DEFAULT_JOB_NAME);
        this.description = jobConf.get(
                JobConstants.JOB_DESCRIPTION, JobConstants.DEFAULT_JOB_DESCRIPTION);
        this.jobInstanceId = jobConf.get(JobConstants.JOB_INSTANCE_ID);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getJobInstanceId() {
        return jobInstanceId;
    }

    public void setJobInstanceId(String jobInstanceId) {
        this.jobInstanceId = jobInstanceId;
    }

    public JobProfile getJobConf() {
        return this.jobConf;
    }

}
