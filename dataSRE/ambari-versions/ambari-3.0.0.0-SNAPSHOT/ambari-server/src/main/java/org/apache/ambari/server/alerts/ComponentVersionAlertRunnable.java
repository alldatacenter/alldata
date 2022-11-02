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
package org.apache.ambari.server.alerts;

import java.text.MessageFormat;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.orm.entities.AlertDefinitionEntity;
import org.apache.ambari.server.orm.entities.RepositoryVersionEntity;
import org.apache.ambari.server.orm.entities.UpgradeEntity;
import org.apache.ambari.server.stack.upgrade.Direction;
import org.apache.ambari.server.state.Alert;
import org.apache.ambari.server.state.AlertState;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.ComponentInfo;
import org.apache.ambari.server.state.Host;
import org.apache.ambari.server.state.Service;
import org.apache.ambari.server.state.ServiceComponent;
import org.apache.ambari.server.state.ServiceComponentHost;
import org.apache.ambari.server.state.StackId;
import org.apache.commons.lang.StringUtils;

import com.google.inject.Inject;

/**
 * The {@link ComponentVersionAlertRunnable} is used to determine if the
 * reported version of host components match what is expected. If there is a
 * mismatch, then an alert will be triggered indicating which components are in
 * need of attention.
 * <p/>
 * This alert will not run during upgrades or when the cluster is still being
 * provisioned.
 */
public class ComponentVersionAlertRunnable extends AlertRunnable {

  /**
   * The message for the alert when all components are reporting correct
   * versions.
   */
  private static final String ALL_COMPONENTS_CORRECT_MSG = "All components are reporting their expected versions.";

  /**
   * The message for the alert when there is an upgrade in progress.
   */
  private static final String UPGRADE_IN_PROGRESS_MSG = "This alert will be suspended while the {0} is in progress.";

  /**
   * The unknown component error message.
   */
  private static final String UNKNOWN_COMPONENT_MSG_TEMPLATE = "Unable to retrieve component information for {0}/{1}";

  /**
   * The version mismatch message.
   */
  private static final String MISMATCHED_VERSIONS_MSG = "The following components are reporting unexpected versions: ";

  @Inject
  private AmbariMetaInfo m_metaInfo;

  /**
   * Constructor.
   *
   * @param definitionName
   */
  public ComponentVersionAlertRunnable(String definitionName) {
    super(definitionName);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  List<Alert> execute(Cluster cluster, AlertDefinitionEntity myDefinition) throws AmbariException {
    // if there is an upgrade in progress, then skip running this alert
    UpgradeEntity upgrade = cluster.getUpgradeInProgress();
    if (null != upgrade) {
      Direction direction = upgrade.getDirection();
      String message = MessageFormat.format(UPGRADE_IN_PROGRESS_MSG, direction.getText(false));

      return Collections.singletonList(
          buildAlert(cluster, myDefinition, AlertState.SKIPPED, message));
    }

    TreeMap<Host, Set<ServiceComponentHost>> versionMismatches = new TreeMap<>();
    Collection<Host> hosts = cluster.getHosts();

    for (Host host : hosts) {
      List<ServiceComponentHost> hostComponents = cluster.getServiceComponentHosts(host.getHostName());
      for (ServiceComponentHost hostComponent : hostComponents) {
        Service service = cluster.getService(hostComponent.getServiceName());
        ServiceComponent serviceComponent = service.getServiceComponent(hostComponent.getServiceComponentName());

        RepositoryVersionEntity desiredRepositoryVersion = service.getDesiredRepositoryVersion();
        StackId desiredStackId = serviceComponent.getDesiredStackId();
        String desiredVersion = desiredRepositoryVersion.getVersion();

        final ComponentInfo componentInfo;
        try {
          componentInfo = m_metaInfo.getComponent(desiredStackId.getStackName(),
              desiredStackId.getStackVersion(), hostComponent.getServiceName(),
              hostComponent.getServiceComponentName());
        } catch (AmbariException ambariException) {
          // throw an UNKNOWN response if we can't load component info
          String message = MessageFormat.format(UNKNOWN_COMPONENT_MSG_TEMPLATE,
              hostComponent.getServiceName(), hostComponent.getServiceComponentName());

          return Collections.singletonList(
              buildAlert(cluster, myDefinition, AlertState.UNKNOWN, message));
        }

        // skip components that don't advertise a version
        if (!componentInfo.isVersionAdvertised()) {
          continue;
        }

        String version = hostComponent.getVersion();
        if (!StringUtils.equals(version, desiredVersion)) {
          Set<ServiceComponentHost> mismatchedComponents = versionMismatches.get(host);
          if (null == mismatchedComponents) {
            mismatchedComponents = new HashSet<>();
            versionMismatches.put(host, mismatchedComponents);
          }

          mismatchedComponents.add(hostComponent);
        }
      }
    }

    AlertState alertState = AlertState.OK;
    String alertText = ALL_COMPONENTS_CORRECT_MSG;

    // if there are any components reporting the wrong version, fire off a warning
    if (!versionMismatches.isEmpty()) {
      StringBuilder buffer = new StringBuilder(MISMATCHED_VERSIONS_MSG);
      buffer.append(System.lineSeparator());

      for (Host host : versionMismatches.keySet()) {
        buffer.append("  ").append(host.getHostName());
        buffer.append(System.lineSeparator());
        for (ServiceComponentHost hostComponent : versionMismatches.get(host)) {
          buffer.append("    ").append(hostComponent.getServiceComponentName()).append(": ").append(
              hostComponent.getVersion()).append(System.lineSeparator());
        }
      }

      alertText = buffer.toString();
      alertState = AlertState.WARNING;
    }

    return Collections.singletonList(buildAlert(cluster, myDefinition, alertState, alertText));
  }
}
