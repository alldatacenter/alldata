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
 * Audit event for deleting a user
 */
@Immutable
public class DeleteUserRequestAuditEvent extends RequestAuditEvent {

  public static class DeleteUserRequestAuditEventBuilder extends RequestAuditEventBuilder<DeleteUserRequestAuditEvent, DeleteUserRequestAuditEventBuilder> {

    /**
     * Name of the deleted user
     */
    private String username;

    public DeleteUserRequestAuditEventBuilder() {
      super(DeleteUserRequestAuditEventBuilder.class);
      super.withOperation("User delete");
    }

    @Override
    protected DeleteUserRequestAuditEvent newAuditEvent() {
      return new DeleteUserRequestAuditEvent(this);
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
        .append(", Deleted Username(")
        .append(username)
        .append(")");
    }

    public DeleteUserRequestAuditEventBuilder withDeletedUsername(String username) {
      this.username = username;
      return this;
    }

  }

  protected DeleteUserRequestAuditEvent() {
  }

  /**
   * {@inheritDoc}
   */
  protected DeleteUserRequestAuditEvent(DeleteUserRequestAuditEventBuilder builder) {
    super(builder);
  }

  /**
   * Returns an builder for {@link DeleteUserRequestAuditEvent}
   *
   * @return a builder instance
   */
  public static DeleteUserRequestAuditEventBuilder builder() {
    return new DeleteUserRequestAuditEventBuilder();
  }

}
