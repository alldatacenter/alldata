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

package org.apache.ambari.server.ldap.service;

import org.apache.ambari.server.ldap.domain.AmbariLdapConfiguration;
import org.apache.directory.ldap.client.api.LdapConnectionConfig;

/**
 * Contract for creating connection configuration instances.
 * Implementers are in charge for implementing any required custom logic based on the ambari configuration properties.
 * (Eg.: using custom key stores etc...)
 */
public interface LdapConnectionConfigService {

  /**
   * Creates and sets up an ldap connection configuration instance based on the provided ambari ldap configuration instance.
   *
   * @param ambariLdapConfiguration instance holding configuration values
   * @return a set up ldap connection configuration instance
   * @throws AmbariLdapException if an error occurs while setting up the connection configuration
   */
  LdapConnectionConfig createLdapConnectionConfig(AmbariLdapConfiguration ambariLdapConfiguration) throws AmbariLdapException;

}
