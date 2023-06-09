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
import com.bettercloud.vault.response.AuthResponse;
import org.apache.drill.categories.SecurityTest;
import org.apache.drill.common.config.DrillProperties;
import org.apache.drill.exec.ExecConstants;
import org.apache.drill.test.ClientFixture;
import org.apache.drill.test.ClusterFixture;
import org.apache.drill.test.ClusterTest;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.vault.VaultContainer;
import org.testcontainers.vault.VaultLogLevel;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.fail;

@Category(SecurityTest.class)
public class TestVaultUserAuthenticator extends ClusterTest {

  private static final String ROOT_TOKEN_VALUE = "vault-token";

  private static String vaultAddr;

  @ClassRule
  public static final VaultContainer<?> vaultContainer =
      new VaultContainer<>(DockerImageName.parse("vault").withTag("1.1.3"))
          .withLogLevel(VaultLogLevel.Debug)
          .withVaultToken(ROOT_TOKEN_VALUE)
          .withInitCommand(
            "auth enable userpass",
            "write auth/userpass/users/alice password=pass1 policies=admins",
            "write auth/userpass/users/bob password=buzzkill policies=admins"
          );

  @BeforeClass
  public static void init() throws Exception {
    vaultAddr = String.format(
      "http://%s:%d",
      vaultContainer.getHost(),
      vaultContainer.getMappedPort(8200)
    );
  }

  @Test
  public void testUserPassAuth() throws Exception {
    cluster = ClusterFixture.bareBuilder(dirTestWatcher)
      .clusterSize(3)
      .configProperty(ExecConstants.ALLOW_LOOPBACK_ADDRESS_BINDING, true)
      .configProperty(ExecConstants.USER_AUTHENTICATION_ENABLED, true)
      .configProperty(ExecConstants.USER_AUTHENTICATOR_IMPL, "vault")
      .configProperty(VaultUserAuthenticator.VAULT_ADDRESS, vaultAddr)
      .configProperty(
        VaultUserAuthenticator.VAULT_AUTH_METHOD,
        VaultUserAuthenticator.VaultAuthMethod.USER_PASS
      )
      .build();

    tryCredentials("notalice", "pass1", cluster, false);
    tryCredentials("notbob", "buzzkill", cluster, false);
    tryCredentials("alice", "wrong", cluster, false);
    tryCredentials("bob", "incorrect", cluster, false);
    tryCredentials("alice", "pass1", cluster, true);
    tryCredentials("bob", "buzzkill", cluster, true);
  }

  @Test
  public void testVaultTokenAuth() throws Exception {
    // Use the Vault client lib to obtain Vault tokens for our test users.
    VaultConfig vaultConfig = new VaultConfig()
      .address(vaultAddr)
      .token(ROOT_TOKEN_VALUE)
      .build();

    Vault vault = new Vault(vaultConfig);
    AuthResponse aliceResp = vault.auth().loginByUserPass("alice", "pass1");
    String aliceAuthToken = aliceResp.getAuthClientToken();
    AuthResponse bobResp = vault.auth().loginByUserPass("bob", "buzzkill");
    String bobAuthToken = bobResp.getAuthClientToken();

    // set up a new cluster with a config option selecting Vault token auth
    cluster = ClusterFixture.bareBuilder(dirTestWatcher)
      .clusterSize(3)
      .configProperty(ExecConstants.ALLOW_LOOPBACK_ADDRESS_BINDING, true)
      .configProperty(ExecConstants.USER_AUTHENTICATION_ENABLED, true)
      .configProperty(ExecConstants.USER_AUTHENTICATOR_IMPL, "vault")
      .configProperty(VaultUserAuthenticator.VAULT_ADDRESS, vaultAddr)
      .configProperty(
        VaultUserAuthenticator.VAULT_AUTH_METHOD,
        VaultUserAuthenticator.VaultAuthMethod.VAULT_TOKEN
      )
      .build();

    tryCredentials("notalice", aliceAuthToken, cluster, false);
    tryCredentials("notbob", bobAuthToken, cluster, false);
    tryCredentials("alice", "wrong", cluster, false);
    tryCredentials("bob", "incorrect", cluster, false);
    tryCredentials("alice", aliceAuthToken, cluster, true);
    tryCredentials("bob", bobAuthToken, cluster, true);
  }

  private static void tryCredentials(String user, String password, ClusterFixture cluster, boolean shouldSucceed) throws Exception {
    try {
      ClientFixture client = cluster.clientBuilder()
        .property(DrillProperties.USER, user)
        .property(DrillProperties.PASSWORD, password)
        .build();

      // Run few queries using the new client
      List<String> queries = Arrays.asList(
        "SHOW SCHEMAS",
        "USE INFORMATION_SCHEMA",
        "SHOW TABLES",
        "SELECT * FROM INFORMATION_SCHEMA.`TABLES` WHERE TABLE_NAME LIKE 'COLUMNS'",
        "SELECT * FROM cp.`region.json` LIMIT 5");

      for (String query : queries) {
        client.queryBuilder().sql(query).run();
      }

      if (!shouldSucceed) {
        fail("Expected connect to fail because of incorrect username / password combination, but it succeeded");
      }
    } catch (IllegalStateException e) {
      if (shouldSucceed) {
        throw e;
      }
    }
  }

}
