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

import java.util.List;

/**
 * LDAP sync specification entity.
 */
public class LdapSyncSpecEntity {

  /**
   * The principal type.
   */
  private PrincipalType principalType;

  /**
   * The sync type.
   */
  private SyncType syncType;

  /**
   * The list of principal names.
   */
  private List<String> principalNames;

  /**
   * A Boolean value indicating whether to (re)exectue the post user creation hook on previously
   * existing users (if the post user creation hook feature has been enabled)
   */
  private boolean postProcessExistingUsers;

  // ----- Constructors ------------------------------------------------------

  /**
   * Construct an LdapSyncSpecEntity.
   *
   * @param principalType            the principal type
   * @param syncType                 the sync type
   * @param principalNames           the list of principal names; may not be null
   * @param postProcessExistingUsers true, to process existing users; false, otherwise
   */
  public LdapSyncSpecEntity(PrincipalType principalType, SyncType syncType, List<String> principalNames, boolean postProcessExistingUsers) {
    this.principalType  = principalType;
    this.syncType       = syncType;
    this.principalNames = principalNames;
    this.postProcessExistingUsers = postProcessExistingUsers;

    assert principalNames != null;

    if (syncType == SyncType.SPECIFIC) {
      if (principalNames.isEmpty()) {
        throw new IllegalArgumentException("Missing principal names for " + syncType + " sync-type.");
      }
    } else {
      if (!principalNames.isEmpty()) {
        throw new IllegalArgumentException("Principal names should not be specified for " + syncType + " sync-type.");
      }
    }
  }


  // ----- LdapSyncSpecEntity ------------------------------------------------

  /**
   * Get the principal type.
   *
   * @return the principal type
   */
  public PrincipalType getPrincipalType() {
    return principalType;
  }

  /**
   * Get the sync type.
   *
   * @return the sync type
   */
  public SyncType getSyncType() {
    return syncType;
  }

  /**
   * Get a list of principal names.
   *
   * @return the list of principal names.
   */
  public List<String> getPrincipalNames() {
    return principalNames;
  }

  /**
   * Gets whether to execute the post user creation hook on previously existing users
   * (if the post user creation hook feature has been enabled), on not.
   *
   * @return true, to process existing users; false, otherwise
   */
  public boolean getPostProcessExistingUsers() {
    return postProcessExistingUsers;
  }


  // ----- enum : PrincipalType ----------------------------------------------

  /**
   * LDAP sync principal type.
   */
  public enum PrincipalType {
    USERS,
    GROUPS;

    /**
     * Get the enum value for the given principal type name string, ignoring case.
     *
     * @param type  the principal type name
     *
     * @return the enum value for the given type name
     */
    public static PrincipalType valueOfIgnoreCase(String type) {
      return valueOf(type.toUpperCase());
    }
  }


  // ----- enum : SyncType ---------------------------------------------------

  /**
   * LDAP sync type.
   */
  public enum SyncType {
    ALL,       // sync all principals
    EXISTING,  // sync only principals that currently exist in Ambari
    SPECIFIC;  // sync only the named principals

    /**
     * Get the enum value for the given sync type name string, ignoring case.
     *
     * @param type  the sync type name
     *
     * @return the enum value for the given type name
     */
    public static SyncType valueOfIgnoreCase(String type) {
      return valueOf(type.toUpperCase());
    }
  }
}
