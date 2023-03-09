/*
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing,
software distributed under the License is distributed on an
"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
KIND, either express or implied.  See the License for the
specific language governing permissions and limitations
under the License.
*/

package org.apache.griffin.core.job;

import java.util.List;

import org.apache.griffin.core.job.entity.AbstractJob;
import org.apache.griffin.core.job.entity.JobHealth;
import org.apache.griffin.core.job.entity.JobInstanceBean;
import org.quartz.SchedulerException;

public interface JobService {

    List<AbstractJob> getAliveJobs(String type);

    AbstractJob addJob(AbstractJob js) throws Exception;

    AbstractJob getJobConfig(Long jobId);

    AbstractJob onAction(Long jobId, String action) throws Exception;

    void deleteJob(Long jobId) throws SchedulerException;

    void deleteJob(String jobName) throws SchedulerException;

    List<JobInstanceBean> findInstancesOfJob(Long jobId, int page, int size);

    List<JobInstanceBean> findInstancesByTriggerKey(String triggerKey);

    JobHealth getHealthInfo();

    String getJobHdfsSinksPath(String jobName, long timestamp);

    JobInstanceBean findInstance(Long id);

    String triggerJobById(Long id) throws SchedulerException;
}
