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

package org.apache.ambari.server.audit.request;

import org.apache.ambari.server.api.services.Request;
import org.apache.ambari.server.api.services.ResultStatus;
import org.apache.ambari.server.audit.event.AbstractUserAuditEvent;

/**
 * Base class for start operation audit events.
 */
public abstract class RequestAuditEvent extends AbstractUserAuditEvent {

  public abstract static class RequestAuditEventBuilder<T extends RequestAuditEvent, TBuilder extends RequestAuditEventBuilder<T, TBuilder>>
    extends AbstractUserAuditEventBuilder<T, TBuilder> {

    /**
     * Request type (PUT, POST, DELETE, etc...)
     */
    private Request.Type requestType;

    /**
     * Result status, that contains http statuses (OK, ACCEPTED, FORBIDDEN, etc...)
     */
    private ResultStatus resultStatus;

    /**
     * The url that is called
     */
    private String url;

    /**
     * Description of the operation
     */
    private String operation;

    protected RequestAuditEventBuilder(Class<? extends TBuilder> builderClass) {
      super(builderClass);
    }

    /**
     * Appends to the event the details of the incoming request.
     *
     * @param builder builder for the audit event details.
     */
    @Override
    protected void buildAuditMessage(StringBuilder builder) {
      super.buildAuditMessage(builder);
      if (operation != null) {
        builder
          .append(", Operation(")
          .append(operation)
          .append(")");
      }
      builder
        .append(", RequestType(")
        .append(requestType)
        .append("), ")
        .append("url(")
        .append(url)
        .append("), ResultStatus(")
        .append(resultStatus.getStatusCode())
        .append(" ")
        .append(resultStatus.getStatus())
        .append(")");

      if (resultStatus.isErrorState()) {
        builder.append(", Reason(")
          .append(resultStatus.getMessage())
          .append(")");
      }
    }

    /**
     * Sets the request type to be added to the audit event.
     *
     * @param requestType request type to be added to the audit event.
     * @return this builder
     */
    public TBuilder withRequestType(Request.Type requestType) {
      this.requestType = requestType;

      return self();
    }

    /**
     * Sets the url to be added to the audit event.
     *
     * @param url url to be added to the audit event.
     * @return this builder
     */
    public TBuilder withUrl(String url) {
      this.url = url;

      return self();
    }

    /**
     * Sets the result status to be added to the audit event.
     *
     * @param resultStatus result status to be added to the audit event.
     * @return this builder
     */
    public TBuilder withResultStatus(ResultStatus resultStatus) {
      this.resultStatus = resultStatus;

      return self();
    }

    /**
     * Sets the operation to be added to the audit event.
     *
     * @param operation operation to be added to the audit event.
     * @return this builder
     */
    public TBuilder withOperation(String operation) {
      this.operation = operation;

      return self();
    }
  }

  protected RequestAuditEvent() {
  }

  /**
   * {@inheritDoc}
   */
  protected RequestAuditEvent(RequestAuditEventBuilder<?, ?> builder) {
    super(builder);
  }

}

