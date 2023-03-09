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

package org.apache.griffin.core.job.entity;

import java.util.List;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

@Entity
@DiscriminatorValue("griffinBatchJob")
public class BatchJob extends AbstractJob {
    private static final long serialVersionUID = -1114269860236729008L;

    @Override
    public String getType() {
        return JobType.BATCH.getName();
    }

    public BatchJob() {
        super();
    }

    public BatchJob(Long measureId, String jobName, String name, String group,
                    boolean deleted) {
        super(measureId, jobName, name, group, deleted);
        this.metricName = jobName;
    }

    public BatchJob(Long jobId, Long measureId, String jobName, String qJobName,
                    String qGroupName, boolean deleted) {
        this(measureId, jobName, qJobName, qGroupName, deleted);
        setId(jobId);
    }

    public BatchJob(Long measureId, String jobName, String cronExpression,
                    String timeZone, List<JobDataSegment> segments,
                    boolean deleted) {
        super(measureId, jobName, cronExpression, timeZone, segments, deleted);
    }

}
