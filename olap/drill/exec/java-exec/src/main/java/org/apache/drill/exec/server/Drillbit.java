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
package org.apache.drill.exec.server;

import org.apache.curator.framework.api.ACLProvider;
import org.apache.curator.utils.ZKPaths;
import org.apache.drill.common.AutoCloseables;
import org.apache.drill.common.StackTrace;
import org.apache.drill.common.concurrent.ExtendedLatch;
import org.apache.drill.common.config.DrillConfig;
import org.apache.drill.common.map.CaseInsensitiveMap;
import org.apache.drill.common.scanner.ClassPathScanner;
import org.apache.drill.common.scanner.persistence.ScanResult;
import org.apache.drill.exec.ExecConstants;
import org.apache.drill.exec.coord.ClusterCoordinator;
import org.apache.drill.exec.coord.ClusterCoordinator.RegistrationHandle;
import org.apache.drill.exec.coord.zk.ZKACLProviderFactory;
import org.apache.drill.exec.coord.zk.ZKClusterCoordinator;
import org.apache.drill.exec.exception.DrillbitStartupException;
import org.apache.drill.exec.proto.CoordinationProtos.DrillbitEndpoint;
import org.apache.drill.exec.proto.CoordinationProtos.DrillbitEndpoint.State;
import org.apache.drill.exec.server.DrillbitStateManager.DrillbitState;
import org.apache.drill.exec.server.options.OptionDefinition;
import org.apache.drill.exec.server.options.OptionValue;
import org.apache.drill.exec.server.options.OptionValue.OptionScope;
import org.apache.drill.exec.server.options.SystemOptionManager;
import org.apache.drill.exec.server.rest.WebServer;
import org.apache.drill.exec.service.ServiceEngine;
import org.apache.drill.exec.store.StoragePluginRegistry;
import org.apache.drill.exec.store.sys.PersistentStoreProvider;
import org.apache.drill.exec.store.sys.PersistentStoreRegistry;
import org.apache.drill.exec.store.sys.store.provider.CachingPersistentStoreProvider;
import org.apache.drill.exec.store.sys.store.provider.InMemoryStoreProvider;
import org.apache.drill.exec.store.sys.store.provider.LocalPersistentStoreProvider;
import org.apache.drill.common.util.GuavaPatcher;
import org.apache.drill.common.util.ProtobufPatcher;
import org.apache.drill.exec.work.WorkManager;
import org.apache.drill.shaded.guava.com.google.common.annotations.VisibleForTesting;
import org.apache.drill.shaded.guava.com.google.common.base.Stopwatch;
import org.apache.zookeeper.Environment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;

import javax.tools.ToolProvider;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Starts, tracks and stops all the required services for a Drillbit daemon to work.
 */
public class Drillbit implements AutoCloseable {

  private static final Logger logger = LoggerFactory.getLogger(Drillbit.class);

  static {
    /*
     * HBase and MapR-DB clients use older version of protobuf,
     * and override some methods that became final in recent versions.
     * This code removes these final modifiers.
     */
    ProtobufPatcher.patch();
    /*
     * Some libraries, such as Hadoop or HBase, depend on incompatible versions of Guava.
     * This code adds back some methods to so that the libraries can work with single Guava version.
     */
    GuavaPatcher.patch();
    Environment.logEnv("Drillbit environment: ", logger);
    // Jersey uses java.util.logging - create bridge: jul to slf4j
    SLF4JBridgeHandler.removeHandlersForRootLogger();
    SLF4JBridgeHandler.install();
  }

  public final static String SYSTEM_OPTIONS_NAME = "org.apache.drill.exec.server.Drillbit.system_options";

  private final ClusterCoordinator coord;
  private final ServiceEngine engine;
  private final PersistentStoreProvider storeProvider;
  private final WorkManager manager;
  private final BootStrapContext context;
  private final WebServer webServer;
  private final int gracePeriod;
  private final DrillbitStateManager stateManager;
  private GracefulShutdownThread gracefulShutdownThread;
  private Thread shutdownHook;

  private volatile boolean quiescentMode;
  private volatile boolean forcefulShutdown;
  private volatile boolean interruptPollShutdown = true;

  public void setQuiescentMode(boolean quiescentMode) {
    this.quiescentMode = quiescentMode;
  }

  public void setForcefulShutdown(boolean forcefulShutdown) {
    this.forcefulShutdown = forcefulShutdown;
  }

  public RegistrationHandle getRegistrationHandle() {
    return registrationHandle;
  }

  private RegistrationHandle registrationHandle;
  private volatile StoragePluginRegistry storageRegistry;
  private final PersistentStoreProvider profileStoreProvider;

  @VisibleForTesting
  public Drillbit(
    final DrillConfig config,
    final RemoteServiceSet serviceSet) throws Exception {
    this(config, SystemOptionManager.createDefaultOptionDefinitions(), serviceSet, ClassPathScanner.fromPrescan(config));
  }

  @VisibleForTesting
  public Drillbit(
      final DrillConfig config,
      final CaseInsensitiveMap<OptionDefinition> definitions,
      final RemoteServiceSet serviceSet) throws Exception {
    this(config, definitions, serviceSet, ClassPathScanner.fromPrescan(config));
  }

  public Drillbit(
    final DrillConfig config,
    final RemoteServiceSet serviceSet,
    final ScanResult classpathScan) throws Exception {
    this(config, SystemOptionManager.createDefaultOptionDefinitions(), serviceSet, classpathScan);
  }

  @VisibleForTesting
  public Drillbit(
    final DrillConfig config,
    final CaseInsensitiveMap<OptionDefinition> definitions,
    final RemoteServiceSet serviceSet,
    final ScanResult classpathScan) throws Exception {

    //Must start up with access to JDK Compiler
    if (ToolProvider.getSystemJavaCompiler() == null) {
      throw new DrillbitStartupException("JDK Java compiler not available. Ensure Drill is running with the java executable from a JDK and not a JRE");
    }

    gracePeriod = config.getInt(ExecConstants.GRACE_PERIOD);
    final Stopwatch w = Stopwatch.createStarted();
    logger.debug("Construction started.");
    boolean drillPortHunt = config.getBoolean(ExecConstants.DRILL_PORT_HUNT);
    boolean bindToLoopbackAddress = config.getBoolean(ExecConstants.ALLOW_LOOPBACK_ADDRESS_BINDING);
    final boolean allowPortHunting = (serviceSet != null) || drillPortHunt;
    context = new BootStrapContext(config, definitions, classpathScan);
    manager = new WorkManager(context);

    webServer = new WebServer(context, manager, this);
    boolean isDistributedMode = (serviceSet == null) && !bindToLoopbackAddress;
    if (serviceSet != null) {
      coord = serviceSet.getCoordinator();
      storeProvider = new CachingPersistentStoreProvider(new LocalPersistentStoreProvider(config));
    } else {
      String clusterId = config.getString(ExecConstants.SERVICE_NAME);
      String zkRoot = config.getString(ExecConstants.ZK_ROOT);
      String drillClusterPath = ZKPaths.PATH_SEPARATOR + zkRoot + ZKPaths.PATH_SEPARATOR + clusterId;
      ACLProvider aclProvider = ZKACLProviderFactory.getACLProvider(config, drillClusterPath, context);
      coord = new ZKClusterCoordinator(config, aclProvider);
      storeProvider = new PersistentStoreRegistry<>(this.coord, config).newPStoreProvider();
    }

    //Check if InMemory Profile Store, else use Default Store Provider
    if (config.getBoolean(ExecConstants.PROFILES_STORE_INMEMORY)) {
      profileStoreProvider = new InMemoryStoreProvider(config.getInt(ExecConstants.PROFILES_STORE_CAPACITY));
      logger.info("Upto {} latest query profiles will be retained in-memory", config.getInt(ExecConstants.PROFILES_STORE_CAPACITY));
    } else {
      profileStoreProvider = storeProvider;
    }

    engine = new ServiceEngine(manager, context, allowPortHunting, isDistributedMode);

    stateManager = new DrillbitStateManager(DrillbitState.STARTUP);
    logger.info("Construction completed ({} ms).", w.elapsed(TimeUnit.MILLISECONDS));
  }

  public int getUserPort() {
    return engine.getUserPort();
  }

  public int getWebServerPort() {
    return webServer.getPort();
  }

  @VisibleForTesting
  public WorkManager getManager() { return manager; }

  @VisibleForTesting
  public WebServer getWebServer() { return webServer; }

  public void run() throws Exception {
    final Stopwatch w = Stopwatch.createStarted();
    logger.debug("Startup begun.");
    gracefulShutdownThread = new GracefulShutdownThread(this, new StackTrace());
    coord.start(10000);
    stateManager.setState(DrillbitState.ONLINE);
    storeProvider.start();
    if (profileStoreProvider != storeProvider) {
      profileStoreProvider.start();
    }
    DrillbitEndpoint md = engine.start();
    manager.start(md, engine.getController(), engine.getDataConnectionCreator(), coord, storeProvider, profileStoreProvider);
    final DrillbitContext drillbitContext = manager.getContext();
    storageRegistry = drillbitContext.getStorage();
    storageRegistry.init();
    drillbitContext.getOptionManager().init();
    javaPropertiesToSystemOptions();
    manager.getContext().getRemoteFunctionRegistry().init(context.getConfig(), storeProvider, coord);
    webServer.start();
    //Discovering HTTP port (in case of port hunting)
    if (webServer.isRunning()) {
      int httpPort = getWebServerPort();
      md = md.toBuilder().setHttpPort(httpPort).build();
    }
    registrationHandle = coord.register(md);
    // Must start the RM after the above since it needs to read system options.
    drillbitContext.startRM();

    shutdownHook = new ShutdownThread(this, new StackTrace());
    Runtime.getRuntime().addShutdownHook(shutdownHook);
    gracefulShutdownThread.start();
    logger.info("Startup completed ({} ms).", w.elapsed(TimeUnit.MILLISECONDS));
  }

  /**
   * Wait uninterruptibly
   */
  private void waitForGracePeriod() {
    ExtendedLatch exitLatch = new ExtendedLatch();
    exitLatch.awaitUninterruptibly(gracePeriod);
  }

  private void updateState(State state) {
    if (registrationHandle != null) {
      coord.update(registrationHandle, state);
    }
  }

  public void shutdown() {
    this.close();
  }

  /**
   * The drillbit is moved into Quiescent state and the drillbit waits for grace
   * period amount of time. Then drillbit moves into draining state and waits
   * for all the queries and fragments to complete.
   */
  @Override
  public synchronized void close() {
    if (!stateManager.getState().equals(DrillbitState.ONLINE)) {
      return;
    }
    final Stopwatch w = Stopwatch.createStarted();
    logger.debug("Shutdown begun.");
    // We don't really want for Drillbits to pile up in memory, so the hook should be removed
    // It might be better to use PhantomReferences to cleanup as soon as Drillbit becomes
    // unreachable, however current approach seems to be good enough.
    Thread shutdownHook = this.shutdownHook;
    if (shutdownHook != null && Thread.currentThread() != shutdownHook) {
      try {
        Runtime.getRuntime().removeShutdownHook(shutdownHook);
      } catch (IllegalArgumentException e) {
        // If shutdown is in progress, just ignore the removal
      }
    }
    updateState(State.QUIESCENT);
    stateManager.setState(DrillbitState.GRACE);
    waitForGracePeriod();
    stateManager.setState(DrillbitState.DRAINING);
    // wait for all the in-flight queries to finish
    manager.waitToExit(forcefulShutdown);
    //safe to exit
    updateState(State.OFFLINE);
    stateManager.setState(DrillbitState.OFFLINE);
    if (quiescentMode) {
      return;
    }
    if (coord != null && registrationHandle != null) {
      coord.unregister(registrationHandle);
    }
    try {
      Thread.sleep(context.getConfig().getInt(ExecConstants.ZK_REFRESH) * 2);
    } catch (final InterruptedException e) {
      logger.warn("Interrupted while sleeping during coordination deregistration.");

      // Preserve evidence that the interruption occurred so that code higher up on the call stack can learn of the
      // interruption and respond to it if it wants to.
      Thread.currentThread().interrupt();
    }

    try {
      AutoCloseables.close(
          webServer,
          engine,
          storeProvider,
          coord,
          manager,
          storageRegistry,
          context);

      //Closing the profile store provider if distinct
      if (storeProvider != profileStoreProvider) {
        AutoCloseables.close(profileStoreProvider);
      }
    } catch(Exception e) {
      logger.warn("Failure on close()", e);
    }

    logger.info("Shutdown completed ({} ms).", w.elapsed(TimeUnit.MILLISECONDS) );
    stateManager.setState(DrillbitState.SHUTDOWN);
    // Interrupt GracefulShutdownThread since Drillbit close is not called from it.
    if (interruptPollShutdown) {
      gracefulShutdownThread.interrupt();
    }
  }

  private void javaPropertiesToSystemOptions() {
    // get the system options property
    final String allSystemProps = System.getProperty(SYSTEM_OPTIONS_NAME);
    if ((allSystemProps == null) || allSystemProps.isEmpty()) {
      return;
    }

    final SystemOptionManager optionManager = getContext().getOptionManager();

    // parse out the properties, validate, and then set them
    final String[] systemProps = allSystemProps.split(",");
    for (final String systemProp : systemProps) {
      final String[] keyValue = systemProp.split("=");
      if (keyValue.length != 2) {
        throwInvalidSystemOption(systemProp, "does not contain a key=value assignment");
      }

      final String optionName = keyValue[0].trim();
      if (optionName.isEmpty()) {
        throwInvalidSystemOption(systemProp, "does not contain a key before the assignment");
      }

      final String optionString = stripQuotes(keyValue[1].trim(), systemProp);
      if (optionString.isEmpty()) {
        throwInvalidSystemOption(systemProp, "does not contain a value after the assignment");
      }

      final OptionValue defaultValue = optionManager.getOption(optionName);

      if (defaultValue == null) {
        throwInvalidSystemOption(systemProp, "does not specify a valid option name");
      }

      if (!defaultValue.accessibleScopes.inScopeOf(OptionScope.SYSTEM)) {
        throwInvalidSystemOption(systemProp, "does not specify a SYSTEM option ");
      }

      optionManager.setLocalOption(defaultValue.kind, optionName, optionString);
    }
  }

  /**
   * Polls for graceful file to check if graceful shutdown is triggered from the script.
   */
  private static class GracefulShutdownThread extends Thread {

    private static final String DRILL_HOME = "DRILL_HOME";
    private static final String GRACEFUL_SIGFILE = "GRACEFUL_SIGFILE";
    private static final String NOT_SUPPORTED_MESSAGE = "Graceful shutdown from command line will not be supported.";

    private final Drillbit drillbit;
    private final StackTrace stackTrace;

    GracefulShutdownThread(Drillbit drillbit, StackTrace stackTrace) {
      this.drillbit = drillbit;
      this.stackTrace = stackTrace;

      setName("Drillbit-Graceful-Shutdown#" + getName());
    }

    @Override
    public void run () {
      try {
        pollShutdown();
      } catch (InterruptedException e) {
        drillbit.interruptPollShutdown = false;
        logger.debug("Graceful Shutdown thread was interrupted", e);
      } catch (IOException e) {
        throw new RuntimeException("Exception while polling for graceful shutdown\n" + stackTrace, e);
      }
    }

    /**
     * Poll for the graceful file, if the file is found or modified, close the Drillbit.
     * In case if the {@link #DRILL_HOME} or {@link #GRACEFUL_SIGFILE} environment variables are not set,
     * graceful shutdown will not be supported from the command line.
     */
    private void pollShutdown() throws IOException, InterruptedException {
      Path drillHomePath = getDrillHomePath();
      String gracefulFile = System.getenv(GRACEFUL_SIGFILE);

      if (!areShutdownArgsValid(drillHomePath, gracefulFile)) {
        return;
      }

      while (true) {
        try (WatchService watchService = Objects.requireNonNull(drillHomePath).getFileSystem().newWatchService()) {
          drillHomePath.register(watchService, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_MODIFY);
          while (true) {
            WatchKey watchKey = watchService.take();
            for (WatchEvent<?> event : watchKey.pollEvents()) {
              if (StandardWatchEventKinds.OVERFLOW != event.kind()) {
                Path changedPath = (Path) event.context();
                if (changedPath != null && changedPath.endsWith(gracefulFile)) {
                  drillbit.interruptPollShutdown = false;
                  drillbit.close();
                  return;
                }
              }
            }

            if (!watchKey.reset()) {
              logger.debug("Watch key is no longer valid, attempting to re-register Drill home path in Watch Service");
              break;
            }
          }
        }
      }
    }

    private Path getDrillHomePath() {
      String drillHome = System.getenv(DRILL_HOME);
      try {
        return drillHome == null ? null : Paths.get(drillHome);
      } catch (InvalidPathException e) {
        logger.warn("Unable to construct {} path [{}]: {}.", DRILL_HOME, drillHome, e.getMessage());
        logger.debug("Invalid {} path", DRILL_HOME, e);
        return null;
      }
    }

    private boolean areShutdownArgsValid(Path drillHomePath, String gracefulFile) {
      StringBuilder builder = new StringBuilder();
      if (drillHomePath == null) {
        builder.append(DRILL_HOME).append(" is unset or invalid.");
      } else {
        if (!Files.exists(drillHomePath)) {
          builder.append(DRILL_HOME).append("path [").append(drillHomePath).append("] does not exist.");
        }
      }

      if (gracefulFile == null) {
        if (builder.length() != 0) {
          builder.append(" ");
        }
        builder.append(GRACEFUL_SIGFILE).append(" is unset.");
      }

      if (builder.length() != 0) {
        logger.warn("{}. {}", NOT_SUPPORTED_MESSAGE, builder.toString());
        return false;
      }

      return true;
    }
  }

  /**
   * Shutdown hook for Drillbit. Closes the drillbit, and reports on errors that
   * occur during closure, as well as the location the drillbit was started from.
   */
  private static class ShutdownThread extends Thread {
    private final static AtomicInteger idCounter = new AtomicInteger(0);
    private final Drillbit drillbit;
    private final StackTrace stackTrace;

    /**
     * Constructor.
     *
     * @param drillbit the drillbit to close down
     * @param stackTrace the stack trace from where the Drillbit was started;
     *   use new StackTrace() to generate this
     */
    ShutdownThread(final Drillbit drillbit, final StackTrace stackTrace) {
      this.drillbit = drillbit;
      this.stackTrace = stackTrace;
      /*
       * TODO should we try to determine a test class name?
       * See https://blogs.oracle.com/tor/entry/how_to_determine_the_junit
       */

      setName("Drillbit-ShutdownHook#" + idCounter.getAndIncrement());
    }

    @Override
    public void run() {
      if (FailureUtils.hadUnrecoverableFailure()) {
        // We cannot close drill cleanly in this case.
        return;
      }

      logger.info("Received shutdown request.");
      try {
        /*
         * We can avoid metrics deregistration concurrency issues by only closing
         * one drillbit at a time. To enforce that, we synchronize on a convenient
         * singleton object.
         */
        synchronized(idCounter) {
          drillbit.close();
        }
      } catch(final Exception e) {
        throw new RuntimeException("Caught exception closing Drillbit started from\n" + stackTrace, e);
      }
    }
  }

  public DrillbitContext getContext() {
    return manager.getContext();
  }

  @VisibleForTesting
  public GracefulShutdownThread getGracefulShutdownThread() {
    return gracefulShutdownThread;
  }

  public static void main(final String[] cli) throws DrillbitStartupException {
    final StartupOptions options = StartupOptions.parse(cli);
    start(options);
  }

  public static Drillbit start(final StartupOptions options) throws DrillbitStartupException {
    return start(DrillConfig.create(options.getConfigLocation()), SystemOptionManager.createDefaultOptionDefinitions(), null);
  }

  public static Drillbit start(final DrillConfig config) throws DrillbitStartupException {
    return start(config, SystemOptionManager.createDefaultOptionDefinitions(), null);
  }

  public static Drillbit start(final DrillConfig config, final RemoteServiceSet remoteServiceSet) throws DrillbitStartupException {
    return start(config, SystemOptionManager.createDefaultOptionDefinitions(), remoteServiceSet);
  }

  @VisibleForTesting
  public static Drillbit start(final DrillConfig config, final CaseInsensitiveMap<OptionDefinition> validators,
                               final RemoteServiceSet remoteServiceSet)
      throws DrillbitStartupException {
    logger.debug("Starting new Drillbit.");
    // TODO: allow passing as a parameter
    ScanResult classpathScan = ClassPathScanner.fromPrescan(config);
    Drillbit bit;
    try {
      bit = new Drillbit(config, validators, remoteServiceSet, classpathScan);
    } catch (final Exception ex) {
      if (ex instanceof DrillbitStartupException) {
        throw (DrillbitStartupException) ex;
      } else {
        throw new DrillbitStartupException("Failure while initializing values in Drillbit.", ex);
      }
    }

    try {
      bit.run();
    } catch (final Exception e) {
      logger.error("Failure during initial startup of Drillbit.", e);
      bit.close();
      throw new DrillbitStartupException("Failure during initial startup of Drillbit.", e);
    }
    logger.debug("Started new Drillbit.");
    return bit;
  }

  private static void throwInvalidSystemOption(final String systemProp, final String errorMessage) {
    throw new IllegalStateException("Property \"" + SYSTEM_OPTIONS_NAME + "\" part \"" + systemProp
        + "\" " + errorMessage + ".");
  }

  private static String stripQuotes(final String s, final String systemProp) {
    if (s.isEmpty()) {
      return s;
    }

    final char cFirst = s.charAt(0);
    final char cLast = s.charAt(s.length() - 1);
    if ((cFirst == '"') || (cFirst == '\'')) {
      if (cLast != cFirst) {
        throwInvalidSystemOption(systemProp, "quoted value does not have closing quote");
      }

      return s.substring(1, s.length() - 2); // strip the quotes
    }

    if ((cLast == '"') || (cLast == '\'')) {
      throwInvalidSystemOption(systemProp, "value has unbalanced closing quote");
    }

    // return as-is
    return s;
  }
}
