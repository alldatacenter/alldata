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
 * Audit event for tracking operations
 */
@Immutable
public class OperationStatusAuditEvent extends AbstractUserAuditEvent {

  public static class OperationStatusAuditEventBuilder extends AbstractUserAuditEventBuilder<OperationStatusAuditEvent, OperationStatusAuditEventBuilder> {

    /**
     * Request identifier
     */
    private String requestId;

    /**
     * Status of the whole request
     */
    private String status;

    /**
     * Name of the operation
     */
    private String operation;

    private OperationStatusAuditEventBuilder() {
      super(OperationStatusAuditEventBuilder.class);
    }

    @Override
    protected OperationStatusAuditEvent newAuditEvent() {
      return new OperationStatusAuditEvent(this);
    }

    /**
     * Builds and audit log message based on the member variables
     *
     * @param builder builder for the audit event details.
     */
    @Override
    protected void buildAuditMessage(StringBuilder builder) {
      super.buildAuditMessage(builder);
      builder
        .append(", Operation(")
        .append(this.operation)
        .append("), Status(")
        .append(this.status)
        .append("), RequestId(")
        .append(this.requestId)
        .append(")");
    }


    public OperationStatusAuditEventBuilder withStatus(String status) {
      this.status = status;
      return this;
    }

    public OperationStatusAuditEventBuilder withRequestId(String requestId) {
      this.requestId = requestId;
      return this;
    }

    public OperationStatusAuditEventBuilder withRequestContext(String operation) {
      this.operation = operation;
      return this;
    }
  }

  private OperationStatusAuditEvent() {
  }

  /**
   * {@inheritDoc}
   */
  private OperationStatusAuditEvent(OperationStatusAuditEventBuilder builder) {
    super(builder);
  }

  /**
   * Returns an builder for {@link OperationStatusAuditEvent}
   *
   * @return a builder instance
   */
  public static OperationStatusAuditEventBuilder builder() {
    return new OperationStatusAuditEventBuilder();
  }

}
