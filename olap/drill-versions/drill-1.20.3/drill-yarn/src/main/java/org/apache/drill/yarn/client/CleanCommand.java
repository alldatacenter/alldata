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

import org.apache.drill.yarn.core.DfsFacade;
import org.apache.drill.yarn.core.DrillOnYarnConfig;

import com.typesafe.config.Config;

import org.apache.drill.yarn.core.DfsFacade.DfsFacadeException;

public class CleanCommand extends ClientCommand {
  private Config config;
  private DfsFacade dfs;

  @Override
  public void run() throws ClientException {
    config = DrillOnYarnConfig.config();
    if (!isLocalized()) {
      System.out.println("Not using localized files; nothing to clean.");
      return;
    }
    connectToDfs();
    removeDrillArchive();
    removeSiteArchive();
  }

  public boolean isLocalized() {
    return config.getBoolean(DrillOnYarnConfig.LOCALIZE_DRILL);
  }

  protected void connectToDfs() throws ClientException {
    try {
      System.out.print("Connecting to DFS...");
      dfs = new DfsFacade(config);
      dfs.connect();
      System.out.println(" Connected.");
    } catch (DfsFacadeException e) {
      System.out.println("Failed.");
      throw new ClientException("Failed to connect to DFS", e);
    }
  }

  private void removeDrillArchive() {
    String localArchivePath = config
        .getString(DrillOnYarnConfig.DRILL_ARCHIVE_PATH);
    String archiveName = new File(localArchivePath).getName();
    removeArchive(archiveName);
  }

  private void removeArchive(String archiveName) {
    System.out.print("Removing " + archiveName + " ...");
    try {
      dfs.removeDrillFile(archiveName);
      System.out.println(" Removed");
    } catch (DfsFacadeException e) {
      System.out.println();
      System.err.println(e.getMessage());
    }
  }

  private void removeSiteArchive() {
    DrillOnYarnConfig doyConfig = DrillOnYarnConfig.instance();
    if (!doyConfig.hasSiteDir()) {
      return;
    }
    String archiveName = DrillOnYarnConfig.SITE_ARCHIVE_NAME;
    removeArchive(archiveName);
  }

}
