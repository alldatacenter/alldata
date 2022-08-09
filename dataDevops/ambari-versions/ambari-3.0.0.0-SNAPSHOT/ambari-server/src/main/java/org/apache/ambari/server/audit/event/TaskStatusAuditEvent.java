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
 * Audit event for tracking task status
 */
@Immutable
public class TaskStatusAuditEvent extends AbstractUserAuditEvent {

  public static class TaskStatusAuditEventBuilder extends AbstractUserAuditEventBuilder<TaskStatusAuditEvent, TaskStatusAuditEventBuilder> {

    /**
     * Request identifier
     */
    private String requestId;

    /**
     * Task identifier
     */
    private String taskId;

    /**
     * Request identifier
     */
    private String hostName;

    /**
     * Status of the whole request
     */
    private String status;

    /**
     * Name of the operation
     */
    private String operation;

    /**
     * Task command details
     */
    private String details;

    private TaskStatusAuditEventBuilder() {
      super(TaskStatusAuditEventBuilder.class);
    }

    @Override
    protected TaskStatusAuditEvent newAuditEvent() {
      return new TaskStatusAuditEvent(this);
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
        .append(this.operation);

      if (details != null) {
        builder.append("), Details(")
          .append(this.details);
      }

      builder.append("), Status(")
        .append(this.status)
        .append("), RequestId(")
        .append(this.requestId)
        .append("), TaskId(")
        .append(this.taskId)
        .append("), Hostname(")
        .append(this.hostName)
        .append(")");
    }


    public TaskStatusAuditEventBuilder withStatus(String status) {
      this.status = status;
      return this;
    }

    public TaskStatusAuditEventBuilder withRequestId(String requestId) {
      this.requestId = requestId;
      return this;
    }

    public TaskStatusAuditEventBuilder withTaskId(String taskId) {
      this.taskId = taskId;
      return this;
    }

    public TaskStatusAuditEventBuilder withHostName(String hostName) {
      this.hostName = hostName;
      return this;
    }

    public TaskStatusAuditEventBuilder withOperation(String operation) {
      this.operation = operation;
      return this;
    }

    public TaskStatusAuditEventBuilder withDetails(String details) {
      this.details = details;
      return this;
    }
  }

  private TaskStatusAuditEvent() {
  }

  /**
   * {@inheritDoc}
   */
  private TaskStatusAuditEvent(TaskStatusAuditEventBuilder builder) {
    super(builder);
  }

  /**
   * Returns an builder for {@link TaskStatusAuditEvent}
   *
   * @return a builder instance
   */
  public static TaskStatusAuditEventBuilder builder() {
    return new TaskStatusAuditEventBuilder();
  }

}
