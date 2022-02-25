/**
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

package org.apache.ambari.server.events.publishers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.ambari.server.EagerSingleton;
import org.apache.ambari.server.controller.utilities.ServiceCalculatedStateFactory;
import org.apache.ambari.server.controller.utilities.state.ServiceCalculatedState;
import org.apache.ambari.server.events.STOMPEvent;
import org.apache.ambari.server.events.ServiceUpdateEvent;
import org.apache.ambari.server.state.State;

import com.google.common.eventbus.EventBus;
import com.google.inject.Inject;

@EagerSingleton
public class ServiceUpdateEventPublisher extends BufferedUpdateEventPublisher<ServiceUpdateEvent> {
  private Map<String, Map<String, State>> states = new HashMap<>();

  @Inject
  public ServiceUpdateEventPublisher(STOMPUpdatePublisher stompUpdatePublisher) {
    super(stompUpdatePublisher);
  }


  @Override
  public STOMPEvent.Type getType() {
    return STOMPEvent.Type.SERVICE;
  }

  @Override
  public void mergeBufferAndPost(List<ServiceUpdateEvent> events, EventBus eventBus) {
    List<ServiceUpdateEvent> filtered = new ArrayList<>();
    for (ServiceUpdateEvent event : events) {
      int pos = filtered.indexOf(event);
      if (pos != -1) {
        if (event.isStateChanged()) {
          filtered.get(pos).setStateChanged(true);
        }
        if (event.getMaintenanceState() != null) {
          filtered.get(pos).setMaintenanceState(event.getMaintenanceState());
        }
      } else {
        filtered.add(event);
      }
    }
    for (ServiceUpdateEvent serviceUpdateEvent : filtered) {
      // calc state
      if (serviceUpdateEvent.isStateChanged()) {
        ServiceCalculatedState serviceCalculatedState =
            ServiceCalculatedStateFactory.getServiceStateProvider(serviceUpdateEvent.getServiceName());
        State serviceState =
            serviceCalculatedState.getState(serviceUpdateEvent.getClusterName(), serviceUpdateEvent.getServiceName());

        String serviceName = serviceUpdateEvent.getServiceName();
        String clusterName = serviceUpdateEvent.getClusterName();

        // retrieve state from cache
        // don't send update when state was not changed and update doesn't have maintenance info
        if (states.containsKey(clusterName) && states.get(clusterName).containsKey(serviceName)
            && states.get(clusterName).get(serviceName).equals(serviceState)
            && serviceUpdateEvent.getMaintenanceState() == null) {
          continue;
        }
        states.computeIfAbsent(clusterName, c -> new HashMap<>()).put(serviceName, serviceState);
        serviceUpdateEvent.setState(serviceState);
      }

      eventBus.post(serviceUpdateEvent);
    }
  }
}
