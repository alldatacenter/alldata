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
import org.apache.commons.lang.StringUtils;
import org.apache.linkis.common.conf.CommonVars;
import org.apache.linkis.scheduler.AbstractScheduler;
import org.apache.linkis.scheduler.SchedulerContext;
import org.apache.linkis.scheduler.executer.ErrorExecuteResponse;
import org.apache.linkis.scheduler.executer.ExecutorManager;
import org.apache.linkis.scheduler.queue.ConsumerManager;
import org.apache.linkis.scheduler.queue.Job;
import org.apache.linkis.scheduler.queue.JobInfo;
import org.apache.linkis.scheduler.queue.SchedulerEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.function.Function;

/**
 * Generic scheduler for streamis
 */
public class StreamisScheduler extends AbstractScheduler implements FutureScheduler {

    private static final Logger LOG = LoggerFactory.getLogger(StreamisScheduler.class);

    public static class Constraints{

        private static final CommonVars<String> TENANCY_PATTERN = CommonVars.apply("wds.streamis.job.scheduler.consumer.tenancies", "hadoop");

        private static final CommonVars<Integer> GROUP_INIT_CAPACITY = CommonVars.apply("wds.streamis.job.scheduler.group.min.capacity", 1000);

        private static final CommonVars<Integer> GROUP_MAX_CAPACITY = CommonVars.apply("wds.streamis.job.scheduler.group.max.capacity", 5000);

        private static final CommonVars<Integer> GROUP_MAX_RUNNING_JOBS = CommonVars.apply("wds.streamis.job.scheduler.group.max.running-jobs", 30);
    }
    /**
     * Scheduler context
     */
    private SchedulerContext schedulerContext;

    /**
     * Executor manager
     */
    private ExecutorManager executorManager;

    /**
     * Consumer manager
     */
    private ConsumerManager consumerManager;

    public StreamisScheduler(){

    }

    public StreamisScheduler(ExecutorManager executorManager, ConsumerManager consumerManager){
        this.executorManager = executorManager;
        this.consumerManager = consumerManager;
    }

    @Override
    public void init() {
        TenancyGroupFactory groupFactory = new TenancyGroupFactory();
        String tenancies = Constraints.TENANCY_PATTERN.getValue();
        groupFactory.setTenancies(StringUtils.isNotBlank(tenancies)? Arrays.asList(tenancies.split(",")) : Collections.emptyList());
        groupFactory.setDefaultInitCapacity(Constraints.GROUP_INIT_CAPACITY.getValue());
        groupFactory.setDefaultMaxCapacity(Constraints.GROUP_MAX_CAPACITY.getValue());
        groupFactory.setDefaultMaxRunningJobs(Constraints.GROUP_MAX_RUNNING_JOBS.getValue());
        this.schedulerContext = new StreamisSchedulerContext(groupFactory, consumerManager, executorManager);
    }

    @Override
    public String getName() {
        return "Streamis-Tenancy-Scheduler";
    }

    @Override
    public SchedulerContext getSchedulerContext() {
        return schedulerContext;
    }

    @Override
    public void submit(SchedulerEvent event) {
        super.submit(event);
    }

    @Override
    public <T> Future<T> submit(SchedulerEvent event, Function<SchedulerEvent, T> resultMapping) throws StreamisScheduleException {
        // Empty future
        CompletableFuture<SchedulerEvent> completableFuture = new CompletableFuture<>();
        if (event instanceof StreamisSchedulerEvent){
            StreamisSchedulerEvent schedulerEvent = (StreamisSchedulerEvent)event;
            // Set the completed future
            schedulerEvent.setCompleteFuture(completableFuture);
            if (event instanceof Job) {
                // Invoke the prepare method
                JobInfo jobInfo = ((Job) event).getJobInfo();
                try {
                    schedulerEvent.prepare(jobInfo);
                } catch (Exception e) {
                    ((Job) event).transitionCompleted(new ErrorExecuteResponse(e.getMessage(), e));
                }
            }
        } else{
            LOG.warn("Scheduler event type {} is not support future return", event.getClass().getCanonicalName());
            completableFuture.complete(event);
        }
        submit(event);
        return completableFuture.thenApply(resultMapping);
    }
}
