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
import org.apache.ambari.server.StaticallyInject;
import org.apache.ambari.server.controller.ServiceComponentHostRequest;
import org.apache.ambari.server.controller.ServiceComponentHostResponse;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.State;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Determines the service status for Flume.  Generically, this means that
 * the state of Flume is the lowest ordinal state calculated.  For example:
 * <ul>
 *   <li>If all handlers are STARTED, service is STARTED.</li>
 *   <li>If one handler is INSTALLED, the service is INSTALLED.</li>
 * </ul>
 */
@StaticallyInject
public final class FlumeServiceCalculatedState extends DefaultServiceCalculatedState
  implements ServiceCalculatedState {

  private static final Logger LOG = LoggerFactory.getLogger(FlumeServiceCalculatedState.class);

  @Override
  public State getState(String clusterName, String serviceName) {
    try {
      Cluster cluster = getCluster(clusterName);
      if (cluster != null && managementControllerProvider != null) {

        ServiceComponentHostRequest request = new ServiceComponentHostRequest(clusterName,
          serviceName, null, null, null);

        Set<ServiceComponentHostResponse> hostComponentResponses =
          managementControllerProvider.get().getHostComponents(Collections.singleton(request), true);

        State state = State.UNKNOWN;
        for (ServiceComponentHostResponse schr : hostComponentResponses) {
          State schState = getHostComponentState(schr);
          if (schState.ordinal() < state.ordinal()) {
            state = schState;
          }
        }
        return state;
      }
    } catch (AmbariException e) {
      LOG.error("Can't determine service state.", e);
    }
    return State.UNKNOWN;
  }
}
