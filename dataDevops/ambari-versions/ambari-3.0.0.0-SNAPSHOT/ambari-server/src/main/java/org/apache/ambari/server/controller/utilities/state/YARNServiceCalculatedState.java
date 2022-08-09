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
import org.apache.ambari.server.controller.ServiceComponentHostRequest;
import org.apache.ambari.server.controller.ServiceComponentHostResponse;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.ComponentInfo;
import org.apache.ambari.server.state.Service;
import org.apache.ambari.server.state.StackId;
import org.apache.ambari.server.state.State;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Calculator of YARN service state.
 */
@StaticallyInject
public final class YARNServiceCalculatedState extends DefaultServiceCalculatedState
  implements ServiceCalculatedState {

  private static final Logger LOG = LoggerFactory.getLogger(YARNServiceCalculatedState.class);

  @Override
  public State getState(String clusterName, String serviceName) {
    try {
      Cluster cluster = getCluster(clusterName);
      if (cluster != null && managementControllerProvider != null) {
        AmbariMetaInfo ambariMetaInfo = managementControllerProvider.get().getAmbariMetaInfo();
        Service service = cluster.getService(serviceName);
        StackId stackId = service.getDesiredStackId();


        ServiceComponentHostRequest request = new ServiceComponentHostRequest(clusterName,
          serviceName, null, null, null);

        Set<ServiceComponentHostResponse> hostComponentResponses =
          managementControllerProvider.get().getHostComponents(Collections.singleton(request), true);

        int     resourceManagerActiveCount      = 0;
        boolean isAppTimeLineServerActive       = false;
        State   nonStartedState                 = null;

        for (ServiceComponentHostResponse hostComponentResponse : hostComponentResponses ) {
          try {
            ComponentInfo componentInfo = ambariMetaInfo.getComponent(stackId.getStackName(),
              stackId.getStackVersion(), hostComponentResponse.getServiceName(),
              hostComponentResponse.getComponentName());

            if (componentInfo.isMaster()) {
              String componentName = hostComponentResponse.getComponentName();

              State state = getHostComponentState(hostComponentResponse);

              switch (state) {
                case STARTED:
                case DISABLED:
                  if (componentName.equals("RESOURCEMANAGER")) {
                    ++resourceManagerActiveCount;
                  } else if (componentName.equals("APP_TIMELINE_SERVER")) {
                    isAppTimeLineServerActive = true;
                  }
                  break;
                default:
                  nonStartedState = state;
              }
            }
          } catch (ObjectNotFoundException e) {
            // component doesn't exist, nothing to do
          }
        }

        if ( nonStartedState == null ||  // all started
          (isAppTimeLineServerActive &&
            resourceManagerActiveCount > 0)) {  // at least one active resource manager
          return State.STARTED;
        }
        return nonStartedState;
      }
    } catch (AmbariException e) {
      LOG.error("Can't determine service state.", e);
    }
    return State.UNKNOWN;
  }

}
