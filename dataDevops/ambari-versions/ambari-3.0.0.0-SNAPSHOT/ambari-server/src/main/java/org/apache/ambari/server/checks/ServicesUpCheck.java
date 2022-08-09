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
package org.apache.ambari.server.checks;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.ambari.annotations.UpgradeCheckInfo;
import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.orm.models.HostComponentSummary;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.ComponentInfo;
import org.apache.ambari.server.state.Host;
import org.apache.ambari.server.state.MaintenanceState;
import org.apache.ambari.server.state.Service;
import org.apache.ambari.server.state.ServiceComponent;
import org.apache.ambari.server.state.StackId;
import org.apache.ambari.server.state.State;
import org.apache.ambari.spi.upgrade.UpgradeCheckDescription;
import org.apache.ambari.spi.upgrade.UpgradeCheckGroup;
import org.apache.ambari.spi.upgrade.UpgradeCheckRequest;
import org.apache.ambari.spi.upgrade.UpgradeCheckResult;
import org.apache.ambari.spi.upgrade.UpgradeCheckStatus;
import org.apache.ambari.spi.upgrade.UpgradeCheckType;
import org.apache.ambari.spi.upgrade.UpgradeType;
import org.apache.commons.lang.StringUtils;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Singleton;

/**
 * The {@link ServicesUpCheck} class is used to ensure that services are up
 * "enough" before an upgrade begins. This class uses the following rules:
 * <ul>
 * <li>If the component is a CLIENT, it will skip it</li>
 * <li>If the component is a MASTER then it must be online and not in MM</li>
 * <li>If the component is a SLAVE
 * <ul>
 * <li>If the cardinality is 1+, then determine if {@value #SLAVE_THRESHOLD} %
 * are running. Hosts in MM are counted as being "up" since they are not part of
 * the upgrade.</li>
 * <li>If the cardinality is 0, then all instances must be online. Hosts in MM
 * are counted as being "up" since they are not part of the upgrade.</li>
 * </ul>
 * </ul>
 * It seems counter-intuitive to have a liveliness check which allows a
 * percentage of the slaves to be down. The goal is to be able to start an
 * upgrade, even if some slave components on healthy hosts are down. We still
 * want hosts to be scehdule for upgrade of their other components.
 */
@Singleton
@UpgradeCheckInfo(
    group = UpgradeCheckGroup.LIVELINESS,
    order = 2.0f,
    required = { UpgradeType.ROLLING, UpgradeType.NON_ROLLING, UpgradeType.HOST_ORDERED })
public class ServicesUpCheck extends ClusterCheck {

  private static final float SLAVE_THRESHOLD = 0.5f;

  static final UpgradeCheckDescription SERVICES_UP = new UpgradeCheckDescription("SERVICES_UP",
      UpgradeCheckType.SERVICE,
      "All services must be started",
      new ImmutableMap.Builder<String, String>()
        .put(UpgradeCheckDescription.DEFAULT,
            "The following Services must be started: {{fails}}. Try to do a Stop & Start in case they were started outside of Ambari.").build());

  /**
   * Constructor.
   */
  public ServicesUpCheck() {
    super(SERVICES_UP);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public UpgradeCheckResult perform(UpgradeCheckRequest request)
      throws AmbariException {
    UpgradeCheckResult result = new UpgradeCheckResult(this);

    final String clusterName = request.getClusterName();
    final Cluster cluster = clustersProvider.get().getCluster(clusterName);
    List<String> errorMessages = new ArrayList<>();
    LinkedHashSet<ServiceDetail> failedServices = new LinkedHashSet<>();

    Set<String> servicesInUpgrade = checkHelperProvider.get().getServicesInUpgrade(request);
    for (String serviceName : servicesInUpgrade) {
      final Service service = cluster.getService(serviceName);

      // Ignore services like Tez that are clientOnly.
      if (service.isClientOnlyService()) {
        continue;
      }

      Map<String, ServiceComponent> serviceComponents = service.getServiceComponents();
      for (Map.Entry<String, ServiceComponent> component : serviceComponents.entrySet()) {

        ServiceComponent serviceComponent = component.getValue();
        // In Services like HDFS, ignore components like HDFS Client
        if (serviceComponent.isClientComponent()) {
          continue;
        }

        // skip if the component is not part of the finalization version check
        if (!serviceComponent.isVersionAdvertised()) {
          continue;
        }

        // TODO, add more logic that checks the Upgrade Pack.
        // These components are not in the upgrade pack and do not advertise a
        // version:
        // ZKFC, Ambari Metrics, Kerberos, Atlas (right now).
        // Generally, if it advertises a version => in the upgrade pack.
        // So it can be in the Upgrade Pack but not advertise a version.
        List<HostComponentSummary> hostComponentSummaries = HostComponentSummary.getHostComponentSummaries(
            service.getName(), serviceComponent.getName());

        // not installed, nothing to do
        if (hostComponentSummaries.isEmpty()) {
          continue;
        }

        // non-master, "true" slaves with cardinality 1+
        boolean checkThreshold = false;
        if (!serviceComponent.isMasterComponent()) {
          StackId stackId = service.getDesiredStackId();
          ComponentInfo componentInfo = ambariMetaInfo.get().getComponent(stackId.getStackName(),
              stackId.getStackVersion(), serviceComponent.getServiceName(),
              serviceComponent.getName());

          String cardinality = componentInfo.getCardinality();
          // !!! check if there can be more than one. This will match, say,
          // datanodes but not ZKFC
          if (null != cardinality
              && (cardinality.equals("ALL") || cardinality.matches("[1-9].*"))) {
            checkThreshold = true;
          }
        }

        // check threshold for slaves which have a non-0 cardinality
        if (checkThreshold) {
          int total = hostComponentSummaries.size();
          int up = 0;
          int down = 0;

          for (HostComponentSummary summary : hostComponentSummaries) {
            if (isConsideredDown(cluster, serviceComponent, summary)) {
              down++;
            } else {
              up++;
            }
          }

          if ((float) down / total > SLAVE_THRESHOLD) { // arbitrary
            failedServices.add(new ServiceDetail(serviceName));

            String message = MessageFormat.format(
                "{0}: {1} out of {2} {3} are started; there should be {4,number,percent} started before upgrading.",
                service.getName(), up, total, serviceComponent.getName(), SLAVE_THRESHOLD);
            errorMessages.add(message);
          }
        } else {
          for (HostComponentSummary summary : hostComponentSummaries) {
            if (isConsideredDown(cluster, serviceComponent, summary)) {
              failedServices.add(new ServiceDetail(serviceName));

              String message = MessageFormat.format("{0}: {1} (in {2} on host {3})",
                  service.getName(), serviceComponent.getName(), summary.getCurrentState(),
                  summary.getHostName());
              errorMessages.add(message);
              break;
            }
          }
        }
      }
    }

    if (!errorMessages.isEmpty()) {
      result.setFailedOn(
          failedServices.stream().map(failedService -> failedService.serviceName).collect(
              Collectors.toCollection(LinkedHashSet::new)));

      result.getFailedDetail().addAll(failedServices);
      result.setStatus(UpgradeCheckStatus.FAIL);
      result.setFailReason(
          "The following Service Components should be in a started state.  Please invoke a service Stop and full Start and try again. "
              + StringUtils.join(errorMessages, ", "));
    }

    return result;
  }

  /**
   * Gets whether this component should be considered as being "down" for the
   * purposes of this check. Component type, maintenance mode, and state are
   * taken into account.
   *
   * @param cluster
   *          the cluster
   * @param serviceComponent
   *          the component
   * @param summary
   *          a summary of the state of the component on a host
   * @return {@code true} if the host component should be considered as failing
   *         this test.
   * @throws AmbariException
   */
  private boolean isConsideredDown(Cluster cluster, ServiceComponent serviceComponent,
      HostComponentSummary summary) throws AmbariException {
    Host host = clustersProvider.get().getHostById(summary.getHostId());
    MaintenanceState maintenanceState = host.getMaintenanceState(cluster.getClusterId());

    // non-MASTER components in maintenance mode should not count
    if (maintenanceState == MaintenanceState.ON && !serviceComponent.isMasterComponent()) {
      return false;
    }

    State desiredState = summary.getDesiredState();
    State currentState = summary.getCurrentState();

    switch (desiredState) {
      case INSTALLED:
      case STARTED:
        return currentState != State.STARTED;
      default:
        return false;
    }
  }
}
