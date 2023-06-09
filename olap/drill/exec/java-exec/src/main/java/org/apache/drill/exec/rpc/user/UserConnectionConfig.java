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
package org.apache.drill.exec.rpc.user;

import org.apache.drill.common.config.DrillConfig;
import org.apache.drill.exec.ExecConstants;
import org.apache.drill.exec.exception.DrillbitStartupException;
import org.apache.drill.exec.memory.BufferAllocator;
import org.apache.drill.exec.rpc.AbstractConnectionConfig;
import org.apache.drill.exec.rpc.RequestHandler;
import org.apache.drill.exec.rpc.RpcConstants;
import org.apache.drill.exec.rpc.security.AuthenticatorProvider;
import org.apache.drill.exec.server.BootStrapContext;

// config for bit to user connection
// package private
class UserConnectionConfig extends AbstractConnectionConfig {
  private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(UserConnectionConfig.class);

  private final boolean authEnabled;
  private final boolean sslEnabled;
  private final InboundImpersonationManager impersonationManager;

  private final UserServerRequestHandler handler;

  UserConnectionConfig(BufferAllocator allocator, BootStrapContext context, UserServerRequestHandler handler)
    throws DrillbitStartupException {
    super(allocator, context);
    this.handler = handler;

    final DrillConfig config = context.getConfig();
    final AuthenticatorProvider authProvider = getAuthProvider();

    if (config.getBoolean(ExecConstants.USER_AUTHENTICATION_ENABLED)) {
      if (authProvider.getAllFactoryNames().isEmpty()) {
        throw new DrillbitStartupException("Authentication enabled, but no mechanisms found. Please check " +
            "authentication configuration.");
      }
      authEnabled = true;

      // Update encryption related parameters.
      encryptionContext.setEncryption(config.getBoolean(ExecConstants.USER_ENCRYPTION_SASL_ENABLED));
      final int maxWrappedSize = config.getInt(ExecConstants.USER_ENCRYPTION_SASL_MAX_WRAPPED_SIZE);

      if (maxWrappedSize <= 0) {
        throw new DrillbitStartupException(String.format("Invalid value configured for " +
            "user.encryption.sasl.max_wrapped_size. Must be a positive integer in bytes with a recommended max value " +
            "of %s", RpcConstants.MAX_RECOMMENDED_WRAPPED_SIZE));
      } else if (maxWrappedSize > RpcConstants.MAX_RECOMMENDED_WRAPPED_SIZE) {
        logger.warn("The configured value of user.encryption.sasl.max_wrapped_size: {} is too big. This may cause " +
            "higher memory pressure. [Details: Recommended max value is {}]",
            maxWrappedSize, RpcConstants.MAX_RECOMMENDED_WRAPPED_SIZE);
      }
      encryptionContext.setMaxWrappedSize(maxWrappedSize);
      logger.info("Configured all user connections to require authentication with encryption: {} using: {}",
          encryptionContext.getEncryptionCtxtString(), authProvider.getAllFactoryNames());
    } else if (config.getBoolean(ExecConstants.USER_ENCRYPTION_SASL_ENABLED)) {
      throw new DrillbitStartupException("Invalid security configuration. Encryption using SASL is enabled with " +
          "authentication disabled. Please check the security.user configurations.");
    } else {
      authEnabled = false;
    }
    impersonationManager = config.getBoolean(ExecConstants.IMPERSONATION_ENABLED)
        ? new InboundImpersonationManager()
        : null;
    sslEnabled = config.getBoolean(ExecConstants.USER_SSL_ENABLED);
    if (isSSLEnabled() && isAuthEnabled() && isEncryptionEnabled()) {
      logger.warn("The server is configured to use both SSL and SASL encryption (only one should be configured).");
    }
  }

  @Override
  public String getName() {
    return "user server";
  }

  boolean isAuthEnabled() {
    return authEnabled;
  }

  boolean isSSLEnabled() {
    return sslEnabled;
  }

  InboundImpersonationManager getImpersonationManager() {
    return impersonationManager;
  }

  RequestHandler<UserServer.BitToUserConnection> getMessageHandler() {
    return handler;
  }
}
