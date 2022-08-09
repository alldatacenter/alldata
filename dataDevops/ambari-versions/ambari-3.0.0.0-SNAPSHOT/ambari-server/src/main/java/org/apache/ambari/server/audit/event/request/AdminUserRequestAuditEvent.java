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
 * Audit event for setting or unsetting a user as admin
 */
@Immutable
public class AdminUserRequestAuditEvent extends RequestAuditEvent {

  public static class AdminUserRequestAuditEventBuilder extends RequestAuditEventBuilder<AdminUserRequestAuditEvent, AdminUserRequestAuditEventBuilder> {

    /**
     * Admin is set or not
     */
    private boolean admin;

    /**
     * Name of the user that is set or unset as admin
     */
    private String username;

    public AdminUserRequestAuditEventBuilder() {
      super(AdminUserRequestAuditEventBuilder.class);
      super.withOperation("Set user admin");
    }

    @Override
    protected AdminUserRequestAuditEvent newAuditEvent() {
      return new AdminUserRequestAuditEvent(this);
    }

    /**
     * Appends to the event the details of the incoming request.
     *
     * @param builder builder for the audit event details.
     */
    @Override
    protected void buildAuditMessage(StringBuilder builder) {
      super.buildAuditMessage(builder);

      builder
        .append(", Affeted username(")
        .append(username)
        .append("), ")
        .append("Administrator(")
        .append(admin ? "yes" : "no")
        .append(")");
    }

    public AdminUserRequestAuditEventBuilder withAdmin(boolean admin) {
      this.admin = admin;
      return this;
    }

    public AdminUserRequestAuditEventBuilder withAffectedUsername(String username) {
      this.username = username;
      return this;
    }

  }

  protected AdminUserRequestAuditEvent() {
  }

  /**
   * {@inheritDoc}
   */
  protected AdminUserRequestAuditEvent(AdminUserRequestAuditEventBuilder builder) {
    super(builder);
  }

  /**
   * Returns an builder for {@link AdminUserRequestAuditEvent}
   *
   * @return a builder instance
   */
  public static AdminUserRequestAuditEventBuilder builder() {
    return new AdminUserRequestAuditEventBuilder();
  }

}
