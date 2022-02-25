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
 * Audit event for changing alert group
 */
@Immutable
public class ChangeAlertGroupRequestAuditEvent extends RequestAuditEvent {

  public static class ChangeAlertGroupRequestAuditEventBuilder extends RequestAuditEventBuilder<ChangeAlertGroupRequestAuditEvent, ChangeAlertGroupRequestAuditEventBuilder> {

    /**
     * Group name
     */
    private String name;

    /**
     * Definition ids
     */
    private List<String> definitionIds;

    /**
     * Notification ids
     */
    private List<String> notificationIds;

    public ChangeAlertGroupRequestAuditEventBuilder() {
      super(ChangeAlertGroupRequestAuditEventBuilder.class);
      super.withOperation("Alert group change");
    }

    @Override
    protected ChangeAlertGroupRequestAuditEvent newAuditEvent() {
      return new ChangeAlertGroupRequestAuditEvent(this);
    }

    /**
     * Appends to the event the details of the incoming request.
     *
     * @param builder builder for the audit event details.
     */
    @Override
    protected void buildAuditMessage(StringBuilder builder) {
      super.buildAuditMessage(builder);

      builder.append(", Alert group name(")
        .append(name)
        .append("), Definition IDs(")
        .append(StringUtils.join(definitionIds, ", "))
        .append("), Notification IDs(")
        .append(StringUtils.join(notificationIds, ", "))
        .append(")");
    }

    public ChangeAlertGroupRequestAuditEventBuilder withName(String name) {
      this.name = name;
      return this;
    }

    public ChangeAlertGroupRequestAuditEventBuilder withDefinitionIds(List<String> ids) {
      this.definitionIds = ids;
      return this;
    }

    public ChangeAlertGroupRequestAuditEventBuilder withNotificationIds(List<String> ids) {
      this.notificationIds = ids;
      return this;
    }
  }

  protected ChangeAlertGroupRequestAuditEvent() {
  }

  /**
   * {@inheritDoc}
   */
  protected ChangeAlertGroupRequestAuditEvent(ChangeAlertGroupRequestAuditEventBuilder builder) {
    super(builder);
  }

  /**
   * Returns an builder for {@link ChangeAlertGroupRequestAuditEvent}
   *
   * @return a builder instance
   */
  public static ChangeAlertGroupRequestAuditEventBuilder builder() {
    return new ChangeAlertGroupRequestAuditEventBuilder();
  }

}
