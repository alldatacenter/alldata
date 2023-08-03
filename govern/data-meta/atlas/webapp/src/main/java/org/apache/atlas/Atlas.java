/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.atlas;

import org.apache.atlas.security.SecurityProperties;
import org.apache.atlas.web.service.EmbeddedServer;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.lang.StringUtils;
import org.apache.hadoop.util.ShutdownHookManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;

import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Iterator;

/**
 * Driver for running Metadata as a standalone server with embedded jetty server.
 */
public final class Atlas {
    private static final Logger LOG = LoggerFactory.getLogger(Atlas.class);
    private static final String APP_PATH = "app";
    private static final String APP_PORT = "port";
    private static final String ATLAS_HOME = "atlas.home";
    private static final String ATLAS_DATA = "atlas.data";
    private static final String ATLAS_LOG_DIR = "atlas.log.dir";
    public static final String ATLAS_SERVER_HTTPS_PORT = "atlas.server.https.port";
    public static final String ATLAS_SERVER_HTTP_PORT = "atlas.server.http.port";


    private static EmbeddedServer server;

    static {
        ShutdownHookManager.get().addShutdownHook(new Thread() {
            @Override
            public void run() {
                try {
                    LOG.info("==> Shutdown of Atlas");

                    shutdown();
                } catch (Exception e) {
                    LOG.error("Failed to shutdown", e);
                } finally {
                    LOG.info("<== Shutdown of Atlas");
                }
            }
        }, AtlasConstants.ATLAS_SHUTDOWN_HOOK_PRIORITY);
    }

    private static void shutdown() {
        server.stop();
    }

    /**
     * Prevent users from constructing this.
     */
    private Atlas() {
    }

    protected static CommandLine parseArgs(String[] args) throws ParseException {
        Options options = new Options();
        Option opt;

        opt = new Option(APP_PATH, true, "Application Path");
        opt.setRequired(false);
        options.addOption(opt);

        opt = new Option(APP_PORT, true, "Application Port");
        opt.setRequired(false);
        options.addOption(opt);

        return new GnuParser().parse(options, args);
    }

    public static void main(String[] args) throws Exception {
        CommandLine cmd = parseArgs(args);
        PropertiesConfiguration buildConfiguration = new PropertiesConfiguration("atlas-buildinfo.properties");
        String appPath = "webapp/target/atlas-webapp-" + getProjectVersion(buildConfiguration);

        if (cmd.hasOption(APP_PATH)) {
            appPath = cmd.getOptionValue(APP_PATH);
        }

        setApplicationHome();
        Configuration configuration = ApplicationProperties.get();
        final String enableTLSFlag = configuration.getString(SecurityProperties.TLS_ENABLED);
        final String appHost = configuration.getString(SecurityProperties.BIND_ADDRESS, EmbeddedServer.ATLAS_DEFAULT_BIND_ADDRESS);

        if (!isLocalAddress(InetAddress.getByName(appHost))) {
            String msg =
                "Failed to start Atlas server. Address " + appHost
                    + " does not belong to this host. Correct configuration parameter: "
                    + SecurityProperties.BIND_ADDRESS;
            LOG.error(msg);
            throw new IOException(msg);
        }

        final int appPort = getApplicationPort(cmd, enableTLSFlag, configuration);
        System.setProperty(AtlasConstants.SYSTEM_PROPERTY_APP_PORT, String.valueOf(appPort));
        final boolean enableTLS = isTLSEnabled(enableTLSFlag, appPort);
        configuration.setProperty(SecurityProperties.TLS_ENABLED, String.valueOf(enableTLS));

        showStartupInfo(buildConfiguration, enableTLS, appPort);

        server = EmbeddedServer.newServer(appHost, appPort, appPath, enableTLS);
        installLogBridge();

        server.start();
    }

    private static void setApplicationHome() {
        if (System.getProperty(ATLAS_HOME) == null) {
            System.setProperty(ATLAS_HOME, "target");
        }
        if (System.getProperty(ATLAS_DATA) == null) {
            System.setProperty(ATLAS_DATA, "target/data");
        }
        if (System.getProperty(ATLAS_LOG_DIR) == null) {
            System.setProperty(ATLAS_LOG_DIR, "target/logs");
        }
    }

    public static String getProjectVersion(PropertiesConfiguration buildConfiguration) {
        return buildConfiguration.getString("project.version");
    }

    static int getApplicationPort(CommandLine cmd, String enableTLSFlag, Configuration configuration) {
        String optionValue = cmd.hasOption(APP_PORT) ? cmd.getOptionValue(APP_PORT) : null;

        final int appPort;

        if (StringUtils.isNotEmpty(optionValue)) {
            appPort = Integer.valueOf(optionValue);
        } else {
            // default : atlas.enableTLS is true
            appPort = getPortValue(configuration, enableTLSFlag);
        }

        return appPort;
    }

    private static int getPortValue(Configuration configuration, String enableTLSFlag) {
        int appPort;

        assert configuration != null;
        appPort = StringUtils.isEmpty(enableTLSFlag) || enableTLSFlag.equals("true") ?
            configuration.getInt(ATLAS_SERVER_HTTPS_PORT, 21443) :
            configuration.getInt(ATLAS_SERVER_HTTP_PORT, 21000);
        return appPort;
    }

    private static boolean isTLSEnabled(String enableTLSFlag, int appPort) {
        return Boolean.valueOf(StringUtils.isEmpty(enableTLSFlag) ?
                System.getProperty(SecurityProperties.TLS_ENABLED, (appPort % 1000) == 443 ? "true" : "false") : enableTLSFlag);
    }

    private static boolean isLocalAddress(InetAddress addr) {
        // Check if the address is any local or loop back
        boolean local = addr.isAnyLocalAddress() || addr.isLoopbackAddress();

        // Check if the address is defined on any interface
        if (!local) {
            try {
                local = NetworkInterface.getByInetAddress(addr) != null;
            } catch (SocketException e) {
                local = false;
            }
        }
        return local;
    }

    private static void showStartupInfo(PropertiesConfiguration buildConfiguration, boolean enableTLS, int appPort) {
        StringBuilder buffer = new StringBuilder();
        buffer.append("\n############################################");
        buffer.append("############################################");
        buffer.append("\n                               Atlas Server (STARTUP)");
        buffer.append("\n");
        try {
            final Iterator<String> keys = buildConfiguration.getKeys();
            while (keys.hasNext()) {
                String key = keys.next();
                buffer.append('\n').append('\t').append(key).
                        append(":\t").append(buildConfiguration.getProperty(key));
            }
        } catch (Throwable e) {
            buffer.append("*** Unable to get build info ***");
        }
        buffer.append("\n############################################");
        buffer.append("############################################");
        LOG.info(buffer.toString());
        LOG.info(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");
        LOG.info("Server starting with TLS ? {} on port {}", enableTLS, appPort);
        LOG.info("<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<");
    }

    private static void installLogBridge() {
        // Optionally remove existing handlers attached to j.u.l root logger
        SLF4JBridgeHandler.removeHandlersForRootLogger();  // (since SLF4J 1.6.5)

        // add SLF4JBridgeHandler to j.u.l's root logger, should be done once during
        // the initialization phase of your application
        SLF4JBridgeHandler.install();
    }

}
