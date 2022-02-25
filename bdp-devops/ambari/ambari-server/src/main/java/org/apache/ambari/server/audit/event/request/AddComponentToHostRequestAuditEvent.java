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

import java.util.Set;

import javax.annotation.concurrent.Immutable;

import org.apache.ambari.server.audit.request.RequestAuditEvent;
import org.apache.commons.lang.StringUtils;

/**
 * Audit event for adding components to a host
 */
@Immutable
public class AddComponentToHostRequestAuditEvent extends RequestAuditEvent {

  public static class AddComponentToHostRequestAuditEventBuilder extends RequestAuditEventBuilder<AddComponentToHostRequestAuditEvent, AddComponentToHostRequestAuditEventBuilder> {

    /**
     * Host name
     */
    private String hostName;

    /**
     * Component name
     */
    private Set<String> components;

    public AddComponentToHostRequestAuditEventBuilder() {
      super(AddComponentToHostRequestAuditEventBuilder.class);
      super.withOperation("Component addition to host");
    }

    @Override
    protected AddComponentToHostRequestAuditEvent newAuditEvent() {
      return new AddComponentToHostRequestAuditEvent(this);
    }

    /**
     * Appends to the event the details of the incoming request.
     *
     * @param builder builder for the audit event details.
     */
    @Override
    protected void buildAuditMessage(StringBuilder builder) {
      super.buildAuditMessage(builder);

      builder.append(", Hostname(")
        .append(hostName)
        .append("), Component(")
        .append(components == null ? "" : StringUtils.join(components, ", "))
        .append(")");
    }

    public AddComponentToHostRequestAuditEventBuilder withHostName(String hostName) {
      this.hostName = hostName;
      return this;
    }

    public AddComponentToHostRequestAuditEventBuilder withComponents(Set<String> component) {
      this.components = component;
      return this;
    }
  }

  protected AddComponentToHostRequestAuditEvent() {
  }

  /**
   * {@inheritDoc}
   */
  protected AddComponentToHostRequestAuditEvent(AddComponentToHostRequestAuditEventBuilder builder) {
    super(builder);
  }

  /**
   * Returns an builder for {@link AddComponentToHostRequestAuditEvent}
   *
   * @return a builder instance
   */
  public static AddComponentToHostRequestAuditEventBuilder builder() {
    return new AddComponentToHostRequestAuditEventBuilder();
  }

}
