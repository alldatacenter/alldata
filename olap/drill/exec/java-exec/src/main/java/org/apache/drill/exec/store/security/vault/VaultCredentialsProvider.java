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
package org.apache.drill.exec.store.security.vault;

import com.bettercloud.vault.Vault;
import com.bettercloud.vault.VaultConfig;
import com.bettercloud.vault.VaultException;
import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.OptBoolean;
import org.apache.drill.common.config.DrillConfig;
import org.apache.drill.common.logical.security.CredentialsProvider;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Implementation of {@link CredentialsProvider} that obtains credential values from
 * {@link Vault}.
 */
public class VaultCredentialsProvider implements CredentialsProvider {

  // Drill boot options used to configure a Vault credentials provider
  public static final String VAULT_ADDRESS = "drill.exec.storage.vault.address";
  public static final String VAULT_TOKEN = "drill.exec.storage.vault.token";

  private final String secretPath;

  private final Map<String, String> propertyNames;

  private final Vault vault;

  /**
   * @param secretPath The Vault key value from which to read
   * @param propertyNames map with credential names as keys and vault keys as values.
   * @param config drill config
   * @throws VaultException if exception happens when connecting to Vault.
   */
  @JsonCreator
  public VaultCredentialsProvider(
      @JsonProperty("secretPath") String secretPath,
      @JsonProperty("propertyNames") Map<String, String> propertyNames,
      @JacksonInject(useInput = OptBoolean.FALSE) DrillConfig config) throws VaultException {
    this.propertyNames = propertyNames;
    this.secretPath = secretPath;
    String vaultAddress = Objects.requireNonNull(config.getString(VAULT_ADDRESS),
        String.format("Vault address is not specified. Please set [%s] config property.", VAULT_ADDRESS));
    String token = Objects.requireNonNull(config.getString(VAULT_TOKEN),
        String.format("Vault token is not specified. Please set [%s] config property.", VAULT_TOKEN));

    VaultConfig vaultConfig = new VaultConfig()
        .address(vaultAddress)
        .token(token)
        .build();
    this.vault = new Vault(vaultConfig);
  }

  @Override
  public Map<String, String> getCredentials() {
    Map<String, String> credentials = new HashMap<>();
    propertyNames.forEach((key, value) -> {
      try {
        String credValue = vault.logical()
            .read(secretPath)
            .getData()
            .get(value);
        credentials.put(key, credValue);
      } catch (VaultException e) {
        throw new RuntimeException("Error while fetching credentials from vault", e);
      }
    });

    return credentials;
  }

  public String getSecretPath() {
    return secretPath;
  }

  public Map<String, String> getPropertyNames() {
    return propertyNames;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    VaultCredentialsProvider that = (VaultCredentialsProvider) o;
    return Objects.equals(secretPath, that.secretPath) && Objects.equals(propertyNames, that.propertyNames);
  }

  @Override
  public int hashCode() {
    return Objects.hash(secretPath, propertyNames);
  }
}
