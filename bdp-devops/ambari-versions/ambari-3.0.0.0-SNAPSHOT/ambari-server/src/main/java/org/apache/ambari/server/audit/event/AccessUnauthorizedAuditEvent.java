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

package org.apache.ambari.server.audit.event;

import javax.annotation.concurrent.Immutable;

/**
 * Access to given resource is not authorized.
 */
@Immutable
public class AccessUnauthorizedAuditEvent extends AbstractUserAuditEvent {

  public static class AccessUnauthorizedAuditEventBuilder
    extends AbstractUserAuditEventBuilder<AccessUnauthorizedAuditEvent, AccessUnauthorizedAuditEventBuilder> {

    /**
     * Name of http method (PUT, POST, DELETE, etc...)
     */
    private String httpMethodName;

    /**
     * The resource path that is tried to be accessed
     */
    private String resourcePath;

    private AccessUnauthorizedAuditEventBuilder() {
      super(AccessUnauthorizedAuditEventBuilder.class);
    }

    /**
     * Appends to the aduit event detail the list of the privileges
     * possessed by the principal requesting access to a resource.
     *
     * @param builder builder for the audit event details.
     */
    @Override
    protected void buildAuditMessage(StringBuilder builder) {
      super.buildAuditMessage(builder);

      builder
        .append(", Operation(")
        .append(httpMethodName)
        .append("), ResourcePath(")
        .append(resourcePath)
        .append("), Status(Failed), Reason(Access not authorized)");
    }

    public AccessUnauthorizedAuditEventBuilder withHttpMethodName(String httpMethodName) {
      this.httpMethodName = httpMethodName;
      return this;
    }

    public AccessUnauthorizedAuditEventBuilder withResourcePath(String resourcePath) {
      this.resourcePath = resourcePath;
      return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected AccessUnauthorizedAuditEvent newAuditEvent() {
      return new AccessUnauthorizedAuditEvent(this);
    }
  }

  private AccessUnauthorizedAuditEvent() {
  }

  /**
   * {@inheritDoc}
   */
  private AccessUnauthorizedAuditEvent(AccessUnauthorizedAuditEventBuilder builder) {
    super(builder);
  }

  /**
   * Returns an builder for {@link AccessUnauthorizedAuditEvent}
   *
   * @return a builder instance
   */
  public static AccessUnauthorizedAuditEventBuilder builder() {
    return new AccessUnauthorizedAuditEventBuilder();
  }
}
