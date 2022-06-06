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

import org.apache.ambari.server.audit.request.RequestAuditEvent;

/**
 * Audit event for request
 */
@Immutable
public class AddRequestRequestAuditEvent extends RequestAuditEvent {

  public static class AddRequestAuditEventBuilder extends RequestAuditEventBuilder<AddRequestRequestAuditEvent, AddRequestAuditEventBuilder> {

    /**
     * Command in the request
     */
    private String command;

    /**
     * Cluster name
     */
    private String clusterName;

    public AddRequestAuditEventBuilder() {
      super(AddRequestAuditEventBuilder.class);
      super.withOperation("Request from server");
    }

    @Override
    protected AddRequestRequestAuditEvent newAuditEvent() {
      return new AddRequestRequestAuditEvent(this);
    }

    /**
     * Appends to the event the details of the incoming request.
     *
     * @param builder builder for the audit event details.
     */
    @Override
    protected void buildAuditMessage(StringBuilder builder) {
      super.buildAuditMessage(builder);

      builder.append(", Command(")
        .append(command)
        .append("), Cluster name(")
        .append(clusterName)
        .append(")");
    }

    public AddRequestAuditEventBuilder withClusterName(String clusterName) {
      this.clusterName = clusterName;
      return this;
    }

    public AddRequestAuditEventBuilder withCommand(String command) {
      this.command = command;
      return this;
    }
  }

  protected AddRequestRequestAuditEvent() {
  }

  /**
   * {@inheritDoc}
   */
  protected AddRequestRequestAuditEvent(AddRequestAuditEventBuilder builder) {
    super(builder);
  }

  /**
   * Returns an builder for {@link AddRequestRequestAuditEvent}
   *
   * @return a builder instance
   */
  public static AddRequestAuditEventBuilder builder() {
    return new AddRequestAuditEventBuilder();
  }

}
