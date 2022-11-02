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
 * Audit event for blueprint deletion
 */
@Immutable
public class DeleteBlueprintRequestAuditEvent extends RequestAuditEvent {

  public static class DeleteBlueprintRequestAuditEventBuilder extends RequestAuditEventBuilder<DeleteBlueprintRequestAuditEvent, DeleteBlueprintRequestAuditEventBuilder> {

    /**
     * Name of the deleted blueprint
     */
    private String blueprintName;

    public DeleteBlueprintRequestAuditEventBuilder() {
      super(DeleteBlueprintRequestAuditEventBuilder.class);
      super.withOperation("Delete blueprint");
    }

    @Override
    protected DeleteBlueprintRequestAuditEvent newAuditEvent() {
      return new DeleteBlueprintRequestAuditEvent(this);
    }

    /**
     * Appends to the event the details of the incoming request.
     *
     * @param builder builder for the audit event details.
     */
    @Override
    protected void buildAuditMessage(StringBuilder builder) {
      super.buildAuditMessage(builder);

      builder
        .append(", Blueprint name(")
        .append(blueprintName)
        .append(")");
    }

    public DeleteBlueprintRequestAuditEventBuilder withBlueprintName(String blueprintName) {
      this.blueprintName = blueprintName;
      return this;
    }

  }

  protected DeleteBlueprintRequestAuditEvent() {
  }

  /**
   * {@inheritDoc}
   */
  protected DeleteBlueprintRequestAuditEvent(DeleteBlueprintRequestAuditEventBuilder builder) {
    super(builder);
  }

  /**
   * Returns an builder for {@link DeleteBlueprintRequestAuditEvent}
   *
   * @return a builder instance
   */
  public static DeleteBlueprintRequestAuditEventBuilder builder() {
    return new DeleteBlueprintRequestAuditEventBuilder();
  }

}
