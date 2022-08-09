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

package org.apache.ambari.server.controller.utilities.state;

import java.util.Collections;
import java.util.Set;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.ObjectNotFoundException;
import org.apache.ambari.server.StaticallyInject;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.ServiceComponentHostRequest;
import org.apache.ambari.server.controller.ServiceComponentHostResponse;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.ComponentInfo;
import org.apache.ambari.server.state.MaintenanceState;
import org.apache.ambari.server.state.Service;
import org.apache.ambari.server.state.StackId;
import org.apache.ambari.server.state.State;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Provider;

/**
 * Default calculator of service state.
 * The following rules should apply :
 * For services that have all components DISABLED, the service state should be DISABLED.
 * For services that have any master components, the service state should
 * be STARTED if all master components are STARTED.
 * For services that have all client components, the service state should
 * be INSTALLED if all of the components are INSTALLED.
 * For all other cases the state of the service should match the highest state of all
 * of its component states or UNKNOWN if the component states can not be determined.
 */
@StaticallyInject
public class DefaultServiceCalculatedState implements ServiceCalculatedState {

  private static final Logger LOG = LoggerFactory.getLogger(DefaultServiceCalculatedState.class);

  @Inject
  private static Provider<Clusters> clustersProvider;

  @Inject
  static Provider<AmbariManagementController> managementControllerProvider;


  // Get the State of a host component
  protected State getHostComponentState(ServiceComponentHostResponse hostComponent) {
    return State.valueOf(hostComponent.getLiveState());
  }

  protected Cluster getCluster(String clusterName) throws AmbariException {
    if (clustersProvider != null && clusterName != null && clusterName.length() > 0) {
      Clusters clusters = clustersProvider.get();
      if (clusters != null){
          return clusters.getCluster(clusterName);
      }
    }
    return null;
  }

  @Override
  public State getState(String clusterName, String serviceName) {
      try {
        Cluster cluster = getCluster(clusterName);
        if (cluster != null && managementControllerProvider != null) {
          Service service = cluster.getService(serviceName);
          AmbariMetaInfo ambariMetaInfo = managementControllerProvider.get().getAmbariMetaInfo();
          StackId stackId = service.getDesiredStackId();

          ServiceComponentHostRequest request = new ServiceComponentHostRequest(clusterName,
            serviceName, null, null, null);

          Set<ServiceComponentHostResponse> hostComponentResponses =
            managementControllerProvider.get().getHostComponents(Collections.singleton(request), true);

          State   masterState = null;
          State   clientState = null;
          State   otherState = null;
          State   maxMMState = null; // The worst state among components in MM

          boolean hasDisabled  = false;
          boolean hasMaster    = false;
          boolean hasOther     = false;
          boolean hasClient    = false;
          boolean hasMM        = false;

          for (ServiceComponentHostResponse hostComponentResponse : hostComponentResponses ) {
            try {
              ComponentInfo componentInfo = ambariMetaInfo.getComponent(stackId.getStackName(),
                stackId.getStackVersion(), hostComponentResponse.getServiceName(),
                hostComponentResponse.getComponentName());

              State state = getHostComponentState(hostComponentResponse);
              // Components in MM should not affect service status,
              // so we tend to ignore them
              boolean isInMaintenance = ! MaintenanceState.OFF.toString().
                equals(hostComponentResponse.getMaintenanceState());

              if (state.equals(State.DISABLED)) {
                hasDisabled = true;
              }

              if (isInMaintenance && !componentInfo.isClient()) {
                hasMM = true;
                if ( maxMMState == null || state.ordinal() > maxMMState.ordinal()) {
                  maxMMState = state;
                }
              }

              if (componentInfo.isMaster()) {
                if (state.equals(State.STARTED) || ! isInMaintenance) {
                  // We rely on master's state to determine service state
                  hasMaster = true;
                }

                if (! state.equals(State.STARTED) &&
                  ! isInMaintenance &&  // Ignore status of MM component
                  ( masterState == null || state.ordinal() > masterState.ordinal())) {
                  masterState = state;
                }
              } else if (componentInfo.isClient()) {
                hasClient = true;
                if (!state.equals(State.INSTALLED) &&
                  (clientState == null || state.ordinal() > clientState.ordinal())) {
                  clientState = state;
                }
              } else {
                if (state.equals(State.STARTED) || ! isInMaintenance) {
                  // We rely on slaves's state to determine service state
                  hasOther = true;
                }
                if (! state.equals(State.STARTED) &&
                  ! isInMaintenance && // Ignore status of MM component
                  ( otherState == null || state.ordinal() > otherState.ordinal())) {
                  otherState = state;
                }
              }
            } catch (ObjectNotFoundException e) {
              // component doesn't exist, nothing to do
            }
          }

          return hasMaster   ? masterState == null ? State.STARTED : masterState :
            hasOther    ? otherState == null ? State.STARTED : otherState :
              hasClient   ? clientState == null ? State.INSTALLED : clientState :
                hasDisabled ? State.DISABLED :
                  hasMM       ? maxMMState : State.UNKNOWN;
        }
      } catch (AmbariException e) {
        LOG.error("Can't determine service state.", e);
      }
    return State.UNKNOWN;
  }
}
