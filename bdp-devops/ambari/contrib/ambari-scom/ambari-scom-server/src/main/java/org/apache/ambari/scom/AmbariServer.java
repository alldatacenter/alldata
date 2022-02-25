
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

package org.apache.ambari.scom;


import com.google.gson.Gson;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Scopes;
import com.google.inject.Singleton;
import com.google.inject.persist.Transactional;
import com.google.inject.persist.jpa.JpaPersistModule;
import com.sun.jersey.spi.container.servlet.ServletContainer;
import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.api.AmbariPersistFilter;
import org.apache.ambari.server.configuration.ComponentSSLConfiguration;
import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.controller.HostsMap;
import org.apache.ambari.server.orm.GuiceJpaInitializer;
import org.apache.ambari.server.orm.PersistenceType;
import org.apache.ambari.server.security.authorization.AmbariLdapAuthenticationProvider;
import org.apache.ambari.server.security.authorization.AmbariLocalUserDetailsService;
import org.apache.ambari.server.security.authorization.Users;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.nio.SelectChannelConnector;
import org.eclipse.jetty.server.ssl.SslSelectChannelConnector;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.crypto.password.StandardPasswordEncoder;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.GenericWebApplicationContext;
import org.springframework.web.filter.DelegatingFilterProxy;

import javax.crypto.BadPaddingException;
import java.io.File;
import java.net.BindException;
import java.util.Map;
import java.util.Properties;

/**
 * Main Ambari server class.
 */
@Singleton
public class AmbariServer {
  /**
   * The Jetty server.
   */
  private Server server = null;

  /**
   * The Ambari configuration.
   */
  @Inject
  Configuration configuration;

  /**
   * The Guice injector.
   */
  @Inject
  Injector injector;


  // Set the SQLProviderModule for the API providers.
  static {
    System.setProperty("provider.module.class", "org.apache.ambari.scom.SQLProviderModule");
  }

  // ----- Constants ---------------------------------------------------------

  private static final String CONTEXT_PATH = "/";

  private static final String SPRING_CONTEXT_LOCATION =
      "classpath:META-INF/spring-security.xml";

  /**
   * The logger.
   */
  private static final Logger LOG = LoggerFactory.getLogger(AmbariServer.class);


  // ----- AmbariServer ------------------------------------------------------

  public static void main(String[] args) throws Exception {
    Injector injector = Guice.createInjector(new ControllerModule());
    injector.getInstance(GuiceJpaInitializer.class);

    AmbariServer ambariServer = null;
    try {
      LOG.info("Getting the controller");
      ambariServer = injector.getInstance(AmbariServer.class);

      ComponentSSLConfiguration.instance().init(ambariServer.configuration);
      SinkConnectionFactory.instance().init(ambariServer.configuration);
      ClusterDefinitionProvider.instance().init(ambariServer.configuration);

      if (ambariServer != null) {
        ambariServer.run();
      }
    } catch (Throwable t) {
      LOG.error("Failed to run the Ambari Server", t);
      if (ambariServer != null) {
        ambariServer.stop();
      }
      System.exit(-1);
    }
  }


  // ----- helper methods ----------------------------------------------------

  // Run the server
  private void run() throws Exception {
    addInMemoryUsers();

    server = new Server();

    try {
      ClassPathXmlApplicationContext parentSpringAppContext =
          new ClassPathXmlApplicationContext();
      parentSpringAppContext.refresh();
      ConfigurableListableBeanFactory factory = parentSpringAppContext.
          getBeanFactory();
      factory.registerSingleton("guiceInjector",
          injector);
      factory.registerSingleton("passwordEncoder",
          injector.getInstance(PasswordEncoder.class));
      factory.registerSingleton("ambariLocalUserService",
          injector.getInstance(AmbariLocalUserDetailsService.class));
      factory.registerSingleton("ambariLdapAuthenticationProvider",
          injector.getInstance(AmbariLdapAuthenticationProvider.class));

      //Spring Security xml config depends on this Bean
      String[] contextLocations = {SPRING_CONTEXT_LOCATION};
      ClassPathXmlApplicationContext springAppContext = new
          ClassPathXmlApplicationContext(contextLocations, parentSpringAppContext);

      //setting ambari web context
      ServletContextHandler root = new ServletContextHandler(server, CONTEXT_PATH,
          ServletContextHandler.SECURITY | ServletContextHandler.SESSIONS);

      //Changing session cookie name to avoid conflicts
      root.getSessionHandler().getSessionManager().setSessionCookie("AMBARISESSIONID");

      GenericWebApplicationContext springWebAppContext = new GenericWebApplicationContext();
      springWebAppContext.setServletContext(root.getServletContext());
      springWebAppContext.setParent(springAppContext);
      /* Configure web app context */
      root.setResourceBase(configuration.getWebAppDir());

      root.getServletContext().setAttribute(
          WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE,
          springWebAppContext);

      ServletHolder rootServlet = root.addServlet(DefaultServlet.class, "/");
      rootServlet.setInitOrder(1);

      //Spring Security Filter initialization
      DelegatingFilterProxy springSecurityFilter = new DelegatingFilterProxy();
      springSecurityFilter.setTargetBeanName("springSecurityFilterChain");

      //session-per-request strategy for api
      root.addFilter(new FilterHolder(injector.getInstance(AmbariPersistFilter.class)), "/api/*", 1);

      if (configuration.getApiAuthentication()) {
        root.addFilter(new FilterHolder(springSecurityFilter), "/api/*", 1);
      }

      //Secured connector for 2-way auth
      SslSelectChannelConnector sslConnectorTwoWay = new
          SslSelectChannelConnector();
      sslConnectorTwoWay.setPort(configuration.getTwoWayAuthPort());

      Map<String, String> configsMap = configuration.getConfigsMap();
      String keystore = configsMap.get(Configuration.SRVR_KSTR_DIR_KEY) +
          File.separator + configsMap.get(Configuration.KSTR_NAME_KEY);
      String srvrCrtPass = configsMap.get(Configuration.SRVR_CRT_PASS_KEY);
      sslConnectorTwoWay.setKeystore(keystore);
      sslConnectorTwoWay.setTruststore(keystore);
      sslConnectorTwoWay.setPassword(srvrCrtPass);
      sslConnectorTwoWay.setKeyPassword(srvrCrtPass);
      sslConnectorTwoWay.setTrustPassword(srvrCrtPass);
      sslConnectorTwoWay.setKeystoreType("PKCS12");
      sslConnectorTwoWay.setTruststoreType("PKCS12");
      sslConnectorTwoWay.setNeedClientAuth(configuration.getTwoWaySsl());

      //Secured connector for 1-way auth
      SslContextFactory contextFactory = new SslContextFactory(true);
      contextFactory.setKeyStorePath(keystore);
      contextFactory.setTrustStore(keystore);
      contextFactory.setKeyStorePassword(srvrCrtPass);
      contextFactory.setKeyManagerPassword(srvrCrtPass);
      contextFactory.setTrustStorePassword(srvrCrtPass);
      contextFactory.setKeyStoreType("PKCS12");
      contextFactory.setTrustStoreType("PKCS12");

      contextFactory.setNeedClientAuth(false);
      SslSelectChannelConnector sslConnectorOneWay = new SslSelectChannelConnector(contextFactory);
      sslConnectorOneWay.setPort(configuration.getOneWayAuthPort());
      sslConnectorOneWay.setAcceptors(2);
      sslConnectorTwoWay.setAcceptors(2);

      ServletHolder sh = new ServletHolder(ServletContainer.class);
      sh.setInitParameter("com.sun.jersey.config.property.resourceConfigClass",
          "com.sun.jersey.api.core.PackagesResourceConfig");
      sh.setInitParameter("com.sun.jersey.config.property.packages",
          "org.apache.ambari.server.api.rest;" +
              "org.apache.ambari.server.api.services;" +
              "org.apache.ambari.eventdb.webservice;" +
              "org.apache.ambari.server.api");
      sh.setInitParameter("com.sun.jersey.api.json.POJOMappingFeature",
          "true");
      root.addServlet(sh, "/api/v1/*");
      sh.setInitOrder(2);


      //Set jetty thread pool
      server.setThreadPool(new QueuedThreadPool(25));

      /* Configure the API server to use the NIO connectors */
      SelectChannelConnector apiConnector;

      if (configuration.getApiSSLAuthentication()) {
        String httpsKeystore = configsMap.get(Configuration.CLIENT_API_SSL_KSTR_DIR_NAME_KEY) +
            File.separator + configsMap.get(Configuration.CLIENT_API_SSL_KSTR_NAME_KEY);
        LOG.info("API SSL Authentication is turned on. Keystore - " + httpsKeystore);

        String httpsCrtPass = configsMap.get(Configuration.CLIENT_API_SSL_CRT_PASS_KEY);

        SslSelectChannelConnector sapiConnector = new SslSelectChannelConnector();
        sapiConnector.setPort(configuration.getClientSSLApiPort());
        sapiConnector.setKeystore(httpsKeystore);
        sapiConnector.setTruststore(httpsKeystore);
        sapiConnector.setPassword(httpsCrtPass);
        sapiConnector.setKeyPassword(httpsCrtPass);
        sapiConnector.setTrustPassword(httpsCrtPass);
        sapiConnector.setKeystoreType("PKCS12");
        sapiConnector.setTruststoreType("PKCS12");
        sapiConnector.setMaxIdleTime(configuration.getConnectionMaxIdleTime());
        apiConnector = sapiConnector;
      } else {
        apiConnector = new SelectChannelConnector();
        apiConnector.setPort(configuration.getClientApiPort());
        apiConnector.setMaxIdleTime(configuration.getConnectionMaxIdleTime());
      }

      server.addConnector(apiConnector);

      server.setStopAtShutdown(true);
      springAppContext.start();

      String osType = configuration.getServerOsType();
      if (osType == null || osType.isEmpty()) {
        throw new RuntimeException(Configuration.OS_VERSION_KEY + " is not "
            + " set in the ambari.properties file");
      }

      /*
       * Start the server after controller state is recovered.
       */
      server.start();
      LOG.info("********* Started Server **********");

      server.join();
      LOG.info("Joined the Server");
    } catch (BadPaddingException bpe) {

      LOG.error("Bad keystore or private key password. " +
          "HTTPS certificate re-importing may be required.");
      throw bpe;
    } catch (BindException bindException) {

      LOG.error("Could not bind to server port - instance may already be running. " +
          "Terminating this instance.", bindException);
      throw bindException;
    }
  }

  // Creates default users and roles if in-memory database is used
  @Transactional
  void addInMemoryUsers() {
    if (getPersistenceType(configuration) == PersistenceType.IN_MEMORY &&
        configuration.getApiAuthentication()) {
      LOG.info("In-memory database is used - creating default users");
      Users users = injector.getInstance(Users.class);

      try {
        users.createUser("admin", "admin", true, true, false);
        users.createUser("user", "user", true, false, false);
      } catch (AmbariException e) {
        throw new RuntimeException(e);
      }
    }
  }

  // Stop the server
  private void stop() throws Exception {
    try {
      server.stop();
    } catch (Exception e) {
      LOG.error("Error stopping the server", e);
    }
  }

  // get the persistence type for the given configuration
  private static PersistenceType getPersistenceType(Configuration configuration) {
    String value = configuration.getProperty(Configuration.SERVER_PERSISTENCE_TYPE_KEY);
    return value == null ? PersistenceType.IN_MEMORY : PersistenceType.fromString(value);
  }


  // ----- inner class : ControllerModule ------------------------------------

  /**
   * Used for injection purposes.
   */
  private static class ControllerModule extends AbstractModule {

    private final Configuration configuration;
    private final HostsMap hostsMap;


    // ----- Constructors ----------------------------------------------------

    /**
     * Construct a controller module.
     */
    public ControllerModule(){
      configuration = new Configuration();
      hostsMap      = new HostsMap(configuration);
    }


    // ----- AbstractModule --------------------------------------------------

    @Override
    protected void configure() {
      bind(Configuration.class).toInstance(configuration);
      bind(HostsMap.class).toInstance(hostsMap);
      bind(PasswordEncoder.class).toInstance(new StandardPasswordEncoder());

      install(buildJpaPersistModule());
      bind(Gson.class).in(Scopes.SINGLETON);
    }


    // ----- helper methods --------------------------------------------------

    // Create the JPA persistence module
    private JpaPersistModule buildJpaPersistModule() {
      PersistenceType  persistenceType  = getPersistenceType(configuration);
      JpaPersistModule jpaPersistModule = new JpaPersistModule(Configuration.JDBC_UNIT_NAME);
      Properties       properties       = new Properties();
      String           databaseDriver;
      String           databaseUrl;

      if (persistenceType == PersistenceType.LOCAL) {
        databaseDriver = configuration.getLocalDatabaseUrl();
        databaseUrl    = Configuration.JDBC_LOCAL_DRIVER;

      }
      else {
        if (persistenceType == PersistenceType.IN_MEMORY) {
          databaseDriver = Configuration.JDBC_IN_MEMROY_DRIVER;
          databaseUrl = Configuration.JDBC_IN_MEMORY_URL;
        }
        else {
          databaseDriver = configuration.getDatabaseDriver();
          databaseUrl    = configuration.getDatabaseUrl();
        }
      }

      if (databaseDriver != null && databaseUrl != null) {
        properties.setProperty("javax.persistence.jdbc.url",    databaseUrl);
        properties.setProperty("javax.persistence.jdbc.driver", databaseDriver);

        properties.setProperty("eclipselink.logging.level",  "INFO");
        properties.setProperty("eclipselink.logging.logger", "org.apache.ambari.scom.logging.JpaLogger");

        // custom jdbc properties
        Map<String, String> custom = configuration.getDatabaseCustomProperties();

        if (0 != custom.size()) {
          for (Map.Entry<String, String> entry : custom.entrySet()) {
            properties.setProperty("eclipselink.jdbc.property." + entry.getKey(),
                entry.getValue());
          }
        }

        if (persistenceType == PersistenceType.IN_MEMORY) {
          properties.setProperty("eclipselink.ddl-generation",       "drop-and-create-tables");
          properties.setProperty("eclipselink.orm.throw.exceptions", "true");
          jpaPersistModule.properties(properties);
        } else {
          properties.setProperty("javax.persistence.jdbc.user",   configuration.getDatabaseUser());
          properties.setProperty("javax.persistence.jdbc.password",
              configuration.getProperty(Configuration.SERVER_JDBC_USER_PASSWD_KEY));

          switch (configuration.getJPATableGenerationStrategy()) {
            case CREATE:
              properties.setProperty("eclipselink.ddl-generation", "create-tables");
              break;
            case DROP_AND_CREATE:
              properties.setProperty("eclipselink.ddl-generation", "drop-and-create-tables");
              break;
            default:
              break;
          }
          properties.setProperty("eclipselink.ddl-generation.output-mode", "both");
          properties.setProperty("eclipselink.create-ddl-jdbc-file-name",  "DDL-create.jdbc");
          properties.setProperty("eclipselink.drop-ddl-jdbc-file-name",    "DDL-drop.jdbc");

          jpaPersistModule.properties(properties);
        }
      }
      return jpaPersistModule;
    }
  }
}

