/*
 * Copyright 2021 WeBank
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.webank.wedatasphere.streamis.jobmanager.manager.scheduler;

import com.webank.wedatasphere.streamis.jobmanager.manager.scheduler.exception.StreamisScheduleException;
import org.apache.linkis.scheduler.queue.JobInfo;
import org.apache.linkis.scheduler.queue.SchedulerEvent;

import java.util.concurrent.CompletableFuture;

/**
 * Scheduler event
 */
public interface StreamisSchedulerEvent extends SchedulerEvent {

    /**
     * Set the complete future to listen the end of scheduler
     * @param completeFuture complete future
     * @param <T>
     */
    void setCompleteFuture(CompletableFuture<SchedulerEvent> completeFuture);

    /**
     * Tenancy in event
     * @param tenancy tenancy
     */
    void setTenancy(String tenancy);

    String getTenancy();

    /**
     * Prepare method
     * @param scheduleJob job info
     */
    void prepare(JobInfo scheduleJob) throws StreamisScheduleException;

    /**
     * Schedule method
     * @param scheduleJob job info
     */
    void schedule(JobInfo scheduleJob) throws StreamisScheduleException;

    /**
     * Handle error
     * @param scheduleJob job info
     * @param t error stack
     */
    void errorHandle(JobInfo scheduleJob, Throwable t);
    /**
     * Post handle
     * @param scheduleJob job info
     */
    void postHandle(JobInfo scheduleJob) throws StreamisScheduleException;
}
