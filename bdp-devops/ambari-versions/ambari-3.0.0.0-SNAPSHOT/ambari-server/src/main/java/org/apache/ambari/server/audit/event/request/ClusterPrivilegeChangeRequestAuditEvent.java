/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ambari.server.audit.event.request;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.annotation.concurrent.Immutable;

import org.apache.ambari.server.audit.request.RequestAuditEvent;
import org.apache.commons.lang.StringUtils;

/**
 * Audit event for changing cluster privilege
 */
@Immutable
public class ClusterPrivilegeChangeRequestAuditEvent extends RequestAuditEvent {

  public static class ClusterPrivilegeChangeRequestAuditEventBuilder extends RequestAuditEventBuilder<ClusterPrivilegeChangeRequestAuditEvent, ClusterPrivilegeChangeRequestAuditEventBuilder> {

    /**
     * Roles for users
     * username -> list of roles
     */
    private Map<String, List<String>> users;

    /**
     * Roles for groups
     * group name -> list of roles
     */
    private Map<String, List<String>> groups;

    /**
     * Roles for roles
     * role name -> list of roles
     */
    private Map<String, List<String>> roles;

    public ClusterPrivilegeChangeRequestAuditEventBuilder() {
      super(ClusterPrivilegeChangeRequestAuditEventBuilder.class);
      super.withOperation("Role change");
    }

    @Override
    protected ClusterPrivilegeChangeRequestAuditEvent newAuditEvent() {
      return new ClusterPrivilegeChangeRequestAuditEvent(this);
    }

    /**
     * Appends to the event the details of the incoming request.
     *
     * @param builder builder for the audit event details.
     */
    @Override
    protected void buildAuditMessage(StringBuilder builder) {
      super.buildAuditMessage(builder);

      SortedSet<String> roleSet = new TreeSet<>();
      roleSet.addAll(users.keySet());
      roleSet.addAll(groups.keySet());
      roleSet.addAll(roles.keySet());

      builder.append(", Roles(");
      if (!users.isEmpty() || !groups.isEmpty()|| !roles.isEmpty()) {
        builder.append(System.lineSeparator());
      }

      List<String> lines = new LinkedList<>();

      for (String role : roleSet) {
        lines.add(role + ": ");
        if (users.get(role) != null && !users.get(role).isEmpty()) {
          lines.add("  Users: " + StringUtils.join(users.get(role), ", "));
        }
        if (groups.get(role) != null && !groups.get(role).isEmpty()) {
          lines.add("  Groups: " + StringUtils.join(groups.get(role), ", "));
        }
        if (roles.get(role) != null && !roles.get(role).isEmpty()) {
          lines.add("  Roles: " + StringUtils.join(roles.get(role), ", "));
        }
      }

      builder.append(StringUtils.join(lines, System.lineSeparator()));

      builder.append(")");
    }

    public ClusterPrivilegeChangeRequestAuditEventBuilder withUsers(Map<String, List<String>> users) {
      this.users = users;
      return this;
    }

    public ClusterPrivilegeChangeRequestAuditEventBuilder withGroups(Map<String, List<String>> groups) {
      this.groups = groups;
      return this;
    }

    public ClusterPrivilegeChangeRequestAuditEventBuilder withRoles(Map<String, List<String>> roles) {
      this.roles = roles;
      return this;
    }
  }

  protected ClusterPrivilegeChangeRequestAuditEvent() {
  }

  /**
   * {@inheritDoc}
   */
  protected ClusterPrivilegeChangeRequestAuditEvent(ClusterPrivilegeChangeRequestAuditEventBuilder builder) {
    super(builder);
  }

  /**
   * Returns an builder for {@link ClusterPrivilegeChangeRequestAuditEvent}
   *
   * @return a builder instance
   */
  public static ClusterPrivilegeChangeRequestAuditEventBuilder builder() {
    return new ClusterPrivilegeChangeRequestAuditEventBuilder();
  }

}
