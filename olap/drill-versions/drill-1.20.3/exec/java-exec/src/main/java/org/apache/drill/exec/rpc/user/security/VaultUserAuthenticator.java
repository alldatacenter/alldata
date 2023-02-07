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
package org.apache.drill.exec.rpc.user.security;

import com.bettercloud.vault.Vault;
import com.bettercloud.vault.VaultConfig;
import com.bettercloud.vault.VaultException;
import com.bettercloud.vault.response.LookupResponse;
import org.apache.drill.common.config.DrillConfig;
import org.apache.drill.exec.exception.DrillbitStartupException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Objects;

/**
 * Implement {@link org.apache.drill.exec.rpc.user.security.UserAuthenticator}
 * based on HashiCorp Vault.  Configure the Vault client using the Drill BOOT
 * options that appear below.
 */
@UserAuthenticatorTemplate(type = "vault")
public class VaultUserAuthenticator implements UserAuthenticator {
  private static final Logger logger = LoggerFactory.getLogger(VaultUserAuthenticator.class);

  // Drill boot options used to configure Vault auth.
  public static final String VAULT_ADDRESS = "drill.exec.security.user.auth.vault.address";
  public static final String VAULT_AUTH_METHOD = "drill.exec.security.user.auth.vault.method";

  // The subset of Vault auth methods that are supported by this authenticator
  public enum VaultAuthMethod {
    APP_ROLE,
    LDAP,
    USER_PASS,
    VAULT_TOKEN
  }

  private VaultConfig vaultConfig;

  private Vault vault;

  private VaultAuthMethod authMethod;

  /**
   * Reads Drill BOOT options and uses them to set up a Vault client.
   * @param config object providing Drill config settings for Vault
   * @throws DrillbitStartupException if the provided Vault configuration is invalid
   */
  @Override
  public void setup(DrillConfig config) throws DrillbitStartupException {
    // Read config values
    String vaultAddress = Objects.requireNonNull(
      config.getString(VAULT_ADDRESS),
      String.format(
        "Vault address BOOT option is not specified. Please set [%s] config " +
        "option.",
        VAULT_ADDRESS
      )
    );

    this.authMethod = VaultAuthMethod.valueOf(
      Objects.requireNonNull(
        config.getString(VAULT_AUTH_METHOD),
        String.format(
          "Vault auth method is not specified. Please set [%s] config option.",
          VAULT_AUTH_METHOD
        )
      )
    );

    // There's no need for Drill have its own Vault token for it to use auth
    // methods.
    VaultConfig vaultConfBuilder = new VaultConfig().address(vaultAddress);

    // Initialise Vault client
    try {
      logger.debug(
        "Tries to init a Vault client with Vault addr = {}, auth method = {}",
        vaultAddress,
        authMethod
      );

      this.vaultConfig = vaultConfBuilder.build();
      this.vault = new Vault(this.vaultConfig);
    } catch (VaultException e) {
      logger.error(String.join(System.lineSeparator(),
          "Error initialising the Vault client library using configuration: ",
          "\tvaultAddress: {}",
          "\tauthMethod: {}"
        ),
        vaultAddress,
        authMethod,
        e
      );
      throw new DrillbitStartupException(
        "Error initialising the Vault client library: " + e.getMessage(),
        e
      );
    }
  }

  /**
   * Attempts to authenticate a Drill user with the provided password and the
   * configured Vault auth method.  Only auth methods that have a natural
   * mapping to PLAIN authentication's user and password strings are supported.
   * @param user username
   * @param password password
   * @throws UserAuthenticationException if the authentication attempt fails
   */
  @Override
  public void authenticate(
    String user,
    String password
  ) throws UserAuthenticationException {

    try {
      logger.debug("Tries to authenticate user {} using {}", user, authMethod);

      switch (authMethod) {
        case APP_ROLE:
          // user = role id, password = secret id
          vault.auth().loginByAppRole(user, password);
          break;
        case LDAP:
          // user = username, password = password
          vault.auth().loginByLDAP(user, password);
          break;
        case USER_PASS:
          // user = username, password = password
          vault.auth().loginByUserPass(user, password);
          break;
        case VAULT_TOKEN:
          // user = username, password = vault token

          // The BetterCloud Vault client doesn't provide an auth-by-token
          // method so instead we create a throwaway Vault client using the
          // provided token and send a token lookup request to Vault which
          // will fail if the token is invalid.
          VaultConfig lookupConfig = new VaultConfig()
            .address(this.vaultConfig.getAddress())
            .token(password)
            .build();

          LookupResponse lookupResp = new Vault(lookupConfig).auth().lookupSelf();
          // Check token owner using getPath() because getUsername() sometimes
          // returns null.
          if (!lookupResp.getPath().endsWith("/" + user)) {
            throw new UserAuthenticationException(String.format(
              "Attempted to authenticate user %s with a Vault token that is " +
              " valid but has path %s!",
              user,
              lookupResp.getPath()
            ));
          }
          break;

        default:
          throw new UserAuthenticationException(String.format(
            "The Vault authentication method '%s' is not supported",
            authMethod
          ));
        }
    } catch (VaultException e) {
      logger.warn("Failed to authenticate user {} using {}: {}.", user, authMethod, e);
      throw new UserAuthenticationException(
        String.format(
          "Failed to authenticate user %s using %s: %s",
          user,
          authMethod,
          e.getMessage()
        )
      );
    }

    logger.info(
      "User {} authenticated against Vault successfully.",
      user
    );
  }

  @Override
  public void close() throws IOException {
    this.vault = null;
    logger.debug("Has been closed.");
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    VaultUserAuthenticator that = (VaultUserAuthenticator) o;
    return Objects.equals(vaultConfig, that.vaultConfig)
      && Objects.equals(vault, that.vault)
      && authMethod == that.authMethod;
  }

  @Override
  public int hashCode() {
    return Objects.hash(vaultConfig, vault, authMethod);
  }
}
