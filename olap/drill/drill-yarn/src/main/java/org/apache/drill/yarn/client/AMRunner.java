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
package org.apache.drill.yarn.client;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.Map;

import org.apache.drill.yarn.core.AppSpec;
import org.apache.drill.yarn.core.DoYUtil;
import org.apache.drill.yarn.core.DrillOnYarnConfig;
import org.apache.drill.yarn.core.LaunchSpec;
import org.apache.drill.yarn.core.YarnClientException;
import org.apache.drill.yarn.core.YarnRMClient;
import org.apache.hadoop.yarn.api.ApplicationConstants;
import org.apache.hadoop.yarn.api.protocolrecords.GetNewApplicationResponse;
import org.apache.hadoop.yarn.api.records.ApplicationId;
import org.apache.hadoop.yarn.api.records.LocalResource;
import org.apache.hadoop.yarn.api.records.YarnApplicationState;

import com.typesafe.config.Config;

/**
 * Launch the AM through YARN. Builds the launch description, then tracks
 * the launch operation itself. Finally, provides the user with links to
 * track the AM both through YARN and via the AM's own web UI.
 */

public class AMRunner {
  private Config config;
  private boolean verbose;
  private ApplicationId appId;
  public Map<String, LocalResource> resources;
  public String drillArchivePath;
  public String siteArchivePath;
  public String remoteDrillHome;
  public String remoteSiteDir;
  private YarnRMClient client;
  private GetNewApplicationResponse appResponse;
  private boolean dryRun;

  public AMRunner(Config config, boolean verbose, boolean dryRun) {
    this.config = config;
    this.verbose = verbose;
    this.dryRun = dryRun;
  }

  public void run() throws ClientException {
    connectToYarn();
    if (dryRun) {
      doDryRun();
    } else {
      doLaunch();
    }
  }

  private void connectToYarn() {
    System.out.print("Loading YARN Config...");
    client = new YarnRMClient();
    System.out.println(" Loaded.");
  }

  private void doDryRun() throws ClientException {
    AppSpec master = buildSpec();
    dump(master, System.out);
  }

  private void doLaunch() throws ClientException {
    createApp();
    AppSpec master = buildSpec();
    if (verbose) {
      dump(master, System.out);
    }
    validateResources(master);
    launchApp(master);
    waitForStartAndReport(master.appName);
    writeAppIdFile();
  }

  private void dump(AppSpec master, PrintStream out) {
    out.println("----------------------------------------------");
    out.println("Application Master Launch Spec");
    master.dump(out);
    out.println("----------------------------------------------");
  }

  private AppSpec buildSpec() throws ClientException {
    AppSpec master = new AppSpec();

    // Heap memory

    String heapMem = config.getString( DrillOnYarnConfig.AM_HEAP );
    master.env.put( DrillOnYarnConfig.AM_HEAP_ENV_VAR, heapMem );

    // Any additional VM arguments from the config file.

    addIfSet( master, DrillOnYarnConfig.AM_VM_ARGS, DrillOnYarnConfig.AM_JAVA_OPTS_ENV_VAR );

    // Any user specified override jars
    // Not really needed by the AM.

    addIfSet( master, DrillOnYarnConfig.AM_PREFIX_CLASSPATH, DrillOnYarnConfig.DRILL_CLASSPATH_PREFIX_ENV_VAR );

    // Any user specified classpath.

    addIfSet( master, DrillOnYarnConfig.AM_CLASSPATH, DrillOnYarnConfig.DRILL_CLASSPATH_ENV_VAR );

    // Any user-specified library path

    addIfSet( master, DrillOnYarnConfig.JAVA_LIB_PATH, DrillOnYarnConfig.DOY_LIBPATH_ENV_VAR );

    // AM logs (of which there are none.
    // Relies on the LOG_DIR_EXPANSION_VAR marker which is replaced by
    // the container log directory.
    // Must be set for the AM to prevent drill-config.sh from trying to create
    // the log directory in $DRILL_HOME (which won't be writable under YARN.)

    if (!config.getBoolean(DrillOnYarnConfig.DISABLE_YARN_LOGS)) {
      master.env.put("DRILL_YARN_LOG_DIR",
          ApplicationConstants.LOG_DIR_EXPANSION_VAR);
    }

    // AM launch script
    // The drill home location is either a non-localized location,
    // or, more typically, the expanded Drill directory under the
    // container's working directory. When the localized directory,
    // we rely on the fact that the current working directory is
    // set to the container directory, so we just need the name
    // of the Drill folder under the cwd.

    master.command = remoteDrillHome + "/bin/drill-am.sh";

    // If site dir, add that as an argument.

    if ( remoteSiteDir != null ) {
      master.cmdArgs.add( "--site" );
      master.cmdArgs.add( remoteSiteDir );
    }

    // Strangely, YARN has no way to tell an AM what its app ID
    // is. So, we pass it along here.

    String appIdStr = dryRun ? "Unknown" : appId.toString();
    master.env.put( DrillOnYarnConfig.APP_ID_ENV_VAR, appIdStr );

    // Debug launch: dumps environment variables and other information
    // in the launch script.

    if ( config.getBoolean( DrillOnYarnConfig.AM_DEBUG_LAUNCH ) ) {
      master.env.put( DrillOnYarnConfig.DRILL_DEBUG_ENV_VAR, "1" );
    }

    // If localized, add the drill and optionally site archive.

    if ( config.getBoolean( DrillOnYarnConfig.LOCALIZE_DRILL) ) {

      // Also, YARN has no way to tell an AM what localized resources are
      // available, so we pass them along as environment variables.

      master.env.put( DrillOnYarnConfig.DRILL_ARCHIVE_ENV_VAR, drillArchivePath );
      if ( siteArchivePath != null ) {
        master.env.put( DrillOnYarnConfig.SITE_ARCHIVE_ENV_VAR, siteArchivePath );
      }
    }

    // Localized resources

    master.resources.putAll( resources );

    // Container specification.

    master.memoryMb = config.getInt( DrillOnYarnConfig.AM_MEMORY );
    master.vCores = config.getInt( DrillOnYarnConfig.AM_VCORES );
    master.disks = config.getDouble( DrillOnYarnConfig.AM_DISKS );
    master.appName = config.getString( DrillOnYarnConfig.APP_NAME );
    master.queueName = config.getString( DrillOnYarnConfig.YARN_QUEUE );
    master.priority = config.getInt( DrillOnYarnConfig.YARN_PRIORITY );
    master.nodeLabelExpr = config.getString( DrillOnYarnConfig.AM_NODE_LABEL_EXPR );
    return master;
  }

  private void addIfSet(LaunchSpec spec, String configParam, String envVar) {
    String value = config.getString(configParam);
    if (!DoYUtil.isBlank(value)) {
      spec.env.put(envVar, value);
    }
  }

  private void createApp() throws ClientException {
    try {
      appResponse = client.createAppMaster();
    } catch (YarnClientException e) {
      throw new ClientException("Failed to allocate Drill application master",
          e);
    }
    appId = appResponse.getApplicationId();
    System.out.println("Application ID: " + appId.toString());
  }

  private void validateResources( AppSpec master ) throws ClientException {

    // Memory and core checks per YARN app specs.

    int maxMemory = appResponse.getMaximumResourceCapability().getMemory();
    int maxCores = appResponse.getMaximumResourceCapability().getVirtualCores();
    if (verbose) {
      System.out.println("Max Memory: " + maxMemory);
      System.out.println("Max Cores: " + maxCores);
    }

    // YARN behaves very badly if we request a container larger than the
    // maximum.

    if (master.memoryMb > maxMemory) {
     throw new ClientException( "YARN maximum memory is " + maxMemory
         + " but the application master requests " + master.memoryMb );
    }
    if (master.vCores > maxCores) {
      throw new ClientException("YARN maximum vcores is " + maxCores
          + " but the application master requests " + master.vCores);
    }

    // Verify the limits for the Drillbit as well.

    if (config.getInt(DrillOnYarnConfig.DRILLBIT_MEMORY) > maxMemory) {
      throw new ClientException(
          "YARN maximum memory is " + maxMemory + " but the Drillbit requests "
              + config.getInt(DrillOnYarnConfig.DRILLBIT_MEMORY));
    }
    if (config.getInt(DrillOnYarnConfig.DRILLBIT_VCORES) > maxCores) {
      throw new ClientException("YARN maximum vcores is " + maxCores
          + " but the Drillbit requests "
          + config.getInt(DrillOnYarnConfig.DRILLBIT_VCORES));
    }
  }

  private void launchApp(AppSpec master) throws ClientException {
    try {
      client.submitAppMaster(master);
    } catch (YarnClientException e) {
      throw new ClientException("Failed to start Drill application master", e);
    }
  }

  /**
   * Write the app id file needed for subsequent commands. The app id file is
   * the only way we know the YARN application associated with our Drill-on-YARN
   * session. This file is ready by subsequent status, resize and stop commands
   * so we can find our Drill AM on the YARN cluster.
   *
   * @throws ClientException
   */

  private void writeAppIdFile() throws ClientException {
    // Write the appid file that lets us work with the app later
    // (Analogous to a pid file.)
    // File goes into the directory above Drill Home (which should be the
    // folder that contains the localized archive) and is named for the
    // ZK cluster (to ensure that the name is a valid file name.)

    File appIdFile = ClientCommand.getAppIdFile();
    try {
      PrintWriter writer = new PrintWriter(new FileWriter(appIdFile));
      writer.println(appId);
      writer.close();
    } catch (IOException e) {
      throw new ClientException(
          "Failed to write appid file: " + appIdFile.getAbsolutePath());
    }
  }

  /**
   * Poll YARN to track the launch process of the application so that we can
   * wait until the AM is live before pointing the user to the AM's web UI.
   */

  private class StartMonitor {
    StatusCommand.Reporter reporter;
    private YarnApplicationState state;
    private int pollWaitSec;
    private int startupWaitSec;

    public StartMonitor() {
      pollWaitSec = config.getInt(DrillOnYarnConfig.CLIENT_POLL_SEC);
      if (pollWaitSec < 1) {
        pollWaitSec = 1;
      }
      startupWaitSec = config.getInt(DrillOnYarnConfig.CLIENT_START_WAIT_SEC);
    }

    void run(String appName) throws ClientException {
      System.out.print("Launching " + appName + "...");
      reporter = new StatusCommand.Reporter(client);
      reporter.getReport();
      if (!reporter.isStarting()) {
        return;
      }
      updateState(reporter.getState());
      try {
        int attemptCount = startupWaitSec / pollWaitSec;
        for (int attempt = 0; attempt < attemptCount; attempt++) {
          if (!poll()) {
            break;
          }
        }
      } finally {
        System.out.println();
      }
      reporter.display(verbose, true);
      if (reporter.isStarting()) {
        System.out.println(
            "Application Master is slow to start, use the 'status' command later to check status.");
      }
    }

    private boolean poll() throws ClientException {
      try {
        Thread.sleep(pollWaitSec * 1000);
      } catch (InterruptedException e) {
        return false;
      }
      reporter.getReport();
      if (!reporter.isStarting()) {
        return false;
      }
      YarnApplicationState newState = reporter.getState();
      if (newState == state) {
        System.out.print(".");
        return true;
      }
      System.out.println();
      updateState(newState);
      return true;
    }

    private void updateState(YarnApplicationState newState) {
      state = newState;
      if (verbose) {
        System.out.print("Application State: ");
        System.out.println(state.toString());
        System.out.print("Starting...");
      }
    }
  }

  private void waitForStartAndReport(String appName) throws ClientException {
    StartMonitor monitor = new StartMonitor();
    monitor.run(appName);
  }
}