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

import javax.annotation.concurrent.Immutable;

/**
 * Logout audit event.
 */
@Immutable
public class LogoutAuditEvent extends AbstractUserAuditEvent {
  public static class LogoutAuditEventBuilder
    extends AbstractUserAuditEventBuilder<LogoutAuditEvent, LogoutAuditEventBuilder> {

    private LogoutAuditEventBuilder() {
      super(LogoutAuditEventBuilder.class);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void buildAuditMessage(StringBuilder builder) {
      super.buildAuditMessage(builder);
      builder.append(", Operation(Logout), Status(Success)");
    }

    @Override
    protected LogoutAuditEvent newAuditEvent() {
      return new LogoutAuditEvent(this);
    }
  }

  private LogoutAuditEvent() {
  }

  /**
   * {@inheritDoc}
   */
  private LogoutAuditEvent(LogoutAuditEventBuilder builder) {
    super(builder);

  }

  /**
   * Returns an builder for {@link LogoutAuditEvent}
   *
   * @return a builder instance
   */
  public static LogoutAuditEventBuilder builder() {
    return new LogoutAuditEventBuilder();
  }
}
