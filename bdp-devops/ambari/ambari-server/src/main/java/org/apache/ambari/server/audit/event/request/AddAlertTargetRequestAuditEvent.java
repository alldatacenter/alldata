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

import java.util.List;

import javax.annotation.concurrent.Immutable;

import org.apache.ambari.server.audit.request.RequestAuditEvent;
import org.apache.commons.lang.StringUtils;

/**
 * Audit event for adding alert target (= notification)
 */
@Immutable
public class AddAlertTargetRequestAuditEvent extends RequestAuditEvent {

  public static class AddAlertTargetRequestAuditEventBuilder extends RequestAuditEventBuilder<AddAlertTargetRequestAuditEvent, AddAlertTargetRequestAuditEventBuilder> {

    /**
     * Name of the alert target
     */
    private String name;

    /**
     * Description of the alert target
     */
    private String description;

    /**
     * Type of the alert target
     */
    private String notificationType;

    /**
     * Group list that belongs to the alert target
     */
    private List<String> groupIds;

    /**
     * Email address that should be in the from field
     */
    private String emailFrom;

    /**
     * Eamil recipients
     */
    private List<String> emailRecipients;

    /**
     * Alert states for the alert target
     */
    private List<String> alertStates;

    public AddAlertTargetRequestAuditEventBuilder() {
      super(AddAlertTargetRequestAuditEventBuilder.class);
      super.withOperation("Notification addition");
    }

    @Override
    protected AddAlertTargetRequestAuditEvent newAuditEvent() {
      return new AddAlertTargetRequestAuditEvent(this);
    }

    /**
     * Appends to the event the details of the incoming request.
     *
     * @param builder builder for the audit event details.
     */
    @Override
    protected void buildAuditMessage(StringBuilder builder) {
      super.buildAuditMessage(builder);

      builder.append(", Notification name(")
        .append(name)
        .append("), Description(")
        .append(description)
        .append("), Notification type(")
        .append(notificationType)
        .append("), Group IDs(")
        .append(StringUtils.join(groupIds, ", "));

      if (emailFrom != null) {
        builder.append("), Email from(")
          .append(emailFrom);
      }

      if (emailRecipients != null && !emailRecipients.isEmpty()) {
        builder.append("), Email to(")
          .append(StringUtils.join(emailRecipients, ", "));
      }
      builder.append("), Alert states(")
        .append(StringUtils.join(alertStates, ", "))
        .append(")");
    }

    public AddAlertTargetRequestAuditEventBuilder withName(String name) {
      this.name = name;
      return this;
    }

    public AddAlertTargetRequestAuditEventBuilder withDescription(String description) {
      this.description = description;
      return this;
    }

    public AddAlertTargetRequestAuditEventBuilder withNotificationType(String notificationType) {
      this.notificationType = notificationType;
      return this;
    }

    public AddAlertTargetRequestAuditEventBuilder withGroupIds(List<String> groupIds) {
      this.groupIds = groupIds;
      return this;
    }

    public AddAlertTargetRequestAuditEventBuilder withEmailFrom(String emailFrom) {
      this.emailFrom = emailFrom;
      return this;
    }

    public AddAlertTargetRequestAuditEventBuilder withEmailRecipients(List<String> emailRecipients) {
      this.emailRecipients = emailRecipients;
      return this;
    }

    public AddAlertTargetRequestAuditEventBuilder withAlertStates(List<String> alertStates) {
      this.alertStates = alertStates;
      return this;
    }
  }

  protected AddAlertTargetRequestAuditEvent() {
  }

  /**
   * {@inheritDoc}
   */
  protected AddAlertTargetRequestAuditEvent(AddAlertTargetRequestAuditEventBuilder builder) {
    super(builder);
  }

  /**
   * Returns an builder for {@link AddAlertTargetRequestAuditEvent}
   *
   * @return a builder instance
   */
  public static AddAlertTargetRequestAuditEventBuilder builder() {
    return new AddAlertTargetRequestAuditEventBuilder();
  }

}
