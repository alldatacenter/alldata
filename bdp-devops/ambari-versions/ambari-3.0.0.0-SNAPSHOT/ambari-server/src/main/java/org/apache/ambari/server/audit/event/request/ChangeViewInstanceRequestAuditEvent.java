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
 * Audit event for change view instance
 */
@Immutable
public class ChangeViewInstanceRequestAuditEvent extends RequestAuditEvent {

  public static class ChangeViewInstanceRequestAuditEventBuilder extends RequestAuditEventBuilder<ChangeViewInstanceRequestAuditEvent, ChangeViewInstanceRequestAuditEventBuilder> {

    /**
     * Description for the view instance
     */
    private String description;

    /**
     * Name for the view instance
     */
    private String name;

    /**
     * Type for view instance (Files, Tez)
     */
    private String type;

    /**
     * Display name for view instance
     */
    private String displayName;

    /**
     * Version for view instance
     */
    private String version;

    public ChangeViewInstanceRequestAuditEventBuilder() {
      super(ChangeViewInstanceRequestAuditEventBuilder.class);
      super.withOperation("View change");
    }

    @Override
    protected ChangeViewInstanceRequestAuditEvent newAuditEvent() {
      return new ChangeViewInstanceRequestAuditEvent(this);
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

    public ChangeViewInstanceRequestAuditEventBuilder withDescription(String description) {
      this.description = description;
      return this;
    }

    public ChangeViewInstanceRequestAuditEventBuilder withName(String name) {
      this.name = name;
      return this;
    }

    public ChangeViewInstanceRequestAuditEventBuilder withType(String type) {
      this.type = type;
      return this;
    }

    public ChangeViewInstanceRequestAuditEventBuilder withDisplayName(String displayName) {
      this.displayName = displayName;
      return this;
    }

    public ChangeViewInstanceRequestAuditEventBuilder withVersion(String version) {
      this.version = version;
      return this;
    }
  }

  protected ChangeViewInstanceRequestAuditEvent() {
  }

  /**
   * {@inheritDoc}
   */
  protected ChangeViewInstanceRequestAuditEvent(ChangeViewInstanceRequestAuditEventBuilder builder) {
    super(builder);
  }

  /**
   * Returns an builder for {@link ChangeViewInstanceRequestAuditEvent}
   *
   * @return a builder instance
   */
  public static ChangeViewInstanceRequestAuditEventBuilder builder() {
    return new ChangeViewInstanceRequestAuditEventBuilder();
  }

}
