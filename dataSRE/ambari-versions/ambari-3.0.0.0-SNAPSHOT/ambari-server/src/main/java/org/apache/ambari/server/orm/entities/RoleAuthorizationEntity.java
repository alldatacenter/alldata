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


import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;

/**
 * Represents an authorization (typically assigned to a permission)
 */
@Table(name = "roleauthorization")
@Entity
@NamedQueries({
    @NamedQuery(name = "findAll", query = "SELECT a FROM RoleAuthorizationEntity a")
})
public class RoleAuthorizationEntity {

  /**
   * The authorization id.
   */
  @Id
  @Column(name = "authorization_id")
  private String authorizationId;


  /**
   * The authorization name.
   */
  @Column(name = "authorization_name")
  private String authorizationName;

  // ----- RoleAuthorizationEntity ---------------------------------------------------

  /**
   * Get the authorization id.
   *
   * @return the authorization id.
   */
  public String getAuthorizationId() {
    return authorizationId;
  }

  /**
   * Set the authorization id.
   *
   * @param authorizationId the type id.
   */
  public void setAuthorizationId(String authorizationId) {
    this.authorizationId = authorizationId;
  }

  /**
   * Get the authorization name.
   *
   * @return the authorization name
   */
  public String getAuthorizationName() {
    return authorizationName;
  }

  /**
   * Set the authorization name.
   *
   * @param authorizationName the authorization name
   */
  public void setAuthorizationName(String authorizationName) {
    this.authorizationName = authorizationName;
  }

  // ----- Object overrides --------------------------------------------------

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    RoleAuthorizationEntity that = (RoleAuthorizationEntity) o;

    return !(authorizationId != null ? !authorizationId.equals(that.authorizationId) : that.authorizationId != null) &&
        !(authorizationName != null ? !authorizationName.equals(that.authorizationName) : that.authorizationName != null);
  }

  @Override
  public int hashCode() {
    int result = authorizationId != null ? authorizationId.hashCode() : 0;
    result = 31 * result + (authorizationName != null ? authorizationName.hashCode() : 0);
    return result;
  }
}
