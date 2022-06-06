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

package org.apache.ambari.server.view;

import java.util.List;
import java.util.Map;

import org.apache.ambari.server.controller.spi.PropertyProvider;
import org.apache.ambari.server.controller.spi.ProviderModule;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.spi.ResourceProvider;

/**
 * Module which allows for discovery of view resource providers.
 * This module wraps another module and delegates to it for
 * any resource types not defined in a view.
 */
public class ViewProviderModule implements ProviderModule {
  /**
   * The delegate provider module.
   */
  private final ProviderModule providerModule;


  // ----- Constructors ------------------------------------------------------

  /**
   * Construct a view provider module.
   *
   * @param providerModule     the delegate provider module
   */
  private ViewProviderModule(ProviderModule providerModule) {
    this.providerModule = providerModule;
  }


  // ----- ProviderModule ----------------------------------------------------

  @Override
  public ResourceProvider getResourceProvider(Resource.Type type) {

    Map<Resource.Type, ResourceProvider> resourceProviders =
        ViewRegistry.getInstance().getResourceProviders();

    if (resourceProviders.containsKey(type)) {
      return resourceProviders.get(type);
    }
    return providerModule.getResourceProvider(type);
  }

  @Override
  public List<PropertyProvider> getPropertyProviders(Resource.Type type) {
    return providerModule.getPropertyProviders(type);
  }


  // ----- helper methods -----------------------------------------

  /**
   * Factory method to get a view provider module.
   *
   * @param module  the delegate provider module
   *
   * @return a view provider module
   */
  public static ViewProviderModule getViewProviderModule(ProviderModule module) {
    return new ViewProviderModule(module);
  }
}
