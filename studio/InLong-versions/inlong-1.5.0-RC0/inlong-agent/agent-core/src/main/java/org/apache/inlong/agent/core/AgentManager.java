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

package org.apache.inlong.agent.core;

import org.apache.inlong.agent.common.AbstractDaemon;
import org.apache.inlong.agent.conf.AgentConfiguration;
import org.apache.inlong.agent.conf.JobProfile;
import org.apache.inlong.agent.conf.ProfileFetcher;
import org.apache.inlong.agent.conf.TriggerProfile;
import org.apache.inlong.agent.constant.AgentConstants;
import org.apache.inlong.agent.core.conf.ConfigJetty;
import org.apache.inlong.agent.core.job.JobManager;
import org.apache.inlong.agent.core.task.TaskManager;
import org.apache.inlong.agent.core.task.TaskPositionManager;
import org.apache.inlong.agent.core.trigger.TriggerManager;
import org.apache.inlong.agent.db.CommandDb;
import org.apache.inlong.agent.db.Db;
import org.apache.inlong.agent.db.JobProfileDb;
import org.apache.inlong.agent.db.LocalProfile;
import org.apache.inlong.agent.db.TriggerProfileDb;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.lang.reflect.Constructor;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.apache.inlong.agent.constant.AgentConstants.AGENT_CONF_PARENT;
import static org.apache.inlong.agent.constant.AgentConstants.DEFAULT_AGENT_CONF_PARENT;
import static org.apache.inlong.agent.constant.JobConstants.JOB_TRIGGER;

/**
 * Agent Manager, the bridge for job manager, task manager, db e.t.c it manages agent level operations and communicates
 * with outside system.
 */
public class AgentManager extends AbstractDaemon {

    private static final Logger LOGGER = LoggerFactory.getLogger(AgentManager.class);
    private final JobManager jobManager;
    private final TaskManager taskManager;
    private final TriggerManager triggerManager;
    private final TaskPositionManager taskPositionManager;
    private final HeartbeatManager heartbeatManager;
    private final ProfileFetcher fetcher;
    private final AgentConfiguration conf;
    private final ExecutorService agentConfMonitor;
    private final Db db;
    private final LocalProfile localProfile;
    private final CommandDb commandDb;
    private final JobProfileDb jobProfileDb;
    // jetty for config operations via http.
    private ConfigJetty configJetty;

    public AgentManager() {
        conf = AgentConfiguration.getAgentConf();
        agentConfMonitor = Executors.newSingleThreadExecutor();
        this.db = initDb();
        commandDb = new CommandDb(db);
        jobProfileDb = new JobProfileDb(db);
        String parentConfPath = conf.get(AGENT_CONF_PARENT, DEFAULT_AGENT_CONF_PARENT);
        localProfile = new LocalProfile(parentConfPath);
        triggerManager = new TriggerManager(this, new TriggerProfileDb(db));
        jobManager = new JobManager(this, jobProfileDb);
        taskManager = new TaskManager(this);
        fetcher = initFetcher(this);
        heartbeatManager = HeartbeatManager.getInstance(this);
        taskPositionManager = TaskPositionManager.getInstance(this);
        // need to be an option.
        if (conf.getBoolean(
                AgentConstants.AGENT_ENABLE_HTTP, AgentConstants.DEFAULT_AGENT_ENABLE_HTTP)) {
            this.configJetty = new ConfigJetty(jobManager, triggerManager);
        }
    }

    /**
     * init fetch by class name
     */
    private ProfileFetcher initFetcher(AgentManager agentManager) {
        try {
            Constructor<?> constructor =
                    Class.forName(conf.get(AgentConstants.AGENT_FETCHER_CLASSNAME))
                            .getDeclaredConstructor(AgentManager.class);
            constructor.setAccessible(true);
            return (ProfileFetcher) constructor.newInstance(agentManager);
        } catch (Exception ex) {
            LOGGER.warn("cannot find fetcher: ", ex);
        }
        return null;
    }

    /**
     * init db by class name
     *
     * @return db
     */
    private Db initDb() {
        try {
            // db is a required component, so if not init correctly,
            // throw exception and stop running.
            return (Db) Class.forName(conf.get(
                    AgentConstants.AGENT_DB_CLASSNAME, AgentConstants.DEFAULT_AGENT_DB_CLASSNAME))
                    .newInstance();
        } catch (Exception ex) {
            throw new UnsupportedClassVersionError(ex.getMessage());
        }
    }

    private Runnable startHotConfReplace() {
        return new Runnable() {

            private long lastModifiedTime = 0L;

            @Override
            public void run() {
                while (true) {
                    try {
                        Thread.sleep(10 * 1000); // 10s check
                        File file = new File(
                                conf.getConfigLocation(AgentConfiguration.DEFAULT_CONFIG_FILE).getFile());
                        if (!file.exists()) {
                            continue;
                        }
                        if (file.lastModified() > lastModifiedTime) {
                            conf.reloadFromLocalPropertiesFile();
                            lastModifiedTime = file.lastModified();
                        }
                    } catch (InterruptedException e) {
                        LOGGER.error("Interrupted when flush agent conf.", e);
                    }
                }
            }
        };
    }

    public JobManager getJobManager() {
        return jobManager;
    }

    public Db getDb() {
        return db;
    }

    public JobProfileDb getJobProfileDb() {
        return jobProfileDb;
    }

    public ProfileFetcher getFetcher() {
        return fetcher;
    }

    public CommandDb getCommandDb() {
        return commandDb;
    }

    public TriggerManager getTriggerManager() {
        return triggerManager;
    }

    public TaskPositionManager getTaskPositionManager() {
        return taskPositionManager;
    }

    public TaskManager getTaskManager() {
        return taskManager;
    }

    public HeartbeatManager getHeartbeatManager() {
        return heartbeatManager;
    }

    @Override
    public void join() {
        super.join();
        jobManager.join();
        taskManager.join();
    }

    @Override
    public void start() throws Exception {
        LOGGER.info("starting agent manager");
        agentConfMonitor.submit(startHotConfReplace());
        jobManager.start();
        triggerManager.start();
        taskManager.start();
        heartbeatManager.start();
        taskPositionManager.start();
        // read job profiles from local
        List<JobProfile> profileList = localProfile.readFromLocal();
        for (JobProfile profile : profileList) {
            if (profile.hasKey(JOB_TRIGGER)) {
                TriggerProfile triggerProfile = TriggerProfile.parseJobProfile(profile);
                // there is no need to store this profile in triggerDB, because
                // this profile comes from local file.
                triggerManager.restoreTrigger(triggerProfile);
            } else {
                // job db store instance info, so it's suitable to use submitJobProfile
                // to store instance into job db.
                jobManager.submitFileJobProfile(profile);
            }
        }
        if (fetcher != null) {
            fetcher.start();
        }
    }

    /**
     * It should guarantee thread-safe, and can be invoked many times.
     *
     * @throws Exception exceptions
     */
    @Override
    public void stop() throws Exception {
        if (configJetty != null) {
            configJetty.close();
        }
        if (fetcher != null) {
            fetcher.stop();
        }
        // TODO: change job state which is in running state.
        LOGGER.info("stopping agent manager");
        // close in order: trigger -> job -> task
        triggerManager.stop();
        jobManager.stop();
        taskManager.stop();
        heartbeatManager.stop();
        taskPositionManager.stop();
        agentConfMonitor.shutdown();
        this.db.close();
    }
}
