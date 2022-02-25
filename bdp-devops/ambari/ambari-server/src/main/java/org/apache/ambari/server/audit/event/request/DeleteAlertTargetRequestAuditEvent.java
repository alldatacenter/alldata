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
 * Audit event for deleting alert target (=notification)
 */
@Immutable
public class DeleteAlertTargetRequestAuditEvent extends RequestAuditEvent {

  public static class DeleteAlertTargetRequestAuditEventBuilder extends RequestAuditEventBuilder<DeleteAlertTargetRequestAuditEvent, DeleteAlertTargetRequestAuditEventBuilder> {

    /**
     * Alert target id
     */
    private String id;

    public DeleteAlertTargetRequestAuditEventBuilder() {
      super(DeleteAlertTargetRequestAuditEventBuilder.class);
      super.withOperation("Notification removal");
    }

    @Override
    protected DeleteAlertTargetRequestAuditEvent newAuditEvent() {
      return new DeleteAlertTargetRequestAuditEvent(this);
    }

    /**
     * Appends to the event the details of the incoming request.
     *
     * @param builder builder for the audit event details.
     */
    @Override
    protected void buildAuditMessage(StringBuilder builder) {
      super.buildAuditMessage(builder);

      builder.append(", Notification ID(")
        .append(id)
        .append(")");
    }

    public DeleteAlertTargetRequestAuditEventBuilder withId(String id) {
      this.id = id;
      return this;
    }
  }

  protected DeleteAlertTargetRequestAuditEvent() {
  }

  /**
   * {@inheritDoc}
   */
  protected DeleteAlertTargetRequestAuditEvent(DeleteAlertTargetRequestAuditEventBuilder builder) {
    super(builder);
  }

  /**
   * Returns an builder for {@link DeleteAlertTargetRequestAuditEvent}
   *
   * @return a builder instance
   */
  public static DeleteAlertTargetRequestAuditEventBuilder builder() {
    return new DeleteAlertTargetRequestAuditEventBuilder();
  }

}
