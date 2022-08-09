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

@Immutable
public class CreatePrincipalKerberosAuditEvent extends AbstractKerberosAuditEvent {

  public static class CreatePrincipalKerberosAuditEventBuilder extends AbstractKerberosAuditEventBuilder<CreatePrincipalKerberosAuditEvent, CreatePrincipalKerberosAuditEventBuilder> {

    private String principal;

    private CreatePrincipalKerberosAuditEventBuilder() {
      super(CreatePrincipalKerberosAuditEventBuilder.class);
      this.withOperation("Principal creation");
    }

    @Override
    protected void buildAuditMessage(StringBuilder builder) {
      super.buildAuditMessage(builder);
      builder.append(", Principal(")
        .append(principal)
        .append(")");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected CreatePrincipalKerberosAuditEvent newAuditEvent() {
      return new CreatePrincipalKerberosAuditEvent(this);
    }

    public CreatePrincipalKerberosAuditEventBuilder withPrincipal(String principal) {
      this.principal = principal;
      return this;
    }
  }

  private CreatePrincipalKerberosAuditEvent() {
  }

  /**
   * {@inheritDoc}
   */
  private CreatePrincipalKerberosAuditEvent(CreatePrincipalKerberosAuditEventBuilder builder) {
    super(builder);
  }

  /**
   * Returns an builder for {@link CreatePrincipalKerberosAuditEvent}
   *
   * @return a builder instance
   */
  public static CreatePrincipalKerberosAuditEventBuilder builder() {
    return new CreatePrincipalKerberosAuditEventBuilder();
  }
}
