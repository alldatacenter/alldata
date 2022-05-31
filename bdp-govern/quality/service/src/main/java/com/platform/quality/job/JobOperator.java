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

package com.platform.quality.job;

import com.platform.quality.job.entity.AbstractJob;
import com.platform.quality.job.entity.JobHealth;
import com.platform.quality.job.entity.JobState;
import com.platform.quality.measure.entity.GriffinMeasure;
import org.quartz.SchedulerException;

public interface JobOperator {
    AbstractJob add(AbstractJob job, GriffinMeasure measure)
        throws Exception;

    void start(AbstractJob job) throws Exception;

    void stop(AbstractJob job) throws SchedulerException;

    void delete(AbstractJob job) throws SchedulerException;

    JobHealth getHealth(JobHealth jobHealth, AbstractJob job)
        throws SchedulerException;

    JobState getState(AbstractJob job, String action)
        throws SchedulerException;
}
