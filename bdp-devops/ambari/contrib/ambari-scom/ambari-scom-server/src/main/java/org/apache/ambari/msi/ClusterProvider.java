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

/**
 * A cluster resource provider for a MSI defined cluster.
 */
public class ClusterProvider extends BaseResourceProvider {

  // Clusters
  protected static final String CLUSTER_NAME_PROPERTY_ID    = PropertyHelper.getPropertyId("Clusters", "cluster_name");
  protected static final String CLUSTER_VERSION_PROPERTY_ID = PropertyHelper.getPropertyId("Clusters", "version");


  // ----- Constructors ------------------------------------------------------

  /**
   * Construct a resource provider based on the given cluster definition.
   *
   * @param clusterDefinition  the cluster definition
   */
  public ClusterProvider(ClusterDefinition clusterDefinition) {
    super(Resource.Type.Cluster, clusterDefinition);
    initClusterResources();
  }


  // ----- AbstractResourceProvider ------------------------------------------

  @Override
  public void updateProperties(Resource resource, Request request, Predicate predicate) {
    // Do nothing
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
  private void initClusterResources() {
    Resource cluster = new ResourceImpl(Resource.Type.Cluster);
    ClusterDefinition clusterDefinition = getClusterDefinition();
    cluster.setProperty(CLUSTER_NAME_PROPERTY_ID, clusterDefinition.getClusterName());
    cluster.setProperty(CLUSTER_VERSION_PROPERTY_ID, clusterDefinition.getVersionId());

    addResource(cluster);
  }
}
