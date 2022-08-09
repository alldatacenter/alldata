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

package org.apache.ambari.server.controller.utilities;

import org.apache.ambari.server.controller.internal.ClusterControllerImpl;
import org.apache.ambari.server.controller.spi.ClusterController;
import org.apache.ambari.server.controller.spi.ProviderModule;
import org.apache.ambari.server.view.ViewProviderModule;

/**
 * Temporary class to bootstrap a cluster controller.  TODO : Replace this global state with injection.
 */
public class ClusterControllerHelper {

  private static String PROVIDER_MODULE_CLASS = System.getProperty("provider.module.class",
      "org.apache.ambari.server.controller.internal.DefaultProviderModule");

  private static ClusterController controller;

  public static synchronized ClusterController getClusterController() {
    if (controller == null) {
      try {
        Class<?> implClass = Class.forName(PROVIDER_MODULE_CLASS);
        ProviderModule providerModule = ViewProviderModule.getViewProviderModule((ProviderModule) implClass.newInstance());
        controller = new ClusterControllerImpl(providerModule);

      } catch (Exception e) {
        throw new IllegalStateException("Can't create provider module " + PROVIDER_MODULE_CLASS, e);
      }
    }
    return controller;
  }
}
