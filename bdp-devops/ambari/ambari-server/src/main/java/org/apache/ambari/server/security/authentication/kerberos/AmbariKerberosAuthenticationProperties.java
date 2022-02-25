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

package org.apache.ambari.server.security.authentication.kerberos;

import java.util.Collections;
import java.util.List;

import org.apache.ambari.server.security.authorization.UserAuthenticationType;

/**
 * AmbariKerberosAuthenticationProperties is a container for Kerberos authentication-related
 * configuration properties. This container holds interpreted configuration data to be used when
 * authenticating users using Kerberos.
 * <p>
 * If Kerberos authentication is not enabled for Ambari <code>{@link #kerberosAuthenticationEnabled} == false</code>,
 * then there is no guarantee that any other property in this container is valid.
 */
public class AmbariKerberosAuthenticationProperties {

  /**
   * A boolean value indicating whether Kerberos authentication is enabled in Ambari (<code>true</code>)
   * or not (<code>false</code>).
   */
  private boolean kerberosAuthenticationEnabled = false;

  /**
   * The SPNEGO principal name
   */
  private String spnegoPrincipalName = null;

  /**
   * The (absolute) path to the SPNEGO keytab file
   */
  private String spnegoKeytabFilePath = null;

  /**
   * A list of {@link UserAuthenticationType}s in order of preference for use when looking up user accounts in the
   * Ambari database
   */
  private List<UserAuthenticationType> orderedUserTypes = Collections.emptyList();

  /**
   * Auth-to-local rules to use to feed to an auth-to-local rules processor used to translate
   * principal names to local user names.
   */
  private String authToLocalRules;

  /**
   * Get whether Kerberos authentication is enabled or not.
   *
   * @return <code>true</code> if Kerberos authentication is enabled; otherwise <code>false</code>
   */
  public boolean isKerberosAuthenticationEnabled() {
    return kerberosAuthenticationEnabled;
  }

  /**
   * Sets whether Kerberos authentication is enabled or not.
   *
   * @param kerberosAuthenticationEnabled <code>true</code> if Kerberos authentication is enabled; otherwise <code>false</code>
   */
  public void setKerberosAuthenticationEnabled(boolean kerberosAuthenticationEnabled) {
    this.kerberosAuthenticationEnabled = kerberosAuthenticationEnabled;
  }

  /**
   * Gets the configured SPNEGO principal name. This may be <code>null</code> if Kerberos
   * authentication is not enabled.
   *
   * @return the SPNEGO principal name or <code>null</code> if Kerberos authentication is not enabled
   */
  public String getSpnegoPrincipalName() {
    return spnegoPrincipalName;
  }

  /**
   * Sets the configured SPNEGO principal name.
   *
   * @param spnegoPrincipalName a principal name
   */
  public void setSpnegoPrincipalName(String spnegoPrincipalName) {
    this.spnegoPrincipalName = spnegoPrincipalName;
  }

  /**
   * Gets the configured SPNEGO keytab file path. This may be <code>null</code> if Kerberos
   * authentication is not enabled.
   *
   * @return the SPNEGO keytab file path or <code>null</code> if Kerberos authentication is not enabled
   */
  public String getSpnegoKeytabFilePath() {
    return spnegoKeytabFilePath;
  }

  /**
   * Sets the configured SPNEGO keytab file path.
   *
   * @param spnegoKeytabFilePath a keytab file path
   */
  public void setSpnegoKeytabFilePath(String spnegoKeytabFilePath) {
    this.spnegoKeytabFilePath = spnegoKeytabFilePath;
  }

  /**
   * Gets the list of {@link UserAuthenticationType}s (in preference order) to use to look up uer accounts in the Ambari database.
   *
   * @return a list of {@link UserAuthenticationType}s
   */
  public List<UserAuthenticationType> getOrderedUserTypes() {
    return orderedUserTypes;
  }

  /**
   * Gets the configured auth-to-local rule set. This may be <code>null</code> if Kerberos
   * authentication is not enabled.
   *
   * @return a string representing an auth-to-local rule set or <code>null</code> if Kerberos authentication is not enabled
   */
  public String getAuthToLocalRules() {
    return authToLocalRules;
  }

  /**
   * Sets the configured auth-to-local rule set.
   *
   * @param authToLocalRules a string representing an auth-to-local rule set
   */
  public void setAuthToLocalRules(String authToLocalRules) {
    this.authToLocalRules = authToLocalRules;
  }
}
