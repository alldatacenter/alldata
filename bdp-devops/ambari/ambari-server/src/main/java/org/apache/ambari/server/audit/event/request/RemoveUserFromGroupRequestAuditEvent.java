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
 * Audit event for removing a user from a group
 */
@Immutable
public class RemoveUserFromGroupRequestAuditEvent extends RequestAuditEvent {

  public static class AddUserToGroupRequestAuditEventBuilder extends RequestAuditEventBuilder<RemoveUserFromGroupRequestAuditEvent, AddUserToGroupRequestAuditEventBuilder> {

    /**
     * Group name to remove from
     */
    private String groupName;

    /**
     * Name of the user that is removed
     */
    private String affectedUserName;

    public AddUserToGroupRequestAuditEventBuilder() {
      super(AddUserToGroupRequestAuditEventBuilder.class);
      super.withOperation("User removal from group");
    }

    @Override
    protected RemoveUserFromGroupRequestAuditEvent newAuditEvent() {
      return new RemoveUserFromGroupRequestAuditEvent(this);
    }

    /**
     * Appends to the event the details of the incoming request.
     *
     * @param builder builder for the audit event details.
     */
    @Override
    protected void buildAuditMessage(StringBuilder builder) {
      super.buildAuditMessage(builder);

      builder.append(", Group(");
      builder.append(groupName);
      builder.append("), Affected username(");
      builder.append(affectedUserName);
      builder.append(")");
    }

    public AddUserToGroupRequestAuditEventBuilder withGroupName(String groupName) {
      this.groupName = groupName;
      return this;
    }

    public AddUserToGroupRequestAuditEventBuilder withAffectedUserName(String userName) {
      this.affectedUserName = userName;
      return this;
    }
  }

  protected RemoveUserFromGroupRequestAuditEvent() {
  }

  /**
   * {@inheritDoc}
   */
  protected RemoveUserFromGroupRequestAuditEvent(AddUserToGroupRequestAuditEventBuilder builder) {
    super(builder);
  }

  /**
   * Returns an builder for {@link RemoveUserFromGroupRequestAuditEvent}
   *
   * @return a builder instance
   */
  public static AddUserToGroupRequestAuditEventBuilder builder() {
    return new AddUserToGroupRequestAuditEventBuilder();
  }

}
