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

import org.apache.drill.yarn.client.StatusCommand.Reporter;
import org.apache.drill.yarn.core.DrillOnYarnConfig;
import org.apache.drill.yarn.core.YarnRMClient;
import org.apache.hadoop.yarn.api.records.ApplicationId;

import com.typesafe.config.Config;

/**
 * Launches a drill cluster by uploading the Drill archive then launching the
 * Drill Application Master (AM). For testing, can also do just the upload or
 * just the launch. Handles both a localized Drill and a non-localized launch
 * (which uses a pre-installed Drill.)
 * <p>
 * This single operation combines upload and launch because the upload
 * Information is needed by the launch.
 * <p>
 * On the surface, it would seem that uploading a file and launching an app
 * should be simple operations. However, under YARN, we must handle a large
 * number of details that must be gotten exactly right. Plus, both the upload
 * and launch can be slow operations, so we provide feedback to the user that
 * something is, indeed, happening.
 */

public class StartCommand extends ClientCommand {
  private Config config;
  private boolean upload;
  private boolean launch;
  private boolean dryRun;

  public StartCommand(boolean upload, boolean launch) {
    this.upload = upload;
    this.launch = launch;
  }

  @Override
  public void run() throws ClientException {
    checkExistingApp();

    dryRun = opts.dryRun;
    config = DrillOnYarnConfig.config();
    FileUploader uploader = upload();
    if (launch) {
      launch(uploader);
    }
  }

  /**
   * Check if an application ID file exists. If it does, check if an application
   * is running. If an app is running, then we can't start a new one. If the app
   * is not running, then clean up the "orphan" app id file.
   *
   * @throws ClientException
   */

  private void checkExistingApp() throws ClientException {
    File appIdFile = getAppIdFile();
    if (!appIdFile.exists()) {
      return;
    }

    // File exists. Ask YARN about status.

    Reporter reporter;
    ApplicationId appId;
    try {
      System.out.println("Found app ID file: " + appIdFile.getAbsolutePath());
      appId = checkAppId();
      System.out.print("Checking application ID: " + appId.toString() + "...");
      YarnRMClient client = new YarnRMClient(appId);
      reporter = new Reporter(client);
      reporter.getReport();
    } catch (ClientException e) {
      // This exception occurs when we ask for a report about an application
      // that
      // YARN does not know about. (YARN has likely been restarted.)

      System.out.println(" Not running.");
      appIdFile.delete();
      return;
    }

    // YARN knows about the application. But, was it stopped, perhaps from the
    // web UI?

    if (reporter.isStopped()) {
      System.out.println(" Completed with state " + reporter.getState());
      appIdFile.delete();
      return;
    }

    // The app (or another one with the same App ID) is running.

    System.out.println(" Still running!");
    throw new ClientException(
        "Error: AM already running as Application ID: " + appId);
  }

  private FileUploader upload() throws ClientException {
    FileUploader uploader;
    if (!config.getBoolean(DrillOnYarnConfig.LOCALIZE_DRILL)) {
      uploader = new FileUploader.NonLocalized(dryRun, opts.verbose);
    } else if (upload) {
      uploader = new FileUploader.UploadFiles(opts.force, dryRun, opts.verbose);
    } else {
      uploader = new FileUploader.ReuseFiles(dryRun, opts.verbose);
    }
    uploader.run();
    return uploader;
  }

  private void launch(FileUploader uploader) throws ClientException {
    AMRunner runner = new AMRunner(config, opts.verbose, dryRun);
    runner.resources = uploader.resources;
    runner.remoteDrillHome = uploader.remoteDrillHome;
    runner.remoteSiteDir = uploader.remoteSiteDir;
    if (uploader.isLocalized()) {
      runner.drillArchivePath = uploader.drillArchivePath.toString();
      if (uploader.hasSiteDir()) {
        runner.siteArchivePath = uploader.siteArchivePath.toString();
      }
    }
    runner.run();
  }
}
