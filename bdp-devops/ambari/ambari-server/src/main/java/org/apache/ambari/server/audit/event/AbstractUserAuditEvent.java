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

import org.apache.ambari.server.security.authorization.AuthorizationHelper;
import org.apache.commons.lang.StringUtils;

/**
 * Base class for audit events which are result of user actions. It appends
 * to audit event details the user name and remote ip of the host
 * where user actions originates from.
 */
public abstract class AbstractUserAuditEvent extends AbstractAuditEvent {

  public static abstract class AbstractUserAuditEventBuilder<T extends AbstractUserAuditEvent, TBuilder extends AbstractUserAuditEventBuilder<T, TBuilder>>
    extends AbstractAuditEventBuilder<T, TBuilder> {

    /**
     * Name of the user started the operation
     */
    private String userName = AuthorizationHelper.getAuthenticatedName();

    /**
     * Name of the proxy user if proxied
     */
    private String proxyUserName = AuthorizationHelper.getProxyUserName();

    /**
     * Ip of the user who started the operation. Note: remote ip might not be the original ip (proxies, routers can modify it)
     */
    private String remoteIp;

    protected AbstractUserAuditEventBuilder(Class<? extends TBuilder> builderClass) {
      super(builderClass);
    }

    /**
     * Appends to audit event details the user name and remote ip of the host
     * where user actions originates from.
     *
     * @param builder builder for the audit message details.
     */
    @Override
    protected void buildAuditMessage(StringBuilder builder) {
      builder
        .append("User(")
        .append(this.userName)
        .append("), RemoteIp(")
        .append(this.remoteIp)
        .append(")");
      if (StringUtils.isNotEmpty(this.proxyUserName)){
        builder
            .append(", ProxyUser(")
            .append(this.proxyUserName)
            .append(")");
      }
    }

    /**
     * Sets the user name of the user that initiated the audited action.
     *
     * @param userName
     * @return the builder
     */
    public TBuilder withUserName(String userName) {
      this.userName = userName;

      return self();
    }

    /**
     * Sets the proxy user name.
     *
     * @param proxyUserName
     * @return the builder
     */
    public TBuilder withProxyUserName(String proxyUserName) {
      this.proxyUserName = proxyUserName;

      return self();
    }

    /**
     * Sets the remote ip where the user action originated from.
     *
     * @param ip
     * @return the builder
     */
    public TBuilder withRemoteIp(String ip) {
      this.remoteIp = ip;

      return self();
    }
  }

  protected AbstractUserAuditEvent() {
  }

  /**
   * {@inheritDoc}
   */
  protected AbstractUserAuditEvent(AbstractUserAuditEventBuilder<?, ?> builder) {
    super(builder);
  }

}
