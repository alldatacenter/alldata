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
 * Audit event for updating an upgrade item
 */
@Immutable
public class UpdateUpgradeItemRequestAuditEvent extends RequestAuditEvent {

  public static class UpdateUpgradeItemRequestAuditEventBuilder extends RequestAuditEventBuilder<UpdateUpgradeItemRequestAuditEvent, UpdateUpgradeItemRequestAuditEventBuilder> {

    /**
     * Stage id
     */
    private String stageId;

    /**
     * Status
     */
    private String status;

    /**
     * Request id
     */
    private String requestId;


    public UpdateUpgradeItemRequestAuditEventBuilder() {
      super(UpdateUpgradeItemRequestAuditEventBuilder.class);
      super.withOperation("Action confirmation by the user");
    }

    @Override
    protected UpdateUpgradeItemRequestAuditEvent newAuditEvent() {
      return new UpdateUpgradeItemRequestAuditEvent(this);
    }

    /**
     * Appends to the event the details of the incoming request.
     *
     * @param builder builder for the audit event details.
     */
    @Override
    protected void buildAuditMessage(StringBuilder builder) {
      super.buildAuditMessage(builder);

      builder.append(", Stage id(")
        .append(stageId)
        .append("), Status(")
        .append(status)
        .append("), Request id(")
        .append(requestId)
        .append(")");
    }

    public UpdateUpgradeItemRequestAuditEventBuilder withStageId(String stageId) {
      this.stageId = stageId;
      return this;
    }

    public UpdateUpgradeItemRequestAuditEventBuilder withStatus(String status) {
      this.status = status;
      return this;
    }

    public UpdateUpgradeItemRequestAuditEventBuilder withRequestId(String requestId) {
      this.requestId = requestId;
      return this;
    }
  }

  protected UpdateUpgradeItemRequestAuditEvent() {
  }

  /**
   * {@inheritDoc}
   */
  protected UpdateUpgradeItemRequestAuditEvent(UpdateUpgradeItemRequestAuditEventBuilder builder) {
    super(builder);
  }

  /**
   * Returns an builder for {@link UpdateUpgradeItemRequestAuditEvent}
   *
   * @return a builder instance
   */
  public static UpdateUpgradeItemRequestAuditEventBuilder builder() {
    return new UpdateUpgradeItemRequestAuditEventBuilder();
  }

}
