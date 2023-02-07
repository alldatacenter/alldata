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

package org.apache.inlong.agent.core.trigger;

import com.google.common.base.Preconditions;
import org.apache.commons.lang3.StringUtils;
import org.apache.inlong.agent.common.AbstractDaemon;
import org.apache.inlong.agent.conf.AgentConfiguration;
import org.apache.inlong.agent.conf.JobProfile;
import org.apache.inlong.agent.conf.TriggerProfile;
import org.apache.inlong.agent.constant.AgentConstants;
import org.apache.inlong.agent.constant.JobConstants;
import org.apache.inlong.agent.core.AgentManager;
import org.apache.inlong.agent.core.job.JobWrapper;
import org.apache.inlong.agent.db.TriggerProfileDb;
import org.apache.inlong.agent.plugin.Trigger;
import org.apache.inlong.agent.utils.ThreadUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import static org.apache.inlong.agent.constant.AgentConstants.DEFAULT_TRIGGER_MAX_RUNNING_NUM;
import static org.apache.inlong.agent.constant.AgentConstants.TRIGGER_MAX_RUNNING_NUM;
import static org.apache.inlong.agent.constant.JobConstants.JOB_ID;

/**
 * manager for triggers.
 */
public class TriggerManager extends AbstractDaemon {

    private static final Logger LOGGER = LoggerFactory.getLogger(TriggerManager.class);
    private final AgentManager manager;
    private final TriggerProfileDb triggerProfileDB;
    private final ConcurrentHashMap<String, Trigger> triggerMap;
    private final AgentConfiguration conf;
    private final int triggerFetchInterval;
    private final int maxRunningNum;

    public TriggerManager(AgentManager manager, TriggerProfileDb triggerProfileDb) {
        this.conf = AgentConfiguration.getAgentConf();
        this.manager = manager;
        this.triggerProfileDB = triggerProfileDb;
        this.triggerMap = new ConcurrentHashMap<>();
        this.triggerFetchInterval = conf.getInt(AgentConstants.TRIGGER_FETCH_INTERVAL,
                AgentConstants.DEFAULT_TRIGGER_FETCH_INTERVAL);
        this.maxRunningNum = conf.getInt(TRIGGER_MAX_RUNNING_NUM, DEFAULT_TRIGGER_MAX_RUNNING_NUM);
    }

    /**
     * Restore trigger task.
     *
     * @param triggerProfile trigger profile
     */
    public boolean restoreTrigger(TriggerProfile triggerProfile) {
        try {
            Class<?> triggerClass = Class.forName(triggerProfile.get(JobConstants.JOB_TRIGGER));
            Trigger trigger = (Trigger) triggerClass.newInstance();
            String triggerId = triggerProfile.get(JOB_ID);
            if (triggerMap.containsKey(triggerId)) {
                deleteTrigger(triggerId);
                LOGGER.warn("trigger {} is running, stop it", triggerId);
            }
            triggerMap.put(triggerId, trigger);
            trigger.init(triggerProfile);
            trigger.run();
        } catch (Throwable ex) {
            LOGGER.error("add trigger error: ", ex);
            ThreadUtils.threadThrowableHandler(Thread.currentThread(), ex);
            return false;
        }
        return true;
    }

    public Trigger getTrigger(String triggerId) {
        return triggerMap.get(triggerId);
    }

    /**
     * Submit trigger task.It guarantees eventual consistency.
     * 1.store db kv.
     * 2.start trigger thread.
     *
     * @param triggerProfile trigger profile
     */
    public void submitTrigger(TriggerProfile triggerProfile) {
        // make sure all required key exists.
        if (!triggerProfile.allRequiredKeyExist() || this.triggerMap.size() > maxRunningNum) {
            throw new IllegalArgumentException(
                    String.format(
                            "trigger %s not all required key exists or size %d exceed %d",
                            triggerProfile.toJsonStr(), this.triggerMap.size(), maxRunningNum));
        }
        // repeat check
        if (triggerProfileDB.getTriggers().stream()
                .anyMatch(profile -> profile.getTriggerId().equals(triggerProfile.getTriggerId()))) {
            return;
        }

        LOGGER.info("submit trigger {}", triggerProfile.toJsonStr());
        // This action must be done before saving in db, because the job.instance.id is needed for the next recovery
        manager.getJobManager().submitJobProfile(triggerProfile, true);
        triggerProfileDB.storeTrigger(triggerProfile);
        restoreTrigger(triggerProfile);
    }

    /**
     * Submit trigger task.It guarantees eventual consistency.
     * 1.stop trigger task and related collecting job.
     * 2.delete db kv.
     *
     * @param triggerId trigger profile.
     */
    public void deleteTrigger(String triggerId) {
        // repeat check
        if (!triggerProfileDB.getTriggers().stream()
                .anyMatch(profile -> profile.getTriggerId().equals(triggerId))) {
            return;
        }

        LOGGER.info("delete trigger {}", triggerId);
        Trigger trigger = triggerMap.remove(triggerId);
        if (trigger != null) {
            manager.getJobManager().deleteJob(trigger.getTriggerProfile().getInstanceId());
            trigger.destroy();
        }
        triggerProfileDB.deleteTrigger(triggerId);
    }

    private Runnable jobFetchThread() {
        return () -> {
            Thread.currentThread().setName("TriggerManager-jobFetch");
            // wait until jobManager initialize finish, because subtask add relay on memory 'jobs' rebuild
            // todo:Subsequent addition of the notification mechanism for subscribing to the jobmanager life cycle
            while (isRunnable()) {
                try {
                    triggerMap.forEach((s, trigger) -> {
                        Map<String, String> profile = trigger.fetchJobProfile();
                        if (profile != null) {
                            Map<String, JobWrapper> jobWrapperMap = manager.getJobManager().getJobs();
                            JobWrapper job = jobWrapperMap.get(trigger.getTriggerProfile().getInstanceId());
                            String subTaskFile = profile.getOrDefault(JobConstants.JOB_DIR_FILTER_PATTERNS, "");
                            Preconditions.checkArgument(StringUtils.isNotBlank(subTaskFile),
                                    String.format("Trigger %s fetched task file should not be null.", s));
                            // Add new watched file as subtask of trigger job.
                            // In order to solve the situation that the job will automatically recover after restarting,
                            // but the trigger will also automatically rematch all files to rebuild watchKey, it is
                            // necessary to filter the stated monitored file task.

                            boolean alreadyExistTask = job.exist(tasks -> tasks.stream()
                                    .filter(task -> !task.getJobConf().hasKey(JobConstants.JOB_TRIGGER))
                                    .filter(task -> subTaskFile.equals(
                                            task.getJobConf().get(JobConstants.JOB_DIR_FILTER_PATTERNS, "")))
                                    .findAny().isPresent());
                            if (!alreadyExistTask) {
                                LOGGER.info("Trigger job {} add new task file {}, total task {}",
                                        job.getJob().getName(), subTaskFile, job.getAllTasks().size());
                                JobWrapper jobWrapper = jobWrapperMap.get(trigger.getTriggerProfile().getInstanceId());
                                JobProfile taskProfile = JobProfile.parseJsonStr(
                                        jobWrapper.getJob().getJobConf().toJsonStr());
                                profile.forEach((k, v) -> taskProfile.set(k, v));
                                jobWrapper.submit(taskProfile);
                            }
                        }
                    });
                    TimeUnit.SECONDS.sleep(triggerFetchInterval);
                } catch (Throwable e) {
                    LOGGER.info("ignored exception: ", e);
                    ThreadUtils.threadThrowableHandler(Thread.currentThread(), e);
                }
            }
        };
    }

    /**
     * init all triggers when daemon started.
     */
    private void initTriggers() {
        // fetch all triggers from db
        List<TriggerProfile> profileList = triggerProfileDB.getTriggers();
        for (TriggerProfile profile : profileList) {
            restoreTrigger(profile);
        }
    }

    private void stopTriggers() {
        triggerMap.forEach((s, trigger) -> {
            trigger.destroy();
        });
    }

    @Override
    public void start() {
        initTriggers();
        submitWorker(jobFetchThread());
    }

    @Override
    public void stop() {
        // stop all triggers
        stopTriggers();
    }

}
