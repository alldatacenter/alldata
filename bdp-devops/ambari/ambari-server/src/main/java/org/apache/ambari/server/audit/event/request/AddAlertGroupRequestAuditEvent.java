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
 * Audit event for adding alert group
 */
@Immutable
public class AddAlertGroupRequestAuditEvent extends RequestAuditEvent {

  public static class AddAlertGroupRequestAuditEventBuilder extends RequestAuditEventBuilder<AddAlertGroupRequestAuditEvent, AddAlertGroupRequestAuditEventBuilder> {

    /**
     * Group name
     */
    private String name;

    /**
     * Definition ids for the group
     */
    private List<String> definitionIds;

    /**
     * Notification ids for the group
     */
    private List<String> notificationIds;

    public AddAlertGroupRequestAuditEventBuilder() {
      super(AddAlertGroupRequestAuditEventBuilder.class);
      super.withOperation("Alert group addition");
    }

    @Override
    protected AddAlertGroupRequestAuditEvent newAuditEvent() {
      return new AddAlertGroupRequestAuditEvent(this);
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

    public AddAlertGroupRequestAuditEventBuilder withName(String name) {
      this.name = name;
      return this;
    }

    public AddAlertGroupRequestAuditEventBuilder withDefinitionIds(List<String> ids) {
      this.definitionIds = ids;
      return this;
    }

    public AddAlertGroupRequestAuditEventBuilder withNotificationIds(List<String> ids) {
      this.notificationIds = ids;
      return this;
    }
  }

  protected AddAlertGroupRequestAuditEvent() {
  }

  /**
   * {@inheritDoc}
   */
  protected AddAlertGroupRequestAuditEvent(AddAlertGroupRequestAuditEventBuilder builder) {
    super(builder);
  }

  /**
   * Returns an builder for {@link AddAlertGroupRequestAuditEvent}
   *
   * @return a builder instance
   */
  public static AddAlertGroupRequestAuditEventBuilder builder() {
    return new AddAlertGroupRequestAuditEventBuilder();
  }

}
