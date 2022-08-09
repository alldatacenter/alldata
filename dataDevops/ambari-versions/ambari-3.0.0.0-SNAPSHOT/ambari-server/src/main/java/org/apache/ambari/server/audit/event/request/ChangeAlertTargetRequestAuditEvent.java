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

import org.apache.ambari.server.audit.request.RequestAuditEvent;
import org.apache.commons.lang.StringUtils;

public class ChangeAlertTargetRequestAuditEvent extends RequestAuditEvent {

  public static class ChangeAlertTargetRequestAuditEventBuilder extends RequestAuditEventBuilder<ChangeAlertTargetRequestAuditEvent, ChangeAlertTargetRequestAuditEventBuilder> {

    private String name;
    private String description;
    private String notificationType;
    private List<String> groupIds;
    private String emailFrom;
    private List<String> emailRecipients;
    private List<String> alertStates;

    public ChangeAlertTargetRequestAuditEventBuilder() {
      super(ChangeAlertTargetRequestAuditEventBuilder.class);
      super.withOperation("Notification change");
    }

    @Override
    protected ChangeAlertTargetRequestAuditEvent newAuditEvent() {
      return new ChangeAlertTargetRequestAuditEvent(this);
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

    public ChangeAlertTargetRequestAuditEventBuilder withName(String name) {
      this.name = name;
      return this;
    }

    public ChangeAlertTargetRequestAuditEventBuilder withDescription(String description) {
      this.description = description;
      return this;
    }

    public ChangeAlertTargetRequestAuditEventBuilder withNotificationType(String notificationType) {
      this.notificationType = notificationType;
      return this;
    }

    public ChangeAlertTargetRequestAuditEventBuilder withGroupIds(List<String> groupIds) {
      this.groupIds = groupIds;
      return this;
    }

    public ChangeAlertTargetRequestAuditEventBuilder withEmailFrom(String emailFrom) {
      this.emailFrom = emailFrom;
      return this;
    }

    public ChangeAlertTargetRequestAuditEventBuilder withEmailRecipients(List<String> emailRecipients) {
      this.emailRecipients = emailRecipients;
      return this;
    }

    public ChangeAlertTargetRequestAuditEventBuilder withAlertStates(List<String> alertStates) {
      this.alertStates = alertStates;
      return this;
    }
  }

  protected ChangeAlertTargetRequestAuditEvent() {
  }

  /**
   * {@inheritDoc}
   */
  protected ChangeAlertTargetRequestAuditEvent(ChangeAlertTargetRequestAuditEventBuilder builder) {
    super(builder);
  }

  /**
   * Returns an builder for {@link ChangeAlertTargetRequestAuditEvent}
   *
   * @return a builder instance
   */
  public static ChangeAlertTargetRequestAuditEventBuilder builder() {
    return new ChangeAlertTargetRequestAuditEventBuilder();
  }

}
