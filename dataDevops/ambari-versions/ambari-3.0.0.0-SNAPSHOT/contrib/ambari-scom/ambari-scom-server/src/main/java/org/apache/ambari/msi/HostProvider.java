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
import org.apache.ambari.server.controller.spi.SystemException;
import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Set;

/**
 * A host resource provider for a MSI defined cluster.
 */
public class HostProvider extends BaseResourceProvider {

  // Hosts
  protected static final String HOST_CLUSTER_NAME_PROPERTY_ID =
      PropertyHelper.getPropertyId("Hosts", "cluster_name");
  protected static final String HOST_NAME_PROPERTY_ID =
      PropertyHelper.getPropertyId("Hosts", "host_name");
  protected static final String HOST_STATE_PROPERTY_ID =
      PropertyHelper.getPropertyId("Hosts", "host_state");
  protected static final String HOST_IP_PROPERTY_ID =
      PropertyHelper.getPropertyId("Hosts", "ip");


  // ----- Constants ---------------------------------------------------------

  protected final static Logger LOG =
      LoggerFactory.getLogger(HostProvider.class);


  // ----- Constructors ------------------------------------------------------

  /**
   * Construct a resource provider based on the given cluster definition.
   *
   * @param clusterDefinition  the cluster definition
   */
  public HostProvider(ClusterDefinition clusterDefinition) {
    super(Resource.Type.Host, clusterDefinition);
    initHostResources();
  }


  // ----- AbstractResourceProvider ------------------------------------------

  @Override
  public void updateProperties(Resource resource, Request request, Predicate predicate) {
    Set<String> propertyIds = getRequestPropertyIds(request, predicate);
    if (contains(propertyIds, HOST_STATE_PROPERTY_ID)) {
      String hostName = (String) resource.getPropertyValue(HOST_NAME_PROPERTY_ID);
      resource.setProperty(HOST_STATE_PROPERTY_ID, getClusterDefinition().getHostState(hostName));
    }
  }

  @Override
  public int updateProperties(Resource resource, Map<String, Object> properties) {
    // Do nothing
    return -1;
  }


  // ----- helper methods ----------------------------------------------------

  /**
   * Create the resources based on the cluster definition.
   */
  private void initHostResources() {
    ClusterDefinition clusterDefinition = getClusterDefinition();
    String            clusterName       = clusterDefinition.getClusterName();
    Set<String>       hosts             = clusterDefinition.getHosts();

    for (String hostName : hosts) {
      Resource host = new ResourceImpl(Resource.Type.Host);
      host.setProperty(HOST_CLUSTER_NAME_PROPERTY_ID, clusterName);
      host.setProperty(HOST_NAME_PROPERTY_ID, hostName);
      try {
        host.setProperty(HOST_IP_PROPERTY_ID, clusterDefinition.getHostInfoProvider().getHostAddress(hostName));
      } catch (SystemException e) {
        if (LOG.isErrorEnabled()) {
          LOG.error("Can't set host ip address : caught exception", e);
        }
      }

      addResource(host);
    }
  }
}
