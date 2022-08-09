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

/**
 * Contract defining operations to detect user and group attributes.
 */
public interface LdapAttributeDetectionService {

  /**
   * Decorates the passed in configuration with the detected ldap user attribute values
   *
   * @param ambariLdapConfiguration configuration instance holding connection details
   * @return the configuration decorated with user related attributes
   */
  AmbariLdapConfiguration detectLdapUserAttributes(AmbariLdapConfiguration ambariLdapConfiguration) throws AmbariLdapException;

  /**
   * Decorates the passed in configuration with the detected ldap group attribute values
   *
   * @param ambariLdapConfiguration configuration instance holding connection details
   * @return the configuration decorated with group related attributes
   */
  AmbariLdapConfiguration detectLdapGroupAttributes(AmbariLdapConfiguration ambariLdapConfiguration) throws AmbariLdapException;
}

