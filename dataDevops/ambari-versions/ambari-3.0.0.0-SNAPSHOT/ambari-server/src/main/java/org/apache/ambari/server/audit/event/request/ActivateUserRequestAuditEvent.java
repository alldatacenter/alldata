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
 * Audit event for setting a user active or inactive
 */
@Immutable
public class ActivateUserRequestAuditEvent extends RequestAuditEvent {

  public static class ActivateUserRequestAuditEventBuilder extends RequestAuditEventBuilder<ActivateUserRequestAuditEvent, ActivateUserRequestAuditEventBuilder> {

    /**
     * Active or inactive
     */
    private boolean active;

    /**
     * Name of the user to set active or inactive
     */
    private String username;

    public ActivateUserRequestAuditEventBuilder() {
      super(ActivateUserRequestAuditEventBuilder.class);
      super.withOperation("Set user active/inactive");
    }

    @Override
    protected ActivateUserRequestAuditEvent newAuditEvent() {
      return new ActivateUserRequestAuditEvent(this);
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
        .append(", Affected username(")
        .append(username)
        .append("), ")
        .append("Active(")
        .append(active ? "yes" : "no")
        .append(")");
    }

    public ActivateUserRequestAuditEventBuilder withActive(boolean active) {
      this.active = active;
      return this;
    }

    public ActivateUserRequestAuditEventBuilder withAffectedUsername(String username) {
      this.username = username;
      return this;
    }

  }

  protected ActivateUserRequestAuditEvent() {
  }

  /**
   * {@inheritDoc}
   */
  protected ActivateUserRequestAuditEvent(ActivateUserRequestAuditEventBuilder builder) {
    super(builder);
  }

  /**
   * Returns an builder for {@link ActivateUserRequestAuditEvent}
   *
   * @return a builder instance
   */
  public static ActivateUserRequestAuditEventBuilder builder() {
    return new ActivateUserRequestAuditEventBuilder();
  }

}
