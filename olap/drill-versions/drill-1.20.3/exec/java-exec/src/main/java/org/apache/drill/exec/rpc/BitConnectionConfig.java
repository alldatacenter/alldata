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
package org.apache.drill.exec.rpc;

import org.apache.drill.common.KerberosUtil;
import org.apache.drill.common.config.DrillConfig;
import org.apache.drill.common.config.DrillProperties;
import org.apache.drill.exec.ExecConstants;
import org.apache.drill.exec.exception.DrillbitStartupException;
import org.apache.drill.exec.memory.BufferAllocator;
import org.apache.drill.exec.proto.CoordinationProtos.DrillbitEndpoint;
import org.apache.drill.exec.rpc.security.AuthStringUtil;
import org.apache.drill.exec.rpc.security.AuthenticatorFactory;
import org.apache.drill.exec.rpc.security.AuthenticatorProvider;
import org.apache.drill.exec.server.BootStrapContext;
import org.apache.hadoop.security.HadoopKerberosName;
import org.apache.hadoop.security.UserGroupInformation;

import javax.security.sasl.SaslException;
import java.io.IOException;
import java.util.List;
import java.util.Map;

// config for bit to bit connection
public abstract class BitConnectionConfig extends AbstractConnectionConfig {
  private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(BitConnectionConfig.class);

  private final String authMechanismToUse;
  private final boolean useLoginPrincipal;

  protected BitConnectionConfig(BufferAllocator allocator, BootStrapContext context) throws DrillbitStartupException {
    super(allocator, context);

    final DrillConfig config = context.getConfig();
    final AuthenticatorProvider authProvider = getAuthProvider();

    if (config.getBoolean(ExecConstants.BIT_AUTHENTICATION_ENABLED)) {
      this.authMechanismToUse = config.getString(ExecConstants.BIT_AUTHENTICATION_MECHANISM);
      try {
        authProvider.getAuthenticatorFactory(authMechanismToUse);
      } catch (final SaslException e) {
        throw new DrillbitStartupException(String.format(
            "'%s' mechanism not found for bit-to-bit authentication. Please check authentication configuration.",
            authMechanismToUse));
      }

      // Update encryption related configurations
      encryptionContext.setEncryption(config.getBoolean(ExecConstants.BIT_ENCRYPTION_SASL_ENABLED));
      final int maxWrappedSize = config.getInt(ExecConstants.BIT_ENCRYPTION_SASL_MAX_WRAPPED_SIZE);

      if (maxWrappedSize <= 0) {
        throw new DrillbitStartupException(String.format("Invalid value configured for " +
            "bit.encryption.sasl.max_wrapped_size. Must be a positive integer in bytes with a recommended max value " +
            "of %s", RpcConstants.MAX_RECOMMENDED_WRAPPED_SIZE));
      } else if (maxWrappedSize > RpcConstants.MAX_RECOMMENDED_WRAPPED_SIZE) {
        logger.warn("The configured value of bit.encryption.sasl.max_wrapped_size: {} is too big. This may cause " +
            "higher memory pressure. [Details: Recommended max value is {}]",
            maxWrappedSize, RpcConstants.MAX_RECOMMENDED_WRAPPED_SIZE);
      }
      encryptionContext.setMaxWrappedSize(maxWrappedSize);

      logger.info("Configured bit-to-bit connections to require authentication using: {} with encryption: {}",
          authMechanismToUse, encryptionContext.getEncryptionCtxtString());

    } else if (config.getBoolean(ExecConstants.BIT_ENCRYPTION_SASL_ENABLED)) {
      throw new DrillbitStartupException("Invalid security configuration. Encryption using SASL is enabled with " +
          "authentication disabled. Please check the security.bit configurations.");
    } else {
      this.authMechanismToUse = null;
    }
    this.useLoginPrincipal = config.getBoolean(ExecConstants.USE_LOGIN_PRINCIPAL);
  }

  // returns null iff auth is disabled
  public String getAuthMechanismToUse() {
    return authMechanismToUse;
  }

  // convenience method
  public AuthenticatorFactory getAuthFactory(final List<String> remoteMechanisms) throws SaslException {
    if (authMechanismToUse == null) {
      throw new SaslException("Authentication is not enabled");
    }
    if (!AuthStringUtil.listContains(remoteMechanisms, authMechanismToUse)) {
      throw new SaslException(String.format("Remote does not support authentication using '%s'", authMechanismToUse));
    }
    return getAuthProvider().getAuthenticatorFactory(authMechanismToUse);
  }

  public Map<String, ?> getSaslClientProperties(final DrillbitEndpoint remoteEndpoint,
                                                final Map<String, String> overrides) throws IOException {
    final DrillProperties properties = DrillProperties.createEmpty();

    final UserGroupInformation loginUser = UserGroupInformation.getLoginUser();
    if (loginUser.getAuthenticationMethod() == UserGroupInformation.AuthenticationMethod.KERBEROS) {
      final HadoopKerberosName loginPrincipal = new HadoopKerberosName(loginUser.getUserName());
      if (!useLoginPrincipal) {
        properties.setProperty(DrillProperties.SERVICE_PRINCIPAL,
            KerberosUtil.getPrincipalFromParts(loginPrincipal.getShortName(),
                remoteEndpoint.getAddress(),
                loginPrincipal.getRealm()));
      } else {
        properties.setProperty(DrillProperties.SERVICE_PRINCIPAL, loginPrincipal.toString());
      }
    }

    properties.merge(overrides);
    return properties.stringPropertiesAsMap();
  }
}
