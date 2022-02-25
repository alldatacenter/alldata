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

import javax.annotation.concurrent.Immutable;

import org.apache.ambari.server.audit.request.RequestAuditEvent;

/**
 * Audit event for privilege change
 */
@Immutable
public class PrivilegeChangeRequestAuditEvent extends RequestAuditEvent {

  public static class PrivilegeChangeRequestAuditEventBuilder extends RequestAuditEventBuilder<PrivilegeChangeRequestAuditEvent, PrivilegeChangeRequestAuditEventBuilder> {

    /**
     * User name (this or group is set)
     */
    private String user;

    /**
     * Group name (this or user is set)
     */
    private String group;

    /**
     * Role for the user or group
     */
    private String role;

    public PrivilegeChangeRequestAuditEventBuilder() {
      super(PrivilegeChangeRequestAuditEventBuilder.class);
      super.withOperation("Role change");
    }

    @Override
    protected PrivilegeChangeRequestAuditEvent newAuditEvent() {
      return new PrivilegeChangeRequestAuditEvent(this);
    }

    /**
     * Appends to the event the details of the incoming request.
     *
     * @param builder builder for the audit event details.
     */
    @Override
    protected void buildAuditMessage(StringBuilder builder) {
      super.buildAuditMessage(builder);

      builder.append(", Role(")
        .append(role)
        .append(")");

      if (user != null) {
        builder.append(", User(").append(user).append(")");
      }
      if (group != null) {
        builder.append(", Group(").append(group).append(")");
      }
    }

    public PrivilegeChangeRequestAuditEventBuilder withUser(String user) {
      this.user = user;
      return this;
    }

    public PrivilegeChangeRequestAuditEventBuilder withGroup(String group) {
      this.group = group;
      return this;
    }

    public PrivilegeChangeRequestAuditEventBuilder withRole(String role) {
      this.role = role;
      return this;
    }
  }

  protected PrivilegeChangeRequestAuditEvent() {
  }

  /**
   * {@inheritDoc}
   */
  protected PrivilegeChangeRequestAuditEvent(PrivilegeChangeRequestAuditEventBuilder builder) {
    super(builder);
  }

  /**
   * Returns an builder for {@link PrivilegeChangeRequestAuditEvent}
   *
   * @return a builder instance
   */
  public static PrivilegeChangeRequestAuditEventBuilder builder() {
    return new PrivilegeChangeRequestAuditEventBuilder();
  }

}
