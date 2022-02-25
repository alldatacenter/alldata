/*
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

package org.apache.ambari.server.bootstrap;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import org.apache.ambari.server.utils.Closeables;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Runnable class that gets the hoststatus output by looking at the files
 * in a certain directory. Only meant to be useful for bootstrap as of now.
 */
class BSHostStatusCollector {
  private File requestIdDir;
  private List<BSHostStatus> hostStatus;
  public static final String logFileFilter = ".log";
  public static final String doneFileFilter = ".done";
  private static final Logger LOG = LoggerFactory.getLogger(BSHostStatusCollector.class);

  private List<String> hosts;

  public BSHostStatusCollector(File requestIdDir, List<String> hosts) {
    this.requestIdDir = requestIdDir;
    this.hosts = hosts;
  }

  public List<BSHostStatus> getHostStatus() {
    return hostStatus;
  }

  public void run() {
    LOG.info("Request directory " + requestIdDir);
    hostStatus = new ArrayList<>();
    if (hosts == null) {
      return;
    }
    File done;
    File log;
    LOG.info("HostList for polling on " + hosts);
    for (String host : hosts) {
      /* Read through the files and gather output */
      BSHostStatus status = new BSHostStatus();
      status.setHostName(host);
      done = new File(requestIdDir, host + doneFileFilter);
      log = new File(requestIdDir, host + logFileFilter);
      if (LOG.isDebugEnabled()) {
        LOG.debug("Polling bootstrap status for host, requestDir={}, host={}, doneFileExists={}, logFileExists={}", requestIdDir, host, done.exists(), log.exists());
      }
      if (!done.exists()) {
        status.setStatus("RUNNING");
      } else {
        status.setStatus("FAILED");
        try {
          String statusCode = FileUtils.readFileToString(done, Charset.defaultCharset()).trim();
          if (statusCode.equals("0")) {
            status.setStatus("DONE");
          }
          
          updateStatus(status, statusCode);
        } catch (IOException e) {
          LOG.info("Error reading done file " + done);
        }
      }
      if (!log.exists()) {
        status.setLog("");
      } else {
        String logString = "";
        BufferedReader reader = null;
        try {
          StringBuilder sb = new StringBuilder();
          reader = new BufferedReader(new FileReader(log));

          String line = null;
          while (null != (line = reader.readLine())) {
            if (line.startsWith("tcgetattr:") || line.startsWith("tput:"))
              continue;

            if (0 != sb.length() || 0 == line.length())
              sb.append('\n');

            if (-1 != line.indexOf ("\\n"))
              sb.append(line.replace("\\n", "\n"));
            else
              sb.append(line);
          }
          
          logString = sb.toString();
        } catch (IOException e) {
          LOG.info("Error reading log file " + log +
                  ". Log file may be have not created yet");
        } finally {
          Closeables.closeSilently(reader);
        }
        status.setLog(logString);
      }
      hostStatus.add(status);
    }
  }
  
  private void updateStatus(BSHostStatus status, String statusCode) {
    
    status.setStatusCode(statusCode);
    
    int reason = -1;
    try {
      reason = Integer.parseInt(statusCode);
    } catch (Exception ignored) {
    }
    
    switch (reason) {
    // case X: (as we find them)
    case 2:
      status.setStatusAction("Processing could not continue because the file was not found.");
      break;
    case 255:
    default:
      if (null != status.getLog()) {
        String lowerLog = status.getLog().toLowerCase();
        if (-1 != lowerLog.indexOf("permission denied") && -1 != lowerLog.indexOf("publickey")) {
          status.setStatusAction("Use correct SSH key");
        } else if (-1 != lowerLog.indexOf("connect to host")) {
          status.setStatusAction("Please verify that the hostname '" + status.getHostName() + "' is correct.");
        }
      }
      break;
    }
    
  }
}
