/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.inlong.sort.standalone;

import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.flume.Channel;
import org.apache.flume.SinkRunner;
import org.apache.flume.SourceRunner;
import org.apache.flume.lifecycle.LifecycleState;
import org.apache.flume.lifecycle.LifecycleSupervisor;
import org.apache.flume.lifecycle.LifecycleSupervisor.SupervisorPolicy;
import org.apache.flume.node.MaterializedConfiguration;
import org.apache.inlong.common.pojo.sortstandalone.SortTaskConfig;
import org.apache.inlong.sort.standalone.config.holder.SortClusterConfigHolder;
import org.apache.inlong.sort.standalone.utils.FlumeConfigGenerator;
import org.apache.inlong.sort.standalone.utils.InlongLoggerFactory;
import org.slf4j.Logger;

import com.google.common.eventbus.Subscribe;

/**
 * 
 * SortTask
 */
public class SortTask {

    public static final Logger LOG = InlongLoggerFactory.getLogger(SortTask.class);

    private final String taskName;
    private final LifecycleSupervisor supervisor;
    private MaterializedConfiguration materializedConfiguration;
    private final ReentrantLock lifecycleLock = new ReentrantLock();

    /**
     * Constructor
     * 
     * @param taskName
     */
    public SortTask(String taskName) {
        this.taskName = taskName;
        this.supervisor = new LifecycleSupervisor();
    }

    /**
     * get taskName
     * 
     * @return the taskName
     */
    public String getTaskName() {
        return taskName;
    }

    /**
     * start
     */
    public void start() {
        SortTaskConfig config = SortClusterConfigHolder.getTaskConfig(taskName);
        if (config == null) {
            return;
        }

        //
        Map<String, String> flumeConfiguration = FlumeConfigGenerator.generateFlumeConfiguration(config);
        LOG.info("Start sort task:{},config:{}", taskName, flumeConfiguration);
        PropertiesConfigurationProvider configurationProvider = new PropertiesConfigurationProvider(
                config.getName(), flumeConfiguration);
        this.handleConfigurationEvent(configurationProvider.getConfiguration());
    }

    /**
     * handleConfigurationEvent
     * 
     * @param conf
     */
    @Subscribe
    public void handleConfigurationEvent(MaterializedConfiguration conf) {
        try {
            lifecycleLock.lockInterruptibly();
            stopAllComponents();
            startAllComponents(conf);
        } catch (InterruptedException e) {
            LOG.info("Interrupted while trying to handle configuration event");
            return;
        } finally {
            // If interrupted while trying to lock, we don't own the lock, so must not attempt to unlock
            if (lifecycleLock.isHeldByCurrentThread()) {
                lifecycleLock.unlock();
            }
        }
    }

    /**
     * stop
     */
    public void stop() {
        lifecycleLock.lock();
        stopAllComponents();
        try {
            supervisor.stop();
        } finally {
            lifecycleLock.unlock();
        }
    }

    /**
     * stopAllComponents
     */
    private void stopAllComponents() {
        if (this.materializedConfiguration != null) {
            LOG.info("Shutting down configuration: {}", this.materializedConfiguration);
            for (Entry<String, SourceRunner> entry : this.materializedConfiguration.getSourceRunners().entrySet()) {
                try {
                    LOG.info("Stopping Source " + entry.getKey());
                    supervisor.unsupervise(entry.getValue());
                } catch (Exception e) {
                    LOG.error("Error while stopping {}", entry.getValue(), e);
                }
            }

            for (Entry<String, SinkRunner> entry : this.materializedConfiguration.getSinkRunners().entrySet()) {
                try {
                    LOG.info("Stopping Sink " + entry.getKey());
                    supervisor.unsupervise(entry.getValue());
                } catch (Exception e) {
                    LOG.error("Error while stopping {}", entry.getValue(), e);
                }
            }

            for (Entry<String, Channel> entry : this.materializedConfiguration.getChannels().entrySet()) {
                try {
                    LOG.info("Stopping Channel " + entry.getKey());
                    supervisor.unsupervise(entry.getValue());
                } catch (Exception e) {
                    LOG.error("Error while stopping {}", entry.getValue(), e);
                }
            }
        }
    }

    /**
     * startAllComponents
     * 
     * @param materializedConfiguration
     */
    private void startAllComponents(MaterializedConfiguration materializedConfiguration) {
        LOG.info("Starting new configuration:{}", materializedConfiguration);

        this.materializedConfiguration = materializedConfiguration;

        for (Entry<String, Channel> entry : materializedConfiguration.getChannels().entrySet()) {
            try {
                LOG.info("Starting Channel " + entry.getKey());
                supervisor.supervise(entry.getValue(),
                        new SupervisorPolicy.AlwaysRestartPolicy(), LifecycleState.START);
            } catch (Exception e) {
                LOG.error("Error while starting {}", entry.getValue(), e);
            }
        }

        /*
         * Wait for all channels to start.
         */
        for (Channel ch : materializedConfiguration.getChannels().values()) {
            while (ch.getLifecycleState() != LifecycleState.START
                    && !supervisor.isComponentInErrorState(ch)) {
                try {
                    LOG.info("Waiting for channel: " + ch.getName() + " to start. Sleeping for 500 ms");
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    LOG.error("Interrupted while waiting for channel to start.", e);
                }
            }
        }

        for (Entry<String, SinkRunner> entry : materializedConfiguration.getSinkRunners().entrySet()) {
            try {
                LOG.info("Starting Sink " + entry.getKey());
                supervisor.supervise(entry.getValue(),
                        new SupervisorPolicy.AlwaysRestartPolicy(), LifecycleState.START);
            } catch (Exception e) {
                LOG.error("Error while starting {}", entry.getValue(), e);
            }
        }

        for (Entry<String, SourceRunner> entry : materializedConfiguration.getSourceRunners().entrySet()) {
            try {
                LOG.info("Starting Source " + entry.getKey());
                supervisor.supervise(entry.getValue(),
                        new SupervisorPolicy.AlwaysRestartPolicy(), LifecycleState.START);
            } catch (Exception e) {
                LOG.error("Error while starting {}", entry.getValue(), e);
            }
        }
    }
}