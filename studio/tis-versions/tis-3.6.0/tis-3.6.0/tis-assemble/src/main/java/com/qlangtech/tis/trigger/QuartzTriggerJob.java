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

import com.qlangtech.tis.trigger.impl.NullTriggerContext;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

/**
 * @date 2012-6-25
 */
public final class QuartzTriggerJob implements Job {

    private static final Logger log = LoggerFactory.getLogger(QuartzTriggerJob.class);

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {

        JobDataMap data = context.getJobDetail().getJobDataMap();

        JobSchedule schedule = (JobSchedule) data.get(TriggerJobManage.JOB_SCHEDULE);
        TriggerJobManage triggerJobServer = (TriggerJobManage) data
                .get(TriggerJobManage.JOB_TRIGGER_SERVER);
        Objects.requireNonNull(schedule, "schedule can not be null");

        log.info("job was fire with trigger:" + schedule.getIndexName() + ",crontab:"
                + schedule.getCrobexp());
        // conn.trigger(//context.getTrigger(),
        // schedule.getJobid());

        // 执行全量任务开始
        try {
            triggerJobServer.triggerFullDump(schedule.getIndexName(), ExecType.FULLBUILD, new NullTriggerContext());
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new JobExecutionException(e);
        }

    }
}
