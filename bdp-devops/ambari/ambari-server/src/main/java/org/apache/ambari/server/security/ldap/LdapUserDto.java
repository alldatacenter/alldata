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
package org.apache.ambari.server.security.ldap;

/**
 * Pojo with information about LDAP user.
 */
public class LdapUserDto {
  /**
   * Name of the user. Should be always unique.
   */
  private String userName;

  /**
   * Determines if the LDAP user is synchronized with internal user in database.
   */
  private boolean synced;

  /**
   * Unique identifier from LDAP.
   */
  private String uid;

  /**
   * Distinguished name from LDAP.
   */
  private String dn;

  /**
   * Get the user name.
   *
   * @return the user name
   */
  public String getUserName() {
    return userName;
  }

  /**
   * Set the user name.
   *
   * @param userName the user name
   */
  public void setUserName(String userName) {
    this.userName = userName;
  }

  /**
   * Get the synced flag.
   *
   * @return the synced flag
   */
  public boolean isSynced() {
    return synced;
  }

  /**
   * Set the synced flag
   *
   * @param synced the synced flag
   */
  public void setSynced(boolean synced) {
    this.synced = synced;
  }

  /**
   * Get the UID.
   *
   * @return the UID
   */
  public String getUid() {
    return uid;
  }

  /**
   * Set the UID.
   *
   * @param uid the UID
   */
  public void setUid(String uid) {
    this.uid = uid;
  }

  /**
   * Get the DN.
   *
   * @return the DN
   */
  public String getDn() {
    return dn;
  }

  /**
   * Set the DN.
   *
   * @param dn the DN
   */
  public void setDn(String dn) {
    this.dn = dn;
  }

  @Override
  public int hashCode() {
    int result = userName != null ? userName.hashCode() : 0;
    return result;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    LdapUserDto that = (LdapUserDto) o;

    if (userName != null ? !userName.equals(that.getUserName()) : that.getUserName() != null) return false;

    return true;
  }
}
