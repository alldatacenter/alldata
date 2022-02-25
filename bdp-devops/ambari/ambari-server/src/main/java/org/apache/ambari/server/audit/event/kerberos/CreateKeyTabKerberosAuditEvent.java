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
 * Audit event for kerberos keytab change
 */
@Immutable
public class CreateKeyTabKerberosAuditEvent extends AbstractKerberosAuditEvent {

  public static class CreateKeyTabKerberosAuditEventBuilder extends AbstractKerberosAuditEventBuilder<CreateKeyTabKerberosAuditEvent, CreateKeyTabKerberosAuditEventBuilder> {

    /**
     * Path to keytab file
     */
    private String keyTabFilePath;

    /**
     * Host name
     */
    private String hostName;

    /**
     * Principal that belons to the keytab file
     */
    private String principal;

    private CreateKeyTabKerberosAuditEventBuilder() {
      super(CreateKeyTabKerberosAuditEventBuilder.class);
      this.withOperation("Keytab file creation");
    }

    @Override
    protected void buildAuditMessage(StringBuilder builder) {
      super.buildAuditMessage(builder);

      builder
        .append(", Principal(")
        .append(principal)
        .append("), Hostname(")
        .append(hostName)
        .append("), Keytab file(")
        .append(keyTabFilePath)
        .append(")");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected CreateKeyTabKerberosAuditEvent newAuditEvent() {
      return new CreateKeyTabKerberosAuditEvent(this);
    }

    public CreateKeyTabKerberosAuditEventBuilder withKeyTabFilePath(String keyTabFilePath) {
      this.keyTabFilePath = keyTabFilePath;
      return this;
    }

    public CreateKeyTabKerberosAuditEventBuilder withHostName(String hostName) {
      this.hostName = hostName;
      return this;
    }

    public CreateKeyTabKerberosAuditEventBuilder withPrincipal(String principal) {
      this.principal = principal;
      return this;
    }

    public boolean hasPrincipal() {
      return principal != null;
    }
  }

  private CreateKeyTabKerberosAuditEvent() {
  }

  /**
   * {@inheritDoc}
   */
  private CreateKeyTabKerberosAuditEvent(CreateKeyTabKerberosAuditEventBuilder builder) {
    super(builder);
  }

  /**
   * Returns an builder for {@link CreateKeyTabKerberosAuditEvent}
   *
   * @return a builder instance
   */
  public static CreateKeyTabKerberosAuditEventBuilder builder() {
    return new CreateKeyTabKerberosAuditEventBuilder();
  }
}
