/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
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

import static org.apache.ambari.server.api.services.stackadvisor.StackAdvisorRequest.StackAdvisorRequestType.SSO_CONFIGURATIONS;
import static org.apache.ambari.server.configuration.AmbariServerConfigurationCategory.SSO_CONFIGURATION;
import static org.apache.ambari.server.configuration.AmbariServerConfigurationKey.SSO_ENABLED_SERVICES;
import static org.apache.ambari.server.configuration.AmbariServerConfigurationKey.SSO_MANAGE_SERVICES;

import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.api.services.stackadvisor.StackAdvisorHelper;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.events.publishers.AmbariEventPublisher;
import org.apache.ambari.server.orm.dao.AmbariConfigurationDAO;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.ConfigHelper;

import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * AmbariServerSSOConfigurationHandler is an {@link AmbariServerConfigurationHandler} implementation
 * handing changes to the SSO configuration
 */
@Singleton
public class AmbariServerSSOConfigurationHandler extends AmbariServerStackAdvisorAwareConfigurationHandler {

  @Inject
  public AmbariServerSSOConfigurationHandler(Clusters clusters, ConfigHelper configHelper, AmbariManagementController managementController,
      StackAdvisorHelper stackAdvisorHelper, AmbariConfigurationDAO ambariConfigurationDAO, AmbariEventPublisher publisher) {
    super(ambariConfigurationDAO, publisher, clusters, configHelper, managementController, stackAdvisorHelper);
  }

  @Override
  public void updateComponentCategory(String categoryName, Map<String, String> properties, boolean removePropertiesIfNotSpecified) throws AmbariException {
    // Use the default implementation of #updateComponentCategory; however if Ambari is managing the SSO implementations
    // always process them, even the of sso-configuration properties have not been changed since we do not
    // know of the Ambari SSO data has changed in the ambari.properties file.  For example the authentication.jwt.providerUrl
    // or authentication.jwt.publicKey values.
    super.updateComponentCategory(categoryName, properties, removePropertiesIfNotSpecified);

    // Determine if Ambari is managing SSO configurations...
    final Map<String, String> ssoProperties = getConfigurationProperties(SSO_CONFIGURATION.getCategoryName());
    final boolean manageSSOConfigurations = (ssoProperties != null) && "true".equalsIgnoreCase(ssoProperties.get(SSO_MANAGE_SERVICES.key()));

    if (manageSSOConfigurations) {
      processClusters(SSO_CONFIGURATIONS);
    }
  }

  /**
   * Gets the set of services for which the user declared  Ambari to enable SSO integration.
   * <p>
   * If Ambari is not managing SSO integration configuration for services the set of names will be empry.
   *
   * @return a set of service names
   */
  public Set<String> getSSOEnabledServices() {
    return getEnabledServices(SSO_CONFIGURATION.getCategoryName(), SSO_MANAGE_SERVICES.key(), SSO_ENABLED_SERVICES.key());
  }
  
  @Override
  protected String getServiceVersionNote() {
    return "Ambari-managed single sign-on configurations";
  }
}
