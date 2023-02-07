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
package org.apache.drill.yarn.appMaster;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.drill.yarn.core.ContainerRequestSpec;
import org.apache.drill.yarn.core.DfsFacade;
import org.apache.drill.yarn.core.DfsFacade.DfsFacadeException;
import org.apache.drill.yarn.core.DoYUtil;
import org.apache.drill.yarn.core.DoyConfigException;
import org.apache.drill.yarn.core.DrillOnYarnConfig;
import org.apache.drill.yarn.core.LaunchSpec;
import org.apache.drill.yarn.appMaster.http.AMSecurityManagerImpl;
import org.apache.drill.yarn.core.ClusterDef;
import org.apache.drill.yarn.zk.ZKClusterCoordinatorDriver;
import org.apache.drill.yarn.zk.ZKRegistry;
import org.apache.hadoop.yarn.api.ApplicationConstants;
import org.apache.hadoop.yarn.api.records.LocalResource;

import com.typesafe.config.Config;

/**
 * Builds a controller for a cluster of Drillbits. The AM is designed to be
 * mostly generic; only this class contains knowledge that the tasks being
 * managed are drillbits. This design ensures that we can add other Drill
 * components in the future without the need to make major changes to the AM
 * logic.
 * <p>
 * The controller consists of a generic dispatcher and cluster controller, along
 * with a Drill-specific scheduler and task launch specification. Drill also
 * includes an interface to ZooKeeper to monitor Drillbits.
 * <p>
 * The AM is launched by YARN. All it knows is what is in its launch environment
 * or configuration files. The client must set up all the information that the
 * AM needs. Static information appears in configuration files. But, dynamic
 * information (or that which is inconvenient to repeat in configuration files)
 * must arrive in environment variables. See {@link DrillOnYarnConfig} for more
 * information.
 */

public class DrillControllerFactory implements ControllerFactory {
  private static final Log LOG = LogFactory.getLog(DrillControllerFactory.class);
  private Config config = DrillOnYarnConfig.config();
  private String drillArchivePath;
  private String siteArchivePath;
  private boolean localized;

  @Override
  public Dispatcher build() throws ControllerFactoryException {
    LOG.info(
        "Initializing AM for " + config.getString(DrillOnYarnConfig.APP_NAME));
    Dispatcher dispatcher;
    try {
      Map<String, LocalResource> resources = prepareResources();

      TaskSpec taskSpec = buildDrillTaskSpec(resources);

      // Prepare dispatcher

      int timerPeriodMs = config.getInt(DrillOnYarnConfig.AM_TICK_PERIOD_MS);
      dispatcher = new Dispatcher(timerPeriodMs);
      int pollPeriodMs = config.getInt(DrillOnYarnConfig.AM_POLL_PERIOD_MS);
      AMYarnFacadeImpl yarn = new AMYarnFacadeImpl(pollPeriodMs);
      dispatcher.setYarn(yarn);
      dispatcher.getController()
          .setMaxRetries(config.getInt(DrillOnYarnConfig.DRILLBIT_MAX_RETRIES));

      int requestTimeoutSecs = DrillOnYarnConfig.config().getInt( DrillOnYarnConfig.DRILLBIT_REQUEST_TIMEOUT_SEC);
      int maxExtraNodes = DrillOnYarnConfig.config().getInt(DrillOnYarnConfig.DRILLBIT_MAX_EXTRA_NODES);

      // Assume basic scheduler for now.
      ClusterDef.ClusterGroup pool = ClusterDef.getCluster(config, 0);
      Scheduler testGroup = new DrillbitScheduler(pool.getName(), taskSpec,
          pool.getCount(), requestTimeoutSecs, maxExtraNodes);
      dispatcher.getController().registerScheduler(testGroup);
      pool.modifyTaskSpec(taskSpec);

      // ZooKeeper setup

      buildZooKeeper(config, dispatcher);
    } catch (YarnFacadeException | DoyConfigException e) {
      throw new ControllerFactoryException("Drill AM intitialization failed", e);
    }

    // Tracking Url
    // TODO: HTTPS support

    dispatcher.setHttpPort(config.getInt(DrillOnYarnConfig.HTTP_PORT));
    String trackingUrl = null;
    if (config.getBoolean(DrillOnYarnConfig.HTTP_ENABLED)) {
      trackingUrl = "http://<host>:<port>/redirect";
      dispatcher.setTrackingUrl(trackingUrl);
    }

    // Enable/disable check for auto shutdown when no nodes are running.

    dispatcher.getController().enableFailureCheck(
        config.getBoolean(DrillOnYarnConfig.AM_ENABLE_AUTO_SHUTDOWN));

    // Define the security manager

    AMSecurityManagerImpl.setup();

    return dispatcher;
  }

  /**
   * Prepare the files ("resources" in YARN terminology) that YARN should
   * download ("localize") for the Drillbit. We need both the Drill software and
   * the user's site-specific configuration.
   *
   * @return resources
   * @throws YarnFacadeException
   */

  private Map<String, LocalResource> prepareResources()
      throws YarnFacadeException {
    try {
      DfsFacade dfs = new DfsFacade(config);
      localized = dfs.isLocalized();
      if (!localized) {
        return null;
      }
      dfs.connect();
      Map<String, LocalResource> resources = new HashMap<>();
      DrillOnYarnConfig drillConfig = DrillOnYarnConfig.instance();

      // Localize the Drill archive.

      drillArchivePath = drillConfig.getDrillArchiveDfsPath();
      DfsFacade.Localizer localizer = new DfsFacade.Localizer(dfs,
          drillArchivePath);
      String key = config.getString(DrillOnYarnConfig.DRILL_ARCHIVE_KEY);
      localizer.defineResources(resources, key);
      LOG.info("Localizing " + drillArchivePath + " with key \"" + key + "\"");

      // Localize the site archive, if any.

      siteArchivePath = drillConfig.getSiteArchiveDfsPath();
      if (siteArchivePath != null) {
        localizer = new DfsFacade.Localizer(dfs, siteArchivePath);
        key = config.getString(DrillOnYarnConfig.SITE_ARCHIVE_KEY);
        localizer.defineResources(resources, key);
        LOG.info("Localizing " + siteArchivePath + " with key \"" + key + "\"");
      }
      return resources;
    } catch (DfsFacadeException e) {
      throw new YarnFacadeException(
          "Failed to get DFS status for Drill archive", e);
    }
  }

  /**
   * Constructs the Drill launch command. The launch uses the YARN-specific
   * yarn-drillbit.sh script, setting up the required input environment
   * variables.
   * <p>
   * This is an exercise in getting many details just right. The code here sets
   * the environment variables required by (and documented in) yarn-drillbit.sh.
   * The easiest way to understand this code is to insert an "echo" statement in
   * drill-bit.sh to echo the launch command there. Then, look in YARN's NM
   * private container directory for the launch_container.sh script to see the
   * command generated by the following code. Compare the two to validate that
   * the code does the right thing.
   * <p>
   * This class is very Linux-specific. The usual adjustments must be made to
   * adapt it to Windows.
   *
   * @param resources the means to set up the required environment variables
   * @return task specification
   * @throws DoyConfigException
   */

  private TaskSpec buildDrillTaskSpec(Map<String, LocalResource> resources)
      throws DoyConfigException {
    DrillOnYarnConfig doyConfig = DrillOnYarnConfig.instance();

    // Drillbit launch description

    ContainerRequestSpec containerSpec = new ContainerRequestSpec();
    containerSpec.memoryMb = config.getInt(DrillOnYarnConfig.DRILLBIT_MEMORY);
    containerSpec.vCores = config.getInt(DrillOnYarnConfig.DRILLBIT_VCORES);
    containerSpec.disks = config.getDouble(DrillOnYarnConfig.DRILLBIT_DISKS);

    LaunchSpec drillbitSpec = new LaunchSpec();

    // The drill home location is either a non-localized location,
    // or, more typically, the expanded Drill directory under the
    // container's working directory. When the localized directory,
    // we rely on the fact that the current working directory is
    // set to the container directory, so we just need the name
    // of the Drill folder under the cwd.

    String drillHome = doyConfig.getRemoteDrillHome();
    drillbitSpec.env.put("DRILL_HOME", drillHome);
    LOG.trace("Drillbit DRILL_HOME: " + drillHome);

    // Heap memory

    addIfSet(drillbitSpec, DrillOnYarnConfig.DRILLBIT_HEAP, "DRILL_HEAP");

    // Direct memory

    addIfSet(drillbitSpec, DrillOnYarnConfig.DRILLBIT_DIRECT_MEM,
        "DRILL_MAX_DIRECT_MEMORY");

    // Code cache

    addIfSet(drillbitSpec, DrillOnYarnConfig.DRILLBIT_CODE_CACHE,
        "DRILLBIT_CODE_CACHE_SIZE");

    // Any additional VM arguments from the config file.

    addIfSet(drillbitSpec, DrillOnYarnConfig.DRILLBIT_VM_ARGS,
        "DRILL_JVM_OPTS");

    // Any user-specified library path

    addIfSet(drillbitSpec, DrillOnYarnConfig.JAVA_LIB_PATH,
        DrillOnYarnConfig.DOY_LIBPATH_ENV_VAR);

    // Drill logs.
    // Relies on the LOG_DIR_EXPANSION_VAR marker which is replaced by
    // the container log directory.

    if (!config.getBoolean(DrillOnYarnConfig.DISABLE_YARN_LOGS)) {
      drillbitSpec.env.put("DRILL_YARN_LOG_DIR",
          ApplicationConstants.LOG_DIR_EXPANSION_VAR);
    }

    // Debug option.

    if (config.getBoolean(DrillOnYarnConfig.DRILLBIT_DEBUG_LAUNCH)) {
      drillbitSpec.env.put(DrillOnYarnConfig.DRILL_DEBUG_ENV_VAR, "1");
    }

    // Hadoop home should be set in drill-env.sh since it is needed
    // for client launch as well as the AM.

    // addIfSet( drillbitSpec, DrillOnYarnConfig.HADOOP_HOME, "HADOOP_HOME" );

    // Garbage collection (gc) logging. In drillbit.sh logging can be
    // configured to go anywhere. In YARN, all logs go to the YARN log
    // directory; the gc log file is always called "gc.log".

    if (config.getBoolean(DrillOnYarnConfig.DRILLBIT_LOG_GC)) {
      drillbitSpec.env.put("ENABLE_GC_LOG", "1");
    }

    // Class path additions.

    addIfSet(drillbitSpec, DrillOnYarnConfig.DRILLBIT_PREFIX_CLASSPATH,
        DrillOnYarnConfig.DRILL_CLASSPATH_PREFIX_ENV_VAR);
    addIfSet(drillbitSpec, DrillOnYarnConfig.DRILLBIT_CLASSPATH,
        DrillOnYarnConfig.DRILL_CLASSPATH_ENV_VAR);

    // Drill-config.sh has specific entries for Hadoop and Hbase. To prevent
    // an endless number of such one-off cases, we add a general extension
    // class path. But, we retain Hadoop and Hbase for backward compatibility.

    addIfSet(drillbitSpec, DrillOnYarnConfig.DRILLBIT_EXTN_CLASSPATH,
        "EXTN_CLASSPATH");
    addIfSet(drillbitSpec, DrillOnYarnConfig.HADOOP_CLASSPATH,
        "DRILL_HADOOP_CLASSPATH");
    addIfSet(drillbitSpec, DrillOnYarnConfig.HBASE_CLASSPATH,
        "DRILL_HBASE_CLASSPATH");

    // Note that there is no equivalent of niceness for YARN: YARN controls
    // the niceness of its child processes.

    // Drillbit launch script under YARN
    // Here we can use DRILL_HOME because all env vars are set before
    // issuing this command.

    drillbitSpec.command = "$DRILL_HOME/bin/yarn-drillbit.sh";

    // Configuration (site directory), if given.

    String siteDirPath = doyConfig.getRemoteSiteDir();
    if (siteDirPath != null) {
      drillbitSpec.cmdArgs.add("--site");
      drillbitSpec.cmdArgs.add(siteDirPath);
    }

    // Localized resources

    if (resources != null) {
      drillbitSpec.resources.putAll(resources);
    }

    // Container definition.

    TaskSpec taskSpec = new TaskSpec();
    taskSpec.name = "Drillbit";
    taskSpec.containerSpec = containerSpec;
    taskSpec.launchSpec = drillbitSpec;
    taskSpec.maxRetries = config.getInt(DrillOnYarnConfig.DRILLBIT_MAX_RETRIES);
    return taskSpec;
  }

  /**
   * Utility method to create an environment variable in the process launch
   * specification if a given Drill-on-YARN configuration variable is set,
   * copying the config value to the environment variable.
   *
   * @param spec launch specification
   * @param configParam config value
   * @param envVar environment variable
   */

  public void addIfSet(LaunchSpec spec, String configParam, String envVar) {
    String value = config.getString(configParam);
    if (!DoYUtil.isBlank(value)) {
      spec.env.put(envVar, value);
    }
  }

  public static class ZKRegistryAddOn implements DispatcherAddOn {
    ZKRegistry zkRegistry;

    public ZKRegistryAddOn(ZKRegistry zkRegistry) {
      this.zkRegistry = zkRegistry;
    }

    @Override
    public void start(ClusterController controller) {
      zkRegistry.start(controller);
    }

    @Override
    public void finish(ClusterController controller) {
      zkRegistry.finish(controller);
    }
  }

  /**
   * Create the Drill-on-YARN version of the ZooKeeper cluster coordinator.
   * Compared to the Drill version, this one takes its parameters via a builder
   * pattern in the form of the cluster coordinator driver.
   *
   * @param config used to build a Drill-on-YARN configuration
   * @param dispatcher dispatches different events to the cluster controller
   */

  private void buildZooKeeper(Config config, Dispatcher dispatcher) {
    String zkConnect = config.getString(DrillOnYarnConfig.ZK_CONNECT);
    String zkRoot = config.getString(DrillOnYarnConfig.ZK_ROOT);
    String clusterId = config.getString(DrillOnYarnConfig.CLUSTER_ID);
    int failureTimeoutMs = config
        .getInt(DrillOnYarnConfig.ZK_FAILURE_TIMEOUT_MS);
    int retryCount = config.getInt(DrillOnYarnConfig.ZK_RETRY_COUNT);
    int retryDelayMs = config.getInt(DrillOnYarnConfig.ZK_RETRY_DELAY_MS);
    int userPort = config.getInt(DrillOnYarnConfig.DRILLBIT_USER_PORT);
    int bitPort = config.getInt(DrillOnYarnConfig.DRILLBIT_BIT_PORT);
    ZKClusterCoordinatorDriver driver = new ZKClusterCoordinatorDriver()
        .setConnect(zkConnect, zkRoot, clusterId)
        .setFailureTimoutMs(failureTimeoutMs)
        .setRetryCount(retryCount)
        .setRetryDelayMs(retryDelayMs)
        .setPorts(userPort, bitPort, bitPort + 1);
    ZKRegistry zkRegistry = new ZKRegistry(driver);
    dispatcher.registerAddOn(new ZKRegistryAddOn(zkRegistry));

    // The ZK driver is started and stopped in conjunction with the
    // controller lifecycle.

    dispatcher.getController().registerLifecycleListener(zkRegistry);

    // The ZK driver also handles registering the AM for the cluster.

    dispatcher.setAMRegistrar(driver);

    // The UI needs access to ZK to report unmanaged drillbits. We use
    // a property to avoid unnecessary code dependencies.

    dispatcher.getController().setProperty(ZKRegistry.CONTROLLER_PROPERTY,
        zkRegistry);
  }
}
