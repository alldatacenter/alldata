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
import org.apache.drill.yarn.core.DrillOnYarnConfig;
import org.apache.drill.yarn.core.YarnRMClient;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import com.typesafe.config.Config;

public class ResizeCommand extends ClientCommand {
  private Config config;
  private YarnRMClient client;

  @Override
  public void run() throws ClientException {
    config = DrillOnYarnConfig.config();
    client = getClient();
    System.out.println(
        "Resizing cluster for Application ID: " + client.getAppId().toString());

    // First get an application report to ensure that the AM is,
    // in fact, running, and to get the HTTP endpoint.

    StatusCommand.Reporter reporter = new StatusCommand.Reporter(client);
    try {
      reporter.getReport();
    } catch (ClientException e) {
      reporter = null;
    }
    String prefix = opts.resizePrefix;
    int quantity = opts.resizeValue;
    String cmd;
    if (prefix.equals("+")) {
      cmd = "grow";
      if (opts.verbose) {
        System.out.println("Growing cluster by " + quantity + " nodes.");
      }
    } else if (prefix.equals("-")) {
      cmd = "shrink";
      if (opts.verbose) {
        System.out.println("Shrinking cluster by " + quantity + " nodes.");
      }
    } else {
      cmd = "resize";
      if (opts.verbose) {
        System.out.println("Resizing cluster to " + quantity + " nodes.");
      }
    }
    if (sendResize(reporter.getAmUrl(), cmd, quantity)) {
      System.out.println("Use web UI or status command to check progress.");
    }
  }

  private boolean sendResize(String baseUrl, String cmd, int quantity) {
    try {
      if (DoYUtil.isBlank(baseUrl)) {
        return false;
      }
      SimpleRestClient restClient = new SimpleRestClient();
      String tail = "rest/" + cmd + "/" + quantity;
      String masterKey = config.getString(DrillOnYarnConfig.HTTP_REST_KEY);
      if (!DoYUtil.isBlank(masterKey)) {
        tail += "?key=" + masterKey;
      }
      if (opts.verbose) {
        System.out.println("Resizing with POST " + baseUrl + "/" + tail);
      }
      String result = restClient.send(baseUrl, tail, true);

      JSONParser parser = new JSONParser();
      Object response;
      try {
        response = parser.parse(result);
      } catch (ParseException e) {
        System.err.println("Invalid response received from AM");
        if (opts.verbose) {
          System.out.println(result);
          System.out.println(e.getMessage());
        }
        return false;
      }
      JSONObject root = (JSONObject) response;

      System.out.println("AM responded: " + root.get("message"));
      if ("ok".equals(root.get("status"))) {
        return true;
      }
      System.err.println("Failed to resize the application master.");
      return false;
    } catch (ClientException e) {
      System.err.println("Resize failed: " + e.getMessage());
      return false;
    }
  }

}
