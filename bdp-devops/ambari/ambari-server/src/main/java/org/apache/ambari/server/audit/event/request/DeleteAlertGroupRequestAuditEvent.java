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

/***
 * Audit event for alert group deletion
 */
@Immutable
public class DeleteAlertGroupRequestAuditEvent extends RequestAuditEvent {

  public static class DeleteAlertGroupRequestAuditEventBuilder extends RequestAuditEventBuilder<DeleteAlertGroupRequestAuditEvent, DeleteAlertGroupRequestAuditEventBuilder> {

    /**
     * Alert group id
     */
    private String id;

    public DeleteAlertGroupRequestAuditEventBuilder() {
      super(DeleteAlertGroupRequestAuditEventBuilder.class);
      super.withOperation("Alert group removal");
    }

    @Override
    protected DeleteAlertGroupRequestAuditEvent newAuditEvent() {
      return new DeleteAlertGroupRequestAuditEvent(this);
    }

    /**
     * Appends to the event the details of the incoming request.
     *
     * @param builder builder for the audit event details.
     */
    @Override
    protected void buildAuditMessage(StringBuilder builder) {
      super.buildAuditMessage(builder);

      builder.append(", Alert group ID(")
        .append(id)
        .append(")");
    }

    public DeleteAlertGroupRequestAuditEventBuilder withId(String id) {
      this.id = id;
      return this;
    }
  }

  protected DeleteAlertGroupRequestAuditEvent() {
  }

  /**
   * {@inheritDoc}
   */
  protected DeleteAlertGroupRequestAuditEvent(DeleteAlertGroupRequestAuditEventBuilder builder) {
    super(builder);
  }

  /**
   * Returns an builder for {@link DeleteAlertGroupRequestAuditEvent}
   *
   * @return a builder instance
   */
  public static DeleteAlertGroupRequestAuditEventBuilder builder() {
    return new DeleteAlertGroupRequestAuditEventBuilder();
  }

}
