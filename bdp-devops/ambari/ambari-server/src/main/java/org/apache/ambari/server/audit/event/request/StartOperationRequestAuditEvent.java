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

import org.apache.ambari.server.audit.event.AbstractUserAuditEvent;

/**
 * Start operation request was accepted.
 */
@Immutable
public class StartOperationRequestAuditEvent extends AbstractUserAuditEvent {

  public static class StartOperationAuditEventBuilder
    extends AbstractUserAuditEventBuilder<StartOperationRequestAuditEvent, StartOperationAuditEventBuilder> {

    /**
     * Request id
     */
    private String requestId;

    /**
     * Reason of failure, if it is set, then the request is considered as failed
     */
    private String reasonOfFailure;

    /**
     * Description of the request
     */
    private String operation;

    /**
     * Target host of the request
     */
    private String hostname;

    private StartOperationAuditEventBuilder() {
      super(StartOperationAuditEventBuilder.class);
    }

    /**
     * Appends to the audit event the identifier of the
     * operation through whcih the operation progress can be tracked.
     *
     * @param builder builder for the audit event details.
     */
    @Override
    protected void buildAuditMessage(StringBuilder builder) {
      super.buildAuditMessage(builder);

      builder
        .append(", Operation(")
        .append(operation);
      if(hostname != null) {
        builder.append("), Host name(").append(hostname);
      }
      builder.append("), RequestId(")
        .append(requestId)
        .append("), Status(")
        .append(reasonOfFailure == null ? "Successfully queued" : "Failed to queue");

      if (reasonOfFailure != null) {
        builder.append("), Reason(")
          .append(reasonOfFailure);
      }
      builder.append(")");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected StartOperationRequestAuditEvent newAuditEvent() {
      return new StartOperationRequestAuditEvent(this);
    }

    /**
     * Sets the identifier of the operation through which the operation progress can be tracked.
     *
     * @param requestId the identifier of the operation through which the operation progress can be tracked.
     * @return this builder
     */
    public StartOperationAuditEventBuilder withRequestId(String requestId) {
      this.requestId = requestId;
      return this;
    }

    public StartOperationAuditEventBuilder withReasonOfFailure(String reasonOfFailure) {
      this.reasonOfFailure = reasonOfFailure;
      return this;
    }

    public StartOperationAuditEventBuilder withOperation(String operation) {
      this.operation = operation;
      return this;
    }

    public StartOperationAuditEventBuilder withHostname(String hostname) {
      this.hostname = hostname;
      return this;
    }
  }

  private StartOperationRequestAuditEvent() {
  }

  /**
   * {@inheritDoc}
   */
  private StartOperationRequestAuditEvent(StartOperationAuditEventBuilder builder) {
    super(builder);
  }

  /**
   * Returns an builder for {@link StartOperationRequestAuditEvent}
   *
   * @return a builder instance
   */
  public static StartOperationAuditEventBuilder builder() {
    return new StartOperationAuditEventBuilder();
  }
}
