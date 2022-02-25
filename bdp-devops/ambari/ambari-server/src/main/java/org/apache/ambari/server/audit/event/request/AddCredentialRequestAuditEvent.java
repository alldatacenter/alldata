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
 * Audit event for adding credential
 */
@Immutable
public class AddCredentialRequestAuditEvent extends RequestAuditEvent {

  public static class AddCredentialAuditEventBuilder extends RequestAuditEventBuilder<AddCredentialRequestAuditEvent, AddCredentialAuditEventBuilder> {

    /**
     * Credential type (e.g. temporary)
     */
    private String type;

    /**
     * Cluster name
     */
    private String clusterName;

    /**
     * Principal
     */
    private String principal;

    /**
     * Alias for the credential
     */
    private String alias;

    public AddCredentialAuditEventBuilder() {
      super(AddCredentialAuditEventBuilder.class);
      super.withOperation("Credential addition");
    }

    @Override
    protected AddCredentialRequestAuditEvent newAuditEvent() {
      return new AddCredentialRequestAuditEvent(this);
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
        .append("), Principal(")
        .append(principal)
        .append("), Alias(")
        .append(alias)
        .append("), Cluster name(")
        .append(clusterName)
        .append(")");
    }

    public AddCredentialAuditEventBuilder withType(String type) {
      this.type = type;
      return this;
    }

    public AddCredentialAuditEventBuilder withClusterName(String clusterName) {
      this.clusterName = clusterName;
      return this;
    }

    public AddCredentialAuditEventBuilder withPrincipal(String principal) {
      this.principal = principal;
      return this;
    }

    public AddCredentialAuditEventBuilder withAlias(String alias) {
      this.alias = alias;
      return this;
    }
  }

  protected AddCredentialRequestAuditEvent() {
  }

  /**
   * {@inheritDoc}
   */
  protected AddCredentialRequestAuditEvent(AddCredentialAuditEventBuilder builder) {
    super(builder);
  }

  /**
   * Returns an builder for {@link AddCredentialRequestAuditEvent}
   *
   * @return a builder instance
   */
  public static AddCredentialAuditEventBuilder builder() {
    return new AddCredentialAuditEventBuilder();
  }

}
