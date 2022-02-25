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
 * Audit event for group membership change
 */
@Immutable
public class MembershipChangeRequestAuditEvent extends RequestAuditEvent {

  public static class AddUserToGroupRequestAuditEventBuilder extends RequestAuditEventBuilder<MembershipChangeRequestAuditEvent, AddUserToGroupRequestAuditEventBuilder> {

    /**
     * New list of users
     */
    private List<String> userNameList;

    /**
     * Group name
     */
    private String groupName;

    public AddUserToGroupRequestAuditEventBuilder() {
      super(AddUserToGroupRequestAuditEventBuilder.class);
      super.withOperation("Membership change");
    }

    @Override
    protected MembershipChangeRequestAuditEvent newAuditEvent() {
      return new MembershipChangeRequestAuditEvent(this);
    }

    /**
     * Appends to the event the details of the incoming request.
     *
     * @param builder builder for the audit event details.
     */
    @Override
    protected void buildAuditMessage(StringBuilder builder) {
      super.buildAuditMessage(builder);

      builder.append(", Group(")
        .append(groupName)
        .append("), Members(");

      if (userNameList.isEmpty()) {
        builder.append("<empty>");
      } else {
        builder.append(StringUtils.join(userNameList, ", "));
      }

      builder.append(")");
    }

    public AddUserToGroupRequestAuditEventBuilder withUserNameList(List<String> users) {
      this.userNameList = users;
      return this;
    }

    public AddUserToGroupRequestAuditEventBuilder withGroupName(String groupName) {
      this.groupName = groupName;
      return this;
    }

  }

  protected MembershipChangeRequestAuditEvent() {
  }

  /**
   * {@inheritDoc}
   */
  protected MembershipChangeRequestAuditEvent(AddUserToGroupRequestAuditEventBuilder builder) {
    super(builder);
  }

  /**
   * Returns an builder for {@link MembershipChangeRequestAuditEvent}
   *
   * @return a builder instance
   */
  public static AddUserToGroupRequestAuditEventBuilder builder() {
    return new AddUserToGroupRequestAuditEventBuilder();
  }

}
