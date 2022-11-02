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

package org.apache.ambari.server.audit.event;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.annotation.concurrent.Immutable;

import org.apache.commons.lang.StringUtils;

/**
 * Login audit event.
 */
@Immutable
public class LoginAuditEvent extends AbstractUserAuditEvent {

  public static class LoginAuditEventBuilder
    extends AbstractUserAuditEventBuilder<LoginAuditEvent, LoginAuditEventBuilder> {

    private LoginAuditEventBuilder() {
      super(LoginAuditEventBuilder.class);
    }

    /**
     * List of roles possessed by the principal requesting access to a resource.
     * [ view name | cluster name | 'Ambari'] -> list of permissions
     */
    private Map<String, List<String>> roles;

    /**
     * Reason of failure, if it is not null, then the request status is consider as failed
     */
    private String reasonOfFailure;

    /**
     * Number of consecutive failed authentication attempts since the last successful attempt
     */
    private Integer consecutiveFailures;

    /**
     * {@inheritDoc}
     */
    @Override
    protected void buildAuditMessage(StringBuilder builder) {
      super.buildAuditMessage(builder);

      builder.append(", Operation(User login), Roles(").append(System.lineSeparator());

      if (roles != null && !roles.isEmpty()) {
        List<String> lines = new LinkedList<>();
        for (Map.Entry<String, List<String>> entry : roles.entrySet()) {
          lines.add("    " + entry.getKey() + ": " + StringUtils.join(entry.getValue(), ", "));
        }
        builder.append(StringUtils.join(lines, System.lineSeparator()));
        builder.append(System.lineSeparator());
      }
      builder.append("), Status(")
        .append(reasonOfFailure == null ? "Success" : "Failed");

      if (reasonOfFailure != null) {
        builder.append("), Reason(")
          .append(reasonOfFailure);

        builder.append("), Consecutive failures(")
            .append((consecutiveFailures == null) ? "UNKNOWN USER" : String.valueOf(consecutiveFailures));
      }
      builder.append(")");
    }

    /**
     * Sets the list of roles possessed by the principal requesting access to a resource.
     *
     * @param roles
     * @return this builder
     */
    public LoginAuditEventBuilder withRoles(Map<String, List<String>> roles) {
      this.roles = roles;

      return this;
    }

    public LoginAuditEventBuilder withReasonOfFailure(String reasonOfFailure) {
      this.reasonOfFailure = reasonOfFailure;
      return this;
    }

    /**
     * Set the number of consecutive authentication failures since the last successful authentication
     * attempt
     *
     * @param consecutiveFailures the number of consecutive authentication failures
     * @return this builder
     */
    public LoginAuditEventBuilder withConsecutiveFailures(Integer consecutiveFailures) {
      this.consecutiveFailures = consecutiveFailures;
      return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected LoginAuditEvent newAuditEvent() {
      return new LoginAuditEvent(this);
    }

  }


  private LoginAuditEvent() {
  }

  /**
   * {@inheritDoc}
   */
  private LoginAuditEvent(LoginAuditEventBuilder builder) {
    super(builder);

  }

  /**
   * Returns an builder for {@link LoginAuditEvent}
   *
   * @return a builder instance
   */
  public static LoginAuditEventBuilder builder() {
    return new LoginAuditEventBuilder();
  }
}
