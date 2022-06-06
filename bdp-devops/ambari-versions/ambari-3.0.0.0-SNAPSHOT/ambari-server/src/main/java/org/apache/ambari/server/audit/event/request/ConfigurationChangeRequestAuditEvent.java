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
 * Audit event for configuration change
 */
@Immutable
public class ConfigurationChangeRequestAuditEvent extends RequestAuditEvent {

  public static class ConfigurationChangeRequestAuditEventBuilder extends RequestAuditEventBuilder<ConfigurationChangeRequestAuditEvent, ConfigurationChangeRequestAuditEventBuilder> {

    /**
     * Version number of the new version
     */
    private String versionNumber;

    /**
     * Version note for the new version
     */
    private String versionNote;

    public ConfigurationChangeRequestAuditEventBuilder() {
      super(ConfigurationChangeRequestAuditEventBuilder.class);
      super.withOperation("Configuration change");
    }

    @Override
    protected ConfigurationChangeRequestAuditEvent newAuditEvent() {
      return new ConfigurationChangeRequestAuditEvent(this);
    }

    /**
     * Appends to the event the details of the incoming request.
     *
     * @param builder builder for the audit event details.
     */
    @Override
    protected void buildAuditMessage(StringBuilder builder) {
      super.buildAuditMessage(builder);

      builder
        .append(", VersionNumber(V")
        .append(versionNumber)
        .append("), ")
        .append("VersionNote(")
        .append(versionNote)
        .append(")");
    }

    public ConfigurationChangeRequestAuditEventBuilder withVersionNumber(String versionNumber) {
      this.versionNumber = versionNumber;
      return this;
    }

    public ConfigurationChangeRequestAuditEventBuilder withVersionNote(String versionNote) {
      this.versionNote = versionNote;
      return this;
    }

  }

  protected ConfigurationChangeRequestAuditEvent() {
  }

  /**
   * {@inheritDoc}
   */
  protected ConfigurationChangeRequestAuditEvent(ConfigurationChangeRequestAuditEventBuilder builder) {
    super(builder);
  }

  /**
   * Returns an builder for {@link ConfigurationChangeRequestAuditEvent}
   *
   * @return a builder instance
   */
  public static ConfigurationChangeRequestAuditEventBuilder builder() {
    return new ConfigurationChangeRequestAuditEventBuilder();
  }

}
