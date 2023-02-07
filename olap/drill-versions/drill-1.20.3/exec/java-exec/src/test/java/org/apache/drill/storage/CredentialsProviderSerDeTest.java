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
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.drill.common.config.DrillConfig;
import org.apache.drill.common.logical.security.CredentialsProvider;
import org.apache.drill.exec.store.security.EnvCredentialsProvider;
import org.apache.drill.exec.store.security.HadoopCredentialsProvider;
import org.apache.drill.common.logical.security.PlainCredentialsProvider;
import org.apache.drill.exec.store.security.UsernamePasswordCredentials;
import org.apache.drill.exec.store.security.vault.VaultCredentialsProvider;
import org.apache.drill.shaded.guava.com.google.common.collect.ImmutableMap;
import org.apache.drill.test.ClusterFixture;
import org.apache.drill.test.ClusterTest;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.vault.VaultContainer;

import static org.junit.Assert.assertEquals;

public class CredentialsProviderSerDeTest extends ClusterTest {

  private static final String VAULT_TOKEN_VALUE = "vault-token";

  private static final String SECRET_PATH = "secret/testing";

  @ClassRule
  public static final VaultContainer<?> vaultContainer =
      new VaultContainer<>(DockerImageName.parse("vault").withTag("1.1.3"))
          .withVaultToken(VAULT_TOKEN_VALUE)
          .withSecretInVault(SECRET_PATH,
              "top_secret=password1",
              "db_password=dbpassword1");

  @BeforeClass
  public static void init() throws Exception {
    startCluster(ClusterFixture.builder(dirTestWatcher)
        .configProperty(VaultCredentialsProvider.VAULT_ADDRESS, "http://" + vaultContainer.getHost() + ":" + vaultContainer.getFirstMappedPort())
        .configProperty(VaultCredentialsProvider.VAULT_TOKEN, VAULT_TOKEN_VALUE));
  }

  @Test
  public void testEnvCredentialsProviderSerDe() throws JsonProcessingException {
    ObjectMapper mapper = cluster.drillbit().getContext().getLpPersistence().getMapper();

    CredentialsProvider envCredentialsProvider = new EnvCredentialsProvider(ImmutableMap.of(
        UsernamePasswordCredentials.USERNAME, "myLoginVar",
        UsernamePasswordCredentials.PASSWORD, "myPassVar"));

    String serialized = mapper.writerFor(CredentialsProvider.class).writeValueAsString(envCredentialsProvider);

    String expected =
        "{\n" +
        "  \"credentialsProviderType\" : \"EnvCredentialsProvider\",\n" +
        "  \"envVariables\" : {\n" +
        "    \"username\" : \"myLoginVar\",\n" +
        "    \"password\" : \"myPassVar\"\n" +
        "  }\n" +
        "}";

    assertEquals(expected, serialized);

    CredentialsProvider deserialized = mapper.readerFor(CredentialsProvider.class).readValue(serialized);

    assertEquals(envCredentialsProvider, deserialized);
  }

  @Test
  public void testPlainCredentialsProviderSerDe() throws JsonProcessingException {
    ObjectMapper mapper = cluster.drillbit().getContext().getLpPersistence().getMapper();

    CredentialsProvider credentialsProvider = new PlainCredentialsProvider(ImmutableMap.of(
        UsernamePasswordCredentials.USERNAME, "myLogin",
        UsernamePasswordCredentials.PASSWORD, "myPass"));

    String serialized = mapper.writerFor(CredentialsProvider.class).writeValueAsString(credentialsProvider);

    String expected =
        "{\n" +
        "  \"credentialsProviderType\" : \"PlainCredentialsProvider\",\n" +
        "  \"credentials\" : {\n" +
        "    \"username\" : \"myLogin\",\n" +
        "    \"password\" : \"myPass\"\n" +
        "  }\n" +
        "}";

    assertEquals(expected, serialized);

    CredentialsProvider deserialized = mapper.readerFor(CredentialsProvider.class).readValue(serialized);

    assertEquals(credentialsProvider, deserialized);
  }

  @Test
  public void testPlainCredentialsProviderWithNoType() throws JsonProcessingException {
    ObjectMapper mapper = cluster.drillbit().getContext().getLpPersistence().getMapper();

    CredentialsProvider expected = new PlainCredentialsProvider(ImmutableMap.of(
        UsernamePasswordCredentials.USERNAME, "myLogin",
        UsernamePasswordCredentials.PASSWORD, "myPass"));

    String serialized =
        "{\n" +
        "  \"credentials\" : {\n" +
        "    \"username\" : \"myLogin\",\n" +
        "    \"password\" : \"myPass\"\n" +
        "  }\n" +
        "}";

    CredentialsProvider deserialized = mapper.readerFor(CredentialsProvider.class).readValue(serialized);

    assertEquals(expected, deserialized);
  }

  @Test
  public void testHadoopCredentialsProviderSerDe() throws JsonProcessingException {
    ObjectMapper mapper = cluster.drillbit().getContext().getLpPersistence().getMapper();

    CredentialsProvider credentialsProvider = new HadoopCredentialsProvider(ImmutableMap.of(
        UsernamePasswordCredentials.USERNAME, "myLoginProp",
        UsernamePasswordCredentials.PASSWORD, "myPassProp"));

    String serialized = mapper.writerFor(CredentialsProvider.class).writeValueAsString(credentialsProvider);

    String expected =
        "{\n" +
        "  \"credentialsProviderType\" : \"HadoopCredentialsProvider\",\n" +
        "  \"propertyNames\" : {\n" +
        "    \"username\" : \"myLoginProp\",\n" +
        "    \"password\" : \"myPassProp\"\n" +
        "  }\n" +
        "}";

    assertEquals(expected, serialized);

    CredentialsProvider deserialized = mapper.readerFor(CredentialsProvider.class).readValue(serialized);

    assertEquals(credentialsProvider, deserialized);
  }

  @Test
  public void testVaultCredentialsProviderSerDe() throws JsonProcessingException, VaultException {
    ObjectMapper mapper = cluster.drillbit().getContext().getLpPersistence().getMapper();

    DrillConfig config = cluster.drillbit().getContext().getConfig();

    CredentialsProvider credentialsProvider = new VaultCredentialsProvider(SECRET_PATH, ImmutableMap.of(
        UsernamePasswordCredentials.USERNAME, "myLoginProp",
        UsernamePasswordCredentials.PASSWORD, "myPassProp"),
        config);

    String serialized = mapper.writerFor(CredentialsProvider.class).writeValueAsString(credentialsProvider);

    String expected =
        "{\n" +
        "  \"credentialsProviderType\" : \"VaultCredentialsProvider\",\n" +
        "  \"secretPath\" : \"secret/testing\",\n" +
        "  \"propertyNames\" : {\n" +
        "    \"username\" : \"myLoginProp\",\n" +
        "    \"password\" : \"myPassProp\"\n" +
        "  }\n" +
        "}";

    assertEquals(expected, serialized);

    CredentialsProvider deserialized = mapper.readerFor(CredentialsProvider.class).readValue(serialized);

    assertEquals(credentialsProvider, deserialized);
  }
}
