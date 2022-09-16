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

package org.apache.inlong.audit.node;

import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;
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
import org.apache.flume.node.PropertiesFileConfigurationProvider;
import org.apache.flume.util.SSLUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * Application
 */
public class Application {

    private static final Logger logger = LoggerFactory
            .getLogger(Application.class);

    public static final String CONF_MONITOR_CLASS = "flume.monitoring.type";
    public static final String CONF_MONITOR_PREFIX = "flume.monitoring.";

    private final List<LifecycleAware> components;
    private final LifecycleSupervisor supervisor;
    private MaterializedConfiguration materializedConfiguration;
    private MonitorService monitorServer;
    private final ReentrantLock lifecycleLock = new ReentrantLock();

    public Application() {
        this(new ArrayList<LifecycleAware>(0));
    }

    public Application(List<LifecycleAware> components) {
        this.components = components;
        supervisor = new LifecycleSupervisor();
    }

    public void start() {
        lifecycleLock.lock();
        try {
            for (LifecycleAware component : components) {

                supervisor.supervise(component,
                        new SupervisorPolicy.AlwaysRestartPolicy(), LifecycleState.START);
            }
        } finally {
            lifecycleLock.unlock();
        }
    }

    @Subscribe
    public void handleConfigurationEvent(MaterializedConfiguration conf) {
        try {
            lifecycleLock.lockInterruptibly();
            stopAllComponents();
            startAllComponents(conf);
        } catch (InterruptedException e) {
            logger.info("Interrupted while trying to handle configuration event");
            return;
        } finally {
            // If interrupted while trying to lock, we don't own the lock, so must not attempt to unlock
            if (lifecycleLock.isHeldByCurrentThread()) {
                lifecycleLock.unlock();
            }
        }
    }

    public void stop() {
        lifecycleLock.lock();
        stopAllComponents();
        try {
            supervisor.stop();
            if (monitorServer != null) {
                monitorServer.stop();
            }
        } finally {
            lifecycleLock.unlock();
        }
    }

    private void stopAllComponents() {
        if (this.materializedConfiguration != null) {
            logger.info("Shutting down configuration: {}", this.materializedConfiguration);
            for (Entry<String, SourceRunner> entry : this.materializedConfiguration
                    .getSourceRunners().entrySet()) {
                try {
                    logger.info("Stopping Source " + entry.getKey());
                    supervisor.unsupervise(entry.getValue());
                } catch (Exception e) {
                    logger.error("Error while stopping {}", entry.getValue(), e);
                }
            }

            for (Entry<String, SinkRunner> entry : this.materializedConfiguration.getSinkRunners()
                    .entrySet()) {
                try {
                    logger.info("Stopping Sink " + entry.getKey());
                    supervisor.unsupervise(entry.getValue());
                } catch (Exception e) {
                    logger.error("Error while stopping {}", entry.getValue(), e);
                }
            }

            for (Entry<String, Channel> entry : this.materializedConfiguration.getChannels()
                    .entrySet()) {
                try {
                    logger.info("Stopping Channel " + entry.getKey());
                    supervisor.unsupervise(entry.getValue());
                } catch (Exception e) {
                    logger.error("Error while stopping {}", entry.getValue(), e);
                }
            }
        }
        if (monitorServer != null) {
            monitorServer.stop();
        }
    }

    private void startAllComponents(MaterializedConfiguration materializedConfiguration) {
        logger.info("Starting new configuration:{}", materializedConfiguration);

        this.materializedConfiguration = materializedConfiguration;

        for (Entry<String, Channel> entry : materializedConfiguration.getChannels().entrySet()) {
            try {
                logger.info("Starting Channel " + entry.getKey());
                supervisor.supervise(entry.getValue(),
                        new SupervisorPolicy.AlwaysRestartPolicy(), LifecycleState.START);
            } catch (Exception e) {
                logger.error("Error while starting {}", entry.getValue(), e);
            }
        }

        /*
         * Wait for all channels to start.
         */
        for (Channel ch : materializedConfiguration.getChannels().values()) {
            while (ch.getLifecycleState() != LifecycleState.START
                    && !supervisor.isComponentInErrorState(ch)) {
                try {
                    logger.info("Waiting for channel: " + ch.getName()
                            + " to start. Sleeping for 500 ms");
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    logger.error("Interrupted while waiting for channel to start.", e);
                    Throwables.propagate(e);
                }
            }
        }

        for (Entry<String, SinkRunner> entry : materializedConfiguration.getSinkRunners()
                .entrySet()) {
            try {
                logger.info("Starting Sink " + entry.getKey());
                supervisor.supervise(entry.getValue(),
                        new SupervisorPolicy.AlwaysRestartPolicy(), LifecycleState.START);
            } catch (Exception e) {
                logger.error("Error while starting {}", entry.getValue(), e);
            }
        }

        for (Entry<String, SourceRunner> entry : materializedConfiguration.getSourceRunners()
                .entrySet()) {
            try {
                logger.info("Starting Source " + entry.getKey());
                supervisor.supervise(entry.getValue(),
                        new SupervisorPolicy.AlwaysRestartPolicy(), LifecycleState.START);
            } catch (Exception e) {
                logger.error("Error while starting {}", entry.getValue(), e);
            }
        }

        this.loadMonitoring();
    }

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
                    klass = MonitoringType.valueOf(
                            monitorType.toUpperCase(Locale.ENGLISH)).getMonitorClass();
                } catch (Exception e) {
                    // Not a known type, use FQCN
                    klass = (Class<? extends MonitorService>) Class.forName(monitorType);
                }
                this.monitorServer = klass.getDeclaredConstructor().newInstance();
                Context context = new Context();
                for (String key : keys) {
                    if (key.startsWith(CONF_MONITOR_PREFIX)) {
                        context.put(key.substring(CONF_MONITOR_PREFIX.length()),
                                systemProps.getProperty(key));
                    }
                }
                monitorServer.configure(context);
                monitorServer.start();
            }
        } catch (Exception e) {
            logger.warn("Error starting monitoring. "
                    + "Monitoring might not be available.", e);
        }

    }

    /**
     * main
     * @param args
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

            option = new Option("h", "help", false, "display help text");
            options.addOption(option);

            CommandLineParser parser = new GnuParser();
            CommandLine commandLine = parser.parse(options, args);

            if (commandLine.hasOption('h')) {
                new HelpFormatter().printHelp("flume-ng agent", options, true);
                return;
            }

            String agentName = commandLine.getOptionValue('n');
            boolean reload = !commandLine.hasOption("no-reload-conf");

            Application application;

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
                        logger.error("Failed to read canonical path for file: " + path,
                                ex);
                    }
                    throw new ParseException(
                            "The specified configuration file does not exist: " + path);
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
                configurationProvider = new PropertiesFileConfigurationProvider(
                        agentName, configurationFile);
                application = new Application();
                application.handleConfigurationEvent(configurationProvider.getConfiguration());
            }

            //start application
            application.start();

            final Application appReference = application;
            Runtime.getRuntime().addShutdownHook(new Thread("agent-shutdown-hook") {

                @Override
                public void run() {
                    appReference.stop();
                }
            });

        } catch (Exception e) {
            logger.error("A fatal error occurred while running. Exception follows.", e);
        }
    }
}