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
package org.apache.ambari.server.controller;

import java.util.Map;

import org.apache.ambari.server.serveraction.kerberos.KDCType;
import org.apache.ambari.server.state.SecurityType;

/**
 * KerberosDetails is a helper class to hold the details of the relevant Kerberos-specific
 * configurations so they may be passed around more easily.
 */
public class KerberosDetails {
  private String defaultRealm;
  private KDCType kdcType;
  private Map<String, String> kerberosEnvProperties;
  private SecurityType securityType;
  private Boolean manageIdentities;

  public void setDefaultRealm(String defaultRealm) {
    this.defaultRealm = defaultRealm;
  }

  public String getDefaultRealm() {
    return defaultRealm;
  }

  public void setKdcType(KDCType kdcType) {
    this.kdcType = kdcType;
  }

  public KDCType getKdcType() {
    return kdcType;
  }

  public void setKerberosEnvProperties(Map<String, String> kerberosEnvProperties) {
    this.kerberosEnvProperties = kerberosEnvProperties;
  }

  public Map<String, String> getKerberosEnvProperties() {
    return kerberosEnvProperties;
  }

  public void setSecurityType(SecurityType securityType) {
    this.securityType = securityType;
  }

  public SecurityType getSecurityType() {
    return securityType;
  }

  public boolean manageIdentities() {
    if (manageIdentities == null) {
      return (kerberosEnvProperties == null) ||
        !"false".equalsIgnoreCase(kerberosEnvProperties.get(KerberosHelper.MANAGE_IDENTITIES));
    } else {
      return manageIdentities;
    }
  }

  public void setManageIdentities(Boolean manageIdentities) {
    this.manageIdentities = manageIdentities;
  }

  public boolean createAmbariPrincipal() {
    return (kerberosEnvProperties == null) ||
      !"false".equalsIgnoreCase(kerberosEnvProperties.get(KerberosHelper.CREATE_AMBARI_PRINCIPAL));
  }

  public String getPreconfigureServices() {
    return (kerberosEnvProperties == null) ? "" : kerberosEnvProperties.get(KerberosHelper.PRECONFIGURE_SERVICES);
  }
}