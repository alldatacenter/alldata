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
package org.apache.ambari.server.serveraction.upgrades;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentMap;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.actionmanager.HostRoleStatus;
import org.apache.ambari.server.agent.CommandReport;
import org.apache.ambari.server.stack.upgrade.orchestrate.UpgradeContext;
import org.apache.ambari.server.state.Cluster;
import org.apache.commons.lang.StringUtils;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

/**
 * Action that checks component versions to ensure {@link FinalizeUpgradeAction} will
 * complete successfully.
 */
public class ComponentVersionCheckAction extends FinalizeUpgradeAction {


  @Override
  public CommandReport execute(ConcurrentMap<String, Object> requestSharedDataContext)
      throws AmbariException, InterruptedException {

    String clusterName = getExecutionCommand().getClusterName();

    Cluster cluster = getClusters().getCluster(clusterName);

    UpgradeContext upgradeContext = getUpgradeContext(cluster);
    Set<InfoTuple> errors = validateComponentVersions(upgradeContext);

    StringBuilder outSB = new StringBuilder();
    StringBuilder errSB = new StringBuilder();

    if (errors.isEmpty()) {
      outSB.append("All service components are reporting the correct version.");
      return createCommandReport(0, HostRoleStatus.COMPLETED, "{}", outSB.toString(), errSB.toString());
    } else {
      String structuredOut = getErrors(outSB, errSB, errors);
      return createCommandReport(-1, HostRoleStatus.FAILED, structuredOut, outSB.toString(), errSB.toString());
    }
  }

  private String getErrors(StringBuilder outSB, StringBuilder errSB, Set<InfoTuple> errors) {

    errSB.append("Finalization will not be able to completed because of the following version inconsistencies:");
    errSB.append(System.lineSeparator());

    Set<String> hosts = new TreeSet<>();
    Map<String, JsonArray> hostDetails = new HashMap<>();

    for (InfoTuple tuple : errors) {
      errSB.append("  ");
      errSB.append(tuple.hostName).append(": ");
      errSB.append(System.lineSeparator()).append("    ");
      errSB.append(tuple.serviceName).append('/').append(tuple.componentName);
      errSB.append(" reports ").append(StringUtils.trimToEmpty(tuple.currentVersion));
      errSB.append(" but expects ").append(tuple.targetVersion);
      errSB.append(System.lineSeparator());

      hosts.add(tuple.hostName);

      if (!hostDetails.containsKey(tuple.hostName)) {
        hostDetails.put(tuple.hostName, new JsonArray());
      }

      JsonObject obj = new JsonObject();
      obj.addProperty("service", tuple.serviceName);
      obj.addProperty("component", tuple.componentName);
      obj.addProperty("version", tuple.currentVersion);
      obj.addProperty("targetVersion", tuple.targetVersion);

      hostDetails.get(tuple.hostName).add(obj);
    }

    JsonArray hostJson = new JsonArray();
    for (String h : hosts) {
      hostJson.add(new JsonPrimitive(h));
    }

    JsonObject valueJson = new JsonObject();
    for (Entry<String, JsonArray> entry : hostDetails.entrySet()) {
      valueJson.add(entry.getKey(), entry.getValue());
    }

    outSB.append(String.format("There were errors on the following hosts: %s",
        StringUtils.join(hosts, ", ")));

    JsonObject obj = new JsonObject();
    obj.add("hosts", hostJson);
    obj.add("host_detail", valueJson);

    return obj.toString();
  }



}
