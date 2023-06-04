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

package org.apache.inlong.dataproxy.http;

import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;
import java.util.EnumSet;
import javax.servlet.DispatcherType;
import java.lang.reflect.Constructor;
import java.util.Map;
import org.apache.flume.Context;
import org.apache.flume.channel.ChannelProcessor;
import org.apache.inlong.common.monitor.MonitorIndex;
import org.apache.inlong.common.monitor.MonitorIndexExt;
import org.apache.inlong.dataproxy.config.ConfigManager;
import org.apache.inlong.dataproxy.config.remote.ConfigMessageServlet;
import org.apache.inlong.dataproxy.metrics.DataProxyMetricItemSet;
import org.apache.inlong.dataproxy.source.ServiceDecoder;
import org.apache.flume.source.http.HTTPSource;
import org.apache.flume.source.http.HTTPSourceConfigurationConstants;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.SecureRequestCustomizer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SimpleHttpSource extends HttpBaseSource {

    private static final Logger LOG = LoggerFactory.getLogger(HTTPSource.class);

    public static final String POOL_SIZE = "poolSize";
    public static final String IDEL_TIME = "idelTime";
    public static final String BUFFER_SIZE = "bufferSize";
    public static final String BACKLOG = "backlog";

    private volatile Integer port;
    private volatile Server srv;
    private Map<String, String> subProps;
    private MessageHandler messageHandler;

    // SSL configuration variable
    private volatile String keyStorePath;
    private volatile String keyStorePassword;
    private volatile Boolean sslEnabled;

    private int threadPoolSize = 512;
    private int maxIdelTime = 600000;
    private int requestBufferSize = 10000;
    private int backlog = 2048;

    @Override
    public void configure(Context context) {
        super.configure(context);
        try {
            port = context.getInteger(HTTPSourceConfigurationConstants.CONFIG_PORT);
            threadPoolSize = context.getInteger(POOL_SIZE, 512);
            maxIdelTime = context.getInteger(IDEL_TIME, 600000);
            requestBufferSize = context.getInteger(BUFFER_SIZE, 10000);
            backlog = context.getInteger(BACKLOG, 2048);
            LOG.info("http backlog set to {}", backlog);
            checkPort();

            // SSL related config
            sslEnabled = context.getBoolean(HTTPSourceConfigurationConstants.SSL_ENABLED, false);
            if (sslEnabled) {
                LOG.debug("SSL configuration enabled");
                keyStorePath = context.getString(HTTPSourceConfigurationConstants.SSL_KEYSTORE);
                Preconditions.checkArgument(keyStorePath != null && !keyStorePath.isEmpty(),
                        "Keystore is required for SSL Conifguration");
                keyStorePassword =
                        context.getString(HTTPSourceConfigurationConstants.SSL_KEYSTORE_PASSWORD);
                Preconditions.checkArgument(keyStorePassword != null,
                        "Keystore password is required for SSL Configuration");
            }

            // ref: http://docs.codehaus.org/display/JETTY/Embedding+Jetty
            // ref: http://jetty.codehaus.org/jetty/jetty-6/apidocs/org/mortbay/jetty/servlet
            // /Context.html
            subProps = context.getSubProperties(
                    HTTPSourceConfigurationConstants.CONFIG_HANDLER_PREFIX);
        } catch (Exception ex) {
            LOG.error("Error configuring HTTPSource!", ex);
            Throwables.propagate(ex);
        }
    }

    @Override
    public synchronized void stop() {
        super.stop();
    }

    @Override
    public synchronized void start() {
        super.start();
        try {

            @SuppressWarnings("unchecked")
            Class<? extends MessageHandler> clazz =
                    (Class<? extends MessageHandler>) Class.forName(messageHandlerName);
            Constructor ctor = clazz.getConstructor(ChannelProcessor.class,
                    MonitorIndex.class, MonitorIndexExt.class, DataProxyMetricItemSet.class, ServiceDecoder.class);
            LOG.info("Using channel processor:{}", getChannelProcessor().getClass().getName());
            messageHandler = (MessageHandler) ctor
                    .newInstance(getChannelProcessor(), monitorIndex, monitorIndexExt, metricItemSet, null);
            messageHandler.configure(new Context(subProps));
            srv = new Server(new QueuedThreadPool(threadPoolSize));
            Connector[] connectors = new Connector[1];
            if (sslEnabled) {
                SslContextFactory sslContextFactory = new SslContextFactory(keyStorePath);
                sslContextFactory.setKeyStorePassword(keyStorePassword);
                SslConnectionFactory ssl = new SslConnectionFactory(sslContextFactory,
                        "http/1.1");
                HttpConfiguration httpsConfig = new HttpConfiguration();
                httpsConfig.setSecureScheme("https");
                httpsConfig.setSecurePort(port);
                httpsConfig.addCustomizer(new SecureRequestCustomizer());
                ServerConnector http2Connector = new ServerConnector(srv, ssl,
                        new HttpConnectionFactory(httpsConfig));
                srv.addConnector(http2Connector);
                connectors[0] = http2Connector;
                LOG.info("sslEnabled {}", sslEnabled);
            } else {
                ServerConnector connector = new ServerConnector(srv);
                connector.setReuseAddress(true);
                connector.setIdleTimeout(maxIdelTime);
                connector.setAcceptedReceiveBufferSize(requestBufferSize);
                connector.setAcceptQueueSize(backlog);
                connector.setHost(host);
                connector.setPort(port);
                LOG.info("set config maxIdelTime {}, backlog {}", maxIdelTime, backlog);
                connectors[0] = connector;
            }

            srv.setConnectors(connectors);

            ServletContextHandler servletContext =
                    new ServletContextHandler(srv, "/", ServletContextHandler.SESSIONS);
            servletContext.setMaxFormContentSize(maxMsgLength);
            servletContext
                    .addFilter(new FilterHolder(new MessageFilter(maxMsgLength)), "/dataproxy/*",
                            EnumSet.of(DispatcherType.REQUEST));
            servletContext.addServlet(new ServletHolder(new MessageProcessServlet(messageHandler)),
                    "/dataproxy/*");
            servletContext.addServlet(new ServletHolder(new ConfigMessageServlet()),
                    "/dataproxy/config/*");
            srv.start();
            ConfigManager.getInstance().addSourceReportInfo(
                    host, String.valueOf(port), "HTTP");
            Preconditions.checkArgument(srv.getHandler().equals(servletContext));
        } catch (ClassNotFoundException ex) {
            LOG.error("Error while configuring HTTPSource. Exception follows.", ex);
            Throwables.propagate(ex);
        } catch (ClassCastException ex) {
            LOG.error("Deserializer is not an instance of HTTPSourceHandler."
                    + "Deserializer must implement HTTPSourceHandler.");
            Throwables.propagate(ex);
        } catch (Exception ex) {
            LOG.error("Error while starting HTTPSource. Exception follows.", ex);
            Throwables.propagate(ex);
        }
        Preconditions.checkArgument(srv.isRunning());
    }

    private void checkPort() {
        Preconditions.checkNotNull(port, "HTTPSource requires a port number to be"
                + "specified");
    }
}
