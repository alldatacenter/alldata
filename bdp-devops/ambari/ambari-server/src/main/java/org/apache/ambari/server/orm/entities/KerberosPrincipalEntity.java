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

package org.apache.ambari.server.orm.entities;

import java.util.ArrayList;
import java.util.Collection;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.Table;

/**
 * Entity representing a KerberosPrincipal.
 * <p/>
 * Each KerberosPrincipal is related to a host, therefore there may be several entities with the same
 * principal name, but related to different hosts.
 */
@Entity
@Table(name = "kerberos_principal")
@NamedQueries({
    @NamedQuery(name = "KerberosPrincipalEntityFindAll",
        query = "SELECT kp FROM KerberosPrincipalEntity kp")
})
public class KerberosPrincipalEntity {

  @Id
  @Column(name = "principal_name", insertable = true, updatable = false, nullable = false)
  private String principalName = null;

  @Column(name = "is_service", insertable = true, updatable = false, nullable = false)
  private Integer service = 1;

  @Column(name = "cached_keytab_path", insertable = true, updatable = true, nullable = true)
  private String cachedKeytabPath = null;

  @OneToMany(mappedBy = "kerberosPrincipalEntity", cascade = CascadeType.REMOVE, fetch = FetchType.EAGER)
  private Collection<KerberosKeytabPrincipalEntity> kerberosKeytabPrincipalEntities = new ArrayList<>();

  /**
   * Constructs an empty KerberosPrincipalEntity
   */
  public KerberosPrincipalEntity() {
  }

  /**
   * Constructs a new KerberosPrincipalEntity
   *
   * @param principalName    a String declaring the principal name
   * @param service          a boolean value indicating whether the principal is a service principal (true) or not (false)
   * @param cachedKeytabPath a String declaring the location of the related cached keytab
   */
  public KerberosPrincipalEntity(String principalName, boolean service, String cachedKeytabPath) {
    setPrincipalName(principalName);
    setService(service);
    setCachedKeytabPath(cachedKeytabPath);
  }

  /**
   * Gets the principal name for this KerberosPrincipalEntity
   *
   * @return a String indicating this KerberosPrincipalEntity's principal name
   */
  public String getPrincipalName() {
    return principalName;
  }

  /**
   * Sets the principal name for this KerberosPrincipalEntity
   *
   * @param principalName a String indicating this KerberosPrincipalEntity's principal name
   */
  public void setPrincipalName(String principalName) {
    this.principalName = principalName;
  }

  /**
   * Indicates whether this KerberosPrincipalEntity represents a service principal (true) or not (false)
   *
   * @return true if this KerberosPrincipalEntity represents a service principal; otherwise false
   */
  public boolean isService() {
    return (service == 1);
  }

  /**
   * Set whether this KerberosPrincipalEntity represents a service principal (true) or not (false)
   *
   * @param service true if this KerberosPrincipalEntity represents a service principal; otherwise false
   */
  public void setService(boolean service) {
    this.service = (service) ? 1 : 0;
  }

  /**
   * Gets the location of the cached keytab file, if one exists
   *
   * @return a String the location of the cached keytab file, or not if one does not exist
   */
  public String getCachedKeytabPath() {
    return cachedKeytabPath;
  }

  /**
   * Sets the location of the cached keytab file, if one exists
   *
   * @param cachedKeytabPath a String the location of the cached keytab file, or not if one does not exist
   */
  public void setCachedKeytabPath(String cachedKeytabPath) {
    this.cachedKeytabPath = cachedKeytabPath;
  }

  public Collection<KerberosKeytabPrincipalEntity> getKerberosKeytabPrincipalEntities() {
    return kerberosKeytabPrincipalEntities;
  }

  public void setKerberosKeytabPrincipalEntities(Collection<KerberosKeytabPrincipalEntity> kerberosKeytabPrincipalEntities) {
    this.kerberosKeytabPrincipalEntities = kerberosKeytabPrincipalEntities;
  }

  public void addKerberosKeytabPrincipal(KerberosKeytabPrincipalEntity kerberosKeytabPrincipalEntity) {
    if (!kerberosKeytabPrincipalEntities.contains(kerberosKeytabPrincipalEntity)) {
      kerberosKeytabPrincipalEntities.add(kerberosKeytabPrincipalEntity);
    }
  }
}
