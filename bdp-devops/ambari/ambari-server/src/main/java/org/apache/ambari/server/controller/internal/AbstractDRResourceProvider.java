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

package org.apache.ambari.server.controller.internal;

import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.controller.ivory.IvoryService;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.spi.ResourceProvider;

/**
 * Abstract resource provider implementation that maps to an Ivory service.
 */
public abstract class AbstractDRResourceProvider extends AbstractResourceProvider {

  /**
   * The Ivory service.
   */
  private final IvoryService ivoryService;


  // ----- Constructors ------------------------------------------------------

  /**
   * Create a  new resource provider for the given management controller.
   *
   * @param propertyIds    the property ids
   * @param keyPropertyIds the key property ids
   */
  protected AbstractDRResourceProvider(Set<String> propertyIds,
                                       Map<Resource.Type, String> keyPropertyIds,
                                       IvoryService ivoryService) {
    super(propertyIds, keyPropertyIds);
    this.ivoryService = ivoryService;
  }

  // ----- accessors ---------------------------------------------------------

  /**
   * Get the associated service.
   *
   * @return the associated service
   */
  protected IvoryService getService() {
    return ivoryService;
  }

  // ----- utility methods ---------------------------------------------------

  /**
   * Factory method for obtaining a resource provider based on a given Ivory service instance.
   *
   * @param type         the resource type
   * @param service      the Ivory service
   *
   * @return a new resource provider
   */
  public static ResourceProvider getResourceProvider(Resource.Type type,
                                                     IvoryService service) {
    switch (type.getInternalType()) {
      case DRFeed:
        return new FeedResourceProvider(service);
      case DRTargetCluster:
        return new TargetClusterResourceProvider(service);
      case DRInstance:
        return new InstanceResourceProvider(service);
      default:
        throw new IllegalArgumentException("Unknown type " + type);
    }
  }
}
