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

import java.util.List;
import java.util.Map;

import javax.annotation.concurrent.Immutable;

import org.apache.ambari.server.audit.request.RequestAuditEvent;

/**
 * Audit event for adding repository version
 */
@Immutable
public class AddRepositoryVersionRequestAuditEvent extends RequestAuditEvent {

  public static class AddRepositoryVersionAuditEventBuilder extends RequestAuditEventBuilder<AddRepositoryVersionRequestAuditEvent, AddRepositoryVersionAuditEventBuilder> {

    /**
     * Stack name
     */
    private String stackName;

    /**
     * Display name
     */
    private String displayName;

    /**
     * Stack version
     */
    private String stackVersion;

    /**
     * Repository version
     */
    private String repoVersion;

    /**
     * Details of the repositories
     * os type -> list of repositories, where a repository is a key-value map of the properties (repo_id, repo_name, base_url)
     */
    private Map<String, List<Map<String, String>>> repos;

    public AddRepositoryVersionAuditEventBuilder() {
      super(AddRepositoryVersionAuditEventBuilder.class);
      super.withOperation("Repository version addition");
    }

    @Override
    protected AddRepositoryVersionRequestAuditEvent newAuditEvent() {
      return new AddRepositoryVersionRequestAuditEvent(this);
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
        .append("), Display name(")
        .append(displayName)
        .append("), Repo version(")
        .append(repoVersion)
        .append("), Repositories(");

      if (!repos.isEmpty()) {
        builder.append(System.lineSeparator());
      }

      for (Map.Entry<String, List<Map<String, String>>> repo : repos.entrySet()) {
        builder.append("Operating system: ").append(repo.getKey());
        builder.append(System.lineSeparator());
        for (Map<String, String> properties : repo.getValue()) {
          builder.append("    Repository ID(").append(properties.get("repo_id"));
          builder.append("), Repository name(").append(properties.get("repo_name"));
          builder.append("), Base url(").append(properties.get("base_url")).append(")");
          builder.append(System.lineSeparator());
        }
      }

      builder.append(")");
    }

    public AddRepositoryVersionAuditEventBuilder withStackName(String stackName) {
      this.stackName = stackName;
      return this;
    }

    public AddRepositoryVersionAuditEventBuilder withDisplayName(String displayName) {
      this.displayName = displayName;
      return this;
    }

    public AddRepositoryVersionAuditEventBuilder withStackVersion(String stackVersion) {
      this.stackVersion = stackVersion;
      return this;
    }

    public AddRepositoryVersionAuditEventBuilder withRepoVersion(String repoVersion) {
      this.repoVersion = repoVersion;
      return this;
    }

    public AddRepositoryVersionAuditEventBuilder withRepos(Map<String, List<Map<String, String>>> repos) {
      this.repos = repos;
      return this;
    }
  }

  protected AddRepositoryVersionRequestAuditEvent() {
  }

  /**
   * {@inheritDoc}
   */
  protected AddRepositoryVersionRequestAuditEvent(AddRepositoryVersionAuditEventBuilder builder) {
    super(builder);
  }

  /**
   * Returns an builder for {@link AddRepositoryVersionRequestAuditEvent}
   *
   * @return a builder instance
   */
  public static AddRepositoryVersionAuditEventBuilder builder() {
    return new AddRepositoryVersionAuditEventBuilder();
  }

}
