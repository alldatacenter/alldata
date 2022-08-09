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
 * Audit event for creating a user
 */
@Immutable
public class CreateUserRequestAuditEvent extends RequestAuditEvent {

  public static class CreateUserRequestAuditEventBuilder extends RequestAuditEventBuilder<CreateUserRequestAuditEvent, CreateUserRequestAuditEventBuilder> {

    /**
     * Is the user admin
     */
    private boolean admin;

    /**
     * Is the user active
     */
    private boolean active;

    /**
     * Username
     */
    private String username;

    public CreateUserRequestAuditEventBuilder() {
      super(CreateUserRequestAuditEventBuilder.class);
      super.withOperation("User creation");
    }

    @Override
    protected CreateUserRequestAuditEvent newAuditEvent() {
      return new CreateUserRequestAuditEvent(this);
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
        .append(", Created Username(")
        .append(username)
        .append("), Active(")
        .append(active ? "yes" : "no")
        .append("), ")
        .append("Administrator(")
        .append(admin ? "yes" : "no")
        .append(")");
    }

    public CreateUserRequestAuditEventBuilder withAdmin(boolean admin) {
      this.admin = admin;
      return this;
    }

    public CreateUserRequestAuditEventBuilder withActive(boolean active) {
      this.active = active;
      return this;
    }

    public CreateUserRequestAuditEventBuilder withCreatedUsername(String username) {
      this.username = username;
      return this;
    }

  }

  protected CreateUserRequestAuditEvent() {
  }

  /**
   * {@inheritDoc}
   */
  protected CreateUserRequestAuditEvent(CreateUserRequestAuditEventBuilder builder) {
    super(builder);
  }

  /**
   * Returns an builder for {@link CreateUserRequestAuditEvent}
   *
   * @return a builder instance
   */
  public static CreateUserRequestAuditEventBuilder builder() {
    return new CreateUserRequestAuditEventBuilder();
  }

}
