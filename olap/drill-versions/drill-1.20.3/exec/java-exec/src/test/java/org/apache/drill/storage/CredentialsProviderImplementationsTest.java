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
package org.apache.drill.storage;

import com.bettercloud.vault.VaultException;
import org.apache.drill.common.config.DrillConfig;
import org.apache.drill.common.logical.security.CredentialsProvider;
import org.apache.drill.exec.store.security.EnvCredentialsProvider;
import org.apache.drill.exec.store.security.HadoopCredentialsProvider;
import org.apache.drill.exec.store.security.UsernamePasswordCredentials;
import org.apache.drill.exec.store.security.vault.VaultCredentialsProvider;
import org.apache.drill.shaded.guava.com.google.common.collect.ImmutableMap;
import org.apache.drill.test.ClusterFixture;
import org.apache.drill.test.ClusterTest;
import org.apache.hadoop.conf.Configuration;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.vault.VaultContainer;

import java.util.Collections;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class CredentialsProviderImplementationsTest extends ClusterTest {

  private static final String VAULT_TOKEN_VALUE = "vault-token";

  private static final String SECRET_PATH = "secret/testing";

  @ClassRule
  public static final VaultContainer<?> vaultContainer =
      new VaultContainer<>(DockerImageName.parse("vault").withTag("1.1.3"))
          .withVaultToken(VAULT_TOKEN_VALUE)
          .withVaultPort(8200)
          .withSecretInVault(SECRET_PATH,
              "top_secret=password1",
              "db_password=dbpassword1");

  @BeforeClass
  public static void init() throws Exception {
    startCluster(ClusterFixture.builder(dirTestWatcher)
        .configProperty(VaultCredentialsProvider.VAULT_ADDRESS, "http://" + vaultContainer.getHost() + ":" + vaultContainer.getMappedPort(8200))
    .configProperty(VaultCredentialsProvider.VAULT_TOKEN, VAULT_TOKEN_VALUE));
  }

  @Test
  public void testEnvCredentialsProvider() {
    String variableName = "USER";
    String expectedValue = System.getenv(variableName);

    CredentialsProvider envCredentialsProvider = new EnvCredentialsProvider(ImmutableMap.of(
        UsernamePasswordCredentials.USERNAME, variableName));

    Map<String, String> actualCredentials = envCredentialsProvider.getCredentials();

    assertEquals(Collections.singletonMap(UsernamePasswordCredentials.USERNAME, expectedValue),
        actualCredentials);
  }

  @Test
  public void testHadoopCredentialsProvider() {
    Configuration configuration = new Configuration();
    String expectedUsernameValue = "user1";
    String expectedPassValue = "pass123!@#";
    String usernamePropertyName = "username_key";
    String passwordPropertyName = "password_key";
    configuration.set(usernamePropertyName, expectedUsernameValue);
    configuration.set(passwordPropertyName, expectedPassValue);

    CredentialsProvider envCredentialsProvider = new HadoopCredentialsProvider(configuration,
        ImmutableMap.of(
            UsernamePasswordCredentials.USERNAME, usernamePropertyName,
            UsernamePasswordCredentials.PASSWORD, passwordPropertyName));

    Map<String, String> actualCredentials = envCredentialsProvider.getCredentials();

    assertEquals(ImmutableMap.of(
        UsernamePasswordCredentials.USERNAME, expectedUsernameValue,
        UsernamePasswordCredentials.PASSWORD, expectedPassValue),
        actualCredentials);
  }

  @Test
  public void testVaultCredentialsProvider() throws VaultException {
    DrillConfig config = cluster.drillbit().getContext().getConfig();

    CredentialsProvider envCredentialsProvider = new VaultCredentialsProvider(
        SECRET_PATH,
        ImmutableMap.of(UsernamePasswordCredentials.USERNAME, "top_secret",
            UsernamePasswordCredentials.PASSWORD, "db_password"),
        config);

    Map<String, String> actualCredentials = envCredentialsProvider.getCredentials();

    assertEquals(ImmutableMap.of(UsernamePasswordCredentials.USERNAME, "password1",
        UsernamePasswordCredentials.PASSWORD, "dbpassword1"),
        actualCredentials);
  }
}
