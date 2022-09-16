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

import org.apache.inlong.agent.common.AbstractDaemon;
import org.apache.inlong.agent.conf.AgentConfiguration;
import org.apache.inlong.agent.conf.JobProfile;
import org.apache.inlong.agent.conf.TriggerProfile;
import org.apache.inlong.agent.constant.AgentConstants;
import org.apache.inlong.agent.constant.FileCollectType;
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
import static org.apache.inlong.agent.constant.JobConstants.TRIGGER_ONLY_ONE_JOB;

/**
 * manager for triggers.
 */
public class TriggerManager extends AbstractDaemon {

    public static final int JOB_CHECK_INTERVAL = 1;
    private static final Logger LOGGER = LoggerFactory.getLogger(TriggerManager.class);
    private final AgentManager manager;
    private final TriggerProfileDb triggerProfileDB;
    private final ConcurrentHashMap<String, Trigger> triggerMap;
    private final ConcurrentHashMap<String, ConcurrentHashMap<String, JobProfile>> triggerJobMap;
    private final AgentConfiguration conf;
    private final int triggerFetchInterval;
    private final int maxRunningNum;

    public TriggerManager(AgentManager manager, TriggerProfileDb triggerProfileDb) {
        this.conf = AgentConfiguration.getAgentConf();
        this.manager = manager;
        this.triggerProfileDB = triggerProfileDb;
        this.triggerMap = new ConcurrentHashMap<>();
        this.triggerJobMap = new ConcurrentHashMap<>();
        this.triggerFetchInterval = conf.getInt(AgentConstants.TRIGGER_FETCH_INTERVAL,
                AgentConstants.DEFAULT_TRIGGER_FETCH_INTERVAL);
        this.maxRunningNum = conf.getInt(TRIGGER_MAX_RUNNING_NUM, DEFAULT_TRIGGER_MAX_RUNNING_NUM);
    }

    /**
     * submit trigger profile.
     *
     * @param triggerProfile trigger profile
     */
    public boolean addTrigger(TriggerProfile triggerProfile) {
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
            LOGGER.error("exception caught", ex);
            ThreadUtils.threadThrowableHandler(Thread.currentThread(), ex);
            return false;
        }
        return true;
    }

    public Trigger getTrigger(String triggerId) {
        return triggerMap.get(triggerId);
    }

    /**
     * check trigger config, store it to db, and submit this trigger to be executed
     *
     * @return true if success
     */
    public boolean submitTrigger(TriggerProfile triggerProfile) {
        // make sure all required key exists.
        if (!triggerProfile.allRequiredKeyExist() || this.triggerMap.size() > maxRunningNum) {
            LOGGER.error("trigger {} not all required key exists or size {} exceed {}",
                    triggerProfile.toJsonStr(), this.triggerMap.size(), maxRunningNum);
            return false;
        }
        preprocessTrigger(triggerProfile);
        triggerProfileDB.storeTrigger(triggerProfile);
        addTrigger(triggerProfile);
        return true;
    }

    /**
     * Preprocessing before adding trigger, default value FULL
     *
     * FULL: All directory by regex
     * INCREMENT: Directory entry created
     */
    public void preprocessTrigger(TriggerProfile profile) {
        String syncType = profile.get(JobConstants.JOB_FILE_COLLECT_TYPE, FileCollectType.FULL);
        if (FileCollectType.INCREMENT.equals(syncType)) {
            return;
        }
        LOGGER.info("initialize submit full path. trigger {} ", profile.getTriggerId());
        manager.getJobManager().submitFileJobProfile(profile);
    }

    private Runnable jobFetchThread() {
        return () -> {
            while (isRunnable()) {
                try {
                    triggerMap.forEach((s, trigger) -> {
                        JobProfile profile = trigger.fetchJobProfile();
                        if (profile != null) {
                            TriggerProfile triggerProfile = trigger.getTriggerProfile();
                            if (triggerProfile.getBoolean(TRIGGER_ONLY_ONE_JOB, false)) {
                                deleteRelatedJobs(triggerProfile.getTriggerId());
                            }
                            manager.getJobManager().submitFileJobProfile(profile);
                            addToTriggerMap(profile.get(JOB_ID), profile);
                        }
                    });
                    TimeUnit.SECONDS.sleep(triggerFetchInterval);
                } catch (Throwable e) {
                    LOGGER.info("ignored Exception ", e);
                    ThreadUtils.threadThrowableHandler(Thread.currentThread(), e);

                }
            }

        };
    }

    /**
     * delete jobs generated by the trigger
     */
    private void deleteRelatedJobs(String triggerId) {
        LOGGER.info("start to delete related jobs in triggerId {}", triggerId);
        ConcurrentHashMap<String, JobProfile> jobProfiles =
                triggerJobMap.get(triggerId);
        if (jobProfiles != null) {
            LOGGER.info("trigger can only run one job, stop the others {}", jobProfiles.keySet());
            jobProfiles.keySet().forEach(this::deleteJob);
            triggerJobMap.remove(triggerId);
        }
    }

    private void deleteJob(String jobInstanceId) {
        manager.getJobManager().deleteJob(jobInstanceId);
    }

    private Runnable jobCheckThread() {
        return () -> {
            while (isRunnable()) {
                try {
                    triggerJobMap.forEach((s, jobProfiles) -> {
                        for (String jobId : jobProfiles.keySet()) {
                            Map<String, JobWrapper> jobs =
                                    manager.getJobManager().getJobs();
                            if (jobs.get(jobId) == null) {
                                triggerJobMap.remove(jobId);
                            }
                        }
                    });
                    TimeUnit.MINUTES.sleep(JOB_CHECK_INTERVAL);
                } catch (Throwable e) {
                    LOGGER.info("ignored Exception ", e);
                    ThreadUtils.threadThrowableHandler(Thread.currentThread(), e);
                }
            }

        };
    }

    /**
     * need to put profile in triggerJobMap
     */
    private void addToTriggerMap(String triggerId, JobProfile profile) {
        ConcurrentHashMap<String, JobProfile> tmpList =
                new ConcurrentHashMap<>();
        ConcurrentHashMap<String, JobProfile> jobWrappers =
                triggerJobMap.putIfAbsent(triggerId, tmpList);
        if (jobWrappers == null) {
            jobWrappers = tmpList;
        }
        jobWrappers.putIfAbsent(profile.getInstanceId(), profile);
    }

    /**
     * delete trigger by trigger profile.
     *
     * @param triggerId trigger profile.
     */
    public boolean deleteTrigger(String triggerId) {
        LOGGER.info("delete trigger {}", triggerId);
        Trigger trigger = triggerMap.remove(triggerId);
        if (trigger != null) {
            deleteRelatedJobs(triggerId);
            trigger.destroy();
            // delete trigger from db
            triggerProfileDB.deleteTrigger(triggerId);
            return true;
        }
        LOGGER.warn("cannot find trigger {}", triggerId);
        return false;
    }

    /**
     * init all triggers when daemon started.
     */
    private void initTriggers() throws Exception {
        // fetch all triggers from db
        List<TriggerProfile> profileList = triggerProfileDB.getTriggers();
        for (TriggerProfile profile : profileList) {
            addTrigger(profile);
        }
    }

    private void stopTriggers() {
        triggerMap.forEach((s, trigger) -> {
            trigger.destroy();
        });
    }

    @Override
    public void start() throws Exception {
        initTriggers();
        submitWorker(jobFetchThread());
        submitWorker(jobCheckThread());
    }

    @Override
    public void stop() {
        // stop all triggers
        stopTriggers();
    }

}
