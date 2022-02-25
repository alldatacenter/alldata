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
 * Audit event for adding view instance
 */
@Immutable
public class AddViewInstanceRequestAuditEvent extends RequestAuditEvent {

  public static class AddViewInstanceRequestAuditEventBuilder extends RequestAuditEventBuilder<AddViewInstanceRequestAuditEvent, AddViewInstanceRequestAuditEventBuilder> {

    /**
     * View instance description
     */
    private String description;

    /**
     * View instance name
     */
    private String name;

    /**
     * View instance type (Files, Tez, etc)
     */
    private String type;

    /**
     * Display name for the view instance
     */
    private String displayName;

    /**
     * View instance version
     */
    private String version;

    public AddViewInstanceRequestAuditEventBuilder() {
      super(AddViewInstanceRequestAuditEventBuilder.class);
      super.withOperation("View addition");
    }

    @Override
    protected AddViewInstanceRequestAuditEvent newAuditEvent() {
      return new AddViewInstanceRequestAuditEvent(this);
    }

    /**
     * Appends to the event the details of the incoming request.
     *
     * @param builder builder for the audit event details.
     */
    @Override
    protected void buildAuditMessage(StringBuilder builder) {
      super.buildAuditMessage(builder);

      builder.append(", Type(")
        .append(type)
        .append("), Version(")
        .append(version)
        .append("), Name(")
        .append(name)
        .append("), Display name(")
        .append(displayName)
        .append("), Description(")
        .append(description)
        .append(")");
    }

    public AddViewInstanceRequestAuditEventBuilder withDescription(String description) {
      this.description = description;
      return this;
    }

    public AddViewInstanceRequestAuditEventBuilder withName(String name) {
      this.name = name;
      return this;
    }

    public AddViewInstanceRequestAuditEventBuilder withType(String type) {
      this.type = type;
      return this;
    }

    public AddViewInstanceRequestAuditEventBuilder withDisplayName(String displayName) {
      this.displayName = displayName;
      return this;
    }

    public AddViewInstanceRequestAuditEventBuilder withVersion(String version) {
      this.version = version;
      return this;
    }
  }

  protected AddViewInstanceRequestAuditEvent() {
  }

  /**
   * {@inheritDoc}
   */
  protected AddViewInstanceRequestAuditEvent(AddViewInstanceRequestAuditEventBuilder builder) {
    super(builder);
  }

  /**
   * Returns an builder for {@link AddViewInstanceRequestAuditEvent}
   *
   * @return a builder instance
   */
  public static AddViewInstanceRequestAuditEventBuilder builder() {
    return new AddViewInstanceRequestAuditEventBuilder();
  }

}
