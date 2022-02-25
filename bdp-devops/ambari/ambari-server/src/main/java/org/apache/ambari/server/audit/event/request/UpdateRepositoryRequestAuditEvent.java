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
 * Audit event for updating repository
 */
@Immutable
public class UpdateRepositoryRequestAuditEvent extends RequestAuditEvent {

  public static class UpdateRepositoryRequestAuditEventBuilder extends RequestAuditEventBuilder<UpdateRepositoryRequestAuditEvent, UpdateRepositoryRequestAuditEventBuilder> {

    /**
     * Repository name
     */
    private String repo;

    /**
     * Stack name
     */
    private String stackName;

    /**
     * Operating system type
     */
    private String osType;

    /**
     * Base url for the repository
     */
    private String baseUrl;

    /**
     * Stack version
     */
    private String stackVersion;

    public UpdateRepositoryRequestAuditEventBuilder() {
      super(UpdateRepositoryRequestAuditEventBuilder.class);
      super.withOperation("Repository update");
    }

    @Override
    protected UpdateRepositoryRequestAuditEvent newAuditEvent() {
      return new UpdateRepositoryRequestAuditEvent(this);
    }

    /**
     * Appends to the event the details of the incoming request.
     *
     * @param builder builder for the audit event details.
     */
    @Override
    protected void buildAuditMessage(StringBuilder builder) {
      super.buildAuditMessage(builder);

      builder.append(", Stack(")
        .append(stackName)
        .append("), Stack version(")
        .append(stackVersion)
        .append("), OS(")
        .append(osType)
        .append("), Repo id(")
        .append(repo)
        .append("), Base URL(")
        .append(baseUrl)
        .append(")");
    }

    public UpdateRepositoryRequestAuditEventBuilder withRepo(String repo) {
      this.repo = repo;
      return this;
    }

    public UpdateRepositoryRequestAuditEventBuilder withStackName(String stackName) {
      this.stackName = stackName;
      return this;
    }

    public UpdateRepositoryRequestAuditEventBuilder withOsType(String osType) {
      this.osType = osType;
      return this;
    }

    public UpdateRepositoryRequestAuditEventBuilder withBaseUrl(String baseUrl) {
      this.baseUrl = baseUrl;
      return this;
    }

    public UpdateRepositoryRequestAuditEventBuilder withStackVersion(String stackVersion) {
      this.stackVersion = stackVersion;
      return this;
    }
  }

  protected UpdateRepositoryRequestAuditEvent() {
  }

  /**
   * {@inheritDoc}
   */
  protected UpdateRepositoryRequestAuditEvent(UpdateRepositoryRequestAuditEventBuilder builder) {
    super(builder);
  }

  /**
   * Returns an builder for {@link UpdateRepositoryRequestAuditEvent}
   *
   * @return a builder instance
   */
  public static UpdateRepositoryRequestAuditEventBuilder builder() {
    return new UpdateRepositoryRequestAuditEventBuilder();
  }

}
