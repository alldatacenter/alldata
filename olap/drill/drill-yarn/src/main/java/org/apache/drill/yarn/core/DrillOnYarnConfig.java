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
package org.apache.drill.yarn.core;

import java.io.File;
import java.io.PrintStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.apache.drill.common.config.ConfigConstants;
import org.apache.drill.common.config.DrillConfig;
import org.apache.drill.common.scanner.ClassPathScanner;
import org.apache.drill.common.scanner.persistence.ScanResult;
import org.apache.drill.exec.ExecConstants;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigException;
import com.typesafe.config.ConfigFactory;

/**
 * Configuration used within the Drill-on-YARN code. Configuration comes from four
 * sources (in order of precedence):
 * <ol>
 * <li>System properties</li>
 * <li>$SITE_DIR/drill-on-yarn.conf</li>
 * <li>Distribution-specific properties in $SITE_HOME/conf/doy-distrib.conf</li>
 * <li>Drill-on-YARN defaults in drill-on-yarn-defaults.conf. (Which should be
 * disjoint from the Drill settings.)</li>
 * <li>Drill properties (via the Drill override system)</li>
 * </ol>
 * <p>
 * Defines constants for each property, including some defined in Drill. This provides
 * a uniform property access interface even if some properties migrate between DoY and
 * Drill proper.
 */

public class DrillOnYarnConfig {
  public static final String DEFAULTS_FILE_NAME = "drill-on-yarn-defaults.conf";
  public static final String DISTRIB_FILE_NAME = "doy-distrib.conf";
  public static final String CONFIG_FILE_NAME = "drill-on-yarn.conf";

  public static final String DRILL_ON_YARN_PARENT = "drill.yarn";
  public static final String DOY_CLIENT_PARENT = append(DRILL_ON_YARN_PARENT, "client");
  public static final String DOY_AM_PARENT = append(DRILL_ON_YARN_PARENT, "am");
  public static final String DOY_DRILLBIT_PARENT = append(DRILL_ON_YARN_PARENT, "drillbit");
  public static final String FILES_PARENT = append(DRILL_ON_YARN_PARENT, "drill-install");
  public static final String DFS_PARENT = append(DRILL_ON_YARN_PARENT, "dfs");
  public static final String HTTP_PARENT = append(DRILL_ON_YARN_PARENT, "http");
  public static final String YARN_PARENT = append(DRILL_ON_YARN_PARENT, "yarn");
  public static final String HADOOP_PARENT = append(DRILL_ON_YARN_PARENT, "hadoop");
  public static final String CLIENT_PARENT = append(DRILL_ON_YARN_PARENT, "client");

  public static final String APP_NAME = append(DRILL_ON_YARN_PARENT, "app-name");
  public static final String CLUSTER_ID = ExecConstants.SERVICE_NAME;

  public static final String DFS_CONNECTION = append(DFS_PARENT, "connection");
  public static final String DFS_APP_DIR = append(DFS_PARENT, "app-dir");

  public static final String YARN_QUEUE = append(YARN_PARENT, "queue");
  public static final String YARN_PRIORITY = append(YARN_PARENT, "priority");

  public static final String DRILL_ARCHIVE_PATH = append(FILES_PARENT, "client-path");
  public static final String DRILL_DIR_NAME = append(FILES_PARENT, "dir-name");

  /**
   * Key used for the Drill archive file in the AM launch config.
   */

  public static final String DRILL_ARCHIVE_KEY = append(FILES_PARENT, "drill-key");
  public static final String SITE_ARCHIVE_KEY = append(FILES_PARENT, "site-key");
  public static final String LOCALIZE_DRILL = append(FILES_PARENT, "localize");
  public static final String CONF_AS_SITE = append(FILES_PARENT, "conf-as-site");
  public static final String DRILL_HOME = append(FILES_PARENT, "drill-home");
  public static final String SITE_DIR = append(FILES_PARENT, "site-dir");
  public static final String JAVA_LIB_PATH = append(FILES_PARENT, "library-path");

  public static final String HADOOP_HOME = append(HADOOP_PARENT, "home");
  public static final String HADOOP_CLASSPATH = append(HADOOP_PARENT, "class-path");
  public static final String HBASE_CLASSPATH = append(HADOOP_PARENT, "hbase-class-path");

  public static final String MEMORY_KEY = "memory-mb";
  public static final String VCORES_KEY = "vcores";
  public static final String DISKS_KEY = "disks";
  public static final String VM_ARGS_KEY = "vm-args";
  public static final String HEAP_KEY = "heap";

  public static final String AM_MEMORY = append(DOY_AM_PARENT, MEMORY_KEY);
  public static final String AM_VCORES = append(DOY_AM_PARENT, VCORES_KEY);
  public static final String AM_DISKS = append(DOY_AM_PARENT, DISKS_KEY);
  public static final String AM_NODE_LABEL_EXPR = append(DOY_AM_PARENT, "node-label-expr");
  public static final String AM_HEAP = append(DOY_AM_PARENT, HEAP_KEY);
  public static final String AM_VM_ARGS = append(DOY_AM_PARENT, VM_ARGS_KEY);
  public static final String AM_POLL_PERIOD_MS = append(DOY_AM_PARENT, "poll-ms");
  public static final String AM_TICK_PERIOD_MS = append(DOY_AM_PARENT, "tick-ms");
  public static final String AM_PREFIX_CLASSPATH = append(DOY_AM_PARENT, "prefix-class-path");
  public static final String AM_CLASSPATH = append(DOY_AM_PARENT, "class-path");
  public static final String AM_DEBUG_LAUNCH = append(DOY_AM_PARENT, "debug-launch");
  public static final String AM_ENABLE_AUTO_SHUTDOWN = append(DOY_AM_PARENT, "auto-shutdown");

  public static final String DRILLBIT_MEMORY = append(DOY_DRILLBIT_PARENT, MEMORY_KEY);
  public static final String DRILLBIT_VCORES = append(DOY_DRILLBIT_PARENT, VCORES_KEY);
  public static final String DRILLBIT_DISKS = append(DOY_DRILLBIT_PARENT, DISKS_KEY);
  public static final String DRILLBIT_VM_ARGS = append(DOY_DRILLBIT_PARENT, VM_ARGS_KEY);
  public static final String DRILLBIT_HEAP = append(DOY_DRILLBIT_PARENT, HEAP_KEY);
  public static final String DRILLBIT_DIRECT_MEM = append(DOY_DRILLBIT_PARENT, "max-direct-memory");
  public static final String DRILLBIT_CODE_CACHE = append(DOY_DRILLBIT_PARENT, "code-cache");
  public static final String DRILLBIT_LOG_GC = append(DOY_DRILLBIT_PARENT, "log-gc");
  public static final String DRILLBIT_PREFIX_CLASSPATH = append( DOY_DRILLBIT_PARENT, "prefix-class-path");
  public static final String DRILLBIT_EXTN_CLASSPATH = append( DOY_DRILLBIT_PARENT, "extn-class-path");
  public static final String DRILLBIT_CLASSPATH = append(DOY_DRILLBIT_PARENT, "class-path");
  public static final String DRILLBIT_MAX_RETRIES = append(DOY_DRILLBIT_PARENT, "max-retries");
  public static final String DRILLBIT_DEBUG_LAUNCH = append(DOY_DRILLBIT_PARENT, "debug-launch");
  public static final String DRILLBIT_HTTP_PORT = ExecConstants.HTTP_PORT;
  public static final String DISABLE_YARN_LOGS = append(DOY_DRILLBIT_PARENT, "disable-yarn-logs");
  public static final String DRILLBIT_USER_PORT = ExecConstants.INITIAL_USER_PORT;
  public static final String DRILLBIT_BIT_PORT = ExecConstants.INITIAL_BIT_PORT;
  public static final String DRILLBIT_USE_HTTPS = ExecConstants.HTTP_ENABLE_SSL;
  public static final String DRILLBIT_MAX_EXTRA_NODES = append(DOY_DRILLBIT_PARENT, "max-extra-nodes");
  public static final String DRILLBIT_REQUEST_TIMEOUT_SEC = append(DOY_DRILLBIT_PARENT, "request-timeout-secs");

  public static final String ZK_CONNECT = ExecConstants.ZK_CONNECTION;
  public static final String ZK_ROOT = ExecConstants.ZK_ROOT;
  public static final String ZK_FAILURE_TIMEOUT_MS = ExecConstants.ZK_TIMEOUT;
  public static final String ZK_RETRY_COUNT = ExecConstants.ZK_RETRY_TIMES;
  public static final String ZK_RETRY_DELAY_MS = ExecConstants.ZK_RETRY_DELAY;

  // Names selected to be parallel to Drillbit HTTP config.

  public static final String HTTP_ENABLED = append(HTTP_PARENT, "enabled");
  public static final String HTTP_ENABLE_SSL = append(HTTP_PARENT, "ssl-enabled");
  public static final String HTTP_PORT = append(HTTP_PARENT, "port");
  public static final String HTTP_AUTH_TYPE = append(HTTP_PARENT, "auth-type");
  public static final String HTTP_REST_KEY = append(HTTP_PARENT, "rest-key");
  public static final String HTTP_SESSION_MAX_IDLE_SECS = append(HTTP_PARENT, "session-max-idle-secs");
  public static final String HTTP_DOCS_LINK = append(HTTP_PARENT, "docs-link");
  public static final String HTTP_REFRESH_SECS = append(HTTP_PARENT, "refresh-secs");
  public static final String HTTP_USER_NAME = append(HTTP_PARENT, "user-name");
  public static final String HTTP_PASSWORD = append(HTTP_PARENT, "password");

  public static final String AUTH_TYPE_NONE = "none";
  public static final String AUTH_TYPE_DRILL = "drill";
  public static final String AUTH_TYPE_SIMPLE = "simple";

  public static final String CLIENT_POLL_SEC = append(CLIENT_PARENT, "poll-sec");
  public static final String CLIENT_START_WAIT_SEC = append(CLIENT_PARENT, "start-wait-sec");
  public static final String CLIENT_STOP_WAIT_SEC = append(CLIENT_PARENT, "stop-wait-sec");

  public static final String CLUSTERS = append(DRILL_ON_YARN_PARENT, "cluster");

  /**
   * Name of the subdirectory of the container directory that will hold
   * localized Drill distribution files. This name must be consistent between AM
   * launch request and AM launch, and between Drillbit launch request and
   * Drillbit launch. This name is fixed; there is no reason for the user to
   * change it as it is visible only in the YARN container environment.
   */

  public static String LOCAL_DIR_NAME = "drill";

  // Environment variables used to pass information from the Drill-on-YARN
  // Client to the AM, or from the AM to the Drillbit launch script.

  public static final String APP_ID_ENV_VAR = "DRILL_AM_APP_ID";
  public static final String DRILL_ARCHIVE_ENV_VAR = "DRILL_ARCHIVE";
  public static final String SITE_ARCHIVE_ENV_VAR = "SITE_ARCHIVE";
  public static final String DRILL_HOME_ENV_VAR = "DRILL_HOME";
  public static final String DRILL_SITE_ENV_VAR = "DRILL_CONF_DIR";
  public static final String AM_HEAP_ENV_VAR = "DRILL_AM_HEAP";
  public static final String AM_JAVA_OPTS_ENV_VAR = "DRILL_AM_JAVA_OPTS";
  public static final String DRILL_CLASSPATH_ENV_VAR = "DRILL_CLASSPATH";
  public static final String DRILL_CLASSPATH_PREFIX_ENV_VAR = "DRILL_CLASSPATH_PREFIX";
  public static final String DOY_LIBPATH_ENV_VAR = "DOY_JAVA_LIB_PATH";
  public static final String DRILL_DEBUG_ENV_VAR = "DRILL_DEBUG";

  /**
   * Special value for the DRILL_DIR_NAME parameter to indicate to use the base
   * name of the archive as the Drill home path.
   */

  private static final Object BASE_NAME_MARKER = "<base>";

  /**
   * The name of the Drill site archive stored in dfs. Since the archive is
   * created by the client as a temp file, it's local name has no meaning; we
   * use this standard name on dfs.
   */

  public static final String SITE_ARCHIVE_NAME = "site.tar.gz";

  protected static DrillOnYarnConfig instance;
  private File drillSite;
  private File drillHome;
  private static DrillConfig drillConfig;
  private Config config;
  private ScanResult classPathScan;

  public static String append(String parent, String key) {
    return parent + "." + key;
  }

  // Protected only to allow creating a test version of this class.

  protected DrillOnYarnConfig( ) {
  }

  public static DrillOnYarnConfig load() throws DoyConfigException {
    instance = new DrillOnYarnConfig();
    instance.doLoad(Thread.currentThread().getContextClassLoader());
    return instance;
  }

  /**
   * Load the config.
   * @param cl class loader to use for resource searches (except defaults).
   * Allows test to specify a specialized version.
   * <p>
   * Implemented in a way that allows unit testing. The parseUrl( ) methods
   * let us mock the files; the load( ) methods seem to not actually use the
   * provided class loader.
   *
   * @throws DoyConfigException
   */
  protected void doLoad(ClassLoader cl) throws DoyConfigException {
    Config drillConfig = loadDrillConfig();

    // Resolution order, larger numbers take precedence.
    // 1. Drill-on-YARN defaults.
    // File is at root of the package tree.

    URL url = DrillOnYarnConfig.class.getResource(DEFAULTS_FILE_NAME);
    if (url == null) {
      throw new IllegalStateException(
          "Drill-on-YARN defaults file is required: " + DEFAULTS_FILE_NAME);
    }
    config = ConfigFactory.parseURL(url).withFallback(drillConfig);

    // 2. Optional distribution-specific configuration-file.
    // (Lets a vendor, for example, specify the default DFS upload location
    // without tinkering with the user's own settings.

    url = cl.getResource(DISTRIB_FILE_NAME);
    if (url != null) {
      config = ConfigFactory.parseURL(url).withFallback(config);
    }

    // 3. User's Drill-on-YARN configuration.
    // Optional since defaults are fine & ZK comes from drill-override.conf.

    url = cl.getResource(CONFIG_FILE_NAME);
    if (url != null) {
      config = ConfigFactory.parseURL(url).withFallback(config);
    }

    // 4. System properties
    // Allows -Dfoo=bar on the command line.
    // But, note that substitutions are NOT allowed in system properties!

    config = ConfigFactory.systemProperties().withFallback(config);

    // Resolution allows ${foo.bar} syntax in values, but only for values
    // from config files, not from system properties.

    config = config.resolve();
  }

  private static Config loadDrillConfig() {
    drillConfig = DrillConfig
        .create(ConfigConstants.CONFIG_OVERRIDE_RESOURCE_PATHNAME);
    return drillConfig.resolve();
  }

  public DrillConfig getDrillConfig() {
    return drillConfig;
  }

  /**
   * Return Drill's class path scan. This is used only in the main thread during
   * initialization. Not needed by the client, so done in an unsynchronized,
   * lazy fashion.
   *
   * @return Drill's class path scan
   */

  public ScanResult getClassPathScan() {
    if (classPathScan == null) {
      classPathScan = ClassPathScanner.fromPrescan(drillConfig);
    }
    return classPathScan;
  }

  /**
   * Obtain Drill home from the DRILL_HOME environment variable set by
   * drill-config.sh, which is called from drill-on-yarn.sh. When debugging,
   * DRILL_HOME must be set in the environment.
   * <p>
   * This information is required only by the client to prepare for uploads to
   * DFS.
   *
   * @throws DoyConfigException
   */

  public void setClientPaths() throws DoyConfigException {
    setClientDrillHome();
    setSiteDir();
  }

  private void setClientDrillHome() throws DoyConfigException {
    // Try the environment variable that should have been
    // set in drill-on-yarn.sh (for the client) or in the
    // launch environment (for the AM.)

    String homeDir = getEnv(DRILL_HOME_ENV_VAR);

    // For ease in debugging, allow setting the Drill home in
    // drill-on-yarn.conf.
    // This setting is also used for a non-localized run.

    if (DoYUtil.isBlank(homeDir)) {
      homeDir = config.getString(DRILL_HOME);
    }
    if (DoYUtil.isBlank(homeDir)) {
      throw new DoyConfigException(
          "The DRILL_HOME environment variable must point to your Drill install.");
    }
    drillHome = new File(homeDir);
  }

  /**
   * All environment variable access goes through this function to allow unit
   * tests to replace this function to set test values. (The Java environment is
   * immutable, so it is not possible for unit tests to change the actual
   * environment.)
   *
   * @param key key to allow unit tests to replace this function
   * @return environment variable
   */

  protected String getEnv(String key) {
    return System.getenv(key);
  }

  /**
   * On both the client and the AM, the site directory is optional. If provided,
   * it was set with the --config (or --site) option to the script that launched
   * the client or AM. In both cases, the script sets the drill.yarn.siteDir
   * system property (and leaks the DRILL_HOME environment variable.)
   * <p>
   * For ease of debugging, if neither of those are set, this method uses the
   * location of the drill-on-yarn configuration file to infer the site
   * directory.
   * <p>
   * On the client, the site directory will be the "original" directory that
   * contains the user's "master" files. On the AM, the site directory is a
   * localized version of the client directory. Because of the way tar works,
   * both the client and AM site directories have the same name; though the path
   * to that name obviously differs.
   *
   * @throws DoyConfigException
   */

  private void setSiteDir() throws DoyConfigException {
    // The site directory is the one where the config file lives.
    // This should have been set in an environment variable by the launch
    // script.

    String sitePath = getEnv("DRILL_CONF_DIR");
    if (!DoYUtil.isBlank(sitePath)) {
      drillSite = new File(sitePath);
    } else {

      // Otherwise, let's guess it from the config file. This version assists
      // in debugging as it reduces setup steps.

      ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
      if (classLoader == null) {
        classLoader = DrillOnYarnConfig.class.getClassLoader();
      }

      URL url = classLoader.getResource(ConfigConstants.CONFIG_OVERRIDE_RESOURCE_PATHNAME);
      if (url == null) {
        throw new DoyConfigException(
            "Drill configuration file is missing: " + ConfigConstants.CONFIG_OVERRIDE_RESOURCE_PATHNAME);
      }
      File confFile;
      try {
        java.nio.file.Path confPath = Paths.get(url.toURI());
        confFile = confPath.toFile();
      } catch (URISyntaxException e) {
        throw new DoyConfigException(
            "Invalid path to Drill-on-YARN configuration file: "
                + url.toString(),
            e);
      }
      drillSite = confFile.getParentFile();
    }

    // Verify that the site directory is not just $DRILL_HOME/conf.
    // Since the calling script does not differentiate between the two cases.
    // But, treat $DRILL_HOME/conf as the site directory if:
    // 1. The conf-as-site property is true, or
    // 2. The Drill archive resides within $DRILL_HOME.
    //
    // The above situations occur in certain distributions that
    // ship the archive inside the site directory and don't use a
    // site directory.

    if (drillHome.equals(drillSite.getParentFile())
        && !config.getBoolean(CONF_AS_SITE)) {
      String drillArchivePath = config
          .getString(DrillOnYarnConfig.DRILL_ARCHIVE_PATH);
      if (!DoYUtil.isBlank(drillArchivePath)) {
        File archiveFile = new File(drillArchivePath);
        if (!archiveFile.isAbsolute() && !archiveFile.getAbsolutePath()
            .startsWith(drillHome.getAbsolutePath())) {
          drillSite = null;
        }
      }
    }
  }

  /**
   * Retrieve the AM Drill home location from the DRILL_HOME variable set in the
   * drill-am.sh launch script.
   *
   * @throws DoyConfigException
   */

  public void setAmDrillHome() throws DoyConfigException {
    String drillHomeStr = getEnv(DRILL_HOME_ENV_VAR);
    drillHome = new File(drillHomeStr);
    setSiteDir();
  }

  public Config getConfig() {
    return instance.config;
  }

  public static DrillOnYarnConfig instance() {
    assert instance != null;
    return instance;
  }

  public static Config config() {
    return instance().getConfig();
  }

  /**
   * Return the Drill home on this machine as inferred from the config file
   * contents or location.
   *
   * @return Drill home
   */

  public File getLocalDrillHome() {
    return drillHome;
  }

  public void dump() {
    dump(System.out);
  }

  private static final String keys[] = {
    // drill.yarn

    APP_NAME,
    CLUSTER_ID,

    // drill.yarn.dfs

    DFS_CONNECTION,
    DFS_APP_DIR,

    // drill.yarn.hadoop

    HADOOP_HOME,
    HADOOP_CLASSPATH,
    HBASE_CLASSPATH,

    // drill.yarn.yarn

    YARN_QUEUE,
    YARN_PRIORITY,

    // drill.yarn.drill-install

    DRILL_ARCHIVE_PATH,
    DRILL_DIR_NAME,
    LOCALIZE_DRILL,
    CONF_AS_SITE,
    DRILL_HOME,
    DRILL_ARCHIVE_KEY,
    SITE_ARCHIVE_KEY,
    JAVA_LIB_PATH,

    // drill.yarn.client

    CLIENT_POLL_SEC,
    CLIENT_START_WAIT_SEC,
    CLIENT_STOP_WAIT_SEC,

    // drill.yarn.am

    AM_MEMORY,
    AM_VCORES,
    AM_DISKS,
    AM_NODE_LABEL_EXPR,
    AM_VM_ARGS,
    AM_HEAP,
    AM_POLL_PERIOD_MS,
    AM_TICK_PERIOD_MS,
    AM_PREFIX_CLASSPATH,
    AM_CLASSPATH,
    AM_DEBUG_LAUNCH,
    AM_ENABLE_AUTO_SHUTDOWN,

    // drill.yarn.zk

    ZK_CONNECT,
    ZK_ROOT,
    ZK_RETRY_COUNT,
    ZK_RETRY_DELAY_MS,
    ZK_FAILURE_TIMEOUT_MS,

    // drill.yarn.drillbit

    DRILLBIT_MEMORY,
    DRILLBIT_VCORES,
    DRILLBIT_DISKS,
    DRILLBIT_VM_ARGS,
    DRILLBIT_HEAP,
    DRILLBIT_DIRECT_MEM,
    DRILLBIT_CODE_CACHE,
    DRILLBIT_PREFIX_CLASSPATH,
    DRILLBIT_EXTN_CLASSPATH,
    DRILLBIT_CLASSPATH,
    DRILLBIT_MAX_RETRIES,
    DRILLBIT_DEBUG_LAUNCH,
    DRILLBIT_MAX_EXTRA_NODES,
    DRILLBIT_REQUEST_TIMEOUT_SEC,
    DISABLE_YARN_LOGS,
    DRILLBIT_HTTP_PORT,
    DRILLBIT_USER_PORT,
    DRILLBIT_BIT_PORT,
    DRILLBIT_USE_HTTPS,

    // drill.yarn.http

    HTTP_ENABLED,
    HTTP_ENABLE_SSL,
    HTTP_PORT,
    HTTP_AUTH_TYPE,
    HTTP_SESSION_MAX_IDLE_SECS,
    HTTP_DOCS_LINK,
    HTTP_REFRESH_SECS,
    // Do not include AM_REST_KEY: it is supposed to be secret.
    // Same is true of HTTP_USER_NAME and HTTP_PASSWORD
  };

  private static String envVars[] = {
      APP_ID_ENV_VAR,
      DRILL_HOME_ENV_VAR,
      DRILL_SITE_ENV_VAR,
      AM_HEAP_ENV_VAR,
      AM_JAVA_OPTS_ENV_VAR,
      DRILL_CLASSPATH_PREFIX_ENV_VAR,
      DRILL_CLASSPATH_ENV_VAR,
      DRILL_ARCHIVE_ENV_VAR,
      SITE_ARCHIVE_ENV_VAR,
      DRILL_DEBUG_ENV_VAR
  };

  private void dump(PrintStream out) {
    for (String key : keys) {
      out.print(key);
      out.print(" = ");
      try {
        out.println(config.getString(key));
      } catch (ConfigException.Missing e) {
        out.println("<missing>");
      }
    }
    out.print(CLUSTERS);
    out.println("[");
    for (int i = 0; i < clusterGroupCount(); i++) {
      ClusterDef.ClusterGroup cluster = ClusterDef.getCluster(config, i);
      out.print(i);
      out.println(" = {");
      cluster.dump("  ", out);
      out.println("  }");
    }
    out.println("]");
  }

  public void dumpEnv(PrintStream out) {
    out.print("environment");
    out.println("[");
    for (String envVar : envVars) {
      String value = getEnv(envVar);
      out.print(envVar);
      out.print(" = ");
      if (value == null) {
        out.print("<unset>");
      } else {
        out.print("\"");
        out.print(value);
        out.print("\"");
      }
      out.println();
    }
    out.println("]");
  }

  public List<NameValuePair> getPairs() {
    List<NameValuePair> pairs = new ArrayList<>();
    for (String key : keys) {
      pairs.add(new NameValuePair(key, config.getString(key)));
    }
    for (int i = 0; i < clusterGroupCount(); i++) {
      ClusterDef.ClusterGroup pool = ClusterDef.getCluster(config, i);
      pool.getPairs(i, pairs);
    }

    // Add environment variables as "pseudo" properties,
    // prefixed with "envt.".

    for (String envVar : envVars) {
      pairs.add(new NameValuePair("envt." + envVar, getEnv(envVar)));
    }
    return pairs;
  }

  public static String clusterGroupKey(int index, String key) {
    return CLUSTERS + "." + index + "." + key;
  }

  public int clusterGroupCount() {
    return config.getList(CLUSTERS).size();
  }

  private static String suffixes[] = { ".tar.gz", ".tgz", ".zip" };

  public static String findSuffix(String baseName) {
    baseName = baseName.toLowerCase();
    for (String extn : suffixes) {
      if (baseName.endsWith(extn)) {
        return extn;
      }
    }
    return null;
  }

  /**
   * Get the location of Drill home on a remote machine, relative to the
   * container working directory. Used when constructing a launch context.
   * Assumes either the absolute path from the config file, or a constructed
   * path to the localized Drill on the remote node. YARN examples use "./foo"
   * to refer to container resources. But, since we cannot be sure when such a
   * path is evaluated, we explicitly use YARN's PWD environment variable to get
   * the absolute path.
   *
   * @return the remote path, with the "$PWD" environment variable.
   * @throws DoyConfigException
   */

  public String getRemoteDrillHome() throws DoyConfigException {
    // If the application is not localized, then the user can tell us the remote
    // path in the config file. Otherwise, we assume that the remote path is the
    // same as the local path.

    if (!config.getBoolean(LOCALIZE_DRILL)) {
      String drillHomePath = config.getString(DRILL_HOME);
      if (DoYUtil.isBlank(drillHomePath)) {
        drillHomePath = drillHome.getAbsolutePath();
      }
      return drillHomePath;
    }

    // The application is localized. Work out the location within the container
    // directory. The path starts with the "key" we specify when uploading the
    // Drill archive; YARN expands the archive into a folder of that name.

    String drillHome = "$PWD/" + config.getString(DRILL_ARCHIVE_KEY);

    String home = config.getString(DRILL_DIR_NAME);
    if (DoYUtil.isBlank(home)) {
      // Assume the archive expands without a subdirectory.
    }

    // If the special "<base>" marker is used, assume that the path depends
    // on the name of the archive, which we know from the config file.

    else if (home.equals(BASE_NAME_MARKER)) {

      // Otherwise, assume that the archive expands to a directory with the
      // same name as the archive itself (minus the archive suffix.)

      String drillArchivePath = config
          .getString(DrillOnYarnConfig.DRILL_ARCHIVE_PATH);
      if (DoYUtil.isBlank(drillArchivePath)) {
        throw new DoyConfigException("Required config property not set: "
            + DrillOnYarnConfig.DRILL_ARCHIVE_PATH);
      }
      File localArchiveFile = new File(drillArchivePath);
      home = localArchiveFile.getName();
      String suffix = findSuffix(home);
      if (suffix == null) {
        throw new DoyConfigException(DrillOnYarnConfig.DRILL_ARCHIVE_PATH
            + " does not name a valid archive: " + drillArchivePath);
      }
      drillHome += "/" + home.substring(0, home.length() - suffix.length());
    } else {
      // If the user told us the name of the directory within the archive,
      // use it.

      drillHome += "/" + home;
    }
    return drillHome;
  }

  /**
   * Get the optional remote site directory name. This name will include the
   * absolute path for a non-localized application. It will return the path
   * relative to the container for a localized application. In the localized
   * case, the site archive is tar'ed relative to the site directory so that its
   * contents are unarchived directly into the YARN-provided folder (with the
   * name of the archive) key. That is, if the site directory on the client is
   * /var/drill/my-site, the contents of the tar file will be
   * "./drill-override.conf", etc., and the remote location is
   * $PWD/site-key/drill-override.conf, where site-key is the key name used to
   * localize the site archive.
   *
   * @return remote site directory name
   */

  public String getRemoteSiteDir() {
    // If the application does not use a site directory, then return null.

    if (!hasSiteDir()) {
      return null;
    }

    // If the application is not localized, then use the remote site path
    // provided in the config file. Otherwise, assume that the remote path
    // is the same as the local path.

    if (!config.getBoolean(LOCALIZE_DRILL)) {
      String drillSitePath = config.getString(SITE_DIR);
      if (DoYUtil.isBlank(drillSitePath)) {
        drillSitePath = drillSite.getAbsolutePath();
      }
      return drillSitePath;
    }

    // Work out the site directory name as above for the Drill directory.
    // The caller must include a archive subdirectory name if required.

    return "$PWD/" + config.getString(SITE_ARCHIVE_KEY);
  }

  /**
   * Return the app ID file to use for this client run. The file is in the
   * directory that holds the site dir (if a site dir is used), else the
   * directory that holds Drill home (otherwise.) Not that the file does NOT go
   * into the site dir or Drill home as we upload these directories (via
   * archives) to DFS so we don't want to change them by adding a file.
   * <p>
   * It turns out that Drill allows two distinct clusters to share the same ZK
   * root and/or cluster ID (just not the same combination), so the file name
   * contains both parts.
   *
   * @return local app id file
   */

  public File getLocalAppIdFile() {
    String rootDir = config.getString(DrillOnYarnConfig.ZK_ROOT);
    String clusterId = config.getString(DrillOnYarnConfig.CLUSTER_ID);
    String key = rootDir + "-" + clusterId;
    String appIdFileName = key + ".appid";
    File appIdDir;
    if (hasSiteDir()) {
      appIdDir = drillSite.getParentFile();
    } else {
      appIdDir = drillHome.getParentFile();
    }
    return new File(appIdDir, appIdFileName);
  }

  public boolean hasSiteDir() {
    return drillSite != null;
  }

  public File getLocalSiteDir() {
    return drillSite;
  }

  /**
   * Returns the DFS path to the localized Drill archive. This is an AM-only
   * method as it relies on an environment variable set by the client. It is set
   * only if the application is localized, it is not set for a non-localized
   * run.
   *
   * @return the DFS path to the localized Drill archive
   */

  public String getDrillArchiveDfsPath() {
    return getEnv(DrillOnYarnConfig.DRILL_ARCHIVE_ENV_VAR);
  }

  /**
   * Returns the DFS path to the localized site archive. This is an AM-only
   * method as it relies on an environment variable set by the client. This
   * variable is optional; if not set then the AM can infer that the application
   * does not use a site archive (configuration files reside in
   * $DRILL_HOME/conf), or the application is not localized.
   *
   * @return the DFS path to the localized site archive
   */

  public String getSiteArchiveDfsPath() {
    return getEnv(DrillOnYarnConfig.SITE_ARCHIVE_ENV_VAR);
  }
}
