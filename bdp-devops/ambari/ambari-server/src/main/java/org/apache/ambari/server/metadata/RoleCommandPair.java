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
package org.apache.ambari.server.metadata;

import java.util.Objects;

import org.apache.ambari.server.Role;
import org.apache.ambari.server.RoleCommand;

/**
 * Tuple object containing a Role and RoleCommand.
 */
public class RoleCommandPair {

  private final Role role;
  private final RoleCommand cmd;

  public RoleCommandPair(Role _role, RoleCommand _cmd) {
    if (_role == null || _cmd == null) {
      throw new IllegalArgumentException("role = "+_role+", cmd = "+_cmd);
    }
    this.role = _role;
    this.cmd = _cmd;
  }

  @Override
  public int hashCode() {
    return Objects.hash(role, cmd);
  }

  @Override
  public boolean equals(Object other) {
    if (other != null && (other instanceof RoleCommandPair)
      && ((RoleCommandPair) other).role.equals(role)
      && ((RoleCommandPair) other).cmd.equals(cmd)) {
      return true;
    }
    return false;
  }

  Role getRole() {
    return role;
  }

  RoleCommand getCmd() {
    return cmd;
  }

  @Override
  public String toString() {
    return "RoleCommandPair{" + "role=" + role + ", cmd=" + cmd + '}';
  }
}
