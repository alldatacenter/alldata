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

package org.apache.ambari.server.orm.models;

import java.util.ArrayList;
import java.util.List;

import org.apache.ambari.server.StaticallyInject;
import org.apache.ambari.server.orm.dao.HostComponentDesiredStateDAO;
import org.apache.ambari.server.orm.dao.HostComponentStateDAO;
import org.apache.ambari.server.orm.dao.HostDAO;
import org.apache.ambari.server.orm.entities.HostComponentDesiredStateEntity;
import org.apache.ambari.server.orm.entities.HostComponentStateEntity;
import org.apache.ambari.server.orm.entities.HostEntity;
import org.apache.ambari.server.state.State;
import org.codehaus.jackson.annotate.JsonProperty;

import com.google.inject.Inject;

/**
 * The {@link HostComponentSum1mary} class provides a concise representation of
 * the state of a component on a given host. Some of its fields are serializable
 * to JSON.
 */
@StaticallyInject
public class HostComponentSummary {
  @JsonProperty("service_name")
  private String serviceName;

  @JsonProperty("component_name")
  private String componentName;

  @JsonProperty("host_id")
  private Long hostId;

  @JsonProperty("host_name")
  private String hostName;

  @JsonProperty("desired_state")
  private State desiredState;

  @JsonProperty("current_state")
  private State currentState;

  @Inject
  private static HostDAO hostDao;

  @Inject
  private static HostComponentStateDAO hostComponentStateDao;

  @Inject
  private static HostComponentDesiredStateDAO hostComponentDesiredStateDao;

  public HostComponentSummary(String serviceName, String componentName, Long hostId, State desiredState, State currentState) {
    this.serviceName = serviceName;
    this.componentName = componentName;
    this.hostId = hostId;

    HostEntity host = hostDao.findById(hostId);
    if (host != null) {
      hostName = host.getHostName();
    }

    this.desiredState = desiredState;
    this.currentState = currentState;
  }

  public long getHostId() {
    return hostId;
  }

  public String getHostName() {
    return (hostName == null || hostName.isEmpty()) ? "" : hostName;
  }

  public State getDesiredState() {
    return desiredState;
  }

  public State getCurrentState() {
    return currentState;
  }

  public static List<HostComponentSummary> getHostComponentSummaries(String serviceName, String componentName) {
    List<HostComponentSummary> hostComponentSummaries = new ArrayList<>();
    List<HostComponentStateEntity> hostComponentStates = hostComponentStateDao.findByServiceAndComponent(serviceName, componentName);

    if (hostComponentStates != null) {
      for (HostComponentStateEntity hcse : hostComponentStates) {
        // Find the corresponding record for HostComponentDesiredStateEntity
        HostComponentDesiredStateEntity hcdse = hostComponentDesiredStateDao.findByServiceComponentAndHost(hcse.getServiceName(), hcse.getComponentName(), hcse.getHostName());
        if (hcdse != null) {
          HostComponentSummary s = new HostComponentSummary(hcse.getServiceName(), hcse.getComponentName(), hcse.getHostId(), hcdse.getDesiredState(), hcse.getCurrentState());

          hostComponentSummaries.add(s);
        }
      }
    }
    return hostComponentSummaries;
  }

  @Override
  public int hashCode() {
    int result;
    result = 31 + (serviceName != null ? serviceName.hashCode() : 0);
    result = result + (componentName != null ? componentName.hashCode() : 0);
    result = result + (hostId != null ? hostId.hashCode() : 0);
    return result;
  }
}
