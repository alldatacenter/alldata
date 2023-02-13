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

package org.apache.inlong.dataproxy.node;

import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.flume.Channel;
import org.apache.flume.Constants;
import org.apache.flume.Context;
import org.apache.flume.SinkRunner;
import org.apache.flume.SourceRunner;
import org.apache.flume.instrumentation.MonitorService;
import org.apache.flume.instrumentation.MonitoringType;
import org.apache.flume.lifecycle.LifecycleAware;
import org.apache.flume.lifecycle.LifecycleState;
import org.apache.flume.lifecycle.LifecycleSupervisor;
import org.apache.flume.lifecycle.LifecycleSupervisor.SupervisorPolicy;
import org.apache.flume.node.MaterializedConfiguration;
import org.apache.flume.node.PollingPropertiesFileConfigurationProvider;
import org.apache.flume.node.PollingZooKeeperConfigurationProvider;
import org.apache.flume.node.PropertiesFileConfigurationProvider;
import org.apache.flume.node.StaticZooKeeperConfigurationProvider;
import org.apache.flume.util.SSLUtil;
import org.apache.inlong.common.config.IDataProxyConfigHolder;
import org.apache.inlong.common.metric.MetricObserver;
import org.apache.inlong.dataproxy.config.RemoteConfigManager;
import org.apache.inlong.dataproxy.config.holder.CommonPropertiesHolder;
import org.apache.inlong.dataproxy.heartbeat.HeartbeatManager;
import org.apache.inlong.dataproxy.metrics.audit.AuditUtils;
import org.apache.inlong.sdk.commons.admin.AdminTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;

/**
 * DataProxy application
 */
public class Application {

    private static final Logger LOGGER = LoggerFactory.getLogger(Application.class);

    private static final String CONF_MONITOR_CLASS = "flume.monitoring.type";
    private static final String CONF_MONITOR_PREFIX = "flume.monitoring.";
    private final List<LifecycleAware> components;
    private final LifecycleSupervisor supervisor;
    private final ReentrantLock lifecycleLock = new ReentrantLock();
    private MaterializedConfiguration materializedConfiguration;
    private MonitorService monitorServer;
    private AdminTask adminTask;

    /**
     * Constructor
     */
    public Application() {
        this(new ArrayList<>(0));
    }

    /**
     * Constructor
     */
    public Application(List<LifecycleAware> components) {
        this.components = components;
        supervisor = new LifecycleSupervisor();
    }

    /**
     * Main entrance
     */
    public static void main(String[] args) {
        try {
            SSLUtil.initGlobalSSLParameters();
            Options options = new Options();

            Option option = new Option("n", "name", true, "the name of this agent");
            option.setRequired(true);
            options.addOption(option);

            option = new Option("f", "conf-file", true,
                    "specify a config file (required if -z missing)");
            option.setRequired(false);
            options.addOption(option);

            option = new Option(null, "no-reload-conf", false,
                    "do not reload config file if changed");
            options.addOption(option);

            // Options for Zookeeper
            option = new Option("z", "zkConnString", true,
                    "specify the ZooKeeper connection to use (required if -f missing)");
            option.setRequired(false);
            options.addOption(option);

            option = new Option("p", "zkBasePath", true,
                    "specify the base path in ZooKeeper for agent configs");
            option.setRequired(false);
            options.addOption(option);

            option = new Option("h", "help", false, "display help text");
            options.addOption(option);

            // load configuration data from manager
            option = new Option(null, "load-conf-from-manager", false,
                    "load configuration data from manager");
            option.setRequired(false);
            options.addOption(option);

            CommandLineParser parser = new GnuParser();
            CommandLine commandLine = parser.parse(options, args);

            if (commandLine.hasOption('h')) {
                new HelpFormatter().printHelp("flume-ng agent", options, true);
                return;
            }

            // start by manager configuration
            if (commandLine.hasOption("load-conf-from-manager")) {
                startByManagerConf(commandLine);
                return;
            }

            String agentName = commandLine.getOptionValue('n');
            boolean reload = !commandLine.hasOption("no-reload-conf");

            boolean isZkConfigured = commandLine.hasOption('z') || commandLine.hasOption("zkConnString");

            Application application;
            if (isZkConfigured) {
                // get options
                String zkConnectionStr = commandLine.getOptionValue('z');
                String baseZkPath = commandLine.getOptionValue('p');

                if (reload) {
                    EventBus eventBus = new EventBus(agentName + "-event-bus");
                    List<LifecycleAware> components = Lists.newArrayList();
                    PollingZooKeeperConfigurationProvider zProvider = new PollingZooKeeperConfigurationProvider(
                            agentName, zkConnectionStr, baseZkPath, eventBus);
                    components.add(zProvider);
                    application = new Application(components);
                    eventBus.register(application);
                } else {
                    StaticZooKeeperConfigurationProvider zProvider = new StaticZooKeeperConfigurationProvider(
                            agentName, zkConnectionStr, baseZkPath);
                    application = new Application();
                    application.handleConfigurationEvent(zProvider.getConfiguration());
                }
            } else {
                File configurationFile = new File(commandLine.getOptionValue('f'));
                // The following is to ensure that by default the agent will fail on startup
                // if the file does not exist.
                if (!configurationFile.exists()) {
                    // If command line invocation, then need to fail fast
                    if (System.getProperty(Constants.SYSPROP_CALLED_FROM_SERVICE) == null) {
                        String path = configurationFile.getPath();
                        try {
                            path = configurationFile.getCanonicalPath();
                        } catch (IOException ex) {
                            LOGGER.error("failed to read canonical path for file: " + path, ex);
                        }
                        throw new ParseException("configuration file does not exist: " + path);
                    }
                }

                List<LifecycleAware> components = Lists.newArrayList();
                if (reload) {
                    EventBus eventBus = new EventBus(agentName + "-event-bus");
                    PollingPropertiesFileConfigurationProvider configurationProvider;
                    configurationProvider = new PollingPropertiesFileConfigurationProvider(
                            agentName, configurationFile, eventBus, 30);
                    components.add(configurationProvider);
                    application = new Application(components);
                    eventBus.register(application);
                } else {
                    PropertiesFileConfigurationProvider configurationProvider;
                    configurationProvider = new PropertiesFileConfigurationProvider(agentName, configurationFile);
                    application = new Application();
                    application.handleConfigurationEvent(configurationProvider.getConfiguration());
                }
            }
            // metrics
            MetricObserver.init(CommonPropertiesHolder.get());
            // audit
            AuditUtils.initAudit();

            final Application appReference = application;
            Runtime.getRuntime().addShutdownHook(new Thread("data-proxy-shutdown-hook") {

                @Override
                public void run() {
                    AuditUtils.send();
                    appReference.stop();
                }
            });

            // start application
            application.start();
            Thread.sleep(5000);
        } catch (Exception e) {
            LOGGER.error("fatal error occurred while running data-proxy: ", e);
        }
    }

    /**
     * Start by Manager config
     */
    private static void startByManagerConf(CommandLine commandLine) {
        String proxyName = CommonPropertiesHolder.getString(RemoteConfigManager.KEY_PROXY_CLUSTER_NAME);
        ManagerPropsConfigProvider configurationProvider = new ManagerPropsConfigProvider(proxyName);
        Application application = new Application();
        application.handleConfigurationEvent(configurationProvider.getConfiguration());
        application.start();

        final Application appReference = application;
        Runtime.getRuntime().addShutdownHook(new Thread("data-proxy-shutdown-hook") {

            @Override
            public void run() {
                appReference.stop();
            }
        });
    }

    /**
     * Start all components
     */
    public void start() {
        lifecycleLock.lock();
        try {
            for (LifecycleAware component : components) {
                // update dataproxy config
                if (component instanceof IDataProxyConfigHolder) {
                    ((IDataProxyConfigHolder) component).setDataProxyConfig(
                            RemoteConfigManager.getInstance().getCurrentClusterConfigRef());
                }
                supervisor.supervise(component, new SupervisorPolicy.AlwaysRestartPolicy(), LifecycleState.START);
            }
            // start admin task
            this.adminTask = new AdminTask(new Context(CommonPropertiesHolder.get()));
            this.adminTask.start();
            HeartbeatManager heartbeatManager = new HeartbeatManager();
            heartbeatManager.start();
        } finally {
            lifecycleLock.unlock();
        }
    }

    /**
     * Handle the configuration event
     */
    @Subscribe
    public void handleConfigurationEvent(MaterializedConfiguration conf) {
        try {
            lifecycleLock.lockInterruptibly();
            stopAllComponents();
            startAllComponents(conf);
        } catch (InterruptedException e) {
            LOGGER.info("interrupted while handle the configuration event");
        } finally {
            // If interrupted while trying to lock, we don't own the lock, so must not attempt to unlock
            if (lifecycleLock.isHeldByCurrentThread()) {
                lifecycleLock.unlock();
            }
        }
    }

    /**
     * Stop the application
     */
    public void stop() {
        lifecycleLock.lock();
        stopAllComponents();
        try {
            supervisor.stop();
            if (monitorServer != null) {
                monitorServer.stop();
            }
            // stop admin task
            if (this.adminTask != null) {
                this.adminTask.stop();
            }
        } finally {
            lifecycleLock.unlock();
        }
    }

    /**
     * Stop all components
     */
    private void stopAllComponents() {
        LOGGER.info("shutting down configuration: {}", materializedConfiguration);
        if (materializedConfiguration != null) {
            for (Entry<String, SourceRunner> entry : materializedConfiguration.getSourceRunners().entrySet()) {
                try {
                    LOGGER.info("stopping source " + entry.getKey());
                    supervisor.unsupervise(entry.getValue());
                } catch (Exception e) {
                    LOGGER.error("error while stopping source " + entry.getValue(), e);
                }
            }

            for (Entry<String, SinkRunner> entry : materializedConfiguration.getSinkRunners().entrySet()) {
                try {
                    LOGGER.info("stopping sink " + entry.getKey());
                    supervisor.unsupervise(entry.getValue());
                } catch (Exception e) {
                    LOGGER.error("error while stopping sink " + entry.getValue(), e);
                }
            }

            for (Entry<String, Channel> entry : materializedConfiguration.getChannels().entrySet()) {
                try {
                    LOGGER.info("stopping channel " + entry.getKey());
                    supervisor.unsupervise(entry.getValue());
                } catch (Exception e) {
                    LOGGER.error("error while stopping channel " + entry.getValue(), e);
                }
            }
        }

        LOGGER.info("shutting down monitor server: {}", monitorServer);
        if (monitorServer != null) {
            monitorServer.stop();
        }
    }

    /**
     * Start all components
     */
    private void startAllComponents(MaterializedConfiguration materializedConfiguration) {
        LOGGER.info("Starting new configuration:{}", materializedConfiguration);

        this.materializedConfiguration = materializedConfiguration;
        for (Entry<String, Channel> entry : materializedConfiguration.getChannels().entrySet()) {
            try {
                LOGGER.info("starting channel " + entry.getKey());
                supervisor.supervise(entry.getValue(), new SupervisorPolicy.AlwaysRestartPolicy(),
                        LifecycleState.START);
            } catch (Exception e) {
                LOGGER.error("error while starting channel " + entry.getValue(), e);
            }
        }

        // Wait for all channels to start.
        for (Channel ch : materializedConfiguration.getChannels().values()) {
            while (ch.getLifecycleState() != LifecycleState.START
                    && !supervisor.isComponentInErrorState(ch)) {
                try {
                    LOGGER.info("sleeping for 500 ms to wait for channel: {} to start", ch.getName());
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    LOGGER.error("interrupted while waiting for channel to start: ", e);
                    Throwables.propagate(e);
                }
            }
        }

        for (Entry<String, SinkRunner> entry : materializedConfiguration.getSinkRunners().entrySet()) {
            try {
                LOGGER.info("starting sink " + entry.getKey());
                supervisor.supervise(entry.getValue(), new SupervisorPolicy.AlwaysRestartPolicy(),
                        LifecycleState.START);
            } catch (Exception e) {
                LOGGER.error("error while starting sink: " + entry.getValue(), e);
            }
        }

        for (Entry<String, SourceRunner> entry : materializedConfiguration.getSourceRunners().entrySet()) {
            try {
                LOGGER.info("starting source " + entry.getKey());
                supervisor.supervise(entry.getValue(), new SupervisorPolicy.AlwaysRestartPolicy(),
                        LifecycleState.START);
            } catch (Exception e) {
                LOGGER.error("error while starting source: " + entry.getValue(), e);
            }
        }

        this.loadMonitoring();
    }

    /**
     * Load monitoring
     */
    @SuppressWarnings("unchecked")
    private void loadMonitoring() {
        Properties systemProps = System.getProperties();
        Set<String> keys = systemProps.stringPropertyNames();
        try {
            if (keys.contains(CONF_MONITOR_CLASS)) {
                String monitorType = systemProps.getProperty(CONF_MONITOR_CLASS);
                Class<? extends MonitorService> klass;
                try {
                    // Is it a known type?
                    klass = MonitoringType.valueOf(monitorType.toUpperCase(Locale.ENGLISH)).getMonitorClass();
                } catch (Exception e) {
                    // Not a known type, use FQCN
                    klass = (Class<? extends MonitorService>) Class.forName(monitorType);
                }
                this.monitorServer = klass.getDeclaredConstructor().newInstance();
                Context context = new Context();
                for (String key : keys) {
                    if (key.startsWith(CONF_MONITOR_PREFIX)) {
                        context.put(key.substring(CONF_MONITOR_PREFIX.length()), systemProps.getProperty(key));
                    }
                }
                monitorServer.configure(context);
                monitorServer.start();
            }
        } catch (Exception e) {
            LOGGER.warn("starting monitoring error, the monitoring might not be available: ", e);
        }
    }

}
