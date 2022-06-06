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

package org.apache.ambari.msi;

import org.apache.ambari.server.controller.internal.ResourceImpl;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.utilities.PropertyHelper;

import java.util.Map;
import java.util.Set;

/**
 * A host component resource provider for a MSI defined cluster.
 */
public class HostComponentProvider extends BaseResourceProvider {

  // Host Components
  protected static final String HOST_COMPONENT_CLUSTER_NAME_PROPERTY_ID   = PropertyHelper.getPropertyId("HostRoles", "cluster_name");
  protected static final String HOST_COMPONENT_SERVICE_NAME_PROPERTY_ID   = PropertyHelper.getPropertyId("HostRoles", "service_name");
  protected static final String HOST_COMPONENT_COMPONENT_NAME_PROPERTY_ID = PropertyHelper.getPropertyId("HostRoles", "component_name");
  protected static final String HOST_COMPONENT_HOST_NAME_PROPERTY_ID      = PropertyHelper.getPropertyId("HostRoles", "host_name");
  protected static final String HOST_COMPONENT_STATE_PROPERTY_ID          = PropertyHelper.getPropertyId("HostRoles", "state");
  protected static final String HOST_COMPONENT_DESIRED_STATE_PROPERTY_ID  = PropertyHelper.getPropertyId("HostRoles", "desired_state");


  // ----- Constructors ------------------------------------------------------

  /**
   * Construct a resource provider based on the given cluster definition.
   *
   * @param clusterDefinition  the cluster definition
   */
  public HostComponentProvider(ClusterDefinition clusterDefinition) {
    super(Resource.Type.HostComponent, clusterDefinition);
    initHostComponentResources();
  }


  // ----- AbstractResourceProvider ------------------------------------------

  @Override
  public void updateProperties(Resource resource, Request request, Predicate predicate) {
    Set<String> propertyIds = getRequestPropertyIds(request, predicate);
    if (contains(propertyIds, HOST_COMPONENT_STATE_PROPERTY_ID) ||
        contains(propertyIds, HOST_COMPONENT_DESIRED_STATE_PROPERTY_ID)) {
      String componentName = (String) resource.getPropertyValue(HOST_COMPONENT_COMPONENT_NAME_PROPERTY_ID);
      String hostName      = (String) resource.getPropertyValue(HOST_COMPONENT_HOST_NAME_PROPERTY_ID);

      String hostComponentState = getClusterDefinition().getHostComponentState(hostName, componentName);

      resource.setProperty(HOST_COMPONENT_STATE_PROPERTY_ID, hostComponentState);
      resource.setProperty(HOST_COMPONENT_DESIRED_STATE_PROPERTY_ID, hostComponentState);
    }
  }

  @Override
  public int updateProperties(Resource resource, Map<String, Object> properties) {
    int requestId = -1;
    if (properties.containsKey(HOST_COMPONENT_STATE_PROPERTY_ID)) {
      String state         = (String) properties.get(HOST_COMPONENT_STATE_PROPERTY_ID);
      String componentName = (String) resource.getPropertyValue(HOST_COMPONENT_COMPONENT_NAME_PROPERTY_ID);
      String hostName      = (String) resource.getPropertyValue(HOST_COMPONENT_HOST_NAME_PROPERTY_ID);

      requestId = getClusterDefinition().setHostComponentState(hostName, componentName, state);
    }
    return requestId;
  }


  // ----- helper methods ----------------------------------------------------

  /**
   * Create the resources based on the cluster definition.
   */
  private void initHostComponentResources() {
    String      clusterName = getClusterDefinition().getClusterName();
    Set<String> services    = getClusterDefinition().getServices();
    for (String serviceName : services) {
      Set<String> hosts = getClusterDefinition().getHosts();
      for (String hostName : hosts) {
        Set<String> hostComponents = getClusterDefinition().getHostComponents(serviceName, hostName);
        for (String componentName : hostComponents) {
          Resource hostComponent = new ResourceImpl(Resource.Type.HostComponent);
          hostComponent.setProperty(HOST_COMPONENT_CLUSTER_NAME_PROPERTY_ID, clusterName);
          hostComponent.setProperty(HOST_COMPONENT_SERVICE_NAME_PROPERTY_ID, serviceName);
          hostComponent.setProperty(HOST_COMPONENT_COMPONENT_NAME_PROPERTY_ID, componentName);
          hostComponent.setProperty(HOST_COMPONENT_HOST_NAME_PROPERTY_ID, hostName);

          addResource(hostComponent);
        }
      }
    }
  }
}
