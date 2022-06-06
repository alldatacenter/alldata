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
 * Audit event for adding repository
 */
@Immutable
public class AddRepositoryRequestAuditEvent extends RequestAuditEvent {

  public static class AddRepositoryRequestAuditEventBuilder extends RequestAuditEventBuilder<AddRepositoryRequestAuditEvent, AddRepositoryRequestAuditEventBuilder> {

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

    public AddRepositoryRequestAuditEventBuilder() {
      super(AddRepositoryRequestAuditEventBuilder.class);
      super.withOperation("Repository addition");
    }

    @Override
    protected AddRepositoryRequestAuditEvent newAuditEvent() {
      return new AddRepositoryRequestAuditEvent(this);
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

    public AddRepositoryRequestAuditEventBuilder withRepo(String repo) {
      this.repo = repo;
      return this;
    }

    public AddRepositoryRequestAuditEventBuilder withStackName(String stackName) {
      this.stackName = stackName;
      return this;
    }

    public AddRepositoryRequestAuditEventBuilder withOsType(String osType) {
      this.osType = osType;
      return this;
    }

    public AddRepositoryRequestAuditEventBuilder withBaseUrl(String baseUrl) {
      this.baseUrl = baseUrl;
      return this;
    }

    public AddRepositoryRequestAuditEventBuilder withStackVersion(String stackVersion) {
      this.stackVersion = stackVersion;
      return this;
    }
  }

  protected AddRepositoryRequestAuditEvent() {
  }

  /**
   * {@inheritDoc}
   */
  protected AddRepositoryRequestAuditEvent(AddRepositoryRequestAuditEventBuilder builder) {
    super(builder);
  }

  /**
   * Returns an builder for {@link AddRepositoryRequestAuditEvent}
   *
   * @return a builder instance
   */
  public static AddRepositoryRequestAuditEventBuilder builder() {
    return new AddRepositoryRequestAuditEventBuilder();
  }

}
