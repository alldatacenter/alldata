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
package org.apache.ambari.server.controller;

/**
 * The update configuration policies
 * <ul>
 * <li>NONE - No configurations will be updated</li>
 * <li>IDENTITIES_ONLY - New and updated configurations related to Kerberos identity information - principal, keytab file, and auth-to-local rule properties</li>
 * <li>NEW_AND_IDENTITIES - Only new configurations declared by the Kerberos descriptor and stack advisor as well as the identity-related changes</li>
 * <li>ALL - All configuration changes (default)</li>
 * </ul>
 */
public enum UpdateConfigurationPolicy {
  /**
   * No configurations will be updated
   */
  NONE(false, false, false, false),

  /**
   * New and updated configurations related to Kerberos identity information - principal, keytab
   * file, and auth-to-local rule properties
   */
  IDENTITIES_ONLY(false, true, false, false),

  /**
   * Only new configurations declared by the Kerberos descriptor and stack advisor as well as the
   * identity-related changes
   */
  NEW_AND_IDENTITIES(true, true, true, false),

  /**
   * All configuration changes (default)
   */
  ALL(true, true, true, true);

  private final boolean invokeStackAdvisor;
  private final boolean applyIdentityChanges;
  private final boolean applyAdditions;
  private final boolean applyOtherChanges;

  UpdateConfigurationPolicy(boolean invokeStackAdvisor, boolean applyIdentityChanges, boolean applyAdditions, boolean applyOtherChanges) {
    this.invokeStackAdvisor = invokeStackAdvisor;
    this.applyIdentityChanges = applyIdentityChanges;
    this.applyAdditions = applyAdditions;
    this.applyOtherChanges = applyOtherChanges;
  }

  public boolean invokeStackAdvisor() {
    return invokeStackAdvisor;
  }

  public boolean applyIdentityChanges() {
    return applyIdentityChanges;
  }

  public boolean applyAdditions() {
    return applyAdditions;
  }

  public boolean applyOtherChanges() {
    return applyOtherChanges;
  }

  /**
   * Safely translates a {@link UpdateConfigurationPolicy} value to a {@link String} of all
   * lowercase characters.
   *
   * @param value the value to translate
   * @return <code>null</code> if the input is <code>null</code>; otherwise the String value of
   * the policy enum converted to lowercase characters.
   */
  public static String translate(UpdateConfigurationPolicy value) {
    return (value == null) ? null : value.name().toLowerCase();
  }

  /**
   * Safely translates a {@link String} value to an {@link UpdateConfigurationPolicy}.
   * <p>
   * The input value will be trimmed and converted to all uppercase characters.  If "-"'s are used
   * instead of "_"'s, they will be converted.
   *
   * @param stringValue the String to translate
   * @return The translated {@link UpdateConfigurationPolicy} value; or <code>null</code> if a translation cannot be made
   */
  public static UpdateConfigurationPolicy translate(String stringValue) {
    if (stringValue != null) {
      stringValue = stringValue.trim().toUpperCase();

      if (!stringValue.isEmpty()) {
        try {
          return valueOf(stringValue.replace('-', '_'));
        } catch (IllegalArgumentException e) {
          // ignore this and return null later...
        }
      }
    }

    return null;
  }
}
