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

import org.apache.ambari.server.controller.spi.Resource;

import java.util.HashSet;
import java.util.Set;

/**
 * An abstract resource provider for a MSI defined cluster.
 */
public abstract class BaseResourceProvider extends AbstractResourceProvider {

  private final Set<Resource> resources = new HashSet<Resource>();

  /**
   * Construct a resource provider based on the given cluster definition.
   *
   * @param clusterDefinition the cluster definition
   */
  public BaseResourceProvider(Resource.Type type, ClusterDefinition clusterDefinition) {
    super(type, clusterDefinition);
  }

  /**
   * Get the set of resources for this provider.
   *
   * @return the set of resources
   */
  protected Set<Resource> getResources() {
    return resources;
  }

  /**
   * Add a resource to the set of resources provided by this provider.
   *
   * @param resource  the resource to add
   */
  protected void addResource(Resource resource) {
    resources.add(resource);
  }
}
