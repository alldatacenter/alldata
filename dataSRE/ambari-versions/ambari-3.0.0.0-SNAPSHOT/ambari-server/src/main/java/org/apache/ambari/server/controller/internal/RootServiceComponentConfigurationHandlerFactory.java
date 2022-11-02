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

import org.apache.ambari.server.configuration.AmbariServerConfigurationCategory;
import org.apache.ambari.server.controller.RootComponent;
import org.apache.ambari.server.controller.RootService;

import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * RootServiceComponentConfigurationHandlerFactory produces RootServiceComponentConfigurationHandler
 * implementations for the relevant service, component, and category.
 */
@Singleton
public class RootServiceComponentConfigurationHandlerFactory {

  @Inject
  private AmbariServerConfigurationHandler defaultConfigurationHandler;

  @Inject
  private AmbariServerLDAPConfigurationHandler ldapConfigurationHandler;

  @Inject
  private AmbariServerSSOConfigurationHandler ssoConfigurationHandler;

  @Inject
  private AmbariServerConfigurationHandler tproxyConfigurationHandler;

  /**
   * Returns the internal configuration handler used to support various configuration storage facilities.
   *
   * @param serviceName   the service name
   * @param componentName the component name
   * @param categoryName  the category name
   * @return a {@link RootServiceComponentConfigurationHandler}
   */
  public RootServiceComponentConfigurationHandler getInstance(String serviceName, String componentName, String categoryName) {
    if (RootService.AMBARI.name().equals(serviceName)) {
      if (RootComponent.AMBARI_SERVER.name().equals(componentName)) {
        if (AmbariServerConfigurationCategory.LDAP_CONFIGURATION.getCategoryName().equals(categoryName)) {
          return ldapConfigurationHandler;
        } else if (AmbariServerConfigurationCategory.SSO_CONFIGURATION.getCategoryName().equals(categoryName)) {
          return ssoConfigurationHandler;
        } else if (AmbariServerConfigurationCategory.TPROXY_CONFIGURATION.getCategoryName().equals(categoryName)) {
          return tproxyConfigurationHandler;
        } else {
          return defaultConfigurationHandler;
        }
      }
    }

    return null;
  }
}
