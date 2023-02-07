/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.drill.exec.server.rest;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.servlets.MetricsServlet;
import com.codahale.metrics.servlets.ThreadDumpServlet;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.drill.common.config.DrillConfig;
import org.apache.drill.common.exceptions.DrillException;
import org.apache.drill.common.util.DrillFileUtils;
import org.apache.drill.exec.ExecConstants;
import org.apache.drill.exec.exception.DrillbitStartupException;
import org.apache.drill.exec.expr.fn.registry.FunctionHolder;
import org.apache.drill.exec.expr.fn.registry.LocalFunctionRegistry;
import org.apache.drill.exec.server.BootStrapContext;
import org.apache.drill.exec.server.Drillbit;
import org.apache.drill.exec.server.options.OptionList;
import org.apache.drill.exec.server.options.OptionManager;
import org.apache.drill.exec.server.options.OptionValidator.OptionDescription;
import org.apache.drill.exec.server.options.OptionValue;
import org.apache.drill.exec.server.rest.auth.DrillErrorHandler;
import org.apache.drill.exec.server.rest.auth.DrillHttpSecurityHandlerProvider;
import org.apache.drill.exec.server.rest.header.ResponseHeadersSettingFilter;
import org.apache.drill.exec.server.rest.ssl.SslContextFactoryConfigurator;
import org.apache.drill.exec.work.WorkManager;
import org.eclipse.jetty.http.HttpVersion;
import org.eclipse.jetty.security.SecurityHandler;
import org.eclipse.jetty.security.authentication.SessionAuthentication;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.SecureRequestCustomizer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.server.handler.ErrorHandler;
import org.eclipse.jetty.server.session.SessionHandler;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.servlets.CrossOriginFilter;
import org.eclipse.jetty.util.resource.Resource;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.glassfish.jersey.servlet.ServletContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.DispatcherType;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Wrapper class around jetty based web server.
 */
public class WebServer implements AutoCloseable {
  private static final Logger logger = LoggerFactory.getLogger(WebServer.class);

  private static final String ACE_MODE_SQL_TEMPLATE_JS = "ace.mode-sql.template.js";
  private static final String ACE_MODE_SQL_JS = "mode-sql.js";
  private static final String DRILL_FUNCTIONS_PLACEHOLDER = "__DRILL_FUNCTIONS__";

  private static final String STATUS_THREADS_PATH = "/status/threads";
  private static final String STATUS_METRICS_PATH = "/status/metrics";

  private static final String OPTIONS_DESCRIBE_JS = "options.describe.js";
  private static final String OPTIONS_DESCRIBE_TEMPLATE_JS = "options.describe.template.js";

  private static final int PORT_HUNT_TRIES = 100;
  private static final String BASE_STATIC_PATH = "/rest/static/";
  private static final String DRILL_ICON_RESOURCE_RELATIVE_PATH = "img/drill.ico";

  private final DrillConfig config;
  private final MetricRegistry metrics;
  private final WorkManager workManager;
  private final Drillbit drillbit;

  private Server embeddedJetty;

  private File tmpJavaScriptDir;

  /**
   * Create Jetty based web server.
   *
   * @param context     Bootstrap context.
   * @param workManager WorkManager instance.
   * @param drillbit    Drillbit instance.
   */
  public WebServer(final BootStrapContext context, final WorkManager workManager, final Drillbit drillbit) {
    this.config = context.getConfig();
    this.metrics = context.getMetrics();
    this.workManager = workManager;
    this.drillbit = drillbit;
  }

  /**
   * Checks if only impersonation is enabled.
   *
   * @param config Drill configuration
   * @return true if impersonation without authentication is enabled, false otherwise
   */
  public static boolean isOnlyImpersonationEnabled(DrillConfig config) {
    return !config.getBoolean(ExecConstants.USER_AUTHENTICATION_ENABLED)
        && config.getBoolean(ExecConstants.IMPERSONATION_ENABLED);
  }

  /**
   * Start the web server including setup.
   */
  public void start() throws Exception {
    if (!config.getBoolean(ExecConstants.HTTP_ENABLE)) {
      return;
    }

    final QueuedThreadPool threadPool = new QueuedThreadPool(2, 2);
    embeddedJetty = new Server(threadPool);

    final boolean authEnabled = config.getBoolean(ExecConstants.USER_AUTHENTICATION_ENABLED);
    ServletContextHandler webServerContext = createServletContextHandler(authEnabled);
    embeddedJetty.setHandler(webServerContext);

    final int acceptors = config.getInt(ExecConstants.HTTP_JETTY_SERVER_ACCEPTORS);
    final int selectors = config.getInt(ExecConstants.HTTP_JETTY_SERVER_SELECTORS);
    int port = config.getInt(ExecConstants.HTTP_PORT);
    ServerConnector connector = createConnector(port, acceptors, selectors);

    final int handlers = config.getInt(ExecConstants.HTTP_JETTY_SERVER_HANDLERS);
    threadPool.setMaxThreads(handlers + connector.getAcceptors() + connector.getSelectorManager().getSelectorCount());
    embeddedJetty.addConnector(connector);

    embeddedJetty.setDumpAfterStart(config.getBoolean(ExecConstants.HTTP_JETTY_SERVER_DUMP_AFTER_START));
    final boolean portHunt = config.getBoolean(ExecConstants.HTTP_PORT_HUNT);
    for (int retry = 0; retry < PORT_HUNT_TRIES; retry++) {
      connector.setPort(port);
      try {
        embeddedJetty.start();
        return;
      } catch (IOException e) {
        if (portHunt) {
          logger.info("Failed to start on port {}, trying port {}", port, ++port, e);
        } else {
          throw e;
        }
      }
    }
    throw new IOException("Failed to find a port");
  }

  private ServletContextHandler createServletContextHandler(final boolean authEnabled) throws DrillbitStartupException {
    // Add resources
    final ErrorHandler errorHandler = new DrillErrorHandler();

    errorHandler.setShowStacks(true);
    errorHandler.setShowMessageInTitle(true);

    final ServletContextHandler servletContextHandler = new ServletContextHandler(ServletContextHandler.SESSIONS);
    servletContextHandler.setErrorHandler(errorHandler);
    servletContextHandler.setContextPath("/");

    final ServletHolder servletHolder = new ServletHolder(new ServletContainer(
        new DrillRestServer(workManager, servletContextHandler.getServletContext(), drillbit)));
    servletHolder.setInitOrder(1);
    servletContextHandler.addServlet(servletHolder, "/*");

    servletContextHandler.addServlet(new ServletHolder(new MetricsServlet(metrics)), STATUS_METRICS_PATH);
    servletContextHandler.addServlet(new ServletHolder(new ThreadDumpServlet()), STATUS_THREADS_PATH);

    final ServletHolder staticHolder = new ServletHolder("static", DefaultServlet.class);

    // Get resource URL for Drill static assets, based on where Drill icon is located
    String drillIconResourcePath =
        Resource.newClassPathResource(BASE_STATIC_PATH + DRILL_ICON_RESOURCE_RELATIVE_PATH).getURI().toString();
    staticHolder.setInitParameter("resourceBase",
        drillIconResourcePath.substring(0, drillIconResourcePath.length() - DRILL_ICON_RESOURCE_RELATIVE_PATH.length()));
    staticHolder.setInitParameter("dirAllowed", "false");
    staticHolder.setInitParameter("pathInfoOnly", "true");
    servletContextHandler.addServlet(staticHolder, "/static/*");

    // Add Local path resource (This will allow access to dynamically created files like JavaScript)
    final ServletHolder dynamicHolder = new ServletHolder("dynamic", DefaultServlet.class);

    // Skip if unable to get a temp directory (e.g. during Unit tests)
    if (getOrCreateTmpJavaScriptDir() != null) {
      dynamicHolder.setInitParameter("resourceBase", getOrCreateTmpJavaScriptDir().getAbsolutePath());
      dynamicHolder.setInitParameter("dirAllowed", "true");
      dynamicHolder.setInitParameter("pathInfoOnly", "true");
      servletContextHandler.addServlet(dynamicHolder, "/dynamic/*");
    }

    if (authEnabled) {
      // DrillSecurityHandler is used to support SPNEGO and FORM authentication together
      servletContextHandler.setSecurityHandler(new DrillHttpSecurityHandlerProvider(config, workManager.getContext()));
      servletContextHandler.setSessionHandler(createSessionHandler(servletContextHandler.getSecurityHandler()));
    }

    // Applying filters for CSRF protection.
    servletContextHandler.addFilter(CsrfTokenInjectFilter.class, "/*", EnumSet.of(DispatcherType.REQUEST));
    for (String path : new String[]{"/query", "/storage/create_update", "/option/*"}) {
      servletContextHandler.addFilter(CsrfTokenValidateFilter.class, path, EnumSet.of(DispatcherType.REQUEST));
    }

    if (config.getBoolean(ExecConstants.HTTP_CORS_ENABLED)) {
      FilterHolder holder = new FilterHolder(CrossOriginFilter.class);
      holder.setInitParameter(CrossOriginFilter.ALLOWED_ORIGINS_PARAM,
          StringUtils.join(config.getStringList(ExecConstants.HTTP_CORS_ALLOWED_ORIGINS), ","));
      holder.setInitParameter(CrossOriginFilter.ALLOWED_METHODS_PARAM,
          StringUtils.join(config.getStringList(ExecConstants.HTTP_CORS_ALLOWED_METHODS), ","));
      holder.setInitParameter(CrossOriginFilter.ALLOWED_HEADERS_PARAM,
          StringUtils.join(config.getStringList(ExecConstants.HTTP_CORS_ALLOWED_HEADERS), ","));
      holder.setInitParameter(CrossOriginFilter.ALLOW_CREDENTIALS_PARAM,
          String.valueOf(config.getBoolean(ExecConstants.HTTP_CORS_CREDENTIALS)));

      for (String path : new String[]{"*.json", "/storage/*/enable/*", "/status*"}) {
        servletContextHandler.addFilter(holder, path, EnumSet.of(DispatcherType.REQUEST));
      }
    }

    // Allow for Other Drillbits to make REST calls
    FilterHolder filterHolder = new FilterHolder(CrossOriginFilter.class);
    filterHolder.setInitParameter("allowedOrigins", "*");

    // Allowing CORS for metrics only
    servletContextHandler.addFilter(filterHolder, STATUS_METRICS_PATH, null);

    FilterHolder responseHeadersSettingFilter = new FilterHolder(ResponseHeadersSettingFilter.class);
    responseHeadersSettingFilter.setInitParameters(ResponseHeadersSettingFilter.retrieveResponseHeaders(config));
    servletContextHandler.addFilter(responseHeadersSettingFilter, "/*", EnumSet.of(DispatcherType.REQUEST));

    return servletContextHandler;
  }

  /**
   * Create a {@link SessionHandler}
   *
   * @param securityHandler Set of init parameters that are used by the Authentication
   * @return session handler
   */
  private SessionHandler createSessionHandler(final SecurityHandler securityHandler) {
    SessionHandler sessionHandler = new SessionHandler();
    //SessionManager sessionManager = new HashSessionManager();
    sessionHandler.setMaxInactiveInterval(config.getInt(ExecConstants.HTTP_SESSION_MAX_IDLE_SECS));
    // response cookie will be returned with HttpOnly flag
    sessionHandler.getSessionCookieConfig().setHttpOnly(true);
    sessionHandler.addEventListener(new HttpSessionListener() {
      @Override
      public void sessionCreated(HttpSessionEvent se) { }

      @Override
      public void sessionDestroyed(HttpSessionEvent se) {
        final HttpSession session = se.getSession();
        if (session == null) {
          return;
        }

        final Object authCreds = session.getAttribute(SessionAuthentication.__J_AUTHENTICATED);
        if (authCreds != null) {
          final SessionAuthentication sessionAuth = (SessionAuthentication) authCreds;
          securityHandler.logout(sessionAuth);
          session.removeAttribute(SessionAuthentication.__J_AUTHENTICATED);
        }

        // Clear all the resources allocated for this session
        final WebSessionResources webSessionResources =
            (WebSessionResources) session.getAttribute(WebSessionResources.class.getSimpleName());

        if (webSessionResources != null) {
          webSessionResources.close();
          session.removeAttribute(WebSessionResources.class.getSimpleName());
        }
      }
    });

    return sessionHandler;
  }

  public int getPort() {
    if (!isRunning()) {
      throw new UnsupportedOperationException("Http is not enabled");
    }
    return ((ServerConnector)embeddedJetty.getConnectors()[0]).getPort();
  }

  public boolean isRunning() {
    return (embeddedJetty != null && embeddedJetty.getConnectors().length == 1);
  }

  private ServerConnector createConnector(int port, int acceptors, int selectors) throws Exception {
    final ServerConnector serverConnector;
    if (config.getBoolean(ExecConstants.HTTP_ENABLE_SSL)) {
      try {
        serverConnector = createHttpsConnector(port, acceptors, selectors);
      } catch (DrillException e) {
        throw new DrillbitStartupException(e.getMessage(), e);
      }
    } else {
      serverConnector = createHttpConnector(port, acceptors, selectors);
    }

    return serverConnector;
  }

  /**
   * Create an HTTPS connector for given jetty server instance. If the admin has
   * specified keystore/truststore settings they will be used else a self-signed
   * certificate is generated and used.
   *
   * @return Initialized {@link ServerConnector} for HTTPS connections.
   */
  private ServerConnector createHttpsConnector(int port, int acceptors, int selectors) throws Exception {
    logger.info("Setting up HTTPS connector for web server");
    SslContextFactory sslContextFactory = new SslContextFactoryConfigurator(config,
        workManager.getContext().getEndpoint().getAddress())
        .configureNewSslContextFactory();
    final HttpConfiguration httpsConfig = baseHttpConfig();
    httpsConfig.addCustomizer(new SecureRequestCustomizer());

    // SSL Connector
    final ServerConnector sslConnector = new ServerConnector(embeddedJetty,
        null, null, null, acceptors, selectors,
        new SslConnectionFactory(sslContextFactory, HttpVersion.HTTP_1_1.asString()),
        new HttpConnectionFactory(httpsConfig));
    sslConnector.setPort(port);

    return sslConnector;
  }

  /**
   * Create HTTP connector.
   *
   * @return Initialized {@link ServerConnector} instance for HTTP connections.
   */
  private ServerConnector createHttpConnector(int port, int acceptors, int selectors) {
    logger.info("Setting up HTTP connector for web server");
    final ServerConnector httpConnector =
        new ServerConnector(embeddedJetty, null, null, null, acceptors, selectors, new HttpConnectionFactory(baseHttpConfig()));
    httpConnector.setPort(port);

    return httpConnector;
  }

  private HttpConfiguration baseHttpConfig() {
    HttpConfiguration httpConfig = new HttpConfiguration();
    httpConfig.setSendServerVersion(false);
    return httpConfig;
  }

  @Override
  public void close() throws Exception {
    if (embeddedJetty != null) {
      embeddedJetty.stop();
    }
    // Deleting temp directory
    FileUtils.deleteQuietly(tmpJavaScriptDir);
  }

  /**
   * Creates if not exists, and returns File for temporary Javascript directory
   * @return file handle
   */
  public File getOrCreateTmpJavaScriptDir() {
    if (tmpJavaScriptDir == null && this.drillbit.getContext() != null) {
      tmpJavaScriptDir = DrillFileUtils.createTempDir();
      // Perform All auto generated files at this point
      try {
        generateOptionsDescriptionJSFile();
        generateFunctionJS();
      } catch (IOException e) {
        logger.error("Unable to create temp dir for JavaScripts: {}", tmpJavaScriptDir.getPath(), e);
      }
    }
    return tmpJavaScriptDir;
  }

  /**
   * Generate Options Description JavaScript to serve http://drillhost/options ACE library search features
   * @throws IOException when unable to generate functions JS file
   */
  private void generateOptionsDescriptionJSFile() throws IOException {
    // Obtain list of Options & their descriptions
    OptionManager optionManager = this.drillbit.getContext().getOptionManager();
    OptionList publicOptions = optionManager.getPublicOptionList();
    List<OptionValue> options = new ArrayList<>(publicOptions);
    // Add internal options
    OptionList internalOptions = optionManager.getInternalOptionList();
    options.addAll(internalOptions);
    Collections.sort(options);
    int numLeftToWrite = options.size();

    // Template source Javascript file
    InputStream optionsDescribeTemplateStream = Resource.newClassPathResource(OPTIONS_DESCRIBE_TEMPLATE_JS).getInputStream();
    // Generated file
    File optionsDescriptionFile = new File(getOrCreateTmpJavaScriptDir(), OPTIONS_DESCRIBE_JS);
    final String file_content_footer = "};";
    // Create a copy of a template and write with that!
    java.nio.file.Files.copy(optionsDescribeTemplateStream, optionsDescriptionFile.toPath());
    logger.info("Will write {} descriptions to {}", numLeftToWrite, optionsDescriptionFile.getAbsolutePath());

    try (BufferedWriter writer = new BufferedWriter(new FileWriter(optionsDescriptionFile, true))) {
      // Iterate through options
      for (OptionValue option : options) {
        numLeftToWrite--;
        String optionName = option.getName();
        OptionDescription optionDescription = optionManager.getOptionDefinition(optionName).getValidator().getOptionDescription();
        if (optionDescription != null) {
          // Note: We don't need to worry about short descriptions for WebUI, since they will never be explicitly accessed from the map
          writer.append("  \"").append(optionName).append("\" : \"")
          .append(StringEscapeUtils.escapeEcmaScript(optionDescription.getDescription()))
          .append( numLeftToWrite > 0 ? "\"," : "\"");
          writer.newLine();
        }
      }
      writer.append(file_content_footer);
      writer.newLine();
      writer.flush();
    }
  }

  /**
   * Generates ACE library javascript populated with list of available SQL functions
   * @throws IOException when unable to generate JS file with functions
   */
  private void generateFunctionJS() throws IOException {
    // Naturally ordered set of function names
    TreeSet<String> functionSet = new TreeSet<>();
    // Extracting ONLY builtIn functions (i.e those already available)
    List<FunctionHolder> builtInFuncHolderList = this.drillbit.getContext().getFunctionImplementationRegistry().getLocalFunctionRegistry()
        .getAllJarsWithFunctionsHolders().get(LocalFunctionRegistry.BUILT_IN);

    // Build List of 'usable' functions (i.e. functions that start with an alphabet and can be auto-completed by the ACE library)
    // Example of 'unusable' functions would be operators like '<', '!'
    int skipCount = 0;
    for (FunctionHolder builtInFunctionHolder : builtInFuncHolderList) {
      String name = builtInFunctionHolder.getName();
      if (!name.contains(" ") && name.matches("([a-z]|[A-Z])\\w+") && !builtInFunctionHolder.getHolder().isInternal()) {
        functionSet.add(name);
      } else {
        logger.debug("Non-alphabetic leading character. Function skipped : {} ", name);
        skipCount++;
      }
    }
    logger.debug("{} functions will not be available in WebUI", skipCount);

    // Generated file
    File functionsListFile = new File(getOrCreateTmpJavaScriptDir(), ACE_MODE_SQL_JS);
    // Template source Javascript file
    try (InputStream aceModeSqlTemplateStream = Resource.newClassPathResource(ACE_MODE_SQL_TEMPLATE_JS).getInputStream()) {
      // Create a copy of a template and write with that!
      java.nio.file.Files.copy(aceModeSqlTemplateStream, functionsListFile.toPath());
    }

    // Construct String
    String funcListString = String.join("|", functionSet);

    Path path = Paths.get(functionsListFile.getPath());
    try (Stream<String> lines = Files.lines(path)) {
      List <String> replaced =
          lines // Replacing first occurrence
            .map(line -> line.replaceFirst(DRILL_FUNCTIONS_PLACEHOLDER, funcListString))
            .collect(Collectors.toList());
      Files.write(path, replaced);
    }
  }
}
