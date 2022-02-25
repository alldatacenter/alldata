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
 * Audit event for changing view privilege
 */
@Immutable
public class ViewPrivilegeChangeRequestAuditEvent extends RequestAuditEvent {

  public static class ViewPrivilegeChangeRequestAuditEventBuilder extends RequestAuditEventBuilder<ViewPrivilegeChangeRequestAuditEvent, ViewPrivilegeChangeRequestAuditEventBuilder> {

    /**
     * Users with their roles
     */
    private Map<String, List<String>> users;

    /**
     * Groups with their roles
     */
    private Map<String, List<String>> groups;

    /**
     * Roles with their roles
     */
    private Map<String, List<String>> roles;

    /**
     * View name
     */
    private String name;

    /**
     * View type (Files, Tez, etc...)
     */
    private String type;

    /**
     * View version
     */
    private String version;


    public ViewPrivilegeChangeRequestAuditEventBuilder() {
      super(ViewPrivilegeChangeRequestAuditEventBuilder.class);
      super.withOperation("View permission change");
    }

    @Override
    protected ViewPrivilegeChangeRequestAuditEvent newAuditEvent() {
      return new ViewPrivilegeChangeRequestAuditEvent(this);
    }

    /**
     * Appends to the event the details of the incoming request.
     *
     * @param builder builder for the audit event details.
     */
    @Override
    protected void buildAuditMessage(StringBuilder builder) {
      super.buildAuditMessage(builder);

      builder.append(", Type(")
        .append(type)
        .append("), Version(")
        .append(version)
        .append("), Name(")
        .append(name)
        .append(")");

      SortedSet<String> roleSet = new TreeSet<>();
      roleSet.addAll(users.keySet());
      roleSet.addAll(groups.keySet());
      roleSet.addAll(roles.keySet());

      builder.append(", Permissions(");
      if (!users.isEmpty() || !groups.isEmpty() || !roles.isEmpty()) {
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

    public ViewPrivilegeChangeRequestAuditEventBuilder withName(String name) {
      this.name = name;
      return this;
    }

    public ViewPrivilegeChangeRequestAuditEventBuilder withType(String type) {
      this.type = type;
      return this;
    }

    public ViewPrivilegeChangeRequestAuditEventBuilder withVersion(String version) {
      this.version = version;
      return this;
    }

    public ViewPrivilegeChangeRequestAuditEventBuilder withUsers(Map<String, List<String>> users) {
      this.users = users;
      return this;
    }

    public ViewPrivilegeChangeRequestAuditEventBuilder withGroups(Map<String, List<String>> groups) {
      this.groups = groups;
      return this;
    }

    public ViewPrivilegeChangeRequestAuditEventBuilder withRoles(Map<String, List<String>> roles) {
      this.roles = roles;
      return this;
    }
  }

  protected ViewPrivilegeChangeRequestAuditEvent() {
  }

  /**
   * {@inheritDoc}
   */
  protected ViewPrivilegeChangeRequestAuditEvent(ViewPrivilegeChangeRequestAuditEventBuilder builder) {
    super(builder);
  }

  /**
   * Returns an builder for {@link ViewPrivilegeChangeRequestAuditEvent}
   *
   * @return a builder instance
   */
  public static ViewPrivilegeChangeRequestAuditEventBuilder builder() {
    return new ViewPrivilegeChangeRequestAuditEventBuilder();
  }

}
