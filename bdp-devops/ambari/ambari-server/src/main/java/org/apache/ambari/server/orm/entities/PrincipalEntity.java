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

import java.util.HashSet;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinColumns;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.TableGenerator;

/**
 * Represents an admin principal.
 */
@Table(name = "adminprincipal")
@Entity
@TableGenerator(name = "principal_id_generator",
    table = "ambari_sequences", pkColumnName = "sequence_name", valueColumnName = "sequence_value"
    , pkColumnValue = "principal_id_seq"
    , initialValue = 100
    , allocationSize = 500
)
@NamedQueries({
  @NamedQuery(name = "principalByPrivilegeId", query = "SELECT principal FROM PrincipalEntity principal JOIN principal.privileges privilege WHERE privilege.permission.id=:permission_id"),
  @NamedQuery(name = "principalByPrincipalType", query = "SELECT principal FROM PrincipalEntity principal WHERE principal.principalType.name = :principal_type")
})
public class PrincipalEntity {

  /**
   * The type id.
   */
  @Id
  @Column(name = "principal_id")
  @GeneratedValue(strategy = GenerationType.TABLE, generator = "principal_id_generator")
  private Long id;

  @ManyToOne
  @JoinColumns({
      @JoinColumn(name = "principal_type_id", referencedColumnName = "principal_type_id", nullable = false)
  })
  private PrincipalTypeEntity principalType;

  @OneToMany(mappedBy = "principal")
  private Set<PrivilegeEntity> privileges = new HashSet<>();


  // ----- PrincipalEntity ---------------------------------------------------

  /**
   * Get the principal type id.
   *
   * @return the principal type id.
   */
  public Long getId() {
    return id;
  }

  /**
   * Set the principal id.
   *
   * @param id the type id.
   */
  public void setId(Long id) {
    this.id = id;
  }

  /**
   * Get the principal type entity.
   *
   * @return the principal type entity
   */
  public PrincipalTypeEntity getPrincipalType() {
    return principalType;
  }

  /**
   * Set the principal type entity.
   *
   * @param principalType the principal type entity
   */
  public void setPrincipalType(PrincipalTypeEntity principalType) {
    this.principalType = principalType;
  }

  /**
   * Get the principal privileges.
   *
   * @return the principal privileges
   */
  public Set<PrivilegeEntity> getPrivileges() {
    return privileges;
  }

  /**
   * Set the principal privileges.
   *
   * @param privileges the principal privileges
   */
  public void setPrivileges(Set<PrivilegeEntity> privileges) {
    this.privileges = privileges;
  }

  /**
   * Remove a privilege.
   *
   * @param privilege  the privilege entity
   */
  public void removePrivilege(PrivilegeEntity privilege) {
    privileges.remove(privilege);
  }

  // ----- Object overrides --------------------------------------------------

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    PrincipalEntity that = (PrincipalEntity) o;

    return id.equals(that.id) && !(principalType != null ?
        !principalType.equals(that.principalType) : that.principalType != null);
  }

  @Override
  public int hashCode() {
    int result = id.hashCode();
    result = 31 * result + (principalType != null ? principalType.hashCode() : 0);
    return result;
  }
}
