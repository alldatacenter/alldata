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
 * Audit event for adding upgrade
 */
@Immutable
public class AddUpgradeRequestAuditEvent extends RequestAuditEvent {

  public static class AddUpgradeRequestAuditEventBuilder extends RequestAuditEventBuilder<AddUpgradeRequestAuditEvent, AddUpgradeRequestAuditEventBuilder> {

    /**
     * Repository version
     */
    private String repositoryVersionId;

    /**
     * Upgrade type (rolling, non-rolling)
     */
    private String upgradeType;

    /**
     * Cluster name
     */
    private String clusterName;


    public AddUpgradeRequestAuditEventBuilder() {
      super(AddUpgradeRequestAuditEventBuilder.class);
      super.withOperation("Upgrade addition");
    }

    @Override
    protected AddUpgradeRequestAuditEvent newAuditEvent() {
      return new AddUpgradeRequestAuditEvent(this);
    }

    /**
     * Appends to the event the details of the incoming request.
     *
     * @param builder builder for the audit event details.
     */
    @Override
    protected void buildAuditMessage(StringBuilder builder) {
      super.buildAuditMessage(builder);

      builder.append(", Repository version ID(")
        .append(repositoryVersionId)
        .append("), Upgrade type(")
        .append(upgradeType)
        .append("), Cluster name(")
        .append(clusterName)
        .append(")");
    }

    public AddUpgradeRequestAuditEventBuilder withRepositoryVersionId(String repositoryVersionId) {
      this.repositoryVersionId = repositoryVersionId;
      return this;
    }

    public AddUpgradeRequestAuditEventBuilder withUpgradeType(String upgradeType) {
      this.upgradeType = upgradeType;
      return this;
    }

    public AddUpgradeRequestAuditEventBuilder withClusterName(String clusterName) {
      this.clusterName = clusterName;
      return this;
    }
  }

  protected AddUpgradeRequestAuditEvent() {
  }

  /**
   * {@inheritDoc}
   */
  protected AddUpgradeRequestAuditEvent(AddUpgradeRequestAuditEventBuilder builder) {
    super(builder);
  }

  /**
   * Returns an builder for {@link AddUpgradeRequestAuditEvent}
   *
   * @return a builder instance
   */
  public static AddUpgradeRequestAuditEventBuilder builder() {
    return new AddUpgradeRequestAuditEventBuilder();
  }

}
