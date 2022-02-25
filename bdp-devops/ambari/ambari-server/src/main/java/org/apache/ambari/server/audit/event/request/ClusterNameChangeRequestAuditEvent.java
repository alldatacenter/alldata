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

import org.apache.ambari.server.audit.request.RequestAuditEvent;

/**
 * Base class for start operation audit events.
 */
public class ClusterNameChangeRequestAuditEvent extends RequestAuditEvent {

  public static class ClusterNameChangeRequestAuditEventBuilder extends RequestAuditEventBuilder<ClusterNameChangeRequestAuditEvent, ClusterNameChangeRequestAuditEventBuilder> {

    private String oldName;

    private String newName;

    public ClusterNameChangeRequestAuditEventBuilder() {
      super(ClusterNameChangeRequestAuditEventBuilder.class);
      super.withOperation("Cluster name change");
    }

    @Override
    protected ClusterNameChangeRequestAuditEvent newAuditEvent() {
      return new ClusterNameChangeRequestAuditEvent(this);
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
        .append(", Old name(")
        .append(oldName)
        .append("), New name(")
        .append(newName)
        .append(")");
    }

    public ClusterNameChangeRequestAuditEventBuilder withOldName(String oldName) {
      this.oldName = oldName;
      return this;
    }

    public ClusterNameChangeRequestAuditEventBuilder withNewName(String newName) {
      this.newName = newName;
      return this;
    }

  }

  protected ClusterNameChangeRequestAuditEvent() {
  }

  /**
   * {@inheritDoc}
   */
  protected ClusterNameChangeRequestAuditEvent(ClusterNameChangeRequestAuditEventBuilder builder) {
    super(builder);
  }

  /**
   * Returns an builder for {@link ClusterNameChangeRequestAuditEvent}
   *
   * @return a builder instance
   */
  public static ClusterNameChangeRequestAuditEventBuilder builder() {
    return new ClusterNameChangeRequestAuditEventBuilder();
  }

}
