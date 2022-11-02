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

package org.apache.ambari.server.audit.event.kerberos;

import javax.annotation.concurrent.Immutable;

/**
 * Audit event for kerberos security state change of components
 */
@Immutable
public class ChangeSecurityStateKerberosAuditEvent extends AbstractKerberosAuditEvent {

  public static class ChangeSecurityStateKerberosAuditEventBuilder extends AbstractKerberosAuditEventBuilder<ChangeSecurityStateKerberosAuditEvent, ChangeSecurityStateKerberosAuditEventBuilder> {

    /**
     * Service name
     */
    private String service;

    /**
     * Component name
     */
    private String component;

    /**
     * Host name
     */
    private String hostName;

    /**
     * Security state
     */
    private String state;

    private ChangeSecurityStateKerberosAuditEventBuilder() {
      super(ChangeSecurityStateKerberosAuditEventBuilder.class);
      this.withOperation("Security state change");
    }

    @Override
    protected void buildAuditMessage(StringBuilder builder) {
      super.buildAuditMessage(builder);

      builder
        .append(", Hostname(")
        .append(hostName)
        .append("), Service(")
        .append(service)
        .append("), Component(")
        .append(component)
        .append("), State(")
        .append(state)
        .append(")");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected ChangeSecurityStateKerberosAuditEvent newAuditEvent() {
      return new ChangeSecurityStateKerberosAuditEvent(this);
    }

    public ChangeSecurityStateKerberosAuditEventBuilder withService(String service) {
      this.service = service;
      return this;
    }

    public ChangeSecurityStateKerberosAuditEventBuilder withComponent(String component) {
      this.component = component;
      return this;
    }

    public ChangeSecurityStateKerberosAuditEventBuilder withHostName(String hostName) {
      this.hostName = hostName;
      return this;
    }

    public ChangeSecurityStateKerberosAuditEventBuilder withState(String state) {
      this.state = state;
      return this;
    }
  }

  private ChangeSecurityStateKerberosAuditEvent() {
  }

  /**
   * {@inheritDoc}
   */
  private ChangeSecurityStateKerberosAuditEvent(ChangeSecurityStateKerberosAuditEventBuilder builder) {
    super(builder);
  }

  /**
   * Returns an builder for {@link ChangeSecurityStateKerberosAuditEvent}
   *
   * @return a builder instance
   */
  public static ChangeSecurityStateKerberosAuditEventBuilder builder() {
    return new ChangeSecurityStateKerberosAuditEventBuilder();
  }
}
