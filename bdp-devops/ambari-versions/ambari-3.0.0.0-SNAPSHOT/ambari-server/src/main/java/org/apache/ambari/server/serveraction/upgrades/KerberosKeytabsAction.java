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

import java.util.Map;
import java.util.concurrent.ConcurrentMap;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.actionmanager.HostRoleStatus;
import org.apache.ambari.server.agent.CommandReport;
import org.apache.ambari.server.controller.KerberosHelper;
import org.apache.ambari.server.serveraction.kerberos.KDCType;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Config;

import com.google.inject.Inject;

/**
 * This class is made to emulate a manual action.  It runs in RU when Ranger Admin
 * is installed to the cluster and moving to a version that requires a new keytab.
 *
 * There are checks for:
 * <ul>
 *  <li>Kerberos is enabled for the cluster</li>
 *  <li>If the KDC type is set (KDCType is not {@link KDCType#NONE}, implying manual)</li>
 * </ul>
 */
public class KerberosKeytabsAction extends AbstractUpgradeServerAction {

  private static final String KERBEROS_ENV = "kerberos-env";
  private static final String KDC_TYPE_KEY = "kdc_type";

  @Inject
  private KerberosHelper m_kerberosHelper;

  @Override
  public CommandReport execute(ConcurrentMap<String, Object> requestSharedDataContext)
      throws AmbariException, InterruptedException {


    String clusterName = getExecutionCommand().getClusterName();
    Cluster cluster = getClusters().getCluster(clusterName);

    StringBuilder stdout = new StringBuilder();

    stdout.append(String.format("Checking %s is secured by Kerberos... %s",
        clusterName, m_kerberosHelper.isClusterKerberosEnabled(cluster))).append(System.lineSeparator());

    if (!m_kerberosHelper.isClusterKerberosEnabled(cluster)) {
      stdout.append(String.format("Cluster %s is not secured by Kerberos.  No action required.",
          clusterName));

      return createCommandReport(0, HostRoleStatus.COMPLETED, "{}", stdout.toString(), "");
    }

    stdout.append(String.format("Loading %s for cluster %s", KERBEROS_ENV, clusterName)).append(System.lineSeparator());

    Config kerberosEnv = cluster.getDesiredConfigByType(KERBEROS_ENV);
    if (kerberosEnv == null) {
      stdout.append(String.format("Configuration %s was not found.  No action required.", KERBEROS_ENV));

      return createCommandReport(0, HostRoleStatus.COMPLETED, "{}", stdout.toString(), "");
    }

    Map<String, String> kerbProperties = kerberosEnv.getProperties();

    KDCType kdcType = KDCType.NONE;

    if (null != kerbProperties && kerbProperties.containsKey(KDC_TYPE_KEY)) {
      kdcType = KDCType.translate(kerbProperties.get(KDC_TYPE_KEY));
    }

    stdout.append(String.format("Checking KDC type... %s", kdcType)).append(System.lineSeparator());

    if (KDCType.NONE == kdcType) {
      stdout.append(String.format("KDC Type is %s, keytabs are managed manually.  No action required.", kdcType));

      return createCommandReport(0, HostRoleStatus.COMPLETED, "{}", stdout.toString(), "");
    }

    stdout.append(String.format("Ambari is managing Kerberos keytabs.  Regenerate " +
        "keytabs after upgrade is complete."));

    // !!! make this holding, as the user must manually acknowlege.
    return createCommandReport(0, HostRoleStatus.HOLDING, "{}", stdout.toString(), "");
  }

}
