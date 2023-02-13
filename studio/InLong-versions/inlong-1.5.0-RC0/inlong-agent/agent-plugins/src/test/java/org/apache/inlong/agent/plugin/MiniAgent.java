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

package org.apache.inlong.agent.plugin;

import org.apache.inlong.agent.conf.AgentConfiguration;
import org.apache.inlong.agent.conf.JobProfile;
import org.apache.inlong.agent.conf.ProfileFetcher;
import org.apache.inlong.agent.conf.TriggerProfile;
import org.apache.inlong.agent.core.AgentManager;
import org.apache.inlong.agent.core.HeartbeatManager;
import org.apache.inlong.agent.core.task.TaskPositionManager;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.api.support.membermodification.MemberModifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;

import static org.apache.inlong.agent.constant.AgentConstants.AGENT_FETCH_CENTER_INTERVAL_SECONDS;

public class MiniAgent {

    private static final Logger LOGGER = LoggerFactory.getLogger(MiniAgent.class);
    private AgentManager manager;
    private final LinkedBlockingQueue<JobProfile> queueJobs = new LinkedBlockingQueue<>(100);
    private List<TriggerProfile> triggerProfileCache = new ArrayList<>();
    private List<JobProfile> jobProfileCache = new ArrayList<>();

    /**
     * Constructor of MiniAgent.
     */
    public MiniAgent() throws Exception {
        AgentConfiguration conf = AgentConfiguration.getAgentConf();
        conf.setInt(AGENT_FETCH_CENTER_INTERVAL_SECONDS, 1);
        manager = new AgentManager();
        init();
    }

    private void init() throws Exception {
        TaskPositionManager taskPositionManager = PowerMockito.mock(TaskPositionManager.class);
        HeartbeatManager heartbeatManager = PowerMockito.mock(HeartbeatManager.class);
        ProfileFetcher profileFetcher = PowerMockito.mock(ProfileFetcher.class);
        PowerMockito.doNothing().when(taskPositionManager, "start");
        PowerMockito.doNothing().when(taskPositionManager, "stop");
        PowerMockito.doNothing().when(heartbeatManager, "start");
        PowerMockito.doNothing().when(heartbeatManager, "stop");
        PowerMockito.doNothing().when(profileFetcher, "start");
        PowerMockito.doNothing().when(profileFetcher, "stop");
        MemberModifier.field(AgentManager.class, "taskPositionManager").set(manager, taskPositionManager);
        MemberModifier.field(AgentManager.class, "heartbeatManager").set(manager, heartbeatManager);
        MemberModifier.field(AgentManager.class, "fetcher").set(manager, profileFetcher);
    }

    public void start() throws Exception {
        manager.start();
    }

    public AgentManager getManager() {
        return manager;
    }

    public void stop() throws Exception {
        manager.stop();
    }

    public void restart() throws Exception {
        manager.stop();
        manager = new AgentManager();
        init();
        manager.start();
    }

    public void submitJob(JobProfile profile) {
        manager.getJobManager().submitFileJobProfile(profile);
        jobProfileCache.add(profile);
    }

    public void submitTrigger(TriggerProfile triggerProfile) {
        manager.getTriggerManager().submitTrigger(triggerProfile);
        triggerProfileCache.add(triggerProfile);
    }

    public void cleanupJobs() {
        jobProfileCache.forEach(jobProfile -> manager.getJobManager().deleteJob(jobProfile.getInstanceId()));
        jobProfileCache.clear();
    }

    public void cleanupTriggers() {
        triggerProfileCache
                .forEach(triggerProfile -> manager.getTriggerManager().deleteTrigger(triggerProfile.getTriggerId()));
        triggerProfileCache.clear();
    }
}
