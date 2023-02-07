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

import org.apache.drill.yarn.core.DoYUtil;
import org.apache.drill.yarn.core.YarnClientException;
import org.apache.drill.yarn.core.YarnRMClient;
import org.apache.hadoop.yarn.api.records.ApplicationReport;
import org.apache.hadoop.yarn.api.records.FinalApplicationStatus;
import org.apache.hadoop.yarn.api.records.YarnApplicationState;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class StatusCommand extends ClientCommand {
  public static class Reporter {
    private YarnRMClient client;
    ApplicationReport report;

    public Reporter(YarnRMClient client) {
      this.client = client;
    }

    public void getReport() throws ClientException {
      try {
        report = client.getAppReport();
      } catch (YarnClientException e) {
        throw new ClientException(
            "Failed to get report for Drill application master", e);
      }
    }

    public void display(boolean verbose, boolean isNew) {
      YarnApplicationState state = report.getYarnApplicationState();
      if (verbose || !isNew) {
        System.out.println("Application State: " + state.toString());
        System.out.println("Host: " + report.getHost());
      }
      if (verbose || !isNew) {
        System.out.println("Queue: " + report.getQueue());
        System.out.println("User: " + report.getUser());
        long startTime = report.getStartTime();
        System.out.println("Start Time: " + DoYUtil.toIsoTime(startTime));
        System.out.println("Application Name: " + report.getName());
      }
      System.out.println("Tracking URL: " + report.getTrackingUrl());
      if (isNew) {
        System.out.println("Application Master URL: " + getAmUrl());
      }
      showFinalStatus();
    }

    public String getAmUrl() {
      return StatusCommand.getAmUrl(report);
    }

    public void showFinalStatus() {
      YarnApplicationState state = report.getYarnApplicationState();
      if (state == YarnApplicationState.FAILED
          || state == YarnApplicationState.FINISHED) {
        FinalApplicationStatus status = report.getFinalApplicationStatus();
        System.out.println("Final status: " + status.toString());
        if (status != FinalApplicationStatus.SUCCEEDED) {
          String diag = report.getDiagnostics();
          if (!DoYUtil.isBlank(diag)) {
            System.out.println("Diagnostics: " + diag);
          }
        }
      }
    }

    public YarnApplicationState getState() {
      return report.getYarnApplicationState();
    }

    public boolean isStarting() {
      YarnApplicationState state = getState();
      return state == YarnApplicationState.ACCEPTED
          || state == YarnApplicationState.NEW
          || state == YarnApplicationState.NEW_SAVING
          || state == YarnApplicationState.SUBMITTED;
    }

    public boolean isStopped() {
      YarnApplicationState state = getState();
      return state == YarnApplicationState.FAILED
          || state == YarnApplicationState.FINISHED
          || state == YarnApplicationState.KILLED;
    }

    public boolean isRunning() {
      YarnApplicationState state = getState();
      return state == YarnApplicationState.RUNNING;
    }
  }

  public static String getAmUrl(ApplicationReport report) {
    return DoYUtil.unwrapAmUrl(report.getOriginalTrackingUrl());
  }

  @Override
  public void run() throws ClientException {
    YarnRMClient client = getClient();
    System.out.println("Application ID: " + client.getAppId().toString());
    Reporter reporter = new Reporter(client);
    try {
      reporter.getReport();
    } catch (Exception e) {
      removeAppIdFile();
      System.out.println("Application is not running.");
      return;
    }
    reporter.display(opts.verbose, false);
    if (reporter.isRunning()) {
      showAmStatus(reporter.report);
    }
  }

  private void showAmStatus(ApplicationReport report) {
    try {
      String baseUrl = getAmUrl(report);
      if (DoYUtil.isBlank(baseUrl)) {
        return;
      }
      SimpleRestClient restClient = new SimpleRestClient();
      String tail = "rest/status";
      if (opts.verbose) {
        System.out.println("Getting status with " + baseUrl + "/" + tail);
      }
      String result = restClient.send(baseUrl, tail, false);
      formatResponse(result);
      System.out.println("For more information, visit: " + baseUrl);
    } catch (ClientException e) {
      System.out.println("Failed to get AM status");
      System.err.println(e.getMessage());
    }
  }

  private void formatResponse(String result) {
    JSONParser parser = new JSONParser();
    Object status;
    try {
      status = parser.parse(result);
    } catch (ParseException e) {
      System.err.println("Invalid response received from AM");
      if (opts.verbose) {
        System.out.println(result);
        System.out.println(e.getMessage());
      }
      return;
    }
    JSONObject root = (JSONObject) status;
    showMetric("AM State", root, "state");
    showMetric("Target Drillbit Count", root.get("summary"), "targetBitCount");
    showMetric("Live Drillbit Count", root.get("summary"), "liveBitCount");
    showMetric("Unmanaged Drillbit Count", root.get("summary"), "unmanagedCount");
    showMetric("Blacklisted Node Count", root.get("summary"), "blackListCount");
    showMetric("Free Node Count", root.get("summary"), "freeNodeCount");
  }

  private void showMetric(String label, Object object, String key) {
    if (object == null) {
      return;
    }
    if (!(object instanceof JSONObject)) {
      return;
    }
    object = ((JSONObject) object).get(key);
    if (object == null) {
      return;
    }
    System.out.println(label + ": " + object.toString());
  }
}
