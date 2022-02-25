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
 * Audit event for creating a group
 */
@Immutable
public class CreateGroupRequestAuditEvent extends RequestAuditEvent {

  public static class CreateGroupRequestAuditEventBuilder extends RequestAuditEventBuilder<CreateGroupRequestAuditEvent, CreateGroupRequestAuditEventBuilder> {

    /**
     * NAme of the created group
     */
    private String groupName;

    public CreateGroupRequestAuditEventBuilder() {
      super(CreateGroupRequestAuditEventBuilder.class);
      super.withOperation("Group creation");
    }

    @Override
    protected CreateGroupRequestAuditEvent newAuditEvent() {
      return new CreateGroupRequestAuditEvent(this);
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
        .append(", Group(")
        .append(groupName)
        .append(")");
    }

    public CreateGroupRequestAuditEventBuilder withGroupName(String groupName) {
      this.groupName = groupName;
      return this;
    }

  }

  protected CreateGroupRequestAuditEvent() {
  }

  /**
   * {@inheritDoc}
   */
  protected CreateGroupRequestAuditEvent(CreateGroupRequestAuditEventBuilder builder) {
    super(builder);
  }

  /**
   * Returns an builder for {@link CreateGroupRequestAuditEvent}
   *
   * @return a builder instance
   */
  public static CreateGroupRequestAuditEventBuilder builder() {
    return new CreateGroupRequestAuditEventBuilder();
  }

}
