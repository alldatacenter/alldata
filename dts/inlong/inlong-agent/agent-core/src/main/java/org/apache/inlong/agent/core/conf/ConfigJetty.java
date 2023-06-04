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

package org.apache.inlong.agent.core.conf;

import static org.apache.inlong.agent.constant.JobConstants.JOB_SOURCE_TYPE;
import static org.apache.inlong.agent.constant.JobConstants.JOB_TRIGGER;

import java.io.Closeable;
import org.apache.inlong.agent.conf.AgentConfiguration;
import org.apache.inlong.agent.conf.JobProfile;
import org.apache.inlong.agent.conf.TriggerProfile;
import org.apache.inlong.agent.constant.AgentConstants;
import org.apache.inlong.agent.core.job.JobManager;
import org.apache.inlong.agent.core.trigger.TriggerManager;
import org.apache.inlong.common.enums.TaskTypeEnum;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * start http server and get job/agent config via http
 */
public class ConfigJetty implements Closeable {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigJetty.class);

    private final AgentConfiguration conf;
    private final Server server;
    private final JobManager jobManager;
    private final TriggerManager triggerManager;

    public ConfigJetty(JobManager jobManager, TriggerManager triggerManager) {
        this.conf = AgentConfiguration.getAgentConf();
        this.jobManager = jobManager;
        this.triggerManager = triggerManager;
        server = new Server();
        try {
            initJetty();
        } catch (Exception ex) {
            LOGGER.error("exception caught", ex);
        }
    }

    private void initJetty() throws Exception {
        // not using resource try to avoid AutoClosable's close() on the given stream
        ServerConnector connector = new ServerConnector(this.server);
        connector.setPort(conf.getInt(
                AgentConstants.AGENT_HTTP_PORT, AgentConstants.DEFAULT_AGENT_HTTP_PORT));
        server.setConnectors(new Connector[]{connector});

        ServletHandler servletHandler = new ServletHandler();
        ServletHolder holder = new ServletHolder(new ConfigServlet(this));
        servletHandler.addServletWithMapping(holder, "/config/*");
        server.setHandler(servletHandler);
        server.start();
    }

    /**
     * store job conf:
     * 1. if it's trigger job, store it in local db;
     * 2. for other job, submit it to jobManager to be executed
     *
     * @param jobProfile JobProfile
     */
    public void storeJobConf(JobProfile jobProfile) {
        // store job conf to bdb
        if (jobProfile != null) {
            // trigger job is a special kind of job
            if (jobProfile.hasKey(JOB_TRIGGER)) {
                triggerManager.submitTrigger(
                        TriggerProfile.parseJsonStr(jobProfile.toJsonStr()), true);
            } else {
                TaskTypeEnum taskType = TaskTypeEnum
                        .getTaskType(jobProfile.getInt(JOB_SOURCE_TYPE));
                switch (taskType) {
                    case FILE:
                        jobManager.submitFileJobProfile(jobProfile);
                        break;
                    case KAFKA:
                    case BINLOG:
                    case SQL:
                        jobManager.submitJobProfile(jobProfile, true, true);
                        break;
                    default:
                        LOGGER.error("source type not supported {}", taskType);
                }
            }
        }
    }

    public void storeAgentConf(String confJsonStr) {
        // store agent conf to local file
        AgentConfiguration conf = AgentConfiguration.getAgentConf();
        conf.loadJsonStrResource(confJsonStr);
        conf.flushToLocalPropertiesFile();
    }

    /**
     * delete job from conf
     *
     * @param jobProfile JobProfile
     */
    public void deleteJobConf(JobProfile jobProfile) {
        if (jobProfile != null) {
            if (jobProfile.hasKey(JOB_TRIGGER)) {
                triggerManager.deleteTrigger(TriggerProfile.parseJobProfile(jobProfile).getTriggerId(), false);
            } else {
                jobManager.deleteJob(jobProfile.getInstanceId(), false);
            }
        }
    }

    @Override
    public void close() {
        try {
            if (this.server != null) {
                this.server.stop();
            }
        } catch (Exception ex) {
            LOGGER.error("exception caught", ex);
        }
    }
}
