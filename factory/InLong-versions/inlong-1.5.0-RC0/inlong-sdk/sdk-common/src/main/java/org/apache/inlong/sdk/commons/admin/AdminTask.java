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

package org.apache.inlong.sdk.commons.admin;

import java.io.IOException;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.flume.Channel;
import org.apache.flume.Context;
import org.apache.flume.SinkRunner;
import org.apache.flume.SourceRunner;
import org.apache.flume.lifecycle.LifecycleState;
import org.apache.flume.lifecycle.LifecycleSupervisor;
import org.apache.flume.lifecycle.LifecycleSupervisor.SupervisorPolicy;
import org.apache.flume.node.MaterializedConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.eventbus.Subscribe;

/**
 * 
 * AdminTask
 */
public class AdminTask {

    public static final Logger LOG = LoggerFactory.getLogger(AdminTask.class);
    public static final String KEY_HOST = "adminTask.host";
    public static final String KEY_PORT = "adminTask.port";
    public static final String KEY_HANDLER = "adminTask.handler";
    public static final String FLUME_ROOT = "admin";

    private Context context;
    private final LifecycleSupervisor supervisor;
    private MaterializedConfiguration materializedConfiguration;
    private final ReentrantLock lifecycleLock = new ReentrantLock();

    /**
     * Constructor
     * 
     * @param context
     */
    public AdminTask(Context context) {
        this.context = context;
        this.supervisor = new LifecycleSupervisor();
    }

    /**
     * start
     */
    public void start() {
        try {
            Map<String, String> flumeConfiguration = generateFlumeConfiguration();
            if (flumeConfiguration == null) {
                return;
            }
            LOG.info("Start admin task,flumeConf:{}", flumeConfiguration);
            PropertiesConfigurationProvider configurationProvider = new PropertiesConfigurationProvider(
                    FLUME_ROOT, flumeConfiguration);
            this.handleConfigurationEvent(configurationProvider.getConfiguration());
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        }
    }

    /**
     * generateFlumeConfiguration
     * 
     * @return             Map
     * @throws IOException
     */
    private Map<String, String> generateFlumeConfiguration() throws IOException {
        String host = context.getString(KEY_HOST);
        if (host == null) {
            LOG.error("Can not start admin task, host is null.");
            return null;
        }
        String port = context.getString(KEY_PORT);
        if (port == null) {
            LOG.error("Can not start admin task, port is null.");
            return null;
        }
        String handlerType = context.getString(KEY_HANDLER);
        if (handlerType == null) {
            LOG.error("Can not start admin task, handlerType is null.");
            return null;
        }
        String flumeString = String.format("admin.sources=r1\n"
                + "admin.sinks=k1\n"
                + "admin.channels=c1\n"
                + "admin.sources.r1.type=" + AdminHttpSource.class.getName() + "\n"
                + "admin.sources.r1.bind=%s\n"
                + "admin.sources.r1.port=%s\n"
                + "admin.sources.r1.channels=c1\n"
                + "admin.sources.r1.handler=%s\n"
                + "admin.sinks.k1.type=logger\n"
                + "admin.sinks.k1.channel=c1\n"
                + "admin.channels.c1.type=memory\n"
                + "admin.channels.c1.capacity=1000\n"
                + "admin.channels.c1.transactionCapacity=100", host,
                port,
                handlerType);
        Properties props = new Properties();
        props.load(new StringReader(flumeString));
        Map<String, String> flumeMap = new HashMap<>();
        props.forEach((key, value) -> {
            flumeMap.put(String.valueOf(key), String.valueOf(value));
        });
        // adminTask.handler.stopService.type=org.apache.inlong.dataproxy.admin.ProxyServiceAdminEventHandler
        // adminTask.handler.stopService.param1=xxx
        // adminTask.handler.stopService.param2=xxx
        // adminTask.handler.stopConsumer.type=org.apache.inlong.sort.standalone.admin.ConsumerServiceAdminEventHandler
        // adminTask.handler.stopConsumer.param1=xxx
        // adminTask.handler.stopConsumer.param2=xxx
        // adminTask.handler.ackAll.type=org.apache.inlong.sort.standalone.admin.ConsumerServiceAdminEventHandler
        // adminTask.handler.ackAll.param1=xxx
        // adminTask.handler.ackAll.param2=xxx
        Map<String, String> subHandlerConfig = context.getSubProperties(KEY_HANDLER + ".");
        subHandlerConfig.forEach((key, value) -> {
            flumeMap.put("admin.sources.r1.handler." + key, value);
        });
        return flumeMap;
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

    /**
     * main
     * 
     * @param args
     */
    public static void main(String[] args) {
        try {
            Context context = new Context();
            context.put(KEY_HOST, "127.0.0.1");
            context.put(KEY_PORT, "8080");
            context.put(KEY_HANDLER, "org.apache.inlong.dataproxy.admin.AdminJsonHandler");
            context.put(KEY_HANDLER + ".stopService.type",
                    "org.apache.inlong.dataproxy.admin.ProxyServiceAdminEventHandler");
            AdminTask task = new AdminTask(context);
            task.start();
            Thread.sleep(10000);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}